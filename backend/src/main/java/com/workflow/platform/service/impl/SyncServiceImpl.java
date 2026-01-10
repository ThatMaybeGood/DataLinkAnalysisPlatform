package com.workflow.platform.service.impl;

import com.workflow.platform.component.SyncQueueManager;
import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.service.SyncService;
import com.workflow.platform.util.OfflineSyncUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SyncServiceImpl implements SyncService {

    @Autowired
    private OfflineSyncUtil offlineSyncUtil;

    @Autowired
    private SyncQueueManager syncQueueManager;

    @Override
    public OfflineSyncUtil.SyncResult performFullSync() {
        return offlineSyncUtil.performFullSync();
    }

    @Override
    public OfflineSyncUtil.SyncResult performDeltaSync() {
        return offlineSyncUtil.performDeltaSync();
    }

    @Override
    public String addSyncTask(SyncTaskDTO taskDTO) {
        return offlineSyncUtil.scheduleSyncTask(taskDTO);
    }

    @Override
    public List<String> addSyncTasks(List<SyncTaskDTO> taskDTOs) {
        return syncQueueManager.addSyncTasks(taskDTOs);
    }

    @Override
    public OfflineSyncUtil.SyncStatus getSyncStatus() {
        return offlineSyncUtil.getSyncStatus();
    }

    @Override
    public SyncTaskDTO getTaskStatus(String taskId) {
        return syncQueueManager.getTaskStatus(taskId);
    }

    @Override
    public boolean cancelTask(String taskId) {
        return syncQueueManager.cancelTask(taskId);
    }

    @Override
    public boolean retryTask(String taskId) {
        return syncQueueManager.retryTask(taskId);
    }

    @Override
    public List<OfflineSyncUtil.ConflictRecord> getConflictRecords(int limit) {
        return offlineSyncUtil.getConflictRecords(limit);
    }

    @Override
    public boolean resolveConflict(String conflictId, String strategy, Object customData) {
        return offlineSyncUtil.resolveSpecificConflict(
                conflictId, strategy, (java.util.Map<String, Object>) customData);
    }

    @Override
    public void pauseSync() {
        syncQueueManager.pauseSync();
    }

    @Override
    public void resumeSync() {
        syncQueueManager.resumeSync();
    }

    @Override
    public void forceSync() {
        syncQueueManager.forceSync();
    }

    @Override
    public int cleanupHistory(int maxSize) {
        return syncQueueManager.cleanupHistory(maxSize);
    }
}