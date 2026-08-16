package com.datalink.platform.monitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工单实体
 */
@Data
@TableName("ticket")
public class Ticket {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联告警 */
    @TableField("alert_id")
    private Long alertId;
    /** 处理人 */
    private String assignee;
    private String priority;
    /** OPEN/PROCESSING/RESOLVED */
    private String status;
    private String description;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 解决时间 */
    @TableField("resolved_at")
    private LocalDateTime resolvedAt;
}
