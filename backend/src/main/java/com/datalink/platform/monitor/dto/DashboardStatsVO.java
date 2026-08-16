package com.datalink.platform.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 监控总览仪表盘统计
 */
@Data
public class DashboardStatsVO {
    /** 流程总数 */
    private int processCount;
    /** 运行中实例数 */
    private int runningInstances;
    /** 今日完成实例数 */
    private int doneToday;
    /** 未关闭告警数 */
    private int openAlerts;
    /** 卡点实例数 */
    private int stuckCount;
    /** 检测点覆盖率（百分比） */
    private int checkpointCoverage;
    /** 平均流程耗时（格式化串） */
    private String avgDuration;
    /** 最慢站点 TopN */
    private List<SlowNodeItem> topSlowNodes;
    /** 实例运行趋势 */
    private List<TrendItem> instanceTrend;

    /**
     * 慢节点项
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SlowNodeItem {
        /** 站点名 */
        private String name;
        /** 平均耗时（格式化串） */
        private String duration;
    }

    /**
     * 趋势项（按时间分桶）
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendItem {
        /** 时间标签，如 08-15 */
        private String label;
        /** 实例数量 */
        private int value;
    }
}
