package com.workflow.platform.service.impl;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:52
 */

import com.workflow.platform.exception.WorkflowException;
import com.workflow.platform.model.dto.WorkflowVersionDTO;
import com.workflow.platform.model.vo.WorkflowVersionVO;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.model.entity.WorkflowVersionEntity;
import com.workflow.platform.repository.WorkflowRepository;
import com.workflow.platform.repository.WorkflowVersionRepository;
import com.workflow.platform.service.WorkflowService;
import com.workflow.platform.service.WorkflowVersionService;
import com.workflow.platform.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流版本服务实现
 */
@Service
@Slf4j
public class WorkflowVersionServiceImpl implements WorkflowVersionService {

    @Autowired
    private WorkflowVersionRepository versionRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowService workflowService;

    @Override
    @Transactional
    public WorkflowVersionVO createVersion(WorkflowVersionDTO versionDTO) {
        log.info("创建工作流版本，工作流ID: {}", versionDTO.getWorkflowId());

        // 验证工作流是否存在
        WorkflowEntity workflow = workflowRepository.findById(versionDTO.getWorkflowId())
                .orElseThrow(() -> new WorkflowException("工作流不存在"));

        // 获取当前最大版本号
        Integer maxVersion = versionRepository.findMaxVersionNumber(Long.valueOf(versionDTO.getWorkflowId()));
        int newVersionNumber = maxVersion + 1;

        // 将当前版本标记为非当前
        versionRepository.markAllVersionsAsNotCurrent(Long.valueOf(versionDTO.getWorkflowId()));

        // 创建版本实体
        WorkflowVersionEntity versionEntity = new WorkflowVersionEntity();
        BeanUtils.copyProperties(versionDTO, versionEntity);

        // 设置版本号
        versionEntity.setVersionNumber(newVersionNumber);

        // 如果没有提供版本名称，使用默认名称
        if (!StringUtils.hasText(versionEntity.getVersionName())) {
            versionEntity.setVersionName("v" + newVersionNumber);
        }

        // 设置工作流数据
        versionEntity.setWorkflowData(JsonUtil.toJson(workflow));

        // 设置节点数据（如果有）
        if (versionDTO.getNodeData() != null) {
            versionEntity.setNodeData(versionDTO.getNodeData());
        }

        // 设置验证规则数据（如果有）
        if (versionDTO.getValidationData() != null) {
            versionEntity.setValidationData(versionDTO.getValidationData());
        }

        // 设置为当前版本
        versionEntity.setIsCurrent(true);
        versionEntity.setCreateTime(LocalDateTime.now());

        // 保存版本
        versionRepository.save(versionEntity);

        log.info("创建工作流版本成功，工作流ID: {}，版本号: {}",
                versionDTO.getWorkflowId(), newVersionNumber);

        return convertToVO(versionEntity);
    }

    @Override
    public List<WorkflowVersionVO> getVersions(Long workflowId) {
        log.debug("获取工作流版本列表，工作流ID: {}", workflowId);

        List<WorkflowVersionEntity> versionEntities =
                versionRepository.findByWorkflowIdOrderByVersionNumberDesc(workflowId);

        return versionEntities.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public WorkflowVersionVO getVersionDetail(Long versionId) {
        log.debug("获取版本详情，版本ID: {}", versionId);

        WorkflowVersionEntity versionEntity = versionRepository.findById(versionId)
                .orElseThrow(() -> new WorkflowException("版本不存在"));

        return convertToVO(versionEntity);
    }


    @Override
    public WorkflowVersionVO getVersionByNumber(Long workflowId, Integer versionNumber) {
        log.debug("根据版本号获取版本，工作流ID: {}，版本号: {}", workflowId, versionNumber);

        WorkflowVersionEntity versionEntity = versionRepository
                .findByWorkflowIdAndVersionNumber(workflowId, versionNumber)
                .orElseThrow(() -> new WorkflowException("版本不存在"));

        return convertToVO(versionEntity);
    }

    @Override
    public WorkflowVersionVO getCurrentVersion(Long workflowId) {
        log.debug("获取当前版本，工作流ID: {}", workflowId);

        WorkflowVersionEntity versionEntity = versionRepository
                .findByWorkflowIdAndIsCurrentTrue(workflowId)
                .orElseThrow(() -> new WorkflowException("当前版本不存在"));

        return convertToVO(versionEntity);
    }

    @Override
    @Transactional
    public WorkflowEntity rollbackToVersion(Long workflowId, Long versionId) {
        log.info("回滚工作流到指定版本，工作流ID: {}，版本ID: {}", workflowId, versionId);

        // 获取目标版本
        WorkflowVersionEntity targetVersion = versionRepository.findById(versionId)
                .orElseThrow(() -> new WorkflowException("目标版本不存在"));

        // 验证版本是否属于该工作流
        if (!targetVersion.getWorkflowId().equals(workflowId)) {
            throw new WorkflowException("版本不属于该工作流");
        }

        // 解析工作流数据
        WorkflowEntity workflowData = JsonUtil.fromJson(targetVersion.getWorkflowData(), WorkflowEntity.class);

        // 更新工作流
        WorkflowEntity currentWorkflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowException("工作流不存在"));

        BeanUtils.copyProperties(workflowData, currentWorkflow, "id", "createTime");
        currentWorkflow.setUpdateTime(LocalDateTime.now());

        // 保存工作流
        workflowRepository.save(currentWorkflow);

        // 创建回滚记录（作为新版本）
        WorkflowVersionDTO rollbackVersion = new WorkflowVersionDTO();
        rollbackVersion.setWorkflowId(workflowId);
        rollbackVersion.setVersionName("Rollback to v" + targetVersion.getVersionNumber());
        rollbackVersion.setDescription("回滚到版本 " + targetVersion.getVersionNumber());
        rollbackVersion.setChangeSummary("回滚操作");

        createVersion(rollbackVersion);

        log.info("工作流回滚成功，工作流ID: {}，回滚到版本: {}",
                workflowId, targetVersion.getVersionNumber());

        return currentWorkflow;
    }

