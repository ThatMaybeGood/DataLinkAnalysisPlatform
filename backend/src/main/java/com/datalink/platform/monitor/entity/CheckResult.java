package com.datalink.platform.monitor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测结果实体（留痕）
 */
@Data
@TableName("check_result")
public class CheckResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 所属检测点 */
    @TableField("checkpoint_id")
    private Long checkpointId;
    /** 关联实例（可为空） */
    @TableField("instance_id")
    private Long instanceId;
    /** 检测时间 */
    @TableField("check_time")
    private LocalDateTime checkTime;
    /** PASS/FAIL/TIMEOUT/SKIP */
    private String status;
    /** 检测结果值（value 为 H2 保留字，反引号引用） */
    @TableField("`value`")
    private String value;
    private String message;
}
