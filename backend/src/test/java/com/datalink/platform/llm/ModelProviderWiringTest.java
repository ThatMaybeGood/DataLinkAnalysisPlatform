package com.datalink.platform.llm;

import com.datalink.platform.llm.dto.LlmRefineRequest;
import com.datalink.platform.llm.dto.LlmRefineResult;
import com.datalink.platform.llm.provider.ModelProvider;
import com.datalink.platform.llm.provider.RuntimeModelProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G4 大模型接入层装配测试：默认环境无 LLM_API_KEY 时，
 * 上下文中装配唯一的 RuntimeModelProvider（运行时分发，无 Key 时内部走 Noop 兜底）。
 *
 * <p>应用库用独立内存 H2（datalink_llm_test，与其它测试类隔离缓存）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:datalink_llm_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=secret",
        "datalink.llm.api-key="
})
class ModelProviderWiringTest {

    @Autowired
    private ModelProvider modelProvider;

    @Test
    void runtime_provider_wired_and_noop_when_no_api_key() {
        assertInstanceOf(RuntimeModelProvider.class, modelProvider,
                "G4 后装配运行时 RuntimeModelProvider，按当前配置分发");
        // 无 Key：对外 name()=noop、available()=false，refine 内部走 Noop 兜底（见 noop_refine_passthrough）
        assertEquals("noop", modelProvider.name());
        assertFalse(modelProvider.available());
    }

    @Test
    void noop_refine_passthrough() {
        LlmRefineRequest req = LlmRefineRequest.builder()
                .database("演示库")
                .candidates(List.of())
                .flows(List.of())
                .build();
        LlmRefineResult result = modelProvider.refine(req);

        assertEquals("noop", result.getProvider());
        assertTrue(result.getAddedNodes().isEmpty());
        assertTrue(result.getAddedEdges().isEmpty());
        assertTrue(result.getRenameMap().isEmpty());
        assertEquals(1, result.getRefinements().size());
        assertEquals("noop", result.getRefinements().get(0).getType());
    }
}
