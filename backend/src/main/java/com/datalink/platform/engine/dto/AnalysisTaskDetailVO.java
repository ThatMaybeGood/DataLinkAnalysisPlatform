package com.datalink.platform.engine.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分析任务详情（含草稿快照，供前端回看）。
 * draftSnapshot：ENGINE → EngineDraftVO / LLM → RefineResultVO 的 JSON 对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnalysisTaskDetailVO extends AnalysisTaskVO {
    private Object draftSnapshot;
}
