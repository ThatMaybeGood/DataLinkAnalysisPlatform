package com.workflow.platform.service;

import com.workflow.platform.model.dto.NodeExecutionResult;
import com.workflow.platform.model.entity.NodeEntity;

import java.util.Map;

public interface NodeExecutor {
    void executeNode(NodeEntity node, Map<String, Object> context);

    NodeExecutionResult execute(NodeEntity currentNodeEntity, Map<String, Object> context);
}
