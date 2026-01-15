package com.workflow.platform.model.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/16 00:13
 */
@Data
@Builder
public class WorkflowExportDataDTO {
    private WorkflowDTO workflow;
    private String aliases;
    private List<RuleDTO> validationRules;
    private Long exportTime;
    private String version;
    private List<NodeDTO> nodes;
    private String connectors;
    private String executionLogs;
}
