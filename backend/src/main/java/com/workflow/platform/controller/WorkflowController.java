package com.workflow.platform.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.dto.WorkflowQueryDTO;
import com.workflow.platform.model.vo.WorkflowVO;
import com.workflow.platform.service.WorkflowService;
import com.workflow.platform.service.WorkflowServiceFactory;

// import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 工作流管理控制器
 * 提供工作流的CRUD、执行、导入导出等操作的REST API
 * 支持在线和离线两种模式，通过WorkflowServiceFactory自动切换
 */
@RestController
@RequestMapping("/api/workflows")
// @Api(tags = "工作流管理", description = "工作流的创建、查询、执行、导入导出等操作")
@RequiredArgsConstructor
@Slf4j
@Validated
public class WorkflowController {

	private final WorkflowServiceFactory serviceFactory;

	// ========== 基础CRUD操作 ==========

	@PostMapping
	@ApiOperation(value = "创建工作流", notes = "创建一个新的工作流")
	public ResponseEntity<WorkflowVO> createWorkflow(
			@Valid @RequestBody WorkflowDTO workflowDTO,
			HttpServletRequest request) {

		log.info("接收到创建工作流请求: {}", workflowDTO.getName());

		// 设置模式信息
		workflowDTO.setMode(getRequestMode(request));

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		WorkflowVO result = service.createWorkflow(workflowDTO);

		log.info("工作流创建成功: {} (ID: {})", result.getName(), result.getId());

		return ResponseEntity.status(HttpStatus.CREATED).body(result);
	}

	@PutMapping("/{id}")
	@ApiOperation(value = "更新工作流", notes = "更新指定ID的工作流")
	public ResponseEntity<WorkflowVO> updateWorkflow(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			@Valid @RequestBody WorkflowDTO workflowDTO,
			HttpServletRequest request) {

		log.info("接收到更新工作流请求: ID={}", id);

		// 设置模式信息
		workflowDTO.setMode(getRequestMode(request));

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		WorkflowVO result = service.updateWorkflow(id, workflowDTO);

		log.info("工作流更新成功: {} (ID: {})", result.getName(), result.getId());

		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}")
	@ApiOperation(value = "获取工作流详情", notes = "根据ID获取工作流的详细信息")
	public ResponseEntity<WorkflowVO> getWorkflow(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			HttpServletRequest request) {

		log.info("接收到获取工作流详情请求: ID={}", id);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		WorkflowVO result = service.getWorkflow(id);

		log.info("工作流详情获取成功: {} (ID: {})", result.getName(), result.getId());

		return ResponseEntity.ok(result);
	}

	@DeleteMapping("/{id}")
	@ApiOperation(value = "删除工作流", notes = "删除指定ID的工作流")
	public ResponseEntity<Void> deleteWorkflow(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			HttpServletRequest request) {

		log.info("接收到删除工作流请求: ID={}", id);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		service.deleteWorkflow(id);

		log.info("工作流删除成功: ID={}", id);

		return ResponseEntity.noContent().build();
	}

	// ========== 查询操作 ==========

	@GetMapping
	@ApiOperation(value = "查询工作流列表", notes = "根据条件查询工作流列表，支持分页")
	public ResponseEntity<Page<WorkflowVO>> queryWorkflows(
			@Valid WorkflowQueryDTO queryDTO,
			@PageableDefault(size = 20, sort = "updatedAt,desc") Pageable pageable,
			HttpServletRequest request) {

		log.info("接收到查询工作流列表请求: {}", queryDTO);

		// 设置模式信息
		queryDTO.setMode(getRequestMode(request));

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		Page<WorkflowVO> result = service.queryWorkflows(queryDTO, pageable);

		log.info("工作流列表查询成功，共 {} 条记录", result.getTotalElements());

		return ResponseEntity.ok(result);
	}

	@GetMapping("/search")
	@ApiOperation(value = "搜索工作流", notes = "根据关键词搜索工作流")
	public ResponseEntity<Page<WorkflowVO>> searchWorkflows(
			@ApiParam(value = "搜索关键词", required = true) @RequestParam String keyword,
			@PageableDefault(size = 20) Pageable pageable,
			HttpServletRequest request) {

		log.info("接收到搜索工作流请求，关键词: {}", keyword);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		Page<WorkflowVO> result = service.searchWorkflows(keyword, pageable);

		log.info("工作流搜索成功，共 {} 条结果", result.getTotalElements());

		return ResponseEntity.ok(result);
	}

