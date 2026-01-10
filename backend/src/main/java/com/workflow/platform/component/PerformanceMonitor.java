package com.workflow.platform.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.SystemHealth;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 性能监控器 - 监控系统性能指标
 */
@Slf4j
@Component
public class PerformanceMonitor {

    @Value("${workflow.platform.performance.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${workflow.platform.performance.sampling-interval:60000}")
    private long samplingInterval;

    @Value("${workflow.platform.performance.history-size:1000}")
    private int historySize;

    @Value("${workflow.platform.performance.threshold.cpu:80}")
    private double cpuThreshold;

    @Value("${workflow.platform.performance.threshold.memory:90}")
    private double memoryThreshold;

    @Value("${workflow.platform.performance.threshold.disk:85}")
    private double diskThreshold;

    @Value("${workflow.platform.performance.alert.enabled:true}")
    private boolean alertEnabled;

    // 性能指标存储
    private final Map<String, PerformanceMetric> metrics = new ConcurrentHashMap<>();
    private final Map<String, List<PerformanceSample>> history = new ConcurrentHashMap<>();

    // 性能统计
    private final PerformanceStats stats = new PerformanceStats();
    private final ReentrantReadWriteLock statsLock = new ReentrantReadWriteLock();

    // 监控线程
    private Thread monitoringThread;
    private volatile boolean running = false;

    /**
     * 开始性能监控
     */
    public synchronized void startMonitoring() {
        if (!monitoringEnabled || running) {
            return;
        }

        log.info("启动性能监控，采样间隔: {}ms", samplingInterval);

        running = true;
        monitoringThread = new Thread(this::monitoringLoop, "performance-monitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }

    /**
     * 停止性能监控
     */
    public synchronized void stopMonitoring() {
        if (!running) {
            return;
        }

        log.info("停止性能监控");

        running = false;
        if (monitoringThread != null) {
            monitoringThread.interrupt();
            monitoringThread = null;
        }
    }

    /**
     * 记录性能指标
     */
    public void recordMetric(String metricName, double value, Map<String, Object> tags) {
        if (!monitoringEnabled) {
            return;
        }

        long timestamp = System.currentTimeMillis();
        PerformanceMetric metric = new PerformanceMetric(metricName, value, timestamp, tags);

        metrics.put(metricName, metric);
        addToHistory(metric);

        // 更新统计
        updateStats(metric);

        // 检查阈值
        checkThreshold(metric);
    }

    /**
     * 记录API性能
     */
    public void recordApiPerformance(String apiName, long duration, boolean success,
                                     int statusCode, Map<String, Object> metadata) {
        if (!monitoringEnabled) {
            return;
        }

        Map<String, Object> tags = new HashMap<>();
        tags.put("api", apiName);
        tags.put("success", success);
        tags.put("statusCode", statusCode);

        if (metadata != null) {
            tags.putAll(metadata);
        }

        recordMetric("api.response.time", duration, tags);
        recordMetric("api.request.count", 1, tags);

        if (!success) {
            recordMetric("api.error.count", 1, tags);
        }
    }

    /**
     * 记录数据库性能
     */
    public void recordDatabasePerformance(String operation, long duration,
                                          boolean success, String table, int rowCount) {
        if (!monitoringEnabled) {
            return;
        }

        Map<String, Object> tags = new HashMap<>();
        tags.put("operation", operation);
        tags.put("table", table);
        tags.put("success", success);
        tags.put("rowCount", rowCount);

        recordMetric("db.operation.time", duration, tags);
        recordMetric("db.operation.count", 1, tags);
    }

    /**
     * 记录缓存性能
     */
    public void recordCachePerformance(String operation, String cacheName,
                                       long duration, boolean hit, int size) {
        if (!monitoringEnabled) {
            return;
        }

        Map<String, Object> tags = new HashMap<>();
        tags.put("operation", operation);
        tags.put("cache", cacheName);
        tags.put("hit", hit);
        tags.put("size", size);

        recordMetric("cache.operation.time", duration, tags);
        recordMetric("cache.operation.count", 1, tags);

        if (hit) {
            recordMetric("cache.hit.count", 1, tags);
        } else {
            recordMetric("cache.miss.count", 1, tags);
        }
    }

    /**
     * 获取性能指标
     */
    public PerformanceMetric getMetric(String metricName) {
        return metrics.get(metricName);
    }

    /**
     * 获取所有指标
     */
    public Map<String, PerformanceMetric> getAllMetrics() {
        return new HashMap<>(metrics);
    }

    /**
     * 获取性能历史
     */
    public List<PerformanceSample> getMetricHistory(String metricName,
                                                    Long startTime, Long endTime) {
        List<PerformanceSample> samples = history.get(metricName);
        if (samples == null) {
            return Collections.emptyList();
        }

        if (startTime == null && endTime == null) {
            return new ArrayList<>(samples);
        }

        long start = startTime != null ? startTime : 0;
        long end = endTime != null ? endTime : Long.MAX_VALUE;

        return samples.stream()
                .filter(sample -> sample.getTimestamp() >= start && sample.getTimestamp() <= end)
                .toList();
    }

    /**
     * 获取性能统计
     */
    public PerformanceStats getPerformanceStats() {
        statsLock.readLock().lock();
        try {
            // 克隆统计对象
            PerformanceStats clone = new PerformanceStats();
            clone.setSystemStats(stats.getSystemStats());
            clone.setApiStats(stats.getApiStats());
            clone.setDatabaseStats(stats.getDatabaseStats());
            clone.setCacheStats(stats.getCacheStats());
            clone.setCustomStats(stats.getCustomStats());
            clone.setAlerts(stats.getAlerts());
            return clone;
        } finally {
            statsLock.readLock().unlock();
        }
    }

    /**
     * 获取系统健康状态
     */
    public SystemHealth getSystemHealth() {
        SystemHealth health = new SystemHealth();
        health.setTimestamp(System.currentTimeMillis());

        // 收集系统指标
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();

        // CPU使用率
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean =
                    (com.sun.management.OperatingSystemMXBean) osBean;
            double cpuUsage = sunOsBean.getProcessCpuLoad() * 100;
            health.setCpuUsage(cpuUsage);
            health.setCpuHealthy(cpuUsage < cpuThreshold);
        }

        // 内存使用率
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double memoryUsage = (double) usedMemory / totalMemory * 100;

        health.setMemoryTotal(totalMemory);
        health.setMemoryUsed(usedMemory);
        health.setMemoryUsage(memoryUsage);
        health.setMemoryHealthy(memoryUsage < memoryThreshold);

        // 线程状态
        health.setThreadCount(threadBean.getThreadCount());
        health.setDaemonThreadCount(threadBean.getDaemonThreadCount());

        // 垃圾收集
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        long gcCount = 0;
        long gcTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            gcCount += gcBean.getCollectionCount();
            gcTime += gcBean.getCollectionTime();
        }

        health.setGcCount(gcCount);
        health.setGcTime(gcTime);

        // 整体健康状态
        health.setOverallHealthy(
                health.isCpuHealthy() &&
                        health.isMemoryHealthy() &&
                        health.getThreadCount() < 1000 // 线程数阈值
        );

        // 计算健康分数（0-100）
        double healthScore = 100;
        if (!health.isCpuHealthy()) healthScore -= 20;
        if (!health.isMemoryHealthy()) healthScore -= 20;
        if (health.getThreadCount() > 1000) healthScore -= 10;
        if (memoryUsage > 95) healthScore -= 10;

        health.setHealthScore(Math.max(0, healthScore));

        return health;
    }

