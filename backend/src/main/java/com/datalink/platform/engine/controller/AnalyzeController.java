package com.datalink.platform.engine.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.engine.dto.EngineDraftVO;
import com.datalink.platform.engine.service.EngineAnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
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
}