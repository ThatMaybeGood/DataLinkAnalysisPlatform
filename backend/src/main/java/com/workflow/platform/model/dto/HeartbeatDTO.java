package com.workflow.platform.model.dto;

import com.workflow.platform.enums.ModeType;
import lombok.Data;

import java.util.Map;

@Data
public class HeartbeatDTO {
    private String clientId;
    private ModeType mode;
    private String clientVersion;
    private String platform;
    private String sessionId;
    private long timestamp;
    private Map<String, Object> metadata;
}