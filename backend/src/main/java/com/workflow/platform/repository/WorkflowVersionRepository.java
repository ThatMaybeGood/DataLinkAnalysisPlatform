package com.workflow.platform.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:51
 */

import com.workflow.platform.model.entity.WorkflowVersionEntity;

/**
 * 工作流版本数据访问层
 */
@Repository
public interface WorkflowVersionRepository extends JpaRepository<WorkflowVersionEntity, Long> {

	/**
	 * 根据工作流ID查找所有版本
	 */
	List<WorkflowVersionEntity> findByWorkflowIdOrderByVersionNumberDesc(Long workflowId);

	/**
	 * 根据工作流ID和版本号查找版本
	 */
	Optional<WorkflowVersionEntity> findByWorkflowIdAndVersionNumber(Long workflowId, Integer versionNumber);

	WorkflowVersionEntity findByWorkflowIdAndVersionNumber(String workflowId, Integer versionNumber);

	/**
	 * 查找工作流的当前版本
	 */
	Optional<WorkflowVersionEntity> findByWorkflowIdAndIsCurrentTrue(Long workflowId);

	/**
	 * 查找指定标签的版本
	 */
	List<WorkflowVersionEntity> findByWorkflowIdAndTagsContaining(Long workflowId, String tag);

	/**
	 * 根据创建人查找版本
	 */
	List<WorkflowVersionEntity> findByWorkflowIdAndCreatedBy(Long workflowId, String createdBy);

	/**
	 * 查找最新N个版本
	 */
	@Query("SELECT v FROM WorkflowVersionEntity v WHERE v.workflowId = :workflowId ORDER BY v.versionNumber DESC")
	List<WorkflowVersionEntity> findLatestVersions(@Param("workflowId") Long workflowId,
			org.springframework.data.domain.Pageable pageable);

	/**
	 * 更新所有版本为非当前版本
	 */
	@Modifying
	@Query("UPDATE WorkflowVersionEntity v SET v.isCurrent = false WHERE v.workflowId = :workflowId")
	void markAllVersionsAsNotCurrent(@Param("workflowId") Long workflowId);

	/**
	 * 设置指定版本为当前版本
	 */
	@Modifying
	@Query("UPDATE WorkflowVersionEntity v SET v.isCurrent = true WHERE v.id = :versionId")
	void markAsCurrentVersion(@Param("versionId") Long versionId);

	/**
	 * 获取工作流的最新版本号
	 */
	@Query("SELECT COALESCE(MAX(v.versionNumber), 0) FROM WorkflowVersionEntity v WHERE v.workflowId = :workflowId")
	Integer findMaxVersionNumber(@Param("workflowId") Long workflowId);

	/**
	 * 删除工作流的所有版本
	 */
	@Modifying
	@Query("DELETE FROM WorkflowVersionEntity v WHERE v.workflowId = :workflowId")
	void deleteByWorkflowId(@Param("workflowId") Long workflowId);

	/**
	 * 根据时间范围查找版本
	 */
	@Query("SELECT v FROM WorkflowVersionEntity v WHERE v.workflowId = :workflowId AND v.createTime BETWEEN :startTime AND :endTime ORDER BY v.createTime DESC")
	List<WorkflowVersionEntity> findByCreateTimeBetween(@Param("workflowId") Long workflowId,
			@Param("startTime") java.time.LocalDateTime startTime,
			@Param("endTime") java.time.LocalDateTime endTime);

	WorkflowVersionEntity findFirstByBranchIdOrderByVersionNumberDesc(String branchId);
}
