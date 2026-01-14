package com.workflow.platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流模板数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTemplateDTO {

    /**
     * 模板唯一标识
     * 对应 .id(templateId)
     */
    private String id;

    /**
     * 模板名称
     * 对应 .name(templateName)
     */
    private String name;

    /**
     * 模板描述
     * 对应 .description(description)
     */
    private String description;

    /**
     * 模板分类
     * 对应 .category(...)
     */
    private String category;

    /**
     * 标签列表
     * 对应 .tags(...)
     */
    private List<String> tags;

    /**
     * 工作流的具体数据/定义
     * 对应 .workflowData(workflow)
     * 注意：如果 workflow 是一个复杂对象，这里可以使用 Object，或者具体的 POJO 类型，
     * 如果是存储 JSON 字符串，则使用 String。
     */
    private WorkflowDTO workflowData;

    /**
     * 创建人 ID 或 用户名
     * 对应 .createdBy(createdBy)
     */
    private String createdBy;

    /**
     * 创建时间戳
     * 对应 .createdAt(System.currentTimeMillis()) -> 返回 long
     */
    private Long createdAt;

    /**
     * 使用次数
     * 对应 .usageCount(0)
     */
    private Integer usageCount;

    /**
     * 评分
     * 对应 .rating(0.0) -> 浮点数
     */
    private Double rating;

    /**
     * 兼容性信息（例如版本范围）
     * 对应 .compatibility(...)
     */
    private Map<String, Object> compatibility;

    /**
     * 额外的元数据（键值对）
     * 对应 .metadata(...)
     */
    private Map<String, Object> metadata;


    private  String validationRules;

    private String version;

    private Long lastUsedAt;

    private Long updatedAt;

    private String updatedBy;

    private List ratings;

    private int ratingCount;

    private int exportCount;

    private String importedBy;

    private long importedAt;

}