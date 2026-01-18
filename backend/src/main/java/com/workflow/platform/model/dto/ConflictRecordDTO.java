package com.workflow.platform.model.dto;

// import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

import javax.validation.constraints.NotBlank;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:22
 */

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 冲突记录数据传输对象
 */
@Data
@ApiModel(description = "冲突记录数据传输对象")
public class ConflictRecordDTO {

	@ApiModelProperty(value = "冲突类型", required = true, example = "WORKFLOW_CONFLICT")
	@NotBlank(message = "冲突类型不能为空")
	private String conflictType;

	@ApiModelProperty(value = "对象类型", required = true, example = "WORKFLOW")
	@NotBlank(message = "对象类型不能为空")
	private String objectType;

	@ApiModelProperty(value = "对象ID", required = true, example = "1")
	@NotBlank(message = "对象ID不能为空")
	private String objectId;

	@ApiModelProperty(value = "对象名称", example = "订单处理流程")
	private String objectName;

	@ApiModelProperty(value = "本地数据")
	private Map<String, Object> localData;

	@ApiModelProperty(value = "远程数据")
	private Map<String, Object> remoteData;

	@ApiModelProperty(value = "本地数据版本号", example = "1")
	private Integer localVersion;

	@ApiModelProperty(value = "远程数据版本号", example = "2")
	private Integer remoteVersion;

	@ApiModelProperty(value = "本地更新时间")
	private LocalDateTime localUpdateTime;

	@ApiModelProperty(value = "远程更新时间")
	private LocalDateTime remoteUpdateTime;

	@ApiModelProperty(value = "解决策略", example = "MANUAL")
	private String resolutionStrategy;

	@ApiModelProperty(value = "解决备注")
	private String resolutionNotes;

	@ApiModelProperty(value = "同步任务ID", example = "sync_001")
	private String syncTaskId;

	@ApiModelProperty(value = "设备/客户端标识", example = "web_client_001")
	private String clientId;

	@ApiModelProperty(value = "IP地址", example = "192.168.1.100")
	private String ipAddress;

	@ApiModelProperty(value = "元数据")
	private Map<String, Object> metadata;

	@ApiModelProperty(value = "冲突详细描述", example = "工作流配置被不同用户同时修改")
	private String description;

	@ApiModelProperty(value = "冲突严重程度", example = "MEDIUM")
	private String severity;

	@ApiModelProperty(value = "是否自动解决", example = "false")
	private Boolean autoResolved;
}
