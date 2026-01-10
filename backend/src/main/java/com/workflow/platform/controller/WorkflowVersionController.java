package com.workflow.platform.controller;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:53
 */


import com.workflow.platform.model.dto.WorkflowVersionDTO;
import com.workflow.platform.model.vo.WorkflowVersionVO;
import com.workflow.platform.service.WorkflowVersionService;
import com.workflow.platform.service.impl.WorkflowVersionServiceImpl;
import com.workflow.platform.service.impl.WorkflowVersionServiceImpl.VersionComparisonResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 工作流版本控制器
 */
@RestController
@RequestMapping("/api/workflow/versions")
@Api(tags = "工作流版本管理")
@Slf4j
public class WorkflowVersionController {

    @Autowired
    private WorkflowVersionService workflowVersionService;

    @PostMapping
    @ApiOperation("创建新版本")
    public ResponseEntity<WorkflowVersionVO> createVersion(
            @Valid @RequestBody WorkflowVersionDTO versionDTO) {
        log.info("创建新版本，工作流ID: {}", versionDTO.getWorkflowId());

        WorkflowVersionVO versionVO = workflowVersionService.createVersion(versionDTO);
        return ResponseEntity.ok(versionVO);
    }

    @GetMapping("/workflow/{workflowId}")
    @ApiOperation("获取工作流的所有版本")
    public ResponseEntity<List<WorkflowVersionVO>> getVersions(
            @ApiParam(value = "工作流ID", required = true)
            @PathVariable Long workflowId) {
        log.debug("获取工作流版本列表，工作流ID: {}", workflowId);

        List<WorkflowVersionVO> versions = workflowVersionService.getVersions(workflowId);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{versionId}")
    @ApiOperation("获取版本详情")
    public ResponseEntity<WorkflowVersionVO> getVersionDetail(
            @ApiParam(value = "版本ID", required = true)
            @PathVariable Long versionId) {
        log.debug("获取版本详情，版本ID: {}", versionId);

        WorkflowVersionVO versionVO = workflowVersionService.getVersionDetail(versionId);
        return ResponseEntity.ok(versionVO);
    }

    @GetMapping("/workflow/{workflowId}/current")
    @ApiOperation("获取当前版本")
    public ResponseEntity<WorkflowVersionVO> getCurrentVersion(
            @ApiParam(value = "工作流ID", required = true)
            @PathVariable Long workflowId) {
        log.debug("获取当前版本，工作流ID: {}", workflowId);

        WorkflowVersionVO versionVO = workflowVersionService.getCurrentVersion(workflowId);
        return ResponseEntity.ok(versionVO);
    }

    @GetMapping("/workflow/{workflowId}/version/{versionNumber}")
    @ApiOperation("根据版本号获取版本")
    public ResponseEntity<WorkflowVersionVO> getVersionByNumber(
            @ApiParam(value = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @ApiParam(value = "版本号", required = true)
            @PathVariable Integer versionNumber) {
        log.debug("根据版本号获取版本，工作流ID: {}，版本号: {}", workflowId, versionNumber);

        WorkflowVersionVO versionVO = workflowVersionService.getVersionByNumber(workflowId, versionNumber);
        return ResponseEntity.ok(versionVO);
    }

    @PostMapping("/workflow/{workflowId}/rollback/{versionId}")
    @ApiOperation("回滚到指定版本")
    public ResponseEntity<Void> rollbackToVersion(
            @ApiParam(value = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @ApiParam(value = "版本ID", required = true)
            @PathVariable Long versionId) {
        log.info("回滚到指定版本，工作流ID: {}，版本ID: {}", workflowId, versionId);

        workflowVersionService.rollbackToVersion(workflowId, versionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/compare")
    @ApiOperation("比较两个版本")
    public ResponseEntity<VersionComparisonResult> compareVersions(
            @ApiParam(value = "版本1ID", required = true)
            @RequestParam Long versionId1,
            @ApiParam(value = "版本2ID", required = true)
            @RequestParam Long versionId2) {
        log.debug("比较版本，版本1: {}，版本2: {}", versionId1, versionId2);

        VersionComparisonResult result = workflowVersionService.compareVersions(versionId1, versionId2);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{versionId}")
    @ApiOperation("删除版本")
    public ResponseEntity<Void> deleteVersion(
            @ApiParam(value = "版本ID", required = true)
            @PathVariable Long versionId) {
        log.info("删除版本，版本ID: {}", versionId);

        workflowVersionService.deleteVersion(versionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{versionId}/tag")
    @ApiOperation("标记版本")
    public ResponseEntity<Void> tagVersion(
            @ApiParam(value = "版本ID", required = true)
            @PathVariable Long versionId,
            @ApiParam(value = "标签", required = true)
            @RequestParam String tag) {
        log.info("标记版本，版本ID: {}，标签: {}", versionId, tag);

        workflowVersionService.tagVersion(versionId, tag);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/workflow/{workflowId}/search")
    @ApiOperation("搜索版本")
    public ResponseEntity<List<WorkflowVersionVO>> searchVersions(
            @ApiParam(value = "工作流ID", required = true)
            @PathVariable Long workflowId,
            @ApiParam(value = "标签")
            @RequestParam(required = false) String tag,
            @ApiParam(value = "创建人")
            @RequestParam(required = false) String createdBy) {
        log.debug("搜索版本，工作流ID: {}，标签: {}，创建人: {}",
                workflowId, tag, createdBy);

        WorkflowVersionServiceImpl.VersionSearchCriteria criteria =
                new WorkflowVersionServiceImpl.VersionSearchCriteria();
        criteria.setTag(tag);
        criteria.setCreatedBy(createdBy);

        List<WorkflowVersionVO> versions = workflowVersionService.searchVersions(workflowId, criteria);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{versionId}/export")
    @ApiOperation("导出版本数据")
    public ResponseEntity<String> exportVersion(
            @ApiParam(value = "版本ID", required = true)
            @PathVariable Long versionId) {
        log.debug("导出版本数据，版本ID: {}", versionId);

        String exportData = workflowVersionService.exportVersion(versionId);
        return ResponseEntity.ok(exportData);
    }

    @PostMapping("/import")
    @ApiOperation("导入版本数据")
    public ResponseEntity<WorkflowVersionVO> importVersion(
            @ApiParam(value = "版本数据", required = true)
            @RequestBody String versionData) {
        log.info("导入版本数据");

        WorkflowVersionVO versionVO = workflowVersionService.importVersion(versionData);
        return ResponseEntity.ok(versionVO);
    }
}