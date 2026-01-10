package com.workflow.platform.component.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.exception.TemplateException;
import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.entity.WorkflowTemplateEntity;
import com.workflow.platform.repository.WorkflowTemplateRepository;
import com.workflow.platform.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工作流模板引擎 - 提供模板创建、应用、管理和推荐功能
 */
@Slf4j
@Component
public class WorkflowTemplateEngine {

    @Autowired
    private WorkflowTemplateRepository templateRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonUtil jsonUtil;

    @Autowired
    private TemplateRecommender templateRecommender;

    @Value("${workflow.platform.templates.enabled:true}")
    private boolean templatesEnabled;

    @Value("${workflow.platform.templates.max-custom-templates:100}")
    private int maxCustomTemplates;

    @Value("${workflow.platform.templates.auto-generation:true}")
    private boolean autoGeneration;

    @Value("${workflow.platform.templates.recommendation-threshold:0.7}")
    private double recommendationThreshold;

    // 模板缓存
    private final Map<String, WorkflowTemplateEntity> templateCache = new ConcurrentHashMap<>();
    private final Map<String, TemplateCacheEntry> compiledTemplateCache = new ConcurrentHashMap<>();

    /**
     * 从工作流创建模板
     */
    public WorkflowTemplateEntity createTemplateFromWorkflow(String workflowId, WorkflowDTO workflow,
                                                             String templateName, String description,
                                                             String category, String createdBy,
                                                             boolean isPublic) {
        try {
            log.info("创建工作流模板: {} -> {}", workflowId, templateName);

            // 检查模板数量限制
            if (!canCreateTemplate(createdBy)) {
                throw new TemplateException("已达到最大模板创建限制");
            }

            // 创建工作流模板
            WorkflowTemplateEntity template = new WorkflowTemplateEntity();
            template.setTemplateId(generateTemplateId());
            template.setTemplateName(templateName);
            template.setDescription(description);
            template.setCategory(category);
            template.setCreatedBy(createdBy);
            template.setCreatedTime(new Date());
            template.setUpdatedTime(new Date());
            template.setPublic(isPublic);
            template.setUsageCount(0);
            template.setRating(0.0);
            template.setRatingCount(0);
            template.setTags(new ArrayList<>());

            // 提取模板数据
            TemplateData templateData = extractTemplateData(workflow);
            template.setTemplateData(jsonUtil.toJson(templateData));

            // 提取模板变量
            List<TemplateVariable> variables = extractTemplateVariables(workflow);
            template.setVariables(jsonUtil.toJson(variables));

            // 设置元数据
            TemplateMetadata metadata = createTemplateMetadata(workflow, templateData);
            template.setMetadata(jsonUtil.toJson(metadata));

            // 设置来源工作流
            template.setSourceWorkflowId(workflowId);
            template.setSourceWorkflowName(workflow.getName());

            // 保存模板
            template = templateRepository.save(template);

            // 更新缓存
            templateCache.put(template.getTemplateId(), template);

            // 记录模板创建事件
            logTemplateCreation(template, createdBy);

            log.info("模板创建成功: {} ({})", templateName, template.getTemplateId());
            return template;

        } catch (Exception e) {
            log.error("创建工作流模板失败: {}，错误: {}", workflowId, e.getMessage(), e);
            throw new TemplateException("创建模板失败: " + e.getMessage(), e);
        }
    }

    /**
     * 应用模板创建工作流
     */
    public WorkflowDTO applyTemplate(String templateId, Map<String, Object> parameters,
                                     String workflowName, String createdBy) {
        try {
            log.info("应用模板创建