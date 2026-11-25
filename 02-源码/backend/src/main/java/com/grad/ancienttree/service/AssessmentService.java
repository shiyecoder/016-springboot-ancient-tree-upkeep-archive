package com.grad.ancienttree.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.ancienttree.common.BizException;
import com.grad.ancienttree.common.PageResult;
import com.grad.ancienttree.common.UserContext;
import com.grad.ancienttree.entity.AncientTree;
import com.grad.ancienttree.entity.AnnualAssessment;
import com.grad.ancienttree.mapper.AncientTreeMapper;
import com.grad.ancienttree.mapper.AnnualAssessmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 年度评估：按生长势、结构安全、病虫害、生境四项评分，加权生成综合分与健康等级，并给出下年建议。
 * 权重固定为 30/30/20/20，保证同一输入得到同一结果，评估口径可复核。
 */
@Service
@RequiredArgsConstructor
public class AssessmentService {

    private static final int W_GROWTH = 30;
    private static final int W_STRUCTURE = 30;
    private static final int W_PEST = 20;
    private static final int W_HABITAT = 20;

    private final AnnualAssessmentMapper assessmentMapper;
    private final AncientTreeMapper treeMapper;

    public PageResult<Map<String, Object>> page(int page, int size, Integer year, String healthLevel, Long treeId) {
        LambdaQueryWrapper<AnnualAssessment> q = new LambdaQueryWrapper<>();
        q.eq(year != null, AnnualAssessment::getAssessYear, year)
         .eq(healthLevel != null && !healthLevel.isBlank(), AnnualAssessment::getHealthLevel, healthLevel)
         .eq(treeId != null, AnnualAssessment::getTreeId, treeId)
         .orderByDesc(AnnualAssessment::getAssessYear).orderByDesc(AnnualAssessment::getId);
        Page<AnnualAssessment> p = assessmentMapper.selectPage(new Page<>(page, size), q);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AnnualAssessment a : p.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("treeId", a.getTreeId());
            AncientTree t = treeMapper.selectById(a.getTreeId());
            m.put("treeCode", t == null ? "-" : t.getTreeCode());
            m.put("species", t == null ? "-" : t.getSpecies());
            m.put("protectLevel", t == null ? "-" : t.getProtectLevel());
            m.put("region", t == null ? "-" : t.getRegion());
            m.put("assessYear", a.getAssessYear());
            m.put("growthScore", a.getGrowthScore());
            m.put("structureScore", a.getStructureScore());
            m.put("pestScore", a.getPestScore());
            m.put("habitatScore", a.getHabitatScore());
            m.put("totalScore", a.getTotalScore());
            m.put("healthLevel", a.getHealthLevel());
            m.put("suggestion", a.getSuggestion());
            m.put("assessorName", a.getAssessorName());
            m.put("status", a.getStatus());
            m.put("createTime", a.getCreateTime());
            rows.add(m);
        }
        return PageResult.of(p.getTotal(), page, size, rows);
    }

    @Transactional
    public Map<String, Object> create(AnnualAssessment req) {
        AncientTree t = treeMapper.selectById(req.getTreeId());
        if (t == null) {
            throw new BizException(404, "古树档案不存在");
        }
        check(req.getGrowthScore(), "生长势");
        check(req.getStructureScore(), "结构安全");
        check(req.getPestScore(), "病虫害");
        check(req.getHabitatScore(), "生境");
        int year = req.getAssessYear() == null ? LocalDateTime.now().getYear() : req.getAssessYear();
        Long dup = assessmentMapper.selectCount(new LambdaQueryWrapper<AnnualAssessment>()
                .eq(AnnualAssessment::getTreeId, req.getTreeId())
                .eq(AnnualAssessment::getAssessYear, year));
        if (dup != null && dup > 0) {
            throw new BizException("该古树 " + year + " 年度已有评估记录，请勿重复提交");
        }
        int total = (req.getGrowthScore() * W_GROWTH + req.getStructureScore() * W_STRUCTURE
                + req.getPestScore() * W_PEST + req.getHabitatScore() * W_HABITAT) / 100;
        String level = levelOf(total);

        AnnualAssessment a = new AnnualAssessment();
        a.setTreeId(req.getTreeId());
        a.setAssessYear(year);
        a.setGrowthScore(req.getGrowthScore());
        a.setStructureScore(req.getStructureScore());
        a.setPestScore(req.getPestScore());
        a.setHabitatScore(req.getHabitatScore());
        a.setTotalScore(total);
        a.setHealthLevel(level);
        a.setSuggestion(req.getSuggestion() == null ? defaultSuggestion(level, req) : req.getSuggestion());
        a.setAssessorId(UserContext.userId());
        a.setAssessorName(UserContext.realName());
        a.setStatus("已提交");
        a.setCreateTime(LocalDateTime.now());
        assessmentMapper.insert(a);

        // 回写主档健康等级，列表与看板随之更新
        AncientTree u = new AncientTree();
        u.setId(t.getId());
        u.setHealthLevel(level);
        u.setUpdateTime(LocalDateTime.now());
        treeMapper.updateById(u);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", a.getId());
        r.put("totalScore", total);
        r.put("healthLevel", level);
        r.put("suggestion", a.getSuggestion());
        r.put("weights", Map.of("生长势", W_GROWTH, "结构安全", W_STRUCTURE, "病虫害", W_PEST, "生境", W_HABITAT));
        return r;
    }

    private void check(Integer v, String name) {
        if (v == null || v < 0 || v > 100) {
            throw new BizException(name + "得分必须在 0-100 之间");
        }
    }

    /** 综合分 → 健康等级（同一分档全系统一致，便于统计） */
    private String levelOf(int total) {
        if (total >= 85) return "优";
        if (total >= 70) return "良";
        if (total >= 55) return "中";
        if (total >= 40) return "差";
        return "濒危";
    }

    private String defaultSuggestion(String level, AnnualAssessment a) {
        return switch (level) {
            case "优" -> "维持现有养护频次，每年复评一次，做好影像对比存档。";
            case "良" -> "加强病虫害监测，秋末施基肥，建议次年重点观察树冠变化。";
            case "中" -> "安排复壮方案：改善土壤通气、修剪枯枝、补设支撑，半年复评。";
            case "差" -> "列入重点观察名单，制定复壮与防腐方案，每季度巡查一次。";
            default -> "启动紧急抢救程序，专家会诊并报主管部门备案，必要时设置围栏隔离。";
        };
    }

    /** 单株评分趋势，用于专家复核页的对照展示 */
    public List<Map<String, Object>> trend(Long treeId) {
        List<AnnualAssessment> list = assessmentMapper.selectList(new LambdaQueryWrapper<AnnualAssessment>()
                .eq(AnnualAssessment::getTreeId, treeId).orderByAsc(AnnualAssessment::getAssessYear));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AnnualAssessment a : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("year", a.getAssessYear());
            m.put("totalScore", a.getTotalScore());
            m.put("healthLevel", a.getHealthLevel());
            m.put("growthScore", a.getGrowthScore());
            m.put("structureScore", a.getStructureScore());
            m.put("pestScore", a.getPestScore());
            m.put("habitatScore", a.getHabitatScore());
            rows.add(m);
        }
        return rows;
    }
}
