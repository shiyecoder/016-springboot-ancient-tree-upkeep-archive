package com.grad.ancienttree.controller;

import com.grad.ancienttree.common.PageResult;
import com.grad.ancienttree.common.R;
import com.grad.ancienttree.entity.AnnualAssessment;
import com.grad.ancienttree.entity.TreeRule;
import com.grad.ancienttree.dto.TreeDtos;
import com.grad.ancienttree.service.AssessmentService;
import com.grad.ancienttree.service.DashboardService;
import com.grad.ancienttree.service.SeasonalPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 年度评估、季节性养护计划（创新点）与看板接口。 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnalysisController {

    private final AssessmentService assessmentService;
    private final SeasonalPlanService planService;
    private final DashboardService dashboardService;

    // -------- 年度评估 --------
    @GetMapping("/assessment/page")
    public R<PageResult<Map<String, Object>>> assessPage(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) Integer year,
                                                         @RequestParam(required = false) String healthLevel,
                                                         @RequestParam(required = false) Long treeId) {
        return R.ok(assessmentService.page(page, size, year, healthLevel, treeId));
    }

    @PostMapping("/assessment")
    public R<Map<String, Object>> createAssessment(@RequestBody AnnualAssessment req) {
        return R.ok(assessmentService.create(req));
    }

    @GetMapping("/assessment/trend/{treeId}")
    public R<List<Map<String, Object>>> trend(@PathVariable Long treeId) {
        return R.ok(assessmentService.trend(treeId));
    }

    // -------- 创新点：季节性养护计划生成 --------
    @GetMapping("/season/rule/list")
    public R<List<TreeRule>> rules() {
        return R.ok(planService.rules());
    }

    @PostMapping("/season/rule")
    public R<TreeRule> saveRule(@RequestBody TreeRule req) {
        return R.ok(planService.saveRule(req));
    }

    /** 执行计算：返回明细依据，同时落库存留痕 */
    @PostMapping("/ancient-tree/{id}/evaluate")
    public R<Map<String, Object>> evaluate(@PathVariable Long id, @RequestBody(required = false) TreeDtos.EvaluateReq req) {
        return R.ok(planService.evaluate(id, req == null ? null : req.ruleId(), req == null ? null : req.season()));
    }

    @GetMapping("/ancient-tree/{id}/scores")
    public R<List<com.grad.ancienttree.entity.TreeScore>> scores(@PathVariable Long id) {
        return R.ok(planService.scoresOfTree(id));
    }

    /** 人工调整：必须填原因，原始结果不覆盖 */
    @PostMapping("/season/score/{scoreId}/adjust")
    public R<Void> adjust(@PathVariable Long scoreId, @RequestBody TreeDtos.AdjustReq req) {
        planService.adjust(scoreId, req.level(), req.reason());
        return R.ok();
    }

    @PostMapping("/season/score/{scoreId}/adopt")
    public R<Void> adopt(@PathVariable Long scoreId, @RequestParam boolean adopted) {
        planService.adopt(scoreId, adopted);
        return R.ok();
    }

    @GetMapping("/season/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(planService.stats());
    }

    // -------- 看板 --------
    @GetMapping("/ancient-tree/dashboard")
    public R<Map<String, Object>> dashboard(@RequestParam(defaultValue = "30") int days) {
        return R.ok(dashboardService.dashboard(days));
    }
}
