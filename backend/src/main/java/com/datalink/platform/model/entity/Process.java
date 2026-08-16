package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 流程实体（起点→终点）
 */
@Data
@TableName("process")
public class Process {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    /** 唯一编码 */
    private String code;
    /** 场景 DATA/BUSINESS/MANUFACTURING */
    private String scene;
    @TableField("start_node_id")
    private Long startNodeId;
    @TableField("end_node_id")
    private Long endNodeId;
    private String level;
    private String description;
    private String status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
