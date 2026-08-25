package com.datalink.platform.llm.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 大模型接入配置实体（对应 llm_config 表，V9 建表 / V11 支持多行）
 * 多配置可切换：每行一个配置（name），is_active=1 为当前启用（全局唯一）。
 */
@Data
@TableName("llm_config")
public class LlmSetting {
    @TableId(type = IdType.AUTO) private Long id;
    /** 配置名（用户自定义，如「DeepSeek 生产」） */
    private String name;
    @TableField("config_key") private String configKey;
    @TableField("base_url") private String baseUrl;
    /** API Key AES-GCM 加密存储（AesUtil），接口永不返回明文 */
    @TableField("encrypted_api_key") private String encryptedApiKey;
    private String model;
    @TableField("timeout_ms") private Integer timeoutMs;
    @TableField("max_tokens") private Integer maxTokens;
    private Double temperature;
    /** 是否当前启用（1=启用，全局唯一） */
    @TableField("is_active") private Integer isActive;
    @TableField("updated_by") private String updatedBy;
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE) private LocalDateTime updatedAt;
}
