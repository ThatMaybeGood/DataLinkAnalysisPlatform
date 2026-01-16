package com.workflow.platform.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.annotation.RequireMode;
import com.workflow.platform.model.dto.WorkflowDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 离线工作流仓库（基于文件系统）
 * 离线模式下，工作流数据存储在本地JSON文件中
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@RequireMode("offline")
public class OfflineWorkflowRepository {

    @Value("${app.offline.storage.file.path:./data/workflows}")
    private String storagePath;

    private final ObjectMapper objectMapper;

    /**
     * 保存工作流到文件系统
     * @param workflow 工作流DTO
     * @return 保存后的工作流
     * @throws IOException 文件操作异常
     */
    public WorkflowDTO save(WorkflowDTO workflow) throws IOException {
        // 生成ID（如果为空）
        if (workflow.getId() == null || workflow.getId().isEmpty()) {
            workflow.setId(generateId());
        }

        // 设置时间戳
        if (workflow.getCreatedAt() == null) {
            workflow.setCreatedAt(System.currentTimeMillis());
        }
        workflow.setUpdatedAt(System.currentTimeMillis());

        // 保存到文件
        saveToFile(workflow);

        log.info("工作流保存到文件: {}/{}", storagePath, workflow.getId() + ".json");
        return workflow;
    }

    /**
     * 根据ID查找工作流
     * @param id 工作流ID
     * @return Optional包装的工作流DTO
     */
    public Optional<WorkflowDTO> findById(String id) {
        try {
            WorkflowDTO workflow = loadFromFile(id);
            return Optional.ofNullable(workflow);
        } catch (IOException e) {
            log.error("读取工作流文件失败: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 根据别名查找工作流
     * @param alias 别名
     * @return Optional包装的工作流DTO
     */
    public Optional<WorkflowDTO> findByAlias(String alias) {
        return findAll().stream()
                .filter(workflow -> alias.equals(workflow.getAlias()))
                .findFirst();
    }

    /**
     * 查找所有工作流
     * @return 工作流列表
     */
    public List<WorkflowDTO> findAll() {
        List<WorkflowDTO> workflows = new ArrayList<>();
        File dir = new File(storagePath);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
            if (files != null) {
                for (File file : files) {
                    try {
                        WorkflowDTO workflow = objectMapper.readValue(file, WorkflowDTO.class);
                        workflows.add(workflow);
                    } catch (IOException e) {
                        log.error("读取工作流文件失败: {}", file.getName(), e);
                    }
                }
            }
        }

        // 按更新时间倒序排序
        workflows.sort((w1, w2) -> w2.getUpdatedAt().compareTo(w1.getUpdatedAt()));

        return workflows;
    }

    /**
     * 分页查询工作流
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    public Page<WorkflowDTO> findAll(Pageable pageable) {
        List<WorkflowDTO> allWorkflows = findAll();

        // 排序
        if (pageable.getSort().isSorted()) {
            allWorkflows.sort((w1, w2) -> {
                // 这里简化处理，实际应该根据pageable.getSort()进行排序
                return w2.getUpdatedAt().compareTo(w1.getUpdatedAt());
            });
        }

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allWorkflows.size());

        List<WorkflowDTO> pageContent = allWorkflows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allWorkflows.size());
    }

    /**
     * 根据条件查询工作流
     * @param conditions 查询条件
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    public Page<WorkflowDTO> findByConditions(Map<String, Object> conditions, Pageable pageable) {
        List<WorkflowDTO> allWorkflows = findAll();

        // 过滤
        List<WorkflowDTO> filteredWorkflows = allWorkflows.stream()
                .filter(workflow -> matchesConditions(workflow, conditions))
                .collect(Collectors.toList());

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredWorkflows.size());

        List<WorkflowDTO> pageContent = filteredWorkflows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredWorkflows.size());
    }

    /**
     * 删除工作流
     * @param id 工作流ID
     */
    public void deleteById(String id) {
        try {
            Path filePath = getFilePath(id);
            Files.deleteIfExists(filePath);

            // 删除相关文件
            deleteRelatedFiles(id);

            log.info("工作流删除成功: {}", id);
        } catch (IOException e) {
            log.error("删除工作流文件失败: {}", id, e);
            throw new RuntimeException("删除工作流失败", e);
        }
    }

    /**
     * 检查工作流是否存在
     * @param id 工作流ID
     * @return 是否存在
     */
    public boolean existsById(String id) {
        Path filePath = getFilePath(id);
        return Files.exists(filePath);
    }

    /**
     * 检查别名是否存在
     * @param alias 别名
     * @return 是否存在
     */
    public boolean existsByAlias(String alias) {
        return findByAlias(alias).isPresent();
    }

    /**
     * 统计工作流数量
     * @return 数量
     */
    public long count() {
        try {
            Path dirPath = Paths.get(storagePath);
            if (!Files.exists(dirPath)) {
                return 0;
            }

            return Files.list(dirPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .count();
        } catch (IOException e) {
            log.error("统计工作流数量失败", e);
            return 0;
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 保存工作流到文件
     */
    private void saveToFile(WorkflowDTO workflow) throws IOException {
        Path dirPath = Paths.get(storagePath);
        Files.createDirectories(dirPath);

        Path filePath = getFilePath(workflow.getId());
        objectMapper.writeValue(filePath.toFile(), workflow);
    }

    /**
     * 从文件加载工作流
     */
    private WorkflowDTO loadFromFile(String id) throws IOException {
        Path filePath = getFilePath(id);
        if (!Files.exists(filePath)) {
            return null;
        }

        return objectMapper.readValue(filePath.toFile(), WorkflowDTO.class);
    }

    /**
     * 获取文件路径
     */
    private Path getFilePath(String id) {
        return Paths.get(storagePath, id + ".json");
    }

    /**
     * 删除相关文件
     */
    private void deleteRelatedFiles(String workflowId) {
        // 这里可以删除节点、规则等相关文件
        // 实现取决于具体的数据结构
    }

    /**
     * 检查工作流是否匹配条件
     */
    private boolean matchesConditions(WorkflowDTO workflow, Map<String, Object> conditions) {
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            Object fieldValue = getFieldValue(workflow, key);
            if (fieldValue == null || !fieldValue.equals(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 通过反射获取字段值
     */
    private Object getFieldValue(WorkflowDTO workflow, String fieldName) {
        try {
            java.lang.reflect.Field field = WorkflowDTO.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(workflow);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 生成唯一ID
     */
    private String generateId() {
        return "workflow_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }
}