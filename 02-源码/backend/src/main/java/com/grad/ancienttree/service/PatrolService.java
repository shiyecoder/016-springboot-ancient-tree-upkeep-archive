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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 巡查计划与巡查记录服务。
 * 业务规则：巡查频率按保护级别设置；巡查录入树干空洞、倾斜、枯枝、树冠偏冠、病虫害、土壤、支撑和避雷设施；
 * 记录提交后按症状与安全影响自动分级（日常养护/重点观察/紧急抢救），紧急抢救自动生成养护工单。
 */
@Service
@RequiredArgsConstructor
public class PatrolService {

    private final PatrolPlanMapper planMapper;
    private final TreePatrolMapper patrolMapper;
    private final TreeIssueMapper issueMapper;
    private final AncientTreeMapper treeMapper;
    private final CareOrderMapper orderMapper;
    private final SysMessageMapper messageMapper;

    // ---------------- 巡查计划 ----------------

    public PageResult<Map<String, Object>> planPage(int page, int size, String status, String protectLevel, String keyword) {
        LambdaQueryWrapper<PatrolPlan> q = new LambdaQueryWrapper<>();
        q.eq(StringUtils.hasText(status), PatrolPlan::getStatus, status)
         .eq(StringUtils.hasText(protectLevel), PatrolPlan::getProtectLevel, protectLevel)
         .and(StringUtils.hasText(keyword), w -> w.like(PatrolPlan::getPlanName, keyword)
                 .or().like(PatrolPlan::getPlanCode, keyword));
        q.orderByDesc(PatrolPlan::getId);
        Page<PatrolPlan> p = planMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (PatrolPlan pl : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", pl.getId());
            m.put("planCode", pl.getPlanCode());
            m.put("planName", pl.getPlanName());
            m.put("protectLevel", pl.getProtectLevel());
            m.put("region", pl.getRegion());
            m.put("frequency", pl.getFrequency());
            m.put("assigneeName", pl.getAssigneeName());
            m.put("assigneeId", pl.getAssigneeId());
            m.put("startDate", pl.getStartDate());
            m.put("endDate", pl.getEndDate());
            m.put("status", pl.getStatus());
            m.put("remark", pl.getRemark());
            m.put("createTime", pl.getCreateTime());
            // 覆盖树木数：该等级该区域下的档案数量
            Long cover = treeMapper.selectCount(new LambdaQueryWrapper<AncientTree>()
                    .eq(StringUtils.hasText(pl.getProtectLevel()), AncientTree::getProtectLevel, pl.getProtectLevel())
                    .eq(StringUtils.hasText(pl.getRegion()), AncientTree::getRegion, pl.getRegion()));
            m.put("coverCount", cover);
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    @Transactional
    public Long savePlan(PatrolPlan req, Long id) {
        // 保护级别与频率的对应关系由规则约束，不允许随意填
        if (req.getProtectLevel() != null) {
            int min = switch (req.getProtectLevel()) {
                case "一级" -> 4;
                case "二级" -> 2;
                case "三级" -> 1;
                default -> 1;
            };
            if (req.getFrequency() == null || req.getFrequency() < min) {
                throw new BizException(req.getProtectLevel() + "古树每年至少巡查 " + min + " 次");
            }
        }
        if (id == null) {
            PatrolPlan p = new PatrolPlan();
            copy(req, p);
            Long cnt = planMapper.selectCount(null);
            p.setPlanCode("XC" + LocalDate.now().getYear() + String.format("%03d", (cnt == null ? 0 : cnt) + 1));
            p.setStatus(req.getStatus() == null ? "草稿" : req.getStatus());
            p.setCreateTime(LocalDateTime.now());
            planMapper.insert(p);
            return p.getId();
        }
        PatrolPlan p = new PatrolPlan();
        p.setId(id);
        copy(req, p);
        planMapper.updateById(p);
        return id;
    }

    private void copy(PatrolPlan from, PatrolPlan to) {
        to.setPlanName(from.getPlanName());
        to.setProtectLevel(from.getProtectLevel());
        to.setRegion(from.getRegion());
        to.setFrequency(from.getFrequency());
        to.setAssigneeId(from.getAssigneeId());
        to.setAssigneeName(from.getAssigneeName());
        to.setStartDate(from.getStartDate());
        to.setEndDate(from.getEndDate());
        to.setStatus(from.getStatus());
        to.setRemark(from.getRemark());
    }

    // ---------------- 巡查记录 ----------------

    /** 巡查记录列表：快捷标签 全部/待办/处理中/已完成/异常 */
    public PageResult<Map<String, Object>> patrolPage(int page, int size, String tab, String issueLevel,
                                                      String keyword, Long treeId) {
        LambdaQueryWrapper<TreePatrol> q = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(tab) && !"全部".equals(tab)) {
            switch (tab) {
                case "待办" -> q.eq(TreePatrol::getStatus, "待提交");
                case "处理中" -> q.eq(TreePatrol::getStatus, "已提交");
                case "已完成" -> q.eq(TreePatrol::getStatus, "已生成工单");
                case "异常" -> q.in(TreePatrol::getIssueLevel, List.of("重点观察", "紧急抢救"));
                default -> { }
            }
        }
        q.eq(StringUtils.hasText(issueLevel), TreePatrol::getIssueLevel, issueLevel)
         .eq(treeId != null, TreePatrol::getTreeId, treeId)
         .and(StringUtils.hasText(keyword), w -> w.like(TreePatrol::getPatrolCode, keyword)
                 .or().like(TreePatrol::getPatrolUserName, keyword));
        // 巡查人员只看自己的记录
        if (UserContext.isPatrol()) {
            q.eq(TreePatrol::getPatrolUserId, UserContext.userId());
        }
        q.orderByDesc(TreePatrol::getPatrolTime);
        Page<TreePatrol> p = patrolMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TreePatrol t : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("patrolCode", t.getPatrolCode());
            m.put("treeId", t.getTreeId());
            AncientTree tree = treeMapper.selectById(t.getTreeId());
            m.put("treeCode", tree == null ? "-" : tree.getTreeCode());
            m.put("species", tree == null ? "-" : tree.getSpecies());
            m.put("protectLevel", tree == null ? "-" : tree.getProtectLevel());
            m.put("location", tree == null ? "-" : tree.getLocation());
            m.put("patrolUserName", t.getPatrolUserName());
            m.put("patrolTime", t.getPatrolTime());
            m.put("issueLevel", t.getIssueLevel());
            m.put("result", t.getResult());
            m.put("status", t.getStatus());
            m.put("trunkHollow", t.getTrunkHollow());
            m.put("leanDegree", t.getLeanDegree());
            m.put("deadBranch", t.getDeadBranch());
            m.put("crownDeviation", t.getCrownDeviation());
            m.put("pestDesc", t.getPestDesc());
            m.put("soilDesc", t.getSoilDesc());
            m.put("supportFacility", t.getSupportFacility());
            m.put("lightningFacility", t.getLightningFacility());
            m.put("photos", t.getPhotos());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /**
     * 提交巡查记录。核心逻辑：症状 → 问题分级 → 必要时联动工单。
     * 分级依据（写死为业务规则）：
     *   出现空洞/严重倾斜/避雷失效 且 涉及一级古树 → 紧急抢救
     *   有病虫害/枯枝/支撑缺失 → 重点观察
     *   其余 → 日常养护
     */
    @Transactional
    public Map<String, Object> submitPatrol(TreePatrol req) {
        AncientTree tree = treeMapper.selectById(req.getTreeId());
        if (tree == null) {
            throw new BizException(404, "古树档案不存在");
        }
        String level = grade(tree, req);
        Long cnt = patrolMapper.selectCount(null);
        TreePatrol p = new TreePatrol();
        p.setPatrolCode("XJ" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%03d", (cnt == null ? 0 : cnt) + 1));
        p.setTreeId(req.getTreeId());
        p.setPlanId(req.getPlanId());
        p.setPatrolUserId(UserContext.userId());
        p.setPatrolUserName(UserContext.realName());
        p.setPatrolTime(req.getPatrolTime() == null ? LocalDateTime.now() : req.getPatrolTime());
        p.setTrunkHollow(req.getTrunkHollow());
        p.setLeanDegree(req.getLeanDegree());
        p.setDeadBranch(req.getDeadBranch());
        p.setCrownDeviation(req.getCrownDeviation());
        p.setPestDesc(req.getPestDesc());
        p.setSoilDesc(req.getSoilDesc());
        p.setSupportFacility(req.getSupportFacility());
        p.setLightningFacility(req.getLightningFacility());
        p.setPhotos(req.getPhotos());
        p.setIssueLevel(level);
        p.setResult(req.getResult());
        p.setStatus("已提交");
        p.setCreateTime(LocalDateTime.now());
        patrolMapper.insert(p);

        // 回写最近巡查日期
        AncientTree upd = new AncientTree();
        upd.setId(tree.getId());
        upd.setLastPatrolDate(p.getPatrolTime().toLocalDate());
        upd.setUpdateTime(LocalDateTime.now());
        treeMapper.updateById(upd);

        // 生成问题，并联动工单
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patrolId", p.getId());
        result.put("patrolCode", p.getPatrolCode());
        result.put("issueLevel", level);
        if (!"日常养护".equals(level)) {
            TreeIssue issue = new TreeIssue();
            Long icnt = issueMapper.selectCount(null);
            issue.setIssueCode("WT" + LocalDate.now().getYear() + String.format("%04d", (icnt == null ? 0 : icnt) + 1));
            issue.setTreeId(tree.getId());
            issue.setPatrolId(p.getId());
            issue.setIssueType(detectIssueType(req));
            issue.setSeverity("紧急抢救".equals(level) ? "严重" : "一般");
            issue.setIssueLevel(level);
            issue.setDescription(buildIssueDesc(req));
            issue.setStatus("待处理");
            issue.setCreateTime(LocalDateTime.now());
            issueMapper.insert(issue);
            result.put("issueId", issue.getId());
            result.put("issueCode", issue.getIssueCode());

            // 紧急抢救：允许先下达临时措施，工单直接进入执行中，但必须在 24 小时内补全方案与审批
            if ("紧急抢救".equals(level)) {
                CareOrder order = new CareOrder();
                Long ocnt = orderMapper.selectCount(null);
                order.setOrderCode("YH" + LocalDate.now().getYear() + String.format("%04d", (ocnt == null ? 0 : ocnt) + 1));
                order.setTreeId(tree.getId());
                order.setIssueId(issue.getId());
                order.setCareType("复壮");
                order.setPlanContent("紧急抢救：先行支撑加固、树洞临时封堵，24 小时内补全方案");
                order.setExecutorOrg(tree.getManagerOrg());
                order.setExecutorName(UserContext.realName());
                order.setStatus("执行中");
                order.setUrgency("紧急");
                order.setDeadline(LocalDate.now().plusDays(3));
                order.setProgress(10);
                order.setTempMeasure("临时支撑 + 树洞封堵 + 设置警戒围栏");
                order.setTempMeasureDeadline(LocalDateTime.now().plusHours(24));
                order.setCreateTime(LocalDateTime.now());
                order.setUpdateTime(LocalDateTime.now());
                orderMapper.insert(order);

                TreeIssue iu = new TreeIssue();
                iu.setId(issue.getId());
                iu.setStatus("已派单");
                iu.setHandleOrderId(order.getId());
                issueMapper.updateById(iu);

                p.setStatus("已生成工单");
                patrolMapper.updateById(p);
                result.put("orderId", order.getId());
                result.put("orderCode", order.getOrderCode());
                result.put("tempMeasureDeadline", order.getTempMeasureDeadline());

                SysMessage msg = new SysMessage();
                msg.setTitle("紧急抢救待补全方案");
                msg.setContent("古树 " + tree.getTreeCode() + " 触发紧急抢救，工单 " + order.getOrderCode()
                        + " 须在 24 小时内补全方案与审批");
                msg.setMsgType("即将超时");
                msg.setBizNo(order.getOrderCode());
                msg.setLinkUrl("/care/order/" + order.getId());
                msg.setReceiverId(1L);
                msg.setIsRead(0);
                msg.setCreateTime(LocalDateTime.now());
                messageMapper.insert(msg);
            }
        }
        return result;
    }

    /** 问题分级：症状 + 保护等级 + 安全影响三维判断 */
    private String grade(AncientTree tree, TreePatrol r) {
        boolean hollow = has(r.getTrunkHollow(), "空洞", "腐烂", "开裂");
        boolean lean = has(r.getLeanDegree(), "严重", "明显");
        boolean lightning = has(r.getLightningFacility(), "失效", "缺失");
        boolean supportBad = has(r.getSupportFacility(), "失效", "缺失", "松动");
        boolean pest = has(r.getPestDesc(), "虫", "病", "蛀", "腐");
        boolean dead = has(r.getDeadBranch(), "大量", "多处");
        boolean danger = hollow || lean || lightning;
        if (danger && ("一级".equals(tree.getProtectLevel()) || "濒危".equals(tree.getHealthLevel()))) {
            return "紧急抢救";
        }
        if (danger || pest || dead || supportBad) {
            return "重点观察";
        }
        return "日常养护";
    }

    private boolean has(String src, String... keys) {
        if (src == null) return false;
        for (String k : keys) {
            if (src.contains(k)) return true;
        }
        return false;
    }

    private String detectIssueType(TreePatrol r) {
        if (has(r.getPestDesc(), "虫", "病", "蛀")) return "病虫害";
        if (has(r.getTrunkHollow(), "空洞", "腐烂")) return "树体空洞";
        if (has(r.getLeanDegree(), "严重", "明显")) return "树体倾斜";
        if (has(r.getDeadBranch(), "枯", "大量")) return "枯枝";
        if (has(r.getSupportFacility(), "失效", "缺失")) return "支撑设施";
        if (has(r.getLightningFacility(), "失效", "缺失")) return "避雷设施";
        return "生境问题";
    }

    private String buildIssueDesc(TreePatrol r) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(r.getTrunkHollow())) parts.add("树干：" + r.getTrunkHollow());
        if (StringUtils.hasText(r.getLeanDegree())) parts.add("倾斜：" + r.getLeanDegree());
        if (StringUtils.hasText(r.getDeadBranch())) parts.add("枯枝：" + r.getDeadBranch());
        if (StringUtils.hasText(r.getCrownDeviation())) parts.add("树冠：" + r.getCrownDeviation());
        if (StringUtils.hasText(r.getPestDesc())) parts.add("病虫害：" + r.getPestDesc());
        if (StringUtils.hasText(r.getSoilDesc())) parts.add("土壤：" + r.getSoilDesc());
        return String.join("；", parts);
    }

    public List<Map<String, Object>> issuesOfTree(Long treeId) {
        List<TreeIssue> list = issueMapper.selectList(new LambdaQueryWrapper<TreeIssue>()
                .eq(TreeIssue::getTreeId, treeId).orderByDesc(TreeIssue::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TreeIssue i : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("issueCode", i.getIssueCode());
            m.put("issueType", i.getIssueType());
            m.put("severity", i.getSeverity());
            m.put("issueLevel", i.getIssueLevel());
            m.put("description", i.getDescription());
            m.put("status", i.getStatus());
            m.put("handleOrderId", i.getHandleOrderId());
            m.put("createTime", i.getCreateTime());
            rows.add(m);
        }
        return rows;
    }

    public PageResult<Map<String, Object>> issuePage(int page, int size, String issueLevel, String status) {
        LambdaQueryWrapper<TreeIssue> q = new LambdaQueryWrapper<>();
        q.eq(StringUtils.hasText(issueLevel), TreeIssue::getIssueLevel, issueLevel)
         .eq(StringUtils.hasText(status), TreeIssue::getStatus, status)
         .orderByDesc(TreeIssue::getId);
        Page<TreeIssue> p = issueMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TreeIssue i : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("issueCode", i.getIssueCode());
            m.put("treeId", i.getTreeId());
            AncientTree t = treeMapper.selectById(i.getTreeId());
            m.put("treeCode", t == null ? "-" : t.getTreeCode());
            m.put("species", t == null ? "-" : t.getSpecies());
            m.put("protectLevel", t == null ? "-" : t.getProtectLevel());
            m.put("issueType", i.getIssueType());
            m.put("severity", i.getSeverity());
            m.put("issueLevel", i.getIssueLevel());
            m.put("description", i.getDescription());
            m.put("status", i.getStatus());
            m.put("handleOrderId", i.getHandleOrderId());
            m.put("createTime", i.getCreateTime());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }
}
