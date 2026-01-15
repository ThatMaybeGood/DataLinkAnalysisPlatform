package com.workflow.platform.model.entity;

import com.workflow.platform.enums.NodeType;

import java.util.Map;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/16 00:36
 */
public class NodeEntity {
    private String id;
    private String name;
    private NodeType type;
    private Map<String, Object> properties;
    // 其他字段...
    // 省略getter和setter方法...
}
