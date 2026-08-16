package com.datalink.platform.openapi.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.dto.ProcessVO;
import com.datalink.platform.model.service.GraphService;
import com.datalink.platform.monitor.dto.InstanceVO;
import com.datalink.platform.monitor.dto.SaveInstanceRequest;
import com.datalink.platform.monitor.entity.CheckResult;
import com.datalink.platform.monitor.entity.Instance;
import com.datalink.platform.monitor.mapper.CheckResultMapper;
import com.datalink.platform.monitor.mapper.InstanceMapper;
import com.datalink.platform.monitor.service.InstanceService;
import com.datalink.platform.openapi.dto.OpenInstanceReport;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开放 API：供外部系统集成使用，鉴权由 OpenApiAuthFilter 的 X-API-Key 独立完成。
 * 能力：上报/更新实例、查询流程与节点、触发检测点检测。
 */
@RestController
@RequestMapping("/api/open")
@RequiredArgsConstructor
public class OpenApiController {

    private final InstanceMapper instanceMapper;
    private final InstanceService instanceService;
    private final GraphService graphService;
    private final CheckResultMapper checkResultMapper;

    /** 上报实例：按 bizNo 幂等，存在则更新、不存在则新建 */
    @PostMapping("/instances")
    public Result<InstanceVO> reportInstance(@Validated @RequestBody OpenInstanceReport report) {
        Instance existing = instanceMapper.selectOne(Wrappers.lambdaQuery(Instance.class)
                .eq(Instance::getBizNo, report.getBizNo()));
        SaveInstanceRequest req = new SaveInstanceRequest();
        req.setBizNo(report.getBizNo());
        req.setBizName(report.getBizName());
        req.setProcessId(report.getProcessId());
        req.setRouteId(report.getRouteId());
        req.setStatus(report.getStatus());
        if (existing != null) {
            return Result.ok(instanceService.update(existing.getId(), req));
        }
        return Result.ok(instanceService.create(req));
    }

    /** 查询全部流程 */
    @GetMapping("/processes")
    public Result<List<ProcessVO>> processes() {
        return Result.ok(graphService.processes());
    }

    /** 查询全部站点节点 */
    @GetMapping("/nodes")
    public Result<List<NodeVO>> nodes() {
        return Result.ok(graphService.nodes());
    }

    /** 触发检测点检测：向 check_result 写入一条 PASS 留痕 */
    @PostMapping("/checkpoints/{id}/trigger")
    public Result<Map<String, Object>> triggerCheckpoint(@PathVariable Long id) {
        CheckResult cr = new CheckResult();
        cr.setCheckpointId(id);
        cr.setStatus("PASS");
        cr.setCheckTime(LocalDateTime.now());
        cr.setMessage("开放API触发");
        checkResultMapper.insert(cr);
        Map<String, Object> data = new HashMap<>();
        data.put("checkpointId", id);
        data.put("status", "PASS");
        data.put("time", cr.getCheckTime());
        return Result.ok(data);
    }
}
