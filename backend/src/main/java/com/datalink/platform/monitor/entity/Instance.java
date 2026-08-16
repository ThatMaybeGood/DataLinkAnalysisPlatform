package com.datalink.platform.monitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实例实体（一次真实运行）
 */
@Data
@TableName("instance")
public class Instance {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属流程 */
    @TableField("process_id")
    private Long processId;
    /** 选用路线 */
    @TableField("route_id")
    private Long routeId;
    /** 业务单号/流水号 */
    @TableField("biz_no")
    private String bizNo;
    /** 业务别名（显示名） */
    @TableField("biz_name")
    private String bizName;
    /** 开始时间 */
    @TableField("start_time")
    private LocalDateTime startTime;
    /** 结束时间 */
    @TableField("end_time")
    private LocalDateTime endTime;
    /** RUNNING/SUCCESS/FAIL/STUCK/TIMEOUT */
    private String status;
    /** 当前所处站点 */
    @TableField("current_node_id")
    private Long currentNodeId;
    /** 总耗时（毫秒） */
    @TableField("total_duration_ms")
    private Long totalDurationMs;
    /** INFER特征推断/MANUAL人工/REPORT进度上报/API */
    private String source;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
