package com.workflow.platform.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 冲突统计信息视图对象
 */
@Data
@ApiModel(description = "冲突统计信息视图对象")
public class ConflictStatisticsVO {

    @ApiModelProperty(value = "冲突总数", example = "100")
    private Long totalConflicts;

    @ApiModelProperty(value = "待解决冲突数", example = "20")
    private Long pendingConflicts;

    @ApiModelProperty(value = "已解决冲突数", example = "80")
    private Long resolvedConflicts;

    @ApiModelProperty(value = "自动解决冲突数", example = "50")
    private Long autoResolvedConflicts;

    @ApiModelProperty(value = "手动解决冲突数", example = "30")
    private Long manuallyResolvedConflicts;

    @ApiModelProperty(value = "忽略的冲突数", example = "5")
    private Long ignoredConflicts;

    @ApiModelProperty(value = "今日新增冲突数", example = "5")
    private Long todayNewConflicts;

    @ApiModelProperty(value = "本周新增冲突数", example = "25")
    private Long weeklyNewConflicts;

    @ApiModelProperty(value = "本月新增冲突数", example = "80")
    private Long monthlyNewConflicts;

    @ApiModelProperty(value = "按冲突类型统计")
    private List<TypeStatistic> conflictTypeStatistics;

    @ApiModelProperty(value = "按对象类型统计")
    private List<TypeStatistic> objectTypeStatistics;

    @ApiModelProperty(value = "按解决状态统计")
    private List<StatusStatistic> statusStatistics;

    @ApiModelProperty(value = "按解决策略统计")
    private List<StrategyStatistic> strategyStatistics;

    @ApiModelProperty(value = "按严重程度统计")
    private List<SeverityStatistic> severityStatistics;

    @ApiModelProperty(value = "按日期统计")
    private List<DateStatistic> dateStatistics;

    @ApiModelProperty(value = "按解决人统计")
    private List<ResolverStatistic> resolverStatistics;

    @ApiModelProperty(value = "平均解决时间（分钟）", example = "45.5")
    private Double averageResolutionTime;

    @ApiModelProperty(value = "最长解决时间（分钟）", example = "120.5")
    private Double longestResolutionTime;

    @ApiModelProperty(value = "最短解决时间（分钟）", example = "5.5")
    private Double shortestResolutionTime;

    @ApiModelProperty(value = "自动解决成功率", example = "95.5")
    private Double autoResolutionSuccessRate;

    @ApiModelProperty(value = "手动解决成功率", example = "98.5")
    private Double manualResolutionSuccessRate;

    @ApiModelProperty(value = "冲突解决率", example = "85.5")
    private Double resolutionRate;

    @ApiModelProperty(value = "解决趋势")
    private ResolutionTrend resolutionTrend;

    @ApiModelProperty(value = "高频冲突对象")
    private List<HighFrequencyConflict> highFrequencyConflicts;

    /**
     * 类型统计
     */
    @Data
    @ApiModel(description = "类型统计")
    public static class TypeStatistic {

        @ApiModelProperty(value = "类型", example = "WORKFLOW_CONFLICT")
        private String type;

        @ApiModelProperty(value = "类型名称", example = "工作流冲突")
        private String typeName;

        @ApiModelProperty(value = "数量", example = "50")
        private Long count;

        @ApiModelProperty(value = "占比", example = "50.0")
        private Double percentage;

        @ApiModelProperty(value = "趋势变化", example = "+5.2")
        private Double trendChange;
    }

    /**
     * 状态统计
     */
    @Data
    @ApiModel(description = "状态统计")
    public static class StatusStatistic {

        @ApiModelProperty(value = "状态", example = "RESOLVED")
        private String status;

        @ApiModelProperty(value = "状态名称", example = "已解决")
        private String statusName;

        @ApiModelProperty(value = "数量", example = "80")
        private Long count;

        @ApiModelProperty(value = "占比", example = "80.0")
        private Double percentage;
    }

    /**
     * 策略统计
     */
    @Data
    @ApiModel(description = "解决策略统计")
    public static class StrategyStatistic {

        @ApiModelProperty(value = "策略", example = "CLIENT_PRIORITY")
        private String strategy;

        @ApiModelProperty(value = "策略名称", example = "客户端优先")
        private String strategyName;

        @ApiModelProperty(value = "数量", example = "30")
        private Long count;

        @ApiModelProperty(value = "占比", example = "30.0")
        private Double percentage;

        @ApiModelProperty(value = "平均解决时间", example = "25.5")
        private Double averageTime;

        @ApiModelProperty(value = "成功率", example = "95.5")
        private Double successRate;
    }

