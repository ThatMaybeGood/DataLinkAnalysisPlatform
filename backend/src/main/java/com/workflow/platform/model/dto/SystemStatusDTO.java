package com.workflow.platform.model.dto;

import com.workflow.platform.enums.ModeType;
import lombok.Data;

import java.util.Map;

@Data
public class SystemStatusDTO {
    private long serverTime;
    private ModeType serverMode;
    private String serverVersion;
    private String systemStatus;
    private Map<String, Object> statistics;
    private String clientId;
    private long syncTime;
}