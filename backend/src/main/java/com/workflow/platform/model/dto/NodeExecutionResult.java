package com.workflow.platform.model.dto;

import com.workflow.platform.enums.NodeType;
import lombok.Data;

import java.util.Map;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/16 00:51
 */
@Data
public class NodeExecutionResult {
    private boolean success;
    private String error;
    private Map<String, Object> output;
    private String nodeId;
    private NodeType nodeType;
    private Map<String, Object> properties;


    public boolean isSuccess() {
        return success;
    }



    // 省略getter和setter方法...
}
