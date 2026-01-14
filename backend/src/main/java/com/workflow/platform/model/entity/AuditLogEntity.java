package com.workflow.platform.model.entity;

import lombok.Data;

@Data
public class AuditLogEntity {
    private  String level;

    private Long timestamp;


    private  String logId;

    private String action;

    private String resourceId;

    private String category;
    private String userId;

    private String resourceType;
    private Object details;

    private String ipAddress;

    private String userAgent;

    private String outcome;

    private long createdAt;
}
