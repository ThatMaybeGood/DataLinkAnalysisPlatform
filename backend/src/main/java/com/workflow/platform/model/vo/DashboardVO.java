package com.workflow.platform.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
/**
 * 仪表板VO
 */
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardVO {
	private String id;
	private String name;
	private String description;
	private String createdAt;
	private String updatedAt;
	private String createdBy;
	private String updatedBy;
}
