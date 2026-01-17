package com.workflow.platform.component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workflow.platform.model.dto.AuditLogDTO;
import com.workflow.platform.model.entity.AuditLogEntity;
import com.workflow.platform.repository.AuditLogRepository;
import com.workflow.platform.util.JsonUtil;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 审计日志管理器 - 记录系统操作和事件
 */
@Slf4j
@Component
public class AuditLogManager {

	@Autowired
	private AuditLogRepository auditLogRepository;

	@Value("${workflow.platform.audit.enabled:true}")
	private boolean auditEnabled;

	@Value("${workflow.platform.audit.batch-size:100}")
	private int batchSize;

	@Value("${workflow.platform.audit.flush-interval:5000}")
	private long flushInterval;

	@Value("${workflow.platform.audit.retention-days:90}")
	private int retentionDays;

	@Value("${workflow.platform.audit.level:INFO}")
	private String auditLevel;

	// 审计日志队列
	private final BlockingQueue<AuditLogDTO> auditQueue = new LinkedBlockingQueue<>(10000);

	// 批量处理执行器
	private ScheduledExecutorService scheduler;
	private ExecutorService batchProcessor;

	// 统计
	private final AtomicInteger totalLogs = new AtomicInteger(0);
	private final AtomicInteger queuedLogs = new AtomicInteger(0);
	private final AtomicInteger processedLogs = new AtomicInteger(0);
	private final AtomicInteger failedLogs = new AtomicInteger(0);

	// 缓存最近的审计日志
	private final Map<String, List<AuditLogDTO>> recentLogsCache = new ConcurrentHashMap<>();
	private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

	@PostConstruct
	public void init() {
		if (!auditEnabled) {
			log.info("审计日志系统已禁用");
			return;
		}

		log.info("初始化审计日志管理器");

		// 初始化执行器
		scheduler = Executors.newScheduledThreadPool(2);
		batchProcessor = Executors.newFixedThreadPool(2);

		// 启动批处理任务
		scheduler.scheduleAtFixedRate(() -> {
			try {
				processBatch();
			} catch (Exception e) {
				log.error("审计日志批处理异常", e);
			}
		}, flushInterval, flushInterval, TimeUnit.MILLISECONDS);

		// 启动清理任务
		scheduler.scheduleAtFixedRate(() -> {
			try {
				cleanupOldLogs();
			} catch (Exception e) {
				log.error("审计日志清理异常", e);
			}
		}, 1, 24, TimeUnit.HOURS); // 每天清理一次

		log.info("审计日志管理器初始化完成");
	}

	/**
	 * 记录审计日志
	 */
	public void logAuditEvent(AuditLogDTO auditLog) {
		if (!auditEnabled) {
			return;
		}

		// 检查日志级别
		if (!shouldLog(auditLog.getLevel())) {
			return;
		}

		try {
			// 设置默认值
			if (auditLog.getTimestamp() == 0) {
				auditLog.setTimestamp(System.currentTimeMillis());
			}

			if (auditLog.getLogId() == null) {
				auditLog.setLogId(generateLogId());
			}

			// 添加到队列
			boolean offered = auditQueue.offer(auditLog, 100, TimeUnit.MILLISECONDS);
			if (offered) {
				totalLogs.incrementAndGet();
				queuedLogs.incrementAndGet();

				// 缓存最近的日志
				cacheRecentLog(auditLog);

				log.debug("审计日志已加入队列: {} - {}",
						auditLog.getAction(), auditLog.getResourceId());
			} else {
				log.warn("审计日志队列已满，丢弃日志: {}", auditLog.getAction());
				failedLogs.incrementAndGet();
			}

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			log.error("记录审计日志被中断", e);
		} catch (Exception e) {
			log.error("记录审计日志失败", e);
			failedLogs.incrementAndGet();
		}
	}

