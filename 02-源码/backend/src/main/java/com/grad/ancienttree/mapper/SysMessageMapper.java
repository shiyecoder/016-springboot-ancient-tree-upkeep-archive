package com.grad.ancienttree.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.grad.ancienttree.entity.SysMessage;
import org.apache.ibatis.annotations.Mapper;

/** 站内消息 数据访问。 */
@Mapper
public interface SysMessageMapper extends BaseMapper<SysMessage> {
}
