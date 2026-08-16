package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.InstanceStatsVO;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.ProcessVO;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.dto.SaveNodeRequest;
import com.datalink.platform.model.dto.SaveProcessRequest;
import com.datalink.platform.model.dto.SaveRelationRequest;
import com.datalink.platform.model.dto.SaveRouteRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 建模域 CRUD 服务：节点 / 路网边 / 流程 / 路线建模维护。
 * 路线站点（route_node）随路线联动写入与重写；删除流程时级联清理其下所有路线及路线站点。
 */
@Service
@RequiredArgsConstructor
public class ModelingService {

    /** 与 Jackson 全局格式保持一致（yyyy-MM-dd HH:mm:ss） */
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NodeMapper nodeMapper;
    private final RelationMapper relationMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;
    private final RouteNodeMapper routeNodeMapper;

    // ---------- 节点 ----------

    /** 新建节点：level 缺省 L3、status 缺省 ACTIVE */
    public NodeVO createNode(SaveNodeRequest req) {
        Node n = new Node();
        n.setName(req.getName());
        n.setNodeType(req.getNodeType());
        n.setCode(req.getCode());
        n.setLevel(req.getLevel() == null || req.getLevel().isBlank() ? "L3" : req.getLevel());
        n.setStatus(req.getStatus() == null || req.getStatus().isBlank() ? "ACTIVE" : req.getStatus());
        n.setOwner(req.getOwner());
        n.setDescription(req.getDescription());
        nodeMapper.insert(n);
        return toNodeVO(n);
    }

    /** 更新节点：非空字段覆盖，不存在抛 404 */
    public NodeVO updateNode(Long id, SaveNodeRequest req) {
        Node n = requireNode(id);
        if (req.getName() != null) {
            n.setName(req.getName());
        }
        if (req.getNodeType() != null) {
            n.setNodeType(req.getNodeType());
        }
        if (req.getCode() != null) {
            n.setCode(req.getCode());
        }
        if (req.getLevel() != null && !req.getLevel().isBlank()) {
            n.setLevel(req.getLevel());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            n.setStatus(req.getStatus());
        }
        if (req.getOwner() != null) {
            n.setOwner(req.getOwner());
        }
        if (req.getDescription() != null) {
            n.setDescription(req.getDescription());
        }
        nodeMapper.updateById(n);
        return toNodeVO(n);
    }

    /** 删除节点 */
    public void deleteNode(Long id) {
        nodeMapper.deleteById(id);
    }

    // ---------- 路网边 ----------

    /** 新建路网边 */
    public EdgeVO createRelation(SaveRelationRequest req) {
        Relation r = new Relation();
        r.setFromNodeId(req.getFromNodeId());
        r.setToNodeId(req.getToNodeId());
        r.setRelationType(req.getRelationType());
        r.setLevel(req.getLevel());
        r.setDescription(req.getDescription());
        r.setStatus("ACTIVE");
        relationMapper.insert(r);
        return toEdgeVO(r);
    }

    /** 删除路网边 */
    public void deleteRelation(Long id) {
        relationMapper.deleteById(id);
    }

    // ---------- 流程 ----------

    /** 新建流程 */
    public ProcessVO createProcess(SaveProcessRequest req) {
        Process p = new Process();
        p.setName(req.getName());
        p.setScene(req.getScene());
        p.setLevel(req.getLevel());
        p.setDescription(req.getDescription());
        p.setStartNodeId(req.getStartNodeId());
        p.setEndNodeId(req.getEndNodeId());
        processMapper.insert(p);
        return toProcessVO(p, nodeNameMap());
    }

