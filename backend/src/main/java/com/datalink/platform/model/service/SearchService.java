package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.model.dto.SearchResultVO;
import com.datalink.platform.model.entity.Alias;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Process;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.mapper.AliasMapper;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局搜索服务：单号 / 名称 / 别名通吃，覆盖节点 / 流程 / 路线 / 别名，结果上限 50 条。
 * 别名命中直接映射到目标对象（targetType 取 DB 原值 NODE/PROCESS/ROUTE）。
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final int MAX_RESULTS = 50;

    private final NodeMapper nodeMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;
    private final AliasMapper aliasMapper;

    /** 全局搜索：q 为空返回空列表 */
    public List<SearchResultVO> search(String q) {
        if (q == null || q.isBlank()) {
            return new ArrayList<>();
        }
        String kw = q.trim();
        List<SearchResultVO> results = new ArrayList<>();

        // 节点：名称或编码模糊
        List<Node> nodes = nodeMapper.selectList(Wrappers.lambdaQuery(Node.class)
                .like(Node::getName, kw).or().like(Node::getCode, kw));
        for (Node n : nodes) {
            results.add(nodeResult(n));
            if (results.size() >= MAX_RESULTS) {
                return results;
            }
        }

        // 流程：名称或编码模糊
        List<Process> processes = processMapper.selectList(Wrappers.lambdaQuery(Process.class)
                .like(Process::getName, kw).or().like(Process::getCode, kw));
        for (Process p : processes) {
            results.add(processResult(p));
            if (results.size() >= MAX_RESULTS) {
                return results;
            }
        }

        // 路线：名称模糊
        List<Route> routes = routeMapper.selectList(Wrappers.lambdaQuery(Route.class)
                .like(Route::getName, kw));
        for (Route r : routes) {
            results.add(routeResult(r));
            if (results.size() >= MAX_RESULTS) {
                return results;
            }
        }

        // 别名：名称模糊，映射到目标对象
        List<Alias> aliases = aliasMapper.selectList(Wrappers.lambdaQuery(Alias.class)
                .like(Alias::getName, kw));
        for (Alias a : aliases) {
            results.add(aliasResult(a));
            if (results.size() >= MAX_RESULTS) {
                return results;
            }
        }
        return results;
    }

    private SearchResultVO nodeResult(Node n) {
        SearchResultVO vo = new SearchResultVO();
        vo.setTargetType("NODE");
        vo.setTargetId(String.valueOf(n.getId()));
        vo.setName(n.getName());
        vo.setCode(n.getCode());
        return vo;
    }

    private SearchResultVO processResult(Process p) {
        SearchResultVO vo = new SearchResultVO();
        vo.setTargetType("PROCESS");
        vo.setTargetId(String.valueOf(p.getId()));
        vo.setName(p.getName());
        vo.setCode(p.getCode());
        return vo;
    }

    private SearchResultVO routeResult(Route r) {
        SearchResultVO vo = new SearchResultVO();
        vo.setTargetType("ROUTE");
        vo.setTargetId(String.valueOf(r.getId()));
        vo.setName(r.getName());
        vo.setCode(null);
        return vo;
    }

    private SearchResultVO aliasResult(Alias a) {
        SearchResultVO vo = new SearchResultVO();
        vo.setTargetType(a.getTargetType());
        vo.setTargetId(String.valueOf(a.getTargetId()));
        vo.setName(a.getName());
        vo.setCode(null);
        return vo;
    }
}
