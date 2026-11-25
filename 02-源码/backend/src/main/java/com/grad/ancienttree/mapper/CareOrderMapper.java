package com.grad.ancienttree.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.ancienttree.entity.CareOrder;
import org.apache.ibatis.annotations.Mapper;

/** 养护工单 数据访问。 */
@Mapper
public interface CareOrderMapper extends BaseMapper<CareOrder> {
}
