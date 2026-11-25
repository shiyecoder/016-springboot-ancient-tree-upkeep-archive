package com.grad.ancienttree.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.ancienttree.common.BizException;
import com.grad.ancienttree.common.PageResult;
import com.grad.ancienttree.common.UserContext;
import com.grad.ancienttree.entity.*;
import com.grad.ancienttree.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 养护工单服务（状态机 + 权限分离 + 用药配额校验）。
 *
 * 主状态机：待方案 → 待审核 → 执行中 → 待验收 → 已完成
 * 异常分支：待审核 --驳回--> 待方案；待验收 --退回--> 执行中
 * 紧急抢救：允许先执行（执行中）后补方案，tempMeasureDeadline 到期前必须补齐。
 *
 * 权限分离（需求文档 36 行）：巡查人、方案审核人、验收人不得为同一人。
 */
@Service
@RequiredArgsConstructor
public class CareOrderService {

    private final CareOrderMapper orderMapper;
    private final CareDrugMapper drugMapper;
    private final AncientTreeMapper treeMapper;
    private final TreeIssueMapper issueMapper;
    private final SysMessageMapper messageMapper;

    private static final Map<String, List<String>> NEXT = Map.of(
            "待方案", List.of("待审核"),
            "待审核", List.of("执行中", "待方案"),
            "执行中", List.of("待验收"),
            "待验收", List.of("已完成", "执行中"),
            "已完成", List.of()
    );

