package com.datalink.platform.model.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.PatternLibraryDTO;
import com.datalink.platform.model.entity.PatternLibrary;
import com.datalink.platform.model.service.PatternLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 模式库独立接口（图来源 G5）
 */
@RestController
@RequestMapping("/api/patterns")
@RequiredArgsConstructor
public class PatternLibraryController {

    private final PatternLibraryService patternLibraryService;

    /** 模式库列表 */
    @GetMapping
    public Result<List<PatternLibrary>> list(@RequestParam(required = false) String patternType,
                                                 @RequestParam(required = false) String keyword) {
        return Result.ok(patternLibraryService.list(patternType, keyword));
    }

    /** 新增模式 */
    @PostMapping
    public Result<PatternLibrary> create(@RequestBody PatternLibraryDTO dto) {
        return Result.ok(patternLibraryService.save(dto));
    }
}
