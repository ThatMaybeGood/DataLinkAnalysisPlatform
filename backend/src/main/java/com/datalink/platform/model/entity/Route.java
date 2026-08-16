package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路线实体（从路网定义的一条完整路径）
 */
@Data
@TableName("route")
public class Route {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("process_id")
    private Long processId;
    private String name;
    /** 优先级 DEFAULT/RECOMMENDED/ALTERNATE */
    private String priority;
    private String level;
    private String description;
    private String status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
