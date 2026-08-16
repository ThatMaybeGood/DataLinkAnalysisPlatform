package com.datalink.platform.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.platform.common.PageResult;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Process;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.entity.RouteNode;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.model.mapper.RouteNodeMapper;
import com.datalink.platform.monitor.dto.InstanceNodeVO;
import com.datalink.platform.monitor.dto.InstanceVO;
import com.datalink.platform.monitor.dto.SaveInstanceRequest;
import com.datalink.platform.monitor.entity.Instance;
import com.datalink.platform.monitor.entity.InstanceNode;
import com.datalink.platform.monitor.mapper.InstanceMapper;
import com.datalink.platform.monitor.mapper.InstanceNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程实例服务：分页查询 / 实例站点链路 / 创建与更新。
 */
@Service
@RequiredArgsConstructor
public class InstanceService {

    /** 与 Jackson 全局格式保持一致（yyyy-MM-dd HH:mm:ss） */
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InstanceMapper instanceMapper;
    private final InstanceNodeMapper instanceNodeMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;
    private final NodeMapper nodeMapper;
    private final RouteNodeMapper routeNodeMapper;

    /** 分页查询实例（可按状态过滤），装配流程名/路线名/当前站点/进度/耗时 */
    public PageResult<InstanceVO> page(int page, int size, String status) {
        Page<Instance> p = new Page<>(page, size);
        instanceMapper.selectPage(p, Wrappers.<Instance>lambdaQuery()
                .eq(status != null && !status.isBlank(), Instance::getStatus, status)
                .orderByDesc(Instance::getId));
        List<InstanceVO> records = p.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(records, p.getTotal(), p.getCurrent(), p.getSize());
    }

    /** 实例经过站点列表（seq 升序），装配站点名 */
    public List<InstanceNodeVO> instanceNodes(Long id) {
        List<InstanceNode> nodes = instanceNodeMapper.selectList(Wrappers.lambdaQuery(InstanceNode.class)
                .eq(InstanceNode::getInstanceId, id).orderByAsc(InstanceNode::getSeq));
        List<InstanceNodeVO> result = new ArrayList<>(nodes.size());
        for (InstanceNode n : nodes) {
            InstanceNodeVO vo = new InstanceNodeVO();
            vo.setId(String.valueOf(n.getId()));
            vo.setNodeId(n.getNodeId() == null ? null : String.valueOf(n.getNodeId()));
            Node node = n.getNodeId() == null ? null : nodeMapper.selectById(n.getNodeId());
            vo.setNodeName(node == null ? null : node.getName());
            vo.setSeq(n.getSeq() == null ? null : String.valueOf(n.getSeq()));
            vo.setArriveTime(n.getArriveTime());
            vo.setLeaveTime(n.getLeaveTime());
            vo.setDurationMs(n.getDurationMs());
            vo.setStatus(n.getStatus());
            vo.setMessage(n.getMessage());
            result.add(vo);
        }
        return result;
    }

    /** 创建实例：状态默认 RUNNING、来源默认 MANUAL；nodeIds 非空按序生成站点链路 */
    @Transactional
    public InstanceVO create(SaveInstanceRequest req) {
        Instance inst = new Instance();
        inst.setProcessId(req.getProcessId());
        inst.setRouteId(req.getRouteId());
        inst.setBizNo(req.getBizNo());
        inst.setBizName(req.getBizName());
        inst.setStartTime(LocalDateTime.now());
        inst.setStatus(req.getStatus() == null || req.getStatus().isBlank() ? "RUNNING" : req.getStatus());
        inst.setSource(req.getSource() == null || req.getSource().isBlank() ? "MANUAL" : req.getSource());
        instanceMapper.insert(inst);
        if (req.getNodeIds() != null && !req.getNodeIds().isEmpty()) {
            inst.setCurrentNodeId(insertInstanceNodes(inst.getId(), req.getNodeIds()));
            instanceMapper.updateById(inst);
        }
        return toVO(inst);
    }

