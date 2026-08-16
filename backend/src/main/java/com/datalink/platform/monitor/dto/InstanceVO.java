package com.datalink.platform.monitor.dto;

import lombok.Data;

/**
 * 流程实例视图对象
 */
@Data
public class InstanceVO {
    private String id;
    /** 业务单号/流水号 */
    private String bizNo;
    /** 业务别名（显示名） */
    private String bizName;
    /** 所属流程名 */
    private String processName;
    /** 选用路线名 */
    private String routeName;
    /** RUNNING/SUCCESS/FAIL/STUCK/TIMEOUT */
    private String status;
    /** 完成进度百分比 */
    private int progress;
    /** 当前站点名 */
    private String currentNode;
    /** 当前站点 id */
    private String currentNodeId;
    /** 开始时间（格式化串） */
    private String startTime;
    /** 总耗时（格式化串，如 2h 35m） */
    private String duration;
    /** INFER/MANUAL/REPORT/API */
    private String source;
}
