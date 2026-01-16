package com.workflow.platform.repository;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:54
 */

import com.workflow.platform.model.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知数据访问层
 */
@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * 根据用户ID查找通知
     */
    List<NotificationEntity> findByUserIdOrderByCreateTimeDesc(String userId);

    /**
     * 根据用户ID和是否已读查找
     */
    List<NotificationEntity> findByUserIdAndReadOrderByCreateTimeDesc(String userId, boolean read);

    /**
     * 根据用户ID分页查找
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.userId = :userId AND n.deleted = false ORDER BY n.createTime DESC")
    List<NotificationEntity> findByUserIdOrderByCreateTimeDesc(@Param("userId") String userId,
                                                               org.springframework.data.domain.Pageable pageable);

    /**
     * 统计用户未读通知数量
     */
    int countByUserIdAndReadFalse(String userId);

    /**
     * 根据ID和用户ID查找通知
     */
    NotificationEntity findByIdAndUserId(String id, String userId);

    /**
     * 根据类型查找通知
     */
    List<NotificationEntity> findByType(String type);

    /**
     * 根据相关对象查找通知
     */
    List<NotificationEntity> findByRelatedObjectTypeAndRelatedObjectId(String objectType, String objectId);

    /**
     * 查找过期的通知
     */
    List<NotificationEntity> findByExpireTimeBefore(LocalDateTime expireTime);

    /**
     * 根据发送状态查找通知
     */
    List<NotificationEntity> findBySendStatus(String sendStatus);

    /**
     * 删除过期通知
     */
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.createTime < :cutoffTime AND n.deleted = false")
    int deleteByCreateTimeBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 标记通知为已删除
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.deleted = true, n.deleteTime = :deleteTime WHERE n.id = :id")
    int markAsDeleted(@Param("id") Long id, @Param("deleteTime") LocalDateTime deleteTime);

    /**
     * 批量标记为已读
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true, n.readTime = :readTime WHERE n.id IN :ids AND n.userId = :userId")
    int batchMarkAsRead(@Param("ids") List<Long> ids,
                        @Param("userId") String userId,
                        @Param("readTime") LocalDateTime readTime);

    /**
     * 查找用户最后一条通知
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.userId = :userId AND n.deleted = false ORDER BY n.createTime DESC")
    List<NotificationEntity> findLatestByUserId(@Param("userId") String userId,
                                                org.springframework.data.domain.Pageable pageable);

    List<NotificationEntity> findByUserIdOrderByCreateTimeDesc(String userId, int limit, int offset);
}