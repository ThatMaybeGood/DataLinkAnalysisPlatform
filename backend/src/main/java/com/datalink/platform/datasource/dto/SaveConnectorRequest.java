package com.datalink.platform.datasource.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 保存连接器请求
 */
@Data
public class SaveConnectorRequest {
    @NotBlank(message = "名称不能为空") private String name;
    @NotBlank(message = "数据库类型不能为空") private String dbType; // mysql/postgresql/h2
    private String host;            // H2 可空
    private Integer port;           // H2 可空
    @NotBlank(message = "库名不能为空") private String databaseName;
    private String schemaName;
    @NotBlank(message = "用户名不能为空") private String username;
    private String password;        // 新建必填；编辑留空=不改
    private String config;
    private Integer enabled = 1;
}
