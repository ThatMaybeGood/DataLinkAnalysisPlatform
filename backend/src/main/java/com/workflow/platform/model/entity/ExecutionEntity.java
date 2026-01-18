package com.workflow.platform.model.entity;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Data;

/**
 * @author Mine
 * @version 1.0
 *          描述:
 * @date 2026/1/16 00:40
 */
@Data
public class ExecutionEntity {
	private Long id;
	private String workflowId;
	private String status;
	private Object inputData;
	private Map<String, Object> outputData;
	private String errorMessage;

	private LocalDateTime createTime;

	private LocalDateTime startTime;

	private LocalDateTime endTime;
}
