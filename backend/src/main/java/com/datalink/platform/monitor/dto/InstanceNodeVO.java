package com.datalink.platform.monitor.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 实例经过站点视图对象
 */
@Data
public class InstanceNodeVO {
    private String id;
    /** 站点节点 id */
    private String nodeId;
    /** 站点名 */
    private String nodeName;
    /** 经过顺序 */
    private String seq;
    /** 到达时间 */
    private LocalDateTime arriveTime;
    /** 离开时间 */
    private LocalDateTime leaveTime;
    /** 该环节耗时（毫秒） */
    private Long durationMs;
    private String status;
    private String message;
}
