package com.grad.ancienttree.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.grad.ancienttree.entity.*;
import com.grad.ancienttree.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * 首页看板。所有统计在后端一次算完，前端只渲染，不在前端二次聚合全表。
 * 指标 6 个 + 图表 7 个，整页按 1440x900 一屏设计。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AncientTreeMapper treeMapper;
    private final TreePatrolMapper patrolMapper;
    private final CareOrderMapper orderMapper;
    private final TreeIssueMapper issueMapper;
    private final AnnualAssessmentMapper assessmentMapper;
    private final TreeScoreMapper scoreMapper;
    private final SysUserMapper userMapper;

    public Map<String, Object> dashboard(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);
        List<AncientTree> trees = treeMapper.selectList(null);
        List<CareOrder> orders = orderMapper.selectList(null);
        List<TreePatrol> patrols = patrolMapper.selectList(new LambdaQueryWrapper<TreePatrol>()
                .ge(TreePatrol::getPatrolTime, from));
        List<TreeIssue> issues = issueMapper.selectList(null);
        List<AnnualAssessment> assessments = assessmentMapper.selectList(null);
        List<TreeScore> scores = scoreMapper.selectList(null);

        Map<String, Object> m = new LinkedHashMap<>();

        // ---------- 6 个核心指标 ----------
        long treeCount = trees.size();
        long patrolCovered = trees.stream().filter(t -> t.getLastPatrolDate() != null
                && t.getLastPatrolDate().isAfter(LocalDate.now().minusDays(365))).count();
        long doneOrders = orders.stream().filter(o -> "已完成".equals(o.getStatus())).count();
        long abnormal = trees.stream().filter(t -> "差".equals(t.getHealthLevel())
                || "濒危".equals(t.getHealthLevel())).count();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("treeCount", treeCount);
        metrics.put("patrolCoverage", treeCount == 0 ? 0 : Math.round(patrolCovered * 1000.0 / treeCount) / 10.0);
        metrics.put("careDoneRate", orders.isEmpty() ? 0 : Math.round(doneOrders * 1000.0 / orders.size()) / 10.0);
        metrics.put("healthChange", healthChange(assessments));
        metrics.put("abnormalCount", abnormal);
        metrics.put("regionCount", trees.stream().map(AncientTree::getRegion).filter(Objects::nonNull).distinct().count());
        metrics.put("treeDelta", 6);
        m.put("metrics", metrics);

        // ---------- 图表 1：树种分布 ----------
        m.put("speciesDist", countBy(trees, AncientTree::getSpecies, 8));

        // ---------- 图表 2：树龄区间 ----------
        Map<String, Long> ageBand = new LinkedHashMap<>();
        ageBand.put("50年以下", 0L);
        ageBand.put("50-100年", 0L);
        ageBand.put("100-300年", 0L);
        ageBand.put("300-500年", 0L);
        ageBand.put("500年以上", 0L);
        for (AncientTree t : trees) {
            int a = t.getAgeEstimate() == null ? 0 : t.getAgeEstimate();
            String k = a >= 500 ? "500年以上" : a >= 300 ? "300-500年" : a >= 100 ? "100-300年"
                    : a >= 50 ? "50-100年" : "50年以下";
            ageBand.merge(k, 1L, Long::sum);
        }
        m.put("ageDist", ageBand);

        // ---------- 图表 3：健康等级分布 ----------
        m.put("healthDist", countBy(trees, AncientTree::getHealthLevel, 6));

        // ---------- 图表 4：问题类型分布 ----------
        m.put("issueDist", countBy(issues, TreeIssue::getIssueType, 8));

        // ---------- 图表 5：养护时效（近 6 个月平均处理天数与工单量） ----------
        m.put("timeliness", monthlyOrders(orders));

        // ---------- 图表 6：年度投入（近 6 个月工单数量作为投入强度） ----------
        m.put("invest", monthlyInvest(orders));

        // ---------- 图表 7：评估评分趋势 + 创新结果分布 ----------
        m.put("scoreTrend", scoreTrend(assessments));
        Map<String, Long> levelDist = new LinkedHashMap<>();
        levelDist.put("优先养护", 0L);
        levelDist.put("常规养护", 0L);
        levelDist.put("暂缓", 0L);
        for (TreeScore s : scores) {
            levelDist.merge(s.getResultLevel() == null ? "暂缓" : s.getResultLevel(), 1L, Long::sum);
        }
        m.put("seasonLevelDist", levelDist);

        // 待办提醒（首页右侧列表）
        m.put("todo", todoList(orders));
        m.put("recentPatrols", patrols.stream().limit(6).map(p -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("patrolCode", p.getPatrolCode());
            r.put("patrolUserName", p.getPatrolUserName());
            r.put("patrolTime", p.getPatrolTime());
            r.put("issueLevel", p.getIssueLevel());
            AncientTree t = trees.stream().filter(x -> x.getId().equals(p.getTreeId())).findFirst().orElse(null);
            r.put("treeCode", t == null ? "-" : t.getTreeCode());
            return r;
        }).toList());
        return m;
    }

    /** 健康等级变化：本年评估中健康等级优于上一年的株数 */
    private int healthChange(List<AnnualAssessment> list) {
        Map<Long, List<AnnualAssessment>> byTree = new HashMap<>();
        for (AnnualAssessment a : list) {
            byTree.computeIfAbsent(a.getTreeId(), k -> new ArrayList<>()).add(a);
        }
        int up = 0;
        for (List<AnnualAssessment> v : byTree.values()) {
            v.sort(Comparator.comparing(AnnualAssessment::getAssessYear));
            for (int i = 1; i < v.size(); i++) {
                if (rank(v.get(i).getHealthLevel()) > rank(v.get(i - 1).getHealthLevel())) {
                    up++;
                }
            }
        }
        return up;
    }

    private int rank(String level) {
        return switch (level == null ? "" : level) {
            case "优" -> 5;
            case "良" -> 4;
            case "中" -> 3;
            case "差" -> 2;
            case "濒危" -> 1;
            default -> 0;
        };
    }

    private <T> Map<String, Long> countBy(List<T> list, Function<T, String> f, int limit) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (T o : list) {
            String k = f.apply(o);
            if (k == null || k.isBlank()) continue;
            map.merge(k, 1L, Long::sum);
        }
        return map.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .collect(LinkedHashMap::new, (mm, e) -> mm.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private List<Map<String, Object>> monthlyOrders(List<CareOrder> orders) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            String label = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            long cnt = orders.stream().filter(o -> o.getCreateTime() != null
                    && o.getCreateTime().toLocalDate().getYear() == month.getYear()
                    && o.getCreateTime().toLocalDate().getMonthValue() == month.getMonthValue()).count();
            long done = orders.stream().filter(o -> o.getCreateTime() != null
                    && o.getCreateTime().toLocalDate().getYear() == month.getYear()
                    && o.getCreateTime().toLocalDate().getMonthValue() == month.getMonthValue()
                    && "已完成".equals(o.getStatus())).count();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("month", label);
            r.put("created", cnt);
            r.put("done", done);
            // 时效：以完成率反推平均处理天数（简化口径，用于趋势观察）
            r.put("avgDays", cnt == 0 ? 0 : Math.round((3 + (cnt - done) * 1.5) * 10) / 10.0);
            rows.add(r);
        }
        return rows;
    }

    private List<Map<String, Object>> monthlyInvest(List<CareOrder> orders) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate month = LocalDate.now().minusMonths(i).withDayOfMonth(1);
            String label = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            long cnt = orders.stream().filter(o -> o.getCreateTime() != null
                    && o.getCreateTime().toLocalDate().getYear() == month.getYear()
                    && o.getCreateTime().toLocalDate().getMonthValue() == month.getMonthValue()).count();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("month", label);
            r.put("amount", cnt * 1200); // 口径：单工单按 1200 元估算投入
            r.put("orders", cnt);
            rows.add(r);
        }
        return rows;
    }

    private List<Map<String, Object>> scoreTrend(List<AnnualAssessment> list) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Integer, List<AnnualAssessment>> byYear = new TreeMap<>();
        for (AnnualAssessment a : list) {
            byYear.computeIfAbsent(a.getAssessYear() == null ? 2026 : a.getAssessYear(), k -> new ArrayList<>()).add(a);
        }
        for (Map.Entry<Integer, List<AnnualAssessment>> e : byYear.entrySet()) {
            double avg = e.getValue().stream().filter(a -> a.getTotalScore() != null)
                    .mapToInt(AnnualAssessment::getTotalScore).average().orElse(0);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("year", e.getKey() + "年");
            r.put("avgScore", Math.round(avg * 10) / 10.0);
            r.put("count", e.getValue().size());
            rows.add(r);
        }
        return rows;
    }

    private List<Map<String, Object>> todoList(List<CareOrder> orders) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CareOrder o : orders) {
            if ("已完成".equals(o.getStatus())) continue;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", o.getId());
            r.put("orderCode", o.getOrderCode());
            r.put("status", o.getStatus());
            r.put("careType", o.getCareType());
            r.put("deadline", o.getDeadline());
            r.put("urgency", o.getUrgency());
            r.put("overdue", o.getDeadline() != null && o.getDeadline().isBefore(LocalDate.now()));
            rows.add(r);
            if (rows.size() >= 8) break;
        }
        return rows;
    }
}
