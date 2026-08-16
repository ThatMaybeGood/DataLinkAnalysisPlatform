package com.datalink.platform.model.service;

import com.datalink.platform.model.dto.SearchResultVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局搜索服务测试：单号 / 名称 / 别名通吃。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_model_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
class SearchServiceTest {

    @Autowired
    private SearchService searchService;

    @Test
    void search_pay_matches_process() {
        List<SearchResultVO> results = searchService.search("付款");
        assertTrue(results.stream().anyMatch(r -> "PROCESS".equals(r.getTargetType())
                && "付款流程".equals(r.getName())), "应命中流程 付款流程");
    }

    @Test
    void search_pay_start_matches_node() {
        List<SearchResultVO> results = searchService.search("付款发起");
        assertTrue(results.stream().anyMatch(r -> "NODE".equals(r.getTargetType())
                && "付款发起".equals(r.getName())), "应命中节点 付款发起");
    }

    @Test
    void search_alias_hit_maps_to_node() {
        List<SearchResultVO> results = searchService.search("收银台");
        assertTrue(results.stream().anyMatch(r -> "NODE".equals(r.getTargetType())
                && "收银台".equals(r.getName())), "应经别名命中节点 收银台");
    }

    @Test
    void search_blank_returns_empty() {
        assertTrue(searchService.search("  ").isEmpty());
        assertTrue(searchService.search(null).isEmpty());
    }
}
