package com.workflow.platform.model.dto;

import lombok.Data;

@Data
/**
 * 业务分类DTO
 */
public class CategoryDTO {
	private String id;
	private String name;
	private String description;
	private String createdAt;
	private String updatedAt;
	private String createdBy;
	private String updatedBy;
	private String status;
	private String type;
	private String parentId;
	private String parentName;
	private String parentType;
	private String parentStatus;
}
