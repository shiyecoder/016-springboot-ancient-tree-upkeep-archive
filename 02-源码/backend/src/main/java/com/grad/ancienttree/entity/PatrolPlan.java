package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 巡查计划（按保护级别设频率）。对应表 patrol_plan。
 */
@Data
@TableName("patrol_plan")
public class PatrolPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 计划编号 */
    private String planCode;

    /** 计划名称 */
    private String planName;

    /** 适用保护等级 */
    private String protectLevel;

    /** 区域 */
    private String region;

    /** 每年巡查频次 */
    private Integer frequency;

    /** 执行人ID */
    private Long assigneeId;

    /** 执行人姓名 */
    private String assigneeName;

    /** 开始日期 */
    private LocalDate startDate;

    /** 结束日期 */
    private LocalDate endDate;

    /** 状态：草稿/执行中/已结束 */
    private String status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
