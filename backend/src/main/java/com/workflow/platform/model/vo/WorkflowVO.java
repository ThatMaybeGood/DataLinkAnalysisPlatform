package com.workflow.platform.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工作流视图对象（VO）
 * 用于返回给前端的展示数据
 * 1. WorkflowVersionVO
 *
 * 包含版本基本信息、统计信息、文件信息等
 * 支持复杂的数据结构展示
 * 包含详细的嵌套对象定义
 *
 */
@Data
@ApiModel(description = "工作流视图对象")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowVO {

    @ApiModelProperty(value = "工作流ID", example = "workflow_123456")
    private String id;

    @ApiModelProperty(value = "工作流名称", example = "订单处理流程")
    private String name;

    @ApiModelProperty(value = "工作流别名", example = "order_process")
    private String alias;

    @ApiModelProperty(value = "工作流描述", example = "处理用户订单的全流程")
    private String description;

    @ApiModelProperty(value = "分类", example = "订单管理")
    private String category;

    @ApiModelProperty(value = "标签列表", example = "[\"订单\", \"处理\", \"自动化\"]")
    private List<String> tags;

    @ApiModelProperty(value = "状态", example = "active")
    private String status;

    @ApiModelProperty(value = "版本号", example = "1.0")
    private String version;

    @ApiModelProperty(value = "模式", example = "online")
    private String mode;

    @ApiModelProperty(value = "节点数量", example = "5")
    private Integer nodeCount;

    @ApiModelProperty(value = "执行次数", example = "100")
    private Integer executionCount;

    @ApiModelProperty(value = "成功率", example = "95.5")
    private Double successRate;

    @ApiModelProperty(value = "平均执行耗时（毫秒）", example = "1200")
    private Integer avgDuration;

    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long createdAt;

    @ApiModelProperty(value = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Long updatedAt;

    @ApiModelProperty(value = "创建人")
    private String createdBy;

    @ApiModelProperty(value = "更新人")
    private String updatedBy;

    @ApiModelProperty(value = "简要统计信息")
    private StatisticsVO statistics;

    /**
     * 简要统计信息VO
     */
    @Data
    public static class StatisticsVO {
        @ApiModelProperty(value = "今日执行次数")
        private Integer todayExecutions = 0;

        @ApiModelProperty(value = "今日成功率")
        private Double todaySuccessRate = 0.0;

        @ApiModelProperty(value = "本周执行次数")
        private Integer weeklyExecutions = 0;

        @ApiModelProperty(value = "平均执行时间（毫秒）")
        private Integer avgExecutionTime = 0;
    }

    /**
     * 创建简单的VO对象（用于列表展示）
     */
    public static WorkflowVO simple(com.workflow.platform.model.dto.WorkflowDTO dto) {
        WorkflowVO vo = new WorkflowVO();
        vo.setId(dto.getId());
        vo.setName(dto.getName());
        vo.setAlias(dto.getAlias());
        vo.setDescription(dto.getDescription());
        vo.setCategory(dto.getCategory());
        vo.setStatus(dto.getStatus());
        vo.setMode(dto.getMode());
        vo.setNodeCount(dto.getNodeCount());
        vo.setExecutionCount(dto.getExecutionCount());
        vo.setSuccessRate(dto.getSuccessRate());
        vo.setAvgDuration(dto.getAvgDuration());
        vo.setCreatedAt(dto.getCreatedAt());
        vo.setUpdatedAt(dto.getUpdatedAt());
        return vo;
    }
}