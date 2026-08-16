package com.datalink.platform.monitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警实体
 */
@Data
@TableName("alert")
public class Alert {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** STUCK/FAIL/TIMEOUT/CHECK_FAIL */
    @TableField("alert_type")
    private String alertType;
    /** NODE/INSTANCE/ROUTE/PROCESS */
    @TableField("target_type")
    private String targetType;
    /** 目标对象 id */
    @TableField("target_id")
    private Long targetId;
    private String message;
    private String severity;
    /** NOTIFY/TICKET/AUTO_ACTION（逗号组合） */
    private String disposition;
    /** OPEN/RESOLVED */
    private String status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 解决时间 */
    @TableField("resolved_at")
    private LocalDateTime resolvedAt;
}
