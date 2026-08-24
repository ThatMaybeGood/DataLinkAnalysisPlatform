package com.datalink.platform.monitor.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.CheckpointVO;
import com.datalink.platform.monitor.dto.SaveCheckpointRequest;
import com.datalink.platform.monitor.service.CheckpointService;
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

/**
 * 检测点接口
 */
@RestController
@RequestMapping("/api/checkpoints")
@RequiredArgsConstructor
public class CheckpointController {

    private final CheckpointService checkpointService;

    /** 按节点查检测点列表 */
    @GetMapping
    public Result<List<CheckpointVO>> list(@RequestParam Long nodeId) {
        return Result.ok(checkpointService.listByNode(nodeId));
    }

    /** 新建检测点 */
    @PostMapping
    public Result<CheckpointVO> create(@Validated @RequestBody SaveCheckpointRequest req) {
        return Result.ok(checkpointService.create(req));
    }

    /** 更新检测点 */
    @PutMapping("/{id}")
    public Result<CheckpointVO> update(@PathVariable Long id, @Validated @RequestBody SaveCheckpointRequest req) {
        return Result.ok(checkpointService.update(id, req));
    }

    /** 删除检测点 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        checkpointService.delete(id);
        return Result.ok();
    }

    /** 立即执行一次检测 */
    @PostMapping("/{id}/run")
    public Result<CheckpointVO> run(@PathVariable Long id) {
        return Result.ok(checkpointService.run(id));
    }
}
