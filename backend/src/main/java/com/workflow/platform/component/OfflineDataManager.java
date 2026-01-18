package com.workflow.platform.component;
//离线数据管理器

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workflow.platform.constants.SystemConstants;
import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 离线数据管理器 - 管理离线模式下的文件存储和数据同步
 */
@Slf4j
@Component
public class OfflineDataManager {

	@Value("${workflow.platform.file-storage.base-path:./data}")
	private String basePath;

	@Value("${workflow.platform.file-storage.offline-path:./data/offline}")
	private String offlinePath;

	@Value("${workflow.platform.offline.compression.enabled:true}")
	private boolean compressionEnabled;

	@Value("${workflow.platform.offline.encryption.enabled:false}")
	private boolean encryptionEnabled;

	@Value("${workflow.platform.offline.sync.batch-size:50}")
	private int syncBatchSize;

	// 数据索引
	private final Map<String, FileIndex> fileIndex = new ConcurrentHashMap<>();

	// 内存缓存
	private final Map<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

	// 读写锁
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	// 同步队列
	private final Queue<SyncTask> syncQueue = new LinkedList<>();

	// 同步状态
	private volatile boolean isSyncing = false;

	@PostConstruct
	public void init() {
		try {
			log.info("初始化离线数据管理器，存储路径: {}", offlinePath);

			// 创建必要的目录
			createDirectories();

			// 加载文件索引
			loadFileIndex();

			// 启动同步线程
			startSyncThread();

			log.info("离线数据管理器初始化完成，已索引文件数: {}", fileIndex.size());

		} catch (Exception e) {
			log.error("离线数据管理器初始化失败: {}", e.getMessage(), e);
			throw new RuntimeException("离线数据管理器初始化失败", e);
		}
	}

