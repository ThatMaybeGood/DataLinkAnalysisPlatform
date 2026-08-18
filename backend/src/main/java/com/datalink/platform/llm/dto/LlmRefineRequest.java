package com.datalink.platform.llm.dto;

import com.datalink.platform.engine.dto.EngineCandidateVO;
import com.datalink.platform.engine.dto.EngineFlowVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 大模型润色请求：引擎草稿（候选单据 + 流程模板）作为上下文注入。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRefineRequest {

    /** 库节点显示名 */
    private String database;
    /** 引擎识别的候选单据 */
    private List<EngineCandidateVO> candidates;
    /** 引擎推导的流程模板 */
    private List<EngineFlowVO> flows;
}
