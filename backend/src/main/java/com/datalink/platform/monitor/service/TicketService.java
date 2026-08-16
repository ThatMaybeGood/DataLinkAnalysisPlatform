package com.datalink.platform.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.monitor.dto.CreateTicketRequest;
import com.datalink.platform.monitor.dto.TicketVO;
import com.datalink.platform.monitor.entity.Alert;
import com.datalink.platform.monitor.entity.Ticket;
import com.datalink.platform.monitor.mapper.AlertMapper;
import com.datalink.platform.monitor.mapper.TicketMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工单服务：列表 / 创建（校验关联告警存在）。
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
