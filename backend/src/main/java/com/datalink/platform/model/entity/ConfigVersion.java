package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 配置版本实体（全配置对象留痕快照）
 */
@Data
@TableName("config_version")
public class ConfigVersion {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 目标类型 PROCESS/NODE/ROUTE/CHECKPOINT/CONNECTOR */
    @TableField("target_type")
    private String targetType;
    /** 目标对象 id */
    @TableField("target_id")
    private Long targetId;
    /** 版本号（同一目标自增） */
    private Integer version;
    /** 该版本完整配置快照 */
    private String content;
    /** 变更说明 */
    @TableField("change_note")
    private String changeNote;
    /** 操作人 */
    private String operator;
    /** DRAFT/PENDING_APPROVAL/PUBLISHED/ROLLED_BACK */
    private String status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
