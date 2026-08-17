package com.datalink.platform.engine.service;

import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.datasource.util.AesUtil;
import com.datalink.platform.engine.config.DemoBizDbInitializer;
import com.datalink.platform.engine.dto.EngineCandidateVO;
import com.datalink.platform.engine.dto.EngineDraftVO;
import com.datalink.platform.engine.dto.EngineFlowVO;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.NodeVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图来源引擎分析集成测试。
 *
 * <p>应用库用独立内存 H2（datalink_engine_test，与其它测试类隔离缓存）；
 * 引擎扫描目标是 {@link DemoBizDbInitializer} 创建的 datalink_demo 内存演示库
 * （同一 JVM 内 DB_CLOSE_DELAY=-1 常驻）。@Transactional 隔离写不影响扫描结果。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:datalink_engine_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=secret"
})
@Transactional
class EngineAnalyzeServiceTest {

    @Autowired
    private EngineAnalyzeService service;
    @Autowired
    private ConnectorMapper connectorMapper;
    @Autowired
    private AesUtil aesUtil;

    /** 直接造一个指向演示库的连接器（不依赖种子，测试自包含）。
     * 演示库由 DemoBizDbInitializer（ApplicationRunner）在建 WebApplicationContext 时就绪。 */
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
    void diagnose_tables() {
        Connector c = demoConnector();
        EngineDraftVO draft = service.analyze(c.getId());
        // 打印实际识别的候选与节点，便于校准
        System.out.println("[G3-diag] candidates=" + draft.getCandidates().size());
        for (EngineCandidateVO vo : draft.getCandidates()) {
            System.out.println("[G3-diag]   - " + vo.getTable() + " / " + vo.getName()
                    + " / score~" + vo.getConfidence() + " marks=" + vo.getMarks());
        }
        System.out.println("[G3-diag] nodes=" + draft.getDraftNodes().size());
        for (NodeVO n : draft.getDraftNodes()) {
            System.out.println("[G3-diag]   - " + n.getId() + " type=" + n.getNodeType()
                    + " name=" + n.getName());
        }
        System.out.println("[G3-diag] edges=" + draft.getDraftEdges().size());
        for (EdgeVO e : draft.getDraftEdges()) {
            System.out.println("[G3-diag]   - " + e.getId() + " " + e.getSource() + " -> "
                    + e.getTarget() + " type=" + e.getRelationType());
        }
        System.out.println("[G3-diag] flows=" + draft.getFlows().size());
        for (EngineFlowVO f : draft.getFlows()) {
            System.out.println("[G3-diag]   - " + f.getName() + " ids=" + f.getNodeIds());
        }
        // 断言业务候选数 = 6
        assertEquals(6, draft.getCandidates().size(), "候选单据应为 6 张演示表");
    }

    @Test
    void analyze_demo_db_recognizes_six_candidates() {
        Connector c = demoConnector();
        EngineDraftVO draft = service.analyze(c.getId());

        // 六表全部识别为候选
        assertEquals(6, draft.getCandidates().size(), "应识别 6 个候选单据");
        Map<String, EngineCandidateVO> byTable = draft.getCandidates().stream()
                .collect(Collectors.toMap(EngineCandidateVO::getTable, x -> x, (a, b) -> a));

        // 表名齐全
        for (String t : List.of("reg_order", "fee_order", "refund_apply", "settle_bill", "pay_record", "prescription_detail")) {
            assertTrue(byTable.containsKey(t), "缺少候选: " + t);
        }

        // 主单应命中主键 + 单号信号；明细表低置信且带主子表信号
        assertTrue(byTable.get("reg_order").getMarks().contains("主键"));
        assertTrue(byTable.get("reg_order").getMarks().contains("单号"));
        assertTrue(byTable.get("prescription_detail").getMarks().contains("主子表"),
                "处方明细应有主子表信号");
        assertTrue(byTable.get("prescription_detail").isLow(),
                "明细表应为低置信（实测 " + byTable.get("prescription_detail").getConfidence() + "）");
    }

    @Test
    void analyze_produces_draft_graph() {
        Connector c = demoConnector();
        EngineDraftVO draft = service.analyze(c.getId());

        // 1 个库节点 + 6 表节点
        assertNotNull(draft.getDatabase());
        List<NodeVO> nodes = draft.getDraftNodes();
        assertEquals(7, nodes.size(), "草稿节点 = 1 库 + 6 表");
        assertEquals(1, nodes.stream().filter(n -> "DATABASE".equals(n.getNodeType())).count());
        assertEquals(6, nodes.stream().filter(n -> "TABLE".equals(n.getNodeType())).count());

        // 边：库→表 6 条 + 引用方向若干
        List<EdgeVO> edges = draft.getDraftEdges();
        assertTrue(edges.size() >= 6, "至少 6 条库→表边");

        // 流程模板至少 1 条
        assertFalse(draft.getFlows().isEmpty(), "应推导出至少 1 条流程模板");
        for (EngineFlowVO f : draft.getFlows()) {
            assertTrue(f.getNodeIds().size() >= 2, "流程应含 ≥2 站");
            assertEquals(f.getNodeIds().size(), f.getTableNames().size());
        }
    }

    @Test
    void analyze_confidence_in_spec_range() {
        Connector c = demoConnector();
        EngineDraftVO draft = service.analyze(c.getId());
        for (EngineCandidateVO vo : draft.getCandidates()) {
            assertTrue(vo.getConfidence() >= 5 && vo.getConfidence() <= 100,
                    vo.getTable() + " 置信度越界: " + vo.getConfidence());
        }
        // 设计 15.3：主表识别置信度区间 60~85——强主表（挂号单/收费单）应落在此区间
        Map<String, EngineCandidateVO> byTable = draft.getCandidates().stream()
                .collect(Collectors.toMap(EngineCandidateVO::getTable, x -> x, (a, b) -> a));
        EngineCandidateVO reg = byTable.get("reg_order");
        assertTrue(reg.getConfidence() >= 60 && reg.getConfidence() <= 85,
                "主表置信度应在 60~85 区间，实际 " + reg.getConfidence());
        assertTrue(reg.getMarks().contains("引用"),
                "挂号单被收费单/退费单引用，应命中引用信号");
    }

    @Test
    void analyze_missing_connector_404() {
        assertThrows(BusinessException.class, () -> service.analyze(999999L));
    }

    @Test
    void analyze_disabled_connector_rejected() {
        Connector c = demoConnector();
        c.setEnabled(0);
        connectorMapper.updateById(c);
        assertThrows(BusinessException.class, () -> service.analyze(c.getId()));
    }
}
