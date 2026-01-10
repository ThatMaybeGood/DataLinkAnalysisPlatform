package com.workflow.platform.component.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.exception.VersionException;
import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.entity.WorkflowVersionEntity;
import com.workflow.platform.repository.WorkflowVersionRepository;
import com.workflow.platform.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 工作流版本管理器 - 提供完整的版本管理功能
 */
@Slf4j
@Component
public class WorkflowVersionManager {

    @Autowired
    private WorkflowVersionRepository versionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JsonUtil jsonUtil;

    @Value("${workflow.platform.versioning.max-versions:50}")
    private int maxVersions;

    @Value("${workflow.platform.versioning.auto-version:true}")
    private boolean autoVersioning;

    @Value("${workflow.platform.versioning.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${workflow.platform.versioning.delta-storage:true}")
    private boolean deltaStorage;

    // 内存缓存
    private final Map<String, VersionCache> versionCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // 版本锁
    private final Map<String, Object> versionLocks = new ConcurrentHashMap<>();

    /**
     * 创建新版本
     */
    public WorkflowVersionEntity createVersion(String workflowId, WorkflowDTO workflow,
                                               String versionName, String description,
                                               String createdBy) {
        Object lock = getVersionLock(workflowId);
        synchronized (lock) {
            try {
                log.info("创建工作流版本: {} - {}", workflowId, versionName);

                // 获取当前版本号
                Integer nextVersion = getNextVersionNumber(workflowId);

                // 创建版本实体
                WorkflowVersionEntity version = new WorkflowVersionEntity();
                version.setWorkflowId(workflowId);
                version.setVersionNumber(nextVersion);
                version.setVersionName(versionName);
                version.setDescription(description);
                version.setCreatedBy(createdBy);
                version.setCreatedTime(new Date());
                version.setStatus("active");

                // 获取前一个版本（用于增量存储）
                WorkflowVersionEntity previousVersion = null;
                if (deltaStorage && nextVersion > 1) {
                    previousVersion = versionRepository.findByWorkflowIdAndVersionNumber(
                            workflowId, nextVersion - 1);
                }

                // 存储版本数据
                String versionData = prepareVersionData(workflow, previousVersion);
                version.setVersionData(versionData);

                // 计算哈希值
                String checksum = calculateChecksum(versionData);
                version.setChecksum(checksum);

                // 计算大小
                version.setDataSize(versionData.length());

                // 保存版本
                version = versionRepository.save(version);

                // 更新缓存
                updateVersionCache(workflowId, version);

                // 清理旧版本（如果超过最大限制）
                cleanupOldVersions(workflowId);

                log.info("版本创建成功: {} - v{}", workflowId, nextVersion);
                return version;

            } catch (Exception e) {
                log.error("创建工作流版本失败: {}，错误: {}", workflowId, e.getMessage(), e);
                throw new VersionException("创建版本失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 获取特定版本
     */
    public WorkflowDTO getVersion(String workflowId, int versionNumber) {
        // 检查缓存
        WorkflowDTO cached = getFromCache(workflowId, versionNumber);
        if (cached != null) {
            return cached;
        }

        cacheLock.readLock().lock();
        try {
            WorkflowVersionEntity version = versionRepository
                    .findByWorkflowIdAndVersionNumber(workflowId, versionNumber);

            if (version == null) {
                throw new VersionException("版本不存在: " + workflowId + " v" + versionNumber);
            }

            // 验证数据完整性
            validateVersionData(version);

            // 恢复版本数据
            WorkflowDTO workflow = restoreVersionData(version);

            // 更新缓存
            updateCache(workflowId, versionNumber, workflow);

            return workflow;

        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * 获取所有版本列表
     */
    public List<WorkflowVersionEntity> getVersionList(String workflowId) {
        List<WorkflowVersionEntity> versions = versionRepository
                .findByWorkflowIdOrderByVersionNumberDesc(workflowId);

        // 为每个版本添加元数据
        versions.forEach(this::enrichVersionMetadata);

        return versions;
    }

    /**
     * 获取版本差异
     */
    public VersionDiff getVersionDiff(String workflowId, int version1, int version2) {
        WorkflowDTO v1 = getVersion(workflowId, version1);
        WorkflowDTO v2 = getVersion(workflowId, version2);

        VersionDiff diff = new VersionDiff();
        diff.setWorkflowId(workflowId);
        diff.setVersion1(version1);
        diff.setVersion2(version2);
        diff.setDiffTime(new Date());

        // 比较基本信息
        diff.setBasicChanges(compareBasicInfo(v1, v2));

        // 比较节点
        diff.setNodeChanges(compareNodes(v1, v2));

        // 比较连接
        diff.setConnectionChanges(compareConnections(v1, v2));

        // 比较配置
        diff.setConfigChanges(compareConfigs(v1, v2));

        // 计算总体变化统计
        calculateDiffStatistics(diff);

        return diff;
    }

    /**
     * 回滚到指定版本
     */
    public WorkflowDTO rollbackToVersion(String workflowId, int targetVersion,
                                         String rolledBackBy, String reason) {
        Object lock = getVersionLock(workflowId);
        synchronized (lock) {
            try {
                log.info("回滚工作流版本: {} -> v{}，原因: {}", workflowId, targetVersion, reason);

                // 获取目标版本
                WorkflowDTO targetWorkflow = getVersion(workflowId, targetVersion);

                // 创建回滚版本
                WorkflowVersionEntity rollbackVersion = new WorkflowVersionEntity();
                rollbackVersion.setWorkflowId(workflowId);
                rollbackVersion.setVersionNumber(getNextVersionNumber(workflowId));
                rollbackVersion.setVersionName("Rollback to v" + targetVersion);
                rollbackVersion.setDescription("回滚到版本 " + targetVersion + "，原因: " + reason);
                rollbackVersion.setCreatedBy(rolledBackBy);
                rollbackVersion.setCreatedTime(new Date());
                rollbackVersion.setStatus("rollback");
                rollbackVersion.setRollbackFromVersion(targetVersion);
                rollbackVersion.setRollbackReason(reason);

                // 存储回滚版本数据
                String versionData = jsonUtil.toJson(targetWorkflow);
                if (compressionEnabled) {
                    versionData = compressData(versionData);
                }

                rollbackVersion.setVersionData(versionData);
                rollbackVersion.setChecksum(calculateChecksum(versionData));
                rollbackVersion.setDataSize(versionData.length());

                // 保存回滚版本
                rollbackVersion = versionRepository.save(rollbackVersion);

                // 更新缓存
                updateVersionCache(workflowId, rollbackVersion);

                // 清理旧版本
                cleanupOldVersions(workflowId);

                // 记录回滚日志
                logRollbackEvent(workflowId, targetVersion, rollbackVersion.getVersionNumber(),
                        rolledBackBy, reason);

                log.info("回滚成功: {} -> v{} (新版本: v{})",
                        workflowId, targetVersion, rollbackVersion.getVersionNumber());

                return targetWorkflow;

            } catch (Exception e) {
                log.error("回滚工作流版本失败: {}，错误: {}", workflowId, e.getMessage(), e);
                throw new VersionException("回滚失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 创建版本分支
     */
    public String createBranch(String workflowId, int baseVersion, String branchName,
                               String description, String createdBy) {
        Object lock = getVersionLock(workflowId);
        synchronized (lock) {
            try {
                log.info("创建工作流分支: {}，基础版本: v{}，分支: {}",
                        workflowId, baseVersion, branchName);

                // 生成分支ID
                String branchId = generateBranchId(workflowId, branchName);

                // 获取基础版本
                WorkflowDTO baseWorkflow = getVersion(workflowId, baseVersion);

                // 修改工作流信息以标识分支
                baseWorkflow.setBranchId(branchId);
                baseWorkflow.setBranchName(branchName);
                baseWorkflow.setBasedOnVersion(baseVersion);
                baseWorkflow.setBranchCreatedBy(createdBy);
                baseWorkflow.setBranchCreatedTime(new Date());

                // 创建分支版本
                WorkflowVersionEntity branchVersion = new WorkflowVersionEntity();
                branchVersion.setWorkflowId(workflowId);
                branchVersion.setVersionNumber(1); // 分支从版本1开始
                branchVersion.setVersionName(branchName + " v1");
                branchVersion.setDescription(description);
                branchVersion.setCreatedBy(createdBy);
                branchVersion.setCreatedTime(new Date());
                branchVersion.setStatus("branch");
                branchVersion.setBranchId(branchId);
                branchVersion.setBranchName(branchName);
                branchVersion.setBasedOnVersion(baseVersion);

                // 存储分支数据
                String versionData = jsonUtil.toJson(baseWorkflow);
                if (compressionEnabled) {
                    versionData = compressData(versionData);
                }

                branchVersion.setVersionData(versionData);
                branchVersion.setChecksum(calculateChecksum(versionData));
                branchVersion.setDataSize(versionData.length());

                // 保存分支版本
                branchVersion = versionRepository.save(branchVersion);

                // 创建分支记录
                createBranchRecord(branchId, workflowId, baseVersion, branchName,
                        createdBy, description);

                log.info("分支创建成功: {}，分支ID: {}", branchName, branchId);

                return branchId;

            } catch (Exception e) {
                log.error("创建工作流分支失败: {}，错误: {}", workflowId, e.getMessage(), e);
                throw new VersionException("创建分支失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 合并分支
     */
    public MergeResult mergeBranch(String branchId, String targetWorkflowId,
                                   int targetVersion, String mergedBy,
                                   String strategy, String description) {
        Object lock = getVersionLock(branchId);
        synchronized (lock) {
            try {
                log.info("合并分支: {} -> {} v{}，策略: {}",
                        branchId, targetWorkflowId, targetVersion, strategy);

                // 获取分支最新版本
                WorkflowVersionEntity branchVersion = getLatestBranchVersion(branchId);
                if (branchVersion == null) {
                    throw new VersionException("分支不存在或为空: " + branchId);
                }

                // 获取目标版本
                WorkflowDTO targetWorkflow = getVersion(targetWorkflowId, targetVersion);

                // 获取分支工作流
                WorkflowDTO branchWorkflow = restoreVersionData(branchVersion);

                // 执行合并
                WorkflowDTO mergedWorkflow = mergeWorkflows(targetWorkflow, branchWorkflow, strategy);

                // 创建合并版本
                WorkflowVersionEntity mergeVersion = new WorkflowVersionEntity();
                mergeVersion.setWorkflowId(targetWorkflowId);
                mergeVersion.setVersionNumber(getNextVersionNumber(targetWorkflowId));
                mergeVersion.setVersionName("Merge from " + branchId);
                mergeVersion.setDescription(description);
                mergeVersion.setCreatedBy(mergedBy);
                mergeVersion.setCreatedTime(new Date());
                mergeVersion.setStatus("merged");
                mergeVersion.setMergeFromBranch(branchId);
                mergeVersion.setMergeStrategy(strategy);
                mergeVersion.setMergeBaseVersion(targetVersion);

                // 存储合并数据
                String versionData = jsonUtil.toJson(mergedWorkflow);
                if (compressionEnabled) {
                    versionData = compressData(versionData);
                }

                mergeVersion.setVersionData(versionData);
                mergeVersion.setChecksum(calculateChecksum(versionData));
                mergeVersion.setDataSize(versionData.length());

                // 保存合并版本
                mergeVersion = versionRepository.save(mergeVersion);

                // 记录合并结果
                MergeResult result = createMergeResult(branchId, targetWorkflowId,
                        targetVersion, mergeVersion.getVersionNumber(),
                        mergedBy, strategy, description);

                // 关闭分支（可选）
                if ("close".equals(strategy)) {
                    closeBranch(branchId, "已合并到主分支");
                }

                log.info("分支合并成功: {} -> {} v{} -> v{}",
                        branchId, targetWorkflowId, targetVersion, mergeVersion.getVersionNumber());

                return result;

            } catch (Exception e) {
                log.error("合并分支失败: {}，错误: {}", branchId, e.getMessage(), e);
                throw new VersionException("合并失败: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 获取版本标签
     */
    public List<VersionTag> getVersionTags(String workflowId) {
        return versionRepository.findTagsByWorkflowId(workflowId)
                .stream()
                .map(this::convertToVersionTag)
                .collect(Collectors.toList());
    }

    /**
     * 添加版本标签
     */
    public VersionTag addVersionTag(String workflowId, int versionNumber,
                                    String tagName, String tagColor, String description) {
        WorkflowVersionEntity version = versionRepository
                .findByWorkflowIdAndVersionNumber(workflowId, versionNumber);

        if (version == null) {
            throw new VersionException("版本不存在: v" + versionNumber);
        }

        // 创建标签
        VersionTag tag = new VersionTag();
        tag.setWorkflowId(workflowId);
        tag.setVersionNumber(versionNumber);
        tag.setTagName(tagName);
        tag.setTagColor(tagColor);
        tag.setDescription(description);
        tag.setCreatedBy("system");
        tag.setCreatedTime(new Date());

        // 保存标签
        version.setTags(version.getTags() == null ? new ArrayList<>() : version.getTags());
        version.getTags().add(tag);

        // 更新版本
        versionRepository.save(version);

        log.info("添加版本标签: {} v{} - {}", workflowId, versionNumber, tagName);

        return tag;
    }

    /**
     * 获取版本统计
     */
    public VersionStatistics getVersionStatistics(String workflowId) {
        VersionStatistics stats = new VersionStatistics();
        stats.setWorkflowId(workflowId);
        stats.setGeneratedTime(new Date());

        // 获取所有版本
        List<WorkflowVersionEntity> versions = versionRepository
                .findByWorkflowIdOrderByVersionNumberAsc(workflowId);

        if (versions.isEmpty()) {
            return stats;
        }

        // 基本统计
        stats.setTotalVersions(versions.size());
        stats.setFirstVersion(versions.get(0).getVersionNumber());
        stats.setLatestVersion(versions.get(versions.size() - 1).getVersionNumber());

        // 时间统计
        Date firstDate = versions.get(0).getCreatedTime();
        Date lastDate = versions.get(versions.size() - 1).getCreatedTime();
        long duration = lastDate.getTime() - firstDate.getTime();
        stats.setVersionDuration(duration);
        stats.setAverageVersionInterval(duration / Math.max(1, versions.size() - 1));

        // 大小统计
        long totalSize = versions.stream().mapToLong(WorkflowVersionEntity::getDataSize).sum();
        stats.setTotalStorageSize(totalSize);
        stats.setAverageVersionSize(totalSize / versions.size());

        // 类型统计
        Map<String, Long> typeCount = versions.stream()
                .collect(Collectors.groupingBy(
                        WorkflowVersionEntity::getStatus,
                        Collectors.counting()
                ));
        stats.setVersionTypeCount(typeCount);

        // 用户统计
        Map<String, Long> userCount = versions.stream()
                .collect(Collectors.groupingBy(
                        WorkflowVersionEntity::getCreatedBy,
                        Collectors.counting()
                ));
        stats.setContributorCount(userCount);

        // 计算版本活跃度
        calculateVersionActivity(stats, versions);

        return stats;
    }

    /**
     * 清理旧版本
     */
    public int cleanupOldVersions(String workflowId, int keepLastVersions) {
        List<WorkflowVersionEntity> versions = versionRepository
                .findByWorkflowIdOrderByVersionNumberDesc(workflowId);

        if (versions.size() <= keepLastVersions) {
            return 0;
        }

        int removed = 0;
        List<WorkflowVersionEntity> toRemove = versions.subList(keepLastVersions, versions.size());

        for (WorkflowVersionEntity version : toRemove) {
            // 检查是否为重要版本（有标签、回滚点等）
            if (shouldKeepVersion(version)) {
                continue;
            }

            // 标记为已删除
            version.setStatus("deleted");
            version.setDeletedTime(new Date());
            versionRepository.save(version);

            removed++;
            log.debug("清理旧版本: {} v{}", workflowId, version.getVersionNumber());
        }

        log.info("清理工作流版本: {}，保留 {} 个，清理 {} 个",
                workflowId, keepLastVersions, removed);

        return removed;
    }

    // ========== 私有方法 ==========

    private Integer getNextVersionNumber(String workflowId) {
        Integer maxVersion = versionRepository.findMaxVersionNumber(workflowId);
        return (maxVersion == null) ? 1 : maxVersion + 1;
    }

    private String prepareVersionData(WorkflowDTO workflow, WorkflowVersionEntity previousVersion) {
        String versionData = jsonUtil.toJson(workflow);

        if (deltaStorage && previousVersion != null) {
            // 增量存储：只存储变化部分
            try {
                WorkflowDTO previousWorkflow = restoreVersionData(previousVersion);
                String delta = calculateDelta(previousWorkflow, workflow);
                versionData = delta;
            } catch (Exception e) {
                log.warn("增量存储失败，使用完整存储: {}", e.getMessage());
            }
        }

        if (compressionEnabled) {
            versionData = compressData(versionData);
        }

        return versionData;
    }

    private WorkflowDTO restoreVersionData(WorkflowVersionEntity version) {
        String versionData = version.getVersionData();

        if (compressionEnabled) {
            versionData = decompressData(versionData);
        }

        WorkflowDTO workflow = jsonUtil.fromJson(versionData, WorkflowDTO.class);

        // 恢复增量数据（如果需要）
        if (deltaStorage && isDeltaData(versionData)) {
            workflow = applyDelta(workflow, versionData);
        }

        return workflow;
    }

    private void validateVersionData(WorkflowVersionEntity version) {
        String storedChecksum = version.getChecksum();
        String calculatedChecksum = calculateChecksum(version.getVersionData());

        if (!storedChecksum.equals(calculatedChecksum)) {
            throw new VersionException("版本数据损坏: " + version.getWorkflowId() +
                    " v" + version.getVersionNumber());
        }
    }

    private String calculateChecksum(String data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            return bytesToHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(data.hashCode());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String compressData(String data) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(bos);
            gzip.write(data.getBytes());
            gzip.close();
            return java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            log.warn("数据压缩失败: {}", e.getMessage());
            return data;
        }
    }

    private String decompressData(String compressedData) {
        try {
            byte[] data = java.util.Base64.getDecoder().decode(compressedData);
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(data);
            java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bis);
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }

            gzip.close();
            return bos.toString();
        } catch (Exception e) {
            log.warn("数据解压失败: {}", e.getMessage());
            return compressedData;
        }
    }

    private String calculateDelta(WorkflowDTO oldVersion, WorkflowDTO newVersion) {
        // 实现差异计算算法
        // 这里简化为返回完整数据
        return jsonUtil.toJson(newVersion);
    }

    private boolean isDeltaData(String data) {
        // 检查是否为增量数据
        return data.startsWith("DELTA:");
    }

    private WorkflowDTO applyDelta(WorkflowDTO baseWorkflow, String delta) {
        // 应用增量数据
        return baseWorkflow;
    }

    private Object getVersionLock(String workflowId) {
        return versionLocks.computeIfAbsent(workflowId, k -> new Object());
    }

    private void updateVersionCache(String workflowId, WorkflowVersionEntity version) {
        VersionCache cache = versionCache.computeIfAbsent(workflowId, k -> new VersionCache());
        cache.addVersion(version.getVersionNumber(), restoreVersionData(version));
    }

    private WorkflowDTO getFromCache(String workflowId, int versionNumber) {
        VersionCache cache = versionCache.get(workflowId);
        return cache != null ? cache.getVersion(versionNumber) : null;
    }

    private void updateCache(String workflowId, int versionNumber, WorkflowDTO workflow) {
        VersionCache cache = versionCache.computeIfAbsent(workflowId, k -> new VersionCache());
        cache.addVersion(versionNumber, workflow);
    }

    private void cleanupOldVersions(String workflowId) {
        if (maxVersions <= 0) {
            return;
        }

        cleanupOldVersions(workflowId, maxVersions);
    }

    private boolean shouldKeepVersion(WorkflowVersionEntity version) {
        // 重要版本判断逻辑
        return version.getTags() != null && !version.getTags().isEmpty() ||
                "rollback".equals(version.getStatus()) ||
                "merged".equals(version.getStatus()) ||
                version.getVersionNumber() == 1; // 保留第一个版本
    }

    private void enrichVersionMetadata(WorkflowVersionEntity version) {
        // 添加额外元数据
        version.setMetadata(new HashMap<>());
        version.getMetadata().put("dataSizeFormatted", formatSize(version.getDataSize()));
        version.getMetadata().put("compressed", compressionEnabled);

        if (version.getTags() != null) {
            version.getMetadata().put("tagCount", version.getTags().size());
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private List<BasicChange> compareBasicInfo(WorkflowDTO v1, WorkflowDTO v2) {
        List<BasicChange> changes = new ArrayList<>();

        if (!Objects.equals(v1.getName(), v2.getName())) {
            changes.add(new BasicChange("name", v1.getName(), v2.getName()));
        }

        if (!Objects.equals(v1.getDescription(), v2.getDescription())) {
            changes.add(new BasicChange("description", v1.getDescription(), v2.getDescription()));
        }

        if (!Objects.equals(v1.getCategory(), v2.getCategory())) {
            changes.add(new BasicChange("category", v1.getCategory(), v2.getCategory()));
        }

        return changes;
    }

    private List<NodeChange> compareNodes(WorkflowDTO v1, WorkflowDTO v2) {
        List<NodeChange> changes = new ArrayList<>();
        // 实现节点比较逻辑
        return changes;
    }

    private List<ConnectionChange> compareConnections(WorkflowDTO v1, WorkflowDTO v2) {
        List<ConnectionChange> changes = new ArrayList<>();
        // 实现连接比较逻辑
        return changes;
    }

    private List<ConfigChange> compareConfigs(WorkflowDTO v1, WorkflowDTO v2) {
        List<ConfigChange> changes = new ArrayList<>();
        // 实现配置比较逻辑
        return changes;
    }

    private void calculateDiffStatistics(VersionDiff diff) {
        int totalChanges = diff.getBasicChanges().size() +
                diff.getNodeChanges().size() +
                diff.getConnectionChanges().size() +
                diff.getConfigChanges().size();

        diff.setTotalChanges(totalChanges);
        diff.setChangePercentage(calculateChangePercentage(diff));
    }

    private double calculateChangePercentage(VersionDiff diff) {
        // 计算变化百分比
        return diff.getTotalChanges() * 100.0 / Math.max(1, diff.getTotalChanges());
    }

    private void logRollbackEvent(String workflowId, int targetVersion, int newVersion,
                                  String rolledBackBy, String reason) {
        log.info("回滚事件: 工作流={}, 目标版本=v{}, 新版本=v{}, 执行人={}, 原因={}",
                workflowId, targetVersion, newVersion, rolledBackBy, reason);
    }

    private String generateBranchId(String workflowId, String branchName) {
        return workflowId + "_branch_" + branchName.toLowerCase().replace(" ", "_") +
                "_" + System.currentTimeMillis();
    }

    private void createBranchRecord(String branchId, String workflowId, int baseVersion,
                                    String branchName, String createdBy, String description) {
        // 创建分支记录
        log.info("创建分支记录: ID={}, 名称={}, 基础工作流={} v{}",
                branchId, branchName, workflowId, baseVersion);
    }

    private WorkflowVersionEntity getLatestBranchVersion(String branchId) {
        return versionRepository.findFirstByBranchIdOrderByVersionNumberDesc(branchId);
    }

    private WorkflowDTO mergeWorkflows(WorkflowDTO target, WorkflowDTO source, String strategy) {
        // 实现工作流合并逻辑
        switch (strategy) {
            case "theirs":
                return source;
            case "ours":
                return target;
            case "merge":
                return mergeThreeWay(target, source);
            default:
                return source;
        }
    }

    private WorkflowDTO mergeThreeWay(WorkflowDTO base, WorkflowDTO theirs) {
        // 三向合并算法
        WorkflowDTO merged = new WorkflowDTO();

        // 合并基本信息
        merged.setId(base.getId());
        merged.setName(theirs.getName() != null ? theirs.getName() : base.getName());
        merged.setDescription(theirs.getDescription() != null ? theirs.getDescription() : base.getDescription());

        // 合并节点和连接
        merged.setNodes(mergeLists(base.getNodes(), theirs.getNodes()));
        merged.setConnections(mergeLists(base.getConnections(), theirs.getConnections()));

        return merged;
    }

    private <T> List<T> mergeLists(List<T> list1, List<T> list2) {
        List<T> merged = new ArrayList<>();

        if (list1 != null) merged.addAll(list1);
        if (list2 != null) {
            for (T item : list2) {
                if (!merged.contains(item)) {
                    merged.add(item);
                }
            }
        }

        return merged;
    }

    private MergeResult createMergeResult(String branchId, String targetWorkflowId,
                                          int targetVersion, int newVersion,
                                          String mergedBy, String strategy, String description) {
        MergeResult result = new MergeResult();
        result.setBranchId(branchId);
        result.setTargetWorkflowId(targetWorkflowId);
        result.setTargetVersion(targetVersion);
        result.setNewVersion(newVersion);
        result.setMergedBy(mergedBy);
        result.setMergeTime(new Date());
        result.setMergeStrategy(strategy);
        result.setDescription(description);
        result.setSuccess(true);

        return result;
    }

    private void closeBranch(String branchId, String reason) {
        log.info("关闭分支: {}，原因: {}", branchId, reason);
    }

    private VersionTag convertToVersionTag(Object entity) {
        // 转换实体为标签对象
        return new VersionTag();
    }

    private void calculateVersionActivity(VersionStatistics stats, List<WorkflowVersionEntity> versions) {
        // 计算版本活跃度
        if (versions.size() < 2) {
            stats.setActivityScore(0);
            return;
        }

        long totalInterval = 0;
        for (int i = 1; i < versions.size(); i++) {
            long interval = versions.get(i).getCreatedTime().getTime() -
                    versions.get(i-1).getCreatedTime().getTime();
            totalInterval += interval;
        }

        long averageInterval = totalInterval / (versions.size() - 1);
        double activity = 1.0 / (averageInterval / (24 * 60 * 60 * 1000.0)); // 每天版本数
        stats.setActivityScore(activity);
    }

    // ========== 内部类 ==========

    /**
     * 版本缓存
     */
    private static class VersionCache {
        private final Map<Integer, WorkflowDTO> versions = new HashMap<>();
        private final int maxCacheSize = 10;

        public void addVersion(int versionNumber, WorkflowDTO workflow) {
            if (versions.size() >= maxCacheSize) {
                // 移除最旧的版本
                int oldestVersion = versions.keySet().stream()
                        .min(Integer::compareTo)
                        .orElse(0);
                versions.remove(oldestVersion);
            }
            versions.put(versionNumber, workflow);
        }

        public WorkflowDTO getVersion(int versionNumber) {
            return versions.get(versionNumber);
        }
    }

    /**
     * 版本差异
     */
    @Data
    public static class VersionDiff {
        private String workflowId;
        private int version1;
        private int version2;
        private Date diffTime;
        private List<BasicChange> basicChanges;
        private List<NodeChange> nodeChanges;
        private List<ConnectionChange> connectionChanges;
        private List<ConfigChange> configChanges;
        private int totalChanges;
        private double changePercentage;
        private String diffSummary;
    }

    /**
     * 基本变化
     */
    @Data
    @AllArgsConstructor
    public static class BasicChange {
        private String field;
        private Object oldValue;
        private Object newValue;
    }

    /**
     * 节点变化
     */
    @Data
    public static class NodeChange {
        private String nodeId;
        private String changeType; // added, removed, modified
        private Map<String, Object> changes;
    }

    /**
     * 连接变化
     */
    @Data
    public static class ConnectionChange {
        private String connectionId;
        private String changeType;
        private Map<String, Object> changes;
    }

    /**
     * 配置变化
     */
    @Data
    public static class ConfigChange {
        private String configKey;
        private Object oldValue;
        private Object newValue;
    }

    /**
     * 合并结果
     */
    @Data
    public static class MergeResult {
        private String branchId;
        private String targetWorkflowId;
        private int targetVersion;
        private int newVersion;
        private String mergedBy;
        private Date mergeTime;
        private String mergeStrategy;
        private String description;
        private boolean success;
        private String errorMessage;
        private List<String> conflicts;
    }

    /**
     * 版本标签
     */
    @Data
    public static class VersionTag {
        private String workflowId;
        private int versionNumber;
        private String tagName;
        private String tagColor;
        private String description;
        private String createdBy;
        private Date createdTime;
    }

    /**
     * 版本统计
     */
    @Data
    public static class VersionStatistics {
        private String workflowId;
        private Date generatedTime;
        private int totalVersions;
        private int firstVersion;
        private int latestVersion;
        private long versionDuration;
        private long averageVersionInterval;
        private long totalStorageSize;
        private long averageVersionSize;
        private Map<String, Long> versionTypeCount;
        private Map<String, Long> contributorCount;
        private double activityScore;
        private Map<String, Object> additionalStats;
    }
}
