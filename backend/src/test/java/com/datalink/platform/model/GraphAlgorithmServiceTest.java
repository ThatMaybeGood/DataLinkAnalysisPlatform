package com.datalink.platform.model;

import com.datalink.platform.model.dto.ImpactVO;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.PathVO;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.service.GraphAlgorithmService;
import com.datalink.platform.monitor.dto.InstanceVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 图算法服务测试（内存 H2 + Flyway V1~V5 种子数据）。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_graph_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class GraphAlgorithmServiceTest {

    @Autowired
    private GraphAlgorithmService graphAlgorithmService;

    @Test
    void queryPaths_9_to_18() {
        List<PathVO> paths = graphAlgorithmService.queryPaths(9L, 18L, 8);
        assertTrue(paths.size() >= 3, "付款发起→付款完成应至少 3 条路径，实际 " + paths.size());
        List<List<String>> all = paths.stream().map(PathVO::getNodeIds).collect(Collectors.toList());
        for (PathVO p : paths) {
            // length 与 nodeIds 长度一致，节点名对齐，首尾正确
            assertEquals(p.getNodeIds().size(), p.getLength());
            assertEquals(p.getNodeIds().size(), p.getNodeNames().size());
            assertTrue(p.getLength() <= 8);
            assertEquals("9", p.getNodeIds().get(0));
            assertEquals("18", p.getNodeIds().get(p.getNodeIds().size() - 1));
        }
        // 三条分支路径均应枚举到：交易(10→4)、风控(11→12)、大额(13→14)
        assertTrue(all.contains(List.of("9", "10", "4", "15", "16", "18")), "应含标准结算分支路径");
        assertTrue(all.contains(List.of("9", "11", "12", "15", "17", "18")), "应含风控拦截分支路径");
        assertTrue(all.contains(List.of("9", "13", "14", "15", "16", "18")), "应含大额支付分支路径");
    }

    @Test
    void queryPaths_no_path() {
        List<PathVO> paths = graphAlgorithmService.queryPaths(16L, 1L, 8);
        assertNotNull(paths);
        assertTrue(paths.isEmpty(), "记账(16)无法到达订单门户(1)，应返回空 list");
    }

    @Test
    void impact_13() {
        ImpactVO impact = graphAlgorithmService.impact(13L);
        // 下游：含流水表(14) 与结算部门(15)
        List<String> downstreamIds = impact.getDownstream().stream().map(NodeVO::getId).collect(Collectors.toList());
        assertTrue(downstreamIds.contains("14"), "下游应含流水表(14)");
        assertTrue(downstreamIds.contains("15"), "下游应含结算部门(15)");
        // 受影响路线：含路线 3/4/5
        List<String> routeIds = impact.getAffectedRoutes().stream().map(RouteVO::getId).collect(Collectors.toList());
        assertTrue(routeIds.contains("3"), "受影响路线应含路线 3");
        assertTrue(routeIds.contains("4"), "受影响路线应含路线 4");
        assertTrue(routeIds.contains("5"), "受影响路线应含路线 5");
        // 受影响实例：含大额对公付款（current=13）
        List<String> bizNames = impact.getAffectedInstances().stream().map(InstanceVO::getBizName)
                .collect(Collectors.toList());
        assertTrue(bizNames.contains("大额对公付款"), "受影响实例应含大额对公付款");
    }
}
