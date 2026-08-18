package com.datalink.platform.engine.service;

import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.datasource.util.AesUtil;
import com.datalink.platform.engine.config.DemoBizDbInitializer;
import com.datalink.platform.engine.dto.RefineResultVO;
import com.datalink.platform.llm.dto.LlmRefineRequest;
import com.datalink.platform.llm.dto.LlmRefineResult;
import com.datalink.platform.llm.dto.RefinementItem;
import com.datalink.platform.llm.provider.ModelProvider;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.NodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * G4 大模型细化服务集成测试。
 *
 * <p>应用库用独立内存 H2（datalink_refine_test，与其它测试类隔离缓存）；
 * 引擎扫描目标是 {@link DemoBizDbInitializer} 创建的 datalink_demo 内存演示库。
 * ModelProvider 以 @MockBean 替换（不依赖真实 API key / Noop 装配）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:datalink_refine_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=secret"
})
@Transactional
class EngineRefineServiceTest {

    @Autowired
    private EngineAnalyzeService service;
    @Autowired
    private ConnectorMapper connectorMapper;
    @Autowired
    private AesUtil aesUtil;

    @MockBean
    private ModelProvider modelProvider;

    /** 造一个指向演示库的连接器（与 EngineAnalyzeServiceTest 同法，测试自包含）。 */
    private Connector demoConnector() {
        Connector c = new Connector();
        c.setConnectorType("DB");
        c.setName("演示HIS");
        c.setDbType("h2");
        c.setDatabaseName(DemoBizDbInitializer.DEMO_DB);
        c.setUsername("sa");
        c.setEncryptedPwd(aesUtil.encrypt("secret"));
        c.setEnabled(1);
        c.setIsActive(0);
        connectorMapper.insert(c);
        return c;
    }

    @Test
    void refine_merges_llm_increment() {
        Connector c = demoConnector();

        NodeVO llmNode = new NodeVO();
        llmNode.setId("llm-triage");
        llmNode.setName("分诊台");
        llmNode.setNodeType("TABLE");
        EdgeVO llmEdge = new EdgeVO();
        llmEdge.setId("llm-e0");
        llmEdge.setSource("llm-triage");
        llmEdge.setTarget("t-reg_order");
        llmEdge.setRelationType("DATA_FLOW");
        List<RefinementItem> items = List.of(
                new RefinementItem("add_node", "补充分诊台节点"),
                new RefinementItem("add_edge", "分诊台→挂号单"),
                new RefinementItem("rename", "reg_order 更名为挂号单"),
                new RefinementItem("flow", "确认挂号→收费流程"),
                new RefinementItem("note", "整体置信度良好"));

        when(modelProvider.refine(any(LlmRefineRequest.class))).thenReturn(
                LlmRefineResult.builder()
                        .addedNodes(List.of(llmNode))
                        .addedEdges(List.of(llmEdge))
                        .renameMap(Map.of("t-reg_order", "挂号单"))
                        .refinements(items)
                        .provider("mock")
                        .message("细化完成")
                        .build());

        RefineResultVO vo = service.refine(c.getId());

        // 引擎骨架原样返回
        assertNotNull(vo.getBase());
        assertFalse(vo.getBase().getCandidates().isEmpty(), "base.candidates 应非空");
        // LLM 增量与 stub 一致
        assertEquals(1, vo.getAddedNodes().size());
        assertEquals("llm-triage", vo.getAddedNodes().get(0).getId());
        assertTrue(vo.getAddedNodes().get(0).getId().startsWith("llm-"), "增量节点 id 应带 llm- 前缀");
        assertEquals(1, vo.getAddedEdges().size());
        assertEquals("llm-e0", vo.getAddedEdges().get(0).getId());
        assertEquals(Map.of("t-reg_order", "挂号单"), vo.getRenameMap());
        assertEquals(5, vo.getRefinements().size());
        assertEquals("add_node", vo.getRefinements().get(0).getType());
        assertEquals("mock", vo.getProvider());
        assertEquals("细化完成", vo.getMessage());
    }

    @Test
    void refine_provider_exception_degrades_to_base() {
        Connector c = demoConnector();
        when(modelProvider.refine(any(LlmRefineRequest.class)))
                .thenThrow(new RuntimeException("LLM 网关超时"));

        RefineResultVO vo = assertDoesNotThrow(() -> service.refine(c.getId()),
                "大模型异常不应向上抛");

        assertNotNull(vo.getBase(), "降级时 base 引擎原稿仍返回");
        assertFalse(vo.getBase().getCandidates().isEmpty());
        assertEquals("error", vo.getProvider());
        assertTrue(vo.getRefinements().stream().anyMatch(i -> "error".equals(i.getType())),
                "refinements 应含 type=error 项");
        assertTrue(vo.getAddedNodes().isEmpty());
        assertTrue(vo.getAddedEdges().isEmpty());
    }

    @Test
    void refine_null_fields_become_empty_collections() {
        Connector c = demoConnector();
        when(modelProvider.refine(any(LlmRefineRequest.class)))
                .thenReturn(LlmRefineResult.builder().provider("mock").build());

        RefineResultVO vo = service.refine(c.getId());

        assertNotNull(vo.getBase());
        assertNotNull(vo.getAddedNodes(), "null 应防御为空集合");
        assertNotNull(vo.getAddedEdges());
        assertNotNull(vo.getRenameMap());
        assertNotNull(vo.getRefinements());
        assertTrue(vo.getAddedNodes().isEmpty());
        assertTrue(vo.getAddedEdges().isEmpty());
        assertTrue(vo.getRenameMap().isEmpty());
        assertTrue(vo.getRefinements().isEmpty());
        assertEquals("mock", vo.getProvider());
    }
}