	/**
	 * 记录操作审计
	 */
	public void logOperation(String userId, String action, String resourceType,
			String resourceId, Object details, String level) {

		AuditLogDTO auditLog = AuditLogDTO.builder()
				.userId(userId)
				.action(action)
				.resourceType(resourceType)
				.resourceId(resourceId)
				.details(details != null ? JsonUtil.toJson(details) : null)
				.level(level != null ? level : auditLevel)
				.ipAddress(getClientIp())
				.userAgent(getUserAgent())
				.timestamp(System.currentTimeMillis())
				.build();

		logAuditEvent(auditLog);
	}

	/**
	 * 记录安全审计
	 */
	public void logSecurityEvent(String userId, String action, String resourceType,
			String resourceId, String outcome, Object details) {

		AuditLogDTO auditLog = AuditLogDTO.builder()
				.userId(userId)
				.action(action)
				.resourceType(resourceType)
				.resourceId(resourceId)
				.details(details != null ? JsonUtil.toJson(details) : null)
				.level("SECURITY")
				.category("SECURITY")
				.outcome(outcome)
				.ipAddress(getClientIp())
				.userAgent(getUserAgent())
				.timestamp(System.currentTimeMillis())
				.build();

		logAuditEvent(auditLog);
	}

	/**
	 * 记录系统审计
	 */
	public void logSystemEvent(String component, String action, String level,
			Object details) {

		AuditLogDTO auditLog = AuditLogDTO.builder()
				.userId("system")
				.action(action)
				.resourceType("SYSTEM")
				.resourceId(component)
				.details(details != null ? JsonUtil.toJson(details) : null)
				.level(level)
				.category("SYSTEM")
				.ipAddress("127.0.0.1")
				.userAgent("system")
				.timestamp(System.currentTimeMillis())
				.build();

		logAuditEvent(auditLog);
	}

	/**
	 * 查询审计日志
	 */
	public List<AuditLogDTO> queryAuditLogs(AuditQuery query) {
		if (!auditEnabled) {
			return Collections.emptyList();
		}

		try {
			// 先从缓存中查找
			List<AuditLogDTO> cachedLogs = getFromCache(query);
			if (cachedLogs != null && !cachedLogs.isEmpty()) {
				log.debug("从缓存获取审计日志: {}", query);
				return cachedLogs;
			}

			// 从数据库查询
			List<AuditLogEntity> entities = auditLogRepository.findByCriteria(
					query.getUserId(),
					query.getAction(),
					query.getResourceType(),
					query.getResourceId(),
					query.getLevel(),
					query.getStartTime(),
					query.getEndTime(),
					query.getPageable());

			List<AuditLogDTO> logs = entities.stream()
					.map(this::convertToDTO)
					.toList();

			// 更新缓存
			updateCache(query, logs);

			return logs;

		} catch (Exception e) {
			log.error("查询审计日志失败", e);
			return Collections.emptyList();
		}
	}

	/**
	 * 获取审计统计
	 */
	public AuditStats getAuditStats(AuditQuery query) {
		if (!auditEnabled) {
			return new AuditStats();
		}

		try {
			AuditStats stats = new AuditStats();
			stats.setQueryTime(System.currentTimeMillis());

			// 获取统计信息
			Map<String, Long> actionStats = auditLogRepository.countByAction(
					query.getStartTime(), query.getEndTime());
			Map<String, Long> userStats = auditLogRepository.countByUser(
					query.getStartTime(), query.getEndTime());
			Map<String, Long> resourceStats = auditLogRepository.countByResourceType(
					query.getStartTime(), query.getEndTime());
			Map<String, Long> outcomeStats = auditLogRepository.countByOutcome(
					query.getStartTime(), query.getEndTime());

			stats.setTotalLogs(auditLogRepository.countByTimestampBetween(
					query.getStartTime(), query.getEndTime()));
			stats.setActionStats(actionStats);
			stats.setUserStats(userStats);
			stats.setResourceStats(resourceStats);
			stats.setOutcomeStats(outcomeStats);

			// 计算成功率
			long successCount = outcomeStats.getOrDefault("SUCCESS", 0L);
			long failureCount = outcomeStats.getOrDefault("FAILURE", 0L);
			long total = successCount + failureCount;

			if (total > 0) {
				stats.setSuccessRate((double) successCount / total);
			}

			return stats;

		} catch (Exception e) {
			log.error("获取审计统计失败", e);
			return new AuditStats();
		}
	}

