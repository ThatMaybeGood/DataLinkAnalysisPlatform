package com.datalink.platform.model.dto;

import lombok.Data;

/**
 * 模式库新增请求
 */
@Data
public class PatternLibraryDTO {

    /** NODE_NAME/EDGE_NAME/ROUTE_TEMPLATE */
    private String patternType;

    /** 模式匹配键 */
    private String patternKey;

    /** 模式值/模板 */
    private String patternValue;

    /** NODE/EDGE/ROUTE/PATTERN */
    private String sourceType;

    /** 来源对象 id */
    private String sourceId;

    /** 来源操作类型 */
    private String sourceOperation;
}
