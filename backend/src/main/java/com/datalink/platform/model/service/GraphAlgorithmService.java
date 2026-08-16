package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.model.dto.ImpactVO;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.PathVO;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Relation;
import com.datalink.platform.model.entity.RouteNode;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.RelationMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.model.mapper.RouteNodeMapper;
import com.datalink.platform.monitor.dto.InstanceVO;
import com.datalink.platform.monitor.service.InstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 图算法服务（M3 择路深化）：路径查询 + 影响面分析。
 * 路径查询基于 relation 有向边做 DFS 枚举简单路径；影响面基于 BFS 下游扩散。
 */
@Service
@RequiredArgsConstructor
public class GraphAlgorithmService {

    /** 路径查询默认深度上限（节点数） */
    private static final int DEFAULT_MAX_DEPTH = 8;
    /** 路径查询最多返回条数 */
    private static final int MAX_PATHS = 20;

    private final NodeMapper nodeMapper;
    private final RelationMapper relationMapper;
    private final RouteMapper routeMapper;
    private final RouteNodeMapper routeNodeMapper;
    private final GraphService graphService;
    private final InstanceService instanceService;

    /**
     * 路径查询：DFS 枚举 from→to 的全部简单路径（不重复节点），深度上限 maxDepth（节点数，默认 8），最多返回 20 条。
     * 无路径返回空 list；每条 PathVO 的 nodeIds/nodeNames 按序，length 为节点数。
     */
    public List<PathVO> queryPaths(Long fromId, Long toId, int maxDepth) {
        if (maxDepth <= 0) {
            maxDepth = DEFAULT_MAX_DEPTH;
        }
        // 邻接表：from_node_id → to_node_id 列表（按 relation id 升序，保证结果稳定）
        Map<Long, List<Long>> adj = new HashMap<>();
        for (Relation r : relationMapper.selectList(Wrappers.lambdaQuery(Relation.class).orderByAsc(Relation::getId))) {
            adj.computeIfAbsent(r.getFromNodeId(), k -> new ArrayList<>()).add(r.getToNodeId());
        }
        // 节点名映射
        Map<Long, String> nameMap = nodeMapper.selectList(Wrappers.lambdaQuery(Node.class))
                .stream().collect(Collectors.toMap(Node::getId, n -> n.getName() == null ? "" : n.getName()));

        List<List<Long>> paths = new ArrayList<>();
        dfs(fromId, toId, maxDepth, adj, new HashSet<>(), new ArrayList<>(), paths);

        List<PathVO> vos = new ArrayList<>(paths.size());
        for (List<Long> p : paths) {
            List<String> nodeIds = p.stream().map(String::valueOf).collect(Collectors.toList());
            List<String> nodeNames = p.stream().map(id -> nameMap.getOrDefault(id, String.valueOf(id)))
                    .collect(Collectors.toList());
            vos.add(new PathVO(nodeIds, nodeNames, p.size()));
        }
        return vos;
    }

    /**
     * 影响面分析：返回节点全部下游（不含自身）、受影响路线（经过自身∪下游任一站点）与受影响实例（当前站点∈自身∪下游）。
     * 节点不存在抛 BusinessException(NOT_FOUND)。
     */
    public ImpactVO impact(Long nodeId) {
        Node node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "节点不存在");
        }

        // 1. BFS 全部下游（不含自身）
        Set<Long> downstreamIds = bfsDownstream(nodeId);

        // 影响站点集合：自身 ∪ 下游
        Set<Long> affectedNodeIds = new LinkedHashSet<>();
        affectedNodeIds.add(nodeId);
        affectedNodeIds.addAll(downstreamIds);

        // 2. 受影响路线：route_node 中 node_id ∈ 影响站点集合的 route_id
        List<Long> affectedRouteIds = affectedNodeIds.isEmpty() ? new ArrayList<>()
                : routeNodeMapper.selectList(Wrappers.lambdaQuery(RouteNode.class)
                        .in(RouteNode::getNodeId, affectedNodeIds))
                        .stream().map(RouteNode::getRouteId).distinct().sorted().collect(Collectors.toList());
        List<RouteVO> affectedRoutes = graphService.routes().stream()
                .filter(r -> affectedRouteIds.contains(Long.valueOf(r.getId())))
                .collect(Collectors.toList());

        // 3. 受影响实例：current_node_id ∈ 影响站点集合（复用 InstanceService 装配）
        Set<String> affectedNodeIdStrs = affectedNodeIds.stream().map(String::valueOf).collect(Collectors.toSet());
        List<InstanceVO> affectedInstances = instanceService.page(1, 500, null).getRecords().stream()
                .filter(vo -> vo.getCurrentNodeId() != null && affectedNodeIdStrs.contains(vo.getCurrentNodeId()))
                .collect(Collectors.toList());

        // 下游节点视图
        List<NodeVO> downstream = downstreamIds.isEmpty() ? new ArrayList<>()
                : nodeMapper.selectBatchIds(downstreamIds).stream().map(this::toNodeVO).collect(Collectors.toList());

        ImpactVO vo = new ImpactVO();
        vo.setDownstream(downstream);
        vo.setAffectedInstances(affectedInstances);
        vo.setAffectedRoutes(affectedRoutes);
        return vo;
    }

    /** DFS 枚举简单路径：visited 记录当前路径已访问节点，path 为当前路径（按序） */
    private void dfs(Long cur, Long to, int maxDepth, Map<Long, List<Long>> adj,
                     Set<Long> visited, List<Long> path, List<List<Long>> result) {
        if (result.size() >= MAX_PATHS) {
            return;
        }
        visited.add(cur);
        path.add(cur);
        if (cur.equals(to)) {
            result.add(new ArrayList<>(path));
        } else if (path.size() < maxDepth) {
            for (Long next : adj.getOrDefault(cur, Collections.emptyList())) {
                if (!visited.contains(next)) {
                    dfs(next, to, maxDepth, adj, visited, path, result);
                    if (result.size() >= MAX_PATHS) {
                        break;
                    }
                }
            }
        }
        path.remove(path.size() - 1);
        visited.remove(cur);
    }

    /** BFS 沿 relation.from→to 收集全部下游节点（去重、不含自身） */
    private Set<Long> bfsDownstream(Long nodeId) {
        Map<Long, List<Long>> adj = new HashMap<>();
        for (Relation r : relationMapper.selectList(Wrappers.lambdaQuery(Relation.class).orderByAsc(Relation::getId))) {
            adj.computeIfAbsent(r.getFromNodeId(), k -> new ArrayList<>()).add(r.getToNodeId());
        }
        Set<Long> visited = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(nodeId);
        while (!queue.isEmpty()) {
            Long cur = queue.poll();
            for (Long next : adj.getOrDefault(cur, Collections.emptyList())) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
        return visited;
    }

    /** 节点实体转视图（id 转字符串） */
    private NodeVO toNodeVO(Node n) {
        NodeVO vo = new NodeVO();
        vo.setId(String.valueOf(n.getId()));
        vo.setName(n.getName());
        vo.setCode(n.getCode());
        vo.setNodeType(n.getNodeType());
        vo.setLevel(n.getLevel());
        vo.setStatus(n.getStatus());
        vo.setOwner(n.getOwner());
        vo.setDescription(n.getDescription());
        return vo;
    }
}
