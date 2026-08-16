package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 路网边实体（站点间有向连接，支持分岔/汇合）
 */
@Data
@TableName("relation")
public class Relation {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("from_node_id")
    private Long fromNodeId;
    @TableField("to_node_id")
    private Long toNodeId;
    /** 关系类型 DATA_FLOW/API/SUBSCRIBE/APPROVAL... */
    @TableField("relation_type")
    private String relationType;
    private String level;
    private String description;
    private String status;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
