package com.datalink.platform.model.dto;

import lombok.Data;

import java.util.List;

/**
 * 路线视图对象
 */
@Data
public class RouteVO {
    private String id;
    private String processId;
    private String name;
    private String priority;
    private String status;
    /** 有序站点节点 id */
    private List<String> nodeIds;
}
