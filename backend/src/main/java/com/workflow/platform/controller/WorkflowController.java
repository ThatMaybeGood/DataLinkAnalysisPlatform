package com.workflow.platform.controller;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.dto.WorkflowQueryDTO;
import com.workflow.platform.model.vo.WorkflowVO;
import com.workflow.platform.service.WorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@Api(tags = "工作流管理接口")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final WorkflowService workflowService;

    @PostMapping
    @ApiOperation("创建工作流")
    public ResponseEntity<WorkflowVO> createWorkflow(@RequestBody WorkflowDTO workflowDTO) {
        log.info("创建工作流: {}", workflowDTO.getName());
        WorkflowVO workflow = workflowService.createWorkflow(workflowDTO);
        return ResponseEntity.ok(workflow);
    }

    @PutMapping("/{id}")
    @ApiOperation("更新工作流")
    public ResponseEntity<WorkflowVO> updateWorkflow(
            @PathVariable String id,
            @RequestBody WorkflowDTO workflowDTO) {
        log.info("更新工作流: {}", id);
        WorkflowVO workflow = workflowService.updateWorkflow(id, workflowDTO);
        return ResponseEntity.ok(workflow);
    }

    @GetMapping("/{id}")
    @ApiOperation("获取工作流详情")
    public ResponseEntity<WorkflowVO> getWorkflow(@PathVariable String id) {
        log.info("获取工作流详情: {}", id);
        WorkflowVO workflow = workflowService.getWorkflow(id);
        return ResponseEntity.ok(workflow);
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除工作流")
    public ResponseEntity<Void> deleteWorkflow(@PathVariable String id) {
        log.info("删除工作流: {}", id);
        workflowService.deleteWorkflow(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @ApiOperation("查询工作流列表")
    public ResponseEntity<Page<WorkflowVO>> queryWorkflows(
            WorkflowQueryDTO queryDTO,
            Pageable pageable) {
        log.info("查询工作流列表, 参数: {}", queryDTO);
        Page<WorkflowVO> page = workflowService.queryWorkflows(queryDTO, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/execute")
    @ApiOperation("执行工作流")
    public ResponseEntity<WorkflowVO> executeWorkflow(
            @PathVariable String id,
            @RequestBody(required = false) Object parameters) {
        log.info("执行工作流: {}", id);
        WorkflowVO result = workflowService.executeWorkflow(id, parameters);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}/export")
    @ApiOperation("导出工作流")
    public void exportWorkflow(
            @PathVariable String id,
            HttpServletResponse response) throws IOException {
        log.info("导出工作流: {}", id);

        File exportFile = workflowService.exportWorkflow(id);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + exportFile.getName() + "\"");
        response.setContentLength((int) exportFile.length());

        Files.copy(exportFile.toPath(), response.getOutputStream());
        response.flushBuffer();
    }

    @PostMapping("/import")
    @ApiOperation("导入工作流")
    public ResponseEntity<WorkflowVO> importWorkflow(
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("导入工作流文件: {}", file.getOriginalFilename());

        // 保存临时文件
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFilePath = tempDir + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        File tempFile = new File(tempFilePath);
        file.transferTo(tempFile);

        try {
            WorkflowVO workflow = workflowService.importWorkflow(tempFilePath);
            return ResponseEntity.ok(workflow);
        } finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    @GetMapping("/{id}/clone")
    @ApiOperation("克隆工作流")
    public ResponseEntity<WorkflowVO> cloneWorkflow(
            @PathVariable String id,
            @RequestParam(required = false) String newName) {
        log.info("克隆工作流: {}, 新名称: {}", id, newName);
        WorkflowVO clonedWorkflow = workflowService.cloneWorkflow(id, newName);
        return ResponseEntity.ok(clonedWorkflow);
    }

    @GetMapping("/{id}/nodes")
    @ApiOperation("获取工作流节点")
    public ResponseEntity<List<?>> getWorkflowNodes(@PathVariable String id) {
        log.info("获取工作流节点: {}", id);
        List<?> nodes = workflowService.getWorkflowNodes(id);
        return ResponseEntity.ok(nodes);
    }

    @GetMapping("/{id}/executions")
    @ApiOperation("获取工作流执行记录")
    public ResponseEntity<Page<?>> getWorkflowExecutions(
            @PathVariable String id,
            Pageable pageable) {
        log.info("获取工作流执行记录: {}", id);
        Page<?> executions = workflowService.getWorkflowExecutions(id, pageable);
        return ResponseEntity.ok(executions);
    }

    @GetMapping("/search")
    @ApiOperation("搜索工作流")
    public ResponseEntity<Page<WorkflowVO>> searchWorkflows(
            @RequestParam String keyword,
            Pageable pageable) {
        log.info("搜索工作流, 关键词: {}", keyword);
        Page<WorkflowVO> results = workflowService.searchWorkflows(keyword, pageable);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/batch")
    @ApiOperation("批量操作工作流")
    public ResponseEntity<Void> batchOperation(
            @RequestParam String operation,
            @RequestParam List<String> ids,
            @RequestBody(required = false) Object data) {
        log.info("批量操作工作流, 操作: {}, 数量: {}", operation, ids.size());
        workflowService.batchOperation(operation, ids, data);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    @ApiOperation("获取工作流统计信息")
    public ResponseEntity<?> getStatistics() {
        log.info("获取工作流统计信息");
        Object statistics = workflowService.getStatistics();
        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/mode")
    @ApiOperation("获取当前模式")
    public ResponseEntity<String> getCurrentMode() {
        String mode = workflowService.getMode();
        return ResponseEntity.ok(mode);
    }

    @PostMapping("/switch-mode")
    @ApiOperation("切换模式（需要管理员权限）")
    public ResponseEntity<Void> switchMode(@RequestParam String mode) {
        log.info("切换模式为: {}", mode);
        // 这里需要实现模式切换逻辑
        // 可能需要重启应用或重新加载配置
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("工作流控制器异常: ", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("服务器内部错误: " + e.getMessage());
    }
}