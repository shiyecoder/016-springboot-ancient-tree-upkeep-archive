package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 养护工单。对应表 care_order。
 */
@Data
@TableName("care_order")
public class CareOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单编号 */
    private String orderCode;

    /** 古树ID */
    private Long treeId;

    /** 来源问题ID */
    private Long issueId;

    /** 养护类型：修剪/支撑/防腐/施肥/复壮/病虫害防治 */
    private String careType;

    /** 方案内容 */
    private String planContent;

    /** 养护单位 */
    private String executorOrg;

    /** 执行人ID */
    private Long executorUserId;

    /** 执行人姓名 */
    private String executorName;

    /** 方案审核人ID（专家） */
    private Long planAuditorId;

    /** 审核人姓名 */
    private String planAuditorName;

    /** 验收人ID */
    private Long acceptorId;

    /** 验收人姓名 */
    private String acceptorName;

    /** 状态：待方案/待审核/执行中/待验收/已完成 */
    private String status;

    /** 紧急程度：普通/紧急 */
    private String urgency;

    /** 截止日期 */
    private LocalDate deadline;

    /** 执行进度百分比 */
    private Integer progress;

    /** 执行结果说明 */
    private String resultDesc;

    /** 驳回原因 */
    private String rejectReason;

    /** 紧急抢救临时措施 */
    private String tempMeasure;

    /** 临时措施补全截止时间 */
    private LocalDateTime tempMeasureDeadline;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
