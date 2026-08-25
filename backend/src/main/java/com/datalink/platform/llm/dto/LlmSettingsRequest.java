package com.datalink.platform.llm.dto;

import lombok.Data;

/**
 * 大模型接入配置保存入参（POST 新建 / PUT /api/system/llm/{id} 更新）。
 * 全部可选（新建时 name 必填）：apiKey 留空 = 不改。
 */
@Data
public class LlmSettingsRequest {
    /** 配置名（新建必填；更新可选） */
    private String name;
    /** OpenAI 兼容服务地址 */
    private String baseUrl;
    /** API Key（留空 = 不改） */
    private String apiKey;
    /** 模型名 */
    private String model;
    /** 连接 + 读取超时（毫秒） */
    private Integer timeoutMs;
    /** 最大输出 token 数 */
    private Integer maxTokens;
    /** 采样温度 */
    private Double temperature;
}
