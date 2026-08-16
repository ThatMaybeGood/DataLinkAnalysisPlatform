package com.datalink.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存节点请求
 */
@Data
public class SaveNodeRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String nodeType;
    private String code;
    private String level;
    private String status;
    private String owner;
    private String description;
}
