package com.datalink.platform.engine.service;

import com.datalink.platform.engine.dto.EngineDraftVO;
import com.datalink.platform.engine.dto.RefineResultVO;

import java.util.List;

/**
 * 图来源引擎分析服务：连库扫描 → 通用业务单据模式识别 → 草稿。
 */
public interface EngineAnalyzeService {

    /**
     * 对指定 DB 连接器执行引擎分析（每次分析落一条分析任务）。
     *
     * @param connectorId 数据池连接器 id（DB 型，已启用）
     * @return 引擎草稿（draftNodes/draftEdges/candidates/flows）
     */
    EngineDraftVO analyze(Long connectorId);

    /**
     * G4 大模型细化：先跑引擎分析拿骨架，再交 ModelProvider 做增量细化。
     * 大模型不可用/异常时降级返回引擎原稿（base 非空，refinements 含 error 项）。
     *
     * @param connectorId 数据池连接器 id（DB 型，已启用）
     * @return 细化结果（base + 增量节点/边/重命名/说明）
     */
    RefineResultVO refine(Long connectorId);

    /**
     * 多来源合并引擎分析：对多个 DB 连接器各自扫描后合并草稿
     * （节点 id 带来源标识防冲突，候选/流程带库名区分）。
     *
     * @param connectorIds 数据池连接器 id 列表（≥1）
     * @return 合并后的引擎草稿
     */
    EngineDraftVO analyzeBatch(List<Long> connectorIds);

    /**
     * 多来源合并 + 大模型细化。
     *
     * @param connectorIds 数据池连接器 id 列表（≥1）
     * @return 细化结果
     */
    RefineResultVO refineBatch(List<Long> connectorIds);
}