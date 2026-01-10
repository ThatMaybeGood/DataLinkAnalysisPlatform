package com.workflow.platform.component;

import com.workflow.platform.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 冲突解决策略管理器
 */
@Slf4j
@Component
public class ConflictResolutionStrategy {

    /**
     * 时间戳策略 - 使用最新版本
     */
    public Map<String, Object> resolveByTimestamp(Map<String, Object> localData,
                                                  Map<String, Object> remoteData) {
        long localTimestamp = getTimestamp(localData);
        long remoteTimestamp = getTimestamp(remoteData);

        if (localTimestamp >= remoteTimestamp) {
            log.debug("时间戳策略：使用本地数据（本地: {}，远程: {}）",
                    localTimestamp, remoteTimestamp);
            return enhanceWithResolutionInfo(localData, "timestamp", "local_wins");
        } else {
            log.debug("时间戳策略：使用远程数据（本地: {}，远程: {}）",
                    localTimestamp, remoteTimestamp);
            return enhanceWithResolutionInfo(remoteData, "timestamp", "remote_wins");
        }
    }

    /**
     * 版本号策略 - 使用更高版本
     */
    public Map<String, Object> resolveByVersion(Map<String, Object> localData,
                                                Map<String, Object> remoteData) {
        String localVersion = getVersion(localData);
        String remoteVersion = getVersion(remoteData);

        int comparison = compareVersions(localVersion, remoteVersion);

        if (comparison >= 0) {
            log.debug("版本号策略：使用本地数据（本地: {}，远程: {}）",
                    localVersion, remoteVersion);
            return enhanceWithResolutionInfo(localData, "version", "local_wins");
        } else {
            log.debug("版本号策略：使用远程数据（本地: {}，远程: {}）",
                    localVersion, remoteVersion);
            return enhanceWithResolutionInfo(remoteData, "version", "remote_wins");
        }
    }

        /**
         * 合并策略 - 智能合并
         */
        public Map<String, Object> resolveByMerge (Map < String, Object > localData,
                Map < String, Object > remoteData){
            Map<String, Object> mergedData = new HashMap<>();

            // 合并策略：
            // 1. 优先使用远程数据的基础字段
            // 2. 合并配置信息
            // 3. 保留本地数据的元数据

            // 复制远程数据作为基础
            mergedData.putAll(remoteData);

            // 合并重要字段
            mergeField(mergedData, localData, "name");
            mergeField(mergedData, localData, "description");
            mergeField(mergedData, localData, "config");

            // 添加合并标记
            mergedData.put("_merged", true);
            mergedData.put("_mergeTime", System.currentTimeMillis());
            mergedData.put("_mergeStrategy", "intelligent");

            // 记录冲突解决信息
            List<Map<String, Object>> conflictLog = new ArrayList<>();

            // 检查并记录冲突字段
            for (Map.Entry<String, Object> localEntry : localData.entrySet()) {
                String key = localEntry.getKey();
                Object localValue = localEntry.getValue();
                Object remoteValue = remoteData.get(key);

                if (remoteValue != null && !Objects.equals(localValue, remoteValue)) {
                    Map<String, Object> conflict = new HashMap<>();
                    conflict.put("field", key);
                    conflict.put("localValue", localValue);
                    conflict.put("remoteValue", remoteValue);
                    conflict.put("resolvedValue", mergedData.get(key));
                    conflictLog.add(conflict);
                }
            }

            if (!conflictLog.isEmpty()) {
                mergedData.put("_conflictLog", conflictLog);
            }

            log.debug("合并策略：成功合并 {} 个字段，发现 {} 个冲突",
                    mergedData.size(), conflictLog.size());

            return enhanceWithResolutionInfo(mergedData, "merge", "merged");
        }

