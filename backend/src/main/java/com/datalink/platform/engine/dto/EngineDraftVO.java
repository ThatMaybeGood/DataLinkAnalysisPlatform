package com.datalink.platform.engine.dto;

import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.NodeVO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎分析草稿结果（POST /api/analyze 响应）
 * 结构对齐前端 G2 草稿页的 draftNodes/draftEdges/candidates/flows，可直接换数据源。
 */
@Data
public class EngineDraftVO {
    /** 扫描的库名 */
    private String database;
    /** 草稿节点：1 个 DATABASE + 每候选单据 1 个 TABLE */
    private List<NodeVO> draftNodes = new ArrayList<>();
    /** 草稿边：库→表 承载边 + 单号引用链推导的方向边 */
    private List<EdgeVO> draftEdges = new ArrayList<>();
    /** 候选单据清单（含置信度/信号/低置信标记） */
    private List<EngineCandidateVO> candidates = new ArrayList<>();
    /** 引用链推导的流程模板 */
    private List<EngineFlowVO> flows = new ArrayList<>();
    /** 汇总说明 */
    private String message;
}