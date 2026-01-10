package com.workflow.platform.model.dto;

import lombok.Data;

@Data
public class WebSocketMessage {
    private String type; // heartbeat, mode_update, sync_request, etc.
    private long timestamp;
    private Object data;
    private String messageId;
    private String correlationId;
}