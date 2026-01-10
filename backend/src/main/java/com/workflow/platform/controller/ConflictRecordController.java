package com.workflow.platform.controller;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/11 00:40
 */

import com.workflow.platform.model.dto.ConflictRecordDTO;
import com.workflow.platform.model.dto.ConflictResolutionDTO;
import com.workflow.platform.model.vo.ConflictRecordVO;
import com.workflow.platform.model.vo.ConflictStatisticsVO;
import com.workflow.platform.service.ConflictRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 冲突记录控制器
 */
@RestController
@RequestMapping("/api/conflict")
@Api(tags = "冲突记录管理")
@Slf4j
public class ConflictRecordController {

    @Autowired
    private ConflictRecordService conflictRecordService;

    @PostMapping("/records")
    @ApiOperation("创建冲突记录")
    public ResponseEntity<ConflictRecordVO> createConflictRecord(
            @Valid @RequestBody ConflictRecordDTO conflictRecordDTO) {
        log.info("创建冲突记录，对象类型: {}，对象ID: {}",
                conflictRecordDTO.getObjectType(), conflictRecordDTO.getObjectId());

        ConflictRecordVO conflictRecord = conflictRecordService.createConflictRecord(conflictRecordDTO);
        return ResponseEntity.ok(conflictRecord);
    }

    @PostMapping("/records/batch")
    @ApiOperation("批量创建冲突记录")
    public ResponseEntity<List<ConflictRecordVO>> batchCreateConflictRecords(
            @Valid @RequestBody List<ConflictRecordDTO> conflictRecordDTOs) {
        log.info("批量创建冲突记录，数量: {}", conflictRecordDTOs.size());

        List<ConflictRecordVO> conflictRecords = conflictRecordService.batchCreateConflictRecords(conflictRecordDTOs);
        return ResponseEntity.ok(conflictRecords);
    }

    @GetMapping("/records/{id}")
    @ApiOperation("获取冲突记录详情")
    public ResponseEntity<ConflictRecordVO> getConflictRecord(
            @ApiParam(value = "冲突记录ID", required = true)
            @PathVariable Long id) {
        log.debug("获取冲突记录详情，ID: {}", id);

        ConflictRecordVO conflictRecord = conflictRecordService.getConflictRecord(id);
        return ResponseEntity.ok(conflictRecord);
    }

    @GetMapping("/records")
    @ApiOperation("获取冲突记录列表")
    public ResponseEntity<Page<ConflictRecordVO>> getConflictRecords(
            @ApiParam(value = "对象类型")
            @RequestParam(required = false) String objectType,
            @ApiParam(value = "对象ID")
            @RequestParam(required = false) String objectId,
            @ApiParam(value = "冲突类型")
            @RequestParam(required = false) String conflictType,
            @ApiParam(value = "状态")
            @RequestParam(required = false) String status,
            @ApiParam(value = "严重程度")
            @RequestParam(required = false) String severity,
            @ApiParam(value = "页码", defaultValue = "1")
            @RequestParam(defaultValue = "1") Integer page,
            @ApiParam(value = "每页大小", defaultValue = "10")
            @RequestParam(defaultValue = "10") Integer size) {

        log.debug("获取冲突记录列表，参数: objectType={}, objectId={}, status={}, page={}, size={}",
                objectType, objectId, status, page, size);

        Map<String, Object> queryParams = new HashMap<>();
        if (objectType != null) queryParams.put("objectType", objectType);
        if (objectId != null) queryParams.put("objectId", objectId);
        if (conflictType != null) queryParams.put("conflictType", conflictType);
        if (status != null) queryParams.put("status", status);
        if (severity != null) queryParams.put("severity", severity);

        Page<ConflictRecordVO> conflictRecords = conflictRecordService.getConflictRecords(queryParams, page, size);
        return ResponseEntity.ok(conflictRecords);
    }

    @GetMapping("/records/pending")
    @ApiOperation("获取待解决的冲突列表")
    public ResponseEntity<List<ConflictRecordVO>> getPendingConflicts() {
        log.debug("获取待解决的冲突列表");

        List<ConflictRecordVO> conflictRecords = conflictRecordService.getPendingConflicts();
        return ResponseEntity.ok(conflictRecords);
    }

    @GetMapping("/records/high-priority")
    @ApiOperation("获取高优先级冲突列表")
    public ResponseEntity<List<ConflictRecordVO>> getHighPriorityConflicts() {
        log.debug("获取高优先级冲突列表");

        List<ConflictRecordVO> conflictRecords = conflictRecordService.getHighPriorityConflicts();
        return ResponseEntity.ok(conflictRecords);
    }

    @PostMapping("/resolve")
    @ApiOperation("解决冲突")
    public ResponseEntity<ConflictRecordVO> resolveConflict(
            @Valid @RequestBody ConflictResolutionDTO resolutionDTO) {
        log.info("解决冲突，冲突ID: {}，策略: {}",
                resolutionDTO.getConflictId(), resolutionDTO.getResolutionStrategy());

        ConflictRecordVO conflictRecord = conflictRecordService.resolveConflict(resolutionDTO);
        return ResponseEntity.ok(conflictRecord);
    }

    @PostMapping("/resolve/batch")
    @ApiOperation("批量解决冲突")
    public ResponseEntity<List<ConflictRecordVO>> batchResolveConflicts(
            @Valid @RequestBody List<ConflictResolutionDTO> resolutionDTOs) {
        log.info("批量解决冲突，数量: {}", resolutionDTOs.size());

        List<ConflictRecordVO> conflictRecords = conflictRecordService.batchResolveConflicts(resolutionDTOs);
        return ResponseEntity.ok(conflictRecords);
    }

