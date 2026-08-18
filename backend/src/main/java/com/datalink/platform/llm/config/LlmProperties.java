package com.datalink.platform.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * G4 大模型接入层配置（datalink.llm.*）。
 *
 * <p>OpenAI 兼容协议（DeepSeek / 通义等均可用），密钥通过环境变量 LLM_API_KEY 注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "datalink.llm")
public class LlmProperties {

    /** OpenAI 兼容服务地址（如 https://api.deepseek.com/v1） */
    private String baseUrl = "https://api.deepseek.com/v1";
    /** API Key（为空则走 Noop 兜底 Provider） */
    private String apiKey = "";
    /** 模型名 */
    private String model = "deepseek-chat";
    /** 连接 + 读取超时（毫秒） */
    private int timeoutMs = 30000;
    /** 最大输出 token 数 */
    private int maxTokens = 2048;
    /** 采样温度 */
    private Double temperature = 0.2;
}
