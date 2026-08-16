package com.datalink.platform.model.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.SaveRelationRequest;
import com.datalink.platform.model.service.GraphService;
import com.datalink.platform.model.service.ModelingService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 路网边接口
 */
@RestController
@RequestMapping("/api/edges")
@RequiredArgsConstructor
public class RelationController {

    private final GraphService graphService;
    private final ModelingService modelingService;

    /** 路网边列表 */
    @GetMapping
    public Result<List<EdgeVO>> list() {
        return Result.ok(graphService.edges());
    }

    /** 新建路网边 */
    @PostMapping
    public Result<EdgeVO> create(@Validated @RequestBody SaveRelationRequest req) {
        return Result.ok(modelingService.createRelation(req));
    }

    /** 删除路网边 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        modelingService.deleteRelation(id);
        return Result.ok();
    }
}
