
package com.workflow.platform.model.dto;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import groovy.transform.builder.Builder;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 工作流版本数据传输对象
 *
 * 2. WorkflowVersionDTO
 *
 * 用于接收创建/更新版本的请求数据
 * 包含必要的验证注解
 * 支持灵活的版本创建选项
 *
 */
@Data
@Builder
@ApiModel(description = "工作流版本数据传输对象")
public class WorkflowVersionDTO {

	@ApiModelProperty(value = "工作流ID", required = true, example = "1")
	@NotNull(message = "工作流ID不能为空")
	private String workflowId;

	@ApiModelProperty(value = "版本名称", example = "v1.0.0")
	private String versionName;

	@ApiModelProperty(value = "版本描述", example = "初始版本")
	private String description;

	@ApiModelProperty(value = "变更摘要", example = "新增了验证节点")
	private String changeSummary;

	@ApiModelProperty(value = "工作流数据（JSON格式）")
	private WorkflowDTO workflowData;

	@ApiModelProperty(value = "节点数据（JSON格式）")
	private String nodeData;

	@ApiModelProperty(value = "验证规则数据（JSON格式）")
	private String validationData;

	@ApiModelProperty(value = "创建人", example = "admin")
	private String createdBy;

	@ApiModelProperty(value = "版本标签", example = "stable,draft")
	private List<String> tags;

	@ApiModelProperty(value = "是否设置为当前版本", example = "true")
	private Boolean setAsCurrent = true;

	@ApiModelProperty(value = "是否自动生成版本名称", example = "true")
	private Boolean autoGenerateName = true;

	@ApiModelProperty(value = "是否包含元数据", example = "true")
	private Boolean includeMetadata = true;

	@ApiModelProperty(value = "自定义元数据")
	private Map<String, Object> metadata;

	@ApiModelProperty(value = "版本类型", example = "MAJOR", allowableValues = "MAJOR,MINOR,PATCH,CUSTOM")
	private String versionType;

	private String id;

	private int versionNumber;

	private String versionTag;

	private long createdAt;

	private String checksum;

	private long size;

	private String restoredBy;

	private Long restoredAt;

	private Integer restoreCount;

	private Map<String, Object> changeSummaryMap;

	private Integer changeCount;

	// 分支相关
	private boolean isBranch;
	private String branchName;
	private String baseWorkflowId;
	private Integer baseVersion;

}