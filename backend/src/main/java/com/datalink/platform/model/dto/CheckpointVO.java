package com.datalink.platform.model.dto;

import lombok.Data;

/**
 * 检测点视图对象（M1 阶段暂不填充）
 */
@Data
public class CheckpointVO {
    private String id;
    private String name;
    private String kind;
    private String checkType;
    private String status;
    private String lastCheck;
    private String detail;
}
