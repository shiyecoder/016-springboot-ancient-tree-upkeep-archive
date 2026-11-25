package com.grad.ancienttree.controller;

import com.grad.ancienttree.common.PageResult;
import com.grad.ancienttree.common.R;
import com.grad.ancienttree.dto.TreeDtos;
import com.grad.ancienttree.service.AncientTreeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 古树名木档案接口。 */
@RestController
@RequestMapping("/api/ancient-tree")
@RequiredArgsConstructor
public class AncientTreeController {

    private final AncientTreeService service;

    /** 分页查询：服务端分页，默认每页 10 条 */
    @GetMapping("/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String species,
                                                   @RequestParam(required = false) String protectLevel,
                                                   @RequestParam(required = false) String region,
                                                   @RequestParam(required = false) String growthStatus,
                                                   @RequestParam(required = false) String archiveStatus,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        return R.ok(service.page(page, size, species, protectLevel, region, growthStatus,
                archiveStatus, keyword, startDate, endDate));
    }

    /** 详情：基础信息、附件、关联单据与时间轴 */
    @GetMapping("/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(service.detail(id));
    }

    @PostMapping
    public R<Long> create(@Valid @RequestBody TreeDtos.TreeReq req) {
        return R.ok(service.create(req));
    }

    /** 更新：校验版本号，防止覆盖他人修改 */
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody TreeDtos.TreeReq req) {
        service.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        service.remove(id);
        return R.ok();
    }

    /** 重要信息变更申请（保护级别/责任单位/树体状态），进入待审核并保留版本 */
    @PostMapping("/{id}/change")
    public R<Long> change(@PathVariable Long id, @RequestBody TreeDtos.ChangeReq req) {
        return R.ok(service.applyChange(id, req));
    }

    /** 变更审核：通过后同步主档，申请人与审核人必须不同 */
    @PostMapping("/version/{versionId}/audit")
    public R<Void> audit(@PathVariable Long versionId,
                         @RequestParam boolean pass,
                         @RequestParam(required = false) String remark) {
        service.auditChange(versionId, pass, remark);
        return R.ok();
    }

    /** 档案状态流转（迁移/死亡/销号），均走专项审批留痕 */
    @PostMapping("/{id}/transition")
    public R<Void> transition(@PathVariable Long id, @RequestBody TreeDtos.TransitionReq req) {
        service.transition(id, req);
        return R.ok();
    }

    /** 公开页面：只返回已审核档案摘要 */
    @GetMapping("/public/list")
    public R<List<Map<String, Object>>> publicList(@RequestParam(required = false) String species,
                                                   @RequestParam(required = false) String region) {
        return R.ok(service.publicList(species, region));
    }
}
