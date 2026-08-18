package com.datalink.platform.llm.config;

import com.datalink.platform.llm.provider.ModelProvider;
import com.datalink.platform.llm.provider.NoopModelProvider;
import com.datalink.platform.llm.provider.OpenAiCompatibleModelProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * G4 大模型接入层装配：
 * 配置了 datalink.llm.api-key → OpenAI 兼容 Provider；否则 Noop 兜底直通。
 */
@Configuration
public class LlmConfig {

    @Bean
    @ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${datalink.llm.api-key:}')")
    public ModelProvider openAiModelProvider(LlmProperties props, RestClient.Builder builder) {
        return new OpenAiCompatibleModelProvider(props, builder);
    }

    @Bean
    @ConditionalOnMissingBean(ModelProvider.class)
    public ModelProvider noopModelProvider() {
        return new NoopModelProvider();
    }
}
