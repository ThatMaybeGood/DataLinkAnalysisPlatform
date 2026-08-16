package com.datalink.platform.monitor.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.ImpactVO;
import com.datalink.platform.model.dto.PathVO;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.service.GraphAlgorithmService;
import com.datalink.platform.monitor.dto.TraceResultVO;
import com.datalink.platform.monitor.service.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 链路追踪接口（顺藤摸瓜 + 路径查询 + 影响面）
 */
@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
public class TraceController {

    private final TraceService traceService;
    private final GraphAlgorithmService graphAlgorithmService;

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

    /** 路径查询：from→to 全部简单路径（默认深度上限 8） */
    @GetMapping("/path")
    public Result<List<PathVO>> path(@RequestParam Long from,
                                     @RequestParam Long to,
                                     @RequestParam(defaultValue = "8") int maxDepth) {
        return Result.ok(graphAlgorithmService.queryPaths(from, to, maxDepth));
    }

    /** 影响面：某节点故障/变更波及的下游、路线与实例 */
    @GetMapping("/{nodeId}/impact")
    public Result<ImpactVO> impact(@PathVariable Long nodeId) {
        return Result.ok(graphAlgorithmService.impact(nodeId));
    }
}
