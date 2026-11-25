package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 季节性养护计划生成规则（权重版本）。对应表 ancient_tree_rule。
 */
@Data
@TableName("ancient_tree_rule")
public class TreeRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 规则名称 */
    private String ruleName;

    /** 版本号 */
    private Integer version;

    /** 树种权重 */
    private Integer weightSpecies;

    /** 树龄区间权重 */
    private Integer weightAge;

    /** 保护等级权重 */
    private Integer weightLevel;

    /** 健康等级权重 */
    private Integer weightHealth;

    /** 季节权重 */
    private Integer weightSeason;

    /** 上次养护时间权重 */
    private Integer weightLastCare;

    /** 高分阈值（≥为优先养护） */
    private Integer thresholdHigh;

    /** 低分阈值（<为暂缓） */
    private Integer thresholdLow;

    /** 是否启用：1/0 */
    private Integer enabled;

    /** 生效时间 */
    private LocalDateTime effectiveTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
