package com.datalink.platform.engine.dto;

import lombok.Data;

/**
 * G4 大模型细化请求（POST /api/analyze/refine 入参）。
 */
@Data
public class RefineRequest {
    /** 已启用的 DB 连接器 id */
    private Long connectorId;
}
