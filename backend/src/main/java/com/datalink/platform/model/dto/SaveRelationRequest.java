package com.datalink.platform.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存路网边请求
 */
@Data
public class SaveRelationRequest {
    @NotNull
    private Long fromNodeId;
    @NotNull
    private Long toNodeId;
    private String relationType;
    private String level;
    private String description;
}
