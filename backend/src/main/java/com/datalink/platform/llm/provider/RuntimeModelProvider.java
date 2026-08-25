package com.datalink.platform.llm.provider;

import com.datalink.platform.llm.dto.LlmRefineRequest;
import com.datalink.platform.llm.dto.LlmRefineResult;
import com.datalink.platform.llm.dto.LlmSettingsView;
import com.datalink.platform.llm.service.LlmSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * 运行时大模型 Provider（唯一 ModelProvider Bean，替代原条件装配的 LlmConfig）。
 * <p>每次调用按当前生效配置分发：有 Key → OpenAI 兼容；无 Key → Noop 兜底（provider="noop"，
 * 保持前端 refineResult.provider==='noop' 判定不变）。保存 llm_config 后即时生效，无需重启。
 */
@Service
@RequiredArgsConstructor
public class RuntimeModelProvider implements ModelProvider {

    private final LlmSettingsService llmSettingsService;
    private final RestClient.Builder restClientBuilder;

    @Override
    public String name() {
        return llmSettingsService.getView().isHasKey() ? "openai-compatible" : "noop";
    }

    @Override
    public boolean available() {
        return llmSettingsService.getView().isHasKey();
    }

    @Override
    public LlmRefineResult refine(LlmRefineRequest req) {
        LlmSettingsView view = llmSettingsService.getView();
        if (!view.isHasKey()) {
            return new NoopModelProvider().refine(req);
        }
        return new OpenAiCompatibleModelProvider(view, restClientBuilder).refine(req);
    }
}
