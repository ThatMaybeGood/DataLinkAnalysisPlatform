package com.datalink.platform.monitor.controller;

import com.datalink.platform.common.PageResult;
import com.datalink.platform.common.Result;
import com.datalink.platform.monitor.dto.InstanceNodeVO;
import com.datalink.platform.monitor.dto.InstanceVO;
import com.datalink.platform.monitor.dto.SaveInstanceRequest;
import com.datalink.platform.monitor.service.InstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程实例接口
 */
@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
public class InstanceController {

    private final InstanceService instanceService;

    /** 实例分页（可按状态过滤） */
    @GetMapping
    public Result<PageResult<InstanceVO>> page(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String status) {
        return Result.ok(instanceService.page(page, size, status));
    }

    /** 实例经过站点链路 */
    @GetMapping("/{id}/nodes")
    public Result<List<InstanceNodeVO>> nodes(@PathVariable Long id) {
        return Result.ok(instanceService.instanceNodes(id));
    }

    /** 新建实例 */
    @PostMapping
    public Result<InstanceVO> create(@Validated @RequestBody SaveInstanceRequest req) {
        return Result.ok(instanceService.create(req));
    }

    /** 更新实例 */
    @PutMapping("/{id}")
    public Result<InstanceVO> update(@PathVariable Long id, @Validated @RequestBody SaveInstanceRequest req) {
        return Result.ok(instanceService.update(id, req));
    }
}
