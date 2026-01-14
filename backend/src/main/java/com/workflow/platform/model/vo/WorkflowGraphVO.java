package com.workflow.platform.model.vo;//工作流图VO

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 工作流图视图对象 - 用于前端展示工作流图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowGraphVO {

    // 基本信息
    private String workflowId;
    private String workflowName;
    private String workflowDescription;
    private String category;

    // 图数据
    private List<Node> nodes;
    private List<Edge> edges;

    // 布局信息
    private GraphLayout layout;

    // 统计信息
    private Map<String, Object> statistics;

    // 时间信息
    private Date createdTime;
    private Date updatedTime;

    // 元数据
    private Map<String, Object> metadata;

    // ========== 内部类：节点 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Node {
        // 节点标识
        private String id;
        private String name;
        private String type; // start, end, action, decision等
        private String subType; // 子类型

        // 显示信息
        private String label;
        private String description;
        private String icon;
        private String color;
        private String shape; // circle, rect, diamond等

        // 位置信息（可能由前端计算）
        private Double x;
        private Double y;
        private Integer width;
        private Integer height;

        // 业务属性
        private Map<String, Object> properties;
        private Map<String, Object> config;

        // 验证规则
        private List<ValidationRule> validationRules;

        // 状态
        private String status;
        private Boolean disabled;
        private Boolean selected;

        // 执行信息
        private ExecutionInfo executionInfo;

        // 元数据
        private Date createdTime;
        private Date updatedTime;
        private Map<String, Object> metadata;
    }

    // ========== 内部类：边 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Edge {
        // 边标识
        private String id;
        private String source; // 源节点ID
        private String target; // 目标节点ID

        // 显示信息
        private String label;
        private String type; // sequence, condition, parallel等
        private String color;
        private Integer width;
        private String style; // solid, dashed, dotted

        // 业务属性
        private Map<String, Object> properties;
        private Condition condition; // 条件信息

        // 状态
        private String status;
        private Boolean disabled;
        private Boolean selected;

        // 元数据
        private Date createdTime;
        private Date updatedTime;
    }

    // ========== 内部类：条件 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Condition {
        private String expression; // 条件表达式
        private String language; // 表达式语言
        private Map<String, Object> variables; // 变量定义
        private String description;
    }

    // ========== 内部类：验证规则 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationRule {
        private String id;
        private String name;
        private String type; // required, format, range等
        private String expression;
        private String message;
        private String level; // error, warning, info
        private Boolean enabled;
    }

    // ========== 内部类：执行信息 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ExecutionInfo {
        private String executionId;
        private String status; // pending, running, success, failed
        private Date startTime;
        private Date endTime;
        private Long duration; // 毫秒
        private Object result;
        private String errorMessage;
        private Map<String, Object> logs;
    }

    // ========== 内部类：图布局 ==========
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GraphLayout {
        // 布局算法
        private String algorithm; // dagre, force, hierarchical等
        private String direction; // LR, RL, TB, BT

        // 画布尺寸
        private Integer width;
        private Integer height;
        private Integer padding;

        // 节点布局
        private Integer nodeSpacing;
        private Integer rankSpacing;

        // 节点位置映射
        private Map<String, NodePosition> nodePositions;

        // 布局配置
        private Map<String, Object> config;

        // ========== 内部类：节点位置 ==========
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class NodePosition {
            private Double x;
            private Double y;
            private Integer width;
            private Integer height;
            private Integer rotation; // 旋转角度
            private String anchor; // 锚点位置
        }
    }

    // ========== 方法 ==========

    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes != null ? nodes.size() : 0;
    }

    /**
     * 获取边数量
     */
    public int getEdgeCount() {
        return edges != null ? edges.size() : 0;
    }

    /**
     * 查找节点
     */
    public Node findNode(String nodeId) {
        if (nodes == null) return null;
        return nodes.stream()
                .filter(node -> node.getId().equals(nodeId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找节点的出边
     */
    public List<Edge> findOutgoingEdges(String nodeId) {
        if (edges == null) return List.of();
        return edges.stream()
                .filter(edge -> edge.getSource().equals(nodeId))
                .toList();
    }

    /**
     * 查找节点的入边
     */
    public List<Edge> findIncomingEdges(String nodeId) {
        if (edges == null) return List.of();
        return edges.stream()
                .filter(edge -> edge.getTarget().equals(nodeId))
                .toList();
    }

    /**
     * 检查图是否为空
     */
    public boolean isEmpty() {
        return (nodes == null || nodes.isEmpty()) &&
                (edges == null || edges.isEmpty());
    }

    /**
     * 获取开始节点
     */
    public List<Node> getStartNodes() {
        if (nodes == null) return List.of();
        return nodes.stream()
                .filter(node -> "start".equals(node.getType()))
                .toList();
    }

    /**
     * 获取结束节点
     */
    public List<Node> getEndNodes() {
        if (nodes == null) return List.of();
        return nodes.stream()
                .filter(node -> "end".equals(node.getType()))
                .toList();
    }

    /**
     * 获取指定类型的节点
     */
    public List<Node> getNodesByType(String nodeType) {
        if (nodes == null) return List.of();
        return nodes.stream()
                .filter(node -> nodeType.equals(node.getType()))
                .toList();
    }
}