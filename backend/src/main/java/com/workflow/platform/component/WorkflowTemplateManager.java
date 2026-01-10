package com.workflow.platform.component;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.dto.WorkflowTemplateDTO;
import com.workflow.platform.util.JsonUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 工作流模板管理器 - 管理工作流模板和克隆功能
 */
@Slf4j
@Component
public class WorkflowTemplateManager {

    @Autowired
    private OfflineDataManager offlineDataManager;

    @Value("${workflow.platform.templates.enabled:true}")
    private boolean templatesEnabled;

    @Value("${workflow.platform.templates.default-category:general}")
    private String defaultCategory;

    @Value("${workflow.platform.templates.max-templates:100}")
    private int maxTemplates;

    @Value("${workflow.platform.templates.auto-categorize:true}")
    private boolean autoCategorize;

    // 模板存储
    private final Map<String, WorkflowTemplateDTO> templates = new ConcurrentHashMap<>();

    // 模板分类
    private final Map<String, List<String>> categoryTemplates = new ConcurrentHashMap<>();

    /**
     * 从工作流创建模板
     */
    public WorkflowTemplateDTO createTemplateFromWorkflow(WorkflowDTO workflow,
                                                          String templateName,
                                                          String description,
                                                          String category,
                                                          List<String> tags,
                                                          String createdBy) {
        if (!templatesEnabled) {
            log.warn("模板功能未启用");
            return null;
        }

        try {
            // 生成模板ID
            String templateId = generateTemplateId(templateName);

            // 创建模板
            WorkflowTemplateDTO template = WorkflowTemplateDTO.builder()
                    .id(templateId)
                    .name(templateName)
                    .description(description)
                    .category(category != null ? category : autoCategorize(workflow))
                    .tags(tags != null ? tags : extractTags(workflow))
                    .workflowData(workflow)
                    .createdBy(createdBy)
                    .createdAt(System.currentTimeMillis())
                    .usageCount(0)
                    .rating(0.0)
                    .compatibility(generateCompatibilityInfo(workflow))
                    .metadata(generateTemplateMetadata(workflow))
                    .build();

            // 保存模板
            saveTemplate(template);

            log.info("创建工作流模板: {} -> {}，分类: {}",
                    workflow.getId(), templateName, template.getCategory());

            return template;

        } catch (Exception e) {
            log.error("创建工作流模板失败", e);
            throw new RuntimeException("创建模板失败", e);
        }
    }

    /**
     * 从模板创建工作流
     */
    public WorkflowDTO createWorkflowFromTemplate(String templateId,
                                                  String workflowName,
                                                  String description,
                                                  Map<String, Object> parameters,
                                                  String createdBy) {
        if (!templatesEnabled) {
            log.warn("模板功能未启用");
            return null;
        }

        try {
            WorkflowTemplateDTO template = templates.get(templateId);
            if (template == null) {
                throw new IllegalArgumentException("模板不存在: " + templateId);
            }

            // 克隆工作流数据
            WorkflowDTO workflow = deepClone(template.getWorkflowData());

            // 应用模板参数
            applyTemplateParameters(workflow, parameters);

            // 设置工作流属性
            workflow.setId(generateWorkflowId(workflowName));
            workflow.setName(workflowName);
            workflow.setDescription(description != null ? description : workflow.getDescription());
            workflow.setCreatedBy(createdBy);
            workflow.setCreatedAt(System.currentTimeMillis());
            workflow.setUpdatedAt(System.currentTimeMillis());
            workflow.setTemplateId(templateId);
            workflow.setTemplateVersion(template.getVersion());

            // 保存工作流
            offlineDataManager.saveWorkflow(workflow);

            // 更新模板使用统计
            template.setUsageCount(template.getUsageCount() + 1);
            template.setLastUsedAt(System.currentTimeMillis());
            saveTemplate(template);

            log.info("从模板创建工作流: {} -> {}，模板: {}",
                    templateId, workflowName, template.getName());

            return workflow;

        } catch (Exception e) {
            log.error("从模板创建工作流失败", e);
            throw new RuntimeException("从模板创建工作流失败", e);
        }
    }

