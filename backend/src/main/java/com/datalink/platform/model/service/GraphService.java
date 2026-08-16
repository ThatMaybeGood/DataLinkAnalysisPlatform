package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.InstanceStatsVO;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.ProcessVO;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Process;
import com.datalink.platform.model.entity.Relation;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.entity.RouteNode;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.model.mapper.RelationMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.model.mapper.RouteNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 建模域只读装配服务：路网节点 / 边 / 流程 / 路线查询。
 * M1 阶段无实例运行数据，instanceStats 固定全 0。
 */
@Service
@RequiredArgsConstructor
public class GraphService {

    /** 与 Jackson 全局格式保持一致（yyyy-MM-dd HH:mm:ss） */
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NodeMapper nodeMapper;
    private final RelationMapper relationMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;
    private final RouteNodeMapper routeNodeMapper;

    /** 全部站点节点（按 id 升序），id 转字符串、检测点恒为空 */
    public List<NodeVO> nodes() {
        return nodeMapper.selectList(Wrappers.lambdaQuery(Node.class).orderByAsc(Node::getId))
                .stream().map(this::toNodeVO).collect(Collectors.toList());
    }

    /** 全部路网边（id/source/target 均转字符串） */
    public List<EdgeVO> edges() {
        return relationMapper.selectList(Wrappers.lambdaQuery(Relation.class).orderByAsc(Relation::getId))
                .stream().map(this::toEdgeVO).collect(Collectors.toList());
    }

    /** 全部流程：装配起点/终点名、路线数、去重站点数、实例统计（全 0） */
    public List<ProcessVO> processes() {
        Map<Long, String> nodeNameMap = nodeMapper.selectList(Wrappers.lambdaQuery(Node.class))
                .stream().collect(Collectors.toMap(Node::getId, n -> n.getName() == null ? "" : n.getName()));
        return processMapper.selectList(Wrappers.lambdaQuery(Process.class).orderByAsc(Process::getId))
                .stream().map(p -> toProcessVO(p, nodeNameMap)).collect(Collectors.toList());
    }

    /** 全部路线：装配有序站点 nodeIds（String 列表） */
    public List<RouteVO> routes() {
        return routeMapper.selectList(Wrappers.lambdaQuery(Route.class).orderByAsc(Route::getId))
                .stream().map(this::toRouteVO).collect(Collectors.toList());
    }

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

    private EdgeVO toEdgeVO(Relation r) {
        EdgeVO vo = new EdgeVO();
        vo.setId(String.valueOf(r.getId()));
        vo.setSource(String.valueOf(r.getFromNodeId()));
        vo.setTarget(String.valueOf(r.getToNodeId()));
        vo.setRelationType(r.getRelationType());
        return vo;
    }

    private ProcessVO toProcessVO(Process p, Map<Long, String> nodeNameMap) {
        ProcessVO vo = new ProcessVO();
        vo.setId(String.valueOf(p.getId()));
        vo.setName(p.getName());
        vo.setScene(p.getScene());
        vo.setLevel(p.getLevel());
        vo.setDescription(p.getDescription());
        vo.setStartNodeName(p.getStartNodeId() == null ? null : nodeNameMap.get(p.getStartNodeId()));
        vo.setEndNodeName(p.getEndNodeId() == null ? null : nodeNameMap.get(p.getEndNodeId()));
        Long routeCount = routeMapper.selectCount(Wrappers.lambdaQuery(Route.class).eq(Route::getProcessId, p.getId()));
        vo.setRouteCount(routeCount == null ? 0 : routeCount.intValue());
        vo.setNodeCount(routeNodeMapper.countDistinctNodeByProcessId(p.getId()));
        vo.setInstanceStats(new InstanceStatsVO());
        vo.setUpdatedAt(p.getUpdatedAt() == null ? null : p.getUpdatedAt().format(DATETIME_FMT));
        return vo;
    }

    private RouteVO toRouteVO(Route r) {
        RouteVO vo = new RouteVO();
        vo.setId(String.valueOf(r.getId()));
        vo.setProcessId(String.valueOf(r.getProcessId()));
        vo.setName(r.getName());
        vo.setPriority(r.getPriority());
        vo.setStatus(r.getStatus());
        List<RouteNode> rns = routeNodeMapper.selectList(Wrappers.lambdaQuery(RouteNode.class)
                .eq(RouteNode::getRouteId, r.getId()).orderByAsc(RouteNode::getSeq));
        List<String> nodeIds = new ArrayList<>(rns.size());
        for (RouteNode rn : rns) {
            nodeIds.add(String.valueOf(rn.getNodeId()));
        }
        vo.setNodeIds(nodeIds);
        return vo;
    }
}
