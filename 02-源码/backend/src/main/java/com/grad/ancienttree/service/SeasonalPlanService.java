package com.grad.ancienttree.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grad.ancienttree.common.BizException;
import com.grad.ancienttree.common.UserContext;
import com.grad.ancienttree.entity.AncientTree;
import com.grad.ancienttree.entity.TreeRule;
import com.grad.ancienttree.entity.TreeScore;
import com.grad.ancienttree.mapper.AncientTreeMapper;
import com.grad.ancienttree.mapper.TreeRuleMapper;
import com.grad.ancienttree.mapper.TreeScoreMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * 创新点：季节性养护计划生成。
 *
 * 设计原则——「可解释的业务辅助工具」：
 * 1. 规则配置页维护六项权重（树种/树龄区间/保护等级/健康等级/季节/上次养护时间），合计必须 100%；
 * 2. 每次计算保存输入快照、规则版本、各项得分、总分与结果等级，结果可追溯；
 * 3. 结果页同时给出推荐结果、主要影响因素、未满足条件、可改进项，不只显示一个红黄绿标签；
 * 4. 业务负责人可人工调整，但必须填写原因，原始计算结果永不覆盖；
 * 5. 看板统计推荐采纳率与人工调整率，验证模块是否真的有用。
 *
 * 注意：分数越高代表「越需要优先安排养护」，不是健康分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeasonalPlanService {

    private final TreeRuleMapper ruleMapper;
    private final TreeScoreMapper scoreMapper;
    private final AncientTreeMapper treeMapper;
    private final ObjectMapper json;

    /** 树种稀缺系数：越稀有越优先保护 */
    private static final Map<String, Integer> SPECIES_SCORE = Map.of(
            "银杏", 95, "国槐", 90, "侧柏", 88, "香樟", 80,
            "皂荚", 78, "朴树", 72, "黄连木", 85, "枫杨", 65);

    /** 规则配置页：列出全部版本，标注启用状态 */
    public List<TreeRule> rules() {
        return ruleMapper.selectList(new LambdaQueryWrapper<TreeRule>().orderByDesc(TreeRule::getVersion));
    }

    public TreeRule activeRule() {
        TreeRule r = ruleMapper.selectOne(new LambdaQueryWrapper<TreeRule>()
                .eq(TreeRule::getEnabled, 1).orderByDesc(TreeRule::getVersion).last("limit 1"));
        if (r == null) {
            throw new BizException("尚未配置启用的养护计划规则，请先在规则配置页创建");
        }
        return r;
    }

    /**
     * 新增规则版本。权重合计必须等于 100%，否则拒绝保存——
     * 这是硬性校验，前端提示不能替代后端校验。
     */
    @Transactional
    public TreeRule saveRule(TreeRule req) {
        int sum = nz(req.getWeightSpecies()) + nz(req.getWeightAge()) + nz(req.getWeightLevel())
                + nz(req.getWeightHealth()) + nz(req.getWeightSeason()) + nz(req.getWeightLastCare());
        if (sum != 100) {
            throw new BizException("六项权重合计必须等于 100%，当前为 " + sum + "%");
        }
        TreeRule latest = ruleMapper.selectOne(new LambdaQueryWrapper<TreeRule>()
                .orderByDesc(TreeRule::getVersion).last("limit 1"));
        TreeRule r = new TreeRule();
        r.setRuleName(req.getRuleName() == null ? "季节性养护计划规则" : req.getRuleName());
        r.setVersion((latest == null ? 0 : latest.getVersion()) + 1);
        r.setWeightSpecies(nz(req.getWeightSpecies()));
        r.setWeightAge(nz(req.getWeightAge()));
        r.setWeightLevel(nz(req.getWeightLevel()));
        r.setWeightHealth(nz(req.getWeightHealth()));
        r.setWeightSeason(nz(req.getWeightSeason()));
        r.setWeightLastCare(nz(req.getWeightLastCare()));
        r.setThresholdHigh(req.getThresholdHigh() == null ? 70 : req.getThresholdHigh());
        r.setThresholdLow(req.getThresholdLow() == null ? 45 : req.getThresholdLow());
        r.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        r.setEffectiveTime(LocalDateTime.now());
        r.setCreateTime(LocalDateTime.now());
        ruleMapper.insert(r);
        if (r.getEnabled() == 1) {
            // 仅新计算使用新版本，历史结果保留原规则版本，保证可追溯
            ruleMapper.selectList(new LambdaQueryWrapper<TreeRule>()
                            .ne(TreeRule::getId, r.getId()).eq(TreeRule::getEnabled, 1))
                    .forEach(old -> {
                        TreeRule u = new TreeRule();
                        u.setId(old.getId());
                        u.setEnabled(0);
                        ruleMapper.updateById(u);
                    });
        }
        return r;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 执行一次计算并落库留痕 */
    @Transactional
    public Map<String, Object> evaluate(Long treeId, Long ruleId, String season) {
        AncientTree t = treeMapper.selectById(treeId);
        if (t == null) {
            throw new BizException(404, "档案不存在");
        }
        TreeRule rule = ruleId == null ? activeRule() : ruleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BizException("规则版本不存在");
        }
        String seasonName = season == null || season.isBlank() ? seasonOf(LocalDate.now()) : season;

        // ---------- 逐项打分 ----------
        List<Map<String, Object>> details = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        Integer sSpecies = SPECIES_SCORE.getOrDefault(t.getSpecies(), 60);
        boolean hasSpecies = t.getSpecies() != null && !t.getSpecies().isBlank();
        details.add(item("树种", rule.getWeightSpecies(), hasSpecies ? sSpecies : null,
                hasSpecies ? t.getSpecies() + " 属" + (sSpecies >= 85 ? "重点保护树种" : "常见树种") : "缺少树种信息，本项不参与计算"));
        if (!hasSpecies) missing.add("树种未填写");

        Integer sAge = null;
        if (t.getAgeEstimate() != null) {
            int a = t.getAgeEstimate();
            sAge = a >= 500 ? 100 : a >= 300 ? 90 : a >= 100 ? 75 : a >= 50 ? 60 : 40;
            details.add(item("树龄区间", rule.getWeightAge(), sAge,
                    "估测树龄 " + a + " 年，属" + ageBand(a) + "，养护紧迫度 " + sAge + " 分"));
        } else {
            details.add(item("树龄区间", rule.getWeightAge(), null, "缺少树龄，本项不参与计算"));
            missing.add("树龄未填写");
        }

        Integer sLevel = switch (nvl(t.getProtectLevel())) {
            case "一级" -> 100;
            case "二级" -> 85;
            case "三级" -> 70;
            case "后续资源" -> 55;
            default -> null;
        };
        details.add(item("保护等级", rule.getWeightLevel(), sLevel,
                sLevel == null ? "保护等级缺失或非法，本项不参与计算"
                        : "保护等级 " + t.getProtectLevel() + "，紧迫度 " + sLevel + " 分"));
        if (sLevel == null) missing.add("保护等级未填写");

        // 健康越差，越需要优先养护
        Integer sHealth = switch (nvl(t.getHealthLevel())) {
            case "濒危" -> 100;
            case "差" -> 92;
            case "中" -> 70;
            case "良" -> 45;
            case "优" -> 25;
            default -> null;
        };
        details.add(item("健康等级", rule.getWeightHealth(), sHealth,
                sHealth == null ? "健康等级缺失，本项不参与计算"
                        : "当前健康等级「" + t.getHealthLevel() + "」，越差越优先，得分 " + sHealth));
        if (sHealth == null) missing.add("健康等级未填写（建议先完成年度评估）");

        Integer sSeason = switch (seasonName) {
            case "春季" -> 90;
            case "秋季" -> 85;
            case "夏季" -> 72;
            case "冬季" -> 50;
            default -> 60;
        };
        details.add(item("季节", rule.getWeightSeason(), sSeason,
                "当前" + seasonName + "，" + ("春季".equals(seasonName) ? "适宜修剪、施肥与病虫害预防"
                        : "秋季".equals(seasonName) ? "适宜施基肥与树体加固"
                        : "夏季".equals(seasonName) ? "高温需重点补水与病虫监测" : "冬季以结构安全检查为主")
                        + "，季节系数 " + sSeason));

        Integer sLast = null;
        String lastDesc;
        if (t.getLastPatrolDate() != null) {
            long days = ChronoUnit.DAYS.between(t.getLastPatrolDate(), LocalDate.now());
            sLast = days > 365 ? 100 : days > 180 ? 80 : days > 90 ? 60 : 30;
            lastDesc = "距上次巡查养护 " + days + " 天，间隔系数 " + sLast;
        } else {
            lastDesc = "缺少上次养护/巡查时间，本项不参与计算";
        }
        details.add(item("上次养护时间", rule.getWeightLastCare(), sLast, lastDesc));
        if (sLast == null) missing.add("上次巡查时间为空");

        // ---------- 加权总分：缺失项按剩余权重归一化，避免因缺数据直接判低 ----------
        double weighted = 0;
        int weightUsed = 0;
        for (Map<String, Object> d : details) {
            Integer score = (Integer) d.get("score");
            int w = (Integer) d.get("weight");
            if (score != null) {
                weighted += score * w / 100.0;
                weightUsed += w;
            }
        }
        if (weightUsed == 0) {
            throw new BizException("关键字段全部缺失，无法计算，请先补录树种、树龄、保护等级、健康等级");
        }
        int total = (int) Math.round(weighted * 100.0 / weightUsed);

        String level = total >= nz(rule.getThresholdHigh()) ? "优先养护"
                : total >= nz(rule.getThresholdLow()) ? "常规养护" : "暂缓";
        List<String> tasks = recommendTasks(t, seasonName, total, level);
        String explanation = buildExplanation(t, rule, total, level, weightUsed, missing, details);

        TreeScore sc = new TreeScore();
        sc.setTreeId(treeId);
        sc.setRuleId(rule.getId());
        sc.setRuleVersion(rule.getVersion());
        sc.setInputSnapshot(inputSnapshot(t, seasonName));
        sc.setDetailJson(writeJson(details));
        sc.setTotalScore(total);
        sc.setResultLevel(level);
        sc.setRecommendTasks(String.join("；", tasks));
        sc.setExplanation(explanation);
        sc.setUnmetJson(writeJson(missing));
        sc.setCreateTime(LocalDateTime.now());
        scoreMapper.insert(sc);

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("scoreId", sc.getId());
        r.put("treeId", treeId);
        r.put("treeCode", t.getTreeCode());
        r.put("ruleVersion", rule.getVersion());
        r.put("season", seasonName);
        r.put("totalScore", total);
        r.put("resultLevel", level);
        r.put("details", details);
        r.put("weightUsed", weightUsed);
        r.put("missing", missing);
        r.put("tasks", tasks);
        r.put("explanation", explanation);
        r.put("computeTime", sc.getCreateTime());
        return r;
    }

    private Map<String, Object> item(String factor, Integer weight, Integer score, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("factor", factor);
        m.put("weight", weight == null ? 0 : weight);
        m.put("score", score);
        m.put("desc", desc);
        m.put("contribution", score == null ? 0 : (int) Math.round(score * (weight == null ? 0 : weight) / 100.0));
        return m;
    }

    private String ageBand(int a) {
        return a >= 500 ? "千年古树重点保护段" : a >= 300 ? "300 年以上一级古树段"
                : a >= 100 ? "100-300 年二级古树段" : a >= 50 ? "50-100 年后备资源段" : "50 年以下一般树木段";
    }

    /** 推荐任务不是凭空生成，每条都能回溯到某个输入因素 */
    private List<String> recommendTasks(AncientTree t, String season, int total, String level) {
        List<String> tasks = new ArrayList<>();
        if ("差".equals(t.getHealthLevel()) || "濒危".equals(t.getHealthLevel())) {
            tasks.add("安排复壮与树体支撑加固（健康等级「" + t.getHealthLevel() + "」触发）");
        }
        if ("春季".equals(season)) {
            tasks.add("春季修剪与施肥，配合病虫害预防性喷药（季节规则触发）");
        } else if ("秋季".equals(season)) {
            tasks.add("秋季施基肥并检查支撑设施（季节规则触发）");
        } else if ("夏季".equals(season)) {
            tasks.add("夏季抗旱补水与蛀干害虫监测（季节规则触发）");
        } else {
            tasks.add("冬季树体结构安全排查与防冻处理（季节规则触发）");
        }
        if ("一级".equals(t.getProtectLevel())) {
            tasks.add("一级古树季度专项巡查，形成影像对比档案（保护等级触发）");
        }
        if (t.getAgeEstimate() != null && t.getAgeEstimate() >= 300) {
            tasks.add("对树洞与腐朽部位做防腐处理（树龄≥300 年触发）");
        }
        if (t.getLastPatrolDate() == null
                || ChronoUnit.DAYS.between(t.getLastPatrolDate(), LocalDate.now()) > 365) {
            tasks.add("超一年未巡查，立即安排全面体检（上次养护时间触发）");
        }
        if (tasks.size() < 2) {
            tasks.add("常规养护：清理树盘杂物、检查避雷与支撑设施（保底任务）");
        }
        if ("暂缓".equals(level)) {
            tasks.add(0, "综合得分偏低，本季度暂缓专项养护，仅保留日常观察");
        }
        return tasks;
    }

    private String buildExplanation(AncientTree t, TreeRule rule, int total, String level,
                                    int weightUsed, List<String> missing, List<Map<String, Object>> details) {
        // 找出贡献最大的两项作为「主要影响因素」
        List<Map<String, Object>> sorted = new ArrayList<>(details);
        sorted.removeIf(d -> d.get("score") == null);
        sorted.sort((a, b) -> ((Integer) b.get("contribution")).compareTo((Integer) a.get("contribution")));
        StringBuilder sb = new StringBuilder();
        sb.append("古树 ").append(t.getTreeCode()).append("（").append(t.getSpecies()).append("）");
        sb.append("按规则 v").append(rule.getVersion()).append(" 计算，加权总分 ").append(total)
          .append("，判定为「").append(level).append("」。");
        if (!sorted.isEmpty()) {
            sb.append("主要影响因素：");
            for (int i = 0; i < Math.min(2, sorted.size()); i++) {
                Map<String, Object> d = sorted.get(i);
                sb.append(d.get("factor")).append("（权重 ").append(d.get("weight"))
                  .append("%，得分 ").append(d.get("score")).append("）");
                if (i == 0) sb.append("、");
            }
            sb.append("。");
        }
        if (weightUsed < 100) {
            sb.append("本次有 ").append(100 - weightUsed).append("% 权重因字段缺失未参与，总分已按剩余权重归一化，")
              .append("补全数据后结论可能变化。");
        }
        if (!missing.isEmpty()) {
            sb.append("未满足条件：").append(String.join("、", missing)).append("。");
        }
        sb.append("可改进项：补全健康评估与巡查记录可提升判断准确性。");
        return sb.toString();
    }

    private String inputSnapshot(AncientTree t, String season) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("species", t.getSpecies());
        m.put("ageEstimate", t.getAgeEstimate());
        m.put("protectLevel", t.getProtectLevel());
        m.put("healthLevel", t.getHealthLevel());
        m.put("season", season);
        m.put("lastPatrolDate", t.getLastPatrolDate());
        return writeJson(m);
    }

    /**
     * 人工调整：只写 manualLevel / manualReason，绝不覆盖 resultLevel 与 totalScore。
     * 这样采纳率、调整率统计才有意义，也满足「原始结果可追溯」。
     */
    @Transactional
    public void adjust(Long scoreId, String level, String reason) {
        TreeScore sc = scoreMapper.selectById(scoreId);
        if (sc == null) {
            throw new BizException(404, "计算记录不存在");
        }
        if (reason == null || reason.trim().length() < 5) {
            throw new BizException("人工调整必须填写原因，且不少于 5 个字");
        }
        TreeScore u = new TreeScore();
        u.setId(scoreId);
        u.setManualLevel(level);
        u.setManualReason(reason.trim());
        u.setManualBy(UserContext.userId());
        u.setManualTime(LocalDateTime.now());
        scoreMapper.updateById(u);
    }

    /** 标记采纳：业务上是否真的执行了推荐任务 */
    @Transactional
    public void adopt(Long scoreId, boolean adopted) {
        TreeScore u = new TreeScore();
        u.setId(scoreId);
        u.setAdopted(adopted ? 1 : 0);
        scoreMapper.updateById(u);
    }

    /** 创新模块效果统计：推荐数、采纳率、人工调整率、等级分布 */
    public Map<String, Object> stats() {
        List<TreeScore> all = scoreMapper.selectList(new LambdaQueryWrapper<TreeScore>().orderByDesc(TreeScore::getId));
        long total = all.size();
        long adopted = all.stream().filter(s -> s.getAdopted() != null && s.getAdopted() == 1).count();
        long adjusted = all.stream().filter(s -> s.getManualLevel() != null).count();
        Map<String, Long> levelDist = new LinkedHashMap<>();
        levelDist.put("优先养护", 0L);
        levelDist.put("常规养护", 0L);
        levelDist.put("暂缓", 0L);
        for (TreeScore s : all) {
            levelDist.merge(s.getResultLevel() == null ? "暂缓" : s.getResultLevel(), 1L, Long::sum);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", total);
        m.put("adopted", adopted);
        m.put("adjusted", adjusted);
        m.put("adoptRate", total == 0 ? 0 : Math.round(adopted * 1000.0 / total) / 10.0);
        m.put("adjustRate", total == 0 ? 0 : Math.round(adjusted * 1000.0 / total) / 10.0);
        m.put("levelDist", levelDist);
        m.put("recent", all.stream().limit(10).toList());
        return m;
    }

    public List<TreeScore> scoresOfTree(Long treeId) {
        return scoreMapper.selectList(new LambdaQueryWrapper<TreeScore>()
                .eq(TreeScore::getTreeId, treeId).orderByDesc(TreeScore::getId));
    }

    private String seasonOf(LocalDate d) {
        int m = d.getMonthValue();
        if (m >= 3 && m <= 5) return "春季";
        if (m >= 6 && m <= 8) return "夏季";
        if (m >= 9 && m <= 11) return "秋季";
        return "冬季";
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String writeJson(Object o) {
        try {
            return json.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }
}
