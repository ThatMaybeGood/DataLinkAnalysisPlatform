package com.datalink.platform.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.model.entity.RouteNode;
import org.apache.ibatis.annotations.Mapper;

/**
 * 路线站点 Mapper（复合主键，常用自定义查询）
 */
@Mapper
public interface RouteNodeMapper extends BaseMapper<RouteNode> {
}
