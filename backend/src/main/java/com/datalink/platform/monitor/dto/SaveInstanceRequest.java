package com.datalink.platform.monitor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 创建流程实例请求
 */
@Data
public class SaveInstanceRequest {
    /** 业务单号/流水号 */
    @NotBlank
    private String bizNo;
    /** 业务别名（显示名） */
    @NotBlank
    private String bizName;
    /** 所属流程 */
    private Long processId;
    /** 选用路线 */
    private Long routeId;
    /** 可选：按序生成 instance_node 的站点 id 列表 */
    private List<Long> nodeIds;
    /** 状态（可空，默认 RUNNING） */
    private String status;
    /** 来源（可空，默认 MANUAL） */
    private String source;
}
