package com.datalink.platform.model.dto;

import com.datalink.platform.monitor.dto.InstanceVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 影响面视图对象：某节点故障/变更波及的下游节点、路线与流程实例。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImpactVO {
    /** 全部下游节点（不含自身，沿 relation 有向边可达） */
    private List<NodeVO> downstream;
    /** 受影响的流程实例（当前站点 ∈ 自身 ∪ 下游） */
    private List<InstanceVO> affectedInstances;
    /** 受影响的路线（经过自身或下游任一站点） */
    private List<RouteVO> affectedRoutes;
}
