package com.grad.ancienttree.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.ancienttree.common.BizException;
import com.grad.ancienttree.common.PageResult;
import com.grad.ancienttree.common.UserContext;
import com.grad.ancienttree.dto.TreeDtos;
import com.grad.ancienttree.entity.*;
import com.grad.ancienttree.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 古树名木主档服务。
 * 一树一档：编号唯一、重要字段变更走申请审核并保留历史版本、销号/死亡/迁移不物理删除。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AncientTreeService {

    private final AncientTreeMapper treeMapper;
    private final TreeVersionMapper versionMapper;
    private final TreeImageMapper imageMapper;
    private final TreePatrolMapper patrolMapper;
    private final TreeIssueMapper issueMapper;
    private final CareOrderMapper orderMapper;
    private final AnnualAssessmentMapper assessmentMapper;
    private final TreeScoreMapper scoreMapper;
    private final SysMessageMapper messageMapper;
    private final ObjectMapper json;

    /**
     * 分页查询。支持树种、保护等级、区域、生长状况、档案状态、巡查日期区间、关键字。
     * 数据范围：管理员全部；部门负责人按责任单位；巡查/养护按本人负责或本组织。
     */
    public PageResult<Map<String, Object>> page(int page, int size, String species, String protectLevel,
                                                String region, String growthStatus, String archiveStatus,
                                                String keyword, String startDate, String endDate) {
        LambdaQueryWrapper<AncientTree> q = new LambdaQueryWrapper<>();
        q.eq(StringUtils.hasText(species), AncientTree::getSpecies, species)
         .eq(StringUtils.hasText(protectLevel), AncientTree::getProtectLevel, protectLevel)
         .eq(StringUtils.hasText(region), AncientTree::getRegion, region)
         .eq(StringUtils.hasText(growthStatus), AncientTree::getGrowthStatus, growthStatus)
         .eq(StringUtils.hasText(archiveStatus), AncientTree::getArchiveStatus, archiveStatus)
         .ge(StringUtils.hasText(startDate), AncientTree::getLastPatrolDate, startDate)
         .le(StringUtils.hasText(endDate), AncientTree::getLastPatrolDate, endDate)
         .and(StringUtils.hasText(keyword), w -> w.like(AncientTree::getTreeCode, keyword)
                 .or().like(AncientTree::getLocation, keyword)
                 .or().like(AncientTree::getSpecies, keyword));
        applyScope(q);
        q.orderByDesc(AncientTree::getUpdateTime);
        Page<AncientTree> p = treeMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AncientTree t : p.getRecords()) {
            rows.add(toRow(t));
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    /** 数据范围过滤：普通业务人员只看本人负责，部门负责人看本单位 */
    private void applyScope(LambdaQueryWrapper<AncientTree> q) {
        String scope = UserContext.dataScope();
        if ("ALL".equals(scope)) {
            return;
        }
        if ("ORG".equals(scope)) {
            q.eq(AncientTree::getManagerOrg, UserContext.get() == null ? "" : orgNameOf(UserContext.get()));
        } else {
            q.and(w -> w.eq(AncientTree::getManagerUserId, UserContext.userId())
                    .or().eq(AncientTree::getCreateBy, UserContext.realName()));
        }
    }

    private String orgNameOf(UserContext.CurrentUser u) {
        return u == null || u.orgId() == null ? "" : switch (u.orgId().intValue()) {
            case 1 -> "市园林绿化管理局";
            case 2 -> "城东绿化养护所";
            case 3 -> "城西园林工程有限公司";
            default -> "古树保护研究中心";
        };
    }

    /** 详情：基础信息 + 影像 + 版本记录 + 关联工单 + 巡查 + 评估 + 时间轴 */
    public Map<String, Object> detail(Long id) {
        AncientTree t = treeMapper.selectById(id);
        if (t == null) {
            throw new BizException(404, "档案不存在或已删除");
        }
        Map<String, Object> m = toRow(t);
        m.put("images", imageMapper.selectList(new LambdaQueryWrapper<TreeImage>()
                .eq(TreeImage::getTreeId, id).orderByDesc(TreeImage::getId)));
        m.put("versions", versionMapper.selectList(new LambdaQueryWrapper<TreeVersion>()
                .eq(TreeVersion::getTreeId, id).orderByDesc(TreeVersion::getId)));
        List<CareOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<CareOrder>()
                .eq(CareOrder::getTreeId, id).orderByDesc(CareOrder::getId));
        m.put("orders", orders);
        m.put("patrols", patrolMapper.selectList(new LambdaQueryWrapper<TreePatrol>()
                .eq(TreePatrol::getTreeId, id).orderByDesc(TreePatrol::getPatrolTime).last("limit 20")));
        m.put("issues", issueMapper.selectList(new LambdaQueryWrapper<TreeIssue>()
                .eq(TreeIssue::getTreeId, id).orderByDesc(TreeIssue::getId)));
        m.put("assessments", assessmentMapper.selectList(new LambdaQueryWrapper<AnnualAssessment>()
                .eq(AnnualAssessment::getTreeId, id).orderByDesc(AnnualAssessment::getAssessYear)));
        m.put("scores", scoreMapper.selectList(new LambdaQueryWrapper<TreeScore>()
                .eq(TreeScore::getTreeId, id).orderByDesc(TreeScore::getId).last("limit 10")));
        m.put("timeline", timeline(t, orders));
        return m;
    }

    /** 时间轴：创建、变更、审批、执行、验收等事件不可由普通用户修改 */
    private List<Map<String, Object>> timeline(AncientTree t, List<CareOrder> orders) {
        List<Map<String, Object>> tl = new ArrayList<>();
        tl.add(event(t.getCreateTime(), "建档", "档案创建，编号 " + t.getTreeCode(), t.getCreateBy()));
        for (TreeVersion v : versionMapper.selectList(new LambdaQueryWrapper<TreeVersion>()
                .eq(TreeVersion::getTreeId, t.getId()).orderByAsc(TreeVersion::getApplyTime))) {
            tl.add(event(v.getApplyTime(), "变更申请", v.getChangeType() + "：" + v.getReason(), v.getApplicantName()));
            if (v.getAuditTime() != null) {
                tl.add(event(v.getAuditTime(), "变更审核", v.getAuditStatus() + " " + nvl(v.getAuditRemark()), v.getAuditorName()));
            }
        }
        for (CareOrder o : orders) {
            tl.add(event(o.getCreateTime(), "养护派单", o.getCareType() + " 工单 " + o.getOrderCode(), o.getExecutorName()));
            if ("已完成".equals(o.getStatus())) {
                tl.add(event(o.getUpdateTime(), "验收通过", "工单 " + o.getOrderCode() + " 完成", o.getAcceptorName()));
            }
        }
        tl.sort((a, b) -> String.valueOf(b.get("time")).compareTo(String.valueOf(a.get("time"))));
        return tl;
    }

    private Map<String, Object> event(LocalDateTime time, String type, String content, String operator) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("time", time);
        e.put("type", type);
        e.put("content", content);
        e.put("operator", operator == null ? "系统" : operator);
        return e;
    }

    @Transactional
    public Long create(TreeDtos.TreeReq req) {
        // 编号唯一：同业务范围内不允许重复，保存前去掉首尾空格并限制 50 字符
        String code = StringUtils.hasText(req.treeCode()) ? req.treeCode().trim() : nextCode();
        if (code.length() > 50) {
            throw new BizException("档案编号不能超过 50 个字符");
        }
        Long dup = treeMapper.selectCount(new LambdaQueryWrapper<AncientTree>().eq(AncientTree::getTreeCode, code));
        if (dup != null && dup > 0) {
            throw new BizException("档案编号已存在：" + code);
        }
        AncientTree t = new AncientTree();
        apply(t, req);
        t.setTreeCode(code);
        t.setArchiveStatus("正常");
        t.setAuditStatus("待审核");
        t.setVersion(1);
        t.setCreateBy(UserContext.realName());
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        t.setDeleted(0);
        treeMapper.insert(t);
        return t.getId();
    }

    /** 编号由后端生成：GS + 年份 + 4 位流水，避免前端手填冲突 */
    private String nextCode() {
        Long total = treeMapper.selectCount(null);
        return "GS" + LocalDate.now().getYear() + String.format("%04d", (total == null ? 0 : total) + 1);
    }

    @Transactional
    public void update(Long id, TreeDtos.TreeReq req) {
        AncientTree old = treeMapper.selectById(id);
        if (old == null) {
            throw new BizException(404, "档案不存在");
        }
        // 乐观锁：版本号不一致说明他人已修改，提示刷新，不允许覆盖
        if (req.version() != null && old.getVersion() != null && !req.version().equals(old.getVersion())) {
            throw new BizException(409, "该档案已被他人更新，请刷新后重试");
        }
        AncientTree t = new AncientTree();
        t.setId(id);
        apply(t, req);
        t.setVersion((old.getVersion() == null ? 1 : old.getVersion()) + 1);
        t.setUpdateTime(LocalDateTime.now());
        treeMapper.updateById(t);
    }

    private void apply(AncientTree t, TreeDtos.TreeReq req) {
        t.setSpecies(req.species() == null ? null : req.species().trim());
        t.setAgeEstimate(req.ageEstimate());
        t.setLocation(req.location());
        t.setOwnership(req.ownership());
        t.setProtectLevel(req.protectLevel());
        t.setTreeHeight(req.treeHeight());
        t.setDiameter(req.diameter());
        t.setCrownWidth(req.crownWidth());
        t.setHealthLevel(req.healthLevel());
        t.setGrowthStatus(req.growthStatus());
        t.setStory(req.story());
        t.setRegion(req.region());
        t.setLongitude(req.longitude());
        t.setLatitude(req.latitude());
        t.setCoverImage(req.coverImage());
        t.setManagerOrg(req.managerOrg());
        t.setManagerUserId(req.managerUserId());
        t.setLastPatrolDate(req.lastPatrolDate());
    }

    /**
     * 重要信息变更申请：保护级别、责任单位、树体状态。
     * 先落 tree_version 待审核，审核通过后才改主档，原档案不删除。
     */
    @Transactional
    public Long applyChange(Long id, TreeDtos.ChangeReq req) {
        AncientTree t = treeMapper.selectById(id);
        if (t == null) {
            throw new BizException(404, "档案不存在");
        }
        if (req.reason() == null || req.reason().trim().length() < 5) {
            throw new BizException("申请原因不少于 5 个字");
        }
        TreeVersion v = new TreeVersion();
        v.setTreeId(id);
        Long cnt = versionMapper.selectCount(new LambdaQueryWrapper<TreeVersion>().eq(TreeVersion::getTreeId, id));
        v.setVersionNo((cnt == null ? 0 : cnt.intValue()) + 1);
        v.setChangeType(req.changeType());
        v.setBeforeJson(snapshot(t));
        v.setAfterJson(req.afterJson());
        v.setReason(req.reason().trim());
        v.setApplicantId(UserContext.userId());
        v.setApplicantName(UserContext.realName());
        v.setApplyTime(LocalDateTime.now());
        v.setAuditStatus("待审核");
        versionMapper.insert(v);
        pushMessage("待审批", "古树 " + t.getTreeCode() + " 提交" + req.changeType() + "变更申请", t.getTreeCode(), null);
        return v.getId();
    }

    private String snapshot(AncientTree t) {
        try {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("protectLevel", t.getProtectLevel());
            m.put("managerOrg", t.getManagerOrg());
            m.put("growthStatus", t.getGrowthStatus());
            m.put("healthLevel", t.getHealthLevel());
            m.put("archiveStatus", t.getArchiveStatus());
            return json.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 变更审核：通过后同步主档并递增版本号；驳回保留申请记录 */
    @Transactional
    public void auditChange(Long versionId, boolean pass, String remark) {
        TreeVersion v = versionMapper.selectById(versionId);
        if (v == null) {
            throw new BizException(404, "变更申请不存在");
        }
        if (!"待审核".equals(v.getAuditStatus())) {
            throw new BizException("该申请已审核，不能重复审批");
        }
        // 权限分离：申请人不能自己审自己的申请
        if (Objects.equals(v.getApplicantId(), UserContext.userId())) {
            throw new BizException("申请人与审核人不得为同一人");
        }
        TreeVersion upd = new TreeVersion();
        upd.setId(versionId);
        upd.setAuditStatus(pass ? "已通过" : "已驳回");
        upd.setAuditRemark(remark);
        upd.setAuditorId(UserContext.userId());
        upd.setAuditorName(UserContext.realName());
        upd.setAuditTime(LocalDateTime.now());
        versionMapper.updateById(upd);
        if (pass && StringUtils.hasText(v.getAfterJson())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> after = json.readValue(v.getAfterJson(), Map.class);
                AncientTree t = treeMapper.selectById(v.getTreeId());
                AncientTree u = new AncientTree();
                u.setId(v.getTreeId());
                if (after.get("protectLevel") != null) u.setProtectLevel(String.valueOf(after.get("protectLevel")));
                if (after.get("managerOrg") != null) u.setManagerOrg(String.valueOf(after.get("managerOrg")));
                if (after.get("growthStatus") != null) u.setGrowthStatus(String.valueOf(after.get("growthStatus")));
                if (after.get("healthLevel") != null) u.setHealthLevel(String.valueOf(after.get("healthLevel")));
                if (after.get("archiveStatus") != null) u.setArchiveStatus(String.valueOf(after.get("archiveStatus")));
                u.setVersion((t.getVersion() == null ? 1 : t.getVersion()) + 1);
                u.setUpdateTime(LocalDateTime.now());
                treeMapper.updateById(u);
            } catch (Exception e) {
                log.warn("变更快照解析失败：{}", e.getMessage());
            }
        }
    }

    /**
     * 档案状态流转：正常→已迁移/已死亡/已销号 均需专项审批。
     * 这里只做申请入口，实际生效仍走 auditChange，保证留痕。
     */
    @Transactional
    public void transition(Long id, TreeDtos.TransitionReq req) {
        AncientTree t = treeMapper.selectById(id);
        if (t == null) {
            throw new BizException(404, "档案不存在");
        }
        List<String> allowed = List.of("正常", "已迁移", "已死亡", "已销号");
        if (!allowed.contains(req.target())) {
            throw new BizException("不支持的目标状态：" + req.target());
        }
        if (!"正常".equals(t.getArchiveStatus()) && "正常".equals(req.target())) {
            throw new BizException("已归档的档案不能直接恢复，请走变更审批");
        }
        if (req.reason() == null || req.reason().trim().length() < 5) {
            throw new BizException("流转原因不少于 5 个字");
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("archiveStatus", req.target());
        TreeDtos.ChangeReq cr = new TreeDtos.ChangeReq("树体状态", req.reason(), toJson(after));
        applyChange(id, cr);
    }

    private String toJson(Map<String, Object> m) {
        try {
            return json.writeValueAsString(m);
        } catch (Exception e) {
            return "{}";
        }
    }

    /** 逻辑删除：被业务单据引用时禁止删除 */
    @Transactional
    public void remove(Long id) {
        Long orders = orderMapper.selectCount(new LambdaQueryWrapper<CareOrder>().eq(CareOrder::getTreeId, id));
        Long patrols = patrolMapper.selectCount(new LambdaQueryWrapper<TreePatrol>().eq(TreePatrol::getTreeId, id));
        if ((orders != null && orders > 0) || (patrols != null && patrols > 0)) {
            throw new BizException("该档案已有巡查或养护记录，不能删除，请使用销号流程");
        }
        treeMapper.deleteById(id);
    }

    /** 列表行 VO：字段与前端表格列一一对应 */
    public Map<String, Object> toRow(AncientTree t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("treeCode", t.getTreeCode());
        m.put("species", t.getSpecies());
        m.put("ageEstimate", t.getAgeEstimate());
        m.put("location", t.getLocation());
        m.put("ownership", t.getOwnership());
        m.put("protectLevel", t.getProtectLevel());
        m.put("treeHeight", t.getTreeHeight());
        m.put("diameter", t.getDiameter());
        m.put("crownWidth", t.getCrownWidth());
        m.put("healthLevel", t.getHealthLevel());
        m.put("growthStatus", t.getGrowthStatus());
        m.put("story", t.getStory());
        m.put("region", t.getRegion());
        m.put("longitude", t.getLongitude());
        m.put("latitude", t.getLatitude());
        m.put("coverImage", t.getCoverImage());
        m.put("managerOrg", t.getManagerOrg());
        m.put("managerUserId", t.getManagerUserId());
        m.put("lastPatrolDate", t.getLastPatrolDate() == null ? null
                : t.getLastPatrolDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        m.put("archiveStatus", t.getArchiveStatus());
        m.put("auditStatus", t.getAuditStatus());
        m.put("version", t.getVersion());
        m.put("createBy", t.getCreateBy());
        m.put("createTime", t.getCreateTime());
        m.put("updateTime", t.getUpdateTime());
        return m;
    }

    private void pushMessage(String type, String content, String bizNo, String link) {
        SysMessage msg = new SysMessage();
        msg.setTitle(type);
        msg.setContent(content);
        msg.setMsgType(type);
        msg.setBizNo(bizNo);
        msg.setLinkUrl(link == null ? "/tree/list" : link);
        msg.setReceiverId(1L);
        msg.setIsRead(0);
        msg.setCreateTime(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    /** 公开页面只展示已审核档案摘要，不含责任人联系方式与精确坐标 */
    public List<Map<String, Object>> publicList(String species, String region) {
        List<AncientTree> list = treeMapper.selectList(new LambdaQueryWrapper<AncientTree>()
                .eq(AncientTree::getAuditStatus, "已通过")
                .eq(StringUtils.hasText(species), AncientTree::getSpecies, species)
                .eq(StringUtils.hasText(region), AncientTree::getRegion, region)
                .orderByDesc(AncientTree::getAgeEstimate).last("limit 60"));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AncientTree t : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("treeCode", t.getTreeCode());
            m.put("species", t.getSpecies());
            m.put("ageEstimate", t.getAgeEstimate());
            m.put("protectLevel", t.getProtectLevel());
            m.put("region", t.getRegion());
            m.put("location", t.getLocation());
            m.put("story", t.getStory());
            m.put("coverImage", t.getCoverImage());
            m.put("healthLevel", t.getHealthLevel());
            List<TreeImage> imgs = imageMapper.selectList(new LambdaQueryWrapper<TreeImage>()
                    .eq(TreeImage::getTreeId, t.getId()).last("limit 3"));
            m.put("images", imgs);
            rows.add(m);
        }
        return rows;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
