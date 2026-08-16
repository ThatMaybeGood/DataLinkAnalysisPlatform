package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 命名别名实体（通用，任意对象可加）
 */
@Data
@TableName("alias")
public class Alias {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 目标类型 PROCESS/ROUTE/INSTANCE/NODE/VIEW/CHECKPOINT */
    @TableField("target_type")
    private String targetType;
    @TableField("target_id")
    private Long targetId;
    private String name;
    /** 是否主显示名 1/0 */
    @TableField("is_primary")
    private Integer isPrimary;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
