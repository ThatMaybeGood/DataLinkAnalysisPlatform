package com.workflow.platform.component;

import com.workflow.platform.enums.ConflictResolutionType;
import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.model.entity.WorkflowEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端优先策略
 */
@Component("clientPriorityStrategy")
@Slf4j
public class ClientPriorityStrategy implements ConflictResolutionStrategy {

    @Override
    public WorkflowEntity resolveWorkflowConflict(WorkflowEntity localWorkflow,
                                                  WorkflowEntity remoteWorkflow,
                                                  SyncTaskDTO syncTask) {
        log.info("使用客户端优先策略解决工作流冲突，工作流ID: {}", localWorkflow.getId());
        // 比较版本时间戳，选择最新的
        if (localWorkflow.getUpdateTime().isAfter(remoteWorkflow.getUpdateTime())) {
            log.info("使用本地版本，本地更新时间: {}", localWorkflow.getUpdateTime());
            return localWorkflow;
        } else {
            log.info("使用远程版本，远程更新时间: {}", remoteWorkflow.getUpdateTime());
            return remoteWorkflow;
        }
    }

    @Override
    public List<Object> resolveNodeConflict(List<Object> localNodes,
                                            List<Object> remoteNodes,
                                            SyncTaskDTO syncTask) {
        log.info("使用客户端优先策略解决节点冲突");
        // 合并节点，本地节点优先
        Map<String, Object> mergedNodes = new HashMap<>();

        // 先添加远程节点
        remoteNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            mergedNodes.put((String) nodeMap.get("nodeId"), node);
        });

        // 用本地节点覆盖远程节点
        localNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            mergedNodes.put((String) nodeMap.get("nodeId"), node);
        });

        return new ArrayList<>(mergedNodes.values());
    }

    @Override
    public List<Object> resolveValidationConflict(List<Object> localRules,
                                                  List<Object> remoteRules,
                                                  SyncTaskDTO syncTask) {
        log.info("使用客户端优先策略解决验证规则冲突");
        // 合并规则，本地规则优先
        Map<String, Object> mergedRules = new HashMap<>();

        remoteRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            mergedRules.put((String) ruleMap.get("ruleId"), rule);
        });

        localRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            mergedRules.put((String) ruleMap.get("ruleId"), rule);
        });

        return new ArrayList<>(mergedRules.values());
    }

    @Override
    public ConflictResolutionType getStrategyType() {
        return ConflictResolutionType.CLIENT_PRIORITY;
    }
}
