package com.workflow.platform.component;
//同步队列管理器

import com.workflow.platform.enums.SyncStatus;
import com.workflow.platform.model.dto.SyncTaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 同步队列管理器 - 管理离线数据的同步队列和调度
 */
@Slf4j
@Component
public class SyncQueueManager {

    // 同步队列
    private final PriorityBlockingQueue<SyncTask> syncQueue = new PriorityBlockingQueue<>(
            1000, Comparator.comparingLong(SyncTask::getPriority)
    );

    // 正在执行的任务
    private final ConcurrentHashMap<String, SyncTask> runningTasks = new ConcurrentHashMap<>();

    // 已完成任务历史
    private final LinkedBlockingDeque<SyncTask> taskHistory = new LinkedBlockingDeque<>(1000);

    // 线程池
    private ExecutorService syncExecutor;
    private ScheduledExecutorService scheduler;

    // 状态监控
    private final SyncStats stats = new SyncStats();
    private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();

    // 配置参数
    @Value("${workflow.platform.offline.sync.thread-pool-size:5}")
    private int threadPoolSize;

    @Value("${workflow.platform.offline.sync.max-retries:3}")
    private int maxRetries;

    @Value("${workflow.platform.offline.sync.retry-delay:5000}")
    private long retryDelay;

    @Value("${workflow.platform.offline.sync.batch-size:50}")
    private int batchSize;

    @Value("${workflow.platform.offline.sync.queue-capacity:1000}")
    private int queueCapacity;

    @Value("${workflow.platform.offline.sync.enable-auto-sync:true}")
    private boolean enableAutoSync;

