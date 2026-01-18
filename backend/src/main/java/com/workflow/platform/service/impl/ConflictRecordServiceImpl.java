package com.workflow.platform.service.impl;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:27
 */

import com.workflow.platform.exception.ConflictException;
import com.workflow.platform.model.dto.ConflictRecordDTO;
import com.workflow.platform.model.dto.ConflictResolutionDTO;
import com.workflow.platform.model.entity.ConflictRecordEntity;
import com.workflow.platform.model.vo.ConflictRecordVO;
import com.workflow.platform.model.vo.ConflictStatisticsVO;
import com.workflow.platform.repository.ConflictRecordRepository;
import com.workflow.platform.service.ConflictRecordService;
import com.workflow.platform.service.NotificationService;
import com.workflow.platform.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 冲突记录服务实现
 */
@Service
@Slf4j
public class ConflictRecordServiceImpl implements ConflictRecordService {

	@Autowired
	private ConflictRecordRepository conflictRecordRepository;

	@Autowired
	private NotificationService notificationService;

	@Value("${workflow.conflict.auto-resolution.enabled:true}")
	private boolean autoResolutionEnabled;

	@Value("${workflow.conflict.max-retry-count:3}")
	private int maxRetryCount;

	@Value("${workflow.conflict.notification.enabled:true}")
	private boolean notificationEnabled;

	@Value("${workflow.conflict.cleanup.days-to-keep:30}")
	private int daysToKeep;

	@Override
	@Transactional
	public ConflictRecordVO createConflictRecord(ConflictRecordDTO conflictRecordDTO) {
		log.info("创建冲突记录，对象类型: {}，对象ID: {}",
				conflictRecordDTO.getObjectType(), conflictRecordDTO.getObjectId());

		// 验证冲突记录
		if (!validateConflictRecord(conflictRecordDTO)) {
			throw new ConflictException("冲突记录验证失败");
		}

		// 检查重复冲突
		if (checkDuplicateConflict(conflictRecordDTO)) {
			log.warn("发现重复冲突，对象类型: {}，对象ID: {}",
					conflictRecordDTO.getObjectType(), conflictRecordDTO.getObjectId());
			throw new ConflictException("冲突记录已存在");
		}

		// 计算冲突哈希
		String conflictHash = calculateConflictHash(conflictRecordDTO);

		// 创建冲突实体
		ConflictRecordEntity conflictRecord = new ConflictRecordEntity();
		BeanUtils.copyProperties(conflictRecordDTO, conflictRecord);

		// 设置JSON数据
		if (conflictRecordDTO.getLocalData() != null) {
			conflictRecord.setLocalData(JsonUtil.toJson(conflictRecordDTO.getLocalData()));
		}

		if (conflictRecordDTO.getRemoteData() != null) {
			conflictRecord.setRemoteData(JsonUtil.toJson(conflictRecordDTO.getRemoteData()));
		}

		if (conflictRecordDTO.getMetadata() != null) {
			conflictRecord.setMetadata(JsonUtil.toJson(conflictRecordDTO.getMetadata()));
		}

		// 设置其他字段
		conflictRecord.setConflictHash(conflictHash);
		conflictRecord.setDetectedTime(LocalDateTime.now());
		conflictRecord.setStatus("PENDING");

		if (conflictRecordDTO.getSeverity() == null) {
			conflictRecord.setSeverity("MEDIUM");
		}

		// 尝试自动解决
		if (autoResolutionEnabled && canAutoResolve(conflictRecord)) {
			return autoResolveConflictRecord(conflictRecord);
		}

		// 保存冲突记录
		conflictRecordRepository.save(conflictRecord);

		log.info("冲突记录创建成功，ID: {}", conflictRecord.getId());

		// 发送通知（异步）
		if (notificationEnabled) {
			sendConflictNotification(conflictRecord);
		}

		return convertToVO(conflictRecord);
	}

	@Override
	@Transactional
	public List<ConflictRecordVO> batchCreateConflictRecords(List<ConflictRecordDTO> conflictRecordDTOs) {
		log.info("批量创建冲突记录，数量: {}", conflictRecordDTOs.size());

		List<ConflictRecordVO> results = new ArrayList<>();

		for (ConflictRecordDTO dto : conflictRecordDTOs) {
			try {
				ConflictRecordVO vo = createConflictRecord(dto);
				results.add(vo);
			} catch (Exception e) {
				log.error("创建冲突记录失败: {}", dto, e);
				// 继续处理其他记录
			}
		}

		log.info("批量创建冲突记录完成，成功: {}，失败: {}",
				results.size(), conflictRecordDTOs.size() - results.size());

		return results;
	}

	@Override
	public ConflictRecordVO getConflictRecord(Long id) {
		log.debug("获取冲突记录详情，ID: {}", id);

		ConflictRecordEntity conflictRecord = conflictRecordRepository.findById(id)
				.orElseThrow(() -> new ConflictException("冲突记录不存在"));

		ConflictRecordVO vo = convertToVO(conflictRecord);

		// 添加详细分析信息
		enrichConflictDetails(vo);

		return vo;
	}

