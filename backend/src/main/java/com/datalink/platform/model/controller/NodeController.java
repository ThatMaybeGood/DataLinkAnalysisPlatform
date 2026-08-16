package com.datalink.platform.model.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.SaveNodeRequest;
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
 * 站点节点接口
 */
@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final GraphService graphService;
    private final ModelingService modelingService;

    /** 节点列表 */
    @GetMapping
    public Result<List<NodeVO>> list() {
        return Result.ok(graphService.nodes());
    }

    /** 新建节点 */
    @PostMapping
    public Result<NodeVO> create(@Validated @RequestBody SaveNodeRequest req) {
        return Result.ok(modelingService.createNode(req));
    }

    /** 更新节点 */
    @PutMapping("/{id}")
    public Result<NodeVO> update(@PathVariable Long id, @Validated @RequestBody SaveNodeRequest req) {
        return Result.ok(modelingService.updateNode(id, req));
    }

    /** 删除节点 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelingService.deleteNode(id);
        return Result.ok();
    }
}
