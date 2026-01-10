package com.workflow.platform.controller;
//数据同步控制器

import com.workflow.platform.component.SyncQueueManager;
import com.workflow.platform.model.dto.SyncTaskDTO;
import com.workflow.platform.service.SyncService;
import com.workflow.platform.util.OfflineSyncUtil;
import com.workflow.platform.util.OfflineSyncUtil.SyncResult;
import com.workflow.platform.util.OfflineSyncUtil.SyncStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@Tag(name = "同步管理", description = "离线数据同步管理接口")
public class SyncController {

    @Autowired
    private SyncService syncService;

    @Autowired
    private OfflineSyncUtil offlineSyncUtil;

    @Autowired
    private SyncQueueManager syncQueueManager;

    @PostMapping("/full")
    @Operation(summary = "执行完整同步")
    public ResponseEntity<SyncResult> performFullSync() {
        SyncResult result = offlineSyncUtil.performFullSync();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/delta")
    @Operation(summary = "执行增量同步")
    public ResponseEntity<SyncResult> performDeltaSync() {
        SyncResult result = offlineSyncUtil.performDeltaSync();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/task")
    @Operation(summary = "添加同步任务")
    public ResponseEntity<Map<String, Object>> addSyncTask(@RequestBody SyncTaskDTO taskDTO) {
        String taskId = offlineSyncUtil.scheduleSyncTask(taskDTO);

        if (taskId != null) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "taskId", taskId,
                    "message", "同步任务已添加到队列"
            ));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "添加同步任务失败"
            ));
        }
    }

    @GetMapping("/status")
    @Operation(summary = "获取同步状态")
    public ResponseEntity<SyncStatus> getSyncStatus() {
        SyncStatus status = offlineSyncUtil.getSyncStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/queue")
    @Operation(summary = "获取同步队列状态")
    public ResponseEntity<Map<String, Object>> getQueueStatus() {
        SyncQueueManager.SyncStats stats = syncQueueManager.getStats();

        return ResponseEntity.ok(Map.of(
                "queueSize", stats.getQueueSize(),
                "pending", stats.getPending(),
                "running", stats.getRunning(),
                "success", stats.getSuccess(),
                "failed", stats.getFailed(),
                "averageTime", stats.getAverageProcessingTime(),
                "historySize", stats.getHistorySize()
        ));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "获取任务状态")
    public ResponseEntity<SyncTaskDTO> getTaskStatus(@PathVariable String taskId) {
        SyncTaskDTO task = syncQueueManager.getTaskStatus(taskId);

        if (task != null) {
            return ResponseEntity.ok(task);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/task/{taskId}/cancel")
    @Operation(summary = "取消任务")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        boolean cancelled = syncQueueManager.cancelTask(taskId);

        return ResponseEntity.ok(Map.of(
                "success", cancelled,
                "message", cancelled ? "任务已取消" : "取消任务失败"
        ));
    }

    @PostMapping("/task/{taskId}/retry")
    @Operation(summary = "重试任务")
    public ResponseEntity<Map<String, Object>> retryTask(@PathVariable String taskId) {
        boolean retried = syncQueueManager.retryTask(taskId);

        return ResponseEntity.ok(Map.of(
                "success", retried,
                "message", retried ? "任务已加入重试队列" : "重试任务失败"
        ));
    }

    @GetMapping("/conflicts")
    @Operation(summary = "获取冲突记录")
    public ResponseEntity<List<OfflineSyncUtil.ConflictRecord>> getConflictRecords(
            @RequestParam(defaultValue = "10") int limit) {
        List<OfflineSyncUtil.ConflictRecord> records =
                offlineSyncUtil.getConflictRecords(limit);
        return ResponseEntity.ok(records);
    }

    @PostMapping("/conflict/{conflictId}/resolve")
    @Operation(summary = "解决冲突")
    public ResponseEntity<Map<String, Object>> resolveConflict(
            @PathVariable String conflictId,
            @RequestParam String strategy,
            @RequestBody(required = false) Map<String, Object> customData) {

        boolean resolved = offlineSyncUtil.resolveSpecificConflict(
                conflictId, strategy, customData);

        return ResponseEntity.ok(Map.of(
                "success", resolved,
                "message", resolved ? "冲突已解决" : "解决冲突失败"
        ));
    }

    @PostMapping("/pause")
    @Operation(summary = "暂停同步")
    public ResponseEntity<Map<String, Object>> pauseSync() {
        syncQueueManager.pauseSync();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "同步已暂停"
        ));
    }

    @PostMapping("/resume")
    @Operation(summary = "恢复同步")
    public ResponseEntity<Map<String, Object>> resumeSync() {
        syncQueueManager.resumeSync();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "同步已恢复"
        ));
    }

    @PostMapping("/force")
    @Operation(summary = "强制立即同步")
    public ResponseEntity<Map<String, Object>> forceSync() {
        syncQueueManager.forceSync();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "强制同步已执行"
        ));
    }

    @DeleteMapping("/history")
    @Operation(summary = "清理历史记录")
    public ResponseEntity<Map<String, Object>> cleanupHistory(
            @RequestParam(defaultValue = "100") int maxSize) {
        int removed = syncQueueManager.cleanupHistory(maxSize);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "removed", removed,
                "message", "清理完成"
        ));
    }
}
