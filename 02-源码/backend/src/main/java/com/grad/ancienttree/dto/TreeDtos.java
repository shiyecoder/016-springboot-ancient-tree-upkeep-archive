package com.grad.ancienttree.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 古树档案与变更申请的入参。 */
public class TreeDtos {

    /** 新建/编辑档案入参。编号由后端生成，前端只读。 */
    public record TreeReq(
            String treeCode,
            @NotBlank(message = "树种不能为空") String species,
            Integer ageEstimate,
            @NotBlank(message = "所在位置不能为空") String location,
            String ownership,
            @NotBlank(message = "保护等级不能为空") String protectLevel,
            BigDecimal treeHeight,
            BigDecimal diameter,
            BigDecimal crownWidth,
            String healthLevel,
            @NotBlank(message = "生长状况不能为空") String growthStatus,
            String story,
            String region,
            BigDecimal longitude,
            BigDecimal latitude,
            String coverImage,
            String managerOrg,
            Long managerUserId,
            LocalDate lastPatrolDate,
            Integer version
    ) {
    }

    /** 重要信息变更申请：保护级别、责任单位、树体状态都走审核并留版本 */
    public record ChangeReq(@NotBlank String changeType,
                            @NotBlank String reason,
                            String afterJson) {
    }

    /** 状态流转：目标状态 + 原因（退回/驳回/销号时必须填，且不少于 5 字） */
    public record TransitionReq(@NotBlank String target, String reason) {
    }

    /** 创新模块计算入参：允许指定规则版本，不传则用当前启用版本 */
    public record EvaluateReq(Long ruleId, String season) {
    }

    /** 人工调整结果，原始计算结果不得覆盖 */
    public record AdjustReq(@NotBlank String level, @NotBlank String reason) {
    }
}
