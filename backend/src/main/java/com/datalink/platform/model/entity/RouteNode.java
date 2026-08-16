package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 路线站点实体（复合主键 route_id+seq，无 @TableId）
 */
@Data
@TableName("route_node")
public class RouteNode {
    @TableField("route_id")
    private Long routeId;
    /** 站点顺序（与 route_id 组成复合主键） */
    private Integer seq;
    @TableField("node_id")
    private Long nodeId;
    /** SLA：该环节标准时长 */
    @TableField("expected_duration_ms")
    private Long expectedDurationMs;
}