        /**
         * 自定义字段策略
         */
        public Map<String, Object> resolveByCustomRules (Map < String, Object > localData,
                Map < String, Object > remoteData,
                Map < String, String > fieldRules){
            Map<String, Object> resolvedData = new HashMap<>();

            // 应用自定义规则
            for (Map.Entry<String, String> rule : fieldRules.entrySet()) {
                String field = rule.getKey();
                String ruleType = rule.getValue();

                switch (ruleType) {
                    case "local":
                        resolvedData.put(field, localData.get(field));
                        break;
                    case "remote":
                        resolvedData.put(field, remoteData.get(field));
                        break;
                    case "merge":
                        resolvedData.put(field, mergeFieldValue(
                                localData.get(field), remoteData.get(field)));
                        break;
                    case "latest":
                        Object localValue = localData.get(field);
                        Object remoteValue = remoteData.get(field);
                        long localTime = getFieldTimestamp(localValue);
                        long remoteTime = getFieldTimestamp(remoteValue);
                        resolvedData.put(field, localTime >= remoteTime ? localValue : remoteValue);
                        break;
                    default:
                        // 默认使用远程数据
                        resolvedData.put(field, remoteData.get(field));
                }
            }

            // 添加未指定规则的字段（默认使用远程）
            for (Map.Entry<String, Object> entry : remoteData.entrySet()) {
                if (!resolvedData.containsKey(entry.getKey())) {
                    resolvedData.put(entry.getKey(), entry.getValue());
                }
            }

            log.debug("自定义规则策略：应用 {} 条规则", fieldRules.size());
            return enhanceWithResolutionInfo(resolvedData, "custom", "rules_applied");
        }

        /**
         * 用户手动解决策略
         */
        public Map<String, Object> resolveByManualSelection (Map < String, Object > localData,
                Map < String, Object > remoteData,
                Map < String, Object > userSelection){
            Map<String, Object> resolvedData = new HashMap<>();

            // 应用用户选择
            for (Map.Entry<String, Object> selection : userSelection.entrySet()) {
                String field = selection.getKey();
                String source = (String) selection.getValue();

                if ("local".equals(source)) {
                    resolvedData.put(field, localData.get(field));
                } else if ("remote".equals(source)) {
                    resolvedData.put(field, remoteData.get(field));
                } else if ("custom".equals(source)) {
                    // 用户提供了自定义值
                    resolvedData.put(field, selection.getValue());
                }
            }

            // 填充未选择的字段（默认使用远程）
            for (Map.Entry<String, Object> entry : remoteData.entrySet()) {
                if (!resolvedData.containsKey(entry.getKey())) {
                    resolvedData.put(entry.getKey(), entry.getValue());
                }
            }

            log.debug("手动选择策略：用户选择了 {} 个字段", userSelection.size());
            return enhanceWithResolutionInfo(resolvedData, "manual", "user_selected");
        }

        /**
         * 三向合并策略（需要基础版本）
         */
        public Map<String, Object> resolveByThreeWayMerge (Map < String, Object > baseData,
                Map < String, Object > localData,
                Map < String, Object > remoteData){
            Map<String, Object> mergedData = new HashMap<>();

            // 收集所有字段名
            Set<String> allFields = new HashSet<>();
            allFields.addAll(baseData.keySet());
            allFields.addAll(localData.keySet());
            allFields.addAll(remoteData.keySet());

            // 三向合并算法
            for (String field : allFields) {
                Object baseValue = baseData.get(field);
                Object localValue = localData.get(field);
                Object remoteValue = remoteData.get(field);

                // 情况1：本地和远程都没有修改
                if (Objects.equals(localValue, baseValue) && Objects.equals(remoteValue, baseValue)) {
                    mergedData.put(field, baseValue);
                }
                // 情况2：只有本地修改
                else if (!Objects.equals(localValue, baseValue) && Objects.equals(remoteValue, baseValue)) {
                    mergedData.put(field, localValue);
                }
                // 情况3：只有远程修改
                else if (Objects.equals(localValue, baseValue) && !Objects.equals(remoteValue, baseValue)) {
                    mergedData.put(field, remoteValue);
                }
                // 情况4：本地和远程都修改了，但修改相同
                else if (!Objects.equals(localValue, baseValue) && !Objects.equals(remoteValue, baseValue)
                        && Objects.equals(localValue, remoteValue)) {
                    mergedData.put(field, localValue);
                }
                // 情况5：冲突 - 本地和远程都修改了，且修改不同
                else {
                    // 使用智能合并或标记冲突
                    mergedData.put(field, mergeConflictingField(field, baseValue, localValue, remoteValue));
                    mergedData.put(field + "_conflict", true);
                }
            }

            // 添加合并信息
            mergedData.put("_mergeType", "three_way");
            mergedData.put("_hasConflicts", mergedData.keySet().stream()
                    .anyMatch(key -> key.endsWith("_conflict")));

            log.debug("三向合并策略：合并 {} 个字段", allFields.size());
            return enhanceWithResolutionInfo(mergedData, "three_way", "merged");
        }

