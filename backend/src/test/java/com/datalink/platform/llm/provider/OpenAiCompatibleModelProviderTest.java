package com.datalink.platform.llm.provider;

import com.datalink.platform.llm.dto.LlmRefineResult;
import com.datalink.platform.llm.dto.LlmSettingsView;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OpenAI 兼容 Provider 输出解析纯单测（不起 Spring 上下文、不打真实 HTTP）。
 * 主要覆盖 parseContent 静态方法的健壮性。
 */
class OpenAiCompatibleModelProviderTest {

    private OpenAiCompatibleModelProvider newProvider() {
        LlmSettingsView view = LlmSettingsView.builder()
                .baseUrl("https://api.deepseek.com/v1")
                .apiKey("test-key")
                .model("deepseek-chat")
                .timeoutMs(30000)
                .maxTokens(2048)
                .temperature(0.2)
                .build();
        return new OpenAiCompatibleModelProvider(view, RestClient.builder());
    }

    @Test
    void parse_valid_json_with_all_fields() {
        newProvider(); // 确保构造不抛异常
        String json = "{"
                + "\"addedNodes\":[{\"id\":\"llm-billing\",\"name\":\"统一结算中心\",\"code\":\"SETTLE\","
                + "\"nodeType\":\"TABLE\",\"level\":\"L2\",\"status\":\"ACTIVE\"}],"
                + "\"addedEdges\":[{\"id\":\"llm-e1\",\"source\":\"llm-billing\",\"target\":\"reg_order\",\"relationType\":\"DATA_FLOW\"}],"
                + "\"renameMap\":{\"reg_order\":\"挂号单\"},"
                + "\"refinements\":["
                + "{\"type\":\"rename\",\"text\":\"挂号单命名规范化\"},"
                + "{\"type\":\"chain\",\"text\":\"补全收费链路\"},"
                + "{\"type\":\"party\",\"text\":\"补充医保局参与方\"},"
                + "{\"type\":\"relation\",\"text\":\"修正退费与收费关系\"},"
                + "{\"type\":\"flow\",\"text\":\"补全流程模板\"}],"
                + "\"message\":\"润色完成\""
                + "}";
        LlmRefineResult result = OpenAiCompatibleModelProvider.parseContent(json, "openai-compatible");

        assertEquals("openai-compatible", result.getProvider());
        assertEquals("润色完成", result.getMessage());
        assertEquals(1, result.getAddedNodes().size());
        assertEquals("llm-billing", result.getAddedNodes().get(0).getId());
        assertEquals("统一结算中心", result.getAddedNodes().get(0).getName());
        assertEquals("TABLE", result.getAddedNodes().get(0).getNodeType());
        assertEquals(1, result.getAddedEdges().size());
        assertEquals("DATA_FLOW", result.getAddedEdges().get(0).getRelationType());
        assertEquals("挂号单", result.getRenameMap().get("reg_order"));
        assertEquals(5, result.getRefinements().size());
    }

    @Test
    void parse_strips_json_code_fence() {
        String fenced = "```json\n"
                + "{\"addedNodes\":[],\"addedEdges\":[],\"renameMap\":{},"
                + "\"refinements\":[{\"type\":\"flow\",\"text\":\"仅流程补充\"}],\"message\":\"ok\"}"
                + "\n```";
        LlmRefineResult result = OpenAiCompatibleModelProvider.parseContent(fenced, "openai-compatible");

        assertEquals("ok", result.getMessage());
        assertEquals(1, result.getRefinements().size());
        assertEquals("flow", result.getRefinements().get(0).getType());
    }

    @Test
    void parse_illegal_json_falls_back_to_error() {
        LlmRefineResult result = OpenAiCompatibleModelProvider.parseContent("I cannot help", "openai-compatible");

        assertNotNull(result);
        assertTrue(result.getAddedNodes().isEmpty());
        assertTrue(result.getAddedEdges().isEmpty());
        assertTrue(result.getRenameMap().isEmpty());
        assertEquals(1, result.getRefinements().size());
        assertEquals("error", result.getRefinements().get(0).getType());
        assertEquals("openai-compatible", result.getProvider());
    }

    @Test
    void parse_missing_fields_defaults_to_empty() {
        String json = "{\"refinements\":[{\"type\":\"rename\",\"text\":\"只给了说明\"}]}";
        LlmRefineResult result = OpenAiCompatibleModelProvider.parseContent(json, "openai-compatible");

        assertNotNull(result.getAddedNodes());
        assertNotNull(result.getAddedEdges());
        assertNotNull(result.getRenameMap());
        assertTrue(result.getAddedNodes().isEmpty());
        assertTrue(result.getAddedEdges().isEmpty());
        assertTrue(result.getRenameMap().isEmpty());
        assertEquals(1, result.getRefinements().size());
    }

    @Test
    void parse_node_id_gets_llm_prefix_when_missing() {
        String json = "{\"addedNodes\":[{\"id\":\"billing\",\"name\":\"结算\"}]}";
        LlmRefineResult result = OpenAiCompatibleModelProvider.parseContent(json, "openai-compatible");
        assertEquals("llm-billing", result.getAddedNodes().get(0).getId());
    }
}
