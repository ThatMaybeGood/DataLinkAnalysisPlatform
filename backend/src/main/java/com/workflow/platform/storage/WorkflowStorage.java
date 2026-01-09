package com.workflow.platform.storage;

import com.workflow.platform.model.dto.WorkflowDTO;
import java.util.List;

public interface WorkflowStorage {
    WorkflowDTO saveWorkflow(WorkflowDTO workflow);
    WorkflowDTO getWorkflow(String id);
    void deleteWorkflow(String id);
    List<WorkflowDTO> getAllWorkflows();
}