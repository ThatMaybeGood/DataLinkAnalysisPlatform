package com.workflow.platform.model.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AuditLogDTO {

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

    private Long createdAt;
}
