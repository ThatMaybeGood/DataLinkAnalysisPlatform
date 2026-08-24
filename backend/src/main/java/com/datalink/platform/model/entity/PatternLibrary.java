package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模式库实体（图来源 G5 模式沉淀）
 */
@Data
@TableName("pattern_library")
public class PatternLibrary {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** NODE_NAME/EDGE_NAME/ROUTE_TEMPLATE */
    @TableField("pattern_type")
    private String patternType;

    /** 模式匹配键 */
    @TableField("pattern_key")
    private String patternKey;

    /** 模式值/模板 */
    @TableField("pattern_value")
    private String patternValue;

    /** NODE/EDGE/ROUTE/PATTERN */
    @TableField("source_type")
    private String sourceType;

    /** 来源对象 id */
    @TableField("source_id")
    private String sourceId;

    /** 来源操作类型 */
    @TableField("source_operation")
    private String sourceOperation;

    /** 命中次数 */
    @TableField("hit_count")
    private Integer hitCount;

    /** 是否已确认 1/0 */
    private Integer confirmed;

    /** 创建人 */
    @TableField("created_by")
    private String createdBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
