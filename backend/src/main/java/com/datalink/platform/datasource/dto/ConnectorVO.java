package com.datalink.platform.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 连接器视图对象（脱敏：绝不含 password / encryptedPwd 字段）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectorVO {
    private Long id; private String connectorType; private String dbType; private String name;
    private String host; private Integer port; private String username; private String databaseName;
    private String schemaName; private Integer enabled; private Integer isActive;
    private String lastTestStatus; private LocalDateTime lastTestTime; private LocalDateTime createdAt;
    // 无 password / encryptedPwd 字段
}