    /**
     * 严重程度统计
     */
    @Data
    @ApiModel(description = "严重程度统计")
    public static class SeverityStatistic {

        @ApiModelProperty(value = "严重程度", example = "MEDIUM")
        private String severity;

        @ApiModelProperty(value = "严重程度名称", example = "中等")
        private String severityName;

        @ApiModelProperty(value = "数量", example = "60")
        private Long count;

        @ApiModelProperty(value = "占比", example = "60.0")
        private Double percentage;

        @ApiModelProperty(value = "平均解决时间", example = "40.5")
        private Double averageResolutionTime;
    }

    /**
     * 日期统计
     */
    @Data
    @ApiModel(description = "日期统计")
    public static class DateStatistic {

        @ApiModelProperty(value = "日期", example = "2024-01-01")
        private String date;

        @ApiModelProperty(value = "新增冲突数", example = "10")
        private Long newConflicts;

        @ApiModelProperty(value = "解决冲突数", example = "8")
        private Long resolvedConflicts;

        @ApiModelProperty(value = "未解决冲突数", example = "2")
        private Long pendingConflicts;

        @ApiModelProperty(value = "平均解决时间", example = "40.5")
        private Double averageResolutionTime;

        @ApiModelProperty(value = "解决率", example = "80.0")
        private Double resolutionRate;
    }

    /**
     * 解决人统计
     */
    @Data
    @ApiModel(description = "解决人统计")
    public static class ResolverStatistic {

        @ApiModelProperty(value = "解决人ID", example = "user001")
        private String resolverId;

        @ApiModelProperty(value = "解决人姓名", example = "张三")
        private String resolverName;

        @ApiModelProperty(value = "解决数量", example = "25")
        private Long resolvedCount;

        @ApiModelProperty(value = "平均解决时间", example = "35.5")
        private Double averageResolutionTime;

        @ApiModelProperty(value = "成功率", example = "98.5")
        private Double successRate;

        @ApiModelProperty(value = "常用策略", example = "TIMESTAMP_PRIORITY")
        private String preferredStrategy;
    }

    /**
     * 解决趋势
     */
    @Data
    @ApiModel(description = "解决趋势")
    public static class ResolutionTrend {

        @ApiModelProperty(value = "趋势周期", example = "LAST_7_DAYS")
        private String trendPeriod;

        @ApiModelProperty(value = "新增趋势数据")
        private List<TrendData> newConflictsTrend;

        @ApiModelProperty(value = "解决趋势数据")
        private List<TrendData> resolvedConflictsTrend;

        @ApiModelProperty(value = "解决率趋势数据")
        private List<TrendData> resolutionRateTrend;

        @ApiModelProperty(value = "平均解决时间趋势")
        private List<TrendData> averageTimeTrend;

        @ApiModelProperty(value = "趋势分析", example = "冲突数量呈下降趋势，解决效率提升")
        private String trendAnalysis;

        @ApiModelProperty(value = "趋势预测", example = "预计下周冲突数量将继续下降")
        private String trendPrediction;
    }

    /**
     * 趋势数据
     */
    @Data
    @ApiModel(description = "趋势数据")
    public static class TrendData {

        @ApiModelProperty(value = "时间点", example = "2024-01-01")
        private String timePoint;

        @ApiModelProperty(value = "值", example = "10")
        private Double value;

        @ApiModelProperty(value = "变化率", example = "+5.5")
        private Double changeRate;
    }

    /**
     * 高频冲突对象
     */
    @Data
    @ApiModel(description = "高频冲突对象")
    public static class HighFrequencyConflict {

        @ApiModelProperty(value = "对象ID", example = "workflow_001")
        private String objectId;

        @ApiModelProperty(value = "对象名称", example = "订单处理流程")
        private String objectName;

        @ApiModelProperty(value = "对象类型", example = "WORKFLOW")
        private String objectType;

        @ApiModelProperty(value = "冲突次数", example = "15")
        private Integer conflictCount;

        @ApiModelProperty(value = "首次冲突时间", example = "2024-01-01")
        private String firstConflictTime;

        @ApiModelProperty(value = "最近冲突时间", example = "2024-01-15")
        private String lastConflictTime;

        @ApiModelProperty(value = "冲突频率（次/天）", example = "1.5")
        private Double conflictFrequency;

        @ApiModelProperty(value = "主要冲突类型", example = "WORKFLOW_CONFLICT")
        private String mainConflictType;

        @ApiModelProperty(value = "建议", example = "建议增加锁定机制")
        private String suggestion;
    }
}