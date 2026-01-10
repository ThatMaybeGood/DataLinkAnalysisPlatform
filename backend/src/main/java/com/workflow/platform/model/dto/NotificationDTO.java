package com.workflow.platform.model.dto;

import lombok.Data;

@Data
public class NotificationDTO {
    private String title;
    private String content;
    private String level; // info, warning, error
    private String type; // system, user, alert
    private String target; // all, specific, mode
    private String targetValue;
    private boolean persistent;
    private long expirationTime;
}