	/**
	 * 导出审计日志
	 */
	public AuditExport exportAuditLogs(AuditQuery query, ExportFormat format) {
		if (!auditEnabled) {
			return new AuditExport();
		}

		try {
			List<AuditLogDTO> logs = queryAuditLogs(query);
			AuditStats stats = getAuditStats(query);

			AuditExport export = new AuditExport();
			export.setQuery(query);
			export.setStats(stats);
			export.setLogs(logs);
			export.setExportTime(System.currentTimeMillis());
			export.setFormat(format);
			export.setTotalLogs(logs.size());

			// 生成导出数据
			switch (format) {
				case JSON:
					export.setData(JsonUtil.toPrettyJson(export));
					break;
				case CSV:
					export.setData(exportToCsv(logs));
					break;
				case XML:
					export.setData(exportToXml(logs));
					break;
				case PDF:
					export.setData(Arrays.toString(exportToPdf(logs, stats)));
					break;
			}

			// 记录导出操作
			logOperation("system", "EXPORT_AUDIT_LOGS", "AUDIT",
					"export_" + System.currentTimeMillis(),
					Map.of("format", format, "count", logs.size()),
					"INFO");

			return export;

		} catch (Exception e) {
			log.error("导出审计日志失败", e);
			throw new RuntimeException("导出审计日志失败", e);
		}
	}

	/**
	 * 获取系统审计状态
	 */
	public AuditSystemStatus getSystemStatus() {
		AuditSystemStatus status = new AuditSystemStatus();

		status.setEnabled(auditEnabled);
		status.setQueueSize(auditQueue.size());
		status.setTotalLogs(totalLogs.get());
		status.setQueuedLogs(queuedLogs.get());
		status.setProcessedLogs(processedLogs.get());
		status.setFailedLogs(failedLogs.get());
		status.setLastFlushTime(getLastFlushTime());
		status.setCacheSize(recentLogsCache.size());
		status.setDatabaseSize(auditLogRepository.count());

		return status;
	}

	/**
	 * 搜索审计日志
	 */
	public List<AuditLogDTO> searchAuditLogs(String keyword, AuditQuery query) {
		if (!auditEnabled) {
			return Collections.emptyList();
		}

		try {
			List<AuditLogEntity> entities = auditLogRepository.search(
					keyword, query.getStartTime(), query.getEndTime(), query.getPageable());

			return entities.stream()
					.map(this::convertToDTO)
					.toList();

		} catch (Exception e) {
			log.error("搜索审计日志失败", e);
			return Collections.emptyList();
		}
	}

	/**
	 * 清理指定条件的审计日志
	 */
	public CleanupResult cleanupAuditLogs(CleanupCriteria criteria) {
		if (!auditEnabled) {
			return CleanupResult.empty();
		}

		try {
			long startTime = System.currentTimeMillis();
			long cutoffTime = startTime - (criteria.getMaxAgeDays() * 24 * 60 * 60 * 1000L);

			// 执行清理
			int deletedCount = auditLogRepository.deleteByTimestampBefore(cutoffTime);

			// 如果启用了归档，归档被删除的日志
			if (criteria.isArchiveEnabled()) {
				archiveDeletedLogs(cutoffTime);
			}

			CleanupResult result = new CleanupResult();
			result.setDeletedCount(deletedCount);
			result.setCleanupTime(System.currentTimeMillis() - startTime);
			result.setCutoffTime(cutoffTime);

			// 记录清理操作
			logSystemEvent("AuditLogManager", "CLEANUP_AUDIT_LOGS", "INFO",
					Map.of("deletedCount", deletedCount, "cutoffTime", cutoffTime));

			log.info("清理审计日志完成，删除 {} 条记录", deletedCount);
			return result;

		} catch (Exception e) {
			log.error("清理审计日志失败", e);
			throw new RuntimeException("清理审计日志失败", e);
		}
	}