    /**
     * 获取模板详情
     */
    public WorkflowTemplateDTO getTemplate(String templateId) {
        return templates.get(templateId);
    }

    /**
     * 搜索模板
     */
    public List<WorkflowTemplateDTO> searchTemplates(TemplateSearchCriteria criteria) {
        if (!templatesEnabled) {
            return Collections.emptyList();
        }

        return templates.values().stream()
                .filter(template -> matchesCriteria(template, criteria))
                .sorted(getTemplateComparator(criteria.getSortBy(), criteria.getSortOrder()))
                .skip((long) criteria.getPage() * criteria.getSize())
                .limit(criteria.getSize())
                .collect(Collectors.toList());
    }

    /**
     * 获取模板分类
     */
    public Map<String, TemplateCategoryStats> getTemplateCategories() {
        Map<String, TemplateCategoryStats> stats = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : categoryTemplates.entrySet()) {
            String category = entry.getKey();
            List<String> templateIds = entry.getValue();

            TemplateCategoryStats categoryStats = new TemplateCategoryStats(category);
            categoryStats.setTemplateCount(templateIds.size());

            // 计算分类统计
            int totalUsage = 0;
            double totalRating = 0.0;
            int ratedCount = 0;

            for (String templateId : templateIds) {
                WorkflowTemplateDTO template = templates.get(templateId);
                if (template != null) {
                    totalUsage += template.getUsageCount();
                    if (template.getRating() > 0) {
                        totalRating += template.getRating();
                        ratedCount++;
                    }
                }
            }

            categoryStats.setTotalUsage(totalUsage);
            if (ratedCount > 0) {
                categoryStats.setAverageRating(totalRating / ratedCount);
            }
            categoryStats.setRatedCount(ratedCount);

            stats.put(category, categoryStats);
        }

