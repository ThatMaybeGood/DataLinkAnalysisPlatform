package com.datalink.platform.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.model.entity.Node;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站点节点 Mapper
 */
@Mapper
public interface NodeMapper extends BaseMapper<Node> {
}