	@GetMapping("/by-alias/{alias}")
	@ApiOperation(value = "根据别名获取工作流", notes = "根据别名获取工作流的详细信息")
	public ResponseEntity<WorkflowVO> getWorkflowByAlias(
			@ApiParam(value = "工作流别名", required = true) @PathVariable String alias,
			HttpServletRequest request) {

		log.info("接收到根据别名获取工作流请求: alias={}", alias);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		WorkflowVO result = service.getWorkflowByAlias(alias);

		log.info("根据别名获取工作流成功: {} (alias: {})", result.getName(), alias);

		return ResponseEntity.ok(result);
	}

	// ========== 执行操作 ==========

	@PostMapping("/{id}/execute")
	@ApiOperation(value = "执行工作流", notes = "执行指定的工作流")
	public ResponseEntity<Object> executeWorkflow(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			@RequestBody(required = false) Object parameters,
			HttpServletRequest request) {

		log.info("接收到执行工作流请求: ID={}", id);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		Object result = service.executeWorkflow(id, parameters);

		log.info("工作流执行成功: ID={}", id);

		return ResponseEntity.ok(result);
	}

	@PostMapping("/batch-execute")
	@ApiOperation(value = "批量执行工作流", notes = "批量执行多个工作流")
	public ResponseEntity<List<Object>> batchExecuteWorkflows(
			@ApiParam(value = "工作流ID列表", required = true) @RequestBody List<String> ids,
			@RequestBody(required = false) Object parameters,
			HttpServletRequest request) {

		log.info("接收到批量执行工作流请求，共 {} 个工作流", ids.size());

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		List<Object> results = service.batchExecuteWorkflows(ids, parameters);

		log.info("批量工作流执行成功，共 {} 个工作流", results.size());

		return ResponseEntity.ok(results);
	}

	// ========== 导入导出操作 ==========

	@GetMapping("/{id}/export")
	@ApiOperation(value = "导出工作流", notes = "将工作流导出为JSON文件")
	public ResponseEntity<Resource> exportWorkflow(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			HttpServletRequest request,
			HttpServletResponse response) throws IOException {

		log.info("接收到导出工作流请求: ID={}", id);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		String filePath = service.exportWorkflow(id);

		File exportFile = new File(filePath);

		// 设置响应头
		String fileName = exportFile.getName();
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + fileName + "\"");
		response.setContentLength((int) exportFile.length());

		// 将文件内容写入响应
		Files.copy(exportFile.toPath(), response.getOutputStream());
		response.flushBuffer();

		log.info("工作流导出成功: {}，文件: {}", id, fileName);

