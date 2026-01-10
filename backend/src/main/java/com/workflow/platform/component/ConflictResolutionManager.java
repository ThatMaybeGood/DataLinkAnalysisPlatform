package com.workflow.platform.component;

import com.workflow.platform.exception.ConflictException;
import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.enums.ConflictResolutionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 冲突解决管理器
 */
@Component
@Slf4j
public class ConflictResolutionManager {

    private final Map<ConflictResolutionType, ConflictResolutionStrategy> strategyMap;

    @Autowired
    public ConflictResolutionManager(List<ConflictResolutionStrategy> strategies) {
        strategyMap = new ConcurrentHashMap<>();
        strategies.forEach(strategy ->
                strategyMap.put(strategy.getStrategyType(), strategy)
        );
        log.info("初始化冲突解决管理器，加载策略: {}", strategyMap.keySet());
    }

    /**
     * 根据类型获取解决策略
     */
    public ConflictResolutionStrategy getStrategy(ConflictResolutionType type) {
        ConflictResolutionStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            log.warn("未找到冲突解决策略: {}，使用默认策略", type);
            strategy = strategyMap.get(ConflictResolutionType.CLIENT_PRIORITY);
        }
        return strategy;
    }

    /**
     * 解决工作流冲突
     */
    public WorkflowEntity resolveWorkflowConflict(WorkflowEntity localWorkflow,
                                                  WorkflowEntity remoteWorkflow,
                                                  SyncTaskDTO syncTask) {
        ConflictResolutionType resolutionType = determineResolutionType(syncTask);
        ConflictResolutionStrategy strategy = getStrategy(resolutionType);

        log.info("使用策略 {} 解决工作流冲突，工作流ID: {}",
                strategy.getStrategyType(), localWorkflow.getId());

        try {
            return strategy.resolveWorkflowConflict(localWorkflow, remoteWorkflow, syncTask);
        } catch (ConflictException e) {
            // 如果是手动解决策略的异常，直接抛出
            throw e;
        } catch (Exception e) {
            log.error("解决工作流冲突时发生异常", e);
            // 降级处理：使用客户端优先策略
            return strategyMap.get(ConflictResolutionType.CLIENT_PRIORITY)
                    .resolveWorkflowConflict(localWorkflow, remoteWorkflow, syncTask);
        }
    }

    /**
     * 解决节点冲突
     */
    public List<Object> resolveNodeConflict(List<Object> localNodes,
                                            List<Object> remoteNodes,
                                            SyncTaskDTO syncTask) {
        ConflictResolutionType resolutionType = determineResolutionType(syncTask);
        ConflictResolutionStrategy strategy = getStrategy(resolutionType);

        log.info("使用策略 {} 解决节点冲突", strategy.getStrategyType());

        try {
            return strategy.resolveNodeConflict(localNodes, remoteNodes, syncTask);
        } catch (ConflictException e) {
            throw e;
        } catch (Exception e) {
            log.error("解决节点冲突时发生异常", e);
            return strategyMap.get(ConflictResolutionType.CLIENT_PRIORITY)
                    .resolveNodeConflict(localNodes, remoteNodes, syncTask);
        }
    }

    /**
     * 解决验证规则冲突
     */
    public List<Object> resolveValidationConflict(List<Object> localRules,
                                                  List<Object> remoteRules,
                                                  SyncTaskDTO syncTask) {
        ConflictResolutionType resolutionType = determineResolutionType(syncTask);
        ConflictResolutionStrategy strategy = getStrategy(resolutionType);

        log.info("使用策略 {} 解决验证规则冲突", strategy.getStrategyType());

        try {
            return strategy.resolveValidationConflict(localRules, remoteRules, syncTask);
        } catch (Exception e) {
            log.error("解决验证规则冲突时发生异常", e);
            return strategyMap.get(ConflictResolutionType.CLIENT_PRIORITY)
                    .resolveValidationConflict(localRules, remoteRules, syncTask);
        }
    }

    /**
     * 根据同步任务确定解决类型
     */
    private ConflictResolutionType determineResolutionType(SyncTaskDTO syncTask) {
        if (syncTask != null && syncTask.getConflictResolutionType() != null) {
            return syncTask.getConflictResolutionType();
        }

        // 默认策略：根据环境确定
        String defaultStrategy = System.getProperty("workflow.conflict.defaultStrategy",
                "CLIENT_PRIORITY");
        try {
            return ConflictResolutionType.valueOf(defaultStrategy);
        } catch (IllegalArgumentException e) {
            log.warn("无效的默认冲突解决策略: {}，使用CLIENT_PRIORITY", defaultStrategy);
            return ConflictResolutionType.CLIENT_PRIORITY;
        }
    }
}