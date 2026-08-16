package com.datalink.platform.monitor.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.monitor.dto.AlertVO;
import com.datalink.platform.monitor.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 告警接口
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /** 告警列表（可按状态过滤） */
    @GetMapping
    public Result<List<AlertVO>> list(@RequestParam(required = false) String status) {
        return Result.ok(alertService.list(status));
    }

    /** 解决告警 */
    @PostMapping("/{id}/resolve")
    public Result<Void> resolve(@PathVariable Long id) {
        alertService.resolve(id);
        return Result.ok();
    }
}
