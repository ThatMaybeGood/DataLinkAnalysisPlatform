package com.workflow.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.annotation.RequireMode;
import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.dto.WorkflowQueryDTO;
import com.workflow.platform.model.vo.WorkflowVO;
import com.workflow.platform.repository.OfflineWorkflowRepository;
import com.workflow.platform.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 离线工作流服务实现
 * 使用本地文件系统存储数据，支持离线环境下的工作流操作
 */
@Service
@RequiredArgsConstructor
@Slf4j
@RequireMode("offline")
public class OfflineWorkflowServiceImpl implements WorkflowService {

    private final OfflineWorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.offline.storage.file.path:./data/workflows}")
    private String workflowsDir;

    @Value("${app.offline.storage.file.path:./data/nodes}")
    private String nodesDir;

    @Value("${app.offline.storage.file.path:./data/rules}")
    private String rulesDir;

    @Value("${app.offline.storage.file.path:./data/exports}")
    private String exportsDir;

    // ========== 基础CRUD操作 ==========

    @Override
    public WorkflowVO createWorkflow(WorkflowDTO workflowDTO) {
        log.info("创建离线工作流: {}", workflowDTO.getName());

        try {
            // 1. 验证数据
            String validationError = validateWorkflow(workflowDTO);
            if (validationError != null) {
                throw new IllegalArgumentException("工作流数据验证失败: " + validationError);
            }

            // 2. 设置离线模式特定字段
            workflowDTO.setMode("offline");
            workflowDTO.setCreatedAt(LocalDateTime.now());
            workflowDTO.setCreatedBy("offline_user");

            // 3. 保存到文件系统
            WorkflowDTO savedWorkflow = workflowRepository.save(workflowDTO);

            // 4. 转换为VO并返回
            WorkflowVO vo = convertToVO(savedWorkflow);
            log.info("离线工作流创建成功: {}, ID: {}", vo.getName(), vo.getId());

            return vo;

        } catch (IOException e) {
            log.error("创建离线工作流失败: {}", workflowDTO.getName(), e);
            throw new RuntimeException("创建离线工作流失败: " + e.getMessage(), e);
        }
    }

    @Override
    public WorkflowVO updateWorkflow(String id, WorkflowDTO workflowDTO) {
        log.info("更新离线工作流: {}", id);

        try {
            // 1. 查找现有工作流
            Optional<WorkflowDTO> existingWorkflow = workflowRepository.findById(id);
            if (!existingWorkflow.isPresent()) {
                throw new ResourceNotFoundException("工作流不存在: " + id);
            }

            // 2. 验证数据
            String validationError = validateWorkflow(workflowDTO);
            if (validationError != null) {
                throw new IllegalArgumentException("工作流数据验证失败: " + validationError);
            }

            // 3. 保留原始创建信息
            workflowDTO.setId(id);
            workflowDTO.setCreatedAt(existingWorkflow.get().getCreatedAt());
            workflowDTO.setCreatedBy(existingWorkflow.get().getCreatedBy());
            workflowDTO.setUpdatedAt(LocalDateTime.now());
            workflowDTO.setUpdatedBy("offline_user");
            workflowDTO.setMode("offline");

            // 4. 保存更新
            WorkflowDTO updatedWorkflow = workflowRepository.save(workflowDTO);

            // 5. 转换为VO并返回
            WorkflowVO vo = convertToVO(updatedWorkflow);
            log.info("离线工作流更新成功: {}, ID: {}", vo.getName(), vo.getId());

            return vo;

        } catch (IOException e) {
            log.error("更新离线工作流失败: {}", id, e);
            throw new RuntimeException("更新离线工作流失败: " + e.getMessage(), e);
        }
    }

    @Override
    public WorkflowVO getWorkflow(String id) {
        log.info("获取离线工作流详情: {}", id);

        WorkflowDTO workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + id));

        WorkflowVO vo = convertToVO(workflow);

        // 添加统计信息
        addStatistics(vo);

        return vo;
    }

    @Override
    public void deleteWorkflow(String id) {
        log.info("删除离线工作流: {}", id);

        try {
            // 删除工作流文件
            workflowRepository.deleteById(id);

            // 删除相关节点文件
            deleteRelatedFiles(id);

            log.info("离线工作流删除成功: {}", id);

        } catch (Exception e) {
            log.error("删除离线工作流失败: {}", id, e);
            throw new RuntimeException("删除离线工作流失败: " + e.getMessage(), e);
        }
    }

    // ========== 查询操作 ==========

    @Override
    public Page<WorkflowVO> queryWorkflows(WorkflowQueryDTO queryDTO, Pageable pageable) {
        log.info("查询离线工作流列表, 参数: {}", queryDTO);

        // 1. 从文件系统加载所有工作流
        List<WorkflowDTO> allWorkflows = workflowRepository.findAll();

        // 2. 应用过滤条件
        List<WorkflowDTO> filteredWorkflows = filterWorkflows(allWorkflows, queryDTO);

        // 3. 排序
        filteredWorkflows.sort((w1, w2) -> {
            if ("desc".equals(queryDTO.getSortOrder())) {
                return w2.getUpdatedAt().compareTo(w1.getUpdatedAt());
            } else {
                return w1.getUpdatedAt().compareTo(w2.getUpdatedAt());
            }
        });

        // 4. 分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredWorkflows.size());

        List<WorkflowVO> pageContent = filteredWorkflows.subList(start, end)
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, filteredWorkflows.size());
    }

    @Override
    public WorkflowVO getWorkflowByAlias(String alias) {
        log.info("根据别名获取离线工作流: {}", alias);

        WorkflowDTO workflow = workflowRepository.findByAlias(alias)
                .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + alias));

        return convertToVO(workflow);
    }

    @Override
    public Page<WorkflowVO> searchWorkflows(String keyword, Pageable pageable) {
        return null;
    }

    @Override
    public Page<WorkflowVO> getWorkflowsByCategory(String category, Pageable pageable) {
        return null;
    }

    @Override
    public Page<WorkflowVO> getWorkflowsByStatus(String status, Pageable pageable) {
        return null;
    }

    // ========== 执行操作 ==========

    @Override
    public Object executeWorkflow(String id, Object parameters) {
        log.info("执行离线工作流: {}", id);

        try {
            // 1. 获取工作流
            WorkflowDTO workflow = workflowRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + id));

            // 2. 创建执行记录
            String executionId = generateExecutionId();
            Map<String, Object> execution = new HashMap<>();
            execution.put("id", executionId);
            execution.put("workflowId", id);
            execution.put("workflowName", workflow.getName());
            execution.put("parameters", parameters);
            execution.put("status", "running");
            execution.put("startTime", LocalDateTime.now());
            execution.put("createdBy", "offline_user");

            // 3. 加载相关节点
            List<Map<String, Object>> nodes = loadNodesByWorkflowId(id);

            // 4. 执行每个节点
            List<Map<String, Object>> executionSteps = new ArrayList<>();
            for (Map<String, Object> node : nodes) {
                Map<String, Object> stepResult = executeNode(node, parameters);
                executionSteps.add(stepResult);
            }

            // 5. 更新执行记录
            execution.put("status", "completed");
            execution.put("endTime", LocalDateTime.now());
            execution.put("steps", executionSteps);
            execution.put("result", Map.of(
                    "success", true,
                    "totalSteps", executionSteps.size(),
                    "successSteps", executionSteps.stream()
                            .filter(step -> "completed".equals(step.get("status")))
                            .count()
            ));

            // 6. 保存执行记录
            saveExecutionRecord(execution);

            // 7. 更新工作流统计信息
            updateWorkflowStatistics(workflow, execution);

            log.info("离线工作流执行成功: {}, 执行ID: {}", id, executionId);

            return execution;

        } catch (Exception e) {
            log.error("离线工作流执行失败: {}", id, e);
            throw new BusinessException("工作流执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object> batchExecuteWorkflows(List<String> ids, Object parameters) {
        return null;
    }

    @Override
    public Object testWorkflow(String id, Object parameters) {
        return null;
    }

    // ========== 导入导出操作 ==========

    @Override
    public String exportWorkflow(String id) {
        log.info("导出离线工作流: {}", id);

        try {
            // 1. 获取工作流
            WorkflowDTO workflow = workflowRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + id));

            // 2. 加载相关节点
            List<Map<String, Object>> nodes = loadNodesByWorkflowId(id);

            // 3. 加载相关规则
            List<Map<String, Object>> rules = new ArrayList<>();
            for (Map<String, Object> node : nodes) {
                String nodeId = (String) node.get("id");
                rules.addAll(loadRulesByNodeId(nodeId));
            }

            // 4. 构建导出数据
            Map<String, Object> exportData = new HashMap<>();
            exportData.put("version", "1.0.0");
            exportData.put("exportTime", LocalDateTime.now());
            exportData.put("workflow", workflow);
            exportData.put("nodes", nodes);
            exportData.put("validationRules", rules);
            exportData.put("metadata", Map.of(
                    "workflowName", workflow.getName(),
                    "nodeCount", nodes.size(),
                    "ruleCount", rules.size(),
                    "exportFormat", "json",
                    "exportMode", "offline"
            ));

            // 5. 保存到文件
            String exportFileName = "workflow_export_" + id + "_" + System.currentTimeMillis() + ".json";
            Path exportPath = Paths.get(exportsDir, exportFileName);

            Files.createDirectories(exportPath.getParent());
            objectMapper.writeValue(exportPath.toFile(), exportData);

            log.info("离线工作流导出成功: {}, 文件: {}", id, exportPath.toString());

            return exportPath.toString();

        } catch (Exception e) {
            log.error("导出离线工作流失败: {}", id, e);
            throw new RuntimeException("导出工作流失败: " + e.getMessage(), e);
        }
    }

    @Override
    public WorkflowVO importWorkflow(String filePath) {
        log.info("导入离线工作流文件: {}", filePath);

        try {
            // 1. 读取导入文件
            Path importPath = Paths.get(filePath);
            Map<String, Object> importData = objectMapper.readValue(importPath.toFile(), Map.class);

            // 2. 验证导入数据
            if (!importData.containsKey("workflow")) {
                throw new IllegalArgumentException("无效的导入文件: 缺少workflow数据");
            }

            // 3. 提取工作流数据
            Map<String, Object> workflowMap = (Map<String, Object>) importData.get("workflow");
            WorkflowDTO workflowDTO = objectMapper.convertValue(workflowMap, WorkflowDTO.class);

            // 4. 设置导入相关字段
            workflowDTO.setId(null); // 生成新ID
            workflowDTO.setImportedAt(LocalDateTime.now());
            workflowDTO.setMode("offline");

            // 5. 创建工作流
            WorkflowVO result = createWorkflow(workflowDTO);

            // 6. 导入节点（如果存在）
            if (importData.containsKey("nodes")) {
                List<Map<String, Object>> nodes = (List<Map<String, Object>>) importData.get("nodes");
                for (Map<String, Object> node : nodes) {
                    node.put("workflowId", result.getId());
                    node.put("id", generateNodeId());
                    saveNodeToFile(node);
                }
            }

            // 7. 导入规则（如果存在）
            if (importData.containsKey("validationRules")) {
                List<Map<String, Object>> rules = (List<Map<String, Object>>) importData.get("validationRules");
                for (Map<String, Object> rule : rules) {
                    rule.put("id", generateRuleId());
                    saveRuleToFile(rule);
                }
            }

            log.info("离线工作流导入成功: {}, 来源文件: {}", result.getId(), filePath);

            return result;

        } catch (Exception e) {
            log.error("导入离线工作流失败: {}", filePath, e);
            throw new RuntimeException("导入工作流失败: " + e.getMessage(), e);
        }
    }

    @Override
    public WorkflowVO cloneWorkflow(String id, String newName) {
        return null;
    }

    @Override
    public Object getStatistics() {
        return null;
    }

    @Override
    public List<Object> getCategoryStatistics() {
        return null;
    }

    @Override
    public List<Object> getStatusStatistics() {
        return null;
    }

    @Override
    public int batchUpdateStatus(List<String> ids, String status) {
        return 0;
    }

    @Override
    public int batchDeleteWorkflows(List<String> ids) {
        return 0;
    }

    // ========== 工具方法 ==========

    @Override
    public String getMode() {
        return "offline";
    }

    @Override
    public String validateWorkflow(WorkflowDTO workflowDTO) {
        return workflowDTO.validate();
    }

    @Override
    public List<Object> getWorkflowNodes(String workflowId) {
        return null;
    }

    @Override
    public Page<Object> getWorkflowExecutions(String workflowId, Pageable pageable) {
        return null;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 过滤工作流列表
     */
    private List<WorkflowDTO> filterWorkflows(List<WorkflowDTO> workflows, WorkflowQueryDTO queryDTO) {
        return workflows.stream()
                .filter(workflow -> {
                    if (StringUtils.hasText(queryDTO.getName())) {
                        return workflow.getName().contains(queryDTO.getName());
                    }
                    return true;
                })
                .filter(workflow -> {
                    if (StringUtils.hasText(queryDTO.getAlias())) {
                        return queryDTO.getAlias().equals(workflow.getAlias());
                    }
                    return true;
                })
                .filter(workflow -> {
                    if (StringUtils.hasText(queryDTO.getCategory())) {
                        return queryDTO.getCategory().equals(workflow.getCategory());
                    }
                    return true;
                })
                .filter(workflow -> {
                    if (StringUtils.hasText(queryDTO.getStatus())) {
                        return queryDTO.getStatus().equals(workflow.getStatus());
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 转换DTO到VO
     */
    private WorkflowVO convertToVO(WorkflowDTO dto) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setAlias(dto.getAlias());
        vo.setDescription(dto.getDescription());
        vo.setCategory(dto.getCategory());
        vo.setTags(dto.getTags());
        vo.setStatus(dto.getStatus());
        vo.setMode(dto.getMode());
        vo.setNodeCount(dto.getNodeCount());
        vo.setExecutionCount(dto.getExecutionCount());
        vo.setSuccessRate(dto.getSuccessRate());
        vo.setAvgDuration(dto.getAvgDuration());
        vo.setCreatedAt(dto.getCreatedAt());
        vo.setUpdatedAt(dto.getUpdatedAt());
        vo.setCreatedBy(dto.getCreatedBy());
        vo.setUpdatedBy(dto.getUpdatedBy());
        return vo;
    }

    /**
     * 加载工作流的节点
     */
    private List<Map<String, Object>> loadNodesByWorkflowId(String workflowId) {
        List<Map<String, Object>> nodes = new ArrayList<>();
        File dir = new File(nodesDir);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        Map<String, Object> node = objectMapper.readValue(file, Map.class);
                        if (workflowId.equals(node.get("workflowId"))) {
                            nodes.add(node);
                        }
                    } catch (IOException e) {
                        log.error("读取节点文件失败: {}", file.getName(), e);
                    }
                }
            }
        }

        return nodes;
    }

    /**
     * 加载节点的验证规则
     */
    private List<Map<String, Object>> loadRulesByNodeId(String nodeId) {
        List<Map<String, Object>> rules = new ArrayList<>();
        File dir = new File(rulesDir);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        Map<String, Object> rule = objectMapper.readValue(file, Map.class);
                        if (nodeId.equals(rule.get("nodeId"))) {
                            rules.add(rule);
                        }
                    } catch (IOException e) {
                        log.error("读取规则文件失败: {}", file.getName(), e);
                    }
                }
            }
        }

        return rules;
    }

    /**
     * 保存节点到文件
     */
    private void saveNodeToFile(Map<String, Object> node) throws IOException {
        String nodeId = (String) node.get("id");
        Path filePath = Paths.get(nodesDir, nodeId + ".json");
        Files.createDirectories(filePath.getParent());
        objectMapper.writeValue(filePath.toFile(), node);
    }

    /**
     * 保存规则到文件
     */
    private void saveRuleToFile(Map<String, Object> rule) throws IOException {
        String ruleId = (String) rule.get("id");
        Path filePath = Paths.get(rulesDir, ruleId + ".json");
        Files.createDirectories(filePath.getParent());
        objectMapper.writeValue(filePath.toFile(), rule);
    }

    /**
     * 保存执行记录
     */
    private void saveExecutionRecord(Map<String, Object> execution) {
        try {
            String execId = (String) execution.get("id");
            Path execDir = Paths.get("./data/executions");
            Path execPath = execDir.resolve(execId + ".json");

            Files.createDirectories(execDir);
            objectMapper.writeValue(execPath.toFile(), execution);
        } catch (IOException e) {
            log.error("保存执行记录失败", e);
        }
    }

    /**
     * 执行节点
     */
    private Map<String, Object> executeNode(Map<String, Object> node, Object parameters) {
        // 模拟节点执行
        Map<String, Object> result = new HashMap<>();
        result.put("nodeId", node.get("id"));
        result.put("nodeName", node.get("name"));
        result.put("nodeType", node.get("type"));
        result.put("status", "completed");
        result.put("startTime", LocalDateTime.now());
        result.put("endTime", LocalDateTime.now());
        result.put("duration", (int) (Math.random() * 1000));

        // 模拟验证规则执行
        String nodeId = (String) node.get("id");
        List<Map<String, Object>> rules = loadRulesByNodeId(nodeId);
        List<Map<String, Object>> ruleResults = new ArrayList<>();

        for (Map<String, Object> rule : rules) {
            Map<String, Object> ruleResult = executeValidationRule(rule, parameters);
            ruleResults.add(ruleResult);
        }

        result.put("validationResults", ruleResults);
        return result;
    }

    /**
     * 执行验证规则
     */
    private Map<String, Object> executeValidationRule(Map<String, Object> rule, Object parameters) {
        // 模拟验证规则执行
        Map<String, Object> result = new HashMap<>();
        result.put("ruleId", rule.get("id"));
        result.put("ruleName", rule.get("name"));
        result.put("passed", Math.random() > 0.3); // 70%通过率
        result.put("message", (Boolean) result.get("passed") ? "验证通过" : "验证失败");
        result.put("executionTime", LocalDateTime.now());
        return result;
    }

    /**
     * 删除相关文件
     */
    private void deleteRelatedFiles(String workflowId) {
        // 删除节点文件
        deleteFilesInDirectory(nodesDir, workflowId + "_*.json");

        // 删除规则文件
        deleteFilesInDirectory(rulesDir, workflowId + "_*.json");

        // 删除执行记录文件
        deleteFilesInDirectory("./data/executions", "exec_*_" + workflowId + "_*.json");
    }

    /**
     * 删除目录中匹配模式的文件
     */
    private void deleteFilesInDirectory(String directory, String pattern) {
        try {
            Path dirPath = Paths.get(directory);
            if (!Files.exists(dirPath)) {
                return;
            }

            Files.list(dirPath)
                    .filter(path -> path.getFileName().toString().matches(pattern.replace("*", ".*")))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除相关文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.warn("遍历目录失败: {}", directory, e);
        }
    }

    /**
     * 添加统计信息
     */
    private void addStatistics(WorkflowVO vo) {
        WorkflowVO.StatisticsVO stats = new WorkflowVO.StatisticsVO();

        // 这里可以添加离线模式下的统计逻辑
        // 例如：从执行记录文件中统计

        vo.setStatistics(stats);
    }

    /**
     * 更新工作流统计信息
     */
    private void updateWorkflowStatistics(WorkflowDTO workflow, Map<String, Object> execution) {
        try {
            // 更新执行次数
            workflow.setExecutionCount(workflow.getExecutionCount() + 1);

            // 更新成功率（简化计算）
            if ("completed".equals(execution.get("status"))) {
                // 这里应该有更复杂的成功率计算逻辑
                workflow.setSuccessRate(95.5);
            }

            workflow.setUpdatedAt(LocalDateTime.now());
            workflow.setUpdatedBy("offline_user");

            workflowRepository.save(workflow);

        } catch (IOException e) {
            log.error("更新工作流统计信息失败: {}", workflow.getId(), e);
        }
    }

    /**
     * 生成唯一ID
     */
    private String generateExecutionId() {
        return "exec_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateNodeId() {
        return "node_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    private String generateRuleId() {
        return "rule_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    // ========== 异常类 ==========

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }

        public BusinessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}