		return ResponseEntity.ok().build();
	}

	@PostMapping("/import")
	@ApiOperation(value = "导入工作流", notes = "从JSON文件导入工作流")
	public ResponseEntity<WorkflowVO> importWorkflow(
			@ApiParam(value = "工作流文件", required = true) @RequestParam("file") MultipartFile file,
			HttpServletRequest request) throws IOException {

		log.info("接收到导入工作流请求，文件名: {}", file.getOriginalFilename());

		// 保存临时文件
		String tempDir = System.getProperty("java.io.tmpdir");
		String tempFilePath = tempDir + File.separator +
				System.currentTimeMillis() + "_" + file.getOriginalFilename();
		Path tempPath = Paths.get(tempFilePath);

		Files.createDirectories(tempPath.getParent());
		file.transferTo(tempPath.toFile());

		try {
			// 获取对应模式的服务并执行
			WorkflowService service = serviceFactory.getWorkflowService();
			WorkflowVO result = service.importWorkflow(tempFilePath);

			log.info("工作流导入成功: {} (ID: {})", result.getName(), result.getId());

			return ResponseEntity.ok(result);

		} finally {
			// 清理临时文件
			Files.deleteIfExists(tempPath);
		}
	}

	@PostMapping("/{id}/clone")
	@ApiOperation(value = "克隆工作流", notes = "克隆一个工作流")
	public ResponseEntity<WorkflowVO> cloneWorkflow(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			@ApiParam(value = "新工作流名称") @RequestParam(required = false) String newName,
			HttpServletRequest request) {

		log.info("接收到克隆工作流请求: ID={}, 新名称: {}", id, newName);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		WorkflowVO result = service.cloneWorkflow(id, newName);

		log.info("工作流克隆成功: {} (源ID: {}, 新ID: {})",
				result.getName(), id, result.getId());

		return ResponseEntity.ok(result);
	}

	// ========== 统计操作 ==========

	@GetMapping("/statistics")
	@ApiOperation(value = "获取工作流统计信息", notes = "获取工作流的统计信息")
	public ResponseEntity<Object> getStatistics(HttpServletRequest request) {

		log.info("接收到获取工作流统计信息请求");

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		Object result = service.getStatistics();

		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}/nodes")
	@ApiOperation(value = "获取工作流节点", notes = "获取指定工作流的所有节点")
	public ResponseEntity<List<Object>> getWorkflowNodes(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			HttpServletRequest request) {

		log.info("接收到获取工作流节点请求: ID={}", id);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		List<Object> result = service.getWorkflowNodes(id);

		log.info("获取工作流节点成功，共 {} 个节点", result.size());

		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}/executions")
	@ApiOperation(value = "获取工作流执行记录", notes = "获取指定工作流的执行记录")
	public ResponseEntity<Page<Object>> getWorkflowExecutions(
			@ApiParam(value = "工作流ID", required = true) @PathVariable String id,
			@PageableDefault(size = 20, sort = "createdAt,desc") Pageable pageable,
			HttpServletRequest request) {

		log.info("接收到获取工作流执行记录请求: ID={}", id);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		Page<Object> result = service.getWorkflowExecutions(id, pageable);

		log.info("获取工作流执行记录成功，共 {} 条记录", result.getTotalElements());

		return ResponseEntity.ok(result);
	}

	// ========== 批量操作 ==========

	@PostMapping("/batch-update-status")
	@ApiOperation(value = "批量更新工作流状态", notes = "批量更新多个工作流的状态")
	public ResponseEntity<Integer> batchUpdateStatus(
			@ApiParam(value = "工作流ID列表", required = true) @RequestBody List<String> ids,
			@ApiParam(value = "新状态", required = true) @RequestParam String status,
			HttpServletRequest request) {

		log.info("接收到批量更新工作流状态请求，共 {} 个工作流，新状态: {}", ids.size(), status);

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		int result = service.batchUpdateStatus(ids, status);

		log.info("批量更新工作流状态成功，更新了 {} 个工作流", result);

		return ResponseEntity.ok(result);
	}

	@PostMapping("/batch-delete")
	@ApiOperation(value = "批量删除工作流", notes = "批量删除多个工作流")
	public ResponseEntity<Integer> batchDeleteWorkflows(
			@ApiParam(value = "工作流ID列表", required = true) @RequestBody List<String> ids,
			HttpServletRequest request) {

		log.info("接收到批量删除工作流请求，共 {} 个工作流", ids.size());

		// 获取对应模式的服务并执行
		WorkflowService service = serviceFactory.getWorkflowService();
		int result = service.batchDeleteWorkflows(ids);

		log.info("批量删除工作流成功，删除了 {} 个工作流", result);

		return ResponseEntity.ok(result);
	}

	// ========== 模式相关操作 ==========

	@GetMapping("/mode")
	@ApiOperation(value = "获取当前模式", notes = "获取当前工作流服务的运行模式")
	public ResponseEntity<String> getCurrentMode() {

		String mode = serviceFactory.getCurrentMode();
		log.info("获取当前模式: {}", mode);

		return ResponseEntity.ok(mode);
	}

	@GetMapping("/mode-info")
	@ApiOperation(value = "获取模式详细信息", notes = "获取当前模式的详细信息")
	public ResponseEntity<WorkflowServiceFactory.ModeInfo> getModeInfo() {

		WorkflowServiceFactory.ModeInfo info = serviceFactory.getModeInfo();
		log.info("获取模式详细信息: {}", info.getMode());

		return ResponseEntity.ok(info);
	}

	// ========== 异常处理 ==========

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
		log.warn("参数错误: {}", e.getMessage());
		return ResponseEntity.badRequest().body(e.getMessage());
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<String> handleRuntimeException(RuntimeException e) {
		log.error("业务处理错误", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("服务器内部错误: " + e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<String> handleException(Exception e) {
		log.error("系统错误", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("系统错误: " + e.getMessage());
	}

	// ========== 私有辅助方法 ==========

	/**
	 * 从请求中获取模式信息
	 */
	private String getRequestMode(HttpServletRequest request) {
		// 首先检查请求头中的模式信息
		String headerMode = request.getHeader("X-App-Mode");
		if (headerMode != null && ("online".equals(headerMode) || "offline".equals(headerMode))) {
			return headerMode;
		}

		// 默认使用服务工厂的当前模式
		return serviceFactory.getCurrentMode();
	}
}