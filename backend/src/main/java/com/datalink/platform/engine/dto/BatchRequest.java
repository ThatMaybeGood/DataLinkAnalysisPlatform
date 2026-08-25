package com.datalink.platform.engine.dto;

import lombok.Data;

import java.util.List;

/**
 * 多来源合并分析请求体（POST /api/analyze/batch、/api/analyze/refine/batch）
 */
@Data
public class BatchRequest {
    /** 数据池连接器 id 列表（≥1） */
    private List<Long> connectorIds;
}
