package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 古树名木主档（一树一档）。对应表 ancient_tree。
 */
@Data
@TableName("ancient_tree")
public class AncientTree {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 档案编号，业务唯一 */
    private String treeCode;

    /** 树种 */
    private String species;

    /** 估测树龄（年） */
    private Integer ageEstimate;

    /** 所在位置 */
    private String location;

    /** 所有权 */
    private String ownership;

    /** 保护等级：一级/二级/三级/后续资源 */
    private String protectLevel;

    /** 树高（米） */
    private BigDecimal treeHeight;

    /** 胸径（厘米） */
    private BigDecimal diameter;

    /** 冠幅（米） */
    private BigDecimal crownWidth;

    /** 健康等级：优/良/中/差/濒危 */
    private String healthLevel;

    /** 生长状况 */
    private String growthStatus;

    /** 历史故事 */
    private String story;

    /** 所属区域 */
    private String region;

    /** 普通坐标经度（非地图服务） */
    private BigDecimal longitude;

    /** 普通坐标纬度 */
    private BigDecimal latitude;

    /** 封面照片相对路径 */
    private String coverImage;

    /** 责任单位 */
    private String managerOrg;

    /** 责任人用户ID */
    private Long managerUserId;

    /** 最近巡查日期 */
    private LocalDate lastPatrolDate;

    /** 档案状态：正常/已迁移/已死亡/已销号 */
    private String archiveStatus;

    /** 公开审核状态：待审核/已通过/未通过 */
    private String auditStatus;

    /** 乐观锁版本号 */
    private Integer version;

    /** 创建人 */
    private String createBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    private Integer deleted;
}
