package com.datalink.platform.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 保存路线请求
 */
@Data
public class SaveRouteRequest {
    @NotBlank
    private String name;
    @NotNull
    private Long processId;
    private String priority;
    private String status;
    /** 有序站点节点 id */
    private List<Long> nodeIds;
}
