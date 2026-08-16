package com.datalink.platform.model.service;

import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.ProcessVO;
import com.datalink.platform.model.dto.RouteVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 建模域只读装配服务测试（内存 H2 + Flyway V1~V4 种子数据）。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_model_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class GraphServiceTest {

    @Autowired
    private GraphService graphService;

    @Test
    void nodes_returns_all() {
        List<NodeVO> nodes = graphService.nodes();
        assertEquals(18, nodes.size());
    }

    @Test
    void edges_returns_all() {
        assertEquals(20, graphService.edges().size());
    }

    @Test
    void processes_assembled() {
        List<ProcessVO> processes = graphService.processes();
        assertEquals(2, processes.size());
        ProcessVO pay = processes.stream().filter(p -> "付款流程".equals(p.getName()))
                .findFirst().orElse(null);
        assertNotNull(pay, "应能找到付款流程");
        assertEquals("付款发起", pay.getStartNodeName());
        assertEquals("付款完成", pay.getEndNodeName());
        assertEquals(3, pay.getRouteCount());
        // 契约标注 nodeCount=10，按契约 SQL（COUNT DISTINCT route_node.node_id）实为 11（详见汇报）
        assertEquals(11, pay.getNodeCount());
        assertEquals(0, pay.getInstanceStats().getRunning());
        assertEquals(0, pay.getInstanceStats().getSuccess());
        assertEquals(0, pay.getInstanceStats().getFail());
    }

    @Test
    void routes_have_ordered_nodeids() {
        List<RouteVO> routes = graphService.routes();
        assertEquals(5, routes.size());
        RouteVO r3 = routes.stream().filter(r -> "3".equals(r.getId()))
                .findFirst().orElse(null);
        assertNotNull(r3, "应能找到 route id=3");
        assertEquals(List.of("9", "10", "4", "15", "16", "18"), r3.getNodeIds());
    }
}