	@Override
	public Page<ConflictRecordVO> getConflictRecords(Map<String, Object> queryParams, int page, int size) {
		log.debug("获取冲突记录列表，页码: {}，大小: {}", page, size);

		Specification<ConflictRecordEntity> spec = buildSpecification(queryParams);
		Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "detectedTime"));

		Page<ConflictRecordEntity> conflictPage = conflictRecordRepository.findAll(spec, pageable);

		return conflictPage.map(this::convertToVO);
	}

	@Override
	public List<ConflictRecordVO> queryConflictRecords(Map<String, Object> criteria) {
		log.debug("查询冲突记录，条件: {}", criteria);

		Specification<ConflictRecordEntity> spec = buildSpecification(criteria);
		List<ConflictRecordEntity> conflictRecords = conflictRecordRepository.findAll(spec);

		return conflictRecords.stream()
				.map(this::convertToVO)
				.collect(Collectors.toList());
	}

	@Transactional
	public ConflictRecordVO resolveConflict(ConflictResolutionDTO resolutionDTO) {
		log.info("解决冲突，冲突ID: {}，解决策略: {}",
				resolutionDTO.getConflictId(), resolutionDTO.getResolutionStrategy());

		ConflictRecordEntity conflictRecord = conflictRecordRepository.findById(resolutionDTO.getConflictId())
				.orElseThrow(() -> new ConflictException("冲突记录不存在"));

		// 检查状态
		if (!"PENDING".equals(conflictRecord.getStatus())) {
			throw new ConflictException("冲突记录状态不正确: " + conflictRecord.getStatus());
		}

		// 更新冲突记录
		conflictRecord.setStatus("RESOLVED");
		conflictRecord.setResolutionStrategy(resolutionDTO.getResolutionStrategy());
		conflictRecord.setResolvedBy(resolutionDTO.getResolvedBy());
		conflictRecord.setResolvedTime(LocalDateTime.now());
		conflictRecord.setResolutionNotes(resolutionDTO.getResolutionNotes());

		if (resolutionDTO.getResolutionData() != null) {
			conflictRecord.setResolutionResult(JsonUtil.toJson(resolutionDTO.getResolutionData()));
		}

		// 保存更新
		conflictRecordRepository.save(conflictRecord);

		log.info("冲突解决成功，冲突ID: {}", resolutionDTO.getConflictId());

		// 发送解决通知（异步）
		if (notificationEnabled && resolutionDTO.getNotifyUsers() != null && resolutionDTO.getNotifyUsers()) {
			sendResolutionNotification(conflictRecord);
		}

		// 执行解决后的操作
		performPostResolutionActions(conflictRecord);

		return convertToVO(conflictRecord);
	}

	@Transactional
	public List<ConflictRecordVO> batchResolveConflicts(List<ConflictResolutionDTO> resolutionDTOs) {
		log.info("批量解决冲突，数量: {}", resolutionDTOs.size());

		List<ConflictRecordVO> results = new ArrayList<>();

		for (ConflictResolutionDTO dto : resolutionDTOs) {
			try {
				ConflictRecordVO vo = resolveConflict(dto);
				results.add(vo);
			} catch (Exception e) {
				log.error("解决冲突失败: {}", dto.getConflictId(), e);
				// 继续处理其他冲突
			}
		}

		log.info("批量解决冲突完成，成功: {}，失败: {}",
				results.size(), resolutionDTOs.size() - results.size());

		return results;
	}

	@Override
	@Transactional
	public ConflictRecordVO ignoreConflict(Long id, String notes, String ignoredBy) {
		log.info("忽略冲突，冲突ID: {}", id);

		ConflictRecordEntity conflictRecord = conflictRecordRepository.findById(id)
				.orElseThrow(() -> new ConflictException("冲突记录不存在"));

		// 检查状态
		if (!"PENDING".equals(conflictRecord.getStatus())) {
			throw new ConflictException("冲突记录状态不正确: " + conflictRecord.getStatus());
		}

		// 更新冲突记录
		conflictRecord.setStatus("IGNORED");
		conflictRecord.setResolvedBy(ignoredBy);
		conflictRecord.setResolvedTime(LocalDateTime.now());
		conflictRecord.setResolutionNotes(notes);
		conflictRecord.setResolutionStrategy("IGNORED");

		conflictRecordRepository.save(conflictRecord);

		log.info("冲突忽略成功，冲突ID: {}", id);

		return convertToVO(conflictRecord);
	}

	@Override
	@Transactional
	public ConflictRecordVO reopenConflict(Long id, String notes) {
		log.info("重新打开冲突，冲突ID: {}", id);

		ConflictRecordEntity conflictRecord = conflictRecordRepository.findById(id)
				.orElseThrow(() -> new ConflictException("冲突记录不存在"));

		// 检查状态
		if (!"RESOLVED".equals(conflictRecord.getStatus()) && !"IGNORED".equals(conflictRecord.getStatus())) {
			throw new ConflictException("只有已解决或已忽略的冲突可以重新打开");
		}

		// 更新冲突记录
		conflictRecord.setStatus("PENDING");
		conflictRecord.setResolutionStrategy(null);
		conflictRecord.setResolvedBy(null);
		conflictRecord.setResolvedTime(null);
		conflictRecord.setResolutionNotes(notes);
		conflictRecord.setResolutionResult(null);

		conflictRecordRepository.save(conflictRecord);

		log.info("冲突重新打开成功，冲突ID: {}", id);

		return convertToVO(conflictRecord);
	}

	@Override
	@Transactional
	public ConflictRecordVO autoResolveConflict(Long id) {
		log.info("自动解决冲突，冲突ID: {}", id);

		ConflictRecordEntity conflictRecord = conflictRecordRepository.findById(id)
				.orElseThrow(() -> new ConflictException("冲突记录不存在"));

		// 检查是否可以自动解决
		if (!canAutoResolve(conflictRecord)) {
			throw new ConflictException("无法自动解决此冲突");
		}

		// 使用合适的策略自动解决
		String resolutionStrategy = determineAutoResolutionStrategy(conflictRecord);

		// 模拟解决结果
		Map<String, Object> resolutionData = new HashMap<>();
		resolutionData.put("autoResolved", true);
		resolutionData.put("strategy", resolutionStrategy);
		resolutionData.put("timestamp", LocalDateTime.now().toString());

		// 更新冲突记录
		conflictRecord.setStatus("RESOLVED");
		conflictRecord.setResolutionStrategy(resolutionStrategy);
		conflictRecord.setResolvedBy("SYSTEM");
		conflictRecord.setResolvedTime(LocalDateTime.now());
		conflictRecord.setResolutionNotes("系统自动解决");
		conflictRecord.setResolutionResult(JsonUtil.toJson(resolutionData));
		conflictRecord.setAutoResolved(true);

		conflictRecordRepository.save(conflictRecord);

		log.info("冲突自动解决成功，冲突ID: {}，策略: {}", id, resolutionStrategy);

		return convertToVO(conflictRecord);
	}

	@Override
	@Transactional
	public List<ConflictRecordVO> batchAutoResolveConflicts(List<Long> ids) {
		log.info("批量自动解决冲突，数量: {}", ids.size());

		List<ConflictRecordVO> results = new ArrayList<>();

		for (Long id : ids) {
			try {
				ConflictRecordVO vo = autoResolveConflict(id);
				results.add(vo);
			} catch (Exception e) {
				log.error("自动解决冲突失败: {}", id, e);
				// 继续处理其他冲突
			}
		}

		log.info("批量自动解决冲突完成，成功: {}，失败: {}",
				results.size(), ids.size() - results.size());

		return results;
	}

	@Override
	public List<ConflictRecordVO> getPendingConflicts() {
		log.debug("获取待解决的冲突列表");

		List<ConflictRecordEntity> conflictRecords = conflictRecordRepository
				.findByStatusOrderBySeverityDescDetectedTimeDesc("PENDING");

		return conflictRecords.stream()
				.map(this::convertToVO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ConflictRecordVO> getHighPriorityConflicts() {
		log.debug("获取高优先级的冲突列表");

		List<ConflictRecordEntity> conflictRecords = conflictRecordRepository
				.findByStatusOrderBySeverityDescDetectedTimeDesc("PENDING");

		// 过滤高优先级（严重程度为HIGH或CRITICAL）
		return conflictRecords.stream()
				.filter(c -> "HIGH".equals(c.getSeverity()) || "CRITICAL".equals(c.getSeverity()))
				.map(this::convertToVO)
				.collect(Collectors.toList());
	}

	public ConflictStatisticsVO getConflictStatistics(LocalDateTime startTime, LocalDateTime endTime) {
		log.debug("统计冲突信息，开始时间: {}，结束时间: {}", startTime, endTime);

		ConflictStatisticsVO statistics = new ConflictStatisticsVO();

		// 统计总数
		List<Object[]> statusCounts = conflictRecordRepository.countByStatus();
		long total = 0;
		long pending = 0;
		long resolved = 0;

		for (Object[] statusCount : statusCounts) {
			String status = (String) statusCount[0];
			Long count = (Long) statusCount[1];
			total += count;

			if ("PENDING".equals(status)) {
				pending = count;
			} else if ("RESOLVED".equals(status)) {
				resolved = count;
			}
		}

		statistics.setTotalConflicts(total);
		statistics.setPendingConflicts(pending);
		statistics.setResolvedConflicts(resolved);

		// 统计类型分布
		List<Object[]> typeCounts = conflictRecordRepository.countByConflictType();
		List<ConflictStatisticsVO.TypeStatistic> typeStats = new ArrayList<>();

		for (Object[] typeCount : typeCounts) {
			String type = (String) typeCount[0];
			Long count = (Long) typeCount[1];

			ConflictStatisticsVO.TypeStatistic stat = new ConflictStatisticsVO.TypeStatistic();
			stat.setType(type);
			stat.setTypeName(getConflictTypeName(type));
			stat.setCount(count);
			stat.setPercentage(total > 0 ? (double) count / total * 100 : 0.0);

			typeStats.add(stat);
		}

		statistics.setConflictTypeStatistics(typeStats);

		// 统计日期分布
		List<Object[]> dateCounts = conflictRecordRepository.countByDate(startTime, endTime);
		List<ConflictStatisticsVO.DateStatistic> dateStats = new ArrayList<>();

		for (Object[] dateCount : dateCounts) {
			java.sql.Date sqlDate = (java.sql.Date) dateCount[0];
			Long count = (Long) dateCount[1];

			ConflictStatisticsVO.DateStatistic stat = new ConflictStatisticsVO.DateStatistic();
			stat.setDate(sqlDate.toString());
			stat.setNewConflicts(count);

			dateStats.add(stat);
		}

		statistics.setDateStatistics(dateStats);

		return statistics;
	}

	@Override
	public Map<String, Object> getRealtimeConflictStats() {
		log.debug("获取实时冲突统计");

		Map<String, Object> stats = new HashMap<>();

		// 待解决冲突数
		long pendingCount = conflictRecordRepository.countByStatus().size();
		stats.put("pendingCount", pendingCount);

		// 今日新增冲突数
		LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
		LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

		long todayNewCount = conflictRecordRepository.findByDetectedTimeBetween(todayStart, todayEnd).size();
		stats.put("todayNewCount", todayNewCount);

		// 高优先级冲突数
		List<ConflictRecordVO> highPriority = getHighPriorityConflicts();
		stats.put("highPriorityCount", highPriority.size());

		// 最近解决的冲突
		List<ConflictRecordEntity> recentResolved = conflictRecordRepository
				.findByStatusOrderByDetectedTimeDesc("RESOLVED").stream()
				.limit(5)
				.collect(Collectors.toList());
		stats.put("recentResolved", convertToVOList(recentResolved));

		return stats;
	}

	@Override
	public Map<String, Object> analyzeConflictTrend(int days) {
		log.debug("分析冲突趋势，天数: {}", days);

		Map<String, Object> trendAnalysis = new HashMap<>();

		LocalDateTime endDate = LocalDateTime.now();
		LocalDateTime startDate = endDate.minusDays(days);

		// 获取日期统计数据
		List<Object[]> dateCounts = conflictRecordRepository.countByDate(startDate, endDate);

		List<Map<String, Object>> trendData = new ArrayList<>();
		for (Object[] dateCount : dateCounts) {
			java.sql.Date sqlDate = (java.sql.Date) dateCount[0];
			Long count = (Long) dateCount[1];

			Map<String, Object> dataPoint = new HashMap<>();
			dataPoint.put("date", sqlDate.toString());
			dataPoint.put("count", count);

			trendData.add(dataPoint);
		}

		trendAnalysis.put("trendData", trendData);
		trendAnalysis.put("days", days);
		trendAnalysis.put("startDate", startDate);
		trendAnalysis.put("endDate", endDate);

		// 分析趋势
		if (trendData.size() >= 2) {
			long firstDayCount = (Long) trendData.get(0).get("count");
			long lastDayCount = (Long) trendData.get(trendData.size() - 1).get("count");

			double trend = lastDayCount - firstDayCount;
			double trendPercentage = firstDayCount > 0 ? (double) trend / firstDayCount * 100 : 0;

			trendAnalysis.put("trendValue", trend);
			trendAnalysis.put("trendPercentage", trendPercentage);

			if (trend > 0) {
				trendAnalysis.put("trendDirection", "上升");
				trendAnalysis.put("trendLevel", trendPercentage > 20 ? "显著上升" : "轻微上升");
			} else if (trend < 0) {
				trendAnalysis.put("trendDirection", "下降");
				trendAnalysis.put("trendLevel", trendPercentage < -20 ? "显著下降" : "轻微下降");
			} else {
				trendAnalysis.put("trendDirection", "平稳");
				trendAnalysis.put("trendLevel", "保持稳定");
			}
		}

		return trendAnalysis;
	}

	@Override
	@Async
	public CompletableFuture<String> generateConflictReport(LocalDateTime startTime, LocalDateTime endTime,
			String format) {
		log.info("生成冲突报告，开始时间: {}，结束时间: {}，格式: {}", startTime, endTime, format);

		try {
			// 获取统计数据
			ConflictStatisticsVO statistics = getConflictStatistics(startTime, endTime);

			// 获取冲突列表
			List<ConflictRecordEntity> conflicts = conflictRecordRepository
					.findByDetectedTimeBetween(startTime, endTime);

			// 生成报告内容
			StringBuilder report = new StringBuilder();
			report.append("冲突报告\n");
			report.append("==========\n\n");
			report.append("报告时间: ").append(LocalDateTime.now()).append("\n");
			report.append("统计周期: ").append(startTime).append(" 至 ").append(endTime).append("\n\n");

			report.append("总体统计\n");
			report.append("--------\n");
			report.append("总冲突数: ").append(statistics.getTotalConflicts()).append("\n");
			report.append("待解决: ").append(statistics.getPendingConflicts()).append("\n");
			report.append("已解决: ").append(statistics.getResolvedConflicts()).append("\n\n");

			report.append("按类型统计\n");
			report.append("--------\n");
			for (ConflictStatisticsVO.TypeStatistic stat : statistics.getConflictTypeStatistics()) {
				report.append(String.format("%s: %d (%.1f%%)\n",
						stat.getTypeName(), stat.getCount(), stat.getPercentage()));
			}

			report.append("\n详细列表\n");
			report.append("--------\n");
			for (ConflictRecordEntity conflict : conflicts) {
				report.append(String.format("ID: %d, 类型: %s, 对象: %s, 状态: %s, 检测时间: %s\n",
						conflict.getId(),
						conflict.getConflictType(),
						conflict.getObjectName(),
						conflict.getStatus(),
						conflict.getDetectedTime()));
			}

			// 根据格式调整输出
			String reportContent;
			if ("json".equalsIgnoreCase(format)) {
				Map<String, Object> reportData = new HashMap<>();
				reportData.put("statistics", statistics);
				reportData.put("conflicts", convertToVOList(conflicts));
				reportData.put("generatedAt", LocalDateTime.now());

				reportContent = JsonUtil.toJson(reportData);
			} else {
				reportContent = report.toString();
			}

			log.info("冲突报告生成成功");
			return CompletableFuture.completedFuture(reportContent);

		} catch (Exception e) {
			log.error("生成冲突报告失败", e);
			return CompletableFuture.completedFuture("生成冲突报告失败: " + e.getMessage());
		}
	}

	@Override
	@Transactional
	public int cleanupOldConflictRecords(int daysToKeep) {
		log.info("清理旧的冲突记录，保留天数: {}", daysToKeep);

		LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);

		int deletedCount = conflictRecordRepository.deleteOldResolvedRecords(cutoffTime);

		log.info("清理完成，删除记录数: {}", deletedCount);
		return deletedCount;
	}

	@Override
	@Transactional
	public ConflictRecordVO retryConflictDetection(Long id) {
		log.info("重新检测冲突，冲突ID: {}", id);

		ConflictRecordEntity conflictRecord = conflictRecordRepository.findById(id)
				.orElseThrow(() -> new ConflictException("冲突记录不存在"));

		// 检查状态
		if (!"PENDING".equals(conflictRecord.getStatus())) {
			throw new ConflictException("只有待解决的冲突可以重新检测");
		}

		// 检查重试次数
		if (conflictRecord.getRetryCount() >= maxRetryCount) {
			throw new ConflictException("已达到最大重试次数");
		}

		// 更新重试信息
		conflictRecord.setRetryCount(conflictRecord.getRetryCount() + 1);
		conflictRecord.setLastRetryTime(LocalDateTime.now());

		conflictRecordRepository.save(conflictRecord);

		log.info("冲突重新检测成功，冲突ID: {}，重试次数: {}", id, conflictRecord.getRetryCount());

		return convertToVO(conflictRecord);
	}

	@Override
	@Transactional
	public void markAsNotified(Long id) {
		log.debug("标记冲突为已通知，冲突ID: {}", id);

		int updated = conflictRecordRepository.markAsNotified(id);

		if (updated > 0) {
			log.debug("冲突标记为已通知成功，冲突ID: {}", id);
		}
	}

	@Override
	public Map<String, Object> getConflictResolutionSuggestions(Long id) {
		log.debug("获取冲突解决建议，冲突ID: {}", id);

		ConflictRecordEntity conflictRecord = conflictRecordRepository.findById(id)
				.orElseThrow(() -> new ConflictException("冲突记录不存在"));

		Map<String, Object> suggestions = new HashMap<>();

		// 分析冲突类型
		String conflictType = conflictRecord.getConflictType();
		String objectType = conflictRecord.getObjectType();

		// 根据冲突类型提供建议
		List<Map<String, Object>> strategySuggestions = new ArrayList<>();

		if ("WORKFLOW_CONFLICT".equals(conflictType)) {
			// 工作流冲突建议
			strategySuggestions.add(createStrategySuggestion(
					"TIMESTAMP_PRIORITY", "时间戳优先", "使用最新的修改", 4.5, "HIGH"));
			strategySuggestions.add(createStrategySuggestion(
					"MANUAL", "手动合并", "手动检查并合并修改", 4.0, "MEDIUM"));
			strategySuggestions.add(createStrategySuggestion(
					"CLIENT_PRIORITY", "客户端优先", "保留本地修改", 3.5, "LOW"));
		} else if ("NODE_CONFLICT".equals(conflictType)) {
			// 节点冲突建议
			strategySuggestions.add(createStrategySuggestion(
					"MERGE", "合并节点", "尝试自动合并节点配置", 4.2, "MEDIUM"));
			strategySuggestions.add(createStrategySuggestion(
					"TIMESTAMP_PRIORITY", "时间戳优先", "使用最新的节点配置", 4.0, "HIGH"));
		} else {
			// 通用建议
			strategySuggestions.add(createStrategySuggestion(
					"TIMESTAMP_PRIORITY", "时间戳优先", "使用最新的修改", 4.0, "MEDIUM"));
			strategySuggestions.add(createStrategySuggestion(
					"MANUAL", "手动检查", "手动审查冲突", 3.8, "LOW"));
		}

		suggestions.put("strategies", strategySuggestions);
		suggestions.put("analysis", analyzeConflict(conflictRecord));
		suggestions.put("canAutoResolve", canAutoResolve(conflictRecord));
		suggestions.put("recommendedStrategy", getRecommendedStrategy(conflictRecord));

		return suggestions;
	}

	@Override
	public boolean validateConflictRecord(ConflictRecordDTO conflictRecordDTO) {
		if (conflictRecordDTO == null) {
			return false;
		}

		if (!StringUtils.hasText(conflictRecordDTO.getConflictType())) {
			return false;
		}

		if (!StringUtils.hasText(conflictRecordDTO.getObjectType())) {
			return false;
		}

		if (!StringUtils.hasText(conflictRecordDTO.getObjectId())) {
			return false;
		}

		// 检查必要数据
		if (conflictRecordDTO.getLocalData() == null && conflictRecordDTO.getRemoteData() == null) {
			return false;
		}

		return true;
	}

	@Override
	public String calculateConflictHash(ConflictRecordDTO conflictRecordDTO) {
		StringBuilder hashInput = new StringBuilder();

		hashInput.append(conflictRecordDTO.getConflictType())
				.append(conflictRecordDTO.getObjectType())
				.append(conflictRecordDTO.getObjectId())
				.append(conflictRecordDTO.getLocalVersion())
				.append(conflictRecordDTO.getRemoteVersion());

		if (conflictRecordDTO.getLocalUpdateTime() != null) {
			hashInput.append(conflictRecordDTO.getLocalUpdateTime());
		}

		if (conflictRecordDTO.getRemoteUpdateTime() != null) {
			hashInput.append(conflictRecordDTO.getRemoteUpdateTime());
		}

		return DigestUtils.md5DigestAsHex(hashInput.toString().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public boolean checkDuplicateConflict(ConflictRecordDTO conflictRecordDTO) {
		String conflictHash = calculateConflictHash(conflictRecordDTO);
		List<ConflictRecordEntity> duplicates = conflictRecordRepository.findByConflictHash(conflictHash);

		return !duplicates.isEmpty();
	}

	@Override
	public void save(ConflictRecordEntity conflictRecord) {

	}

	// ==================== 私有方法 ====================

	private Specification<ConflictRecordEntity> buildSpecification(Map<String, Object> criteria) {
		return (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (criteria != null) {
				// 对象类型
				if (criteria.containsKey("objectType")) {
					predicates.add(cb.equal(root.get("objectType"), criteria.get("objectType")));
				}

				// 对象ID
				if (criteria.containsKey("objectId")) {
					predicates.add(cb.equal(root.get("objectId"), criteria.get("objectId")));
				}

				// 冲突类型
				if (criteria.containsKey("conflictType")) {
					predicates.add(cb.equal(root.get("conflictType"), criteria.get("conflictType")));
				}

				// 状态
				if (criteria.containsKey("status")) {
					predicates.add(cb.equal(root.get("status"), criteria.get("status")));
				}

				// 严重程度
				if (criteria.containsKey("severity")) {
					predicates.add(cb.equal(root.get("severity"), criteria.get("severity")));
				}

				// 是否已通知
				if (criteria.containsKey("notified")) {
					predicates.add(cb.equal(root.get("notified"), criteria.get("notified")));
				}

				// 是否自动解决
				if (criteria.containsKey("autoResolved")) {
					predicates.add(cb.equal(root.get("autoResolved"), criteria.get("autoResolved")));
				}

				// 时间范围
				if (criteria.containsKey("startTime") && criteria.containsKey("endTime")) {
					LocalDateTime startTime = (LocalDateTime) criteria.get("startTime");
					LocalDateTime endTime = (LocalDateTime) criteria.get("endTime");
					predicates.add(cb.between(root.get("detectedTime"), startTime, endTime));
				}
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
	}

	private ConflictRecordVO convertToVO(ConflictRecordEntity entity) {
		ConflictRecordVO vo = new ConflictRecordVO();
		BeanUtils.copyProperties(entity, vo);

		// 解析JSON数据
		if (StringUtils.hasText(entity.getLocalData())) {
			vo.setLocalDataSummary(JsonUtil.fromJson(entity.getLocalData(), Map.class));
		}

		if (StringUtils.hasText(entity.getRemoteData())) {
			vo.setRemoteDataSummary(JsonUtil.fromJson(entity.getRemoteData(), Map.class));
		}

		if (StringUtils.hasText(entity.getMetadata())) {
			vo.setMetadata(JsonUtil.fromJson(entity.getMetadata(), Map.class));
		}

		if (StringUtils.hasText(entity.getResolutionResult())) {
			vo.setResolutionResult(JsonUtil.fromJson(entity.getResolutionResult(), Map.class));
		}

		// 设置显示名称
		vo.setConflictTypeName(getConflictTypeName(entity.getConflictType()));
		vo.setObjectTypeName(getObjectTypeName(entity.getObjectType()));
		vo.setStatusName(getStatusName(entity.getStatus()));
		vo.setResolutionStrategyName(getResolutionStrategyName(entity.getResolutionStrategy()));
		vo.setSeverityName(getSeverityName(entity.getSeverity()));

		// 设置操作权限
		vo.setCanAutoResolve(canAutoResolve(entity));
		vo.setCanIgnore("PENDING".equals(entity.getStatus()));
		vo.setCanRetry("PENDING".equals(entity.getStatus()) && entity.getRetryCount() < maxRetryCount);

		return vo;
	}

	private List<ConflictRecordVO> convertToVOList(List<ConflictRecordEntity> entities) {
		return entities.stream()
				.map(this::convertToVO)
				.collect(Collectors.toList());
	}

	private boolean canAutoResolve(ConflictRecordEntity conflictRecord) {
		// 检查是否可以自动解决
		if (!autoResolutionEnabled) {
			return false;
		}

		// 检查冲突类型
		String conflictType = conflictRecord.getConflictType();
		if ("WORKFLOW_CONFLICT".equals(conflictType) || "NODE_CONFLICT".equals(conflictType)) {
			// 检查严重程度
			String severity = conflictRecord.getSeverity();
			return !"CRITICAL".equals(severity);
		}

		return false;
	}

	private ConflictRecordVO autoResolveConflictRecord(ConflictRecordEntity conflictRecord) {
		log.info("自动解决冲突记录，ID: {}", conflictRecord.getId());

		// 使用时间戳优先策略自动解决
		conflictRecord.setStatus("RESOLVED");
		conflictRecord.setResolutionStrategy("TIMESTAMP_PRIORITY");
		conflictRecord.setResolvedBy("SYSTEM");
		conflictRecord.setResolvedTime(LocalDateTime.now());
		conflictRecord.setResolutionNotes("系统自动解决（时间戳优先）");
		conflictRecord.setAutoResolved(true);

		// 模拟解决结果
		Map<String, Object> resolutionResult = new HashMap<>();
		resolutionResult.put("autoResolved", true);
		resolutionResult.put("strategy", "TIMESTAMP_PRIORITY");
		resolutionResult.put("timestamp", LocalDateTime.now().toString());

		conflictRecord.setResolutionResult(JsonUtil.toJson(resolutionResult));

		conflictRecordRepository.save(conflictRecord);

		log.info("冲突自动解决成功，ID: {}", conflictRecord.getId());
		return convertToVO(conflictRecord);
	}

	private String determineAutoResolutionStrategy(ConflictRecordEntity conflictRecord) {
		// 根据冲突特征确定自动解决策略
		if (conflictRecord.getLocalUpdateTime() != null && conflictRecord.getRemoteUpdateTime() != null) {
			// 比较时间戳
			if (conflictRecord.getLocalUpdateTime().isAfter(conflictRecord.getRemoteUpdateTime())) {
				return "CLIENT_PRIORITY";
			} else if (conflictRecord.getRemoteUpdateTime().isAfter(conflictRecord.getLocalUpdateTime())) {
				return "SERVER_PRIORITY";
			}
		}

		// 默认使用时间戳优先
		return "TIMESTAMP_PRIORITY";
	}

	private String getRecommendedStrategy(ConflictRecordEntity conflictRecord) {
		// 根据冲突特征推荐解决策略
		if (conflictRecord.getLocalUpdateTime() != null && conflictRecord.getRemoteUpdateTime() != null) {
			long timeDiff = java.time.Duration.between(
					conflictRecord.getLocalUpdateTime(),
					conflictRecord.getRemoteUpdateTime()).toMinutes();

			if (Math.abs(timeDiff) > 60) { // 时间差超过1小时
				if (timeDiff > 0) {
					return "SERVER_PRIORITY";
				} else {
					return "CLIENT_PRIORITY";
				}
			}
		}

		return "TIMESTAMP_PRIORITY";
	}

	private Map<String, Object> createStrategySuggestion(String type, String name,
			String description,
			double score, String priority) {
		Map<String, Object> suggestion = new HashMap<>();
		suggestion.put("strategyType", type);
		suggestion.put("strategyName", name);
		suggestion.put("strategyDescription", description);
		suggestion.put("recommendationScore", score);
		suggestion.put("priority", priority);
		suggestion.put("autoResolutionSupported", !"MANUAL".equals(type));
		return suggestion;
	}

	private Map<String, Object> analyzeConflict(ConflictRecordEntity conflictRecord) {
		Map<String, Object> analysis = new HashMap<>();

		// 分析冲突特征
		analysis.put("conflictType", conflictRecord.getConflictType());
		analysis.put("objectType", conflictRecord.getObjectType());
		analysis.put("severity", conflictRecord.getSeverity());

		// 时间分析
		if (conflictRecord.getLocalUpdateTime() != null && conflictRecord.getRemoteUpdateTime() != null) {
			long timeDiff = java.time.Duration.between(
					conflictRecord.getLocalUpdateTime(),
					conflictRecord.getRemoteUpdateTime()).toMinutes();

			analysis.put("timeDifferenceMinutes", Math.abs(timeDiff));
			analysis.put("newerVersion", timeDiff > 0 ? "REMOTE" : "LOCAL");
		}

		// 数据复杂性分析
		try {
			Map<String, Object> localData = JsonUtil.fromJson(conflictRecord.getLocalData(), Map.class);
			Map<String, Object> remoteData = JsonUtil.fromJson(conflictRecord.getRemoteData(), Map.class);

			analysis.put("localDataSize", conflictRecord.getLocalData().length());
			analysis.put("remoteDataSize", conflictRecord.getRemoteData().length());
			analysis.put("dataComplexity", estimateDataComplexity(localData, remoteData));

		} catch (Exception e) {
			analysis.put("dataComplexity", "UNKNOWN");
		}

		return analysis;
	}

	private String estimateDataComplexity(Map<String, Object> localData, Map<String, Object> remoteData) {
		// 简单估计数据复杂性
		if (localData == null || remoteData == null) {
			return "UNKNOWN";
		}

		int localFields = countFields(localData);
		int remoteFields = countFields(remoteData);
		int totalFields = Math.max(localFields, remoteFields);

		if (totalFields < 10)
			return "LOW";
		if (totalFields < 50)
			return "MEDIUM";
		return "HIGH";
	}

	private int countFields(Map<String, Object> data) {
		if (data == null)
			return 0;

		int count = data.size();
		for (Object value : data.values()) {
			if (value instanceof Map) {
				count += countFields((Map<String, Object>) value);
			} else if (value instanceof List) {
				for (Object item : (List) value) {
					if (item instanceof Map) {
						count += countFields((Map<String, Object>) item);
					}
				}
			}
		}
		return count;
	}

	private void enrichConflictDetails(ConflictRecordVO vo) {
		// 添加差异分析
		if (vo.getLocalDataSummary() != null && vo.getRemoteDataSummary() != null) {
			List<ConflictRecordVO.ConflictDiffDetail> diffDetails = analyzeDifferences(
					vo.getLocalDataSummary(), vo.getRemoteDataSummary());
			vo.setDiffDetails(diffDetails);
		}

		// 添加影响分析
		ConflictRecordVO.ConflictImpactAnalysis impactAnalysis = new ConflictRecordVO.ConflictImpactAnalysis();
		impactAnalysis.setImpactScope("局部影响");
		impactAnalysis.setImpactLevel(vo.getSeverity());
		impactAnalysis.setBusinessImpact(getBusinessImpact(vo.getSeverity()));
		impactAnalysis.setConsistencyRisk(getConsistencyRisk(vo.getSeverity()));
		impactAnalysis.setEstimatedResolutionTime(getEstimatedResolutionTime(vo.getSeverity()));
		impactAnalysis.setResolutionComplexity(getResolutionComplexity(vo.getSeverity()));
		impactAnalysis.setSuggestedPriority(vo.getSeverity());

		vo.setImpactAnalysis(impactAnalysis);

		// 添加建议策略
		List<ConflictRecordVO.SuggestedResolutionStrategy> suggestedStrategies = getSuggestedStrategies(vo);
		vo.setSuggestedStrategies(suggestedStrategies);
	}

	private List<ConflictRecordVO.ConflictDiffDetail> analyzeDifferences(
			Map<String, Object> localData, Map<String, Object> remoteData) {

		List<ConflictRecordVO.ConflictDiffDetail> diffs = new ArrayList<>();

		// 简单的字段对比（实际应该使用更复杂的对比算法）
		Set<String> allKeys = new HashSet<>();
		allKeys.addAll(localData.keySet());
		allKeys.addAll(remoteData.keySet());

		for (String key : allKeys) {
			Object localValue = localData.get(key);
			Object remoteValue = remoteData.get(key);

			if (!Objects.equals(localValue, remoteValue)) {
				ConflictRecordVO.ConflictDiffDetail diff = new ConflictRecordVO.ConflictDiffDetail();
				diff.setFieldPath(key);
				diff.setFieldName(getFieldDisplayName(key));
				diff.setLocalValue(localValue);
				diff.setRemoteValue(remoteValue);
				diff.setDiffType(determineDiffType(localValue, remoteValue));
				diff.setDiffLevel(determineDiffLevel(key, localValue, remoteValue));
				diff.setConflict(true);
				diff.setDescription(generateDiffDescription(key, localValue, remoteValue));

				diffs.add(diff);
			}
		}

		return diffs;
	}

	private String determineDiffType(Object localValue, Object remoteValue) {
		if (localValue == null && remoteValue != null)
			return "ADDED";
		if (localValue != null && remoteValue == null)
			return "DELETED";
		return "MODIFIED";
	}

	private String determineDiffLevel(String field, Object oldValue, Object newValue) {
		// 根据字段重要性确定差异级别
		if (field.contains("name") || field.contains("id") || field.contains("key")) {
			return "HIGH";
		} else if (field.contains("description") || field.contains("config")) {
			return "MEDIUM";
		} else {
			return "LOW";
		}
	}

	private String generateDiffDescription(String field, Object oldValue, Object newValue) {
		return String.format("字段 '%s' 从 '%s' 修改为 '%s'",
				getFieldDisplayName(field),
				oldValue != null ? oldValue.toString() : "空",
				newValue != null ? newValue.toString() : "空");
	}

	private List<ConflictRecordVO.SuggestedResolutionStrategy> getSuggestedStrategies(ConflictRecordVO vo) {
		List<ConflictRecordVO.SuggestedResolutionStrategy> strategies = new ArrayList<>();

		// 根据冲突特征推荐策略
		ConflictRecordVO.SuggestedResolutionStrategy strategy1 = new ConflictRecordVO.SuggestedResolutionStrategy();
		strategy1.setStrategyType("TIMESTAMP_PRIORITY");
		strategy1.setStrategyName("时间戳优先");
		strategy1.setStrategyDescription("使用最新的修改版本");
		strategy1.setApplicableScenario("当时间先后关系明确时");
		strategy1.setRiskLevel("LOW");
		strategy1.setSuccessRate(95.5);
		strategy1.setRecommendationScore(4.5);
		strategy1.setAutoResolutionSupported(true);
		strategy1.setExpectedOutcome("保留最新的数据修改");
		strategies.add(strategy1);

		ConflictRecordVO.SuggestedResolutionStrategy strategy2 = new ConflictRecordVO.SuggestedResolutionStrategy();
		strategy2.setStrategyType("MANUAL");
		strategy2.setStrategyName("手动解决");
		strategy2.setStrategyDescription("手动检查和合并修改");
		strategy2.setApplicableScenario("当修改内容都比较重要时");
		strategy2.setRiskLevel("MEDIUM");
		strategy2.setSuccessRate(100.0);
		strategy2.setRecommendationScore(4.0);
		strategy2.setAutoResolutionSupported(false);
		strategy2.setExpectedOutcome("精确控制合并结果");
		strategies.add(strategy2);

		return strategies;
	}

	private String getBusinessImpact(String severity) {
		switch (severity) {
			case "CRITICAL":
				return "严重影响业务流程";
			case "HIGH":
				return "影响重要功能";
			case "MEDIUM":
				return "影响一般功能";
			case "LOW":
				return "轻微影响";
			default:
				return "未知影响";
		}
	}

	private String getConsistencyRisk(String severity) {
		switch (severity) {
			case "CRITICAL":
				return "高";
			case "HIGH":
				return "中高";
			case "MEDIUM":
				return "中";
			case "LOW":
				return "低";
			default:
				return "未知";
		}
	}

	private Integer getEstimatedResolutionTime(String severity) {
		switch (severity) {
			case "CRITICAL":
				return 120;
			case "HIGH":
				return 60;
			case "MEDIUM":
				return 30;
			case "LOW":
				return 15;
			default:
				return 30;
		}
	}

	private String getResolutionComplexity(String severity) {
		switch (severity) {
			case "CRITICAL":
				return "HIGH";
			case "HIGH":
				return "MEDIUM_HIGH";
			case "MEDIUM":
				return "MEDIUM";
			case "LOW":
				return "LOW";
			default:
				return "MEDIUM";
		}
	}

	private String getConflictTypeName(String conflictType) {
		Map<String, String> nameMap = new HashMap<>();
		nameMap.put("WORKFLOW_CONFLICT", "工作流冲突");
		nameMap.put("NODE_CONFLICT", "节点冲突");
		nameMap.put("VALIDATION_CONFLICT", "验证规则冲突");
		nameMap.put("CATEGORY_CONFLICT", "分类冲突");
		nameMap.put("DATA_CONFLICT", "数据冲突");
		nameMap.put("SYNC_CONFLICT", "同步冲突");

		return nameMap.getOrDefault(conflictType, conflictType);
	}

	private String getObjectTypeName(String objectType) {
		Map<String, String> nameMap = new HashMap<>();
		nameMap.put("WORKFLOW", "工作流");
		nameMap.put("NODE", "节点");
		nameMap.put("VALIDATION", "验证规则");
		nameMap.put("CATEGORY", "分类");
		nameMap.put("USER", "用户");
		nameMap.put("ROLE", "角色");

		return nameMap.getOrDefault(objectType, objectType);
	}

	private String getStatusName(String status) {
		Map<String, String> nameMap = new HashMap<>();
		nameMap.put("PENDING", "待解决");
		nameMap.put("RESOLVED", "已解决");
		nameMap.put("IGNORED", "已忽略");
		nameMap.put("AUTO_RESOLVED", "自动解决");

		return nameMap.getOrDefault(status, status);
	}

	private String getResolutionStrategyName(String strategy) {
		if (strategy == null)
			return "";

		Map<String, String> nameMap = new HashMap<>();
		nameMap.put("CLIENT_PRIORITY", "客户端优先");
		nameMap.put("SERVER_PRIORITY", "服务器优先");
		nameMap.put("TIMESTAMP_PRIORITY", "时间戳优先");
		nameMap.put("MANUAL", "手动解决");
		nameMap.put("MERGE", "合并");
		nameMap.put("IGNORED", "已忽略");

		return nameMap.getOrDefault(strategy, strategy);
	}

	private String getSeverityName(String severity) {
		Map<String, String> nameMap = new HashMap<>();
		nameMap.put("CRITICAL", "严重");
		nameMap.put("HIGH", "高");
		nameMap.put("MEDIUM", "中");
		nameMap.put("LOW", "低");

		return nameMap.getOrDefault(severity, severity);
	}

	private String getFieldDisplayName(String field) {
		Map<String, String> nameMap = new HashMap<>();
		nameMap.put("name", "名称");
		nameMap.put("description", "描述");
		nameMap.put("config", "配置");
		nameMap.put("status", "状态");
		nameMap.put("type", "类型");
		nameMap.put("version", "版本");

		return nameMap.getOrDefault(field, field);
	}

	@Async
	protected void sendConflictNotification(ConflictRecordEntity conflictRecord) {
		try {
			Map<String, Object> notificationData = new HashMap<>();
			notificationData.put("conflictId", conflictRecord.getId());
			notificationData.put("objectType", conflictRecord.getObjectType());
			notificationData.put("objectName", conflictRecord.getObjectName());
			notificationData.put("severity", conflictRecord.getSeverity());
			notificationData.put("detectedTime", conflictRecord.getDetectedTime());

			notificationService.sendConflictNotification(notificationData);

			// 标记为已通知
			markAsNotified(conflictRecord.getId());

		} catch (Exception e) {
			log.error("发送冲突通知失败", e);
		}
	}

	@Async
	private void sendResolutionNotification(ConflictRecordEntity conflictRecord) {
		try {
			Map<String, Object> notificationData = new HashMap<>();
			notificationData.put("conflictId", conflictRecord.getId());
			notificationData.put("objectType", conflictRecord.getObjectType());
			notificationData.put("objectName", conflictRecord.getObjectName());
			notificationData.put("resolutionStrategy", conflictRecord.getResolutionStrategy());
			notificationData.put("resolvedBy", conflictRecord.getResolvedBy());
			notificationData.put("resolvedTime", conflictRecord.getResolvedTime());

			notificationService.sendResolutionNotification(notificationData);

		} catch (Exception e) {
			log.error("发送解决通知失败", e);
		}
	}

	private void performPostResolutionActions(ConflictRecordEntity conflictRecord) {
		// 执行解决后的操作，如清理缓存、重新同步等
		log.debug("执行冲突解决后操作，冲突ID: {}", conflictRecord.getId());

		// 这里可以添加具体的后处理逻辑
		// 例如：清理相关缓存、触发数据同步、更新统计信息等
	}
}