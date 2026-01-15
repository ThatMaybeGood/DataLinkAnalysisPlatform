package com.workflow.platform.model.vo;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.entity.WorkflowEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/15 23:53
 */
@Component
public class WorkflowConverter {


    public WorkflowEntity toEntity(WorkflowDTO workflowDTO) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(workflowDTO.getId());
        entity.setName(workflowDTO.getName());
        entity.setAlias(workflowDTO.getAlias());
        entity.setDescription(workflowDTO.getDescription());
        entity.setCategory(workflowDTO.getCategory());
//        entity.setTags(workflowDTO.getTags());
        entity.setStatus(workflowDTO.getStatus());
        entity.setMode(workflowDTO.getMode());
        entity.setNodeCount(workflowDTO.getNodeCount());
        entity.setExecutionCount(workflowDTO.getExecutionCount());
        entity.setSuccessRate(workflowDTO.getSuccessRate());
        entity.setAvgDuration(workflowDTO.getAvgDuration());
        entity.setCreatedAt(workflowDTO.getCreatedAt());
        entity.setUpdatedAt(workflowDTO.getUpdatedAt());
        entity.setCreatedBy(workflowDTO.getCreatedBy());
        entity.setUpdatedBy(workflowDTO.getUpdatedBy());
        return entity;
    }

    public WorkflowVO toVO(WorkflowEntity savedEntity) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(savedEntity.getId());
        vo.setName(savedEntity.getName());
        vo.setAlias(savedEntity.getAlias());
        vo.setDescription(savedEntity.getDescription());
        vo.setCategory(savedEntity.getCategory());
        vo.setTags(Collections.singletonList(savedEntity.getTags()));
        vo.setStatus(savedEntity.getStatus());
        vo.setMode(savedEntity.getMode());
        vo.setNodeCount(savedEntity.getNodeCount());
        vo.setExecutionCount(savedEntity.getExecutionCount());
        vo.setSuccessRate(savedEntity.getSuccessRate());
        vo.setAvgDuration(savedEntity.getAvgDuration());
        vo.setCreatedAt(savedEntity.getCreatedAt());
        vo.setUpdatedAt(savedEntity.getUpdatedAt());
        vo.setCreatedBy(savedEntity.getCreatedBy());
        vo.setUpdatedBy(savedEntity.getUpdatedBy());
        return vo;
    }

    public void updateEntity(WorkflowEntity entity, WorkflowDTO workflowDTO) {
        entity.setName(workflowDTO.getName());
        entity.setAlias(workflowDTO.getAlias());
        entity.setDescription(workflowDTO.getDescription());
        entity.setCategory(workflowDTO.getCategory());
        entity.setTags(workflowDTO.getTags().toString());
        entity.setStatus(workflowDTO.getStatus());
        entity.setMode(workflowDTO.getMode());
        entity.setNodeCount(workflowDTO.getNodeCount());
        entity.setExecutionCount(workflowDTO.getExecutionCount());
        entity.setSuccessRate(workflowDTO.getSuccessRate());
        entity.setAvgDuration(workflowDTO.getAvgDuration());
        entity.setUpdatedAt(workflowDTO.getUpdatedAt());
        entity.setUpdatedBy(workflowDTO.getUpdatedBy());
    }

    public WorkflowDTO toDTO(WorkflowEntity entity) {
        return WorkflowDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .alias(entity.getAlias())
                .description(entity.getDescription())
                .category(entity.getCategory())
                .tags(Collections.singletonList(entity.getTags()))
                .status(entity.getStatus())
                .mode(entity.getMode())
                .nodeCount(entity.getNodeCount())
                .executionCount(entity.getExecutionCount())
                .successRate(entity.getSuccessRate())
                .avgDuration(entity.getAvgDuration())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