    @Override
    public VersionComparisonResult compareVersions(Long versionId1, Long versionId2) {
        log.debug("比较版本，版本1: {}，版本2: {}", versionId1, versionId2);

        WorkflowVersionEntity version1 = versionRepository.findById(versionId1)
                .orElseThrow(() -> new WorkflowException("版本1不存在"));

        WorkflowVersionEntity version2 = versionRepository.findById(versionId2)
                .orElseThrow(() -> new WorkflowException("版本2不存在"));

        // 比较工作流数据
        WorkflowEntity workflow1 = JsonUtil.fromJson(version1.getWorkflowData(), WorkflowEntity.class);
        WorkflowEntity workflow2 = JsonUtil.fromJson(version2.getWorkflowData(), WorkflowEntity.class);

        VersionComparisonResult result = new VersionComparisonResult();
        result.setVersion1(convertToVO(version1));
        result.setVersion2(convertToVO(version2));
        result.setDifferences(compareWorkflows(workflow1, workflow2));

        return result;
    }

    @Override
    @Transactional
    public boolean deleteVersion(Long versionId) {
        log.info("删除版本，版本ID: {}", versionId);

        WorkflowVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new WorkflowException("版本不存在"));

        // 不能删除当前版本
        if (Boolean.TRUE.equals(version.getIsCurrent())) {
            throw new WorkflowException("不能删除当前版本");
        }

        versionRepository.deleteById(versionId);

