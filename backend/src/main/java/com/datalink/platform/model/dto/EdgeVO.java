package com.datalink.platform.model.dto;

import lombok.Data;

/**
 * 路网边视图对象
 */
@Data
public class EdgeVO {
    private String id;
    private String source;
    private String target;
    private String relationType;
}
