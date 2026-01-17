package com.workflow.platform.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
/**
 * 文件列表VO
 */
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileListVO {
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
