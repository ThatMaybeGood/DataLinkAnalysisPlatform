package com.datalink.platform.monitor.service;

import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Process;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.monitor.entity.Alert;
import com.datalink.platform.monitor.entity.Ticket;
import com.datalink.platform.monitor.mapper.AlertMapper;
import com.datalink.platform.monitor.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 等级预警/干预服务：按目标级别（L1-L4）匹配处置组合，告警创建后按处置规则自动建单。
 */
@Service
@RequiredArgsConstructor
public class InterventionService {

    private final NodeMapper nodeMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;
    private final AlertMapper alertMapper;
    private final TicketMapper ticketMapper;

    /** 按目标级别返回处置组合：L1→自动处置+建单+通知 / L2→建单+通知 / L3→通知 / L4→仅记录 */
    public String dispositionFor(String targetType, Long targetId) {
        String level = resolveLevel(targetType, targetId);
        return switch (level) {
            case "L1" -> "AUTO_ACTION,TICKET,NOTIFY";
            case "L2" -> "TICKET,NOTIFY";
            case "L4" -> "RECORD";
            default -> "NOTIFY";
        };
    }

    /** 告警创建后处置：含 TICKET 时自动创建工单（assignee 空、priority 取 severity、description 取 message） */
    public void onAlertCreated(Alert alert) {
        String disposition = alert.getDisposition();
        if (disposition != null && disposition.contains("TICKET")) {
            Ticket t = new Ticket();
            t.setAlertId(alert.getId());
            t.setPriority(alert.getSeverity());
            t.setDescription(alert.getMessage());
            t.setStatus("OPEN");
            ticketMapper.insert(t);
        }
    }

    /** NODE/PROCESS/ROUTE 取 level，查不到默认 L3 */
    private String resolveLevel(String targetType, Long targetId) {
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
}
