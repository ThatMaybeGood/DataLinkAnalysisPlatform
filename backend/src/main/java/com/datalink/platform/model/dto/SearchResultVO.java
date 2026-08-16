package com.datalink.platform.model.dto;

import lombok.Data;

/**
 * 全局搜索结果视图对象
 */
@Data
public class SearchResultVO {
    private String targetType;
    private String targetId;
    private String name;
    private String code;
}
