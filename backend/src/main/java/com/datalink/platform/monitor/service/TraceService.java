package com.datalink.platform.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.model.dto.CheckpointVO;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Relation;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.entity.RouteNode;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.RelationMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.model.mapper.RouteNodeMapper;
import com.datalink.platform.monitor.dto.TraceResultVO;
import com.datalink.platform.monitor.entity.Checkpoint;
import com.datalink.platform.monitor.entity.CheckResult;
import com.datalink.platform.monitor.mapper.CheckpointMapper;
import com.datalink.platform.monitor.mapper.CheckResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 链路追踪服务：沿 relation 有向边做上下游各 2 层 BFS（顺藤摸瓜），
 * 节点附带检测点与最近检测状态；另支持查询某节点所属路线。
 */
@Service
@RequiredArgsConstructor
public class TraceService {

    private final RelationMapper relationMapper;
    private final NodeMapper nodeMapper;
    private final RouteMapper routeMapper;
    private final RouteNodeMapper routeNodeMapper;
    private final CheckpointMapper checkpointMapper;
    private final CheckResultMapper checkResultMapper;

    /** 给定站点：上游（to→from）与下游（from→to）各取 2 层，去重后返回节点视图 */
    public TraceResultVO trace(Long nodeId) {
        // 上游：先取直接上游（to_node_id=nodeId），再对其向上 1 层
        Set<Long> upstreamIds = new LinkedHashSet<>();
        List<Long> up1 = relationMapper.selectList(Wrappers.lambdaQuery(Relation.class)
                        .eq(Relation::getToNodeId, nodeId))
                .stream().map(Relation::getFromNodeId).distinct().collect(Collectors.toList());
        upstreamIds.addAll(up1);
        if (!up1.isEmpty()) {
            List<Relation> up2 = relationMapper.selectList(Wrappers.lambdaQuery(Relation.class)
                    .in(Relation::getToNodeId, up1));
            for (Relation r : up2) {
                upstreamIds.add(r.getFromNodeId());
            }
        }

        // 下游：先取直接下游（from_node_id=nodeId），再对其向下 1 层
        Set<Long> downstreamIds = new LinkedHashSet<>();
        List<Long> down1 = relationMapper.selectList(Wrappers.lambdaQuery(Relation.class)
                        .eq(Relation::getFromNodeId, nodeId))
                .stream().map(Relation::getToNodeId).distinct().collect(Collectors.toList());
        downstreamIds.addAll(down1);
        if (!down1.isEmpty()) {
            List<Relation> down2 = relationMapper.selectList(Wrappers.lambdaQuery(Relation.class)
                    .in(Relation::getFromNodeId, down1));
            for (Relation r : down2) {
                downstreamIds.add(r.getToNodeId());
            }
        }

        TraceResultVO vo = new TraceResultVO();
        vo.setNodeId(String.valueOf(nodeId));
        vo.setUpstream(toNodeVOs(upstreamIds));
        vo.setDownstream(toNodeVOs(downstreamIds));
        return vo;
    }

    /** 某节点所属的全部路线（nodeIds 按 seq 有序） */
    public List<RouteVO> routesOfNode(Long nodeId) {
        List<RouteNode> rns = routeNodeMapper.selectList(Wrappers.lambdaQuery(RouteNode.class)
                .eq(RouteNode::getNodeId, nodeId));
        if (rns.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> routeIds = rns.stream().map(RouteNode::getRouteId).distinct().sorted().collect(Collectors.toList());
        List<Route> routes = routeMapper.selectBatchIds(routeIds);
        return routes.stream().map(this::toRouteVO).collect(Collectors.toList());
    }

    private List<NodeVO> toNodeVOs(Set<Long> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        return nodeMapper.selectBatchIds(ids).stream().map(this::toNodeVO).collect(Collectors.toList());
    }

    /** 节点视图：附带该节点检测点及最近检测状态（可疑上游可看出检测点异常） */
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

        List<Checkpoint> cps = checkpointMapper.selectList(Wrappers.lambdaQuery(Checkpoint.class)
                .eq(Checkpoint::getNodeId, n.getId()));
        List<CheckpointVO> cpsVO = new ArrayList<>(cps.size());
        for (Checkpoint cp : cps) {
            CheckpointVO cv = new CheckpointVO();
            cv.setId(String.valueOf(cp.getId()));
            cv.setName(cp.getName());
            cv.setKind(cp.getKind());
            cv.setCheckType(cp.getCheckType());
            List<CheckResult> latest = checkResultMapper.selectList(Wrappers.lambdaQuery(CheckResult.class)
                    .eq(CheckResult::getCheckpointId, cp.getId())
                    .orderByDesc(CheckResult::getCheckTime)
                    .last("LIMIT 1"));
            cv.setStatus(latest.isEmpty() ? null : latest.get(0).getStatus());
            cpsVO.add(cv);
        }
        vo.setCheckpoints(cpsVO);
        return vo;
    }

    private RouteVO toRouteVO(Route r) {
        RouteVO vo = new RouteVO();
        vo.setId(String.valueOf(r.getId()));
        vo.setProcessId(r.getProcessId() == null ? null : String.valueOf(r.getProcessId()));
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