	// ========== 私有方法 ==========

	private void processBatch() {
		if (auditQueue.isEmpty()) {
			return;
		}

		List<AuditLogDTO> batch = new ArrayList<>(batchSize);
		auditQueue.drainTo(batch, batchSize);

		if (batch.isEmpty()) {
			return;
		}

		batchProcessor.submit(() -> {
			try {
				List<AuditLogEntity> entities = batch.stream()
						.map(this::convertToEntity)
						.toList();

				// 批量保存到数据库
				auditLogRepository.saveAll(entities);

				// 更新统计
				processedLogs.addAndGet(batch.size());
				queuedLogs.addAndGet(-batch.size());

				log.debug("批量处理审计日志完成: {} 条", batch.size());

			} catch (Exception e) {
				log.error("批量处理审计日志失败", e);
				failedLogs.addAndGet(batch.size());

				// 重新加入队列（可选）
				// auditQueue.addAll(batch);
			}
		});
	}

	private void cleanupOldLogs() {
		if (retentionDays <= 0) {
			return;
		}

		try {
			long cutoffTime = System.currentTimeMillis() -
					(retentionDays * 24 * 60 * 60 * 1000L);

			int deletedCount = auditLogRepository.deleteByTimestampBefore(cutoffTime);

			if (deletedCount > 0) {
				log.info("自动清理审计日志完成，删除 {} 条超过 {} 天的记录",
						deletedCount, retentionDays);

				// 记录清理操作
				logSystemEvent("AuditLogManager", "AUTO_CLEANUP_AUDIT_LOGS", "INFO",
						Map.of("deletedCount", deletedCount, "retentionDays", retentionDays));
			}

		} catch (Exception e) {
			log.error("自动清理审计日志失败", e);
		}
	}

	private boolean shouldLog(String level) {
		// 检查日志级别是否应该记录
		Map<String, Integer> levelPriority = Map.of(
				"DEBUG", 1,
				"INFO", 2,
				"WARN", 3,
				"ERROR", 4,
				"SECURITY", 5);

		int requestedPriority = levelPriority.getOrDefault(level.toUpperCase(), 2);
		int configuredPriority = levelPriority.getOrDefault(auditLevel.toUpperCase(), 2);

		return requestedPriority >= configuredPriority;
	}

	private String generateLogId() {
		return "audit_" + System.currentTimeMillis() + "_" +
				ThreadLocalRandom.current().nextInt(1000, 9999);
	}

	private String getClientIp() {
		// 在实际应用中，这里应该从请求上下文中获取客户端IP
		// 这里简化为返回本地IP
		return "127.0.0.1";
	}

	private String getUserAgent() {
		// 在实际应用中，这里应该从请求上下文中获取User-Agent
		// 这里简化为返回空字符串
		return "";
	}

	private void cacheRecentLog(AuditLogDTO auditLog) {
		String cacheKey = buildCacheKey(auditLog);

		cacheLock.writeLock().lock();
		try {
			List<AuditLogDTO> logs = recentLogsCache.computeIfAbsent(
					cacheKey, k -> new ArrayList<>());

			logs.add(auditLog);

			// 限制缓存大小
			if (logs.size() > 100) {
				logs.remove(0);
			}

		} finally {
			cacheLock.writeLock().unlock();
		}
	}

	private List<AuditLogDTO> getFromCache(AuditQuery query) {
		String cacheKey = buildCacheKey(query);

		cacheLock.readLock().lock();
		try {
			return recentLogsCache.getOrDefault(cacheKey, Collections.emptyList());
		} finally {
			cacheLock.readLock().unlock();
		}
	}

	private void updateCache(AuditQuery query, List<AuditLogDTO> logs) {
		String cacheKey = buildCacheKey(query);

		cacheLock.writeLock().lock();
		try {
			recentLogsCache.put(cacheKey, logs);
		} finally {
			cacheLock.writeLock().unlock();
		}
	}

