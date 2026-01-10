package com.workflow.platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 工作流版本数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersionDTO {
    private String id;
    private String workflowId;
    private int versionNumber;
    private String versionTag;
    private String description;
    private WorkflowDTO workflowData;
    private String createdBy;
    private long createdAt;
    private String checksum;
    private long size;
    private Map<String, Object> metadata;
    private List<String> tags;

    // 分支相关
    private boolean isBranch;
    private String branchName;
    private String baseWorkflowId;
    private Integer baseVersion;

    // 恢复相关
    private String restoredBy;
    private Long restoredAt;
    private Integer restoreCount;

    // 差异分析
    private Map<String, Object> changeSummary;
    private Integer changeCount;

    public String getFullVersionName() {
        if (isBranch) {
            return String.format("%s/%s/v%d", branchName, workflowId, versionNumber);
        } else {
            return String.format("v%d", versionNumber);
        }
    }
}