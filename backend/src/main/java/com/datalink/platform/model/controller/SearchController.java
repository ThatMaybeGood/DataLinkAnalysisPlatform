package com.datalink.platform.model.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.SearchResultVO;
import com.datalink.platform.model.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 全局搜索接口
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /** 全局搜索：单号 / 名称 / 别名通吃 */
    @GetMapping
    public Result<List<SearchResultVO>> search(@RequestParam(required = false) String q) {
        return Result.ok(searchService.search(q));
    }
}
