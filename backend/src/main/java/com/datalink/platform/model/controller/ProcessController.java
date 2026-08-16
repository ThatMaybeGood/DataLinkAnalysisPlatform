package com.datalink.platform.model.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.ProcessVO;
import com.datalink.platform.model.dto.SaveProcessRequest;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 流程接口
 */
@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final GraphService graphService;
    private final ModelingService modelingService;

    /** 流程列表 */
    @GetMapping
    public Result<List<ProcessVO>> list() {
        return Result.ok(graphService.processes());
    }

    /** 新建流程 */
    @PostMapping
    public Result<ProcessVO> create(@Validated @RequestBody SaveProcessRequest req) {
        return Result.ok(modelingService.createProcess(req));
    }

    /** 更新流程 */
    @PutMapping("/{id}")
    public Result<ProcessVO> update(@PathVariable Long id, @Validated @RequestBody SaveProcessRequest req) {
        return Result.ok(modelingService.updateProcess(id, req));
    }

    /** 删除流程（级联清理其下路线） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelingService.deleteProcess(id);
        return Result.ok();
    }
}
