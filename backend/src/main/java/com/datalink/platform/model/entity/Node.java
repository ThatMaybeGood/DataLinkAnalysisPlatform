package com.datalink.platform.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点节点实体（系统/库表/部门岗位/业务动作/制造设备等）
 */
@Data
@TableName("node")
public class Node {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 节点类型 SYSTEM/SUBSYSTEM/DATABASE/TABLE/... */
    @TableField("node_type")
    private String nodeType;
    /** 显示名（主别名） */
    private String name;
    /** 唯一编码 */
    private String code;
    /** 库类型 MYSQL/ORACLE/REDIS... 或系统类别 */
    @TableField("sub_type")
    private String subType;
    private String ip;
    /** 级别 L1核心/L2重要/L3普通/L4一般 */
    private String level;
    private String status;
    /** 责任人 */
    private String owner;
    private String description;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
