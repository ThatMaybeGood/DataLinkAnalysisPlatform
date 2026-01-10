package com.workflow.platform.util;
//工作流图工具

import com.workflow.platform.model.vo.WorkflowGraphVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 工作流图工具类
 */
@Slf4j
@Component
public class WorkflowGraphUtil {

    /**
     * 验证工作流图的完整性
     */
    public boolean validateGraph(WorkflowGraphVO graph) {
        if (graph == null || graph.isEmpty()) {
            log.warn("工作流图为空");
            return false;
        }

        // 检查节点ID唯一性
        Set<String> nodeIds = new HashSet<>();
        for (WorkflowGraphVO.Node node : graph.getNodes()) {
            if (nodeIds.contains(node.getId())) {
                log.error("节点ID重复: {}", node.getId());
                return false;
            }
            nodeIds.add(node.getId());
        }

        // 检查边的节点引用
        for (WorkflowGraphVO.Edge edge : graph.getEdges()) {
            if (!nodeIds.contains(edge.getSource())) {
                log.error("边引用了不存在的源节点: {}", edge.getSource());
                return false;
            }
            if (!nodeIds.contains(edge.getTarget())) {
                log.error("边引用了不存在的目标节点: {}", edge.getTarget());
                return false;
            }
        }

        // 检查是否有开始节点
        List<WorkflowGraphVO.Node> startNodes = graph.getStartNodes();
        if (startNodes.isEmpty()) {
            log.warn("工作流图没有开始节点");
        }

        // 检查是否有结束节点
        List<WorkflowGraphVO.Node> endNodes = graph.getEndNodes();
        if (endNodes.isEmpty()) {
            log.warn("工作流图没有结束节点");
        }

        // 检查是否有孤立的节点（没有连接的节点）
        Set<String> connectedNodes = new HashSet<>();
        for (WorkflowGraphVO.Edge edge : graph.getEdges()) {
            connectedNodes.add(edge.getSource());
            connectedNodes.add(edge.getTarget());
        }

        Set<String> isolatedNodes = nodeIds.stream()
                .filter(id -> !connectedNodes.contains(id))
                .collect(Collectors.toSet());

        if (!isolatedNodes.isEmpty()) {
            log.warn("发现孤立节点: {}", isolatedNodes);
        }

        log.info("工作流图验证通过: 节点数={}, 边数={}, 开始节点数={}, 结束节点数={}, 孤立节点数={}",
                graph.getNodes().size(), graph.getEdges().size(),
                startNodes.size(), endNodes.size(), isolatedNodes.size());

        return true;
    }

    /**
     * 计算图的复杂度分数
     */
    public double calculateComplexityScore(WorkflowGraphVO graph) {
        if (graph == null || graph.isEmpty()) {
            return 0.0;
        }

        int nodeCount = graph.getNodeCount();
        int edgeCount = graph.getEdgeCount();

        // 计算平均节点度数
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, Integer> outDegree = new HashMap<>();

        for (WorkflowGraphVO.Edge edge : graph.getEdges()) {
            outDegree.put(edge.getSource(), outDegree.getOrDefault(edge.getSource(), 0) + 1);
            inDegree.put(edge.getTarget(), inDegree.getOrDefault(edge.getTarget(), 0) + 1);
        }

        double avgInDegree = inDegree.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        double avgOutDegree = outDegree.values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        // 计算最大深度
        int maxDepth = calculateMaxDepth(graph);

        // 计算复杂性分数（简单公式）
        double complexity = (nodeCount * 0.3) +
                (edgeCount * 0.4) +
                ((avgInDegree + avgOutDegree) * 0.2) +
                (maxDepth * 0.1);

        log.debug("计算图复杂度: 节点数={}, 边数={}, 平均入度={}, 平均出度={}, 最大深度={}, 复杂度={}",
                nodeCount, edgeCount, avgInDegree, avgOutDegree, maxDepth, complexity);

        return complexity;
    }

    /**
     * 计算图的最大深度
     */
    public int calculateMaxDepth(WorkflowGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getEdges() == null) {
            return 0;
        }

