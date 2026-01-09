// backend/src/main/java/com/workflow/platform/service/WorkflowService.java
package com.workflow.platform.service;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.vo.WorkflowVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkflowService {
    WorkflowVO createWorkflow(WorkflowDTO workflowDTO);
    WorkflowVO updateWorkflow(String id, WorkflowDTO workflowDTO);
    WorkflowVO getWorkflow(String id);
    void deleteWorkflow(String id);
    Page<WorkflowVO> queryWorkflows(Pageable pageable);
    // 其他方法...
}