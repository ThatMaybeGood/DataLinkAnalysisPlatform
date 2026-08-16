package com.datalink.platform.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Process;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.monitor.dto.DashboardStatsVO;
import com.datalink.platform.monitor.entity.Alert;
import com.datalink.platform.monitor.entity.Checkpoint;
import com.datalink.platform.monitor.entity.Instance;
import com.datalink.platform.monitor.entity.InstanceNode;
import com.datalink.platform.monitor.mapper.AlertMapper;
import com.datalink.platform.monitor.mapper.CheckpointMapper;
import com.datalink.platform.monitor.mapper.InstanceMapper;
import com.datalink.platform.monitor.mapper.InstanceNodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控仪表盘统计服务：总量指标 / 覆盖率 / 平均耗时 / 慢站点 TopN / 运行趋势。
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProcessMapper processMapper;
    private final InstanceMapper instanceMapper;
    private final AlertMapper alertMapper;
    private final NodeMapper nodeMapper;
    private final CheckpointMapper checkpointMapper;
    private final InstanceNodeMapper instanceNodeMapper;

    /** 仪表盘聚合统计 */
    public DashboardStatsVO stats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        vo.setProcessCount(selectCount(processMapper.selectCount(Wrappers.lambdaQuery(Process.class))));
        vo.setRunningInstances(countInstance("RUNNING"));
        vo.setOpenAlerts(countAlert("OPEN"));
        vo.setStuckCount(countInstance("STUCK"));

        // 今日完成：status=SUCCESS 且 start_time 为今天
        List<Instance> successInsts = instanceMapper.selectList(Wrappers.lambdaQuery(Instance.class)
                .eq(Instance::getStatus, "SUCCESS"));
        LocalDate today = LocalDate.now();
        vo.setDoneToday((int) successInsts.stream()
                .filter(i -> i.getStartTime() != null && i.getStartTime().toLocalDate().isEqual(today))
                .count());

        // 检测点覆盖率：有 ≥1 个检测点的节点数 / 节点总数 ×100
        long totalNodes = selectCount(nodeMapper.selectCount(Wrappers.lambdaQuery(Node.class)));
        long coveredNodes = checkpointMapper.selectList(Wrappers.lambdaQuery(Checkpoint.class)
                        .select(Checkpoint::getNodeId))
                .stream().map(Checkpoint::getNodeId).distinct().count();
        vo.setCheckpointCoverage(totalNodes == 0 ? 0 : (int) (coveredNodes * 100 / totalNodes));

        // 平均耗时：SUCCESS 实例 total_duration_ms 均值
        long sum = successInsts.stream().filter(i -> i.getTotalDurationMs() != null)
                .mapToLong(Instance::getTotalDurationMs).sum();
        long cnt = successInsts.stream().filter(i -> i.getTotalDurationMs() != null).count();
        vo.setAvgDuration(cnt == 0 ? "0m" : formatDuration(sum / cnt));

        vo.setTopSlowNodes(topSlowNodes());
        vo.setInstanceTrend(instanceTrend());
        return vo;
    }

    private int countInstance(String status) {
        return selectCount(instanceMapper.selectCount(
                Wrappers.lambdaQuery(Instance.class).eq(Instance::getStatus, status)));
    }

    private int countAlert(String status) {
        return selectCount(alertMapper.selectCount(
                Wrappers.lambdaQuery(Alert.class).eq(Alert::getStatus, status)));
    }

    /** 按 instance_node 聚合各节点总耗时，取前 3 */
    private List<DashboardStatsVO.SlowNodeItem> topSlowNodes() {
        List<InstanceNode> list = instanceNodeMapper.selectList(Wrappers.lambdaQuery(InstanceNode.class)
                .isNotNull(InstanceNode::getDurationMs));
        Map<Long, Long> agg = new HashMap<>();
        for (InstanceNode in : list) {
            agg.merge(in.getNodeId(), in.getDurationMs(), Long::sum);
        }
        return agg.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(e -> {
                    Node node = nodeMapper.selectById(e.getKey());
                    return new DashboardStatsVO.SlowNodeItem(
                            node == null ? null : node.getName(), formatDuration(e.getValue()));
                })
                .collect(Collectors.toList());
    }

    /** 最近 8 小时逐小时实例创建数 */
    private List<DashboardStatsVO.TrendItem> instanceTrend() {
        List<Instance> all = instanceMapper.selectList(Wrappers.lambdaQuery(Instance.class));
        LocalDateTime curHour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        List<DashboardStatsVO.TrendItem> items = new ArrayList<>(8);
        for (int i = 7; i >= 0; i--) {
            LocalDateTime start = curHour.minusHours(i);
            LocalDateTime end = start.plusHours(1);
            String label = start.format(DateTimeFormatter.ofPattern("HH:00"));
            int value = (int) all.stream().filter(inst -> inst.getCreatedAt() != null
                    && !inst.getCreatedAt().isBefore(start) && inst.getCreatedAt().isBefore(end)).count();
            items.add(new DashboardStatsVO.TrendItem(label, value));
        }
        return items;
    }

    private int selectCount(Long count) {
        return count == null ? 0 : count.intValue();
    }

    /** 毫秒格式化：「Xh Ym」或「Xm」 */
    private String formatDuration(long ms) {
        long minutes = ms / 60000;
        if (minutes >= 60) {
            return (minutes / 60) + "h " + (minutes % 60) + "m";
        }
        return minutes + "m";
    }
}