    @PostConstruct
    public void init() {
        log.info("初始化同步队列管理器");

        // 初始化线程池
        syncExecutor = Executors.newFixedThreadPool(threadPoolSize, new SyncThreadFactory());
        scheduler = Executors.newScheduledThreadPool(2);

        // 启动同步处理器
        startSyncProcessors();

        // 启动监控任务
        startMonitoringTask();

        // 启动重试任务
        startRetryTask();

        log.info("同步队列管理器初始化完成，线程池大小: {}", threadPoolSize);
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭同步队列管理器");

        if (syncExecutor != null) {
            syncExecutor.shutdown();
            try {
                if (!syncExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    syncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                syncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
        }

        log.info("同步队列管理器已关闭");
    }

    /**
     * 添加同步任务
     */
    public String addSyncTask(SyncTaskDTO taskDTO) {
        SyncTask task = convertToSyncTask(taskDTO);

        if (syncQueue.size() >= queueCapacity) {
            log.warn("同步队列已满，丢弃任务: {}", task.getId());
            return null;
        }

        syncQueue.offer(task);
        stats.incrementPending();

        log.debug("添加同步任务: {} - {} (优先级: {})",
                task.getId(), task.getType(), task.getPriority());

        return task.getId();
    }

    /**
     * 批量添加同步任务
     */
    public List<String> addSyncTasks(List<SyncTaskDTO> taskDTOs) {
        List<String> taskIds = new ArrayList<>();

        for (SyncTaskDTO taskDTO : taskDTOs) {
            String taskId = addSyncTask(taskDTO);
            if (taskId != null) {
                taskIds.add(taskId);
            }
        }

        log.info("批量添加 {} 个同步任务，成功 {} 个",
                taskDTOs.size(), taskIds.size());

        return taskIds;
    }

    /**
     * 获取任务状态
     */
    public SyncTaskDTO getTaskStatus(String taskId) {
        // 检查运行中的任务
        SyncTask runningTask = runningTasks.get(taskId);
        if (runningTask != null) {
            return convertToSyncTaskDTO(runningTask);
        }

        // 检查历史任务
        for (SyncTask task : taskHistory) {
            if (task.getId().equals(taskId)) {
                return convertToSyncTaskDTO(task);
            }
        }

        // 检查队列中的任务
        for (SyncTask task : syncQueue) {
            if (task.getId().equals(taskId)) {
                return convertToSyncTaskDTO(task);
            }
        }

        return null;
    }

    /**
     * 取消任务
     */
    public boolean cancelTask(String taskId) {
        // 检查运行中的任务
        SyncTask runningTask = runningTasks.get(taskId);
        if (runningTask != null) {
            runningTask.setStatus(SyncStatus.CANCELLED);
            runningTask.setErrorMessage("用户取消");
            runningTask.setEndTime(System.currentTimeMillis());

            completeTask(runningTask);
            return true;
        }

        // 从队列中移除
        Iterator<SyncTask> iterator = syncQueue.iterator();
        while (iterator.hasNext()) {
            SyncTask task = iterator.next();
            if (task.getId().equals(taskId)) {
                iterator.remove();
                task.setStatus(SyncStatus.CANCELLED);
                task.setErrorMessage("用户取消");
                task.setEndTime(System.currentTimeMillis());

                taskHistory.offerFirst(task);
                stats.decrementPending();
                return true;
            }
        }

        return false;
    }

    /**
     * 重新执行失败的任务
     */
    public boolean retryTask(String taskId) {
        // 查找历史中的失败任务
        for (SyncTask task : taskHistory) {
            if (task.getId().equals(taskId) &&
                    (task.getStatus() == SyncStatus.FAILED ||
                            task.getStatus() == SyncStatus.TIMEOUT)) {

                task.setRetryCount(task.getRetryCount() + 1);
                task.setStatus(SyncStatus.PENDING);
                task.setErrorMessage(null);
                task.setStartTime(0);
                task.setEndTime(0);
                task.setPriority(System.currentTimeMillis() - 10000); // 提高优先级

                syncQueue.offer(task);
                taskHistory.remove(task);
                stats.incrementPending();

                log.info("重试任务: {}，重试次数: {}", taskId, task.getRetryCount());
                return true;
            }
        }

        return false;
    }

    /**
     * 获取同步统计信息
     */
    public SyncStats getStats() {
        statsLock.readLock().lock();
        try {
            SyncStats copy = new SyncStats();
            copy.setPending(stats.getPending());
            copy.setRunning(stats.getRunning());
            copy.setSuccess(stats.getSuccess());
            copy.setFailed(stats.getFailed());
            copy.setCancelled(stats.getCancelled());
            copy.setTimeout(stats.getTimeout());
            copy.setTotalProcessed(stats.getTotalProcessed());
            copy.setAverageProcessingTime(stats.getAverageProcessingTime());
            copy.setQueueSize(syncQueue.size());
            copy.setHistorySize(taskHistory.size());
            return copy;
        } finally {
            statsLock.readLock().unlock();
        }
    }

    /**
     * 清理历史任务
     */
    public int cleanupHistory(int maxHistorySize) {
        int removed = 0;

        while (taskHistory.size() > maxHistorySize) {
            taskHistory.pollLast();
            removed++;
        }

        log.info("清理历史任务，移除 {} 个", removed);
        return removed;
    }

    /**
     * 获取队列大小
     */
    public int getQueueSize() {
        return syncQueue.size();
    }

    /**
     * 获取运行中任务数
     */
    public int getRunningTaskCount() {
        return runningTasks.size();
    }

    /**
     * 获取历史任务数
     */
    public int getHistorySize() {
        return taskHistory.size();
    }

    /**
     * 暂停同步
     */
    public void pauseSync() {
        enableAutoSync = false;
        log.info("同步已暂停");
    }

    /**
     * 恢复同步
     */
    public void resumeSync() {
        enableAutoSync = true;
        log.info("同步已恢复");
    }

    /**
     * 强制立即同步
     */
    public void forceSync() {
        if (!enableAutoSync) {
            log.warn("同步被暂停，无法强制同步");
            return;
        }

        // 处理一批任务
        processBatch();

        log.info("强制同步执行完成");
    }

    // ========== 私有方法 ==========

    private void startSyncProcessors() {
        for (int i = 0; i < threadPoolSize; i++) {
            syncExecutor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        if (enableAutoSync && !syncQueue.isEmpty()) {
                            processNextTask();
                        } else {
                            Thread.sleep(1000);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        log.error("同步处理器异常: {}", e.getMessage(), e);
                    }
                }
                log.debug("同步处理器线程结束");
            });
        }

