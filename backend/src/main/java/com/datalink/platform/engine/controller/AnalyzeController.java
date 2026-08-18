package com.datalink.platform.engine.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.engine.dto.EngineDraftVO;
import com.datalink.platform.engine.dto.RefineRequest;
import com.datalink.platform.engine.dto.RefineResultVO;
import com.datalink.platform.engine.service.EngineAnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 图来源引擎分析接口。
 *
 * <p>注意：本接口读取连接器并执行引擎扫描（引擎 MVP 阶段），权限 + GET 由 SecurityConfig
 * 显式放开（.requestMatchers(HttpMethod.GET, "/api/analyze/**")）；深度迭代手工在数据池里补。
 */
@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
public class AnalyzeController {

    private final EngineAnalyzeService engineAnalyzeService;

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
}