package com.datalink.platform.engine.controller;

import com.datalink.platform.common.PageResult;
import com.datalink.platform.common.Result;
import com.datalink.platform.engine.dto.AnalysisTaskDetailVO;
import com.datalink.platform.engine.dto.AnalysisTaskVO;
import com.datalink.platform.engine.dto.BatchRequest;
import com.datalink.platform.engine.dto.EngineDraftVO;
import com.datalink.platform.engine.dto.RefineRequest;
import com.datalink.platform.engine.dto.RefineResultVO;
import com.datalink.platform.engine.service.AnalysisTaskService;
import com.datalink.platform.engine.service.EngineAnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图来源引擎分析接口。
 *
 * <p>单来源：GET /api/analyze?connectorId=、POST /api/analyze/refine；
 * 多来源合并：POST /api/analyze/batch、POST /api/analyze/refine/batch；
 * 分析任务历史：GET /api/analyze/tasks（分页）、GET /api/analyze/tasks/{id}（详情）。
 * 每次分析自动落一条分析任务（RUNNING → SUCCESS/FAILED）。
 */
@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
public class AnalyzeController {

    private final EngineAnalyzeService engineAnalyzeService;
    private final AnalysisTaskService analysisTaskService;

    /**
     * 对指定 DB 连接器执行引擎分析，返回候选单据 + 草稿节点/边。
     *
     * @param connectorId 已启用的 DB 连接器 id
     */
    @GetMapping
    public Result<EngineDraftVO> analyze(@RequestParam("connectorId") Long connectorId) {
        return Result.ok(engineAnalyzeService.analyze(connectorId));
    }

    /**
     * G4 大模型细化：引擎骨架 + LLM 增量节点/边/重命名。
     * 大模型不可用/异常时降级返回引擎原稿（HTTP 200，refinements 含 error 项）。
     */
    @PostMapping("/refine")
    public Result<RefineResultVO> refine(@RequestBody RefineRequest request) {
        return Result.ok(engineAnalyzeService.refine(request.getConnectorId()));
    }

    /** 多来源合并引擎分析：逐个扫描后合并草稿（节点带来源标识） */
    @PostMapping("/batch")
    public Result<EngineDraftVO> analyzeBatch(@RequestBody BatchRequest request) {
        return Result.ok(engineAnalyzeService.analyzeBatch(request.getConnectorIds()));
    }

    /** 多来源合并 + 大模型细化 */
    @PostMapping("/refine/batch")
    public Result<RefineResultVO> refineBatch(@RequestBody BatchRequest request) {
        return Result.ok(engineAnalyzeService.refineBatch(request.getConnectorIds()));
    }

    /** 分析任务分页列表（connectorId 为空 = 全部） */
    @GetMapping("/tasks")
    public Result<PageResult<AnalysisTaskVO>> tasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long connectorId) {
        return Result.ok(analysisTaskService.page(page, size, connectorId));
    }

    /** 分析任务详情（含草稿快照，供回看） */
    @GetMapping("/tasks/{id}")
    public Result<AnalysisTaskDetailVO> taskDetail(@PathVariable Long id) {
        return Result.ok(analysisTaskService.detail(id));
    }
}
