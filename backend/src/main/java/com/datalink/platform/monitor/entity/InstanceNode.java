package com.datalink.platform.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实例经过站点实体（每环节耗时/状态）
 */
@Data
@TableName("instance_node")
public class InstanceNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属实例 */
    @TableField("instance_id")
    private Long instanceId;
    /** 站点节点 id */
    @TableField("node_id")
    private Long nodeId;
    /** 经过顺序 */
    private Integer seq;
    /** 到达时间 */
    @TableField("arrive_time")
    private LocalDateTime arriveTime;
    /** 离开时间 */
    @TableField("leave_time")
    private LocalDateTime leaveTime;
    /** 该环节耗时（毫秒） */
    @TableField("duration_ms")
    private Long durationMs;
    private String status;
    private String message;
}
