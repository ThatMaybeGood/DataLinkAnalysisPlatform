package com.datalink.platform.engine.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎依据单号引用链推导的流程模板（候选业务流）
 */
@Data
public class EngineFlowVO {
    /** 流程名（如「挂号 → 收费 → 退费申请」） */
    private String name;
    /** 途经节点 id（对应 draftEdges 中的节点） */
    private List<String> nodeIds = new ArrayList<>();
    /** 途经表名（reg_order ...） */
    private List<String> tableNames = new ArrayList<>();
}