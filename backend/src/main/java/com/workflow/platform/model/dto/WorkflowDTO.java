package com.workflow.platform.model.dto;

import java.time.LocalDateTime;
// import java.util.Collection;	
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * 工作流数据传输对象（DTO）
 * 用于前端与后端之间的数据传输
 */
@Data
@Builder
@ApiModel(description = "工作流数据传输对象")
public class WorkflowDTO {

	@ApiModelProperty(value = "工作流ID", example = "workflow_123456")
	private String id;

	@NotBlank(message = "工作流名称不能为空")
	@Size(min = 1, max = 200, message = "工作流名称长度必须在1-200之间")
	@ApiModelProperty(value = "工作流名称", required = true, example = "订单处理流程")
	private String name;

	@Pattern(regexp = "^[a-zA-Z0-9_-]*$", message = "别名只能包含字母、数字、下划线和连字符")
	@Size(max = 100, message = "别名长度不能超过100")
	@ApiModelProperty(value = "工作流别名（用于快速搜索）", example = "order_process")
	private String alias;

	@Size(max = 1000, message = "描述长度不能超过1000")
	@ApiModelProperty(value = "工作流描述", example = "处理用户订单的全流程")
	private String description;

	@Size(max = 100, message = "分类长度不能超过100")
	@ApiModelProperty(value = "分类", example = "订单管理")
	private String category;

	@ApiModelProperty(value = "标签列表", example = "[\"订单\", \"处理\", \"自动化\"]")
	private List<String> tags;

	@ApiModelProperty(value = "工作流配置（JSON格式）")
	private Map<String, Object> config;

	@ApiModelProperty(value = "状态: draft/active/inactive/archived", example = "active")
	private String status = "draft"; // draft: 草稿, active: 活跃, inactive: 未激活, archived: 已归档

	@ApiModelProperty(value = "版本号", example = "1.0")
	private String version = "1.0"; // 版本号

	@ApiModelProperty(value = "模式: online/offline", example = "online")
	private String mode = "online"; // online: 在线, offline: 离线

	@ApiModelProperty(value = "节点数量", example = "5")
	private Integer nodeCount = 0; // 节点数量

	@ApiModelProperty(value = "执行次数", example = "100")
	private Integer executionCount = 0; // 执行次数

	@ApiModelProperty(value = "成功率（0-100）", example = "95.5")
	private Double successRate = 0.0; // 成功率

	@ApiModelProperty(value = "平均执行耗时（毫秒）", example = "1200")
	private Integer avgDuration = 0; // 平均执行耗时

	@ApiModelProperty(value = "租户ID")
	private String tenantId; // 租户ID

	@ApiModelProperty(value = "创建时间", hidden = true)
	private Long createdAt; // 创建时间

	@ApiModelProperty(value = "更新时间", hidden = true)
	private Long updatedAt;

	@ApiModelProperty(value = "创建人", hidden = true)
	private String createdBy;

	@ApiModelProperty(value = "更新人", hidden = true)
	private String updatedBy;

	private List<Object> nodes;

	private List<Object> connections;

	private List<Object> validationRules;

	private String templateId;

	private String templateVersion;

	private String branchId;
	private String branchName;
	private Integer basedOnVersion;
	private LocalDateTime branchCreatedTime;
	private String branchCreatedBy;

	private Long importedAt;
	private String importSource;
	private Map<String, Object> importData;

	/**
	 * 验证DTO数据的有效性
	 *
	 * @return 验证结果，null表示验证通过，否则返回错误信息
	 */
	public String validate() {
		if (name == null || name.trim().isEmpty()) {
			return "工作流名称不能为空";
		}

		if (alias != null && !alias.matches("^[a-zA-Z0-9_-]*$")) {
			return "别名只能包含字母、数字、下划线和连字符";
		}

		if (status != null && !List.of("draft", "active", "inactive", "archived").contains(status)) {
			return "状态值无效，必须是: draft, active, inactive, archived";
		}

		if (mode != null && !List.of("online", "offline").contains(mode)) {
			return "模式值无效，必须是: online, offline";
		}

		return null;
	}
}
