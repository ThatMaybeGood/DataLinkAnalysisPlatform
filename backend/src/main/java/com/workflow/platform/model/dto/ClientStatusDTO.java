package com.workflow.platform.model.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ClientStatusDTO {
    private String clientId;
    private String heartbeatStatus;
    private boolean online;
    private long lastHeartbeatTime;
    private boolean modeConsistent;
    private String currentMode;
    private String suggestedMode;
    private Map<String, Object> additionalInfo;
}