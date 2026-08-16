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
 * 告警服务：列表（附带目标对象名）/ 解决。
 */
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper alertMapper;
    private final NodeMapper nodeMapper;
    private final InstanceMapper instanceMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;

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
        // 告警表无级别列，统一默认 L3
        vo.setLevel("L3");
        vo.setTime(a.getCreatedAt());
        vo.setResolvedAt(a.getResolvedAt());
        return vo;
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
