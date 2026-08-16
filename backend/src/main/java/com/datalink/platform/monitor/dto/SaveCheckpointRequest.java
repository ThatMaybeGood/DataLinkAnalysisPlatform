package com.datalink.platform.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 保存检测点请求
 */
@Data
public class SaveCheckpointRequest {
    /** 所属站点节点 */
    @NotNull
    private Long nodeId;
    /** 检测点名称 */
    @NotBlank
    private String name;
    /** 检测类型 DATA_VOLUME/FRESHNESS/DELAY/... */
    @NotBlank
    private String checkType;
    /** 默认 CUSTOM */
    private String kind = "CUSTOM";
    /** 检测频率，默认 5m */
    private String freq = "5m";
    /** 级别，默认 L3 */
    private String level = "L3";
}
