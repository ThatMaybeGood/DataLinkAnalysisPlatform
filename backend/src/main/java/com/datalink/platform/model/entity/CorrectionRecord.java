package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 校正记录实体（图来源 G5 人工校正闭环）
 */
@Data
@TableName("correction_record")
public class CorrectionRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** NODE/EDGE/ROUTE/PATTERN */
    @TableField("target_type")
    private String targetType;

    /** 被校正对象 id */
    @TableField("target_id")
    private String targetId;

    /** 对象显示名 */
    @TableField("target_name")
    private String targetName;

    /** RENAME/CONFIRM/MERGE/ADD/DELETE/REORDER */
    private String operation;

    /** 原值 */
    @TableField("old_value")
    private String oldValue;

    /** 新值 */
    @TableField("new_value")
    private String newValue;

    /** 合并目标 id */
    @TableField("merge_target_id")
    private String mergeTargetId;

    /** 排序后的节点 id 列表 JSON */
    @TableField("reorder_node_ids")
    private String reorderNodeIds;

    /** PENDING/APPLIED/REJECTED */
    private String status;

    /** ENGINE/LLM/MANUAL */
    private String source;

    /** 操作人 */
    private String operator;

    /** 备注 */
    private String remark;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
