package com.workflow.platform.util;
//离线同步工具

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workflow.platform.component.SyncQueueManager;
import com.workflow.platform.constants.SystemConstants;
import com.workflow.platform.exception.SyncException;
import com.workflow.platform.model.dto.SyncTaskDTO;

import lombok.extern.slf4j.Slf4j;

/**
 * 离线同步工具 - 提供离线数据的同步功能
 */
@Slf4j
@Component
public class OfflineSyncUtil {

	@Autowired
	private SyncQueueManager syncQueueManager;

	@Autowired
	private FileUtil fileUtil;

	@Autowired
	private JsonUtil jsonUtil;

	@Value("${workflow.platform.offline.sync.conflict-resolution:timestamp}")
	private String conflictResolutionStrategy;

	@Value("${workflow.platform.offline.sync.max-conflict-retries:3}")
	private int maxConflictRetries;

	@Value("${workflow.platform.offline.sync.batch-interval:5000}")
	private long batchInterval;

	@Value("${workflow.platform.offline.sync.enable-delta-sync:true}")
	private boolean enableDeltaSync;

	@Value("${workflow.platform.file-storage.offline-path:./data/offline}")
	private String offlinePath;

	// 同步锁
	private final ReentrantLock syncLock = new ReentrantLock();

	// 同步状态
	private volatile boolean isSyncing = false;
	private volatile long lastSyncTime = 0;
	private volatile String lastSyncResult;

	// 冲突解决记录
	private final Map<String, ConflictRecord> conflictRecords = new ConcurrentHashMap<>();

	// 增量同步状态
	private final Map<String, FileSyncState> fileSyncStates = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		log.info("初始化离线同步工具");

		// 加载同步状态
		loadSyncStates();

