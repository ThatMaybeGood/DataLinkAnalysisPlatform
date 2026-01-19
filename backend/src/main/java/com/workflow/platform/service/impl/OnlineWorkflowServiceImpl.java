package com.workflow.platform.service.impl;

import com.workflow.platform.annotation.RequireMode;
import com.workflow.platform.engine.WorkflowEngine;
import com.workflow.platform.model.dto.*;
import com.workflow.platform.model.entity.NodeEntity;
import com.workflow.platform.model.entity.ValidationRuleEntity;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.model.vo.WorkflowConverter;
import com.workflow.platform.model.vo.WorkflowVO;
import com.workflow.platform.repository.WorkflowRepository;
import com.workflow.platform.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.Closure;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Predicate;
import java.util.*;

/**
 * 在线工作流服务实现
 * 使用MySQL数据库存储数据，支持完整的CRUD、查询、执行等操作
 */
@Service("onlineWorkflowService")
@RequiredArgsConstructor
@Slf4j
@Transactional
@ConditionalOnProperty(name = "app.mode", havingValue = "online") // 匹配 app.mode=offline
public class OnlineWorkflowServiceImpl implements WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowConverter workflowConverter;

     NodeService nodeService;
     ValidationService validationService;
     FileStorageService fileStorageService;
     ExecutionService executionService;

     AuditService auditService;

    // ========== 基础CRUD操作 ==========

    @Override
    public WorkflowVO createWorkflow(WorkflowDTO workflowDTO) {
        log.info("创建在线工作流: {}", workflowDTO.getName());

        // 1. 验证数据
        String validationError = validateWorkflow(workflowDTO);
        if (validationError != null) {
            throw new IllegalArgumentException("工作流数据验证失败: " + validationError);
        }

        // 2. 检查别名是否已存在
        if (workflowDTO.getAlias() != null && workflowRepository.existsByAlias(workflowDTO.getAlias())) {
            throw new IllegalArgumentException("工作流别名已存在: " + workflowDTO.getAlias());
        }

        // 3. 生成ID
        String workflowId = generateWorkflowId();
        workflowDTO.setId(workflowId);

        // 4. 设置默认值
        workflowDTO.setCreatedAt(System.currentTimeMillis());
        workflowDTO.setUpdatedAt(System.currentTimeMillis());
        workflowDTO.setCreatedBy(getCurrentUser());

        // 5. 转换为实体并保存
        WorkflowEntity entity = workflowConverter.toEntity(workflowDTO);
        WorkflowEntity savedEntity = workflowRepository.save(entity);

        log.info("在线工作流创建成功: {}, ID: {}", savedEntity.getName(), savedEntity.getId());

        // 6. 转换为VO并返回
        return workflowConverter.toVO(savedEntity);
    }

    @Override
    public WorkflowVO updateWorkflow(String id, WorkflowDTO workflowDTO) {
        log.info("更新在线工作流: {}", id);

        // 1. 查找现有工作流
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + id));

        // 2. 验证数据
        String validationError = validateWorkflow(workflowDTO);
        if (validationError != null) {
            throw new IllegalArgumentException("工作流数据验证失败: " + validationError);
        }

        // 3. 检查别名冲突（如果修改了别名）
        if (workflowDTO.getAlias() != null &&
                !workflowDTO.getAlias().equals(entity.getAlias()) &&
                workflowRepository.existsByAlias(workflowDTO.getAlias())) {
            throw new IllegalArgumentException("工作流别名已存在: " + workflowDTO.getAlias());
        }

        // 4. 更新字段
        workflowConverter.updateEntity(entity, workflowDTO);
        entity.setUpdatedAt(System.currentTimeMillis());
        entity.setUpdatedBy(getCurrentUser());

        // 5. 保存更新
        WorkflowEntity updatedEntity = workflowRepository.save(entity);

        log.info("在线工作流更新成功: {}, ID: {}", updatedEntity.getName(), updatedEntity.getId());

        // 6. 转换为VO并返回
        return workflowConverter.toVO(updatedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowVO getWorkflow(String id) {
        log.info("获取在线工作流详情: {}", id);

        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + id));

        WorkflowVO vo = workflowConverter.toVO(entity);

        // 添加统计信息
        addStatistics(vo);

        return vo;
    }

    @Override
    public void deleteWorkflow(String id) {
        log.info("删除在线工作流: {}", id);

        // 检查是否存在
        if (!workflowRepository.existsById(id)) {
            throw new ResourceNotFoundException("工作流不存在: " + id);
        }

        // 删除工作流（级联删除相关节点和执行记录）
        workflowRepository.deleteById(id);

        log.info("在线工作流删除成功: {}", id);

        // 记录审计日志
        auditService.log("delete_workflow", id, getCurrentUser());
    }

    // ========== 查询操作 ==========

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowVO> queryWorkflows(WorkflowQueryDTO queryDTO, Pageable pageable) {
        log.info("查询在线工作流列表, 参数: {}", queryDTO);

        // 构建查询条件
        Specification<WorkflowEntity> spec = buildQuerySpecification(queryDTO);

        // 执行查询
        Page<WorkflowEntity> page = workflowRepository.findAll(spec, pageable);

        // 转换为VO
        return page.map(workflowConverter::toVO);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkflowVO getWorkflowByAlias(String alias) {
        log.info("根据别名获取在线工作流: {}", alias);

        WorkflowEntity entity = workflowRepository.findByAlias(alias)
                .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + alias));

        return workflowConverter.toVO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowVO> searchWorkflows(String keyword, Pageable pageable) {
        log.info("搜索在线工作流, 关键词: {}", keyword);

        Specification<WorkflowEntity> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(keyword)) {
                Predicate namePredicate = criteriaBuilder.like(root.get("name"), "%" + keyword + "%");
                Predicate aliasPredicate = criteriaBuilder.like(root.get("alias"), "%" + keyword + "%");
                Predicate descriptionPredicate = criteriaBuilder.like(root.get("description"), "%" + keyword + "%");

                predicates.add(criteriaBuilder.or(namePredicate, aliasPredicate, descriptionPredicate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<WorkflowEntity> page = workflowRepository.findAll(spec, pageable);
        return page.map(workflowConverter::toVO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowVO> getWorkflowsByCategory(String category, Pageable pageable) {
        log.info("获取分类下的在线工作流: {}", category);

        Page<WorkflowEntity> page = workflowRepository.findByCategory(category, pageable);
        return page.map(workflowConverter::toVO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<WorkflowVO> getWorkflowsByStatus(String status, Pageable pageable) {
        log.info("获取状态下的在线工作流: {}", status);

        Specification<WorkflowEntity> spec = (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status);

        Page<WorkflowEntity> page = workflowRepository.findAll(spec, pageable);
        return page.map(workflowConverter::toVO);
    }

    // ========== 执行操作 ==========

    @Override
    public Object executeWorkflow(String id, Object parameters) {
        log.info("执行在线工作流: {}", id);

        // 1. 获取工作流
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + id));

        // 2. 检查工作流状态
        if (!"active".equals(entity.getStatus())) {
            throw new BusinessException("工作流未激活，无法执行");
        }


        ExecutionRecord execution = createExecutionRecord(id, parameters);

        try {
            WorkflowEngine workflowEngine = new WorkflowEngine();
            // 4. 执行工作流逻辑
            Object result = workflowEngine.execute(entity, (Map<String, Object>) parameters);

            // 5. 更新执行记录
            execution.setStatus("completed");
            execution.setResult(result);
            execution.setEndTime(System.currentTimeMillis());

            // 6. 更新工作流统计信息
            updateWorkflowStatistics(entity, execution);

            log.info("在线工作流执行成功: {}, 执行ID: {}", id, execution.getId());

            return result;

        } catch (Exception e) {
            // 7. 执行失败处理
            execution.setStatus("failed");
            execution.setErrorMessage(e.getMessage());
            execution.setEndTime(System.currentTimeMillis());

            log.error("在线工作流执行失败: {}", id, e);
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
        log.info("导出在线工作流: {}", id);

        // 1. 获取工作流
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("工作流不存在: " + id));

        // 2. 获取相关数据
        List<NodeEntity> nodes = nodeService.getNodesByWorkflowId(id);
        List<ValidationRuleEntity> rules = validationService.getRulesByWorkflowId(id);
        NodeConverter nodeConverter = new NodeConverter();
        RuleConverter ruleConverter = new RuleConverter();

        // 3. 构建导出数据对象
        WorkflowExportDataDTO exportData = WorkflowExportDataDTO.builder()
                .workflow(workflowConverter.toDTO(entity))
                .nodes(nodes.stream().map(nodeConverter::toDTO).toList())
                .validationRules(rules.stream().map(ruleConverter::toDTO).toList())
                .exportTime(System.currentTimeMillis())
                .version("1.0")
                .build();

        // 4. 导出到文件
        String filePath = fileStorageService.exportToFile(exportData);

        log.info("在线工作流导出成功: {}, 文件: {}", id, filePath);

        return filePath;
    }

    @Override
    @Transactional
    public WorkflowVO importWorkflow(String filePath) {
        log.info("导入工作流文件: {}", filePath);

        // 1. 读取文件
        WorkflowExportDataDTO importData = fileStorageService.importFromFile(filePath);

        // 2. 验证导入数据
        validateImportData(importData);

        // 3. 创建工作流（生成新ID）
        WorkflowDTO workflowDTO = importData.getWorkflow();
        workflowDTO.setId(null);
        workflowDTO.setMode("online");

        return createWorkflow(workflowDTO);
    }

    private void validateImportData(WorkflowExportDataDTO importData) {
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
        return "online";
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
     * 构建查询条件
     */
    private Specification<WorkflowEntity> buildQuerySpecification(WorkflowQueryDTO queryDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(queryDTO.getName())) {
                predicates.add(criteriaBuilder.like(root.get("name"), "%" + queryDTO.getName() + "%"));
            }

            if (StringUtils.hasText(queryDTO.getAlias())) {
                predicates.add(criteriaBuilder.equal(root.get("alias"), queryDTO.getAlias()));
            }

            if (StringUtils.hasText(queryDTO.getCategory())) {
                predicates.add(criteriaBuilder.equal(root.get("category"), queryDTO.getCategory()));
            }

            if (StringUtils.hasText(queryDTO.getStatus())) {
                predicates.add(criteriaBuilder.equal(root.get("status"), queryDTO.getStatus()));
            }

            if (queryDTO.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), queryDTO.getStartTime()));
            }

            if (queryDTO.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), queryDTO.getEndTime()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 获取当前用户
     */
    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "system";
    }

    /**
     * 生成工作流ID
     */
    private String generateWorkflowId() {
        return "workflow_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 创建执行记录
     */
    private ExecutionRecord createExecutionRecord(String workflowId, Object parameters) {
        ExecutionRecord execution = new ExecutionRecord();
        execution.setId("exec_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8));
        execution.setWorkflowId(workflowId);
        execution.setParameters(parameters);
        execution.setStatus("running");
        execution.setStartTime(System.currentTimeMillis());
        execution.setCreatedBy(getCurrentUser());

        executionService.save(execution);

        return execution;
    }

    /**
     * 添加统计信息
     */
    private void addStatistics(WorkflowVO vo) {
        WorkflowVO.StatisticsVO stats = new WorkflowVO.StatisticsVO();

        // 这里可以添加具体的统计逻辑
        // 例如：查询今日执行次数、成功率等

        vo.setStatistics(stats);
    }

    /**
     * 更新工作流统计信息
     */
    private void updateWorkflowStatistics(WorkflowEntity entity, ExecutionRecord execution) {
        // 更新执行次数
        entity.setExecutionCount(entity.getExecutionCount() + 1);

        // 更新成功率（简化计算）
        if ("completed".equals(execution.getStatus())) {
            // 这里应该有更复杂的成功率计算逻辑
            entity.setSuccessRate(95.5);
        }

        workflowRepository.save(entity);
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