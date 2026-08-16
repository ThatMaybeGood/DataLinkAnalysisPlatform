package com.datalink.platform.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.model.entity.RouteNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 路线站点 Mapper（复合主键，常用自定义查询）
 */
@Mapper
public interface RouteNodeMapper extends BaseMapper<RouteNode> {

    /**
     * 统计某流程下所有路线覆盖的去重站点数（按 route.process_id 关联）
     */
    @Select("SELECT COUNT(DISTINCT rn.node_id) FROM route_node rn JOIN route r ON rn.route_id = r.id WHERE r.process_id = #{processId}")
    int countDistinctNodeByProcessId(Long processId);
}
