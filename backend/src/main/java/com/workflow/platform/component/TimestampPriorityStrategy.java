package com.workflow.platform.component;

import com.workflow.platform.enums.ConflictResolutionType;
import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.model.entity.WorkflowEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间戳优先策略
 */
@Component("timestampPriorityStrategy")
@Slf4j
public class TimestampPriorityStrategy implements ConflictResolutionStrategy {

    @Override
    public WorkflowEntity resolveWorkflowConflict(WorkflowEntity localWorkflow,
                                                  WorkflowEntity remoteWorkflow,
                                                  SyncTaskDTO syncTask) {
        log.info("使用时间戳优先策略解决工作流冲突");

        LocalDateTime localTime = localWorkflow.getUpdateTime();
        LocalDateTime remoteTime = remoteWorkflow.getUpdateTime();

        if (localTime == null) localTime = localWorkflow.getCreateTime();
        if (remoteTime == null) remoteTime = remoteWorkflow.getCreateTime();

        if (localTime.isAfter(remoteTime)) {
            log.info("本地版本更新，使用本地版本");
            return localWorkflow;
        } else if (remoteTime.isAfter(localTime)) {
            log.info("远程版本更新，使用远程版本");
            return remoteWorkflow;
        } else {
            // 时间相同，使用版本号
            int localVersion = localWorkflow.getVersion() != null ? Integer.parseInt(localWorkflow.getVersion()) : 0;
            int remoteVersion = remoteWorkflow.getVersion() != null ? Integer.parseInt(remoteWorkflow.getVersion()) : 0;

            if (localVersion > remoteVersion) {
                log.info("本地版本号更高，使用本地版本");
                return localWorkflow;
            } else {
                log.info("远程版本号更高或相同，使用远程版本");
                return remoteWorkflow;
            }
        }
    }

    @Override
    public List<Object> resolveNodeConflict(List<Object> localNodes,
                                            List<Object> remoteNodes,
                                            SyncTaskDTO syncTask) {
        log.info("使用时间戳优先策略解决节点冲突");
        // 合并节点，比较每个节点的时间戳
        Map<String, Object> mergedNodes = new HashMap<>();

        // 处理远程节点
        remoteNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            String nodeId = (String) nodeMap.get("nodeId");
            mergedNodes.put(nodeId, node);
        });

        // 处理本地节点
        localNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            String nodeId = (String) nodeMap.get("nodeId");

            if (mergedNodes.containsKey(nodeId)) {
                // 比较时间戳
                Map<String, Object> existingNode = (Map<String, Object>) mergedNodes.get(nodeId);
                LocalDateTime localTime = parseTimestamp(nodeMap.get("updateTime"));
                LocalDateTime remoteTime = parseTimestamp(existingNode.get("updateTime"));

                if (localTime != null && remoteTime != null && localTime.isAfter(remoteTime)) {
                    mergedNodes.put(nodeId, node);
                }
            } else {
                mergedNodes.put(nodeId, node);
            }
        });

        return new ArrayList<>(mergedNodes.values());
    }

    @Override
    public List<Object> resolveValidationConflict(List<Object> localRules,
                                                  List<Object> remoteRules,
                                                  SyncTaskDTO syncTask) {
        log.info("使用时间戳优先策略解决验证规则冲突");
        // 实现逻辑类似节点冲突
        Map<String, Object> mergedRules = new HashMap<>();

        remoteRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            String ruleId = (String) ruleMap.get("ruleId");
            mergedRules.put(ruleId, rule);
        });

        localRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            String ruleId = (String) ruleMap.get("ruleId");

            if (mergedRules.containsKey(ruleId)) {
                Map<String, Object> existingRule = (Map<String, Object>) mergedRules.get(ruleId);
                LocalDateTime localTime = parseTimestamp(ruleMap.get("updateTime"));
                LocalDateTime remoteTime = parseTimestamp(existingRule.get("updateTime"));

                if (localTime != null && remoteTime != null && localTime.isAfter(remoteTime)) {
                    mergedRules.put(ruleId, rule);
                }
            } else {
                mergedRules.put(ruleId, rule);
            }
        });

        return new ArrayList<>(mergedRules.values());
    }

    @Override
    public ConflictResolutionType getStrategyType() {
        return ConflictResolutionType.TIMESTAMP_PRIORITY;
    }

    private LocalDateTime parseTimestamp(Object timestamp) {
        if (timestamp == null) return null;
        if (timestamp instanceof LocalDateTime) {
            return (LocalDateTime) timestamp;
        } else if (timestamp instanceof String) {
            try {
                return LocalDateTime.parse((String) timestamp);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
