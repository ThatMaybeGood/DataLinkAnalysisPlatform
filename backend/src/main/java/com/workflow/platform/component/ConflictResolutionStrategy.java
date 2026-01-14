package com.workflow.platform.component;

import com.workflow.platform.exception.ConflictException;
import com.workflow.platform.model.entity.ConflictRecordEntity;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.enums.ConflictResolutionType;
import com.workflow.platform.service.ConflictRecordService;
import com.workflow.platform.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 冲突解决策略接口
 */
public interface ConflictResolutionStrategy {

    /**
     * 解决工作流冲突
     * @param localWorkflow 本地工作流
     * @param remoteWorkflow 远程工作流
     * @param syncTask 同步任务信息
     * @return 解决后的工作流
     */
    WorkflowEntity resolveWorkflowConflict(WorkflowEntity localWorkflow,
                                           WorkflowEntity remoteWorkflow,
                                           SyncTaskDTO syncTask);

    /**
     * 解决节点冲突
     * @param localNodes 本地节点列表
     * @param remoteNodes 远程节点列表
     * @param syncTask 同步任务信息
     * @return 解决后的节点列表
     */
    List<Object> resolveNodeConflict(List<Object> localNodes,
                                     List<Object> remoteNodes,
                                     SyncTaskDTO syncTask);

    /**
     * 解决验证规则冲突
     * @param localRules 本地规则列表
     * @param remoteRules 远程规则列表
     * @param syncTask 同步任务信息
     * @return 解决后的规则列表
     */
    List<Object> resolveValidationConflict(List<Object> localRules,
                                           List<Object> remoteRules,
                                           SyncTaskDTO syncTask);

    /**
     * 获取策略类型
     * @return 冲突解决类型
     */
    ConflictResolutionType getStrategyType();
}