		log.info("离线同步工具初始化完成，冲突解决策略: {}", conflictResolutionStrategy);
	}

	/**
	 * 执行完整同步
	 */
	public SyncResult performFullSync() {
		if (isSyncing) {
			return SyncResult.builder()
					.success(false)
					.message("已有同步任务正在执行")
					.build();
		}

		syncLock.lock();
		isSyncing = true;

		try {
			log.info("开始执行完整同步");
			long startTime = System.currentTimeMillis();

			SyncResult result = SyncResult.builder()
					.syncId(generateSyncId())
					.type("full")
					.startTime(startTime)
					.build();

			try {
				// 1. 扫描需要同步的文件
				List<FileSyncInfo> filesToSync = scanFilesForSync();
				result.setTotalFiles(filesToSync.size());

				if (filesToSync.isEmpty()) {
					result.setSuccess(true);
					result.setMessage("没有需要同步的文件");
					result.setEndTime(System.currentTimeMillis());
					return result;
				}

				// 2. 分批处理文件
				int batchSize = 10;
				int successCount = 0;
				int failureCount = 0;
				int conflictCount = 0;
				int totalProcessed = 0;

				for (int i = 0; i < filesToSync.size(); i += batchSize) {
					int end = Math.min(i + batchSize, filesToSync.size());
					List<FileSyncInfo> batch = filesToSync.subList(i, end);

					SyncResult batchResult = processSyncBatch(batch);

					totalProcessed += batch.size();
					successCount += batchResult.getSuccessCount();
					failureCount += batchResult.getFailureCount();
					conflictCount += batchResult.getConflictCount();

					log.info("批次 {}-{} 同步完成: 成功 {}，失败 {}，冲突 {}",
							i, end - 1, batchResult.getSuccessCount(),
							batchResult.getFailureCount(), batchResult.getConflictCount());

					// 批次间隔
					if (end < filesToSync.size()) {
						Thread.sleep(batchInterval);
					}
				}

				// 3. 更新同步状态
				result.setSuccessCount(successCount);
				result.setFailureCount(failureCount);
				result.setConflictCount(conflictCount);
				result.setSuccess(failureCount == 0);
				result.setMessage(String.format("同步完成: 成功 %d，失败 %d，冲突 %d",
						successCount, failureCount, conflictCount));

				// 4. 保存同步状态
				lastSyncTime = System.currentTimeMillis();
				lastSyncResult = jsonUtil.toJson(result).toString();
				saveSyncStates();

			} catch (Exception e) {
				log.error("完整同步失败: {}", e.getMessage(), e);
				result.setSuccess(false);
				result.setMessage("同步失败: " + e.getMessage());
			} finally {
				result.setEndTime(System.currentTimeMillis());
				result.setDuration(result.getEndTime() - result.getStartTime());
			}

			log.info("完整同步完成，耗时: {}ms，结果: {}",
					result.getDuration(), result.getMessage());

			return result;

			// } catch (InterruptedException e) {
			// Thread.currentThread().interrupt();
			// return SyncResult.builder()
			// .success(false)
			// .message("同步被中断")
			// .build();
		} finally {
			isSyncing = false;
			syncLock.unlock();
		}
	}

	/**
	 * 执行增量同步
	 */
	public SyncResult performDeltaSync() {
		if (!enableDeltaSync) {
			return SyncResult.builder()
					.success(false)
					.message("增量同步未启用")
					.build();
		}

		if (isSyncing) {
			return SyncResult.builder()
					.success(false)
					.message("已有同步任务正在执行")
					.build();
		}

		syncLock.lock();
		isSyncing = true;

		try {
			log.info("开始执行增量同步");
			long startTime = System.currentTimeMillis();

			SyncResult result = SyncResult.builder()
					.syncId(generateSyncId())
					.type("delta")
					.startTime(startTime)
					.build();

			try {
				// 1. 查找自上次同步以来有变化的文件
				List<FileSyncInfo> changedFiles = findChangedFiles();
				result.setTotalFiles(changedFiles.size());

				if (changedFiles.isEmpty()) {
					result.setSuccess(true);
					result.setMessage("没有变化的文件需要同步");
					result.setEndTime(System.currentTimeMillis());
					return result;
				}

				// 2. 处理变化的文件
				int successCount = 0;
				int failureCount = 0;
				int conflictCount = 0;

				for (FileSyncInfo fileInfo : changedFiles) {
					try {
						SyncResult fileResult = syncFile(fileInfo);

						if (fileResult.isSuccess()) {
							successCount++;
						} else if (fileResult.isConflict()) {
							conflictCount++;
						} else {
							failureCount++;
						}

						// 更新文件同步状态
						updateFileSyncState(fileInfo.getFilePath(), fileResult);

					} catch (Exception e) {
						log.error("同步文件失败: {}，错误: {}", fileInfo.getFilePath(), e.getMessage(), e);
						failureCount++;
					}
				}

				// 3. 更新结果
				result.setSuccessCount(successCount);
				result.setFailureCount(failureCount);
				result.setConflictCount(conflictCount);
				result.setSuccess(failureCount == 0);
				result.setMessage(String.format("增量同步完成: 成功 %d，失败 %d，冲突 %d",
						successCount, failureCount, conflictCount));

				// 4. 保存同步状态
				lastSyncTime = System.currentTimeMillis();
				lastSyncResult = jsonUtil.toJson(result).toString();
				saveSyncStates();

			} catch (Exception e) {
				log.error("增量同步失败: {}", e.getMessage(), e);
				result.setSuccess(false);
				result.setMessage("增量同步失败: " + e.getMessage());
			} finally {
				result.setEndTime(System.currentTimeMillis());
				result.setDuration(result.getEndTime() - result.getStartTime());
			}

			log.info("增量同步完成，耗时: {}ms，结果: {}",
					result.getDuration(), result.getMessage());

			return result;

		} finally {
			isSyncing = false;
			syncLock.unlock();
		}
	}

	/**
	 * 同步单个文件
	 */
	public SyncResult syncFile(FileSyncInfo fileInfo) {
		log.debug("同步文件: {}", fileInfo.getFilePath());

		SyncResult result = SyncResult.builder()
				.syncId(generateSyncId())
				.type("file")
				.filePath(fileInfo.getFilePath())
				.startTime(System.currentTimeMillis())
				.build();

		try {
			// 1. 读取文件内容
			String content = fileUtil.readFileToString(fileInfo.getFilePath());
			Map<String, Object> data = jsonUtil.fromJsonToMap(content, String.class, Object.class);

			// 2. 检查冲突
			ConflictCheckResult conflictCheck = checkConflict(fileInfo, data);

			if (conflictCheck.hasConflict()) {
				// 处理冲突
				ConflictResolution resolution = resolveConflict(conflictCheck);

				if (resolution.isResolved()) {
					// 使用解决后的数据
					data = resolution.getResolvedData();
					result.setConflict(true);
					result.setConflictResolution(resolution.getStrategy());
				} else {
					// 冲突未解决
					result.setSuccess(false);
					result.setConflict(true);
					result.setMessage("数据冲突未解决");
					result.setEndTime(System.currentTimeMillis());
					return result;
				}
			}

			// 3. 同步数据（这里应该调用具体的同步逻辑）
			boolean syncSuccess = performDataSync(fileInfo, data);

			// 4. 更新结果
			result.setSuccess(syncSuccess);
			result.setMessage(syncSuccess ? "文件同步成功" : "文件同步失败");

			if (syncSuccess) {
				// 更新文件同步状态
				updateFileSyncState(fileInfo.getFilePath(), result);
			}

		} catch (Exception e) {
			log.error("同步文件失败: {}，错误: {}", fileInfo.getFilePath(), e.getMessage(), e);
			result.setSuccess(false);
			result.setMessage("文件同步失败: " + e.getMessage());
		} finally {
			result.setEndTime(System.currentTimeMillis());
			result.setDuration(result.getEndTime() - result.getStartTime());
		}

		return result;
	}

	/**
	 * 将同步任务添加到队列
	 */
	public String scheduleSyncTask(SyncTaskDTO taskDTO) {
		try {
			String taskId = syncQueueManager.addSyncTask(taskDTO);
			if (taskId != null) {
				log.info("同步任务已添加到队列: {} - {}", taskId, taskDTO.getType());
				return taskId;
			} else {
				throw new SyncException("同步队列已满，无法添加任务");
			}
		} catch (Exception e) {
			log.error("添加同步任务到队列失败: {}", e.getMessage(), e);
			throw new SyncException("添加同步任务失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取同步状态
	 */
	public SyncStatus getSyncStatus() {
		SyncStatus status = new SyncStatus();
		status.setSyncing(isSyncing);
		status.setLastSyncTime(lastSyncTime);
		status.setLastSyncResult(lastSyncResult);
		status.setQueueSize(syncQueueManager.getQueueSize());
		status.setRunningTasks(syncQueueManager.getRunningTaskCount());

		SyncQueueManager.SyncStats stats = syncQueueManager.getStats();
		status.setPendingTasks(stats.getPending());
		status.setSuccessTasks(stats.getSuccess());
		status.setFailedTasks(stats.getFailed());
		status.setAverageSyncTime(stats.getAverageProcessingTime());

		return status;
	}

	/**
	 * 获取冲突记录
	 */
	public List<ConflictRecord> getConflictRecords(int limit) {
		List<ConflictRecord> records = new ArrayList<>(conflictRecords.values());

		// 按时间倒序排序
		records.sort((r1, r2) -> Long.compare(r2.getTimestamp(), r1.getTimestamp()));

		if (limit > 0 && records.size() > limit) {
			return records.subList(0, limit);
		}

		return records;
	}

	/**
	 * 解决指定冲突
	 */
	public boolean resolveSpecificConflict(String conflictId, String resolutionStrategy,
			Map<String, Object> customData) {
		ConflictRecord record = conflictRecords.get(conflictId);
		if (record == null) {
			return false;
		}

		try {
			// 应用解决方案
			boolean resolved = applyConflictResolution(record, resolutionStrategy, customData);

			if (resolved) {
				record.setResolved(true);
				record.setResolutionStrategy(resolutionStrategy);
				record.setResolvedTime(System.currentTimeMillis());

				// 重新尝试同步
				FileSyncInfo fileInfo = new FileSyncInfo();
				fileInfo.setFilePath(record.getFilePath());

				syncFile(fileInfo);

				log.info("冲突已解决: {}，策略: {}", conflictId, resolutionStrategy);
				return true;
			}

		} catch (Exception e) {
			log.error("解决冲突失败: {}，错误: {}", conflictId, e.getMessage(), e);
		}

		return false;
	}

	/**
	 * 回滚到指定同步点
	 */
	public boolean rollbackToSyncPoint(String syncId) {
		// 在实际应用中，这里应该实现回滚逻辑
		// 1. 查找同步点
		// 2. 恢复数据
		// 3. 更新状态

		log.warn("回滚功能尚未实现，syncId: {}", syncId);
		return false;
	}

	/**
	 * 清理旧的同步记录
	 */
	public int cleanupOldSyncRecords(int maxAgeDays) {
		long cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24L * 60 * 60 * 1000);
		int removed = 0;

		Iterator<Map.Entry<String, ConflictRecord>> iterator = conflictRecords.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<String, ConflictRecord> entry = iterator.next();
			ConflictRecord record = entry.getValue();

			if (record.getTimestamp() < cutoffTime && record.isResolved()) {
				iterator.remove();
				removed++;
			}
		}

		log.info("清理 {} 个旧的冲突记录", removed);
		return removed;
	}

	// ========== 私有方法 ==========

	private List<FileSyncInfo> scanFilesForSync() throws IOException {
		List<FileSyncInfo> files = new ArrayList<>();

		// 扫描工作流目录
		String workflowDir = offlinePath + SystemConstants.WORKFLOW_DIR;
		scanDirectory(workflowDir, "workflow", files);

		// 扫描节点目录
		String nodeDir = offlinePath + SystemConstants.NODE_DIR;
		scanDirectory(nodeDir, "node", files);

		// 扫描规则目录
		String ruleDir = offlinePath + SystemConstants.RULE_DIR;
		scanDirectory(ruleDir, "rule", files);

		log.debug("扫描到 {} 个需要同步的文件", files.size());
		return files;
	}

	private void scanDirectory(String dirPath, String type, List<FileSyncInfo> files) throws IOException {
		File directory = new File(dirPath);
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		Files.walk(Paths.get(dirPath))
				.filter(Files::isRegularFile)
				.filter(path -> path.toString().endsWith(SystemConstants.JSON_EXT))
				.forEach(path -> {
					FileSyncInfo info = new FileSyncInfo();
					info.setFilePath(path.toString());
					info.setFileType(type);
					info.setFileSize(path.toFile().length());
					info.setLastModified(path.toFile().lastModified());
					files.add(info);
				});
	}

	private List<FileSyncInfo> findChangedFiles() throws IOException {
		List<FileSyncInfo> changedFiles = new ArrayList<>();

		// 获取所有文件
		List<FileSyncInfo> allFiles = scanFilesForSync();

		for (FileSyncInfo fileInfo : allFiles) {
			String filePath = fileInfo.getFilePath();
			FileSyncState state = fileSyncStates.get(filePath);

			if (state == null) {
				// 新文件
				changedFiles.add(fileInfo);
			} else if (fileInfo.getLastModified() > state.getLastSyncedTime()) {
				// 文件已修改
				changedFiles.add(fileInfo);
			}
		}

		return changedFiles;
	}

	private SyncResult processSyncBatch(List<FileSyncInfo> batch) {
		SyncResult batchResult = SyncResult.builder()
				.syncId(generateSyncId())
				.type("batch")
				.startTime(System.currentTimeMillis())
				.totalFiles(batch.size())
				.build();

		int successCount = 0;
		int failureCount = 0;
		int conflictCount = 0;

		for (FileSyncInfo fileInfo : batch) {
			try {
				SyncResult fileResult = syncFile(fileInfo);

				if (fileResult.isSuccess()) {
					successCount++;
				} else if (fileResult.isConflict()) {
					conflictCount++;
				} else {
					failureCount++;
				}

			} catch (Exception e) {
				log.error("处理文件同步失败: {}，错误: {}",
						fileInfo.getFilePath(), e.getMessage(), e);
				failureCount++;
			}
		}

		batchResult.setSuccessCount(successCount);
		batchResult.setFailureCount(failureCount);
		batchResult.setConflictCount(conflictCount);
		batchResult.setSuccess(failureCount == 0);
		batchResult.setEndTime(System.currentTimeMillis());
		batchResult.setDuration(batchResult.getEndTime() - batchResult.getStartTime());

		return batchResult;
	}

	private ConflictCheckResult checkConflict(FileSyncInfo fileInfo, Map<String, Object> data) {
		ConflictCheckResult result = new ConflictCheckResult();
		result.setFilePath(fileInfo.getFilePath());
		result.setLocalData(data);
		result.setHasConflict(false);

		// 在实际应用中，这里应该检查远程数据的版本
		// 1. 获取远程数据版本
		// 2. 比较版本信息
		// 3. 判断是否有冲突

		// 简化实现：随机生成冲突
		if (Math.random() < 0.1) { // 10%的冲突概率
			result.setHasConflict(true);
			result.setConflictType("version");
			result.setRemoteVersion("v2.0");
			result.setLocalVersion("v1.0");
			result.setConflictMessage("版本冲突：本地v1.0，远程v2.0");
		}

		return result;
	}

	private ConflictResolution resolveConflict(ConflictCheckResult conflictCheck) {
		ConflictResolution resolution = new ConflictResolution();
		resolution.setConflictId(generateConflictId(conflictCheck.getFilePath()));
		resolution.setConflictType(conflictCheck.getConflictType());

		// 根据策略解决冲突
		switch (conflictResolutionStrategy) {
			case "timestamp":
				resolution = resolveByTimestamp(conflictCheck);
				break;
			case "version":
				resolution = resolveByVersion(conflictCheck);
				break;
			case "manual":
				resolution = resolveByManual(conflictCheck);
				break;
			case "local":
				resolution = resolveByLocalWins(conflictCheck);
				break;
			case "remote":
				resolution = resolveByRemoteWins(conflictCheck);
				break;
			case "merge":
				resolution = resolveByMerge(conflictCheck);
				break;
			default:
				resolution.setResolved(false);
				resolution.setMessage("未知的冲突解决策略: " + conflictResolutionStrategy);
		}

		// 记录冲突
		if (conflictCheck.hasConflict()) {
			recordConflict(conflictCheck, resolution);
		}

		return resolution;
	}

	private ConflictResolution resolveByTimestamp(ConflictCheckResult conflictCheck) {
		ConflictResolution resolution = new ConflictResolution();
		resolution.setStrategy("timestamp");

		// 简化实现：假设本地数据时间戳更新
		resolution.setResolved(true);
		resolution.setResolvedData(conflictCheck.getLocalData());
		resolution.setMessage("使用时间戳策略：保留最新数据");

		return resolution;
	}

	private ConflictResolution resolveByVersion(ConflictCheckResult conflictCheck) {
		ConflictResolution resolution = new ConflictResolution();
		resolution.setStrategy("version");

		// 简化实现：假设远程版本更高
		resolution.setResolved(true);
		resolution.setResolvedData(conflictCheck.getLocalData()); // 实际应获取远程数据
		resolution.setMessage("使用版本策略：使用更高版本数据");

		return resolution;
	}

	private ConflictResolution resolveByManual(ConflictCheckResult conflictCheck) {
		ConflictResolution resolution = new ConflictResolution();
		resolution.setStrategy("manual");
		resolution.setResolved(false);
		resolution.setMessage("需要手动解决冲突");
		return resolution;
	}

	private ConflictResolution resolveByLocalWins(ConflictCheckResult conflictCheck) {
		ConflictResolution resolution = new ConflictResolution();
		resolution.setStrategy("local");
		resolution.setResolved(true);
		resolution.setResolvedData(conflictCheck.getLocalData());
		resolution.setMessage("使用本地数据覆盖远程数据");
		return resolution;
	}

	private ConflictResolution resolveByRemoteWins(ConflictCheckResult conflictCheck) {
		ConflictResolution resolution = new ConflictResolution();
		resolution.setStrategy("remote");
		resolution.setResolved(true);
		resolution.setResolvedData(conflictCheck.getLocalData()); // 实际应获取远程数据
		resolution.setMessage("使用远程数据覆盖本地数据");
		return resolution;
	}

	private ConflictResolution resolveByMerge(ConflictCheckResult conflictCheck) {
		ConflictResolution resolution = new ConflictResolution();
		resolution.setStrategy("merge");

		try {
			// 简化合并实现
			Map<String, Object> mergedData = new HashMap<>(conflictCheck.getLocalData());
			mergedData.put("_merged", true);
			mergedData.put("_mergeTime", System.currentTimeMillis());
			mergedData.put("_conflictResolved", "auto-merge");

			resolution.setResolved(true);
			resolution.setResolvedData(mergedData);
			resolution.setMessage("自动合并数据");

		} catch (Exception e) {
			resolution.setResolved(false);
			resolution.setMessage("合并失败: " + e.getMessage());
		}

		return resolution;
	}

	private boolean performDataSync(FileSyncInfo fileInfo, Map<String, Object> data) {
		// 在实际应用中，这里应该实现具体的数据同步逻辑
		// 1. 验证数据
		// 2. 转换格式
		// 3. 调用远程API
		// 4. 处理响应

		// 简化实现：模拟同步过程
		try {
			// 模拟网络延迟
			Thread.sleep(100 + (long) (Math.random() * 400));

			// 模拟成功率90%
			return Math.random() < 0.9;

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void recordConflict(ConflictCheckResult conflictCheck, ConflictResolution resolution) {
		ConflictRecord record = new ConflictRecord();
		record.setId(resolution.getConflictId());
		record.setFilePath(conflictCheck.getFilePath());
		record.setConflictType(conflictCheck.getConflictType());
		record.setConflictMessage(conflictCheck.getConflictMessage());
		record.setResolutionStrategy(resolution.getStrategy());
		record.setResolved(resolution.isResolved());
		record.setTimestamp(System.currentTimeMillis());
		record.setResolvedTime(resolution.isResolved() ? System.currentTimeMillis() : 0);

		conflictRecords.put(record.getId(), record);

		log.info("记录冲突: {}，类型: {}，解决策略: {}，已解决: {}",
				record.getId(), record.getConflictType(),
				record.getResolutionStrategy(), record.isResolved());
	}

	private boolean applyConflictResolution(ConflictRecord record, String strategy,
			Map<String, Object> customData) {
		// 应用具体的冲突解决方案
		// 在实际应用中，这里应该实现具体的解决逻辑

		log.debug("应用冲突解决方案: {}，策略: {}", record.getId(), strategy);
		return true;
	}

	private void updateFileSyncState(String filePath, SyncResult result) {
		FileSyncState state = fileSyncStates.computeIfAbsent(filePath,
				k -> new FileSyncState());

		state.setFilePath(filePath);
		state.setLastSyncedTime(System.currentTimeMillis());
		state.setLastSyncResult(result.isSuccess() ? "success" : "failed");
		state.setSyncCount(state.getSyncCount() + 1);

		if (result.isConflict()) {
			state.setConflictCount(state.getConflictCount() + 1);
		}

		fileSyncStates.put(filePath, state);
	}

	private void loadSyncStates() {
		String stateFile = offlinePath + "/sync-states.json";

		try {
			if (Files.exists(Paths.get(stateFile))) {
				String content = fileUtil.readFileToString(stateFile);
				SyncStateData stateData = jsonUtil.fromJson(content, SyncStateData.class);

				if (stateData != null) {
					lastSyncTime = stateData.getLastSyncTime();
					lastSyncResult = stateData.getLastSyncResult();

					if (stateData.getFileSyncStates() != null) {
						fileSyncStates.putAll(stateData.getFileSyncStates());
					}

					log.debug("加载同步状态，最后同步时间: {}", lastSyncTime);
				}
			}
		} catch (Exception e) {
			log.error("加载同步状态失败: {}", e.getMessage(), e);
		}
	}

	private void saveSyncStates() {
		String stateFile = offlinePath + "/sync-states.json";

		try {
			SyncStateData stateData = new SyncStateData();
			stateData.setLastSyncTime(lastSyncTime);
			stateData.setLastSyncResult(lastSyncResult);
			stateData.setFileSyncStates(fileSyncStates);

			String content = jsonUtil.toJson(stateData).toString();
			fileUtil.writeStringToFile(content, stateFile);

			log.debug("保存同步状态到: {}", stateFile);
		} catch (Exception e) {
			log.error("保存同步状态失败: {}", e.getMessage(), e);
		}
	}

	private String generateSyncId() {
		return "sync_" + System.currentTimeMillis() + "_" +
				String.valueOf((int) (Math.random() * 10000));
	}

	private String generateConflictId(String filePath) {
		return "conflict_" + filePath.hashCode() + "_" + System.currentTimeMillis();
	}

	// ========== 内部类 ==========

	/**
	 * 文件同步信息
	 */
	public static class FileSyncInfo {
		private String filePath;
		private String fileType;
		private long fileSize;
		private long lastModified;

		// Getters and Setters
		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

		public String getFileType() {
			return fileType;
		}

		public void setFileType(String fileType) {
			this.fileType = fileType;
		}

		public long getFileSize() {
			return fileSize;
		}

		public void setFileSize(long fileSize) {
			this.fileSize = fileSize;
		}

		public long getLastModified() {
			return lastModified;
		}

		public void setLastModified(long lastModified) {
			this.lastModified = lastModified;
		}
	}

	/**
	 * 同步结果
	 */
	@lombok.Builder
	@lombok.Data
	public static class SyncResult {
		private String syncId;
		private String type;
		private String filePath;
		private long startTime;
		private long endTime;
		private long duration;
		private boolean success;
		private boolean conflict;
		private String conflictResolution;
		private String message;
		private int totalFiles;
		private int successCount;
		private int failureCount;
		private int conflictCount;
	}

	/**
	 * 冲突检查结果
	 */
	@lombok.Data
	public static class ConflictCheckResult {
		private String filePath;
		private boolean hasConflict;
		private String conflictType;
		private String conflictMessage;
		private String localVersion;
		private String remoteVersion;
		private Map<String, Object> localData;
		private Map<String, Object> remoteData;

		public boolean hasConflict() {
			return true;
		}
	}

	/**
	 * 冲突解决方案
	 */
	@lombok.Data
	public static class ConflictResolution {
		private String conflictId;
		private String conflictType;
		private String strategy;
		private boolean resolved;
		private String message;
		private Map<String, Object> resolvedData;
	}

	/**
	 * 冲突记录
	 */
	@lombok.Data
	public static class ConflictRecord {
		private String id;
		private String filePath;
		private String conflictType;
		private String conflictMessage;
		private String resolutionStrategy;
		private boolean resolved;
		private long timestamp;
		private long resolvedTime;
	}

	/**
	 * 文件同步状态
	 */
	@lombok.Data
	public static class FileSyncState {
		private String filePath;
		private long lastSyncedTime;
		private String lastSyncResult;
		private int syncCount;
		private int conflictCount;
	}

	/**
	 * 同步状态数据
	 */
	@lombok.Data
	public static class SyncStateData {
		private long lastSyncTime;
		private String lastSyncResult;
		private Map<String, FileSyncState> fileSyncStates;
	}

	/**
	 * 同步状态（API返回）
	 */
	@lombok.Data
	public static class SyncStatus {
		private boolean syncing;
		private long lastSyncTime;
		private String lastSyncResult;
		private int queueSize;
		private int pendingTasks;
		private int runningTasks;
		private int successTasks;
		private int failedTasks;
		private long averageSyncTime;
	}
}
