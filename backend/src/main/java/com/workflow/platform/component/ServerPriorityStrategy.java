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
 * 服务器优先策略
 */
@Component("serverPriorityStrategy")
@Slf4j
public class ServerPriorityStrategy implements ConflictResolutionStrategy {

    @Override
    public WorkflowEntity resolveWorkflowConflict(WorkflowEntity localWorkflow,
                                                  WorkflowEntity remoteWorkflow,
                                                  SyncTaskDTO syncTask) {
        log.info("使用服务器优先策略解决工作流冲突，工作流ID: {}", remoteWorkflow.getId());
        // 服务器版本总是优先
        log.info("使用服务器版本，服务器更新时间: {}", remoteWorkflow.getUpdateTime());
        return remoteWorkflow;
    }

    @Override
    public List<Object> resolveNodeConflict(List<Object> localNodes,
                                            List<Object> remoteNodes,
                                            SyncTaskDTO syncTask) {
        log.info("使用服务器优先策略解决节点冲突");
        // 合并节点，远程节点优先
        Map<String, Object> mergedNodes = new HashMap<>();

        // 先添加本地节点
        localNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            mergedNodes.put((String) nodeMap.get("nodeId"), node);
        });

        // 用远程节点覆盖本地节点
        remoteNodes.forEach(node -> {
            Map<String, Object> nodeMap = (Map<String, Object>) node;
            mergedNodes.put((String) nodeMap.get("nodeId"), node);
        });

        return new ArrayList<>(mergedNodes.values());
    }

    @Override
    public List<Object> resolveValidationConflict(List<Object> localRules,
                                                  List<Object> remoteRules,
                                                  SyncTaskDTO syncTask) {
        log.info("使用服务器优先策略解决验证规则冲突");
        // 合并规则，远程规则优先
        Map<String, Object> mergedRules = new HashMap<>();

        localRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            mergedRules.put((String) ruleMap.get("ruleId"), rule);
        });

        remoteRules.forEach(rule -> {
            Map<String, Object> ruleMap = (Map<String, Object>) rule;
            mergedRules.put((String) ruleMap.get("ruleId"), rule);
        });

        return new ArrayList<>(mergedRules.values());
    }

    @Override
    public ConflictResolutionType getStrategyType() {
        return ConflictResolutionType.SERVER_PRIORITY;
    }
}
