package com.datalink.platform.datasource.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据池连接器实体（对应 connector 表，V3 迁移扩展字段）
 */
@Data
@TableName("connector")
public class Connector {
    @TableId(type = IdType.AUTO) private Long id;
    @TableField("connector_type") private String connectorType; // 固定 "DB"
    @TableField("db_type") private String dbType;               // mysql/postgresql/h2
    private String name;
    private String host;
    private Integer port;
    private String username;
    @TableField("encrypted_pwd") private String encryptedPwd;
    @TableField("database_name") private String databaseName;
    @TableField("schema_name") private String schemaName;
    private String config;           // JSON 扩展参数
    private Integer enabled;         // 1/0
    @TableField("is_active") private Integer isActive;         // 1/0
    @TableField("last_test_status") private String lastTestStatus; // OK/FAIL
    @TableField("last_test_time") private LocalDateTime lastTestTime;
    @TableField(value = "created_at", fill = FieldFill.INSERT) private LocalDateTime createdAt;
}
