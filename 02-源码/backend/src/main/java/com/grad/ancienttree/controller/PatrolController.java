package com.grad.ancienttree.controller;

import com.grad.ancienttree.common.PageResult;
import com.grad.ancienttree.common.R;
import com.grad.ancienttree.entity.PatrolPlan;
import com.grad.ancienttree.entity.TreePatrol;
import com.grad.ancienttree.service.PatrolService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 巡查计划、巡查记录与问题接口。 */
@RestController
@RequestMapping("/api/patrol")
@RequiredArgsConstructor
public class PatrolController {

    private final PatrolService service;

    // -------- 巡查计划 --------
    @GetMapping("/plan/page")
    public R<PageResult<Map<String, Object>>> planPage(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(required = false) String protectLevel,
                                                       @RequestParam(required = false) String keyword) {
        return R.ok(service.planPage(page, size, status, protectLevel, keyword));
    }

    @PostMapping("/plan")
    public R<Long> savePlan(@RequestBody PatrolPlan req) {
        return R.ok(service.savePlan(req, null));
    }

    @PutMapping("/plan/{id}")
    public R<Long> updatePlan(@PathVariable Long id, @RequestBody PatrolPlan req) {
        return R.ok(service.savePlan(req, id));
    }

    // -------- 巡查记录 --------
    @GetMapping("/record/page")
    public R<PageResult<Map<String, Object>>> patrolPage(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int size,
                                                         @RequestParam(required = false) String tab,
                                                         @RequestParam(required = false) String issueLevel,
                                                         @RequestParam(required = false) String keyword,
                                                         @RequestParam(required = false) Long treeId) {
        return R.ok(service.patrolPage(page, size, tab, issueLevel, keyword, treeId));
    }

    @PostMapping("/record")
    public R<Map<String, Object>> submit(@RequestBody TreePatrol req) {
        return R.ok(service.submitPatrol(req));
    }

    // -------- 问题 --------
    @GetMapping("/issue/page")
    public R<PageResult<Map<String, Object>>> issuePage(@RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "10") int size,
                                                        @RequestParam(required = false) String issueLevel,
                                                        @RequestParam(required = false) String status) {
        return R.ok(service.issuePage(page, size, issueLevel, status));
    }

    @GetMapping("/issue/tree/{treeId}")
    public R<List<Map<String, Object>>> issuesOfTree(@PathVariable Long treeId) {
        return R.ok(service.issuesOfTree(treeId));
    }
}