	private String buildCacheKey(AuditLogDTO auditLog) {
		return String.format("%s:%s:%s",
				auditLog.getUserId(),
				auditLog.getAction(),
				auditLog.getResourceType());
	}

	private String buildCacheKey(AuditQuery query) {
		return String.format("%s:%s:%s:%d:%d",
				query.getUserId(),
				query.getAction(),
				query.getResourceType(),
				query.getStartTime(),
				query.getEndTime());
	}

	private AuditLogEntity convertToEntity(AuditLogDTO dto) {
		AuditLogEntity entity = new AuditLogEntity();
		entity.setLogId(dto.getLogId());
		entity.setUserId(dto.getUserId());
		entity.setAction(dto.getAction());
		entity.setResourceType(dto.getResourceType());
		entity.setResourceId(dto.getResourceId());
		entity.setDetails(dto.getDetails());
		entity.setLevel(dto.getLevel());
		entity.setCategory(dto.getCategory());
		entity.setOutcome(dto.getOutcome());
		entity.setIpAddress(dto.getIpAddress());
		entity.setUserAgent(dto.getUserAgent());
		entity.setTimestamp(dto.getTimestamp());
		entity.setCreatedAt(System.currentTimeMillis());
		return entity;
	}

	private AuditLogDTO convertToDTO(AuditLogEntity entity) {
		return AuditLogDTO.builder()
				.logId(entity.getLogId())
				.userId(entity.getUserId())
				.action(entity.getAction())
				.resourceType(entity.getResourceType())
				.resourceId(entity.getResourceId())
				.details(entity.getDetails())
				.level(entity.getLevel())
				.category(entity.getCategory())
				.outcome(entity.getOutcome())
				.ipAddress(entity.getIpAddress())
				.userAgent(entity.getUserAgent())
				.timestamp(entity.getTimestamp())
				.createdAt(entity.getCreatedAt())
				.build();
	}

	private String exportToCsv(List<AuditLogDTO> logs) {
		StringBuilder csv = new StringBuilder();

		// 表头
		csv.append("时间戳,用户ID,操作,资源类型,资源ID,级别,结果,IP地址,详情\n");

		// 数据行
		for (AuditLogDTO log : logs) {
			csv.append(log.getTimestamp()).append(",");
			csv.append(escapeCsv(log.getUserId())).append(",");
			csv.append(escapeCsv(log.getAction())).append(",");
			csv.append(escapeCsv(log.getResourceType())).append(",");
			csv.append(escapeCsv(log.getResourceId())).append(",");
			csv.append(escapeCsv(log.getLevel())).append(",");
			csv.append(escapeCsv(log.getOutcome())).append(",");
			csv.append(escapeCsv(log.getIpAddress())).append(",");
			csv.append(escapeCsv((String) log.getDetails())).append("\n");
		}

		return csv.toString();
	}

	private String escapeCsv(String value) {
		if (value == null) {
			return "";
		}
		if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}

	private String exportToXml(List<AuditLogDTO> logs) {
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<auditLogs>\n");

		for (AuditLogDTO log : logs) {
			xml.append("  <log>\n");
			xml.append("    <timestamp>").append(log.getTimestamp()).append("</timestamp>\n");
			xml.append("    <userId>").append(escapeXml(log.getUserId())).append("</userId>\n");
			xml.append("    <action>").append(escapeXml(log.getAction())).append("</action>\n");
			xml.append("    <resourceType>").append(escapeXml(log.getResourceType())).append("</resourceType>\n");
			xml.append("    <resourceId>").append(escapeXml(log.getResourceId())).append("</resourceId>\n");
			xml.append("    <level>").append(escapeXml(log.getLevel())).append("</level>\n");
			xml.append("    <outcome>").append(escapeXml(log.getOutcome())).append("</outcome>\n");
			xml.append("    <ipAddress>").append(escapeXml(log.getIpAddress())).append("</ipAddress>\n");
			xml.append("  </log>\n");
		}

		xml.append("</auditLogs>");
		return xml.toString();
	}

