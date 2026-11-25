package com.grad.ancienttree.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 用药记录（受方案配额约束）。对应表 care_drug。
 */
@Data
@TableName("care_drug")
public class CareDrug {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 养护工单ID */
    private Long orderId;

    /** 药品名称 */
    private String drugName;

    /** 本次剂量 */
    private BigDecimal dosage;

    /** 单位 */
    private String unit;

    /** 施用日期 */
    private LocalDate applyDate;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 安全措施 */
    private String safetyMeasure;

    /** 方案核定总量 */
    private BigDecimal planQuota;

    /** 本次登记后累计用量 */
    private BigDecimal usedQty;
}
