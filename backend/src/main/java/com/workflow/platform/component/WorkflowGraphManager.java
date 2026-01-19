package com.workflow.platform.component;
//工作流图管理器

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.platform.model.vo.WorkflowGraphVO;
import com.workflow.platform.model.vo.WorkflowGraphVO.Edge;
import com.workflow.platform.model.vo.WorkflowGraphVO.GraphLayout;
import com.workflow.platform.model.vo.WorkflowGraphVO.Node;

// import com.workflow.platform.util.WorkflowGraphUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 工作流图管理器 - 管理工作流的图结构和可视化数据
 */
@Slf4j
@Component
public class WorkflowGraphManager {

	@Autowired
	private ObjectMapper objectMapper;

	//	@Autowired
	// private WorkflowGraphUtil workflowGraphUtil;

	// 缓存工作流图数据
	private final Map<String, WorkflowGraphVO> workflowGraphCache = new ConcurrentHashMap<>();

	// 缓存图布局
	private final Map<String, GraphLayout> layoutCache = new ConcurrentHashMap<>();

	/**
	 * 构建工作流图
	 */
	public WorkflowGraphVO buildWorkflowGraph(String workflowId,
			List<Node> nodes,
			List<Edge> edges) {
		String cacheKey = getCacheKey(workflowId);

		// 检查缓存
		if (workflowGraphCache.containsKey(cacheKey)) {
			return workflowGraphCache.get(cacheKey);
		}

		// 构建新的图
		WorkflowGraphVO graph = WorkflowGraphVO.builder()
				.workflowId(workflowId)
				.nodes(nodes)
				.edges(edges)
				.createdTime(new Date())
				.build();

		// 计算布局
		GraphLayout layout = calculateLayout(nodes, edges);
		graph.setLayout(layout);

		// 计算统计信息
		calculateStatistics(graph);

		// 缓存结果
		workflowGraphCache.put(cacheKey, graph);
		layoutCache.put(cacheKey, layout);

		log.info("工作流图构建完成: workflowId={}, 节点数={}, 边数={}",
				workflowId, nodes.size(), edges.size());

		return graph;
	}

	/**
	 * 获取工作流图
	 */
	public WorkflowGraphVO getWorkflowGraph(String workflowId) {
		String cacheKey = getCacheKey(workflowId);
		return workflowGraphCache.get(cacheKey);
	}

	/**
	 * 更新工作流图
	 */
	public WorkflowGraphVO updateWorkflowGraph(String workflowId,
			List<Node> nodes,
			List<Edge> edges) {
		String cacheKey = getCacheKey(workflowId);

		WorkflowGraphVO existingGraph = workflowGraphCache.get(cacheKey);
		if (existingGraph != null) {
			existingGraph.setNodes(nodes);
			existingGraph.setEdges(edges);
			existingGraph.setUpdatedTime(new Date());

			// 重新计算布局和统计
			GraphLayout layout = calculateLayout(nodes, edges);
			existingGraph.setLayout(layout);
			calculateStatistics(existingGraph);

			layoutCache.put(cacheKey, layout);

			log.info("工作流图更新完成: workflowId={}", workflowId);
			return existingGraph;
		} else {
			return buildWorkflowGraph(workflowId, nodes, edges);
		}
	}

	/**
	 * 清除工作流图缓存
	 */
	public void clearWorkflowGraphCache(String workflowId) {
		String cacheKey = getCacheKey(workflowId);
		workflowGraphCache.remove(cacheKey);
		layoutCache.remove(cacheKey);
		log.debug("清除工作流图缓存: {}", workflowId);
	}

	/**
	 * 清除所有工作流图缓存
	 */
	public void clearAllCache() {
		workflowGraphCache.clear();
		layoutCache.clear();
		log.info("清除所有工作流图缓存");
	}

