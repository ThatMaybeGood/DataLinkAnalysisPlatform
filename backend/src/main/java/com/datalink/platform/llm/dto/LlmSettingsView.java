package com.datalink.platform.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 大模型接入配置视图（当前生效 / 配置列表项；Provider 内部使用）。
 * <p>apiKey 仅内存使用（@JsonIgnore 绝不序列化出去），对外只出掩码 apiKeyMasked。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmSettingsView {
    /** 配置 id（DB 配置才有；环境变量来源为 null） */
    private Long id;
    /** 配置名（DB 配置才有） */
    private String name;
    /** 是否当前启用（仅 DB 配置有意义）；固定 JSON 字段名 isActive（避免 boolean 反序列化为 active） */
    @JsonProperty("isActive")
    private boolean isActive;
    private String baseUrl;
    /** 明文 Key，仅内存；响应不输出 */
    @JsonIgnore
    private String apiKey;
    private String model;
    private Integer timeoutMs;
    private Integer maxTokens;
    private Double temperature;
    /** 掩码显示（如 sk-****abcd），未配置为空串 */
    private String apiKeyMasked;
    private boolean hasKey;
    /** 配置来源：env（环境变量）/ db（数据库覆盖） */
    private String source;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
