package com.datalink.platform.llm.provider;

import com.datalink.platform.llm.dto.LlmRefineRequest;
import com.datalink.platform.llm.dto.LlmRefineResult;
import com.datalink.platform.llm.dto.RefinementItem;

import java.util.List;
import java.util.Map;

/**
 * 兜底 Provider：未配置 LLM_API_KEY 时装配，原样返回引擎草稿。
 *
 * <p>普通类，Bean 由 LlmConfig 装配（不标 @Service）。
 */
public class NoopModelProvider implements ModelProvider {

    @Override
    public String name() {
        return "noop";
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public LlmRefineResult refine(LlmRefineRequest req) {
        return LlmRefineResult.builder()
                .addedNodes(List.of())
                .addedEdges(List.of())
                .renameMap(Map.of())
                .refinements(List.of(new RefinementItem("noop", "未配置大模型 API Key，返回引擎原稿")))
                .provider(name())
                .message("未配置 LLM_API_KEY，走兜底 Provider，原样返回引擎草稿")
                .build();
    }
}
