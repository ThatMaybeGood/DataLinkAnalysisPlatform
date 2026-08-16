package com.datalink.platform.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 站点节点视图对象
 */
@Data
public class NodeVO {
    private String id;
    private String name;
    private String code;
    private String nodeType;
    private String level;
    private String status;
    private String owner;
    private String description;
    /** 检测点列表（M1 阶段恒为空） */
    private List<CheckpointVO> checkpoints = new ArrayList<>();
}
