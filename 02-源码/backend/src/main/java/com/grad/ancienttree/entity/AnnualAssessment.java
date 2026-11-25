package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 年度评估。对应表 annual_assessment。
 */
@Data
@TableName("annual_assessment")
public class AnnualAssessment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 古树ID */
    private Long treeId;

    /** 评估年度 */
    private Integer assessYear;

    /** 生长势得分 */
    private Integer growthScore;

    /** 结构安全得分 */
    private Integer structureScore;

    /** 病虫害得分 */
    private Integer pestScore;

    /** 生境得分 */
    private Integer habitatScore;

    /** 综合得分 */
    private Integer totalScore;

    /** 生成健康等级 */
    private String healthLevel;

    /** 下年建议 */
    private String suggestion;

    /** 评估人（专家）ID */
    private Long assessorId;

    /** 评估人姓名 */
    private String assessorName;

    /** 状态：草稿/已提交 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
