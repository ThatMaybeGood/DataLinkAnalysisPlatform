package com.datalink.platform.engine.dto;

import com.datalink.platform.llm.dto.RefinementItem;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.NodeVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * G4 大模型细化结果（POST /api/analyze/refine 响应）。
 *
 * <p>base 为引擎骨架原样返回（前端留作回退快照）；addedNodes/addedEdges/renameMap/
 * refinements 为大模型增量（节点 id 带 llm- 前缀）。大模型不可用/异常时降级：
 * 增量为空，refinements 含 type="error" 项，base 仍返回。
 */
@Data
@Builder
public class RefineResultVO {
    /** 引擎骨架原稿（前端回退快照） */
    private EngineDraftVO base;
    /** LLM 增量节点（id 带 llm- 前缀） */
    private List<NodeVO> addedNodes;
    /** LLM 增量边 */
    private List<EdgeVO> addedEdges;
    /** 重命名映射：节点 id → 新名称 */
    private Map<String, String> renameMap;
    /** 细化说明项（含降级时的 type="error" 项） */
    private List<RefinementItem> refinements;
    /** 实际 provider 名（noop/mock/openai.../error） */
    private String provider;
    /** 汇总说明 */
    private String message;
}
