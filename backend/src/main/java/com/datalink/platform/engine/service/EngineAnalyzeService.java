package com.datalink.platform.engine.service;

import com.datalink.platform.engine.dto.EngineDraftVO;

/**
 * 图来源引擎分析服务：连库扫描 → 通用业务单据模式识别 → 草稿。
 */
public interface EngineAnalyzeService {

    /**
     * 对指定 DB 连接器执行引擎分析。
     *
     * @param connectorId 数据池连接器 id（DB 型，已启用）
     * @return 引擎草稿（draftNodes/draftEdges/candidates/flows）
     */
    EngineDraftVO analyze(Long connectorId);
}