        // 找到所有起点（没有入边的节点）
        Set<String> startNodes = graph.getNodes().stream()
                .map(WorkflowGraphVO.Node::getId)
                .collect(Collectors.toSet());

        for (WorkflowGraphVO.Edge edge : graph.getEdges()) {
            startNodes.remove(edge.getTarget());
        }

        // 从每个起点计算最大深度
        int maxDepth = 0;
        for (String startNode : startNodes) {
            int depth = calculateDepthFromNode(startNode, graph, new HashMap<>());
            maxDepth = Math.max(maxDepth, depth);
        }

        return maxDepth;
    }

    /**
     * 查找图中的所有路径
     */
    public List<List<String>> findAllPaths(WorkflowGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getEdges() == null) {
            return Collections.emptyList();
        }

        List<List<String>> allPaths = new ArrayList<>();

        // 获取所有开始节点和结束节点
        List<WorkflowGraphVO.Node> startNodes = graph.getStartNodes();
        List<WorkflowGraphVO.Node> endNodes = graph.getEndNodes();

        if (startNodes.isEmpty() || endNodes.isEmpty()) {
            log.warn("没有开始节点或结束节点，无法查找路径");
            return allPaths;
        }

        // 构建邻接表
        Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);

        // 查找所有从开始节点到结束节点的路径
        for (WorkflowGraphVO.Node startNode : startNodes) {
            for (WorkflowGraphVO.Node endNode : endNodes) {
                List<List<String>> paths = findPathsBetweenNodes(
                        startNode.getId(), endNode.getId(), adjacencyList);
                allPaths.addAll(paths);
            }
        }

        log.debug("找到路径数: {}", allPaths.size());
        return allPaths;
    }

    /**
     * 检查图中是否有环
     */
    public boolean hasCycle(WorkflowGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getEdges() == null) {
            return false;
        }

        Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String nodeId : adjacencyList.keySet()) {
            if (hasCycleUtil(nodeId, adjacencyList, visited, recursionStack)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取图的拓扑排序
     */
    public List<String> topologicalSort(WorkflowGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getEdges() == null) {
            return Collections.emptyList();
        }

        Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);
        Set<String> visited = new HashSet<>();
        Stack<String> stack = new Stack<>();

        for (String nodeId : adjacencyList.keySet()) {
            if (!visited.contains(nodeId)) {
                topologicalSortUtil(nodeId, adjacencyList, visited, stack);
            }
        }

        List<String> result = new ArrayList<>();
        while (!stack.isEmpty()) {
            result.add(stack.pop());
        }

        return result;
    }

    /**
     * 查找两个节点之间的所有路径
     */
    public List<List<String>> findPathsBetweenNodes(String startNodeId, String endNodeId,
                                                    WorkflowGraphVO graph) {
        if (graph == null || graph.getNodes() == null || graph.getEdges() == null) {
            return Collections.emptyList();
        }

        Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);
        return findPathsBetweenNodes(startNodeId, endNodeId, adjacencyList);
    }

    /**
     * 计算图的连通分量
     */
    public int countConnectedComponents(WorkflowGraphVO graph) {
        if (graph == null || graph.getNodes() == null) {
            return 0;
        }

        Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);
        Set<String> visited = new HashSet<>();
        int components = 0;

        for (String nodeId : adjacencyList.keySet()) {
            if (!visited.contains(nodeId)) {
                // DFS遍历连通分量
                dfs(nodeId, adjacencyList, visited);
                components++;
            }
        }

        // 考虑孤立的节点
        for (WorkflowGraphVO.Node node : graph.getNodes()) {
            if (!visited.contains(node.getId())) {
                components++;
            }
        }

        return components;
    }

    /**
     * 计算节点的中心性指标
     */
    public Map<String, Double> calculateNodeCentrality(WorkflowGraphVO graph) {
        Map<String, Double> centralityMap = new HashMap<>();

        if (graph == null || graph.getNodes() == null || graph.getEdges() == null) {
            return centralityMap;
        }

        Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);

        // 计算度数中心性
        for (WorkflowGraphVO.Node node : graph.getNodes()) {
            String nodeId = node.getId();
            List<String> neighbors = adjacencyList.get(nodeId);
            int degree = (neighbors != null ? neighbors.size() : 0);

            // 简单中心性：度数 / (节点数-1)
            double centrality = (graph.getNodes().size() > 1) ?
                    degree / (double) (graph.getNodes().size() - 1) : 0;

            centralityMap.put(nodeId, centrality);
        }

        return centralityMap;
    }

    // ========== 私有方法 ==========

    private int calculateDepthFromNode(String nodeId, WorkflowGraphVO graph,
                                       Map<String, Integer> memo) {
        if (memo.containsKey(nodeId)) {
            return memo.get(nodeId);
        }

        // 找出所有出边
        List<WorkflowGraphVO.Edge> outgoingEdges = graph.getEdges().stream()
                .filter(edge -> edge.getSource().equals(nodeId))
                .toList();

        if (outgoingEdges.isEmpty()) {
            memo.put(nodeId, 1);
            return 1;
        }

        int maxDepth = 0;
        for (WorkflowGraphVO.Edge edge : outgoingEdges) {
            int depth = calculateDepthFromNode(edge.getTarget(), graph, memo);
            maxDepth = Math.max(maxDepth, depth);
        }

        memo.put(nodeId, maxDepth + 1);
        return maxDepth + 1;
    }

    private Map<String, List<String>> buildAdjacencyList(WorkflowGraphVO graph) {
        Map<String, List<String>> adjacencyList = new HashMap<>();

        // 初始化所有节点
        for (WorkflowGraphVO.Node node : graph.getNodes()) {
            adjacencyList.put(node.getId(), new ArrayList<>());
        }

        // 添加边
        for (WorkflowGraphVO.Edge edge : graph.getEdges()) {
            adjacencyList.get(edge.getSource()).add(edge.getTarget());
        }

        return adjacencyList;
    }

    private List<List<String>> findPathsBetweenNodes(String startNodeId, String endNodeId,
                                                     Map<String, List<String>> adjacencyList) {
        List<List<String>> allPaths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        List<String> currentPath = new ArrayList<>();

        dfsFindPaths(startNodeId, endNodeId, adjacencyList, visited, currentPath, allPaths);

        return allPaths;
    }

    private void dfsFindPaths(String currentNode, String endNode,
                              Map<String, List<String>> adjacencyList,
                              Set<String> visited,
                              List<String> currentPath,
                              List<List<String>> allPaths) {
        visited.add(currentNode);
        currentPath.add(currentNode);

        if (currentNode.equals(endNode)) {
            allPaths.add(new ArrayList<>(currentPath));
        } else {
            List<String> neighbors = adjacencyList.get(currentNode);
            if (neighbors != null) {
                for (String neighbor : neighbors) {
                    if (!visited.contains(neighbor)) {
                        dfsFindPaths(neighbor, endNode, adjacencyList, visited, currentPath, allPaths);
                    }
                }
            }
        }

        currentPath.remove(currentPath.size() - 1);
        visited.remove(currentNode);
    }

    private boolean hasCycleUtil(String nodeId, Map<String, List<String>> adjacencyList,
                                 Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }

        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        List<String> neighbors = adjacencyList.get(nodeId);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (hasCycleUtil(neighbor, adjacencyList, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }

    private void topologicalSortUtil(String nodeId, Map<String, List<String>> adjacencyList,
                                     Set<String> visited, Stack<String> stack) {
        visited.add(nodeId);

        List<String> neighbors = adjacencyList.get(nodeId);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    topologicalSortUtil(neighbor, adjacencyList, visited, stack);
                }
            }
        }

        stack.push(nodeId);
    }

    private void dfs(String nodeId, Map<String, List<String>> adjacencyList,
                     Set<String> visited) {
        visited.add(nodeId);

        List<String> neighbors = adjacencyList.get(nodeId);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    dfs(neighbor, adjacencyList, visited);
                }
            }
        }
    }
}
