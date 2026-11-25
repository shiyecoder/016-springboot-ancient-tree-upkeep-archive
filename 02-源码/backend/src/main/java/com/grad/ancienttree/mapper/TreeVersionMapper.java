package com.grad.ancienttree.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.ancienttree.entity.TreeVersion;
import org.apache.ibatis.annotations.Mapper;

/** 树木重要信息变更申请与版本留痕 数据访问。 */
@Mapper
public interface TreeVersionMapper extends BaseMapper<TreeVersion> {
}
