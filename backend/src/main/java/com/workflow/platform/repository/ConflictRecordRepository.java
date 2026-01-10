package com.workflow.platform.repository;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:25
 */

import com.workflow.platform.model.entity.ConflictRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 冲突记录数据访问层
 */
@Repository
public interface ConflictRecordRepository extends JpaRepository<ConflictRecordEntity, Long>,
        JpaSpecificationExecutor<ConflictRecordEntity> {

    /**
     * 根据状态查找冲突记录
     */
    List<ConflictRecordEntity> findByStatusOrderByDetectedTimeDesc(String status);

    /**
     * 根据状态分页查找
     */
    Page<ConflictRecordEntity> findByStatus(String status, Pageable pageable);

    /**
     * 根据对象类型和状态查找
     */
    List<ConflictRecordEntity> findByObjectTypeAndStatus(String objectType, String status);

    /**
     * 根据对象ID查找冲突记录
     */
    List<ConflictRecordEntity> findByObjectIdOrderByDetectedTimeDesc(String objectId);

    /**
     * 根据对象类型和对象ID查找
     */
    List<ConflictRecordEntity> findByObjectTypeAndObjectId(String objectType, String objectId);

    /**
     * 根据冲突类型查找
     */
    List<ConflictRecordEntity> findByConflictType(String conflictType);

    /**
     * 根据解决策略查找
     */
    List<ConflictRecordEntity> findByResolutionStrategy(String resolutionStrategy);

    /**
     * 查找指定时间范围内的冲突记录
     */
    List<ConflictRecordEntity> findByDetectedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找需要通知的冲突记录
     */
    List<ConflictRecordEntity> findByStatusAndNotifiedFalse(String status);

    /**
     * 查找待解决的冲突记录（按严重程度排序）
     */
    List<ConflictRecordEntity> findByStatusOrderBySeverityDescDetectedTimeDesc(String status);

    /**
     * 查找自动解决失败的冲突记录
     */
    @Query("SELECT c FROM ConflictRecordEntity c WHERE c.autoResolved = true AND c.status = 'PENDING' AND c.retryCount < :maxRetryCount")
    List<ConflictRecordEntity> findAutoResolveFailed(@Param("maxRetryCount") Integer maxRetryCount);

    /**
     * 统计各种状态的冲突数量
     */
    @Query("SELECT c.status, COUNT(c) FROM ConflictRecordEntity c GROUP BY c.status")
    List<Object[]> countByStatus();

    /**
     * 统计各种冲突类型的数量
     */
    @Query("SELECT c.conflictType, COUNT(c) FROM ConflictRecordEntity c GROUP BY c.conflictType")
    List<Object[]> countByConflictType();

    /**
     * 统计各种严重程度的数量
     */
    @Query("SELECT c.severity, COUNT(c) FROM ConflictRecordEntity c GROUP BY c.severity")
    List<Object[]> countBySeverity();

    /**
     * 按日期统计新增冲突
     */
    @Query("SELECT DATE(c.detectedTime), COUNT(c) FROM ConflictRecordEntity c WHERE c.detectedTime BETWEEN :startDate AND :endDate GROUP BY DATE(c.detectedTime) ORDER BY DATE(c.detectedTime)")
    List<Object[]> countByDate(@Param("startDate") LocalDateTime startDate,
                               @Param("endDate") LocalDateTime endDate);

    /**
     * 更新冲突记录状态
     */
    @Modifying
    @Query("UPDATE ConflictRecordEntity c SET c.status = :status WHERE c.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    /**
     * 批量更新状态
     */
    @Modifying
    @Query("UPDATE ConflictRecordEntity c SET c.status = :status WHERE c.id IN :ids")
    int batchUpdateStatus(@Param("ids") List<Long> ids, @Param("status") String status);

    /**
     * 标记为已通知
     */
    @Modifying
    @Query("UPDATE ConflictRecordEntity c SET c.notified = true WHERE c.id = :id")
    int markAsNotified(@Param("id") Long id);

    /**
     * 增加重试次数
     */
    @Modifying
    @Query("UPDATE ConflictRecordEntity c SET c.retryCount = c.retryCount + 1, c.lastRetryTime = :lastRetryTime WHERE c.id = :id")
    int incrementRetryCount(@Param("id") Long id, @Param("lastRetryTime") LocalDateTime lastRetryTime);

    /**
     * 根据冲突哈希查找重复冲突
     */
    List<ConflictRecordEntity> findByConflictHash(String conflictHash);

    /**
     * 查找超时未解决的冲突
     */
    @Query("SELECT c FROM ConflictRecordEntity c WHERE c.status = 'PENDING' AND c.detectedTime < :timeoutTime")
    List<ConflictRecordEntity> findTimeoutConflicts(@Param("timeoutTime") LocalDateTime timeoutTime);

    /**
     * 根据解决人统计
     */
    @Query("SELECT c.resolvedBy, COUNT(c) FROM ConflictRecordEntity c WHERE c.resolvedBy IS NOT NULL GROUP BY c.resolvedBy ORDER BY COUNT(c) DESC")
    List<Object[]> countByResolvedBy();

    /**
     * 删除旧的冲突记录
     */
    @Modifying
    @Query("DELETE FROM ConflictRecordEntity c WHERE c.detectedTime < :cutoffTime AND c.status = 'RESOLVED'")
    int deleteOldResolvedRecords(@Param("cutoffTime") LocalDateTime cutoffTime);
}