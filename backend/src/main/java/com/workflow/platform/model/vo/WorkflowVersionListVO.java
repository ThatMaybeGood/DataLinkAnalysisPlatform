package com.workflow.platform.model.vo;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:07
 */

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流版本列表视图对象
 *
 * 3. WorkflowVersionListVO
 *
 * 用于版本列表展示
 * 包含分页信息和统计信息
 * 优化列表显示数据结构
 *
 */
@Data
@ApiModel(description = "工作流版本列表视图对象")
public class WorkflowVersionListVO {

    @ApiModelProperty(value = "总记录数", example = "100")
    private Long total;

    @ApiModelProperty(value = "当前页码", example = "1")
    private Integer page;

    @ApiModelProperty(value = "每页大小", example = "10")
    private Integer size;

    @ApiModelProperty(value = "总页数", example = "10")
    private Integer totalPages;

    @ApiModelProperty(value = "版本列表")
    private List<WorkflowVersionItemVO> versions;

    @ApiModelProperty(value = "当前版本信息")
    private WorkflowVersionItemVO currentVersion;

    @ApiModelProperty(value = "版本统计信息")
    private VersionListStatistics statistics;

    /**
     * 版本列表项
     */
    @Data
    @ApiModel(description = "工作流版本列表项")
    public static class WorkflowVersionItemVO {

        @ApiModelProperty(value = "版本ID", example = "1")
        private Long id;

        @ApiModelProperty(value = "版本号", example = "1")
        private Integer versionNumber;

        @ApiModelProperty(value = "版本名称", example = "v1.0.0")
        private String versionName;

        @ApiModelProperty(value = "版本描述", example = "初始版本")
        private String description;

        @ApiModelProperty(value = "创建人", example = "admin")
        private String createdBy;

        @ApiModelProperty(value = "创建时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createTime;

        @ApiModelProperty(value = "是否为当前版本", example = "true")
        private Boolean isCurrent;

        @ApiModelProperty(value = "标签列表")
        private List<String> tags;

        @ApiModelProperty(value = "变更摘要", example = "新增了验证节点")
        private String changeSummary;

        @ApiModelProperty(value = "数据大小", example = "2048")
        private Integer dataSize;

        @ApiModelProperty(value = "节点数量", example = "10")
        private Integer nodeCount;

        @ApiModelProperty(value = "是否可以回滚", example = "true")
        private Boolean canRollback;

        @ApiModelProperty(value = "是否可以删除", example = "true")
        private Boolean canDelete;

        @ApiModelProperty(value = "版本状态", example = "ACTIVE")
        private String status;
    }

    /**
     * 版本列表统计信息
     */
    @Data
    @ApiModel(description = "版本列表统计信息")
    public static class VersionListStatistics {

        @ApiModelProperty(value = "总版本数", example = "50")
        private Integer totalVersions;

        @ApiModelProperty(value = "当前版本", example = "v2.1.0")
        private String currentVersion;

        @ApiModelProperty(value = "最新版本", example = "v2.1.0")
        private String latestVersion;

        @ApiModelProperty(value = "最早版本", example = "v1.0.0")
        private String earliestVersion;

        @ApiModelProperty(value = "稳定版本数", example = "10")
        private Integer stableVersions;

        @ApiModelProperty(value = "草案版本数", example = "5")
        private Integer draftVersions;

        @ApiModelProperty(value = "归档版本数", example = "35")
        private Integer archivedVersions;

        @ApiModelProperty(value = "平均版本大小", example = "1536")
        private Integer avgVersionSize;

        @ApiModelProperty(value = "最近7天创建版本数", example = "3")
        private Integer versionsLast7Days;

        @ApiModelProperty(value = "版本增长率", example = "15.5")
        private Double growthRate;
    }
}