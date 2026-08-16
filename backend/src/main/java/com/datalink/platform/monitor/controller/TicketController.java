package com.datalink.platform.monitor.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.monitor.dto.CreateTicketRequest;
import com.datalink.platform.monitor.dto.TicketVO;
import com.datalink.platform.monitor.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单接口
 */
@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    /** 工单列表 */
    @GetMapping
    public Result<List<TicketVO>> list() {
        return Result.ok(ticketService.list());
    }

    /** 创建工单 */
    @PostMapping
    public Result<TicketVO> create(@Validated @RequestBody CreateTicketRequest req) {
        return Result.ok(ticketService.create(req));
    }
}
