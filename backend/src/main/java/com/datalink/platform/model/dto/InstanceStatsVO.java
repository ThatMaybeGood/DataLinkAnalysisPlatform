package com.datalink.platform.model.dto;

import lombok.Data;

/**
 * 流程实例运行统计
 */
@Data
public class InstanceStatsVO {
    private int running;
    private int success;
    private int fail;
}
