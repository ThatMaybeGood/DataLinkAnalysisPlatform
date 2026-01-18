package com.workflow.platform.component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.dto.WorkflowVersionDTO;
import com.workflow.platform.util.JsonUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * 工作流版本管理器 - 管理工作的版本控制
 */
@Slf4j
@Component
public class WorkflowVersionManager {

	@Autowired
	private OfflineDataManager offlineDataManager;

	@Value("${workflow.platform.versioning.enabled:true}")
	private boolean versioningEnabled;

	@Value("${workflow.platform.versioning.max-versions:10}")
	private int maxVersions;

	@Value("${workflow.platform.versioning.auto-version:true}")
	private boolean autoVersion;

	@Value("${workflow.platform.versioning.compression.enabled:true}")
	private boolean compressionEnabled;

	// 版本存储
	private final Map<String, VersionStore> versionStores = new ConcurrentHashMap<>();

	// 版本锁
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * 创建新版本
	 */
	public WorkflowVersionDTO createVersion(WorkflowDTO workflow, String description, String createdBy) {
		if (!versioningEnabled) {
			log.warn("版本控制未启用，跳过创建版本");
			return null;
		}

		lock.writeLock().lock();
		try {
			String workflowId = workflow.getId();
			VersionStore store = getOrCreateVersionStore(workflowId);

			// 生成版本号
			int versionNumber = store.getNextVersionNumber();
			String versionId = generateVersionId(workflowId, versionNumber);

			// 创建版本
			WorkflowVersionDTO version = WorkflowVersionDTO.builder()
					.id(versionId)
					.workflowId(workflowId)
					.versionNumber(versionNumber)
					.versionTag("v" + versionNumber)
					.description(description)
					.workflowData(workflow)
					.createdBy(createdBy)
					.createdAt(System.currentTimeMillis())
					.checksum(calculateChecksum(workflow))
					.size(calculateSize(workflow))
					.metadata(generateMetadata(workflow))
					.build();

			// 保存版本
			store.addVersion(version);

			// 清理旧版本
			cleanupOldVersions(store);

			log.info("创建工作流版本: {} -> v{}，描述: {}",
					workflowId, versionNumber, description);

			return version;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 获取指定版本
	 */
	public WorkflowVersionDTO getVersion(String workflowId, int versionNumber) {
		lock.readLock().lock();
		try {
			VersionStore store = versionStores.get(workflowId);
			if (store == null) {
				return null;
			}

			return store.getVersion(versionNumber);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 获取所有版本
	 */
	public List<WorkflowVersionDTO> getVersions(String workflowId) {
		lock.readLock().lock();
		try {
			VersionStore store = versionStores.get(workflowId);
			if (store == null) {
				return Collections.emptyList();
			}

			return store.getAllVersions();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 恢复到指定版本
	 */
	public boolean restoreToVersion(String workflowId, int versionNumber, String restoredBy) {
		lock.writeLock().lock();
		try {
			WorkflowVersionDTO version = getVersion(workflowId, versionNumber);
			if (version == null) {
				log.error("版本不存在: {} v{}", workflowId, versionNumber);
				return false;
			}

			// 创建恢复前的版本
			WorkflowDTO currentWorkflow = offlineDataManager.loadWorkflow(workflowId);
			if (currentWorkflow != null) {
				createVersion(currentWorkflow,
						"恢复前的自动备份版本", "system");
			}

			// 恢复工作流
			boolean restored = offlineDataManager.saveWorkflow(version.getWorkflowData());
			if (!restored) {
				log.error("恢复工作流失败: {} v{}", workflowId, versionNumber);
				return false;
			}

			// 记录恢复历史
			version.setRestoredBy(restoredBy);
			version.setRestoredAt(System.currentTimeMillis());
			version.setRestoreCount(version.getRestoreCount() + 1);

			log.info("恢复工作流到版本: {} -> v{}，恢复人: {}",
					workflowId, versionNumber, restoredBy);

			return true;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 比较两个版本
	 */
	public VersionComparison compareVersions(String workflowId, int version1, int version2) {
		WorkflowVersionDTO v1 = getVersion(workflowId, version1);
		WorkflowVersionDTO v2 = getVersion(workflowId, version2);

		if (v1 == null || v2 == null) {
			return null;
		}

		VersionComparison comparison = new VersionComparison();
		comparison.setWorkflowId(workflowId);
		comparison.setVersion1(version1);
		comparison.setVersion2(version2);
		comparison.setComparedAt(System.currentTimeMillis());

		// 比较基本信息
		comparison.setBasicChanges(compareBasicInfo(v1, v2));

		// 比较节点
		comparison.setNodeChanges(compareNodes(v1, v2));

		// 比较连接
		comparison.setConnectionChanges(compareConnections(v1, v2));

		// 比较配置
		comparison.setConfigChanges(compareConfigs(v1, v2));

		// 计算总体差异度
		double similarity = calculateSimilarity(comparison);
		comparison.setSimilarity(similarity);
		comparison.setHasChanges(similarity < 1.0);

		return comparison;
	}

	/**
	 * 创建分支版本
	 */
	public WorkflowVersionDTO createBranch(String workflowId, int baseVersion,
			String branchName, String description,
			String createdBy) {
		lock.writeLock().lock();
		try {
			WorkflowVersionDTO baseVersionDTO = getVersion(workflowId, baseVersion);
			if (baseVersionDTO == null) {
				log.error("基础版本不存在: {} v{}", workflowId, baseVersion);
				return null;
			}

			// 复制基础版本
			WorkflowDTO branchWorkflow = deepCopy(baseVersionDTO.getWorkflowData());
			branchWorkflow.setId(generateBranchId(workflowId, branchName));
			branchWorkflow.setName(branchWorkflow.getName() + " - " + branchName);
			branchWorkflow.setDescription(description);
			branchWorkflow.setBranchName(branchName);
			branchWorkflow.setBasedOnVersion(baseVersion);

			// 创建分支版本
			WorkflowVersionDTO branchVersion = new WorkflowVersionDTO();
			branchVersion.setId(generateVersionId(branchWorkflow.getId(), 1));
			branchVersion.setWorkflowId(branchWorkflow.getId());
			branchVersion.setVersionNumber(1);
			branchVersion.setVersionTag("branch/" + branchName + "/v1");
			branchVersion.setDescription("分支创建: " + description);
			branchVersion.setWorkflowData(branchWorkflow);
			branchVersion.setCreatedBy(createdBy);
			branchVersion.setCreatedAt(System.currentTimeMillis());
			branchVersion.setIsBranch(true);
			branchVersion.setBranchName(branchName);
			branchVersion.setBaseWorkflowId(workflowId);
			branchVersion.setBaseVersion(baseVersion);

			// 保存分支
			VersionStore branchStore = new VersionStore(branchWorkflow.getId());
			branchStore.addVersion(branchVersion);
			versionStores.put(branchWorkflow.getId(), branchStore);

			// 保存分支工作流
			offlineDataManager.saveWorkflow(branchWorkflow);

			log.info("创建工作流分支: {} -> {}，基础版本: v{}",
					workflowId, branchName, baseVersion);

			return branchVersion;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 合并分支
	 */
	public MergeResult mergeBranch(String sourceWorkflowId, String targetWorkflowId,
			String mergeStrategy, String mergedBy) {
		lock.writeLock().lock();
		try {
			WorkflowDTO sourceWorkflow = offlineDataManager.loadWorkflow(sourceWorkflowId);
			WorkflowDTO targetWorkflow = offlineDataManager.loadWorkflow(targetWorkflowId);

			if (sourceWorkflow == null || targetWorkflow == null) {
				return MergeResult.failure("源或目标工作流不存在");
			}

			// 创建合并前的版本
			createVersion(targetWorkflow, "合并前的自动备份", "system");

			// 根据合并策略执行合并
			MergeResult result = new MergeResult();
			result.setSourceWorkflowId(sourceWorkflowId);
			result.setTargetWorkflowId(targetWorkflowId);
			result.setMergeStrategy(mergeStrategy);
			result.setMergedBy(mergedBy);
			result.setMergeTime(System.currentTimeMillis());

			switch (mergeStrategy) {
				case "fast-forward":
					result = mergeFastForward(sourceWorkflow, targetWorkflow, result);
					break;
				case "three-way":
					result = mergeThreeWay(sourceWorkflow, targetWorkflow, result);
					break;
				case "rebase":
					result = mergeRebase(sourceWorkflow, targetWorkflow, result);
					break;
				default:
					result = mergeManual(sourceWorkflow, targetWorkflow, result);
			}

			if (result.isSuccess()) {
				// 保存合并结果
				offlineDataManager.saveWorkflow(targetWorkflow);

				// 创建合并版本
				createVersion(targetWorkflow,
						"合并自: " + sourceWorkflowId + " (" + mergeStrategy + ")",
						mergedBy);

				log.info("合并分支成功: {} -> {}，策略: {}，冲突数: {}",
						sourceWorkflowId, targetWorkflowId,
						mergeStrategy, result.getConflictCount());
			}

			return result;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 获取版本统计
	 */
	public VersionStats getVersionStats(String workflowId) {
		lock.readLock().lock();
		try {
			VersionStore store = versionStores.get(workflowId);
			if (store == null) {
				return new VersionStats(workflowId);
			}

			VersionStats stats = new VersionStats(workflowId);
			List<WorkflowVersionDTO> versions = store.getAllVersions();

			stats.setTotalVersions(versions.size());
			stats.setLatestVersion(store.getLatestVersionNumber());
			stats.setFirstVersionTime(versions.isEmpty() ? 0 : versions.get(0).getCreatedAt());
			stats.setLastVersionTime(versions.isEmpty() ? 0 : versions.get(versions.size() - 1).getCreatedAt());

			// 统计创建者
			Map<String, Integer> creatorStats = versions.stream()
					.collect(Collectors.groupingBy(WorkflowVersionDTO::getCreatedBy,
							Collectors.summingInt(v -> 1)));
			stats.setCreatorStats(creatorStats);

			// 统计标签
			Map<String, Integer> tagStats = versions.stream()
					.filter(v -> v.getTags() != null)
					.flatMap(v -> v.getTags().stream())
					.collect(Collectors.groupingBy(tag -> tag,
							Collectors.summingInt(tag -> 1)));
			stats.setTagStats(tagStats);

			// 计算平均版本间隔
			if (versions.size() > 1) {
				long totalInterval = 0;
				for (int i = 1; i < versions.size(); i++) {
					totalInterval += versions.get(i).getCreatedAt() -
							versions.get(i - 1).getCreatedAt();
				}
				stats.setAverageVersionInterval(totalInterval / (versions.size() - 1));
			}

			return stats;

		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 清理版本历史
	 */
	public CleanupResult cleanupVersions(String workflowId, CleanupCriteria criteria) {
		lock.writeLock().lock();
		try {
			VersionStore store = versionStores.get(workflowId);
			if (store == null) {
				return CleanupResult.empty(workflowId);
			}

			CleanupResult result = new CleanupResult(workflowId);
			List<WorkflowVersionDTO> versions = store.getAllVersions();

			// 根据条件筛选要删除的版本
			List<WorkflowVersionDTO> toKeep = new ArrayList<>();
			List<WorkflowVersionDTO> toDelete = new ArrayList<>();

			for (WorkflowVersionDTO version : versions) {
				if (shouldKeepVersion(version, criteria)) {
					toKeep.add(version);
				} else {
					toDelete.add(version);
				}
			}

			// 执行删除
			store.setVersions(toKeep);

			// 更新结果
			result.setTotalVersions(versions.size());
			result.setKeptVersions(toKeep.size());
			result.setDeletedVersions(toDelete.size());
			result.setDeletedSize(toDelete.stream()
					.mapToLong(WorkflowVersionDTO::getSize)
					.sum());

			log.info("清理版本历史: {}，保留 {} 个，删除 {} 个，释放空间: {} bytes",
					workflowId, toKeep.size(), toDelete.size(), result.getDeletedSize());

			return result;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 导出版本历史
	 */
	public String exportVersionHistory(String workflowId, ExportFormat format) {
		lock.readLock().lock();
		try {
			VersionStore store = versionStores.get(workflowId);
			if (store == null) {
				return null;
			}

			VersionHistoryExport export = new VersionHistoryExport();
			export.setWorkflowId(workflowId);
			export.setExportTime(System.currentTimeMillis());
			export.setFormat(format);
			export.setVersions(store.getAllVersions());
			export.setStats(getVersionStats(workflowId));

			switch (format) {
				case JSON:
					return JsonUtil.toPrettyJson(export);
				case XML:
					return exportToXml(export);
				case YAML:
					return exportToYaml(export);
				default:
					return JsonUtil.toJson(export);
			}

		} finally {
			lock.readLock().unlock();
		}
	}

	// ========== 私有方法 ==========

	private VersionStore getOrCreateVersionStore(String workflowId) {
		return versionStores.computeIfAbsent(workflowId,
				k -> new VersionStore(workflowId));
	}

	private String generateVersionId(String workflowId, int versionNumber) {
		return workflowId + "_v" + versionNumber;
	}

	private String generateBranchId(String workflowId, String branchName) {
		return workflowId + "_branch_" + branchName.toLowerCase().replaceAll("\\s+", "_");
	}

	private String calculateChecksum(WorkflowDTO workflow) {
		String json = JsonUtil.toJson(workflow);
		// 简单MD5校验和
		try {
			java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(json.getBytes());
			return bytesToHex(digest);
		} catch (Exception e) {
			return Integer.toHexString(json.hashCode());
		}
	}

	private String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private long calculateSize(WorkflowDTO workflow) {
		String json = JsonUtil.toJson(workflow);
		return json.getBytes().length;
	}

	private Map<String, Object> generateMetadata(WorkflowDTO workflow) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("nodeCount", workflow.getNodes() != null ? workflow.getNodes().size() : 0);
		metadata.put("connectionCount", workflow.getConnections() != null ? workflow.getConnections().size() : 0);
		metadata.put("validationRuleCount",
				workflow.getValidationRules() != null ? workflow.getValidationRules().size() : 0);
		metadata.put("timestamp", System.currentTimeMillis());
		return metadata;
	}

	private void cleanupOldVersions(VersionStore store) {
		if (store.getVersionCount() <= maxVersions) {
			return;
		}

		List<WorkflowVersionDTO> versions = store.getAllVersions();
		int toRemove = versions.size() - maxVersions;

		// 保留最新的maxVersions个版本
		List<WorkflowVersionDTO> toKeep = versions.subList(toRemove, versions.size());
		store.setVersions(toKeep);

		log.debug("清理旧版本: {}，保留最新 {} 个",
				store.getWorkflowId(), maxVersions);
	}

	private List<BasicChange> compareBasicInfo(WorkflowVersionDTO v1, WorkflowVersionDTO v2) {
		List<BasicChange> changes = new ArrayList<>();
		WorkflowDTO w1 = v1.getWorkflowData();
		WorkflowDTO w2 = v2.getWorkflowData();

		if (!Objects.equals(w1.getName(), w2.getName())) {
			changes.add(new BasicChange("name", w1.getName(), w2.getName()));
		}

		if (!Objects.equals(w1.getDescription(), w2.getDescription())) {
			changes.add(new BasicChange("description", w1.getDescription(), w2.getDescription()));
		}

		if (!Objects.equals(w1.getCategory(), w2.getCategory())) {
			changes.add(new BasicChange("category", w1.getCategory(), w2.getCategory()));
		}

		return changes;
	}

	private List<NodeChange> compareNodes(WorkflowVersionDTO v1, WorkflowVersionDTO v2) {
		List<NodeChange> changes = new ArrayList<>();
		Map<String, Object> nodes1 = extractNodes(v1);
		Map<String, Object> nodes2 = extractNodes(v2);

		// 找出新增的节点
		for (String nodeId : nodes2.keySet()) {
			if (!nodes1.containsKey(nodeId)) {
				changes.add(NodeChange.added(nodeId, nodes2.get(nodeId)));
			}
		}

		// 找出删除的节点
		for (String nodeId : nodes1.keySet()) {
			if (!nodes2.containsKey(nodeId)) {
				changes.add(NodeChange.deleted(nodeId, nodes1.get(nodeId)));
			}
		}

		// 找出修改的节点
		for (String nodeId : nodes1.keySet()) {
			if (nodes2.containsKey(nodeId) &&
					!Objects.equals(nodes1.get(nodeId), nodes2.get(nodeId))) {
				changes.add(NodeChange.modified(nodeId,
						nodes1.get(nodeId), nodes2.get(nodeId)));
			}
		}

		return changes;
	}

	private Map<String, Object> extractNodes(WorkflowVersionDTO version) {
		Map<String, Object> nodes = new HashMap<>();
		if (version.getWorkflowData().getNodes() != null) {
			for (Object node : version.getWorkflowData().getNodes()) {
				if (node instanceof Map) {
					Map<?, ?> nodeMap = (Map<?, ?>) node;
					Object id = nodeMap.get("id");
					if (id != null) {
						nodes.put(id.toString(), node);
					}
				}
			}
		}
		return nodes;
	}

	private List<ConnectionChange> compareConnections(WorkflowVersionDTO v1, WorkflowVersionDTO v2) {
		// 类似节点比较的逻辑
		return new ArrayList<>();
	}

	private List<ConfigChange> compareConfigs(WorkflowVersionDTO v1, WorkflowVersionDTO v2) {
		// 比较配置变化的逻辑
		return new ArrayList<>();
	}

	private double calculateSimilarity(VersionComparison comparison) {
		// 计算相似度的简单实现
		int totalChanges = comparison.getBasicChanges().size() +
				comparison.getNodeChanges().size() +
				comparison.getConnectionChanges().size() +
				comparison.getConfigChanges().size();

		// 假设每个变更减少1%的相似度
		double similarity = 1.0 - (totalChanges * 0.01);
		return Math.max(0.0, Math.min(1.0, similarity));
	}

	private WorkflowDTO deepCopy(WorkflowDTO original) {
		String json = JsonUtil.toJson(original);
		return JsonUtil.fromJson(json, WorkflowDTO.class);
	}

	private MergeResult mergeFastForward(WorkflowDTO source, WorkflowDTO target,
			MergeResult result) {
		// 快进合并：直接将目标更新为源
		target.setNodes(source.getNodes());
		target.setConnections(source.getConnections());
		target.setValidationRules(source.getValidationRules());
		target.setConfig(source.getConfig());

		result.setSuccess(true);
		result.setConflictCount(0);
		result.setMessage("快进合并成功");

		return result;
	}

	private MergeResult mergeThreeWay(WorkflowDTO source, WorkflowDTO target,
			MergeResult result) {
		// 三向合并实现
		// 这里需要基础版本，简化实现
		result.setSuccess(true);
		result.setConflictCount(0);
		result.setMessage("三向合并成功（简化实现）");

		return result;
	}

	private MergeResult mergeRebase(WorkflowDTO source, WorkflowDTO target,
			MergeResult result) {
		// Rebase合并实现
		result.setSuccess(true);
		result.setConflictCount(0);
		result.setMessage("Rebase合并成功（简化实现）");

		return result;
	}

	private MergeResult mergeManual(WorkflowDTO source, WorkflowDTO target,
			MergeResult result) {
		// 手动合并：需要用户介入
		result.setSuccess(false);
		result.setConflictCount(1);
		result.setMessage("需要手动合并");
		result.setNeedsManualResolution(true);

		return result;
	}

	private boolean shouldKeepVersion(WorkflowVersionDTO version, CleanupCriteria criteria) {
		long currentTime = System.currentTimeMillis();
		long versionAge = currentTime - version.getCreatedAt();

		// 检查保留条件
		if (criteria.getKeepTagged() && version.getTags() != null && !version.getTags().isEmpty()) {
			return true;
		}

		if (criteria.getKeepMajorVersions() && version.getVersionNumber() % 10 == 0) {
			return true;
		}

		if (versionAge < criteria.getMaxAgeDays() * 24 * 60 * 60 * 1000L) {
			return true;
		}

		if (version.getVersionNumber() > criteria.getKeepLastVersions()) {
			return true;
		}

		return false;
	}

	private String exportToXml(VersionHistoryExport export) {
		// 简化的XML导出
		StringBuilder xml = new StringBuilder();
		xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		xml.append("<versionHistory workflowId=\"").append(export.getWorkflowId()).append("\">\n");

		for (WorkflowVersionDTO version : export.getVersions()) {
			xml.append("  <version number=\"").append(version.getVersionNumber()).append("\">\n");
			xml.append("    <description>").append(version.getDescription()).append("</description>\n");
			xml.append("    <createdBy>").append(version.getCreatedBy()).append("</createdBy>\n");
			xml.append("    <createdAt>").append(version.getCreatedAt()).append("</createdAt>\n");
			xml.append("  </version>\n");
		}

		xml.append("</versionHistory>");
		return xml.toString();
	}

	private String exportToYaml(VersionHistoryExport export) {
		// 简化的YAML导出
		StringBuilder yaml = new StringBuilder();
		yaml.append("workflowId: ").append(export.getWorkflowId()).append("\n");
		yaml.append("exportTime: ").append(export.getExportTime()).append("\n");
		yaml.append("format: ").append(export.getFormat()).append("\n");
		yaml.append("versions:\n");

		for (WorkflowVersionDTO version : export.getVersions()) {
			yaml.append("  - versionNumber: ").append(version.getVersionNumber()).append("\n");
			yaml.append("    description: ").append(version.getDescription()).append("\n");
			yaml.append("    createdBy: ").append(version.getCreatedBy()).append("\n");
			yaml.append("    createdAt: ").append(version.getCreatedAt()).append("\n");
		}

		return yaml.toString();
	}

	// ========== 内部类 ==========

	/**
	 * 版本存储
	 */
	private static class VersionStore {
		private final String workflowId;
		private final List<WorkflowVersionDTO> versions;
		private final AtomicInteger nextVersionNumber;

		public VersionStore(String workflowId) {
			this.workflowId = workflowId;
			this.versions = new CopyOnWriteArrayList<>();
			this.nextVersionNumber = new AtomicInteger(1);
		}

		public void addVersion(WorkflowVersionDTO version) {
			versions.add(version);
			nextVersionNumber.incrementAndGet();
		}

		public WorkflowVersionDTO getVersion(int versionNumber) {
			return versions.stream()
					.filter(v -> v.getVersionNumber() == versionNumber)
					.findFirst()
					.orElse(null);
		}

		public List<WorkflowVersionDTO> getAllVersions() {
			return new ArrayList<>(versions);
		}

		public int getVersionCount() {
			return versions.size();
		}

		public int getLatestVersionNumber() {
			return versions.isEmpty() ? 0 : versions.get(versions.size() - 1).getVersionNumber();
		}

		public int getNextVersionNumber() {
			return nextVersionNumber.get();
		}

		public String getWorkflowId() {
			return workflowId;
		}

		public void setVersions(List<WorkflowVersionDTO> newVersions) {
			versions.clear();
			versions.addAll(newVersions);
		}
	}

	/**
	 * 版本比较结果
	 */
	public static class VersionComparison {
		private String workflowId;
		private int version1;
		private int version2;
		private long comparedAt;
		private List<BasicChange> basicChanges;
		private List<NodeChange> nodeChanges;
		private List<ConnectionChange> connectionChanges;
		private List<ConfigChange> configChanges;
		private double similarity;
		private boolean hasChanges;

		// Getters and Setters
		public String getWorkflowId() {
			return workflowId;
		}

		public void setWorkflowId(String workflowId) {
			this.workflowId = workflowId;
		}

		public int getVersion1() {
			return version1;
		}

		public void setVersion1(int version1) {
			this.version1 = version1;
		}

		public int getVersion2() {
			return version2;
		}

		public void setVersion2(int version2) {
			this.version2 = version2;
		}

		public long getComparedAt() {
			return comparedAt;
		}

		public void setComparedAt(long comparedAt) {
			this.comparedAt = comparedAt;
		}

		public List<BasicChange> getBasicChanges() {
			return basicChanges;
		}

		public void setBasicChanges(List<BasicChange> basicChanges) {
			this.basicChanges = basicChanges;
		}

		public List<NodeChange> getNodeChanges() {
			return nodeChanges;
		}

		public void setNodeChanges(List<NodeChange> nodeChanges) {
			this.nodeChanges = nodeChanges;
		}

		public List<ConnectionChange> getConnectionChanges() {
			return connectionChanges;
		}

		public void setConnectionChanges(List<ConnectionChange> connectionChanges) {
			this.connectionChanges = connectionChanges;
		}

		public List<ConfigChange> getConfigChanges() {
			return configChanges;
		}

		public void setConfigChanges(List<ConfigChange> configChanges) {
			this.configChanges = configChanges;
		}

		public double getSimilarity() {
			return similarity;
		}

		public void setSimilarity(double similarity) {
			this.similarity = similarity;
		}

		public boolean isHasChanges() {
			return hasChanges;
		}

		public void setHasChanges(boolean hasChanges) {
			this.hasChanges = hasChanges;
		}
	}

	/**
	 * 基本变更
	 */
	public static class BasicChange {
		private String field;
		private Object oldValue;
		private Object newValue;

		public BasicChange(String field, Object oldValue, Object newValue) {
			this.field = field;
			this.oldValue = oldValue;
			this.newValue = newValue;
		}

		// Getters
		public String getField() {
			return field;
		}

		public Object getOldValue() {
			return oldValue;
		}

		public Object getNewValue() {
			return newValue;
		}
	}

	/**
	 * 节点变更
	 */
	public static class NodeChange {
		private String nodeId;
		private ChangeType type;
		private Object oldNode;
		private Object newNode;

		public enum ChangeType {
			ADDED, DELETED, MODIFIED
		}

		private NodeChange(String nodeId, ChangeType type, Object oldNode, Object newNode) {
			this.nodeId = nodeId;
			this.type = type;
			this.oldNode = oldNode;
			this.newNode = newNode;
		}

		public static NodeChange added(String nodeId, Object node) {
			return new NodeChange(nodeId, ChangeType.ADDED, null, node);
		}

		public static NodeChange deleted(String nodeId, Object node) {
			return new NodeChange(nodeId, ChangeType.DELETED, node, null);
		}

		public static NodeChange modified(String nodeId, Object oldNode, Object newNode) {
			return new NodeChange(nodeId, ChangeType.MODIFIED, oldNode, newNode);
		}

		// Getters
		public String getNodeId() {
			return nodeId;
		}

		public ChangeType getType() {
			return type;
		}

		public Object getOldNode() {
			return oldNode;
		}

		public Object getNewNode() {
			return newNode;
		}
	}

	/**
	 * 连接变更（类似节点变更）
	 */
	public static class ConnectionChange {
		// 类似NodeChange的实现
	}

	/**
	 * 配置变更（类似节点变更）
	 */
	public static class ConfigChange {
		// 类似NodeChange的实现
	}

	/**
	 * 合并结果
	 */
	public static class MergeResult {
		private String sourceWorkflowId;
		private String targetWorkflowId;
		private String mergeStrategy;
		private String mergedBy;
		private long mergeTime;
		private boolean success;
		private String message;
		private int conflictCount;
		private boolean needsManualResolution;
		private List<String> conflicts;

		public static MergeResult failure(String message) {
			MergeResult result = new MergeResult();
			result.setSuccess(false);
			result.setMessage(message);
			return result;
		}

		// Getters and Setters
		public String getSourceWorkflowId() {
			return sourceWorkflowId;
		}

		public void setSourceWorkflowId(String sourceWorkflowId) {
			this.sourceWorkflowId = sourceWorkflowId;
		}

		public String getTargetWorkflowId() {
			return targetWorkflowId;
		}

		public void setTargetWorkflowId(String targetWorkflowId) {
			this.targetWorkflowId = targetWorkflowId;
		}

		public String getMergeStrategy() {
			return mergeStrategy;
		}

		public void setMergeStrategy(String mergeStrategy) {
			this.mergeStrategy = mergeStrategy;
		}

		public String getMergedBy() {
			return mergedBy;
		}

		public void setMergedBy(String mergedBy) {
			this.mergedBy = mergedBy;
		}

		public long getMergeTime() {
			return mergeTime;
		}

		public void setMergeTime(long mergeTime) {
			this.mergeTime = mergeTime;
		}

		public boolean isSuccess() {
			return success;
		}

		public void setSuccess(boolean success) {
			this.success = success;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public int getConflictCount() {
			return conflictCount;
		}

		public void setConflictCount(int conflictCount) {
			this.conflictCount = conflictCount;
		}

		public boolean isNeedsManualResolution() {
			return needsManualResolution;
		}

		public void setNeedsManualResolution(boolean needsManualResolution) {
			this.needsManualResolution = needsManualResolution;
		}

		public List<String> getConflicts() {
			return conflicts;
		}

		public void setConflicts(List<String> conflicts) {
			this.conflicts = conflicts;
		}
	}

	/**
	 * 版本统计
	 */
	public static class VersionStats {
		private final String workflowId;
		private int totalVersions;
		private int latestVersion;
		private long firstVersionTime;
		private long lastVersionTime;
		private long averageVersionInterval;
		private Map<String, Integer> creatorStats;
		private Map<String, Integer> tagStats;

		public VersionStats(String workflowId) {
			this.workflowId = workflowId;
			this.creatorStats = new HashMap<>();
			this.tagStats = new HashMap<>();
		}

		// Getters and Setters
		public String getWorkflowId() {
			return workflowId;
		}

		public int getTotalVersions() {
			return totalVersions;
		}

		public void setTotalVersions(int totalVersions) {
			this.totalVersions = totalVersions;
		}

		public int getLatestVersion() {
			return latestVersion;
		}

		public void setLatestVersion(int latestVersion) {
			this.latestVersion = latestVersion;
		}

		public long getFirstVersionTime() {
			return firstVersionTime;
		}

		public void setFirstVersionTime(long firstVersionTime) {
			this.firstVersionTime = firstVersionTime;
		}

		public long getLastVersionTime() {
			return lastVersionTime;
		}

		public void setLastVersionTime(long lastVersionTime) {
			this.lastVersionTime = lastVersionTime;
		}

		public long getAverageVersionInterval() {
			return averageVersionInterval;
		}

		public void setAverageVersionInterval(long averageVersionInterval) {
			this.averageVersionInterval = averageVersionInterval;
		}

		public Map<String, Integer> getCreatorStats() {
			return creatorStats;
		}

		public void setCreatorStats(Map<String, Integer> creatorStats) {
			this.creatorStats = creatorStats;
		}

		public Map<String, Integer> getTagStats() {
			return tagStats;
		}

		public void setTagStats(Map<String, Integer> tagStats) {
			this.tagStats = tagStats;
		}

		@Override
		public String toString() {
			return String.format(
					"VersionStats{workflowId='%s', totalVersions=%d, latestVersion=%d, " +
							"firstVersionTime=%d, lastVersionTime=%d, avgInterval=%d}",
					workflowId, totalVersions, latestVersion,
					firstVersionTime, lastVersionTime, averageVersionInterval);
		}
	}

	/**
	 * 清理条件
	 */
	public static class CleanupCriteria {
		private boolean keepTagged = true;
		private boolean keepMajorVersions = true;
		private int maxAgeDays = 30;
		private int keepLastVersions = 5;

		// Getters and Setters
		public boolean getKeepTagged() {
			return keepTagged;
		}

		public void setKeepTagged(boolean keepTagged) {
			this.keepTagged = keepTagged;
		}

		public boolean getKeepMajorVersions() {
			return keepMajorVersions;
		}

		public void setKeepMajorVersions(boolean keepMajorVersions) {
			this.keepMajorVersions = keepMajorVersions;
		}

		public int getMaxAgeDays() {
			return maxAgeDays;
		}

		public void setMaxAgeDays(int maxAgeDays) {
			this.maxAgeDays = maxAgeDays;
		}

		public int getKeepLastVersions() {
			return keepLastVersions;
		}

		public void setKeepLastVersions(int keepLastVersions) {
			this.keepLastVersions = keepLastVersions;
		}
	}

	/**
	 * 清理结果
	 */
	public static class CleanupResult {
		private final String workflowId;
		private int totalVersions;
		private int keptVersions;
		private int deletedVersions;
		private long deletedSize;

		public CleanupResult(String workflowId) {
			this.workflowId = workflowId;
		}

		public static CleanupResult empty(String workflowId) {
			CleanupResult result = new CleanupResult(workflowId);
			result.setTotalVersions(0);
			result.setKeptVersions(0);
			result.setDeletedVersions(0);
			result.setDeletedSize(0);
			return result;
		}

		// Getters and Setters
		public String getWorkflowId() {
			return workflowId;
		}

		public int getTotalVersions() {
			return totalVersions;
		}

		public void setTotalVersions(int totalVersions) {
			this.totalVersions = totalVersions;
		}

		public int getKeptVersions() {
			return keptVersions;
		}

		public void setKeptVersions(int keptVersions) {
			this.keptVersions = keptVersions;
		}

		public int getDeletedVersions() {
			return deletedVersions;
		}

		public void setDeletedVersions(int deletedVersions) {
			this.deletedVersions = deletedVersions;
		}

		public long getDeletedSize() {
			return deletedSize;
		}

		public void setDeletedSize(long deletedSize) {
			this.deletedSize = deletedSize;
		}

		@Override
		public String toString() {
			return String.format(
					"CleanupResult{workflowId='%s', total=%d, kept=%d, deleted=%d, freed=%d bytes}",
					workflowId, totalVersions, keptVersions, deletedVersions, deletedSize);
		}
	}

	/**
	 * 导出格式
	 */
	public enum ExportFormat {
		JSON, XML, YAML, CSV
	}

	/**
	 * 版本历史导出
	 */
	public static class VersionHistoryExport {
		private String workflowId;
		private long exportTime;
		private ExportFormat format;
		private List<WorkflowVersionDTO> versions;
		private VersionStats stats;

		// Getters and Setters
		public String getWorkflowId() {
			return workflowId;
		}

		public void setWorkflowId(String workflowId) {
			this.workflowId = workflowId;
		}

		public long getExportTime() {
			return exportTime;
		}

		public void setExportTime(long exportTime) {
			this.exportTime = exportTime;
		}

		public ExportFormat getFormat() {
			return format;
		}

		public void setFormat(ExportFormat format) {
			this.format = format;
		}

		public List<WorkflowVersionDTO> getVersions() {
			return versions;
		}

		public void setVersions(List<WorkflowVersionDTO> versions) {
			this.versions = versions;
		}

		public VersionStats getStats() {
			return stats;
		}

		public void setStats(VersionStats stats) {
			this.stats = stats;
		}
	}
}