    /** 更新流程：非空字段覆盖，不存在抛 404 */
    public ProcessVO updateProcess(Long id, SaveProcessRequest req) {
        Process p = requireProcess(id);
        if (req.getName() != null) {
            p.setName(req.getName());
        }
        if (req.getScene() != null) {
            p.setScene(req.getScene());
        }
        if (req.getLevel() != null && !req.getLevel().isBlank()) {
            p.setLevel(req.getLevel());
        }
        if (req.getDescription() != null) {
            p.setDescription(req.getDescription());
        }
        if (req.getStartNodeId() != null) {
            p.setStartNodeId(req.getStartNodeId());
        }
        if (req.getEndNodeId() != null) {
            p.setEndNodeId(req.getEndNodeId());
        }
        processMapper.updateById(p);
        return toProcessVO(p, nodeNameMap());
    }

    /** 删除流程：级联删除其下所有路线及路线站点，再删流程 */
    @Transactional
    public void deleteProcess(Long id) {
        requireProcess(id);
        List<Route> routes = routeMapper.selectList(Wrappers.lambdaQuery(Route.class).eq(Route::getProcessId, id));
        for (Route route : routes) {
            routeNodeMapper.delete(Wrappers.lambdaQuery(RouteNode.class).eq(RouteNode::getRouteId, route.getId()));
        }
        routeMapper.delete(Wrappers.lambdaQuery(Route.class).eq(Route::getProcessId, id));
        processMapper.deleteById(id);
    }

    // ---------- 路线 ----------

    /** 新建路线：同时按序写入路线站点（seq 从 1 起） */
    @Transactional
    public RouteVO createRoute(SaveRouteRequest req) {
        Route r = new Route();
        r.setProcessId(req.getProcessId());
        r.setName(req.getName());
        r.setPriority(req.getPriority());
        r.setStatus(req.getStatus());
        routeMapper.insert(r);
        insertRouteNodes(r.getId(), req.getNodeIds());
        return toRouteVO(r);
    }

    /** 更新路线：覆盖更新站点顺序（删旧插新），不存在抛 404 */
    @Transactional
    public RouteVO updateRoute(Long id, SaveRouteRequest req) {
        Route r = requireRoute(id);
        r.setProcessId(req.getProcessId());
        r.setName(req.getName());
        if (req.getPriority() != null) {
            r.setPriority(req.getPriority());
        }
        if (req.getStatus() != null) {
            r.setStatus(req.getStatus());
        }
        routeMapper.updateById(r);
        routeNodeMapper.delete(Wrappers.lambdaQuery(RouteNode.class).eq(RouteNode::getRouteId, id));
        insertRouteNodes(id, req.getNodeIds());
        return toRouteVO(r);
    }

    /** 删除路线：连同路线站点一并删除 */
    @Transactional
    public void deleteRoute(Long id) {
        requireRoute(id);
        routeNodeMapper.delete(Wrappers.lambdaQuery(RouteNode.class).eq(RouteNode::getRouteId, id));
        routeMapper.deleteById(id);
    }

    // ---------- 内部辅助 ----------

    /** 按序批量写入路线站点，seq 从 1 起 */
    private void insertRouteNodes(Long routeId, List<Long> nodeIds) {
        if (nodeIds == null) {
            return;
        }
        int seq = 1;
        for (Long nodeId : nodeIds) {
            RouteNode rn = new RouteNode();
            rn.setRouteId(routeId);
            rn.setSeq(seq++);
            rn.setNodeId(nodeId);
            routeNodeMapper.insert(rn);
        }
    }

    private Node requireNode(Long id) {
        Node n = nodeMapper.selectById(id);
        if (n == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "节点不存在");
        }
        return n;
    }

    private Process requireProcess(Long id) {
        Process p = processMapper.selectById(id);
        if (p == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "流程不存在");
        }
        return p;
    }

    private Route requireRoute(Long id) {
        Route r = routeMapper.selectById(id);
        if (r == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "路线不存在");
        }
        return r;
    }

    private Map<Long, String> nodeNameMap() {
        return nodeMapper.selectList(Wrappers.lambdaQuery(Node.class))
                .stream().collect(Collectors.toMap(Node::getId, n -> n.getName() == null ? "" : n.getName()));
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
