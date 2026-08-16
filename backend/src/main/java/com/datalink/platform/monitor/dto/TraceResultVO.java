package com.datalink.platform.monitor.dto;

import com.datalink.platform.model.dto.NodeVO;
import lombok.Data;

import java.util.List;

/**
 * 链路追踪结果视图对象（给定站点，回溯上游/下游可达站点）
 */
@Data
public class TraceResultVO {
    /** 追踪起始站点 id */
    private String nodeId;
    /** 上游可达站点 */
    private List<NodeVO> upstream;
    /** 下游可达站点 */
    private List<NodeVO> downstream;
}
