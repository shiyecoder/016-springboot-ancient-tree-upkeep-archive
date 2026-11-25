package com.grad.ancienttree.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.ancienttree.entity.PatrolPlan;
import org.apache.ibatis.annotations.Mapper;

/** 巡查计划（按保护级别设频率） 数据访问。 */
@Mapper
public interface PatrolPlanMapper extends BaseMapper<PatrolPlan> {
}
