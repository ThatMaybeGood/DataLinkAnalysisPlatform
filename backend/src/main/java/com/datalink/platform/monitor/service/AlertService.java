package com.datalink.platform.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Process;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.monitor.dto.AlertVO;
import com.datalink.platform.monitor.dto.SaveAlertRequest;
import com.datalink.platform.monitor.entity.Alert;
import com.datalink.platform.monitor.entity.Instance;
import com.datalink.platform.monitor.mapper.AlertMapper;
import com.datalink.platform.monitor.mapper.InstanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 告警服务：列表（附带目标对象名）/ 创建（按目标级别算处置组合并触发干预）/ 解决。
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper alertMapper;
    private final NodeMapper nodeMapper;
    private final InstanceMapper instanceMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;
    private final InterventionService interventionService;

    /** 告警列表（可按状态过滤，创建时间倒序），按目标类型解析目标名 */
    public List<AlertVO> list(String status) {
        List<Alert> alerts = alertMapper.selectList(Wrappers.lambdaQuery(Alert.class)
                .eq(status != null && !status.isBlank(), Alert::getStatus, status)
                .orderByDesc(Alert::getCreatedAt));
        List<AlertVO> result = new ArrayList<>(alerts.size());
        for (Alert a : alerts) {
            result.add(toVO(a));
        }
        return result;
    }

    /** 创建告警：状态 OPEN、处置组合按目标级别匹配，含 TICKET 时由干预服务自动建单 */
    public AlertVO create(SaveAlertRequest req) {
        Alert a = new Alert();
        a.setAlertType(req.getType());
        a.setTargetType(req.getTargetType());
        a.setTargetId(req.getTargetId());
        a.setMessage(req.getMessage());
        a.setSeverity(req.getSeverity());
        a.setStatus("OPEN");
        a.setDisposition(interventionService.dispositionFor(req.getTargetType(), req.getTargetId()));
        alertMapper.insert(a);
        interventionService.onAlertCreated(a);
        return toVO(a);
    }

    /** 解决告警：状态置 RESOLVED、记录解决时间 */
    public void resolve(Long id) {
        Alert alert = alertMapper.selectById(id);
        if (alert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "告警不存在");
        }
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        alertMapper.updateById(alert);
    }

    private AlertVO toVO(Alert a) {
        AlertVO vo = new AlertVO();
        vo.setId(String.valueOf(a.getId()));
        vo.setType(a.getAlertType());
        vo.setSeverity(a.getSeverity());
        vo.setTargetType(a.getTargetType());
        vo.setTargetName(resolveTargetName(a.getTargetType(), a.getTargetId()));
        vo.setMessage(a.getMessage());
        vo.setStatus(a.getStatus());
        // 告警级别按目标对象反查
        vo.setLevel(resolveTargetLevel(a.getTargetType(), a.getTargetId()));
        vo.setTime(a.getCreatedAt());
        vo.setResolvedAt(a.getResolvedAt());
        return vo;
    }

    /** NODE/PROCESS/ROUTE 取目标 level，查不到默认 L3 */
    private String resolveTargetLevel(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return "L3";
        }
        switch (targetType) {
            case "NODE": {
                Node n = nodeMapper.selectById(targetId);
                return n != null && n.getLevel() != null ? n.getLevel() : "L3";
            }
            case "PROCESS": {
                Process p = processMapper.selectById(targetId);
                return p != null && p.getLevel() != null ? p.getLevel() : "L3";
            }
            case "ROUTE": {
                Route r = routeMapper.selectById(targetId);
                return r != null && r.getLevel() != null ? r.getLevel() : "L3";
            }
            default:
                return "L3";
        }
    }

    /** NODE→节点名 / INSTANCE→业务名 / PROCESS→流程名 / ROUTE→路线名，查不到返回 null */
    private String resolveTargetName(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return null;
        }
        switch (targetType) {
            case "NODE": {
                Node n = nodeMapper.selectById(targetId);
                return n == null ? null : n.getName();
            }
            case "INSTANCE": {
                Instance i = instanceMapper.selectById(targetId);
                return i == null ? null : i.getBizName();
            }
            case "PROCESS": {
                Process p = processMapper.selectById(targetId);
                return p == null ? null : p.getName();
            }
            case "ROUTE": {
                Route r = routeMapper.selectById(targetId);
                return r == null ? null : r.getName();
            }
            default:
                return null;
        }
    }
}
