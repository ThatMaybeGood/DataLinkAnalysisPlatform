package com.workflow.platform.service;

import com.workflow.platform.model.entity.NodeEntity;

import java.util.List;

public interface NodeService {
    List<NodeEntity> getNodesByWorkflowId(String workflowId);

    void addNode(String workflowId, NodeEntity node);

    void deleteNode(String nodeId);

    void updateNode(String nodeId, NodeEntity updatedNode);

    NodeEntity getNodeById(String nodeId);

}
