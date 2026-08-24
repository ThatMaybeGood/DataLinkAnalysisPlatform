package com.datalink.platform.model.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.model.dto.CorrectionRecordDTO;
import com.datalink.platform.model.dto.PatternLibraryDTO;
import com.datalink.platform.model.entity.CorrectionRecord;
import com.datalink.platform.model.entity.PatternLibrary;
import com.datalink.platform.model.service.CorrectionRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 校正记录接口（图来源 G5）
 */
@RestController
@RequestMapping("/api/corrections")
@RequiredArgsConstructor
public class CorrectionRecordController {

    private final CorrectionRecordService correctionRecordService;

    /** 提交校正 */
    @PostMapping
    public Result<CorrectionRecord> submit(@RequestBody CorrectionRecordDTO dto) {
        return Result.ok(correctionRecordService.submit(dto));
    }

    /** 查询某对象校正历史 */
    @GetMapping
    public Result<List<CorrectionRecord>> list(@RequestParam String targetType,
                                                   @RequestParam String targetId) {
        return Result.ok(correctionRecordService.listByTarget(targetType, targetId));
    }

    /** 确认校正生效 */
    @PostMapping("/{id}/confirm")
    public Result<CorrectionRecord> confirm(@PathVariable Long id) {
        return Result.ok(correctionRecordService.confirm(id));
    }

    /** 模式库列表 */
    @GetMapping("/patterns")
    public Result<List<PatternLibrary>> patterns(@RequestParam(required = false) String patternType,
                                                     @RequestParam(required = false) String keyword) {
        return Result.ok(correctionRecordService.listPatterns(patternType, keyword));
    }

    /** 手动新增模式 */
    @PostMapping("/patterns")
    public Result<PatternLibrary> createPattern(@RequestBody PatternLibraryDTO dto) {
        return Result.ok(correctionRecordService.savePattern(dto));
    }
}