        return stats;
    }

    /**
     * 更新模板
     */
    public WorkflowTemplateDTO updateTemplate(String templateId,
                                              WorkflowTemplateDTO updates) {
        if (!templatesEnabled) {
            return null;
        }

        try {
            WorkflowTemplateDTO template = templates.get(templateId);
            if (template == null) {
                throw new IllegalArgumentException("模板不存在: " + templateId);
            }

            // 更新模板属性
            if (updates.getName() != null) {
                template.setName(updates.getName());
            }
            if (updates.getDescription() != null) {
                template.setDescription(updates.getDescription());
            }
            if (updates.getCategory() != null) {
                // 更新分类映射
                updateTemplateCategory(templateId, template.getCategory(), updates.getCategory());
                template.setCategory(updates.getCategory());
            }
            if (updates.getTags() != null) {
                template.setTags(updates.getTags());
            }
            if (updates.getWorkflowData() != null) {
                template.setWorkflowData(updates.getWorkflowData());
                template.setCompatibility(generateCompatibilityInfo(updates.getWorkflowData()));
                template.setMetadata(generateTemplateMetadata(updates.getWorkflowData()));
            }

            template.setVersion(template.getVersion() + 1);
            template.setUpdatedAt(System.currentTimeMillis());
            template.setUpdatedBy(updates.getUpdatedBy());

            // 保存更新
            saveTemplate(template);

            log.info("更新模板: {}，版本: v{}", templateId, template.getVersion());

            return template;

        } catch (Exception e) {
            log.error("更新模板失败", e);
            throw new RuntimeException("更新模板失败", e);
        }
    }

    /**
     * 删除模板
     */
    public boolean deleteTemplate(String templateId) {
        if (!templatesEnabled) {
            return false;
        }

        try {
            WorkflowTemplateDTO template = templates.remove(templateId);
            if (template == null) {
                return false;
            }

            // 从分类映射中移除
            removeTemplateFromCategory(templateId, template.getCategory());

            log.info("删除模板: {}", templateId);
            return true;

        } catch (Exception e) {
            log.error("删除模板失败", e);
            return false;
        }
    }

    /**
     * 为模板评分
     */
    public boolean rateTemplate(String templateId, int rating, String comment, String ratedBy) {
        if (!templatesEnabled) {
            return false;
        }

        try {
            WorkflowTemplateDTO template = templates.get(templateId);
            if (template == null) {
                return false;
            }

            // 验证评分范围
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("评分必须在1-5之间");
            }

            // 更新评分
            List<TemplateRating> ratings = template.getRatings();
            if (ratings == null) {
                ratings = new ArrayList<>();
                template.setRatings(ratings);
            }

            // 添加新评分
            TemplateRating newRating = new TemplateRating();
            newRating.setRating(rating);
            newRating.setComment(comment);
            newRating.setRatedBy(ratedBy);
            newRating.setRatedAt(System.currentTimeMillis());
            ratings.add(newRating);

            // 计算平均分
            double averageRating = ratings.stream()
                    .mapToInt(TemplateRating::getRating)
                    .average()
                    .orElse(0.0);

            template.setRating(averageRating);
            template.setRatingCount(ratings.size());

            // 保存更新
            saveTemplate(template);

            log.info("为模板评分: {} -> {} 星，平均分: {}",
                    templateId, rating, averageRating);

            return true;

        } catch (Exception e) {
            log.error("模板评分失败", e);
            return false;
        }
    }

    /**
     * 导出模板
     */
    public TemplateExport exportTemplate(String templateId, ExportFormat format) {
        if (!templatesEnabled) {
            return null;
        }

        try {
            WorkflowTemplateDTO template = templates.get(templateId);
            if (template == null) {
                throw new IllegalArgumentException("模板不存在: " + templateId);
            }

            TemplateExport export = new TemplateExport();
            export.setTemplate(template);
            export.setExportTime(System.currentTimeMillis());
            export.setFormat(format);

            switch (format) {
                case JSON:
                    export.setData(JsonUtil.toPrettyJson(template));
                    break;
                case XML:
                    export.setData(exportToXml(template));
                    break;
                case YAML:
                    export.setData(exportToYaml(template));
                    break;
            }

            // 记录导出操作
            template.setExportCount(template.getExportCount() + 1);
            saveTemplate(template);

            return export;

        } catch (Exception e) {
            log.error("导出模板失败", e);
            throw new RuntimeException("导出模板失败", e);
        }
    }

    /**
     * 导入模板
     */
    public WorkflowTemplateDTO importTemplate(String templateData,
                                              ImportFormat format,
                                              String importedBy) {
        if (!templatesEnabled) {
            return null;
        }

        try {
            WorkflowTemplateDTO template;

            switch (format) {
                case JSON:
                    template = JsonUtil.fromJson(templateData, WorkflowTemplateDTO.class);
                    break;
                case XML:
                    template = importFromXml(templateData);
                    break;
                case YAML:
                    template = importFromYaml(templateData);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的导入格式: " + format);
            }

            // 验证模板
            validateTemplate(template);

            // 设置导入信息
            template.setImportedBy(importedBy);
            template.setImportedAt(System.currentTimeMillis());

            // 生成新ID（避免冲突）
            if (templates.containsKey(template.getId())) {
                template.setId(generateTemplateId(template.getName()));
            }

            // 保存模板
            saveTemplate(template);

            log.info("导入模板: {} -> {}", template.getId(), template.getName());

            return template;

        } catch (Exception e) {
            log.error("导入模板失败", e);
            throw new RuntimeException("导入模板失败", e);
        }
    }

    /**
     * 获取模板推荐
     */
    public List<WorkflowTemplateDTO> getTemplateRecommendations(RecommendationCriteria criteria) {
        if (!templatesEnabled) {
            return Collections.emptyList();
        }

        List<WorkflowTemplateDTO> recommendations = new ArrayList<>();

        // 基于使用频率的推荐
        if (criteria.isBasedOnUsage()) {
            recommendations.addAll(getPopularTemplates(criteria.getLimit()));
        }

        // 基于评分的推荐
        if (criteria.isBasedOnRating()) {
            recommendations.addAll(getHighlyRatedTemplates(criteria.getLimit()));
        }

        // 基于类别的推荐
        if (criteria.getCategory() != null) {
            recommendations.addAll(getTemplatesByCategory(criteria.getCategory(), criteria.getLimit()));
        }

        // 基于相似度的推荐
        if (criteria.getReferenceWorkflowId() != null) {
            recommendations.addAll(getSimilarTemplates(criteria.getReferenceWorkflowId(), criteria.getLimit()));
        }

        // 去重和排序
        return recommendations.stream()
                .distinct()
                .sorted((t1, t2) -> Double.compare(t2.getRating(), t1.getRating()))
                .limit(criteria.getLimit())
                .collect(Collectors.toList());
    }

    /**
     * 分析模板使用情况
     */
    public TemplateAnalytics analyzeTemplateUsage(AnalyticsCriteria criteria) {
        TemplateAnalytics analytics = new TemplateAnalytics();
        analytics.setAnalysisTime(System.currentTimeMillis());
        analytics.setCriteria(criteria);

        // 收集使用数据
        Map<String, Integer> usageByTemplate = new HashMap<>();
        Map<String, Integer> usageByCategory = new HashMap<>();
        Map<String, Integer> usageByUser = new HashMap<>();
        Map<Integer, Integer> usageByMonth = new HashMap<>();

        long startTime = criteria.getStartTime() != null ? criteria.getStartTime() :
                System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L); // 默认30天
        long endTime = criteria.getEndTime() != null ? criteria.getEndTime() : System.currentTimeMillis();

        for (WorkflowTemplateDTO template : templates.values()) {
            // 统计使用次数
            if (template.getUsageCount() > 0) {
                usageByTemplate.put(template.getId(), template.getUsageCount());

                // 分类统计
                String category = template.getCategory();
                usageByCategory.put(category,
                        usageByCategory.getOrDefault(category, 0) + template.getUsageCount());

                // 用户统计（如果有用户信息）
                if (template.getCreatedBy() != null) {
                    usageByUser.put(template.getCreatedBy(),
                            usageByUser.getOrDefault(template.getCreatedBy(), 0) + template.getUsageCount());
                }
            }

            // 时间统计
            if (template.getLastUsedAt() >= startTime && template.getLastUsedAt() <= endTime) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(template.getLastUsedAt());
                int month = cal.get(Calendar.MONTH) + 1; // 1-12
                usageByMonth.put(month, usageByMonth.getOrDefault(month, 0) + 1);
            }
        }

        analytics.setUsageByTemplate(usageByTemplate);
        analytics.setUsageByCategory(usageByCategory);
        analytics.setUsageByUser(usageByUser);
        analytics.setUsageByMonth(usageByMonth);

        // 计算统计指标
        analytics.setTotalTemplates(templates.size());
        analytics.setTotalCategories(categoryTemplates.size());
        analytics.setTotalUsage(usageByTemplate.values().stream().mapToInt(Integer::intValue).sum());

        if (!templates.isEmpty()) {
            analytics.setAverageUsage((double) analytics.getTotalUsage() / templates.size());
            analytics.setMostUsedTemplate(findMostUsed(usageByTemplate));
            analytics.setMostUsedCategory(findMostUsed(usageByCategory));
        }

        return analytics;
    }

    // ========== 私有方法 ==========

    private String generateTemplateId(String templateName) {
        String baseId = "template_" + templateName.toLowerCase()
                .replaceAll("[^a-z0-9]", "_")
                .replaceAll("_+", "_");

        // 确保ID唯一
        if (templates.containsKey(baseId)) {
            int suffix = 1;
            while (templates.containsKey(baseId + "_" + suffix)) {
                suffix++;
            }
            return baseId + "_" + suffix;
        }

        return baseId;
    }

    private String generateWorkflowId(String workflowName) {
        return "workflow_" + System.currentTimeMillis() + "_" +
                workflowName.hashCode();
    }

    private String autoCategorize(WorkflowDTO workflow) {
        // 自动分类逻辑
        if (workflow.getNodes() != null) {
            int nodeCount = workflow.getNodes().size();
            if (nodeCount <= 5) {
                return "simple";
            } else if (nodeCount <= 15) {
                return "medium";
            } else {
                return "complex";
            }
        }

        return defaultCategory;
    }

    private List<String> extractTags(WorkflowDTO workflow) {
        List<String> tags = new ArrayList<>();

        // 提取节点类型作为标签
        if (workflow.getNodes() != null) {
            Set<String> nodeTypes = new HashSet<>();
            for (Object node : workflow.getNodes()) {
                if (node instanceof Map) {
                    Map<?, ?> nodeMap = (Map<?, ?>) node;
                    Object type = nodeMap.get("type");
                    if (type instanceof String) {
                        nodeTypes.add(((String) type).toLowerCase());
                    }
                }
            }
            tags.addAll(nodeTypes);
        }

        // 添加复杂度标签
        tags.add(autoCategorize(workflow));

        return tags;
    }

    private Map<String, Object> generateCompatibilityInfo(WorkflowDTO workflow) {
        Map<String, Object> compatibility = new HashMap<>();

        compatibility.put("minVersion", "1.0.0");
        compatibility.put("maxVersion", "2.0.0");
        compatibility.put("nodeTypes", extractNodeTypes(workflow));
        compatibility.put("requires", extractRequirements(workflow));

        return compatibility;
    }

    private Map<String, Object> generateTemplateMetadata(WorkflowDTO workflow) {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("nodeCount", workflow.getNodes() != null ? workflow.getNodes().size() : 0);
        metadata.put("connectionCount", workflow.getConnections() != null ? workflow.getConnections().size() : 0);
        metadata.put("validationCount", workflow.getValidationRules() != null ? workflow.getValidationRules().size() : 0);
        metadata.put("complexity", calculateComplexity(workflow));
        metadata.put("estimatedRuntime", estimateRuntime(workflow));

        return metadata;
    }

    private Set<String> extractNodeTypes(WorkflowDTO workflow) {
        Set<String> nodeTypes = new HashSet<>();

        if (workflow.getNodes() != null) {
            for (Object node : workflow.getNodes()) {
                if (node instanceof Map) {
                    Map<?, ?> nodeMap = (Map<?, ?>) node;
                    Object type = nodeMap.get("type");
                    if (type instanceof String) {
                        nodeTypes.add((String) type);
                    }
                }
            }
        }

        return nodeTypes;
    }

    private List<String> extractRequirements(WorkflowDTO workflow) {
        List<String> requirements = new ArrayList<>();

        // 分析工作流需要的组件
        if (workflow.getConfig() != null && workflow.getConfig().containsKey("requirements")) {
            Object reqs = workflow.getConfig().get("requirements");
            if (reqs instanceof List) {
                requirements.addAll((List<String>) reqs);
            }
        }

        return requirements;
    }

    private double calculateComplexity(WorkflowDTO workflow) {
        // 简化的复杂度计算
        int nodeCount = workflow.getNodes() != null ? workflow.getNodes().size() : 0;
        int connectionCount = workflow.getConnections() != null ? workflow.getConnections().size() : 0;

        return (nodeCount * 0.4) + (connectionCount * 0.6);
    }

    private long estimateRuntime(WorkflowDTO workflow) {
        // 简化的运行时间估算
        int nodeCount = workflow.getNodes() != null ? workflow.getNodes().size() : 0;
        return nodeCount * 1000L; // 假设每个节点需要1秒
    }

    private void saveTemplate(WorkflowTemplateDTO template) {
        // 保存到内存
        templates.put(template.getId(), template);

        // 更新分类映射
        updateTemplateCategory(template.getId(), null, template.getCategory());

        // 限制模板数量
        if (templates.size() > maxTemplates) {
            removeOldestTemplate();
        }
    }

    private void updateTemplateCategory(String templateId, String oldCategory, String newCategory) {
        // 从旧分类移除
        if (oldCategory != null) {
            List<String> oldList = categoryTemplates.get(oldCategory);
            if (oldList != null) {
                oldList.remove(templateId);
                if (oldList.isEmpty()) {
                    categoryTemplates.remove(oldCategory);
                }
            }
        }

        // 添加到新分类
        List<String> newList = categoryTemplates.computeIfAbsent(
                newCategory, k -> new ArrayList<>());

        if (!newList.contains(templateId)) {
            newList.add(templateId);
        }
    }

    private void removeTemplateFromCategory(String templateId, String category) {
        List<String> list = categoryTemplates.get(category);
        if (list != null) {
            list.remove(templateId);
            if (list.isEmpty()) {
                categoryTemplates.remove(category);
            }
        }
    }

    private void removeOldestTemplate() {
        // 找到最久未使用的模板
        WorkflowTemplateDTO oldestTemplate = null;
        for (WorkflowTemplateDTO template : templates.values()) {
            if (oldestTemplate == null ||
                    template.getLastUsedAt() < oldestTemplate.getLastUsedAt()) {
                oldestTemplate = template;
            }
        }

        if (oldestTemplate != null) {
            deleteTemplate(oldestTemplate.getId());
        }
    }

    private WorkflowDTO deepClone(WorkflowDTO original) {
        String json = JsonUtil.toJson(original);
        return JsonUtil.fromJson(json, WorkflowDTO.class);
    }

    private void applyTemplateParameters(WorkflowDTO workflow, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        // 应用名称参数
        if (parameters.containsKey("name") && workflow.getName() != null) {
            String namePattern = workflow.getName();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                namePattern = namePattern.replace("{{" + entry.getKey() + "}}",
                        entry.getValue().toString());
            }
            workflow.setName(namePattern);
        }

        // 应用其他参数
        if (workflow.getConfig() == null) {
            workflow.setConfig(new HashMap<>());
        }

        workflow.getConfig().putAll(parameters);
    }

    private boolean matchesCriteria(WorkflowTemplateDTO template, TemplateSearchCriteria criteria) {
        // 名称匹配
        if (criteria.getName() != null &&
                !template.getName().toLowerCase().contains(criteria.getName().toLowerCase())) {
            return false;
        }

        // 分类匹配
        if (criteria.getCategory() != null &&
                !template.getCategory().equalsIgnoreCase(criteria.getCategory())) {
            return false;
        }

        // 标签匹配
        if (criteria.getTags() != null && !criteria.getTags().isEmpty()) {
            if (template.getTags() == null) {
                return false;
            }
            for (String tag : criteria.getTags()) {
                if (!template.getTags().contains(tag.toLowerCase())) {
                    return false;
                }
            }
        }

        // 最小评分
        if (criteria.getMinRating() > 0 && template.getRating() < criteria.getMinRating()) {
            return false;
        }

        // 最小使用次数
        if (criteria.getMinUsage() > 0 && template.getUsageCount() < criteria.getMinUsage()) {
            return false;
        }

        // 兼容性检查
        if (criteria.getCompatibility() != null) {
            Map<String, Object> compat = template.getCompatibility();
            if (compat == null || !checkCompatibility(compat, criteria.getCompatibility())) {
                return false;
            }
        }

        return true;
    }

    private boolean checkCompatibility(Map<String, Object> templateCompat,
                                       Map<String, Object> requiredCompat) {
        // 简化的兼容性检查
        for (Map.Entry<String, Object> entry : requiredCompat.entrySet()) {
            if (!templateCompat.containsKey(entry.getKey()) ||
                    !templateCompat.get(entry.getKey()).equals(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private Comparator<WorkflowTemplateDTO> getTemplateComparator(String sortBy, String sortOrder) {
        Comparator<WorkflowTemplateDTO> comparator;

        switch (sortBy != null ? sortBy : "rating") {
            case "name":
                comparator = Comparator.comparing(WorkflowTemplateDTO::getName);
                break;
            case "usage":
                comparator = Comparator.comparing(WorkflowTemplateDTO::getUsageCount);
                break;
            case "createdAt":
                comparator = Comparator.comparing(WorkflowTemplateDTO::getCreatedAt);
                break;
            case "rating":
            default:
                comparator = Comparator.comparing(WorkflowTemplateDTO::getRating);
        }

        if ("desc".equalsIgnoreCase(sortOrder)) {
            comparator = comparator.reversed();
        }

        return comparator;
    }

    private List<WorkflowTemplateDTO> getPopularTemplates(int limit) {
        return templates.values().stream()
                .sorted((t1, t2) -> Integer.compare(t2.getUsageCount(), t1.getUsageCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<WorkflowTemplateDTO> getHighlyRatedTemplates(int limit) {
        return templates.values().stream()
                .filter(t -> t.getRating() >= 4.0)
                .sorted((t1, t2) -> Double.compare(t2.getRating(), t1.getRating()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<WorkflowTemplateDTO> getTemplatesByCategory(String category, int limit) {
        List<String> templateIds = categoryTemplates.get(category);
        if (templateIds == null) {
            return Collections.emptyList();
        }

        return templateIds.stream()
                .map(templates::get)
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<WorkflowTemplateDTO> getSimilarTemplates(String workflowId, int limit) {
        // 简化的相似度计算
        WorkflowDTO referenceWorkflow = offlineDataManager.loadWorkflow(workflowId);
        if (referenceWorkflow == null) {
            return Collections.emptyList();
        }

        return templates.values().stream()
                .sorted((t1, t2) -> Double.compare(
                        calculateSimilarity(t2.getWorkflowData(), referenceWorkflow),
                        calculateSimilarity(t1.getWorkflowData(), referenceWorkflow)
                ))
                .limit(limit)
                .collect(Collectors.toList());
    }

    private double calculateSimilarity(WorkflowDTO w1, WorkflowDTO w2) {
        // 简化的相似度计算
        int commonNodes = countCommonElements(w1.getNodes(), w2.getNodes());
        int totalNodes = Math.max(countElements(w1.getNodes()), countElements(w2.getNodes()));

        return totalNodes > 0 ? (double) commonNodes / totalNodes : 0.0;
    }

    private int countCommonElements(List<?> list1, List<?> list2) {
        if (list1 == null || list2 == null) {
            return 0;
        }

        // 简化的公共元素计数
        Set<Object> set1 = new HashSet<>(list1);
        Set<Object> set2 = new HashSet<>(list2);
        set1.retainAll(set2);
        return set1.size();
    }

    private int countElements(List<?> list) {
        return list != null ? list.size() : 0;
    }

    private String findMostUsed(Map<String, Integer> usageMap) {
        return usageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private String exportToXml(WorkflowTemplateDTO template) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<workflowTemplate id=\"").append(template.getId()).append("\">\n");
        xml.append("  <name>").append(escapeXml(template.getName())).append("</name>\n");
        xml.append("  <description>").append(escapeXml(template.getDescription())).append("</description>\n");
        xml.append("  <category>").append(template.getCategory()).append("</category>\n");
        xml.append("  <version>").append(template.getVersion()).append("</version>\n");
        xml.append("  <createdBy>").append(template.getCreatedBy()).append("</createdBy>\n");
        xml.append("  <createdAt>").append(template.getCreatedAt()).append("</createdAt>\n");
        xml.append("</workflowTemplate>");
        return xml.toString();
    }

    private String exportToYaml(WorkflowTemplateDTO template) {
        StringBuilder yaml = new StringBuilder();
        yaml.append("id: ").append(template.getId()).append("\n");
        yaml.append("name: ").append(template.getName()).append("\n");
        yaml.append("description: ").append(template.getDescription()).append("\n");
        yaml.append("category: ").append(template.getCategory()).append("\n");
        yaml.append("version: ").append(template.getVersion()).append("\n");
        yaml.append("createdBy: ").append(template.getCreatedBy()).append("\n");
        yaml.append("createdAt: ").append(template.getCreatedAt()).append("\n");
        return yaml.toString();
    }

    private WorkflowTemplateDTO importFromXml(String xmlData) {
        // 简化的XML导入
        // 在实际应用中，应该使用XML解析器
        WorkflowTemplateDTO template = new WorkflowTemplateDTO();

        // 解析XML的简化实现
        if (xmlData.contains("<name>")) {
            int start = xmlData.indexOf("<name>") + 6;
            int end = xmlData.indexOf("</name>", start);
            template.setName(xmlData.substring(start, end));
        }

        // 类似解析其他字段...

        return template;
    }

    private WorkflowTemplateDTO importFromYaml(String yamlData) {
        // 简化的YAML导入
        // 在实际应用中，应该使用YAML解析器
        WorkflowTemplateDTO template = new WorkflowTemplateDTO();

        String[] lines = yamlData.split("\n");
        for (String line : lines) {
            if (line.startsWith("name:")) {
                template.setName(line.substring(5).trim());
            }
            // 类似解析其他字段...
        }

        return template;
    }

    private void validateTemplate(WorkflowTemplateDTO template) {
        if (template.getName() == null || template.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("模板名称不能为空");
        }

        if (template.getWorkflowData() == null) {
            throw new IllegalArgumentException("模板工作流数据不能为空");
        }

        if (template.getCategory() == null) {
            template.setCategory(defaultCategory);
        }
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // ========== 内部类 ==========

    /**
     * 模板搜索条件
     */
    @Data
    public static class TemplateSearchCriteria {
        private String name;
        private String category;
        private List<String> tags;
        private double minRating;
        private int minUsage;
        private Map<String, Object> compatibility;
        private String sortBy;
        private String sortOrder;
        private int page;
        private int size;

        public TemplateSearchCriteria() {
            this.page = 0;
            this.size = 20;
            this.sortBy = "rating";
            this.sortOrder = "desc";
        }
    }

    /**
     * 模板分类统计
     */
    @Data
    public static class TemplateCategoryStats {
        private final String category;
        private int templateCount;
        private int totalUsage;
        private double averageRating;
        private int ratedCount;

        public TemplateCategoryStats(String category) {
            this.category = category;
        }
    }

    /**
     * 模板评分
     */
    @Data
    public static class TemplateRating {
        private int rating;
        private String comment;
        private String ratedBy;
        private long ratedAt;
    }

    /**
     * 模板导出
     */
    @Data
    public static class TemplateExport {
        private WorkflowTemplateDTO template;
        private long exportTime;
        private ExportFormat format;
        private String data;
    }

    /**
     * 导入导出格式
     */
    public enum ExportFormat {
        JSON, XML, YAML
    }

    public enum ImportFormat {
        JSON, XML, YAML
    }

    /**
     * 推荐条件
     */
    @Data
    public static class RecommendationCriteria {
        private boolean basedOnUsage = true;
        private boolean basedOnRating = true;
        private String category;
        private String referenceWorkflowId;
        private int limit = 10;
    }

    /**
     * 模板分析
     */
    @Data
    public static class TemplateAnalytics {
        private long analysisTime;
        private AnalyticsCriteria criteria;
        private int totalTemplates;
        private int totalCategories;
        private int totalUsage;
        private double averageUsage;
        private String mostUsedTemplate;
        private String mostUsedCategory;
        private Map<String, Integer> usageByTemplate;
        private Map<String, Integer> usageByCategory;
        private Map<String, Integer> usageByUser;
        private Map<Integer, Integer> usageByMonth;

        public TemplateAnalytics() {
            this.usageByTemplate = new HashMap<>();
            this.usageByCategory = new HashMap<>();
            this.usageByUser = new HashMap<>();
            this.usageByMonth = new HashMap<>();
        }
    }

    /**
     * 分析条件
     */
    @Data
    public static class AnalyticsCriteria {
        private Long startTime;
        private Long endTime;
        private String category;
        private String createdBy;
    }
}