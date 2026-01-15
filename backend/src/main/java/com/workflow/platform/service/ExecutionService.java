package com.workflow.platform.service;

import com.workflow.platform.service.impl.ExecutionRecord;

import java.util.List;

public interface ExecutionService {
    void save(ExecutionRecord execution);
    void update(ExecutionRecord execution);
    void delete(String id);
    ExecutionRecord findById(String id);
    ExecutionRecord findByWorkflowId(String workflowId);
    ExecutionRecord findByStatus(String status);
    List<ExecutionRecord> findAll();
    List<ExecutionRecord> findByWorkflowIdAndStatus(String workflowId, String status);
}
