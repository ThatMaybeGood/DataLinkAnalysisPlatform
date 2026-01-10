package com.workflow.platform.repository;

import com.workflow.platform.model.entity.WorkflowEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 工作流数据访问接口（在线模式）
 * 继承JpaRepository获得基础CRUD操作
 * 继承JpaSpecificationExecutor支持复杂查询
 */
@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String>,
        JpaSpecificationExecutor<WorkflowEntity> {

    /**
     * 根据别名查找工作流
     * @param alias 工作流别名
     * @return Optional包装的工作流实体
     */
    Optional<WorkflowEntity> findByAlias(String alias);

    /**
     * 根据状态查找工作流列表
     * @param status 状态
     * @return 工作流列表
     */
    List<WorkflowEntity> findByStatus(String status);

    /**
     * 根据分类查找工作流列表
     * @param category 分类
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowEntity> findByCategory(String category, Pageable pageable);

    /**
     * 根据名称模糊查询
     * @param name 名称（支持模糊匹配）
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowEntity> findByNameContaining(String name, Pageable pageable);

    /**
     * 根据租户ID查找工作流
     * @param tenantId 租户ID
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowEntity> findByTenantId(String tenantId, Pageable pageable);

    /**
     * 根据创建时间范围查找工作流
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowEntity> findByCreatedAtBetween(LocalDateTime startTime,
                                                LocalDateTime endTime,
                                                Pageable pageable);

    /**
     * 根据标签查找工作流（JSON字段查询）
     * @param tag 标签
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    @Query(value = "SELECT * FROM workflows WHERE JSON_CONTAINS(tags, :tag)",
            nativeQuery = true)
    Page<WorkflowEntity> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 统计不同状态的工作流数量
     * @return 状态统计结果
     */
    @Query("SELECT w.status, COUNT(w) FROM WorkflowEntity w GROUP BY w.status")
    List<Object[]> countByStatus();

    /**
     * 统计不同分类的工作流数量
     * @return 分类统计结果
     */
    @Query("SELECT w.category, COUNT(w) FROM WorkflowEntity w WHERE w.category IS NOT NULL GROUP BY w.category")
    List<Object[]> countByCategory();

    /**
     * 查找最近更新的工作流
     * @param limit 数量限制
     * @return 工作流列表
     */
    @Query("SELECT w FROM WorkflowEntity w ORDER BY w.updatedAt DESC")
    List<WorkflowEntity> findRecentWorkflows(@Param("limit") int limit);

    /**
     * 根据模式查找工作流
     * @param mode 模式：online/offline
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowEntity> findByMode(String mode, Pageable pageable);

    /**
     * 检查别名是否存在
     * @param alias 别名
     * @return 是否存在
     */
    boolean existsByAlias(String alias);

    /**
     * 批量更新工作流状态
     * @param ids 工作流ID列表
     * @param status 新状态
     * @return 更新数量
     */
    @Query("UPDATE WorkflowEntity w SET w.status = :status WHERE w.id IN :ids")
    int updateStatusByIds(@Param("ids") List<String> ids, @Param("status") String status);
}