        log.info("启动 {} 个同步处理器线程", threadPoolSize);
    }

    private void startMonitoringTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                monitorSyncStatus();
            } catch (Exception e) {
                log.error("监控任务异常: {}", e.getMessage(), e);
            }
        }, 30, 30, TimeUnit.SECONDS);

        log.info("启动同步监控任务，间隔30秒");
    }

    private void startRetryTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                retryFailedTasks();
            } catch (Exception e) {
                log.error("重试任务异常: {}", e.getMessage(), e);
            }
        }, 60, 60, TimeUnit.SECONDS);

        log.info("启动失败任务重试任务，间隔60秒");
    }

    private void processNextTask() throws InterruptedException {
        SyncTask task = syncQueue.poll(1, TimeUnit.SECONDS);
        if (task == null) {
            return;
        }

        // 更新状态
        task.setStatus(SyncStatus.RUNNING);
        task.setStartTime(System.currentTimeMillis());
        runningTasks.put(task.getId(), task);

        stats.decrementPending();
        stats.incrementRunning();

        log.debug("开始处理任务: {} - {}", task.getId(), task.getType());

        // 在实际应用中，这里应该调用具体的同步处理器
        try {
            // 模拟处理时间
            Thread.sleep(task.getEstimatedTime());

            // 处理成功
            task.setStatus(SyncStatus.SUCCESS);
            task.setResult("同步成功");

        } catch (Exception e) {
            // 处理失败
            if (task.getRetryCount() < maxRetries) {
                // 重试
                task.setStatus(SyncStatus.PENDING);
                task.setRetryCount(task.getRetryCount() + 1);
                task.setPriority(System.currentTimeMillis() + retryDelay * task.getRetryCount());
                task.setErrorMessage(e.getMessage());

                syncQueue.offer(task);
                stats.incrementPending();
                log.warn("任务处理失败，将重试: {}，错误: {}", task.getId(), e.getMessage());
            } else {
                // 重试次数用尽
                task.setStatus(SyncStatus.FAILED);
                task.setErrorMessage("重试次数用尽: " + e.getMessage());
                stats.incrementFailed();
            }
        } finally {
            if (task.getStatus() != SyncStatus.PENDING) {
                completeTask(task);
            }
            runningTasks.remove(task.getId());
            stats.decrementRunning();
        }
    }

    private void processBatch() {
        int processed = 0;
        List<SyncTask> batch = new ArrayList<>();

        // 获取一批任务
        syncQueue.drainTo(batch, batchSize);

        for (SyncTask task : batch) {
            try {
                task.setStatus(SyncStatus.RUNNING);
                task.setStartTime(System.currentTimeMillis());
                runningTasks.put(task.getId(), task);

                stats.decrementPending();
                stats.incrementRunning();

                // 批量处理逻辑
                processTaskInBatch(task);

                task.setStatus(SyncStatus.SUCCESS);
                task.setEndTime(System.currentTimeMillis());
                task.setResult("批量处理成功");

                stats.incrementSuccess();

            } catch (Exception e) {
                task.setStatus(SyncStatus.FAILED);
                task.setErrorMessage("批量处理失败: " + e.getMessage());
                task.setEndTime(System.currentTimeMillis());

                stats.incrementFailed();

                log.error("批量处理任务失败: {}，错误: {}", task.getId(), e.getMessage());
            } finally {
                completeTask(task);
                runningTasks.remove(task.getId());
                stats.decrementRunning();
                processed++;
            }
        }

        log.info("批量处理完成，处理 {} 个任务", processed);
    }

    private void processTaskInBatch(SyncTask task) {
        // 实际批量处理逻辑
        // 这里简化为睡眠
        try {
            Thread.sleep(task.getEstimatedTime());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("处理被中断", e);
        }
    }

    private void completeTask(SyncTask task) {
        task.setEndTime(System.currentTimeMillis());

        // 计算处理时间
        long processingTime = task.getEndTime() - task.getStartTime();
        task.setProcessingTime(processingTime);

        // 更新统计
        statsLock.writeLock().lock();
        try {
            stats.addProcessingTime(processingTime);
            stats.incrementTotalProcessed();
        } finally {
            statsLock.writeLock().unlock();
        }

        // 添加到历史
        taskHistory.offerFirst(task);

        // 限制历史大小
        if (taskHistory.size() > 1000) {
            taskHistory.pollLast();
        }

        log.debug("任务完成: {} - {}，状态: {}，耗时: {}ms",
                task.getId(), task.getType(), task.getStatus(), processingTime);
    }

    private void monitorSyncStatus() {
        statsLock.writeLock().lock();
        try {
            // 检查超时任务
            long currentTime = System.currentTimeMillis();
            int timeoutCount = 0;

            for (SyncTask task : runningTasks.values()) {
                if (task.getStartTime() > 0 &&
                        currentTime - task.getStartTime() > task.getTimeout()) {

                    task.setStatus(SyncStatus.TIMEOUT);
                    task.setErrorMessage("任务执行超时");
                    task.setEndTime(currentTime);

                    completeTask(task);
                    runningTasks.remove(task.getId());

                    stats.decrementRunning();
                    stats.incrementTimeout();
                    timeoutCount++;
                }
            }

            if (timeoutCount > 0) {
                log.warn("发现 {} 个超时任务", timeoutCount);
            }

            // 记录监控日志
            if (stats.getTotalProcessed() % 100 == 0) {
                log.info("同步监控 - 队列: {}，运行中: {}，历史: {}，成功: {}，失败: {}，平均耗时: {}ms",
                        syncQueue.size(), runningTasks.size(), taskHistory.size(),
                        stats.getSuccess(), stats.getFailed(), stats.getAverageProcessingTime());
            }

        } finally {
            statsLock.writeLock().unlock();
        }
    }

    private void retryFailedTasks() {
        int retryCount = 0;
        long cutoffTime = System.currentTimeMillis() - retryDelay;

        for (SyncTask task : taskHistory) {
            if ((task.getStatus() == SyncStatus.FAILED ||
                    task.getStatus() == SyncStatus.TIMEOUT) &&
                    task.getEndTime() < cutoffTime &&
                    task.getRetryCount() < maxRetries) {

                task.setRetryCount(task.getRetryCount() + 1);
                task.setStatus(SyncStatus.PENDING);
                task.setErrorMessage(null);
                task.setStartTime(0);
                task.setEndTime(0);
                task.setPriority(System.currentTimeMillis());

                syncQueue.offer(task);
                taskHistory.remove(task);

                stats.incrementPending();
                retryCount++;

                if (retryCount >= 10) { // 每次最多重试10个
                    break;
                }
            }
        }

        if (retryCount > 0) {
            log.info("自动重试 {} 个失败任务", retryCount);
        }
    }

    private SyncTask convertToSyncTask(SyncTaskDTO dto) {
        SyncTask task = new SyncTask();
        task.setId(generateTaskId(dto.getType()));
        task.setType(dto.getType());
        task.setData(dto.getData());
        task.setPriority(dto.getPriority() != 0 ? dto.getPriority() : System.currentTimeMillis());
        task.setStatus(SyncStatus.PENDING);
        task.setRetryCount(0);
        task.setEstimatedTime(dto.getEstimatedTime() > 0 ? dto.getEstimatedTime() : 1000);
        task.setTimeout(dto.getTimeout() > 0 ? dto.getTimeout() : 30000);
        task.setCreatedTime(System.currentTimeMillis());
        task.setMetadata(dto.getMetadata());
        return task;
    }

    private SyncTaskDTO convertToSyncTaskDTO(SyncTask task) {
        SyncTaskDTO dto = new SyncTaskDTO();
        dto.setId(task.getId());
        dto.setType(task.getType());
        dto.setData(task.getData());
        dto.setPriority(task.getPriority());
        dto.setStatus(task.getStatus());
        dto.setRetryCount(task.getRetryCount());
        dto.setEstimatedTime(task.getEstimatedTime());
        dto.setTimeout(task.getTimeout());
        dto.setErrorMessage(task.getErrorMessage());
        dto.setResult(task.getResult());
        dto.setProcessingTime(task.getProcessingTime());
        dto.setCreatedTime(task.getCreatedTime());
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        dto.setMetadata(task.getMetadata());
        return dto;
    }

    private String generateTaskId(String type) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int) (Math.random() * 10000));
        return type + "_" + timestamp + "_" + random;
    }

    // ========== 内部类 ==========

    /**
     * 同步任务
     */
    public static class SyncTask {
        private String id;
        private String type;
        private Object data;
        private long priority;
        private SyncStatus status;
        private int retryCount;
        private long estimatedTime;
        private long timeout;
        private String errorMessage;
        private String result;
        private long processingTime;
        private long createdTime;
        private long startTime;
        private long endTime;
        private Map<String, Object> metadata;

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Object getData() { return data; }
        public void setData(Object data) { this.data = data; }

        public long getPriority() { return priority; }
        public void setPriority(long priority) { this.priority = priority; }

        public SyncStatus getStatus() { return status; }
        public void setStatus(SyncStatus status) { this.status = status; }

        public int getRetryCount() { return retryCount; }
        public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

        public long getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(long estimatedTime) { this.estimatedTime = estimatedTime; }

        public long getTimeout() { return timeout; }
        public void setTimeout(long timeout) { this.timeout = timeout; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getResult() { return result; }
        public void setResult(String result) { this.result = result; }

        public long getProcessingTime() { return processingTime; }
        public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }

        public long getCreatedTime() { return createdTime; }
        public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 同步统计
     */
    public static class SyncStats {
        private final AtomicInteger pending = new AtomicInteger(0);
        private final AtomicInteger running = new AtomicInteger(0);
        private final AtomicInteger success = new AtomicInteger(0);
        private final AtomicInteger failed = new AtomicInteger(0);
        private final AtomicInteger cancelled = new AtomicInteger(0);
        private final AtomicInteger timeout = new AtomicInteger(0);
        private final AtomicInteger totalProcessed = new AtomicInteger(0);
        private final AtomicLong totalProcessingTime = new AtomicLong();
        private int queueSize;
        private int historySize;

        // Getters and Setters
        public int getPending() { return pending.get(); }
        public int getRunning() { return running.get(); }
        public int getSuccess() { return success.get(); }
        public int getFailed() { return failed.get(); }
        public int getCancelled() { return cancelled.get(); }
        public int getTimeout() { return timeout.get(); }
        public int getTotalProcessed() { return totalProcessed.get(); }

        public long getAverageProcessingTime() {
            int processed = totalProcessed.get();
            long totalTime = totalProcessingTime.get();
            return processed > 0 ? totalTime / processed : 0;
        }

        public int getQueueSize() { return queueSize; }
        public int getHistorySize() { return historySize; }

        public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
        public void setHistorySize(int historySize) { this.historySize = historySize; }

        // 原子操作方法
        public void incrementPending() { pending.incrementAndGet(); }
        public void decrementPending() { pending.decrementAndGet(); }
        public void incrementRunning() { running.incrementAndGet(); }
        public void decrementRunning() { running.decrementAndGet(); }
        public void incrementSuccess() { success.incrementAndGet(); }
        public void incrementFailed() { failed.incrementAndGet(); }
        public void incrementCancelled() { cancelled.incrementAndGet(); }
        public void incrementTimeout() { timeout.incrementAndGet(); }
        public void incrementTotalProcessed() { totalProcessed.incrementAndGet(); }

        public void addProcessingTime(long time) {
            totalProcessingTime.addAndGet(time);
        }

        public void setPending(int value) { pending.set(value); }
        public void setRunning(int value) { running.set(value); }
        public void setSuccess(int value) { success.set(value); }
        public void setFailed(int value) { failed.set(value); }
        public void setCancelled(int value) { cancelled.set(value); }
        public void setTimeout(int value) { timeout.set(value); }
        public void setTotalProcessed(int value) { totalProcessed.set(value); }
        public void setAverageProcessingTime(long value) { /* 只读属性 */ }

        @Override
        public String toString() {
            return String.format(
                    "SyncStats{pending=%d, running=%d, success=%d, failed=%d, " +
                            "cancelled=%d, timeout=%d, totalProcessed=%d, avgTime=%dms, " +
                            "queueSize=%d, historySize=%d}",
                    pending.get(), running.get(), success.get(), failed.get(),
                    cancelled.get(), timeout.get(), totalProcessed.get(),
                    getAverageProcessingTime(), queueSize, historySize
            );
        }
    }

    /**
     * 同步线程工厂
     */
    private static class SyncThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix = "sync-worker-";

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            thread.setDaemon(true);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    /**
     * 原子长整型（Java 8兼容）
     */
    private static class AtomicLong {
        private long value = 0;

        public synchronized long get() {
            return value;
        }

        public synchronized void set(long newValue) {
            value = newValue;
        }

        public synchronized long addAndGet(long delta) {
            value += delta;
            return value;
        }
    }
}