	/**
	 * 保存工作流到离线存储
	 */
	public boolean saveWorkflow(WorkflowDTO workflow) {
		lock.writeLock().lock();
		try {
			String workflowId = workflow.getId();
			String filePath = getWorkflowFilePath(workflowId);

			// 创建备份
			createBackup(filePath);

			// 转换为JSON
			String json = JsonUtil.toJson(workflow);

			// 压缩（如果需要）
			byte[] data = json.getBytes(SystemConstants.DEFAULT_CHARSET);
			if (compressionEnabled) {
				data = compressData(data);
			}

			// 加密（如果需要）
			if (encryptionEnabled) {
				data = encryptData(data);
			}

			// 写入文件
			Path path = Paths.get(filePath);
			Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

			// 更新索引
			updateFileIndex(workflowId, filePath, "workflow");

			// 更新缓存
			CacheEntry entry = new CacheEntry(workflow, System.currentTimeMillis());
			memoryCache.put(workflowId, entry);

			log.info("工作流保存到离线存储: {}", workflowId);
			return true;

		} catch (Exception e) {
			log.error("保存工作流到离线存储失败: {}, error: {}", workflow.getId(), e.getMessage(), e);
			return false;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 从离线存储加载工作流
	 */
	public WorkflowDTO loadWorkflow(String workflowId) {
		// 检查缓存
		CacheEntry cached = memoryCache.get(workflowId);
		if (cached != null && !cached.isExpired()) {
			return (WorkflowDTO) cached.getData();
		}

		lock.readLock().lock();
		try {
			String filePath = getWorkflowFilePath(workflowId);
			File file = new File(filePath);

			if (!file.exists()) {
				log.warn("工作流文件不存在: {}", workflowId);
				return null;
			}

			// 读取文件
			byte[] data = Files.readAllBytes(Paths.get(filePath));

			// 解密（如果需要）
			if (encryptionEnabled) {
				data = decryptData(data);
			}

			// 解压（如果需要）
			if (compressionEnabled) {
				data = decompressData(data);
			}

			// 解析JSON
			String json = new String(data, SystemConstants.DEFAULT_CHARSET);
			WorkflowDTO workflow = JsonUtil.fromJson(json, WorkflowDTO.class);

			// 更新缓存
			CacheEntry entry = new CacheEntry(workflow, System.currentTimeMillis());
			memoryCache.put(workflowId, entry);

			log.debug("从离线存储加载工作流: {}", workflowId);
			return workflow;

		} catch (Exception e) {
			log.error("从离线存储加载工作流失败: {}, error: {}", workflowId, e.getMessage(), e);
			return null;
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 删除工作流
	 */
	public boolean deleteWorkflow(String workflowId) {
		lock.writeLock().lock();
		try {
			String filePath = getWorkflowFilePath(workflowId);
			File file = new File(filePath);

			if (file.exists()) {
				// 创建备份
				createBackup(filePath);

				// 删除文件
				boolean deleted = file.delete();

				if (deleted) {
					// 清理索引
					fileIndex.remove(workflowId);

					// 清理缓存
					memoryCache.remove(workflowId);

					log.info("删除工作流文件: {}", workflowId);
					return true;
				}
			}

			return false;

		} catch (Exception e) {
			log.error("删除工作流失败: {}, error: {}", workflowId, e.getMessage(), e);
			return false;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 获取所有工作流ID
	 */
	public List<String> getAllWorkflowIds() {
		lock.readLock().lock();
		try {
			return fileIndex.values().stream()
					.filter(index -> "workflow".equals(index.getType()))
					.map(FileIndex::getId)
					.collect(Collectors.toList());
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 搜索工作流
	 */
	public List<WorkflowDTO> searchWorkflows(String keyword, int page, int size) {
		lock.readLock().lock();
		try {
			List<WorkflowDTO> results = new ArrayList<>();

			for (String workflowId : getAllWorkflowIds()) {
				WorkflowDTO workflow = loadWorkflow(workflowId);
				if (workflow != null && matchesKeyword(workflow, keyword)) {
					results.add(workflow);
				}

				// 分页控制
				if (results.size() >= page * size + size) {
					break;
				}
			}

			// 分页
			int start = page * size;
			int end = Math.min(start + size, results.size());

			if (start < results.size()) {
				return results.subList(start, end);
			} else {
				return Collections.emptyList();
			}

		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 备份离线数据
	 */
	public boolean backupData(String backupName) {
		lock.writeLock().lock();
		try {
			String backupDir = getBackupPath(backupName);
			File backupDirFile = new File(backupDir);

			if (!backupDirFile.exists()) {
				backupDirFile.mkdirs();
			}

			// 备份工作流目录
			Path sourceDir = Paths.get(getWorkflowDir());
			Path targetDir = Paths.get(backupDir, "workflows");

			if (Files.exists(sourceDir)) {
				Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException {
						Path targetFile = targetDir.resolve(sourceDir.relativize(file));
						Files.createDirectories(targetFile.getParent());
						Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
			}

			// 备份索引文件
			String indexPath = getIndexFilePath();
			if (Files.exists(Paths.get(indexPath))) {
				Files.copy(Paths.get(indexPath),
						Paths.get(backupDir, "file-index.json"),
						StandardCopyOption.REPLACE_EXISTING);
			}

			log.info("离线数据备份完成: {}", backupName);
			return true;

		} catch (Exception e) {
			log.error("离线数据备份失败: {}, error: {}", backupName, e.getMessage(), e);
			return false;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 恢复离线数据
	 */
	public boolean restoreData(String backupName) {
		lock.writeLock().lock();
		try {
			String backupDir = getBackupPath(backupName);
			File backupDirFile = new File(backupDir);

			if (!backupDirFile.exists()) {
				log.error("备份不存在: {}", backupName);
				return false;
			}

			// 停止当前服务（这里只是标记，实际需要外部控制）
			isSyncing = false;

			// 清空当前数据
			clearAllData();

			// 恢复工作流目录
			Path sourceDir = Paths.get(backupDir, "workflows");
			Path targetDir = Paths.get(getWorkflowDir());

			if (Files.exists(sourceDir)) {
				Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
							throws IOException {
						Path targetFile = targetDir.resolve(sourceDir.relativize(file));
						Files.createDirectories(targetFile.getParent());
						Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
						return FileVisitResult.CONTINUE;
					}
				});
			}

			// 恢复索引文件
			Path indexSource = Paths.get(backupDir, "file-index.json");
			if (Files.exists(indexSource)) {
				Files.copy(indexSource,
						Paths.get(getIndexFilePath()),
						StandardCopyOption.REPLACE_EXISTING);
			}

			// 重新加载索引
			loadFileIndex();

			// 清理缓存
			memoryCache.clear();

			log.info("离线数据恢复完成: {}", backupName);
			return true;

		} catch (Exception e) {
			log.error("离线数据恢复失败: {}, error: {}", backupName, e.getMessage(), e);
			return false;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 获取存储统计信息
	 */
	public StorageStats getStorageStats() {
		lock.readLock().lock();
		try {
			StorageStats stats = new StorageStats();

			// 文件统计
			stats.setTotalFiles(fileIndex.size());

			// 大小统计
			long totalSize = 0;
			for (FileIndex index : fileIndex.values()) {
				File file = new File(index.getPath());
				if (file.exists()) {
					totalSize += file.length();
				}
			}
			stats.setTotalSize(totalSize);

			// 缓存统计
			stats.setCachedItems(memoryCache.size());

			// 同步队列统计
			stats.setPendingSyncs(syncQueue.size());

			// 存储空间统计
			File storageDir = new File(offlinePath);
			if (storageDir.exists()) {
				stats.setFreeSpace(storageDir.getFreeSpace());
				stats.setTotalSpace(storageDir.getTotalSpace());
				stats.setUsableSpace(storageDir.getUsableSpace());
			}

			return stats;

		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 压缩数据
	 */
	public byte[] compressData(byte[] data) throws IOException {
		try (java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
				GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(data);
			gzip.finish();
			return bos.toByteArray();
		}
	}

	/**
	 * 解压数据
	 */
	public byte[] decompressData(byte[] compressedData) throws IOException {
		try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(compressedData);
				GZIPInputStream gzip = new GZIPInputStream(bis);
				java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream()) {

			byte[] buffer = new byte[1024];
			int len;
			while ((len = gzip.read(buffer)) > 0) {
				bos.write(buffer, 0, len);
			}
			return bos.toByteArray();
		}
	}

	/**
	 * 加密数据（简化版，实际应使用更安全的加密方式）
	 */
	public byte[] encryptData(byte[] data) {
		// 简化加密，实际项目中应使用AES等加密算法
		byte[] key = SystemConstants.ENCRYPTION_KEY.getBytes();
		byte[] result = new byte[data.length];

		for (int i = 0; i < data.length; i++) {
			result[i] = (byte) (data[i] ^ key[i % key.length]);
		}

		return result;
	}

	/**
	 * 解密数据
	 */
	public byte[] decryptData(byte[] encryptedData) {
		// 加密和解密使用相同的异或操作
		return encryptData(encryptedData);
	}

	// ========== 私有方法 ==========

	private void createDirectories() throws IOException {
		String[] dirs = {
				offlinePath,
				getWorkflowDir(),
				getNodeDir(),
				getRuleDir(),
				getExportDir(),
				getBackupDir()
		};

		for (String dir : dirs) {
			File directory = new File(dir);
			if (!directory.exists()) {
				boolean created = directory.mkdirs();
				if (!created) {
					throw new IOException("创建目录失败: " + dir);
				}
				log.debug("创建目录: {}", dir);
			}
		}
	}

	private void loadFileIndex() throws IOException {
		String indexPath = getIndexFilePath();
		File indexFile = new File(indexPath);

		if (!indexFile.exists()) {
			log.info("索引文件不存在，创建新的索引");
			return;
		}

		try {
			String json = new String(Files.readAllBytes(Paths.get(indexPath)),
					SystemConstants.DEFAULT_CHARSET);

			FileIndex[] indices = JsonUtil.fromJson(json, FileIndex[].class);
			if (indices != null) {
				for (FileIndex index : indices) {
					fileIndex.put(index.getId(), index);
				}
			}

			log.info("加载文件索引完成，共 {} 个文件", fileIndex.size());

		} catch (Exception e) {
			log.error("加载文件索引失败: {}", e.getMessage(), e);
			// 索引损坏，重建索引
			rebuildFileIndex();
		}
	}

	private void rebuildFileIndex() throws IOException {
		log.info("开始重建文件索引");
		fileIndex.clear();

		// 扫描工作流目录
		scanDirectory(getWorkflowDir(), "workflow");

		// 保存索引
		saveFileIndex();

		log.info("重建文件索引完成，共 {} 个文件", fileIndex.size());
	}

	private void scanDirectory(String directoryPath, String type) throws IOException {
		File directory = new File(directoryPath);
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
					throws IOException {
				String fileName = file.getFileName().toString();

				// 提取ID（假设文件名就是ID）
				String id = fileName.replaceAll("\\.[^.]+$", "");

				FileIndex index = new FileIndex();
				index.setId(id);
				index.setPath(file.toString());
				index.setType(type);
				index.setSize(attrs.size());
				index.setLastModified(attrs.lastModifiedTime().toMillis());

				fileIndex.put(id, index);

				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void saveFileIndex() throws IOException {
		String indexPath = getIndexFilePath();

		List<FileIndex> indices = new ArrayList<>(fileIndex.values());
		String json = JsonUtil.toJson(indices);

		Files.write(Paths.get(indexPath),
				json.getBytes(SystemConstants.DEFAULT_CHARSET),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	private void updateFileIndex(String id, String filePath, String type) {
		FileIndex index = new FileIndex();
		index.setId(id);
		index.setPath(filePath);
		index.setType(type);
		index.setSize(new File(filePath).length());
		index.setLastModified(System.currentTimeMillis());

		fileIndex.put(id, index);

		// 定期保存索引
		try {
			saveFileIndex();
		} catch (IOException e) {
			log.error("保存文件索引失败: {}", e.getMessage(), e);
		}
	}

	private void createBackup(String filePath) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			return;
		}

		String backupPath = filePath + ".bak";
		Files.copy(Paths.get(filePath),
				Paths.get(backupPath),
				StandardCopyOption.REPLACE_EXISTING);
	}

	private boolean matchesKeyword(WorkflowDTO workflow, String keyword) {
		if (keyword == null || keyword.trim().isEmpty()) {
			return true;
		}

		String lowerKeyword = keyword.toLowerCase();
		return (workflow.getName() != null && workflow.getName().toLowerCase().contains(lowerKeyword)) ||
				(workflow.getDescription() != null && workflow.getDescription().toLowerCase().contains(lowerKeyword)) ||
				(workflow.getCategory() != null && workflow.getCategory().toLowerCase().contains(lowerKeyword));
	}

	private void startSyncThread() {
		Thread syncThread = new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(30000); // 30秒检查一次

					if (!syncQueue.isEmpty() && !isSyncing) {
						processSyncQueue();
					}

				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					log.error("同步线程异常: {}", e.getMessage(), e);
				}
			}
		}, "offline-sync-thread");

		syncThread.setDaemon(true);
		syncThread.start();

		log.info("离线数据同步线程已启动");
	}

	private void processSyncQueue() {
		lock.writeLock().lock();
		isSyncing = true;

		try {
			log.info("开始处理同步队列，待处理任务数: {}", syncQueue.size());

			int processed = 0;
			while (!syncQueue.isEmpty() && processed < syncBatchSize) {
				SyncTask task = syncQueue.poll();
				if (task != null) {
					processSyncTask(task);
					processed++;
				}
			}

			log.info("同步队列处理完成，已处理 {} 个任务", processed);

		} finally {
			isSyncing = false;
			lock.writeLock().unlock();
		}
	}

	private void processSyncTask(SyncTask task) {
		// 这里应该实现具体的同步逻辑
		// 例如：将离线数据同步到在线数据库
		log.debug("处理同步任务: {} - {}", task.getType(), task.getId());
	}

	private void clearAllData() throws IOException {
		// 清空目录
		String[] dirs = {
				getWorkflowDir(),
				getNodeDir(),
				getRuleDir(),
				getExportDir()
		};

		for (String dir : dirs) {
			File directory = new File(dir);
			if (directory.exists()) {
				org.apache.commons.io.FileUtils.cleanDirectory(directory);
			}
		}

		// 清空索引和缓存
		fileIndex.clear();
		memoryCache.clear();
	}

	// ========== 路径获取方法 ==========

	private String getWorkflowDir() {
		return offlinePath + SystemConstants.WORKFLOW_DIR;
	}

	private String getNodeDir() {
		return offlinePath + SystemConstants.NODE_DIR;
	}

	private String getRuleDir() {
		return offlinePath + SystemConstants.RULE_DIR;
	}

	private String getExportDir() {
		return offlinePath + SystemConstants.EXPORT_DIR;
	}

	private String getBackupDir() {
		return offlinePath + SystemConstants.BACKUP_DIR;
	}

	String getWorkflowFilePath(String workflowId) {
		return getWorkflowDir() + "/" + workflowId + SystemConstants.JSON_EXT;
	}

	private String getIndexFilePath() {
		return offlinePath + "/file-index.json";
	}

	private String getBackupPath(String backupName) {
		String timestamp = LocalDateTime.now()
				.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		return getBackupDir() + "/" + backupName + "_" + timestamp;
	}

	// ========== 内部类 ==========

	/**
	 * 文件索引
	 */
	public static class FileIndex {
		private String id;
		private String path;
		private String type;
		private long size;
		private long lastModified;

		// Getters and Setters
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public long getSize() {
			return size;
		}

		public void setSize(long size) {
			this.size = size;
		}

		public long getLastModified() {
			return lastModified;
		}

		public void setLastModified(long lastModified) {
			this.lastModified = lastModified;
		}
	}

	/**
	 * 缓存条目
	 */
	private static class CacheEntry {
		private final Object data;
		private final long timestamp;
		private static final long CACHE_TTL = 5 * 60 * 1000; // 5分钟

		public CacheEntry(Object data, long timestamp) {
			this.data = data;
			this.timestamp = timestamp;
		}

		public Object getData() {
			return data;
		}

		public boolean isExpired() {
			return System.currentTimeMillis() - timestamp > CACHE_TTL;
		}
	}

	/**
	 * 同步任务
	 */
	private static class SyncTask {
		private String id;
		private String type;

		// Getters and Setters
		public String getId() {
			return id;
		}

		public String getType() {
			return type;
		}

	}

	/**
	 * 存储统计信息
	 */
	public static class StorageStats {
		private int totalFiles;
		private long totalSize;
		private int cachedItems;
		private int pendingSyncs;
		private long freeSpace;
		private long totalSpace;
		private long usableSpace;

		// Getters and Setters
		public int getTotalFiles() {
			return totalFiles;
		}

		public void setTotalFiles(int totalFiles) {
			this.totalFiles = totalFiles;
		}

		public long getTotalSize() {
			return totalSize;
		}

		public void setTotalSize(long totalSize) {
			this.totalSize = totalSize;
		}

		public int getCachedItems() {
			return cachedItems;
		}

		public void setCachedItems(int cachedItems) {
			this.cachedItems = cachedItems;
		}

		public int getPendingSyncs() {
			return pendingSyncs;
		}

		public void setPendingSyncs(int pendingSyncs) {
			this.pendingSyncs = pendingSyncs;
		}

		public long getFreeSpace() {
			return freeSpace;
		}

		public void setFreeSpace(long freeSpace) {
			this.freeSpace = freeSpace;
		}

		public long getTotalSpace() {
			return totalSpace;
		}

		public void setTotalSpace(long totalSpace) {
			this.totalSpace = totalSpace;
		}

		public long getUsableSpace() {
			return usableSpace;
		}

		public void setUsableSpace(long usableSpace) {
			this.usableSpace = usableSpace;
		}

		@Override
		public String toString() {
			return String.format(
					"StorageStats{totalFiles=%d, totalSize=%d bytes, cachedItems=%d, " +
							"pendingSyncs=%d, freeSpace=%d bytes, totalSpace=%d bytes}",
					totalFiles, totalSize, cachedItems, pendingSyncs, freeSpace, totalSpace);
		}
	}
}