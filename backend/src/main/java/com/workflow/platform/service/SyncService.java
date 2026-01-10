package com.workflow.platform.service;
//同步服务

import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.util.OfflineSyncUtil;

import java.util.List;

/**
 * 同步服务接口
 */
public interface SyncService {

    /**
     * 执行完整同步
     */
    OfflineSyncUtil.SyncResult performFullSync();

    /**
     * 执行增量同步
     */
    OfflineSyncUtil.SyncResult performDeltaSync();

    /**
     * 添加同步任务
     */
    String addSyncTask(SyncTaskDTO taskDTO);

    /**
     * 批量添加同步任务
     */
    List<String> addSyncTasks(List<SyncTaskDTO> taskDTOs);

    /**
     * 获取同步状态
     */
    OfflineSyncUtil.SyncStatus getSyncStatus();

    /**
     * 获取任务状态
     */
    SyncTaskDTO getTaskStatus(String taskId);

    /**
     * 取消任务
     */
    boolean cancelTask(String taskId);

    /**
     * 重试任务
     */
    boolean retryTask(String taskId);

    /**
     * 获取冲突记录
     */
    List<OfflineSyncUtil.ConflictRecord> getConflictRecords(int limit);

    /**
     * 解决冲突
     */
    boolean resolveConflict(String conflictId, String strategy, Object customData);

    /**
     * 暂停同步
     */
    void pauseSync();

    /**
     * 恢复同步
     */
    void resumeSync();

    /**
     * 强制同步
     */
    void forceSync();

    /**
     * 清理历史记录
     */
    int cleanupHistory(int maxSize);
}