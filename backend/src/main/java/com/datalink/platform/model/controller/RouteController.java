package com.datalink.platform.model.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.dto.SaveRouteRequest;
import com.datalink.platform.model.service.GraphService;
import com.datalink.platform.model.service.ModelingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 路线接口
 */
@RestController
@RequestMapping("/api/routes")
@RequiredArgsConstructor
public class RouteController {

    private final GraphService graphService;
    private final ModelingService modelingService;

    /** 路线列表（可按流程过滤） */
    @GetMapping
    public Result<List<RouteVO>> list(@RequestParam(required = false) Long processId) {
        List<RouteVO> routes = graphService.routes();
        if (processId != null) {
            String pid = String.valueOf(processId);
            routes = routes.stream().filter(r -> pid.equals(r.getProcessId())).collect(Collectors.toList());
        }
        return Result.ok(routes);
    }

    /** 新建路线（含有序路线站点） */
    @PostMapping
    public Result<RouteVO> create(@Validated @RequestBody SaveRouteRequest req) {
        return Result.ok(modelingService.createRoute(req));
    }

    /** 更新路线（覆盖站点顺序） */
    @PutMapping("/{id}")
    public Result<RouteVO> update(@PathVariable Long id, @Validated @RequestBody SaveRouteRequest req) {
        return Result.ok(modelingService.updateRoute(id, req));
    }

    /** 删除路线（连同路线站点） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelingService.deleteRoute(id);
        return Result.ok();
    }
}
