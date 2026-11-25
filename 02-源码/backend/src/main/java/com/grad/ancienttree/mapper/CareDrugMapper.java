package com.grad.ancienttree.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.ancienttree.entity.CareDrug;
import org.apache.ibatis.annotations.Mapper;

/** 用药记录（受方案配额约束） 数据访问。 */
@Mapper
public interface CareDrugMapper extends BaseMapper<CareDrug> {
}