    @PostMapping("/{id}/ignore")
    @ApiOperation("忽略冲突")
    public ResponseEntity<ConflictRecordVO> ignoreConflict(
            @ApiParam(value = "冲突记录ID", required = true)
            @PathVariable Long id,
            @ApiParam(value = "忽略备注")
            @RequestParam(required = false) String notes,
            @ApiParam(value = "忽略人", defaultValue = "system")
            @RequestParam(defaultValue = "system") String ignoredBy) {
        log.info("忽略冲突，冲突ID: {}", id);

        ConflictRecordVO conflictRecord = conflictRecordService.ignoreConflict(id, notes, ignoredBy);
        return ResponseEntity.ok(conflictRecord);
    }

    @PostMapping("/{id}/reopen")
    @ApiOperation("重新打开冲突")
    public ResponseEntity<ConflictRecordVO> reopenConflict(
            @ApiParam(value = "冲突记录ID", required = true)
            @PathVariable Long id,
            @ApiParam(value = "重新打开备注")
            @RequestParam(required = false) String notes) {
        log.info("重新打开冲突，冲突ID: {}", id);

        ConflictRecordVO conflictRecord = conflictRecordService.reopenConflict(id, notes);
        return ResponseEntity.ok(conflictRecord);
    }

    @PostMapping("/{id}/auto-resolve")
    @ApiOperation("自动解决冲突")
    public ResponseEntity<ConflictRecordVO> autoResolveConflict(
            @ApiParam(value = "冲突记录ID", required = true)
            @PathVariable Long id) {
        log.info("自动解决冲突，冲突ID: {}", id);

        ConflictRecordVO conflictRecord = conflictRecordService.autoResolveConflict(id);
        return ResponseEntity.ok(conflictRecord);
    }

    @PostMapping("/auto-resolve/batch")
    @ApiOperation("批量自动解决冲突")
    public ResponseEntity<List<ConflictRecordVO>> batchAutoResolveConflicts(
            @ApiParam(value = "冲突记录ID列表", required = true)
            @RequestBody List<Long> ids) {
        log.info("批量自动解决冲突，数量: {}", ids.size());

        List<ConflictRecordVO> conflictRecords = conflictRecordService.batchAutoResolveConflicts(ids);
        return ResponseEntity.ok(conflictRecords);
    }

    @PostMapping("/{id}/retry")
    @ApiOperation("重新检测冲突")
    public ResponseEntity<ConflictRecordVO> retryConflictDetection(
            @ApiParam(value = "冲突记录ID", required = true)
            @PathVariable Long id) {
        log.info("重新检测冲突，冲突ID: {}", id);

        ConflictRecordVO conflictRecord = conflictRecordService.retryConflictDetection(id);
        return ResponseEntity.ok(conflictRecord);
    }

    @GetMapping("/statistics")
    @ApiOperation("获取冲突统计信息")
    public ResponseEntity<ConflictStatisticsVO> getConflictStatistics(
            @ApiParam(value = "开始时间")
            @RequestParam(required = false) LocalDateTime startTime,
            @ApiParam(value = "结束时间")
            @RequestParam(required = false) LocalDateTime endTime) {

        log.debug("获取冲突统计信息，开始时间: {}，结束时间: {}", startTime, endTime);

        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        ConflictStatisticsVO statistics = conflictRecordService.getConflictStatistics(startTime, endTime);
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/statistics/realtime")
    @ApiOperation("获取实时冲突统计")
    public ResponseEntity<Map<String, Object>> getRealtimeConflictStats() {
        log.debug("获取实时冲突统计");

        Map<String, Object> stats = conflictRecordService.getRealtimeConflictStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/statistics/trend")
    @ApiOperation("分析冲突趋势")
    public ResponseEntity<Map<String, Object>> analyzeConflictTrend(
            @ApiParam(value = "分析天数", defaultValue = "30")
            @RequestParam(defaultValue = "30") Integer days) {
        log.debug("分析冲突趋势，天数: {}", days);

        Map<String, Object> trend = conflictRecordService.analyzeConflictTrend(days);
        return ResponseEntity.ok(trend);
    }

    @GetMapping("/report")
    @ApiOperation("生成冲突报告")
    public ResponseEntity<String> generateConflictReport(
            @ApiParam(value = "开始时间")
            @RequestParam(required = false) LocalDateTime startTime,
            @ApiParam(value = "结束时间")
            @RequestParam(required = false) LocalDateTime endTime,
            @ApiParam(value = "报告格式", defaultValue = "text", allowableValues = "text,json")
            @RequestParam(defaultValue = "text") String format) {

        log.info("生成冲突报告，开始时间: {}，结束时间: {}，格式: {}", startTime, endTime, format);

        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        String report = conflictRecordService.generateConflictReport(startTime, endTime, format).join();
        return ResponseEntity.ok(report);
    }

    @DeleteMapping("/cleanup")
    @ApiOperation("清理旧的冲突记录")
    public ResponseEntity<Integer> cleanupOldConflictRecords(
            @ApiParam(value = "保留天数", defaultValue = "30")
            @RequestParam(defaultValue = "30") Integer daysToKeep) {
        log.info("清理旧的冲突记录，保留天数: {}", daysToKeep);

        int deletedCount = conflictRecordService.cleanupOldConflictRecords(daysToKeep);
        return ResponseEntity.ok(deletedCount);
    }

    @GetMapping("/{id}/suggestions")
    @ApiOperation("获取冲突解决建议")
    public ResponseEntity<Map<String, Object>> getConflictResolutionSuggestions(
            @ApiParam(value = "冲突记录ID", required = true)
            @PathVariable Long id) {
        log.debug("获取冲突解决建议，冲突ID: {}", id);

        Map<String, Object> suggestions = conflictRecordService.getConflictResolutionSuggestions(id);
        return ResponseEntity.ok(suggestions);
    }
}