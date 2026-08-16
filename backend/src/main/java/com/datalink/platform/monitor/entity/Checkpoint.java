package com.datalink.platform.monitor.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测点实体（每个站点可挂多个）
 */
@Data
@TableName("checkpoint")
public class Checkpoint {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属站点节点 */
    @TableField("node_id")
    private Long nodeId;
    private String name;
    /** DEFAULT 系统默认 / CUSTOM 自定义 */
    private String kind;
    /** DATA_VOLUME/FRESHNESS/DELAY/INTEGRITY/SERVICE_STATUS/SQL/SCRIPT/THRESHOLD */
    @TableField("check_type")
    private String checkType;
    /** 规则参数（阈值/SQL/频率等，JSON 文本） */
    private String params;
    private String freq;
    private String level;
    /** 是否启用 1/0 */
    private Integer enabled;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
