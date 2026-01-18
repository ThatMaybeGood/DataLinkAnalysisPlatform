package com.workflow.platform.model.entity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;

import lombok.Data;

/**
 * 基础实体类
 * 所有实体类的基类，包含公共字段
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

	/**
	 * 创建时间
	 */
	@Column(name = "created_at", updatable = false)
	private Long createdAt;

	/**
	 * 更新时间
	 */
	@Column(name = "updated_at")
	private Long updatedAt;

	/**
	 * 创建人
	 */
	@Column(name = "created_by", updatable = false)
	private String createdBy;

	/**
	 * 更新人
	 */
	@Column(name = "updated_by")
	private String updatedBy;

	/**
	 * 实体保存前的回调方法
	 */
	@PrePersist
	protected void onCreate() {
		if (createdAt == null) {
			createdAt = System.currentTimeMillis();
		}
		updatedAt = System.currentTimeMillis();
	}

	/**
	 * 实体更新前的回调方法
	 */
	@PreUpdate
	protected void onUpdate() {
		updatedAt = System.currentTimeMillis();
	}
}