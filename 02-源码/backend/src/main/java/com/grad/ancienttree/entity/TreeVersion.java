package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 树木重要信息变更申请与版本留痕。对应表 tree_version。
 */
@Data
@TableName("tree_version")
public class TreeVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联古树ID */
    private Long treeId;

    /** 版本序号 */
    private Integer versionNo;

    /** 变更类型：保护级别/责任单位/树体状态/迁移/死亡/销号 */
    private String changeType;

    /** 变更前快照JSON */
    private String beforeJson;

    /** 变更后快照JSON */
    private String afterJson;

    /** 申请原因（不少于5字） */
    private String reason;

    /** 申请人ID */
    private Long applicantId;

    /** 申请人姓名 */
    private String applicantName;

    /** 申请时间 */
    private LocalDateTime applyTime;

    /** 审核人ID */
    private Long auditorId;

    /** 审核人姓名 */
    private String auditorName;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核状态：待审核/已通过/已驳回 */
    private String auditStatus;

    /** 审核意见 */
    private String auditRemark;
}
