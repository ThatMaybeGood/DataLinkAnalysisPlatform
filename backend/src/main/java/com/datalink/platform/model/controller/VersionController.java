package com.datalink.platform.model.controller;

import com.datalink.platform.common.PageResult;
import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.VersionVO;
import com.datalink.platform.model.service.ConfigVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置版本接口
 */
@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
public class VersionController {

    private final ConfigVersionService configVersionService;

    /** 版本分页查询（可按目标类型过滤） */
    @GetMapping
    public Result<PageResult<VersionVO>> page(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) String targetType) {
        return Result.ok(configVersionService.page(page, size, targetType));
    }
}
