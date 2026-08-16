package com.datalink.platform.monitor.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.monitor.dto.TraceResultVO;
import com.datalink.platform.monitor.service.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 链路追踪接口（顺藤摸瓜）
 */
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;

    /** 链路追踪：给定站点上下游可达站点 */
    @GetMapping("/{nodeId}/trace")
    public Result<TraceResultVO> trace(@PathVariable Long nodeId) {
        return Result.ok(traceService.trace(nodeId));
    }

    /** 某节点所属路线 */
    @GetMapping("/{nodeId}/routes")
    public Result<List<RouteVO>> routes(@PathVariable Long nodeId) {
        return Result.ok(traceService.routesOfNode(nodeId));
    }
}
