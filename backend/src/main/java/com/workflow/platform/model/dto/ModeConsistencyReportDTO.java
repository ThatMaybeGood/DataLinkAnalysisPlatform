package com.workflow.platform.model.dto;

import com.workflow.platform.enums.ModeType;
import lombok.Data;

@Data
public class ModeConsistencyReportDTO {
    private long checkTime;
    private ModeType serverMode;
    private int totalClients;
    private int consistentClients;
    private int inconsistentClients;
    private int staleClients;
    private double consistencyRate;
    private String[] inconsistentClientIds;
}