        /**
         * 字段级冲突解决
         */
        public Object resolveFieldConflict (String fieldName, Object localValue,
                Object remoteValue, String fieldType){
            switch (fieldType) {
                case "string":
                    return resolveStringConflict((String) localValue, (String) remoteValue);
                case "number":
                    return resolveNumberConflict((Number) localValue, (Number) remoteValue);
                case "boolean":
                    return resolveBooleanConflict((Boolean) localValue, (Boolean) remoteValue);
                case "array":
                    return resolveArrayConflict((List<?>) localValue, (List<?>) remoteValue);
                case "object":
                    return resolveObjectConflict((Map<?, ?>) localValue, (Map<?, ?>) remoteValue);
                case "timestamp":
                    return resolveTimestampConflict((Long) localValue, (Long) remoteValue);
                default:
                    return remoteValue; // 默认使用远程值
            }
        }

        // ========== 私有方法 ==========

        private long getTimestamp (Map < String, Object > data){
            if (data == null) return 0;

            Object timestamp = data.get("timestamp");
            if (timestamp instanceof Long) {
                return (Long) timestamp;
            } else if (timestamp instanceof Integer) {
                return ((Integer) timestamp).longValue();
            } else if (timestamp instanceof String) {
                try {
                    return Long.parseLong((String) timestamp);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }

            // 尝试从其他字段获取时间
            Object updatedAt = data.get("updatedAt");
            if (updatedAt instanceof Long) {
                return (Long) updatedAt;
            }

            Object lastModified = data.get("lastModified");
            if (lastModified instanceof Long) {
                return (Long) lastModified;
            }

            return System.currentTimeMillis();
        }

        private String getVersion (Map < String, Object > data){
            if (data == null) return "1.0.0";

            Object version = data.get("version");
            if (version instanceof String) {
                return (String) version;
            }

            return "1.0.0";
        }

        private int compareVersions (String version1, String version2){
            if (version1 == null || version2 == null) {
                return 0;
            }

            String[] parts1 = version1.split("\\.");
            String[] parts2 = version2.split("\\.");

            int maxLength = Math.max(parts1.length, parts2.length);

            for (int i = 0; i < maxLength; i++) {
                int num1 = i < parts1.length ? parseInt(parts1[i]) : 0;
                int num2 = i < parts2.length ? parseInt(parts2[i]) : 0;

                if (num1 != num2) {
                    return Integer.compare(num1, num2);
                }
            }

            return 0;
        }

        private int parseInt (String str){
            try {
                return Integer.parseInt(str.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private void mergeField (Map < String, Object > target, Map < String, Object > source, String field){
            if (source.containsKey(field)) {
                target.put(field, source.get(field));
            }
        }

    private Object mergeFieldValue(Object value1, Object value2) {
        if (value1 == null) return value2;
        if (value2 == null) return value1;

        if (value1 instanceof Map && value2 instanceof Map) {
            // 既然是业务数据 Map，Key 必然是 String，直接强转
            Map<String, Object> merged = new HashMap<>();
            merged.putAll((Map<String, Object>) value1);
            merged.putAll((Map<String, Object>) value2);
            return merged;
        } else if (value1 instanceof List && value2 instanceof List) {
            List<Object> merged = new ArrayList<>();
            merged.addAll((List<?>) value1);
            merged.addAll((List<?>) value2);
            // 去重并转回 List
            return merged.stream().distinct().collect(java.util.stream.Collectors.toList());
        } else {
            return value2;
        }
    }
        private long getFieldTimestamp (Object value){
            if (value instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) value;
                Object timestamp = map.get("timestamp");
                if (timestamp instanceof Long) {
                    return (Long) timestamp;
                }
            }
            return System.currentTimeMillis();
        }

        private Object mergeConflictingField (String field, Object baseValue,
                Object localValue, Object remoteValue){
            // 根据字段类型选择合并策略
            if (field.contains("name") || field.contains("title")) {
                // 名称类字段：添加冲突标记
                return localValue + " (冲突: " + remoteValue + ")";
            } else if (field.contains("time") || field.contains("date")) {
                // 时间类字段：使用最新时间
                long localTime = getFieldTimestamp(localValue);
                long remoteTime = getFieldTimestamp(remoteValue);
                return localTime >= remoteTime ? localValue : remoteValue;
            } else if (field.contains("config") || field.contains("settings")) {
                // 配置类字段：合并配置
                return mergeFieldValue(localValue, remoteValue);
            } else {
                // 默认：使用远程值，但记录冲突
                return remoteValue;
            }
        }

        private String resolveStringConflict (String local, String remote){
            if (local == null) return remote;
            if (remote == null) return local;

            // 如果内容相同，返回任意一个
            if (local.equals(remote)) return local;

            // 如果一个是另一个的子串，返回较长的
            if (local.contains(remote)) return local;
            if (remote.contains(local)) return remote;

            // 否则合并，用分隔符分开
            return local + " | " + remote;
        }

        private Number resolveNumberConflict (Number local, Number remote){
            if (local == null) return remote;
            if (remote == null) return local;

            // 使用较大的数值
            double localValue = local.doubleValue();
            double remoteValue = remote.doubleValue();

            return localValue >= remoteValue ? local : remote;
        }

        private Boolean resolveBooleanConflict (Boolean local, Boolean remote){
            if (local == null) return remote;
            if (remote == null) return local;

            // 优先使用true
            return local || remote;
        }

        private List<?> resolveArrayConflict (List < ? > local, List < ?>remote){
            if (local == null) return remote;
            if (remote == null) return local;

            List<Object> merged = new ArrayList<>();
            merged.addAll(local);

            // 添加远程中不存在的元素
            for (Object item : remote) {
                if (!containsEquivalent(merged, item)) {
                    merged.add(item);
                }
            }

            return merged;
        }

        private Map<?, ?> resolveObjectConflict (Map < ?, ?>local, Map < ?, ?>remote){
            if (local == null) return remote;
            if (remote == null) return local;

            Map<Object, Object> merged = new HashMap<>();
            merged.putAll(local);
            merged.putAll(remote);

            return merged;
        }

        private Long resolveTimestampConflict (Long local, Long remote){
            if (local == null) return remote;
            if (remote == null) return local;

            // 使用较晚的时间戳
            return Math.max(local, remote);
        }

        private boolean containsEquivalent (List < ? > list, Object item){
            for (Object listItem : list) {
                if (Objects.equals(listItem, item)) {
                    return true;
                }
            }
            return false;
        }

        private Map<String, Object> enhanceWithResolutionInfo (Map < String, Object > data,
                String strategy,
                String resolution){
            Map<String, Object> enhanced = new HashMap<>(data);

            enhanced.put("_resolution", resolution);
            enhanced.put("_strategy", strategy);
            enhanced.put("_resolvedAt", System.currentTimeMillis());

            return enhanced;
        }
    }
