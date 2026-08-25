package com.datalink.platform.engine.service;

import com.datalink.platform.common.PageResult;
import com.datalink.platform.engine.dto.AnalysisTaskDetailVO;
import com.datalink.platform.engine.dto.AnalysisTaskVO;

/**
 * 图来源分析任务服务：发起一次分析即落一条任务（RUNNING → SUCCESS/FAILED）。
 */
public interface AnalysisTaskService {

    /**
     * 发起任务（落 RUNNING 记录）。校验首个来源存在，否则 404 不落记录。
     *
     * @param connectorId   首个来源连接器 id
     * @param connectorIds  多来源合并的全部来源 id（逗号分隔）；单来源传 connectorId 的字符串
     * @param connectorName 来源名快照；为空则由服务按首个来源补
     * @param taskType      ENGINE / LLM
     * @return 含 id 的任务列表项
     */
    AnalysisTaskVO start(Long connectorId, String connectorIds, String connectorName, String taskType);

    /** 任务完成：置 SUCCESS + 草稿快照 JSON + finished_at */
    void finish(Long id, String snapshotJson);

    /** 任务失败：置 FAILED + 错误信息 + finished_at */
    void fail(Long id, String errorMessage);

    /** 分页列表（connectorId 为空 = 全部） */
    PageResult<AnalysisTaskVO> page(int page, int size, Long connectorId);

    /** 详情（含草稿快照） */
    AnalysisTaskDetailVO detail(Long id);
}
