package com.workflow.platform.component;

import com.workflow.platform.constants.ConflictStatus;
import com.workflow.platform.enums.ConflictResolutionType;
import com.workflow.platform.exception.ConflictException;
import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.model.entity.ConflictRecordEntity;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.service.ConflictRecordService;
import com.workflow.platform.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 手动解决策略
 */
@Component("manualResolutionStrategy")
@Slf4j
public class ManualResolutionStrategy implements ConflictResolutionStrategy {

    @Autowired
    private ConflictRecordService conflictRecordService;

    @Override
    public WorkflowEntity resolveWorkflowConflict(WorkflowEntity localWorkflow,
                                                  WorkflowEntity remoteWorkflow,
                                                  SyncTaskDTO syncTask) {
        log.info("检测到需要手动解决的工作流冲突，工作流ID: {}", localWorkflow.getId());

        // 创建冲突记录
        ConflictRecordEntity conflictRecord = new ConflictRecordEntity();
        conflictRecord.setConflictType("WORKFLOW");
        conflictRecord.setObjectId(localWorkflow.getId().toString());
        conflictRecord.setLocalData(JsonUtil.toJson(localWorkflow));
        conflictRecord.setRemoteData(JsonUtil.toJson(remoteWorkflow));
        conflictRecord.setSyncTaskId(syncTask.getId());
        conflictRecord.setStatus(ConflictStatus.PENDING);
        conflictRecord.setCreateTime(LocalDateTime.now());

        conflictRecordService.save(conflictRecord);

        throw new ConflictException("检测到工作流冲突，需要手动解决", conflictRecord);
    }

    @Override
    public List<Object> resolveNodeConflict(List<Object> localNodes,
                                            List<Object> remoteNodes,
                                            SyncTaskDTO syncTask) {
        log.info("检测到需要手动解决的节点冲突");

        // 找出冲突的节点
        Map<String, Object> localNodeMap = convertToMap(localNodes);
        Map<String, Object> remoteNodeMap = convertToMap(remoteNodes);

        List<String> conflictNodeIds = new ArrayList<>();
        for (String nodeId : localNodeMap.keySet()) {
            if (remoteNodeMap.containsKey(nodeId)) {
                Map<String, Object> localNode = (Map<String, Object>) localNodeMap.get(nodeId);
                Map<String, Object> remoteNode = (Map<String, Object>) remoteNodeMap.get(nodeId);

                if (!isNodesEqual(localNode, remoteNode)) {
                    conflictNodeIds.add(nodeId);
                }
            }
        }

        if (!conflictNodeIds.isEmpty()) {
            // 创建冲突记录
            ConflictRecordEntity conflictRecord = new ConflictRecordEntity();
            conflictRecord.setConflictType("NODE");
            conflictRecord.setObjectId(String.join(",", conflictNodeIds));
            conflictRecord.setLocalData(JsonUtil.toJson(localNodes));
            conflictRecord.setRemoteData(JsonUtil.toJson(remoteNodes));
            conflictRecord.setSyncTaskId(syncTask.getId());
            conflictRecord.setStatus(ConflictStatus.PENDING);
            conflictRecord.setCreateTime(LocalDateTime.now());

            conflictRecordService.save(conflictRecord);

            throw new ConflictException("检测到节点冲突，需要手动解决", conflictRecord);
        }

        // 如果没有冲突，合并节点
        return mergeNodes(localNodes, remoteNodes);
    }

    @Override
    public List<Object> resolveValidationConflict(List<Object> localRules,
                                                  List<Object> remoteRules,
                                                  SyncTaskDTO syncTask) {
        log.info("检测到需要手动解决的验证规则冲突");

        // 实现逻辑类似节点冲突
        // 这里简化处理，直接合并
        return mergeRules(localRules, remoteRules);
    }

    @Override
    public ConflictResolutionType getStrategyType() {
        return ConflictResolutionType.MANUAL;
    }

    private Map<String, Object> convertToMap(List<Object> nodes) {
        Map<String, Object> map = new HashMap<>();
        nodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            map.put((String) nodeMap.get("nodeId"), node);
        });
        return map;
    }

    private boolean isNodesEqual(Map<String, Object> node1, Map<String, Object> node2) {
        // 简化的相等比较，实际应该比较所有关键字段
        return Objects.equals(node1.get("contentHash"), node2.get("contentHash"));
    }

    private List<Object> mergeNodes(List<Object> localNodes, List<Object> remoteNodes) {
        Map<String, Object> merged = new HashMap<>();

        localNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            merged.put((String) nodeMap.get("nodeId"), node);
        });

        remoteNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            if (!merged.containsKey(nodeMap.get("nodeId"))) {
                merged.put((String) nodeMap.get("nodeId"), node);
            }
        });

        return new ArrayList<>(merged.values());
    }

    private List<Object> mergeRules(List<Object> localRules, List<Object> remoteRules) {
        Map<String, Object> merged = new HashMap<>();

        localRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            merged.put((String) ruleMap.get("ruleId"), rule);
        });

        remoteRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            if (!merged.containsKey(ruleMap.get("ruleId"))) {
                merged.put((String) ruleMap.get("ruleId"), rule);
            }
        });

        return new ArrayList<>(merged.values());
    }
}
