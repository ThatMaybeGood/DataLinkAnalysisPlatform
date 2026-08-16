package com.datalink.platform.monitor;

import com.datalink.platform.common.PageResult;
import com.datalink.platform.monitor.dto.AlertVO;
import com.datalink.platform.model.dto.CheckpointVO;
import com.datalink.platform.monitor.dto.DashboardStatsVO;
import com.datalink.platform.monitor.dto.InstanceNodeVO;
import com.datalink.platform.monitor.dto.InstanceVO;
import com.datalink.platform.monitor.dto.SaveCheckpointRequest;
import com.datalink.platform.monitor.dto.SaveInstanceRequest;
import com.datalink.platform.monitor.service.AlertService;
import com.datalink.platform.monitor.service.CheckpointService;
import com.datalink.platform.monitor.service.DashboardService;
import com.datalink.platform.monitor.service.InstanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 监控域服务测试（内存 H2 + Flyway V1~V5 种子数据）。
 * 每个测试方法事务回滚，避免写入相互污染。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_monitor_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Transactional
class MonitorServiceTest {

    @Autowired
    private InstanceService instanceService;
    @Autowired
    private CheckpointService checkpointService;
    @Autowired
    private AlertService alertService;
    @Autowired
    private DashboardService dashboardService;

    @Test
    void instance_page_returns_all_with_process_name() {
        PageResult<InstanceVO> page = instanceService.page(1, 20, null);
        assertEquals(6, page.getTotal());
        List<InstanceVO> records = page.getRecords();
        assertEquals(6, records.size());
        assertTrue(records.stream().allMatch(v -> v.getProcessName() != null), "每个实例都应装配流程名");
    }

    @Test
    void instance_nodes_are_ordered_by_seq() {
        List<InstanceNodeVO> nodes = instanceService.instanceNodes(1L);
        assertFalse(nodes.isEmpty());
        List<Integer> seqs = nodes.stream()
                .map(v -> Integer.valueOf(v.getSeq()))
                .collect(Collectors.toList());
        assertEquals(List.of(1, 2, 3, 4, 5, 6), seqs, "实例 1 的站点 seq 应 1~6 有序");
    }

    @Test
    void alerts_list_has_target_name() {
        List<AlertVO> alerts = alertService.list(null);
        assertEquals(5, alerts.size());
        assertTrue(alerts.stream().allMatch(a -> a.getTargetName() != null), "告警目标名应可解析");
    }

    @Test
    void resolve_alert_marks_resolved() {
        alertService.resolve(1L);
        List<AlertVO> resolved = alertService.list("RESOLVED");
        assertTrue(resolved.stream().anyMatch(a -> "1".equals(a.getId())), "告警 1 应变为 RESOLVED");
    }

    @Test
    void dashboard_stats_expected() {
        DashboardStatsVO stats = dashboardService.stats();
        assertEquals(2, stats.getProcessCount());
        assertEquals(3, stats.getOpenAlerts());
        assertEquals(2, stats.getRunningInstances());
        assertEquals(1, stats.getStuckCount());
        assertTrue(stats.getCheckpointCoverage() >= 1, "检测点覆盖率应至少 1%");
    }

    @Test
    void create_instance_generates_node_trail() {
        SaveInstanceRequest req = new SaveInstanceRequest();
        req.setBizNo("TEST-001");
        req.setBizName("测试实例");
        req.setProcessId(2L);
        req.setRouteId(3L);
        req.setNodeIds(List.of(9L, 10L, 13L));
        InstanceVO vo = instanceService.create(req);
        assertNotNull(vo.getId());
        List<InstanceNodeVO> nodes = instanceService.instanceNodes(Long.valueOf(vo.getId()));
        assertEquals(3, nodes.size());
        assertEquals("9", nodes.get(0).getNodeId());
        assertEquals("RUNNING", nodes.get(0).getStatus());
        assertEquals("PENDING", nodes.get(1).getStatus());
    }

    @Test
    void create_checkpoint_persists() {
        SaveCheckpointRequest req = new SaveCheckpointRequest();
        req.setNodeId(3L);
        req.setName("测试检测点");
        req.setCheckType("SQL");
        CheckpointVO vo = checkpointService.create(req);
        assertNotNull(vo.getId());
        List<CheckpointVO> list = checkpointService.listByNode(3L);
        assertTrue(list.stream().anyMatch(c -> c.getId().equals(vo.getId())), "新建检测点应可查询到");
    }
}
