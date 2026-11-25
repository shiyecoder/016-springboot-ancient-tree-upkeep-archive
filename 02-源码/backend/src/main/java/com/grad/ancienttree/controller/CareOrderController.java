package com.grad.ancienttree.controller;

import com.grad.ancienttree.common.PageResult;
import com.grad.ancienttree.common.R;
import com.grad.ancienttree.entity.CareDrug;
import com.grad.ancienttree.entity.CareOrder;
import com.grad.ancienttree.service.CareOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 养护工单与用药管理接口。 */
@RestController
@RequestMapping("/api/care")
@RequiredArgsConstructor
public class CareOrderController {

    private final CareOrderService service;

    @GetMapping("/order/page")
    public R<PageResult<Map<String, Object>>> page(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestParam(required = false) String tab,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String careType,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Long treeId) {
        return R.ok(service.page(page, size, tab, status, careType, keyword, treeId));
    }

    @GetMapping("/order/{id}")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(service.detail(id));
    }

    @PostMapping("/order")
    public R<Long> create(@RequestBody CareOrder req) {
        return R.ok(service.create(req));
    }

    /**
     * 状态流转。action：submit 提交方案 / audit 审核通过 / reject 驳回 /
     * progress 上报进度 / finish 提交验收 / accept 验收通过 / back 验收退回
     */
    @PostMapping("/order/{id}/transition")
    public R<Void> transition(@PathVariable Long id,
                              @RequestParam String action,
                              @RequestParam(required = false) String reason,
                              @RequestParam(required = false) String resultDesc,
                              @RequestParam(required = false) Integer progress) {
        service.transition(id, action, reason, resultDesc, progress);
        return R.ok();
    }

    @GetMapping("/order/{id}/drugs")
    public R<List<CareDrug>> drugs(@PathVariable Long id) {
        return R.ok(service.drugs(id));
    }

    /** 用药登记：后端校验不得超出方案核定数量 */
    @PostMapping("/order/{id}/drug")
    public R<Map<String, Object>> addDrug(@PathVariable Long id, @RequestBody CareDrug req) {
        return R.ok(service.addDrug(id, req));
    }
}