	private String escapeXml(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}

	private byte[] exportToPdf(List<AuditLogDTO> logs, AuditStats stats) {
		// 在实际应用中，这里应该使用PDF生成库（如iText）
		// 这里简化为返回JSON字符串的字节数组
		try {
			Map<String, Object> pdfData = new HashMap<>();
			pdfData.put("logs", logs);
			pdfData.put("stats", stats);
			pdfData.put("exportTime", System.currentTimeMillis());

			return JsonUtil.toJson(pdfData).getBytes();
		} catch (Exception e) {
			log.error("生成PDF失败", e);
			return new byte[0];
		}
	}

	private void archiveDeletedLogs(long cutoffTime) {
		// 在实际应用中，这里应该将删除的日志归档到文件或备份数据库
		log.info("归档删除的审计日志，截止时间: {}", cutoffTime);
	}

	private long getLastFlushTime() {
		// 在实际应用中，这里应该记录最后一次批量处理的时间
		return System.currentTimeMillis();
	}

	// ========== 内部类 ==========

	/**
	 * 审计查询条件
	 */
	@Data
	public static class AuditQuery {
		private String userId;
		private String action;
		private String resourceType;
		private String resourceId;
		private String level;
		private Long startTime;
		private Long endTime;
		private Pageable pageable;

		public AuditQuery() {
			this.endTime = System.currentTimeMillis();
			this.startTime = endTime - (7 * 24 * 60 * 60 * 1000L); // 默认最近7天
			this.pageable = Pageable.ofSize(100);
		}
	}

	/**
	 * 分页信息
	 */
	@Data
	public static class Pageable {
		private int page;
		private int size;
		private String sortBy;
		private String sortDirection;

		public static Pageable ofSize(int size) {
			Pageable pageable = new Pageable();
			pageable.setPage(0);
			pageable.setSize(size);
			pageable.setSortBy("timestamp");
			pageable.setSortDirection("DESC");
			return pageable;
		}
	}

	/**
	 * 审计统计
	 */
	@Data
	public static class AuditStats {
		private long totalLogs;
		private Map<String, Long> actionStats;
		private Map<String, Long> userStats;
		private Map<String, Long> resourceStats;
		private Map<String, Long> outcomeStats;
		private double successRate;
		private long queryTime;

		public AuditStats() {
			this.actionStats = new HashMap<>();
			this.userStats = new HashMap<>();
			this.resourceStats = new HashMap<>();
			this.outcomeStats = new HashMap<>();
		}
	}

	/**
	 * 审计导出
	 */
	@Data
	public static class AuditExport {
		private AuditQuery query;
		private AuditStats stats;
		private List<AuditLogDTO> logs;
		private long exportTime;
		private ExportFormat format;
		private String data;
		private int totalLogs;
	}

	/**
	 * 导出格式
	 */
	public enum ExportFormat {
		JSON, CSV, XML, PDF
	}

	/**
	 * 审计系统状态
	 */
	@Data
	public static class AuditSystemStatus {
		private boolean enabled;
		private int queueSize;
		private int totalLogs;
		private int queuedLogs;
		private int processedLogs;
		private int failedLogs;
		private long lastFlushTime;
		private int cacheSize;
		private long databaseSize;
		private long lastCleanupTime;
	}

	/**
	 * 清理条件
	 */
	@Data
	public static class CleanupCriteria {
		private int maxAgeDays = 90;
		private boolean archiveEnabled = true;
		private boolean dryRun = false;
	}

	/**
	 * 清理结果
	 */
	@Data
	public static class CleanupResult {
		private int deletedCount;
		private long cleanupTime;
		private long cutoffTime;
		private boolean archived;
		private String archivePath;

		public static CleanupResult empty() {
			CleanupResult result = new CleanupResult();
			result.setDeletedCount(0);
			result.setCleanupTime(0);
			result.setArchived(false);
			return result;
		}
	}
}