    /**
     * 生成性能报告
     */
    public PerformanceReport generateReport(ReportCriteria criteria) {
        PerformanceReport report = new PerformanceReport();
        report.setGeneratedAt(System.currentTimeMillis());
        report.setCriteria(criteria);

        // 收集系统健康信息
        report.setSystemHealth(getSystemHealth());

        // 收集性能统计
        report.setPerformanceStats(getPerformanceStats());

        // 收集关键指标趋势
        Map<String, List<PerformanceSample>> trends = new HashMap<>();

        String[] keyMetrics = {
                "api.response.time",
                "db.operation.time",
                "cache.hit.rate",
                "system.cpu.usage",
                "system.memory.usage"
        };

        for (String metric : keyMetrics) {
            List<PerformanceSample> samples = getMetricHistory(
                    metric, criteria.getStartTime(), criteria.getEndTime());
            trends.put(metric, samples);
        }

        report.setMetricTrends(trends);

        // 分析性能问题
        report.setPerformanceIssues(analyzePerformanceIssues());

        // 生成建议
        report.setRecommendations(generateRecommendations(report));

        return report;
    }

    /**
     * 清理历史数据
     */
    public void cleanupHistory(long cutoffTime) {
        int removed = 0;

        for (List<PerformanceSample> samples : history.values()) {
            Iterator<PerformanceSample> iterator = samples.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().getTimestamp() < cutoffTime) {
                    iterator.remove();
                    removed++;
                }
            }
        }

        log.info("清理性能历史数据，移除 {} 条记录", removed);
    }

    /**
     * 重置性能统计
     */
    public void resetStats() {
        statsLock.writeLock().lock();
        try {
            metrics.clear();
            history.clear();
            stats.reset();
            log.info("性能统计已重置");
        } finally {
            statsLock.writeLock().unlock();
        }
    }

    // ========== 私有方法 ==========

    private void monitoringLoop() {
        log.info("性能监控循环开始");

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 收集系统指标
                collectSystemMetrics();

                // 清理旧数据
                long cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000L);
                cleanupHistory(cutoffTime);

                Thread.sleep(samplingInterval);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("性能监控异常", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("性能监控循环结束");
    }

    private void collectSystemMetrics() {
        try {
            // CPU使用率
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunOsBean =
                        (com.sun.management.OperatingSystemMXBean) osBean;

                double cpuUsage = sunOsBean.getSystemCpuLoad() * 100;
                double processCpuUsage = sunOsBean.getProcessCpuLoad() * 100;

                recordMetric("system.cpu.usage", cpuUsage,
                        Map.of("type", "system"));
                recordMetric("system.cpu.process.usage", processCpuUsage,
                        Map.of("type", "process"));
            }

            // 内存使用
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsage = (double) usedMemory / totalMemory * 100;

            recordMetric("system.memory.total", totalMemory,
                    Map.of("unit", "bytes"));
            recordMetric("system.memory.used", usedMemory,
                    Map.of("unit", "bytes"));
            recordMetric("system.memory.usage", memoryUsage,
                    Map.of("unit", "percent"));

            // 线程信息
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            recordMetric("system.thread.count", threadBean.getThreadCount(),
                    Map.of("type", "total"));
            recordMetric("system.thread.daemon.count", threadBean.getDaemonThreadCount(),
                    Map.of("type", "daemon"));

            // 垃圾收集
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                Map<String, Object> tags = Map.of(
                        "name", gcBean.getName(),
                        "type", "gc"
                );

                recordMetric("system.gc.count", gcBean.getCollectionCount(), tags);
                recordMetric("system.gc.time", gcBean.getCollectionTime(), tags);
            }

            // 磁盘使用（简化）
            recordMetric("system.disk.usage", 50.0,
                    Map.of("unit", "percent")); // 实际应从系统获取

        } catch (Exception e) {
            log.error("收集系统指标失败", e);
        }
    }

    private void addToHistory(PerformanceMetric metric) {
        List<PerformanceSample> samples = history.computeIfAbsent(
                metric.getName(), k -> new ArrayList<>());

        PerformanceSample sample = new PerformanceSample(
                metric.getValue(), metric.getTimestamp(), metric.getTags());

        samples.add(sample);

        // 限制历史大小
        if (samples.size() > historySize) {
            samples.remove(0);
        }
    }

    private void updateStats(PerformanceMetric metric) {
        statsLock.writeLock().lock();
        try {
            String name = metric.getName();

            // 更新系统统计
            if (name.startsWith("system.")) {
                stats.updateSystemMetric(name, metric.getValue());
            }
            // 更新API统计
            else if (name.startsWith("api.")) {
                stats.updateApiMetric(name, metric.getValue(), metric.getTags());
            }
            // 更新数据库统计
            else if (name.startsWith("db.")) {
                stats.updateDatabaseMetric(name, metric.getValue(), metric.getTags());
            }
            // 更新缓存统计
            else if (name.startsWith("cache.")) {
                stats.updateCacheMetric(name, metric.getValue(), metric.getTags());
            }
            // 更新自定义统计
            else {
                stats.updateCustomMetric(name, metric.getValue(), metric.getTags());
            }

        } finally {
            statsLock.writeLock().unlock();
        }
    }

    private void checkThreshold(PerformanceMetric metric) {
        if (!alertEnabled) {
            return;
        }

        String name = metric.getName();
        double value = metric.getValue();

        // 检查CPU阈值
        if (name.equals("system.cpu.usage") && value > cpuThreshold) {
            triggerAlert("CPU使用率过高",
                    String.format("CPU使用率: %.2f%% > 阈值: %.2f%%", value, cpuThreshold),
                    "HIGH", metric);
        }

        // 检查内存阈值
        else if (name.equals("system.memory.usage") && value > memoryThreshold) {
            triggerAlert("内存使用率过高",
                    String.format("内存使用率: %.2f%% > 阈值: %.2f%%", value, memoryThreshold),
                    "HIGH", metric);
        }

        // 检查API响应时间
        else if (name.equals("api.response.time") && value > 1000) { // 1秒阈值
            triggerAlert("API响应时间过长",
                    String.format("API响应时间: %.2fms", value),
                    "MEDIUM", metric);
        }

        // 检查数据库操作时间
        else if (name.equals("db.operation.time") && value > 500) { // 500ms阈值
            triggerAlert("数据库操作时间过长",
                    String.format("数据库操作时间: %.2fms", value),
                    "MEDIUM", metric);
        }
    }

    private void triggerAlert(String title, String message, String severity,
                              PerformanceMetric metric) {

        PerformanceAlert alert = new PerformanceAlert();
        alert.setId(generateAlertId());
        alert.setTitle(title);
        alert.setMessage(message);
        alert.setSeverity(severity);
        alert.setMetricName(metric.getName());
        alert.setMetricValue(metric.getValue());
        alert.setTimestamp(System.currentTimeMillis());
        alert.setTags(metric.getTags());

        // 添加到统计
        stats.addAlert(alert);

        // 记录日志
        log.warn("性能告警: {} - {} (值: {})", title, message, metric.getValue());

        // 在实际应用中，这里应该发送通知（邮件、短信、WebHook