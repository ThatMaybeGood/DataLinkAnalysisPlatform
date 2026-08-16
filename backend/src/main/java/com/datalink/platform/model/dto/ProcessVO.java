package com.datalink.platform.model.dto;

import lombok.Data;

/**
 * 流程视图对象
 */
@Data
public class ProcessVO {
    private String id;
    private String name;
    private String scene;
    private String level;
    private String description;
    private String startNodeName;
    private String endNodeName;
    private int nodeCount;
    private int routeCount;
    private InstanceStatsVO instanceStats;
    private String updatedAt;
}
