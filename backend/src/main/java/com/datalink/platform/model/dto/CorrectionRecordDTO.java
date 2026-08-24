package com.datalink.platform.model.dto;

import lombok.Data;

/**
 * 提交校正记录请求
 */
@Data
public class CorrectionRecordDTO {

    /** NODE/EDGE/ROUTE/PATTERN */
    private String targetType;

    /** 被校正对象 id */
    private String targetId;

    /** 对象显示名 */
    private String targetName;

    /** RENAME/CONFIRM/MERGE/ADD/DELETE/REORDER */
    private String operation;

    /** 原值 */
    private String oldValue;

    /** 新值 */
    private String newValue;

    /** 合并目标 id */
    private String mergeTargetId;

    /** 排序后的节点 id 列表 JSON */
    private String reorderNodeIds;

    /** 备注 */
    private String remark;

    /** 是否同时沉淀为模式 */
    private Boolean savePattern;

    /** 模式类型 */
    private String patternType;

    /** 模式名称 */
    private String patternName;

    /** 模式描述 */
    private String patternDescription;
}