	/**
	 * 导出工作流图为JSON
	 */
	public String exportGraphToJson(String workflowId) {
		WorkflowGraphVO graph = getWorkflowGraph(workflowId);
		if (graph == null) {
			return null;
		}

		try {
			return objectMapper.writeValueAsString(graph);
		} catch (Exception e) {
			log.error("导出工作流图失败: workflowId={}, error={}", workflowId, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * 从JSON导入工作流图
	 */
	public WorkflowGraphVO importGraphFromJson(String workflowId, String json) {
		try {
			WorkflowGraphVO graph = objectMapper.readValue(json, WorkflowGraphVO.class);
			graph.setWorkflowId(workflowId);
			graph.setUpdatedTime(new Date());

			// 缓存图数据
			String cacheKey = getCacheKey(workflowId);
			workflowGraphCache.put(cacheKey, graph);

			// 缓存布局
			if (graph.getLayout() == null) {
				GraphLayout layout = calculateLayout(graph.getNodes(), graph.getEdges());
				graph.setLayout(layout);
				layoutCache.put(cacheKey, layout);
			}

			log.info("工作流图导入成功: workflowId={}", workflowId);
			return graph;

		} catch (Exception e) {
			log.error("导入工作流图失败: workflowId={}, error={}", workflowId, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * 查找工作流中的路径
	 */
	public List<List<String>> findPaths(String workflowId, String startNodeId, String endNodeId) {
		WorkflowGraphVO graph = getWorkflowGraph(workflowId);
		if (graph == null) {
			return Collections.emptyList();
		}

		// 构建邻接表
		Map<String, List<String>> adjacencyList = buildAdjacencyList(graph);

		// 使用DFS查找所有路径
		List<List<String>> allPaths = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		List<String> currentPath = new ArrayList<>();

		dfsFindPaths(startNodeId, endNodeId, adjacencyList, visited, currentPath, allPaths);

		log.debug("找到路径数: workflowId={}, start={}, end={}, paths={}",
				workflowId, startNodeId, endNodeId, allPaths.size());

		return allPaths;
	}

	/**
	 * 检查工作流中是否有环
	 */
	public boolean hasCycle(String workflowId) {
		WorkflowGraphVO graph = getWorkflowGraph(workflowId);
		if (graph == null) {
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
	 * 获取工作流图的拓扑排序
	 */
	public List<String> topologicalSort(String workflowId) {
		WorkflowGraphVO graph = getWorkflowGraph(workflowId);
		if (graph == null) {
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

	// ========== 私有方法 ==========

	private String getCacheKey(String workflowId) {
		return "workflow_graph:" + workflowId;
	}

	private GraphLayout calculateLayout(List<Node> nodes, List<Edge> edges) {
		// 使用Dagre布局算法计算节点位置
		// 这里简化为层次布局
		GraphLayout layout = new GraphLayout();
		layout.setAlgorithm("dagre");
		layout.setWidth(1200);
		layout.setHeight(800);
		layout.setPadding(50);

		Map<String, GraphLayout.NodePosition> positions = new HashMap<>();

		// 简单层次布局：按节点类型和连接关系分层
		Map<String, Integer> nodeLevels = new HashMap<>();
		Map<Integer, List<Node>> levelNodes = new HashMap<>();

		// 计算节点的层次
		for (Node node : nodes) {
			int level = calculateNodeLevel(node.getId(), edges, nodeLevels);
			nodeLevels.put(node.getId(), level);
			levelNodes.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
		}

		// 计算节点位置
		int maxLevel = levelNodes.keySet().stream().max(Integer::compareTo).orElse(0);
		int levelHeight = 600 / Math.max(1, maxLevel + 1);
		int nodeWidth = 150;
		int nodeHeight = 80;

		for (Map.Entry<Integer, List<Node>> entry : levelNodes.entrySet()) {
			int level = entry.getKey();
			List<Node> levelNodeList = entry.getValue();

			int levelWidth = 800;
			int xSpacing = levelWidth / Math.max(1, levelNodeList.size() + 1);
			double y = level * levelHeight + 100;

			for (int i = 0; i < levelNodeList.size(); i++) {
				Node node = levelNodeList.get(i);
				double x = xSpacing * (i + 1);

				GraphLayout.NodePosition position = new GraphLayout.NodePosition();
				position.setX(x);
				position.setY(y);
				position.setWidth(nodeWidth);
				position.setHeight(nodeHeight);

				positions.put(node.getId(), position);
			}
		}

		layout.setNodePositions(positions);
		return layout;
	}

	private int calculateNodeLevel(String nodeId, List<Edge> edges, Map<String, Integer> nodeLevels) {
		if (nodeLevels.containsKey(nodeId)) {
			return nodeLevels.get(nodeId);
		}

		// 查找所有指向该节点的边
		List<Edge> incomingEdges = edges.stream()
				.filter(edge -> edge.getTarget().equals(nodeId))
				.toList();

		if (incomingEdges.isEmpty()) {
			// 没有入边，是第一层
			return 0;
		}

		// 计算所有前置节点的最大层次 + 1
		int maxLevel = 0;
		for (Edge edge : incomingEdges) {
			String sourceId = edge.getSource();
			int sourceLevel = calculateNodeLevel(sourceId, edges, nodeLevels);
			maxLevel = Math.max(maxLevel, sourceLevel);
		}

		return maxLevel + 1;
	}

	private void calculateStatistics(WorkflowGraphVO graph) {
		Map<String, Object> stats = new HashMap<>();

		// 节点统计
		stats.put("totalNodes", graph.getNodes().size());

		// 按类型统计节点
		Map<String, Long> nodeTypeCount = graph.getNodes().stream()
				.collect(java.util.stream.Collectors.groupingBy(Node::getType,
						java.util.stream.Collectors.counting()));
		stats.put("nodeTypeCount", nodeTypeCount);

		// 边统计
		stats.put("totalEdges", graph.getEdges().size());

		// 计算节点度数
		Map<String, Integer> inDegree = new HashMap<>();
		Map<String, Integer> outDegree = new HashMap<>();

		for (Edge edge : graph.getEdges()) {
			inDegree.put(edge.getTarget(), inDegree.getOrDefault(edge.getTarget(), 0) + 1);
			outDegree.put(edge.getSource(), outDegree.getOrDefault(edge.getSource(), 0) + 1);
		}

		stats.put("maxInDegree", inDegree.values().stream().max(Integer::compareTo).orElse(0));
		stats.put("maxOutDegree", outDegree.values().stream().max(Integer::compareTo).orElse(0));

		// 计算图的深度
		int graphDepth = calculateGraphDepth(graph);
		stats.put("graphDepth", graphDepth);

		graph.setStatistics(stats);
	}

	private int calculateGraphDepth(WorkflowGraphVO graph) {
		if (graph.getNodes().isEmpty()) {
			return 0;
		}

		// 找到所有起点（没有入边的节点）
		Set<String> startNodes = new HashSet<>();
		for (Node node : graph.getNodes()) {
			startNodes.add(node.getId());
		}

		for (Edge edge : graph.getEdges()) {
			startNodes.remove(edge.getTarget());
		}

		// 从每个起点计算最大深度
		int maxDepth = 0;
		for (String startNode : startNodes) {
			int depth = calculateDepthFromNode(startNode, graph.getEdges(), new HashMap<>());
			maxDepth = Math.max(maxDepth, depth);
		}

		return maxDepth;
	}

	private int calculateDepthFromNode(String nodeId, List<Edge> edges, Map<String, Integer> memo) {
		if (memo.containsKey(nodeId)) {
			return memo.get(nodeId);
		}

		// 找出所有出边
		List<Edge> outgoingEdges = edges.stream()
				.filter(edge -> edge.getSource().equals(nodeId))
				.toList();

		if (outgoingEdges.isEmpty()) {
			memo.put(nodeId, 1);
			return 1;
		}

		int maxDepth = 0;
		for (Edge edge : outgoingEdges) {
			int depth = calculateDepthFromNode(edge.getTarget(), edges, memo);
			maxDepth = Math.max(maxDepth, depth);
		}

		memo.put(nodeId, maxDepth + 1);
		return maxDepth + 1;
	}

	private Map<String, List<String>> buildAdjacencyList(WorkflowGraphVO graph) {
		Map<String, List<String>> adjacencyList = new HashMap<>();

		// 初始化所有节点
		for (Node node : graph.getNodes()) {
			adjacencyList.put(node.getId(), new ArrayList<>());
		}

		// 添加边
		for (Edge edge : graph.getEdges()) {
			adjacencyList.get(edge.getSource()).add(edge.getTarget());
		}

		return adjacencyList;
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
}