    /** 更新实例：非空字段覆盖；nodeIds 非空则重建站点链路并刷新当前站点 */
    @Transactional
    public InstanceVO update(Long id, SaveInstanceRequest req) {
        Instance inst = instanceMapper.selectById(id);
        if (inst == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "实例不存在");
        }
        inst.setBizNo(req.getBizNo());
        inst.setBizName(req.getBizName());
        if (req.getProcessId() != null) {
            inst.setProcessId(req.getProcessId());
        }
        if (req.getRouteId() != null) {
            inst.setRouteId(req.getRouteId());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            inst.setStatus(req.getStatus());
        }
        if (req.getSource() != null && !req.getSource().isBlank()) {
            inst.setSource(req.getSource());
        }
        if (req.getNodeIds() != null && !req.getNodeIds().isEmpty()) {
            instanceNodeMapper.delete(Wrappers.lambdaQuery(InstanceNode.class).eq(InstanceNode::getInstanceId, id));
            inst.setCurrentNodeId(insertInstanceNodes(id, req.getNodeIds()));
        }
        instanceMapper.updateById(inst);
        return toVO(inst);
    }

    /** 按序写入实例站点：首节点 RUNNING，其余 PENDING，均记录到达时间 */
    private Long insertInstanceNodes(Long instanceId, List<Long> nodeIds) {
        LocalDateTime now = LocalDateTime.now();
        Long first = null;
        int seq = 1;
        for (Long nodeId : nodeIds) {
            InstanceNode in = new InstanceNode();
            in.setInstanceId(instanceId);
            in.setNodeId(nodeId);
            in.setSeq(seq);
            in.setArriveTime(now);
            in.setStatus(seq == 1 ? "RUNNING" : "PENDING");
            instanceNodeMapper.insert(in);
            if (seq == 1) {
                first = nodeId;
            }
            seq++;
        }
        return first;
    }

    private InstanceVO toVO(Instance inst) {
        InstanceVO vo = new InstanceVO();
        vo.setId(String.valueOf(inst.getId()));
        vo.setBizNo(inst.getBizNo());
        vo.setBizName(inst.getBizName());
        vo.setProcessName(processName(inst.getProcessId()));
        vo.setRouteName(routeName(inst.getRouteId()));
        vo.setStatus(inst.getStatus());
        vo.setProgress(calcProgress(inst));
        Node cur = inst.getCurrentNodeId() == null ? null : nodeMapper.selectById(inst.getCurrentNodeId());
        vo.setCurrentNode(cur == null ? null : cur.getName());
        vo.setCurrentNodeId(inst.getCurrentNodeId() == null ? null : String.valueOf(inst.getCurrentNodeId()));
        vo.setStartTime(inst.getStartTime() == null ? null : inst.getStartTime().format(DATETIME_FMT));
        vo.setDuration(formatDuration(inst));
        vo.setSource(inst.getSource());
        return vo;
    }

    private String processName(Long id) {
        if (id == null) {
            return null;
        }
        Process p = processMapper.selectById(id);
        return p == null ? null : p.getName();
    }

    private String routeName(Long id) {
        if (id == null) {
            return null;
        }
        Route r = routeMapper.selectById(id);
        return r == null ? null : r.getName();
    }

    /** 进度 = 当前站点在所属路线中的位置 / 路线长度 × 100（取整）；无路线返回 0 */
    private int calcProgress(Instance inst) {
        if (inst.getCurrentNodeId() == null || inst.getRouteId() == null) {
            return 0;
        }
        List<RouteNode> rns = routeNodeMapper.selectList(Wrappers.lambdaQuery(RouteNode.class)
                .eq(RouteNode::getRouteId, inst.getRouteId()).orderByAsc(RouteNode::getSeq));
        for (int i = 0; i < rns.size(); i++) {
            if (inst.getCurrentNodeId().equals(rns.get(i).getNodeId())) {
                return (i + 1) * 100 / rns.size();
            }
        }
        return 0;
    }

    /** 总耗时：优先取 total_duration_ms，否则按 startTime→now 估算 */
    private String formatDuration(Instance inst) {
        Long ms = inst.getTotalDurationMs();
        if (ms == null && inst.getStartTime() != null) {
            ms = Duration.between(inst.getStartTime(), LocalDateTime.now()).toMillis();
        }
        long safe = ms == null || ms < 0 ? 0L : ms;
        long minutes = safe / 60000;
        if (minutes >= 60) {
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
        return minutes + "m";
    }
}
