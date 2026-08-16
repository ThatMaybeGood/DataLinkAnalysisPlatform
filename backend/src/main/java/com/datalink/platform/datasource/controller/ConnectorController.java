package com.datalink.platform.datasource.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.PageResult;
import com.datalink.platform.common.Result;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.dto.CandidateNodeVO;
import com.datalink.platform.datasource.dto.ConnectorVO;
import com.datalink.platform.datasource.dto.PreviewResult;
import com.datalink.platform.datasource.dto.SaveConnectorRequest;
import com.datalink.platform.datasource.dto.TableInfo;
import com.datalink.platform.datasource.dto.TestResult;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.datasource.service.CmdbService;
import com.datalink.platform.datasource.service.ConnectorService;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.mapper.NodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据池连接器接口（9 个接口，统一 /api 前缀，返回 Result）
 */
@RestController
@RequestMapping("/api/connectors")
@RequiredArgsConstructor
public class ConnectorController {

    private final ConnectorService connectorService;
    private final ConnectorMapper connectorMapper;
    private final NodeMapper nodeMapper;
    private final CmdbService cmdbService;

    /** 分页查询连接器列表 */
    @GetMapping
    public Result<PageResult<ConnectorVO>> page(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword) {
        return Result.ok(connectorService.page(page, size, keyword));
    }

    /** 连接器详情 */
    @GetMapping("/{id}")
    public Result<ConnectorVO> detail(@PathVariable Long id) {
        return Result.ok(connectorService.getById(id));
    }

    /** 新建连接器 */
    @PostMapping
    public Result<ConnectorVO> create(@Validated @RequestBody SaveConnectorRequest req) {
        return Result.ok(connectorService.create(req));
    }

    /** 更新连接器 */
    @PutMapping("/{id}")
    public Result<ConnectorVO> update(@PathVariable Long id, @Validated @RequestBody SaveConnectorRequest req) {
        return Result.ok(connectorService.update(id, req));
    }

    /** 删除连接器 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        connectorService.delete(id);
        return Result.ok();
    }

    /** 连通性测试 */
    @PostMapping("/{id}/test")
    public Result<TestResult> test(@PathVariable Long id) {
        return Result.ok(connectorService.test(id));
    }

    /** 设为当前连接（全局唯一激活） */
    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        connectorService.activate(id);
        return Result.ok();
    }

    /** 浏览目标库表清单 */
    @GetMapping("/{id}/tables")
    public Result<List<TableInfo>> tables(@PathVariable Long id) {
        return Result.ok(connectorService.tables(id));
    }

    /** 预览目标表前 50 行 */
    @GetMapping("/{id}/tables/{table}/preview")
    public Result<PreviewResult> preview(@PathVariable Long id, @PathVariable String table) {
        return Result.ok(connectorService.preview(id, table));
    }

    /** 采集 CMDB 资产候选并缓存（返回候选数） */
    @PostMapping("/{id}/sync")
    public Result<Integer> sync(@PathVariable Long id) {
        List<CandidateNodeVO> candidates = cmdbService.fetchAssets(requireConnector(id));
        cmdbService.storeCandidates(id, candidates);
        return Result.ok(candidates.size());
    }

    /** 查看 CMDB 资产候选 */
    @GetMapping("/{id}/candidates")
    public Result<List<CandidateNodeVO>> candidates(@PathVariable Long id) {
        return Result.ok(cmdbService.getCandidates(id));
    }

    /** 一键导入候选为站点节点（code 判重，导入后清空候选） */
    @PostMapping("/{id}/import")
    public Result<Integer> importNodes(@PathVariable Long id) {
        List<CandidateNodeVO> candidates = cmdbService.getCandidates(id);
        int count = 0;
        for (int i = 0; i < candidates.size(); i++) {
            CandidateNodeVO cand = candidates.get(i);
            String code = "CMDB_" + id + "_" + i;
            if (nodeMapper.selectCount(Wrappers.lambdaQuery(Node.class).eq(Node::getCode, code)) > 0) {
                continue;
            }
            Node node = new Node();
            node.setName(cand.getName());
            node.setNodeType(cand.getType() == null || cand.getType().isBlank() ? "SYSTEM" : cand.getType());
            node.setCode(code);
            node.setLevel("L3");
            node.setStatus("ACTIVE");
            node.setDescription(cand.getDescription());
            node.setOwner(cand.getOwner());
            nodeMapper.insert(node);
            count++;
        }
        cmdbService.clear(id);
        return Result.ok(count);
    }

    /** 查询连接器实体，不存在抛 404 */
    private Connector requireConnector(Long id) {
        Connector c = connectorMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "连接不存在");
        }
        return c;
    }
}
