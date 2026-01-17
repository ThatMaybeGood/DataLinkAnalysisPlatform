package com.workflow.platform.model.vo;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.workflow.platform.enums.NodeType;

/**
 * 执行结果VO
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionResultVO {
	private boolean success;
	private String error;
	private Map<String, Object> output;
	private String nodeId;
	private NodeType nodeType;
	private Map<String, Object> properties;
}
