package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 巡查发现的问题。对应表 tree_issue。
 */
@Data
@TableName("tree_issue")
public class TreeIssue {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 问题编号 */
    private String issueCode;

    /** 古树ID */
    private Long treeId;

    /** 来源巡查记录ID */
    private Long patrolId;

    /** 问题类型 */
    private String issueType;

    /** 严重程度 */
    private String severity;

    /** 分级：日常养护/重点观察/紧急抢救 */
    private String issueLevel;

    /** 问题描述 */
    private String description;

    /** 状态：待处理/已派单/已关闭 */
    private String status;

    /** 关联养护工单ID */
    private Long handleOrderId;

    /** 创建时间 */
    private LocalDateTime createTime;
}
