package com.workflow.platform.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 离线工作流仓库 - 基于文件系统的存储
 */
@Repository
@RequiredArgsConstructor
@Slf4j
@ConditionalOnMode("offline")
public class OfflineWorkflowRepository {

    @Value("${app.offline.storage.file.path:./data/workflows}")
    private String storagePath;

    private final ObjectMapper objectMapper;

    /**
     * 保存工作流
     */
    public WorkflowEntity save(WorkflowEntity entity) {
        try {
            // 生成ID（如果没有）
            if (entity.getId() == null) {
                entity.setId(generateId());
            }

            // 设置时间戳
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(LocalDateTime.now());
            }
            entity.setUpdatedAt(LocalDateTime.now());

            // 保存到文件
            saveToFile(entity);

            return entity;
        } catch (IOException e) {
            throw new RuntimeException("保存工作流失败", e);
        }
    }

    /**
     * 根据ID查找工作流
     */
    public Optional<WorkflowEntity> findById(String id) {
        try {
            Path filePath = getFilePath(id);
            if (!Files.exists(filePath)) {
                return Optional.empty();
            }

            WorkflowEntity entity = objectMapper.readValue(filePath.toFile(), WorkflowEntity.class);
            return Optional.of(entity);
        } catch (IOException e) {
            log.error("读取工作流文件失败: {}", id, e);
            return Optional.empty();
        }
    }

    /**
     * 查找所有工作流
     */
    public List<WorkflowEntity> findAll() {
        List<WorkflowEntity> workflows = new ArrayList<>();

        try {
            Path dirPath = Paths.get(storagePath);
            if (!Files.exists(dirPath)) {
                return workflows;
            }

            Files.list(dirPath)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            WorkflowEntity entity = objectMapper.readValue(path.toFile(), WorkflowEntity.class);
                            workflows.add(entity);
                        } catch (IOException e) {
                            log.error("读取工作流文件失败: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("遍历工作流目录失败", e);
        }

        return workflows;
    }

    /**
     * 分页查询工作流
     */
    public Page<WorkflowEntity> findAll(Pageable pageable) {
        List<WorkflowEntity> allWorkflows = findAll();

        // 排序
        if (pageable.getSort().isSorted()) {
            allWorkflows.sort((w1, w2) -> {
                // 实现排序逻辑
                return w1.getUpdatedAt().compareTo(w2.getUpdatedAt());
            });
        }

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allWorkflows.size());

        List<WorkflowEntity> pageContent = allWorkflows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, allWorkflows.size());
    }

    /**
     * 根据条件查询工作流
     */
    public Page<WorkflowEntity> findByConditions(Map<String, Object> conditions, Pageable pageable) {
        List<WorkflowEntity> allWorkflows = findAll();

        // 过滤
        List<WorkflowEntity> filteredWorkflows = allWorkflows.stream()
                .filter(entity -> {
                    for (Map.Entry<String, Object> entry : conditions.entrySet()) {
                        Object value = getFieldValue(entity, entry.getKey());
                        if (value == null || !value.equals(entry.getValue())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());

        // 分页
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredWorkflows.size());

        List<WorkflowEntity> pageContent = filteredWorkflows.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredWorkflows.size());
    }

    /**
     * 删除工作流
     */
    public void deleteById(String id) {
        try {
            Path filePath = getFilePath(id);
            Files.deleteIfExists(filePath);

            // 删除相关文件
            deleteRelatedFiles(id);

            log.info("工作流删除成功: {}", id);
        } catch (IOException e) {
            log.error("删除工作流失败: {}", id, e);
            throw new RuntimeException("删除工作流失败", e);
        }
    }

    /**
     * 检查工作流是否存在
     */
    public boolean existsById(String id) {
        Path filePath = getFilePath(id);
        return Files.exists(filePath);
    }

    /**
     * 统计数量
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

    /**
     * 搜索工作流
     */
    public List<WorkflowEntity> search(String keyword) {
        List<WorkflowEntity> allWorkflows = findAll();

        return allWorkflows.stream()
                .filter(entity -> {
                    return entity.getName().contains(keyword) ||
                            (entity.getAlias() != null && entity.getAlias().contains(keyword)) ||
                            (entity.getDescription() != null && entity.getDescription().contains(keyword));
                })
                .collect(Collectors.toList());
    }

    // 私有辅助方法

    private void saveToFile(WorkflowEntity entity) throws IOException {
        Path dirPath = Paths.get(storagePath);
        Files.createDirectories(dirPath);

        Path filePath = getFilePath(entity.getId());
        objectMapper.writeValue(filePath.toFile(), entity);
    }

    private Path getFilePath(String id) {
        return Paths.get(storagePath, id + ".json");
    }

    private void deleteRelatedFiles(String workflowId) {
        // 删除节点文件
        deleteFilesInDirectory("./data/nodes", workflowId + "_*.json");

        // 删除规则文件
        deleteFilesInDirectory("./data/rules", workflowId + "_*.json");

        // 删除执行记录文件
        deleteFilesInDirectory("./data/executions", "exec_*_" + workflowId + "_*.json");
    }

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

    private Object getFieldValue(WorkflowEntity entity, String fieldName) {
        try {
            java.lang.reflect.Field field = WorkflowEntity.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private String generateId() {
        return "workflow_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}