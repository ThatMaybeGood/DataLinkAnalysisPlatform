package com.datalink.platform.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CMDB 资产候选站点 VO（HTTP 采集映射，可一键导入 node 表）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidateNodeVO {
    private String name;
    private String type;
    private String description;
    private String owner;
}
