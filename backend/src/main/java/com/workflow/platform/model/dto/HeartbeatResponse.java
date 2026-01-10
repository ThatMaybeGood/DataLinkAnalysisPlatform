package com.workflow.platform.model.dto;

import com.workflow.platform.enums.ModeType;
import lombok.Data;

@Data
public class HeartbeatResponse {
    private long timestamp;
    private long serverTime;
    private ModeType serverMode;
    private long nextHeartbeatInterval;
    private boolean needsSync;
    private SystemStatusDTO syncData;
    private boolean modeConsistent;
    private ModeType suggestedMode;
}