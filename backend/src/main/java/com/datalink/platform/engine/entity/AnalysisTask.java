package com.datalink.platform.engine.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图来源分析任务实体（对应 analysis_task 表，V10 迁移）
 * 每次对某来源（可多来源合并）发起的一次分析留痕。
 */
@Data
@TableName("analysis_task")
public class AnalysisTask {
    @TableId(type = IdType.AUTO) private Long id;
    /** 首个来源连接器 id（过滤/兼容单来源） */
    @TableField("connector_id") private Long connectorId;
    /** 多来源合并时的全部来源 id（逗号分隔） */
    @TableField("connector_ids") private String connectorIds;
    /** 来源名快照（删库后仍可显示；多来源为拼接名） */
    @TableField("connector_name") private String connectorName;
    /** ENGINE / LLM */
    @TableField("task_type") private String taskType;
    /** RUNNING / SUCCESS / FAILED */
    private String status;
    /** 草稿/细化结果 JSON 快照 */
    @TableField("draft_snapshot") private String draftSnapshot;
    @TableField("error_message") private String errorMessage;
    private String operator;
    @TableField(value = "created_at", fill = FieldFill.INSERT) private LocalDateTime createdAt;
    @TableField("finished_at") private LocalDateTime finishedAt;
}
