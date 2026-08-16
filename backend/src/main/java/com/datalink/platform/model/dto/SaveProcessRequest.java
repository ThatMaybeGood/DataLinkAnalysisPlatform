package com.datalink.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存流程请求
 */
@Data
public class SaveProcessRequest {
    @NotBlank
    private String name;
    private String scene;
    private String level;
    private String description;
    private Long startNodeId;
    private Long endNodeId;
}
