package com.datalink.platform.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.monitor.dto.CreateTicketRequest;
import com.datalink.platform.monitor.dto.SaveTicketRequest;
import com.datalink.platform.monitor.dto.TicketVO;
import com.datalink.platform.monitor.entity.Alert;
import com.datalink.platform.monitor.entity.Ticket;
import com.datalink.platform.monitor.mapper.AlertMapper;
import com.datalink.platform.monitor.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单服务：列表 / 创建（校验关联告警存在）/ 更新（状态流转 OPEN→PROCESSING→RESOLVED）。
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketMapper ticketMapper;
    private final AlertMapper alertMapper;

    /** 全部工单（创建时间倒序） */
    public List<TicketVO> list() {
        return ticketMapper.selectList(Wrappers.lambdaQuery(Ticket.class).orderByDesc(Ticket::getCreatedAt))
                .stream().map(this::toVO).toList();
    }

    /** 创建工单：校验告警存在，状态默认 OPEN */
    public TicketVO create(CreateTicketRequest req) {
        Alert alert = alertMapper.selectById(req.getAlertId());
        if (alert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "关联告警不存在");
        }
        Ticket t = new Ticket();
        t.setAlertId(req.getAlertId());
        t.setAssignee(req.getAssignee());
        t.setDescription(req.getDescription());
        t.setStatus("OPEN");
        ticketMapper.insert(t);
        return toVO(t);
    }

    /** 更新工单：非空覆盖 assignee/status/description；状态仅向前流转，RESOLVED 记录解决时间；不存在抛 404 */
    public TicketVO update(Long id, SaveTicketRequest req) {
        Ticket t = ticketMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "工单不存在");
        }
        if (req.getAssignee() != null) {
            t.setAssignee(req.getAssignee());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            assertForwardTransition(t.getStatus(), req.getStatus());
            t.setStatus(req.getStatus());
        }
        if (req.getDescription() != null) {
            t.setDescription(req.getDescription());
        }
        if ("RESOLVED".equals(t.getStatus()) && t.getResolvedAt() == null) {
            t.setResolvedAt(LocalDateTime.now());
        }
        ticketMapper.updateById(t);
        return toVO(t);
    }

    /** 状态仅允许向前流转：OPEN(0)→PROCESSING(1)→RESOLVED(2) */
    private void assertForwardTransition(String from, String to) {
        int fromRank = statusRank(from);
        int toRank = statusRank(to);
        if (toRank < fromRank || toRank < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "非法状态流转：" + from + "→" + to);
        }
    }

    private int statusRank(String status) {
        return switch (status) {
            case "OPEN" -> 0;
            case "PROCESSING" -> 1;
            case "RESOLVED" -> 2;
            default -> -1;
        };
    }

    private TicketVO toVO(Ticket t) {
        TicketVO vo = new TicketVO();
        vo.setId(String.valueOf(t.getId()));
        vo.setAlertId(t.getAlertId() == null ? null : String.valueOf(t.getAlertId()));
        vo.setAssignee(t.getAssignee());
        vo.setPriority(t.getPriority());
        vo.setStatus(t.getStatus());
        vo.setDescription(t.getDescription());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setResolvedAt(t.getResolvedAt());
        return vo;
    }
}
