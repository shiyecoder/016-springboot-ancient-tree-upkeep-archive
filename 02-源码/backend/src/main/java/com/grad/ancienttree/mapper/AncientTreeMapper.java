package com.grad.ancienttree.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.ancienttree.entity.AncientTree;
import org.apache.ibatis.annotations.Mapper;

/** 古树名木主档（一树一档） 数据访问。 */
@Mapper
public interface AncientTreeMapper extends BaseMapper<AncientTree> {
}