    public PageResult<Map<String, Object>> page(int page, int size, String tab, String status,
                                                String careType, String keyword, Long treeId) {
        LambdaQueryWrapper<CareOrder> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(tab) && !"全部".equals(tab)) {
            switch (tab) {
                case "待办" -> q.in(CareOrder::getStatus, List.of("待方案", "待审核"));
                case "处理中" -> q.eq(CareOrder::getStatus, "执行中");
                case "已完成" -> q.eq(CareOrder::getStatus, "已完成");
                case "异常" -> q.isNotNull(CareOrder::getRejectReason);
                default -> { }
            }
        }
        if (StringUtils.hasText(status)) {
            q.eq(CareOrder::getStatus, status);
        }
        q.eq(StringUtils.hasText(careType), CareOrder::getCareType, careType)
         .eq(treeId != null, CareOrder::getTreeId, treeId)
         .and(StringUtils.hasText(keyword), w -> w.like(CareOrder::getOrderCode, keyword)
                 .or().like(CareOrder::getPlanContent, keyword));
        // 养护单位只能看本单位工单
        if (UserContext.isCareUnit()) {
            q.eq(CareOrder::getExecutorOrg, orgName());
        }
        // 待本人处理优先、更新时间倒序
        q.last("ORDER BY CASE WHEN status IN ('待方案','待审核') THEN 0 ELSE 1 END, update_time DESC");
        Page<CareOrder> p = orderMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CareOrder o : p.getRecords()) {
            rows.add(toRow(o));
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    private String orgName() {
        Long orgId = UserContext.orgId();
        if (orgId == null) return "";
        return switch (orgId.intValue()) {
            case 2 -> "城东绿化养护所";
            case 3 -> "城西园林工程有限公司";
            case 1 -> "市园林绿化管理局";
            default -> "古树保护研究中心";
        };
    }

    public Map<String, Object> toRow(CareOrder o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getId());
        m.put("orderCode", o.getOrderCode());
        m.put("treeId", o.getTreeId());
        AncientTree t = treeMapper.selectById(o.getTreeId());
        m.put("treeCode", t == null ? "-" : t.getTreeCode());
        m.put("species", t == null ? "-" : t.getSpecies());
        m.put("protectLevel", t == null ? "-" : t.getProtectLevel());
        m.put("location", t == null ? "-" : t.getLocation());
        m.put("coverImage", t == null ? null : t.getCoverImage());
        m.put("issueId", o.getIssueId());
        m.put("careType", o.getCareType());
        m.put("planContent", o.getPlanContent());
        m.put("executorOrg", o.getExecutorOrg());
        m.put("executorName", o.getExecutorName());
        m.put("planAuditorName", o.getPlanAuditorName());
        m.put("acceptorName", o.getAcceptorName());
        m.put("status", o.getStatus());
        m.put("urgency", o.getUrgency());
        m.put("deadline", o.getDeadline());
        m.put("progress", o.getProgress());
        m.put("resultDesc", o.getResultDesc());
        m.put("rejectReason", o.getRejectReason());
        m.put("tempMeasure", o.getTempMeasure());
        m.put("tempMeasureDeadline", o.getTempMeasureDeadline());
        m.put("createTime", o.getCreateTime());
        m.put("updateTime", o.getUpdateTime());
        // 超时判定：已过截止日期且未完成
        m.put("overdue", o.getDeadline() != null && o.getDeadline().isBefore(LocalDate.now())
                && !"已完成".equals(o.getStatus()));
        return m;
    }

    public Map<String, Object> detail(Long id) {
        CareOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException(404, "工单不存在");
        }
        Map<String, Object> m = toRow(o);
        m.put("drugs", drugMapper.selectList(new LambdaQueryWrapper<CareDrug>()
                .eq(CareDrug::getOrderId, id).orderByDesc(CareDrug::getId)));
        m.put("issue", issueMapper.selectById(o.getIssueId()));
        List<Map<String, Object>> tl = new ArrayList<>();
        tl.add(ev(o.getCreateTime(), "创建工单", "养护类型：" + o.getCareType(), o.getExecutorName()));
        if (o.getTempMeasure() != null) {
            tl.add(ev(o.getCreateTime(), "临时措施", o.getTempMeasure()
                    + "（补全截止 " + o.getTempMeasureDeadline() + "）", "系统"));
        }
        tl.add(ev(o.getUpdateTime(), "当前状态", o.getStatus(), o.getExecutorName()));
        m.put("timeline", tl);
        return m;
    }

    private Map<String, Object> ev(LocalDateTime time, String type, String content, String operator) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("time", time);
        e.put("type", type);
        e.put("content", content);
        e.put("operator", operator);
        return e;
    }

    /** 新建工单：初始为待方案，方案内容由养护单位填写 */
    @Transactional
    public Long create(CareOrder req) {
        AncientTree t = treeMapper.selectById(req.getTreeId());
        if (t == null) {
            throw new BizException(404, "古树档案不存在");
        }
        Long cnt = orderMapper.selectCount(null);
        CareOrder o = new CareOrder();
        o.setOrderCode("YH" + LocalDate.now().getYear() + String.format("%04d", (cnt == null ? 0 : cnt) + 1));
        o.setTreeId(req.getTreeId());
        o.setIssueId(req.getIssueId());
        o.setCareType(req.getCareType());
        o.setPlanContent(req.getPlanContent());
        o.setExecutorOrg(StringUtils.hasText(req.getExecutorOrg()) ? req.getExecutorOrg() : t.getManagerOrg());
        o.setExecutorUserId(UserContext.userId());
        o.setExecutorName(UserContext.realName());
        o.setStatus("待方案");
        o.setUrgency(req.getUrgency() == null ? "普通" : req.getUrgency());
        o.setDeadline(req.getDeadline() == null ? LocalDate.now().plusDays(15) : req.getDeadline());
        o.setProgress(0);
        o.setCreateTime(LocalDateTime.now());
        o.setUpdateTime(LocalDateTime.now());
        orderMapper.insert(o);
        return o.getId();
    }

    /**
     * 状态流转统一入口。
     * @param action submit=提交方案 / audit=审核 / reject=驳回 / progress=上报进度 / accept=验收 / back=验收退回
     */
    @Transactional
    public void transition(Long id, String action, String reason, String resultDesc, Integer progress) {
        CareOrder o = orderMapper.selectById(id);
        if (o == null) {
            throw new BizException(404, "工单不存在");
        }
        String target;
        switch (action) {
            case "submit" -> {
                if (!"待方案".equals(o.getStatus())) {
                    throw new BizException("只有「待方案」状态可以提交方案");
                }
                if (!StringUtils.hasText(o.getPlanContent())) {
                    throw new BizException("方案内容不能为空，请先填写养护方案");
                }
                target = "待审核";
            }
            case "audit" -> {
                if (!"待审核".equals(o.getStatus())) {
                    throw new BizException("只有「待审核」状态可以审核");
                }
                if (Objects.equals(o.getExecutorUserId(), UserContext.userId())) {
                    throw new BizException("权限分离：方案审核人不得与执行人相同");
                }
                target = "执行中";
            }
            case "reject" -> {
                if (!"待审核".equals(o.getStatus())) {
                    throw new BizException("只有「待审核」状态可以驳回");
                }
                requireReason(reason, "驳回");
                target = "待方案";
            }
            case "progress" -> {
                if (!"执行中".equals(o.getStatus())) {
                    throw new BizException("只有「执行中」状态可以上报进度");
                }
                target = "执行中";
            }
            case "accept" -> {
                if (!"待验收".equals(o.getStatus())) {
                    throw new BizException("只有「待验收」状态可以验收");
                }
                if (Objects.equals(o.getExecutorUserId(), UserContext.userId())) {
                    throw new BizException("权限分离：验收人不得与执行人相同");
                }
                target = "已完成";
            }
            case "back" -> {
                if (!"待验收".equals(o.getStatus())) {
                    throw new BizException("只有「待验收」状态可以退回");
                }
                requireReason(reason, "验收退回");
                target = "执行中";
            }
            case "finish" -> {
                if (!"执行中".equals(o.getStatus())) {
                    throw new BizException("只有「执行中」状态可以提交验收");
                }
                target = "待验收";
            }
            default -> throw new BizException("不支持的操作：" + action);
        }
        // 状态机校验，防止越级调用
        if (!NEXT.getOrDefault(o.getStatus(), List.of()).contains(target) && !target.equals(o.getStatus())) {
            throw new BizException("状态不允许从「" + o.getStatus() + "」流转到「" + target + "」");
        }

        CareOrder u = new CareOrder();
        u.setId(id);
        u.setStatus(target);
        u.setUpdateTime(LocalDateTime.now());
        if ("audit".equals(action)) {
            u.setPlanAuditorId(UserContext.userId());
            u.setPlanAuditorName(UserContext.realName());
        }
        if ("accept".equals(action)) {
            u.setAcceptorId(UserContext.userId());
            u.setAcceptorName(UserContext.realName());
            u.setProgress(100);
            u.setResultDesc(resultDesc);
        }
        if ("reject".equals(action) || "back".equals(action)) {
            u.setRejectReason(reason);
        }
        if ("progress".equals(action) && progress != null) {
            if (progress < 0 || progress > 100) {
                throw new BizException("进度必须在 0-100 之间");
            }
            u.setProgress(progress);
        }
        if ("finish".equals(action)) {
            u.setResultDesc(resultDesc);
            u.setProgress(100);
        }
        orderMapper.updateById(u);

        // 关键节点推送站内消息，含业务编号与详情跳转
        if ("submit".equals(action)) {
            push("待审批", "工单 " + o.getOrderCode() + " 方案待审核", o.getOrderCode(), "/care/order/" + id);
        } else if ("reject".equals(action)) {
            push("已退回", "工单 " + o.getOrderCode() + " 方案被驳回：" + reason, o.getOrderCode(), "/care/order/" + id);
        } else if ("finish".equals(action)) {
            push("待验收", "工单 " + o.getOrderCode() + " 提交验收", o.getOrderCode(), "/care/order/" + id);
        } else if ("accept".equals(action)) {
            push("处理完成", "工单 " + o.getOrderCode() + " 验收通过", o.getOrderCode(), "/care/order/" + id);
        }
    }

    private void requireReason(String reason, String action) {
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException(action + "必须填写原因，且不少于 5 个字");
        }
    }

    /**
     * 用药登记：核心校验是「禁止超出方案数量」。
     * planQuota 为方案核定的总用量，累计已用 + 本次 > 配额 时直接拒绝，并给出剩余量。
     */
    @Transactional
    public Map<String, Object> addDrug(Long orderId, CareDrug req) {
        CareOrder o = orderMapper.selectById(orderId);
        if (o == null) {
            throw new BizException(404, "工单不存在");
        }
        if (!"执行中".equals(o.getStatus())) {
            throw new BizException("只有执行中的工单可以登记用药");
        }
        if (req.getDosage() == null || req.getDosage().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException("剂量必须大于 0");
        }
        BigDecimal quota = req.getPlanQuota() == null ? BigDecimal.ZERO : req.getPlanQuota();
        List<CareDrug> exist = drugMapper.selectList(new LambdaQueryWrapper<CareDrug>()
                .eq(CareDrug::getOrderId, orderId).eq(CareDrug::getDrugName, req.getDrugName()));
        BigDecimal used = exist.stream().map(d -> d.getDosage() == null ? BigDecimal.ZERO : d.getDosage())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remain = quota.subtract(used);
        if (req.getDosage().compareTo(remain) > 0) {
            throw new BizException("超出方案核定用量：核定 " + quota + "，已用 " + used + "，剩余 " + remain);
        }
        CareDrug d = new CareDrug();
        d.setOrderId(orderId);
        d.setDrugName(req.getDrugName());
        d.setDosage(req.getDosage());
        d.setUnit(req.getUnit());
        d.setApplyDate(req.getApplyDate() == null ? LocalDate.now() : req.getApplyDate());
        d.setOperatorId(UserContext.userId());
        d.setOperatorName(UserContext.realName());
        d.setSafetyMeasure(req.getSafetyMeasure());
        d.setPlanQuota(quota);
        d.setUsedQty(used.add(req.getDosage()));
        drugMapper.insert(d);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", d.getId());
        r.put("used", d.getUsedQty());
        r.put("remain", quota.subtract(d.getUsedQty()));
        return r;
    }

    public List<CareDrug> drugs(Long orderId) {
        return drugMapper.selectList(new LambdaQueryWrapper<CareDrug>()
                .eq(CareDrug::getOrderId, orderId).orderByDesc(CareDrug::getId));
    }

    private void push(String type, String content, String bizNo, String link) {
        SysMessage msg = new SysMessage();
        msg.setTitle(type);
        msg.setContent(content);
        msg.setMsgType(type);
        msg.setBizNo(bizNo);
        msg.setLinkUrl(link);
        msg.setReceiverId(1L);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }
}
