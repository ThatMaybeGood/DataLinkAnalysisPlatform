package com.datalink.platform.monitor.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.monitor.dto.DashboardStatsVO;
import com.datalink.platform.monitor.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 监控仪表盘接口
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** 仪表盘统计 */
    @GetMapping("/stats")
    public Result<DashboardStatsVO> stats() {
        return Result.ok(dashboardService.stats());
    }
}
