package com.datalink.platform.monitor;

import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.monitor.dto.TraceResultVO;
import com.datalink.platform.monitor.service.TraceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 链路追踪服务测试（内存 H2 + Flyway V1~V5 种子数据）。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_monitor_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class TraceServiceTest {

    @Autowired
    private TraceService traceService;

    @Test
    void trace_finds_upstream_and_downstream() {
        TraceResultVO trace = traceService.trace(13L);
        assertEquals("13", trace.getNodeId());
        List<String> upstream = trace.getUpstream().stream().map(NodeVO::getId).collect(Collectors.toList());
        List<String> downstream = trace.getDownstream().stream().map(NodeVO::getId).collect(Collectors.toList());
        assertTrue(upstream.contains("9"), "上游应含付款发起(9)");
        assertTrue(downstream.contains("14"), "下游应含流水表(14)");
        assertTrue(downstream.contains("15"), "下游应含结算部门(15)");
    }

    @Test
    void routes_of_node_contains_expected_routes() {
        List<RouteVO> routes = traceService.routesOfNode(15L);
        List<String> ids = routes.stream().map(RouteVO::getId).collect(Collectors.toList());
        assertTrue(ids.contains("3"), "结算部门应属于路线 3");
        assertTrue(ids.contains("4"), "结算部门应属于路线 4");
        assertTrue(ids.contains("5"), "结算部门应属于路线 5");
    }
}
