// backend/src/main/java/com/workflow/platform/service/impl/OfflineWorkflowServiceImpl.java
package com.workflow.platform.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.vo.WorkflowVO;
import com.workflow.platform.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@ConditionalOnMode("offline")
public class OfflineWorkflowServiceImpl implements WorkflowService {
    private final ObjectMapper objectMapper;
    private final String STORAGE_PATH = "./data/workflows";

    @Override
    public WorkflowVO createWorkflow(WorkflowDTO workflowDTO) {
        // 离线模式实现：保存到文件
        String fileName = workflowDTO.getId() + ".json";
        Path filePath = Paths.get(STORAGE_PATH, fileName);
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writeValue(filePath.toFile(), workflowDTO);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save workflow to file", e);
        }
        // 转换为VO返回
        return convertToVO(workflowDTO);
    }

    @Override
    public WorkflowVO updateWorkflow(String id, WorkflowDTO workflowDTO) {
        // 离线模式实现：更新文件
        // ...
    }

    @Override
    public Page<WorkflowVO> queryWorkflows(Pageable pageable) {
        // 从文件系统读取所有工作流
        List<WorkflowVO> workflows = listAllWorkflows();
        // 实现分页逻辑
        // ...
    }

    private List<WorkflowVO> listAllWorkflows() {
        try {
            return Files.list(Paths.get(STORAGE_PATH))
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> {
                        try {
                            WorkflowDTO dto = objectMapper.readValue(path.toFile(), WorkflowDTO.class);
                            return convertToVO(dto);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to read workflow file", e);
                        }
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to list workflow files", e);
        }
    }

    private WorkflowVO convertToVO(WorkflowDTO dto) {
        // 转换逻辑
        // ...
    }
}