package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 巡查记录。对应表 tree_patrol。
 */
@Data
@TableName("tree_patrol")
public class TreePatrol {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 巡查编号 */
    private String patrolCode;

    /** 古树ID */
    private Long treeId;

    /** 巡查计划ID */
    private Long planId;

    /** 巡查人ID */
    private Long patrolUserId;

    /** 巡查人姓名 */
    private String patrolUserName;

    /** 巡查时间 */
    private LocalDateTime patrolTime;

    /** 树干空洞情况 */
    private String trunkHollow;

    /** 倾斜程度 */
    private String leanDegree;

    /** 枯枝情况 */
    private String deadBranch;

    /** 树冠偏冠情况 */
    private String crownDeviation;

    /** 病虫害描述 */
    private String pestDesc;

    /** 土壤与生境 */
    private String soilDesc;

    /** 支撑设施 */
    private String supportFacility;

    /** 避雷设施 */
    private String lightningFacility;

    /** 现场照片（逗号分隔相对路径） */
    private String photos;

    /** 问题分级：日常养护/重点观察/紧急抢救 */
    private String issueLevel;

    /** 巡查结论 */
    private String result;

    /** 状态：待提交/已提交/已生成工单 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
