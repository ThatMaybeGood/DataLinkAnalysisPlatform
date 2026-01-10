package com.workflow.platform.service;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:26
 */

import com.workflow.platform.model.dto.ConflictRecordDTO;
//import com.workflow.platform.model.dto.ConflictResolutionDTO;
import com.workflow.platform.model.entity.ConflictRecordEntity;
import com.workflow.platform.model.vo.ConflictRecordVO;
//import com.workflow.platform.model.vo.ConflictStatisticsVO;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 冲突记录服务接口
 */
public interface ConflictRecordService {

    /**
     * 创建冲突记录
     */
    ConflictRecordVO createConflictRecord(ConflictRecordDTO conflictRecordDTO);

    /**
     * 批量创建冲突记录
     */
    List<ConflictRecordVO> batchCreateConflictRecords(List<ConflictRecordDTO> conflictRecordDTOs);

    /**
     * 获取冲突记录详情
     */
    ConflictRecordVO getConflictRecord(Long id);

    /**
     * 获取冲突记录列表
     */
    Page<ConflictRecordVO> getConflictRecords(Map<String, Object> queryParams, int page, int size);

    /**
     * 根据条件查询冲突记录
     */
    List<ConflictRecordVO> queryConflictRecords(Map<String, Object> criteria);

//    /**
//     * 解决冲突
//     */
//    ConflictRecordVO resolveConflict(ConflictResolutionDTO resolutionDTO);
//
//    /**
//     * 批量解决冲突
//     */
//    List<ConflictRecordVO> batchResolveConflicts(List<ConflictResolutionDTO> resolutionDTOs);

    /**
     * 忽略冲突
     */
    ConflictRecordVO ignoreConflict(Long id, String notes, String ignoredBy);

    /**
     * 重新打开冲突
     */
    ConflictRecordVO reopenConflict(Long id, String notes);

    /**
     * 自动解决冲突
     */
    ConflictRecordVO autoResolveConflict(Long id);

    /**
     * 批量自动解决冲突
     */
    List<ConflictRecordVO> batchAutoResolveConflicts(List<Long> ids);

    /**
     * 获取待解决的冲突列表
     */
    List<ConflictRecordVO> getPendingConflicts();

    /**
     * 获取高优先级的冲突列表
     */
    List<ConflictRecordVO> getHighPriorityConflicts();

//    /**
//     * 统计冲突信息
//     */
//    ConflictStatisticsVO getConflictStatistics(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取实时冲突统计
     */
    Map<String, Object> getRealtimeConflictStats();

    /**
     * 分析冲突趋势
     */
    Map<String, Object> analyzeConflictTrend(int days);

    /**
     * 生成冲突报告
     */
    String generateConflictReport(LocalDateTime startTime, LocalDateTime endTime, String format);

    /**
     * 清理旧的冲突记录
     */
    int cleanupOldConflictRecords(int daysToKeep);

    /**
     * 重新检测冲突
     */
    ConflictRecordVO retryConflictDetection(Long id);

    /**
     * 标记冲突为已通知
     */
    void markAsNotified(Long id);

    /**
     * 获取冲突解决建议
     */
    Map<String, Object> getConflictResolutionSuggestions(Long id);

    /**
     * 验证冲突记录
     */
    boolean validateConflictRecord(ConflictRecordDTO conflictRecordDTO);

    /**
     * 计算冲突哈希
     */
    String calculateConflictHash(ConflictRecordDTO conflictRecordDTO);

    /**
     * 检查重复冲突
     */
    boolean checkDuplicateConflict(ConflictRecordDTO conflictRecordDTO);

    void save(ConflictRecordEntity conflictRecord);
}