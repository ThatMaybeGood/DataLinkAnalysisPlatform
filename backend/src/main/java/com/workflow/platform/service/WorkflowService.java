package com.workflow.platform.service;

import com.workflow.platform.model.dto.WorkflowDTO;
import com.workflow.platform.model.dto.WorkflowQueryDTO;
import com.workflow.platform.model.vo.WorkflowVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 工作流服务接口
 * 定义工作流相关的业务操作，支持在线和离线两种模式实现
 */
public interface WorkflowService {

    // ========== 基础CRUD操作 ==========

    /**
     * 创建工作流
     * @param workflowDTO 工作流数据
     * @return 创建的工作流视图对象
     */
    WorkflowVO createWorkflow(WorkflowDTO workflowDTO);

    /**
     * 更新工作流
     * @param id 工作流ID
     * @param workflowDTO 更新的工作流数据
     * @return 更新后的工作流视图对象
     */
    WorkflowVO updateWorkflow(String id, WorkflowDTO workflowDTO);

    /**
     * 获取工作流详情
     * @param id 工作流ID
     * @return 工作流视图对象
     */
    WorkflowVO getWorkflow(String id);

    /**
     * 删除工作流
     * @param id 工作流ID
     */
    void deleteWorkflow(String id);

    // ========== 查询操作 ==========

    /**
     * 查询工作流列表（分页）
     * @param queryDTO 查询条件
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowVO> queryWorkflows(WorkflowQueryDTO queryDTO, Pageable pageable);

    /**
     * 根据别名获取工作流
     * @param alias 工作流别名
     * @return 工作流视图对象
     */
    WorkflowVO getWorkflowByAlias(String alias);

    /**
     * 搜索工作流
     * @param keyword 关键词（搜索名称、别名、描述）
     * @param pageable 分页参数
     * @return 分页的搜索结果
     */
    Page<WorkflowVO> searchWorkflows(String keyword, Pageable pageable);

    /**
     * 获取分类下的工作流
     * @param category 分类
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowVO> getWorkflowsByCategory(String category, Pageable pageable);

    /**
     * 获取状态下的工作流
     * @param status 状态
     * @param pageable 分页参数
     * @return 分页的工作流列表
     */
    Page<WorkflowVO> getWorkflowsByStatus(String status, Pageable pageable);

    // ========== 执行操作 ==========

    /**
     * 执行工作流
     * @param id 工作流ID
     * @param parameters 执行参数
     * @return 执行结果
     */
    Object executeWorkflow(String id, Object parameters);

    /**
     * 批量执行工作流
     * @param ids 工作流ID列表
     * @param parameters 执行参数
     * @return 批量执行结果
     */
    List<Object> batchExecuteWorkflows(List<String> ids, Object parameters);

    /**
     * 测试工作流（不保存执行记录）
     * @param id 工作流ID
     * @param parameters 测试参数
     * @return 测试结果
     */
    Object testWorkflow(String id, Object parameters);

    // ========== 导入导出操作 ==========

    /**
     * 导出工作流
     * @param id 工作流ID
     * @return 导出文件路径
     */
    String exportWorkflow(String id);

    /**
     * 导入工作流
     * @param filePath 文件路径
     * @return 导入的工作流视图对象
     */
    WorkflowVO importWorkflow(String filePath);

    /**
     * 克隆工作流
     * @param id 源工作流ID
     * @param newName 新工作流名称
     * @return 克隆的工作流视图对象
     */
    WorkflowVO cloneWorkflow(String id, String newName);

    // ========== 统计操作 ==========

    /**
     * 获取工作流统计信息
     * @return 统计信息
     */
    Object getStatistics();

    /**
     * 获取分类统计
     * @return 分类统计列表
     */
    List<Object> getCategoryStatistics();

    /**
     * 获取状态统计
     * @return 状态统计列表
     */
    List<Object> getStatusStatistics();

    // ========== 批量操作 ==========

    /**
     * 批量更新工作流状态
     * @param ids 工作流ID列表
     * @param status 新状态
     * @return 更新数量
     */
    int batchUpdateStatus(List<String> ids, String status);

    /**
     * 批量删除工作流
     * @param ids 工作流ID列表
     * @return 删除数量
     */
    int batchDeleteWorkflows(List<String> ids);

    // ========== 工具方法 ==========

    /**
     * 获取当前模式
     * @return 模式字符串（online/offline）
     */
    String getMode();

    /**
     * 验证工作流配置
     * @param workflowDTO 工作流数据
     * @return 验证结果，null表示验证通过
     */
    String validateWorkflow(WorkflowDTO workflowDTO);

    /**
     * 获取工作流节点
     * @param workflowId 工作流ID
     * @return 节点列表
     */
    List<Object> getWorkflowNodes(String workflowId);

    /**
     * 获取工作流执行记录
     * @param workflowId 工作流ID
     * @param pageable 分页参数
     * @return 分页的执行记录
     */
    Page<Object> getWorkflowExecutions(String workflowId, Pageable pageable);
}