        log.info("删除版本成功，版本ID: {}", versionId);
        return true;
    }

    @Override
    @Transactional
    public boolean batchDeleteVersions(List<Long> versionIds) {
        log.info("批量删除版本，数量: {}", versionIds.size());

        // 检查是否有当前版本
        List<WorkflowVersionEntity> versions = versionRepository.findAllById(versionIds);
        boolean hasCurrentVersion = versions.stream()
                .anyMatch(v -> Boolean.TRUE.equals(v.getIsCurrent()));

        if (hasCurrentVersion) {
            throw new WorkflowException("不能删除当前版本");
        }

        versionRepository.deleteAllById(versionIds);

        log.info("批量删除版本成功，数量: {}", versionIds.size());
        return true;
    }

    @Override
    @Transactional
    public boolean tagVersion(Long versionId, String tag) {
        log.info("标记版本，版本ID: {}，标签: {}", versionId, tag);

        WorkflowVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new WorkflowException("版本不存在"));

        String currentTags = version.getTags();
        if (!StringUtils.hasText(currentTags)) {
            version.setTags(tag);
        } else {
            // 检查标签是否已存在
            Set<String> tagSet = new HashSet<>(Arrays.asList(currentTags.split(",")));
            tagSet.add(tag);
            version.setTags(String.join(",", tagSet));
        }

        versionRepository.save(version);

        log.info("标记版本成功，版本ID: {}，标签: {}", versionId, tag);
        return true;
    }

    @Override
    public List<WorkflowVersionVO> searchVersions(Long workflowId, VersionSearchCriteria criteria) {
        log.debug("搜索版本，工作流ID: {}，条件: {}", workflowId, criteria);

        List<WorkflowVersionEntity> results = new ArrayList<>();

        if (criteria.getTag() != null) {
            results = versionRepository.findByWorkflowIdAndTagsContaining(workflowId, criteria.getTag());
        } else if (criteria.getCreatedBy() != null) {
            results = versionRepository.findByWorkflowIdAndCreatedBy(workflowId, criteria.getCreatedBy());
        } else if (criteria.getStartTime() != null && criteria.getEndTime() != null) {
            results = versionRepository.findByCreateTimeBetween(workflowId,
                    criteria.getStartTime(), criteria.getEndTime());
        } else {
            // 默认返回最新版本
            Pageable pageable = PageRequest.of(0, criteria.getLimit() != null ? criteria.getLimit() : 10);
            results = versionRepository.findLatestVersions(workflowId, pageable);
        }

        return results.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public String exportVersion(Long versionId) {
        log.debug("导出版本数据，版本ID: {}", versionId);

        WorkflowVersionEntity version = versionRepository.findById(versionId)
                .orElseThrow(() -> new WorkflowException("版本不存在"));

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("versionInfo", convertToVO(version));
        exportData.put("workflowData", JsonUtil.fromJson(version.getWorkflowData(), Map.class));

        if (StringUtils.hasText(version.getNodeData())) {
            exportData.put("nodeData", JsonUtil.fromJson(version.getNodeData(), List.class));
        }

        if (StringUtils.hasText(version.getValidationData())) {
            exportData.put("validationData", JsonUtil.fromJson(version.getValidationData(), List.class));
        }

        return JsonUtil.toJson(exportData);
    }

    @Override
    @Transactional
    public WorkflowVersionVO importVersion(String versionData) {
        log.info("导入版本数据");

        try {
            Map<String, Object> importData = JsonUtil.fromJson(versionData, Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> versionInfo = (Map<String, Object>) importData.get("versionInfo");

            WorkflowVersionDTO versionDTO = new WorkflowVersionDTO();
            versionDTO.setWorkflowId(Long.parseLong(versionInfo.get("workflowId").toString()));
            versionDTO.setVersionName((String) versionInfo.get("versionName"));
            versionDTO.setDescription((String) versionInfo.get("description"));

            // 设置工作流数据
            versionDTO.setWorkflowData(JsonUtil.toJson(importData.get("workflowData")));

            // 设置节点数据
            if (importData.containsKey("nodeData")) {
                versionDTO.setNodeData(JsonUtil.toJson(importData.get("nodeData")));
            }

            // 设置验证规则数据
            if (importData.containsKey("validationData")) {
                versionDTO.setValidationData(JsonUtil.toJson(importData.get("validationData")));
            }

            // 创建版本
            return createVersion(versionDTO);

        } catch (Exception e) {
            log.error("导入版本数据失败", e);
            throw new WorkflowException("导入版本数据失败: " + e.getMessage());
        }
    }

    /**
     * 转换为视图对象
     */
    private WorkflowVersionVO convertToVO(WorkflowVersionEntity entity) {
        WorkflowVersionVO vo = new WorkflowVersionVO();
        BeanUtils.copyProperties(entity, vo);

        // 解析标签
        if (StringUtils.hasText(entity.getTags())) {
            vo.setTagList(Arrays.asList(entity.getTags().split(",")));
        }

        // 计算数据大小
        int dataSize = entity.getWorkflowData().length();
        if (entity.getNodeData() != null) {
            dataSize += entity.getNodeData().length();
        }
        if (entity.getValidationData() != null) {
            dataSize += entity.getValidationData().length();
        }
        vo.setDataSize(dataSize);

        return vo;
    }

    /**
     * 比较两个工作流的差异
     */
    private List<VersionDifference> compareWorkflows(WorkflowEntity workflow1, WorkflowEntity workflow2) {
        List<VersionDifference> differences = new ArrayList<>();

        // 比较名称
        if (!Objects.equals(workflow1.getName(), workflow2.getName())) {
            differences.add(new VersionDifference("name",
                    workflow1.getName(), workflow2.getName()));
        }

        // 比较描述
        if (!Objects.equals(workflow1.getDescription(), workflow2.getDescription())) {
            differences.add(new VersionDifference("description",
                    workflow1.getDescription(), workflow2.getDescription()));
        }

        // 比较配置
        if (!Objects.equals(workflow1.getConfig(), workflow2.getConfig())) {
            differences.add(new VersionDifference("config",
                    workflow1.getConfig(), workflow2.getConfig()));
        }

        // 比较状态
        if (!Objects.equals(workflow1.getStatus(), workflow2.getStatus())) {
            differences.add(new VersionDifference("status",
                    workflow1.getStatus(), workflow2.getStatus()));
        }

        return differences;
    }

    /**
     * 版本比较结果类
     */
    @Data
    public static class VersionComparisonResult {
        private WorkflowVersionVO version1;
        private WorkflowVersionVO version2;
        private List<VersionDifference> differences;
        private LocalDateTime comparisonTime = LocalDateTime.now();
    }

    /**
     * 版本差异类
     */
    @Data
    @AllArgsConstructor
    public static class VersionDifference {
        private String field;
        private Object oldValue;
        private Object newValue;
    }

    /**
     * 版本搜索条件类
     */
    @Data
    public static class VersionSearchCriteria {
        private String tag;
        private String createdBy;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer limit;
    }
}
