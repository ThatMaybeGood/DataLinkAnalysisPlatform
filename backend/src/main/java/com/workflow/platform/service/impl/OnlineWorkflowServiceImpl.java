// backend/src/main/java/com/workflow/platform/service/impl/OnlineWorkflowServiceImpl.java
package com.workflow.platform.service.impl;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.vo.WorkflowVO;
import com.workflow.platform.repository.WorkflowRepository;
import com.workflow.platform.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnMode("online")
public class OnlineWorkflowServiceImpl implements WorkflowService {
    private final WorkflowRepository workflowRepository;

    @Override
    public WorkflowVO createWorkflow(WorkflowDTO workflowDTO) {
        // 在线模式实现：保存到数据库
        // ...
    }

    @Override
    public WorkflowVO updateWorkflow(String id, WorkflowDTO workflowDTO) {
        // 在线模式实现
        // ...
    }

    // 其他方法...
}