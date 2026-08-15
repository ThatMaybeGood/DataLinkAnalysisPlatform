package com.datalink.platform.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名 */
    private String username;

    /** 密码哈希（BCrypt）；仅反序列化写入，不序列化输出，防止泄漏 */
    @TableField("password_hash")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    /** 显示名称 */
    @TableField("display_name")
    private String displayName;

    /** 邮箱 */
    private String email;

    /** 状态：1 启用 / 0 停用 */
    private Integer status;

    /** 创建时间（插入时自动填充） */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
