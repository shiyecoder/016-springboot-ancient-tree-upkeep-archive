package com.grad.ancienttree.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.ancienttree.entity.TreeIssue;
import org.apache.ibatis.annotations.Mapper;

/** 巡查发现的问题 数据访问。 */
@Mapper
public interface TreeIssueMapper extends BaseMapper<TreeIssue> {
}
