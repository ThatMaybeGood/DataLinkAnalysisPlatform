package com.datalink.platform.llm.dto;

import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.NodeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 大模型润色结果：增量节点/边 + 改名映射 + 润色说明。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRefineResult {

    /** 大模型补充的节点（id 必须以 "llm-" 前缀） */
    private List<NodeVO> addedNodes;
    /** 大模型补充的边 */
    private List<EdgeVO> addedEdges;
    /** 改名映射（原显示名 → 新显示名） */
    private Map<String, String> renameMap;
    /** 润色说明（五类 + noop/error） */
    private List<RefinementItem> refinements;
    /** 提供方标识（openai-compatible / noop） */
    private String provider;
    /** 整体说明信息 */
    private String message;
}
