package com.datalink.platform.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存连接器请求
 */
@Data
public class SaveConnectorRequest {
    @NotBlank(message = "名称不能为空") private String name;
    /** 连接器类型：DB（默认，JDBC）/ CMDB（HTTP 采集）等 */
    private String connectorType;
    /** 数据库类型 mysql/postgresql/h2（CMDB 类型可不填） */
    private String dbType;
    private String host;            // H2 可空
    private Integer port;           // H2 可空
    private String databaseName;    // DB 类型必填；CMDB 可空
    private String schemaName;
    private String username;        // DB 类型必填；CMDB 可空
    private String password;        // 新建必填；编辑留空=不改
    private String config;
    private Integer enabled = 1;
}
