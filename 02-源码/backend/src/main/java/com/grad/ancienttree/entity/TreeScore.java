package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 创新模块计算留痕。对应表 ancient_tree_score。
 */
@Data
@TableName("ancient_tree_score")
public class TreeScore {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 古树ID */
    private Long treeId;

    /** 规则ID */
    private Long ruleId;

    /** 规则版本 */
    private Integer ruleVersion;

    /** 输入快照JSON */
    private String inputSnapshot;

    /** 各项得分与权重明细JSON */
    private String detailJson;

    /** 总分 */
    private Integer totalScore;

    /** 结果等级：优先养护/常规养护/暂缓 */
    private String resultLevel;

    /** 推荐任务清单 */
    private String recommendTasks;

    /** 结果解释文本 */
    private String explanation;

    /** 未满足条件JSON */
    private String unmetJson;

    /** 人工调整后等级 */
    private String manualLevel;

    /** 人工调整原因 */
    private String manualReason;

    /** 调整人ID */
    private Long manualBy;

    /** 调整时间 */
    private LocalDateTime manualTime;

    /** 是否采纳：1/0/NULL未定 */
    private Integer adopted;

    /** 计算时间 */
    private LocalDateTime createTime;
}
