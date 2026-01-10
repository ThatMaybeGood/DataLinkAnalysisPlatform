package com.workflow.platform.service;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:52
 */

import com.workflow.platform.model.dto.WorkflowVersionDTO;
import com.workflow.platform.model.vo.WorkflowVersionVO;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.service.impl.WorkflowVersionServiceImpl;

import java.util.List;

/**
 * 工作流版本服务接口
 */
public interface WorkflowVersionService {

    /**
     * 创建新版本
     */
    WorkflowVersionVO createVersion(WorkflowVersionDTO versionDTO);

    /**
     * 获取版本列表
     */
    List<WorkflowVersionVO> getVersions(Long workflowId);

    /**
     * 获取版本详情
     */
    WorkflowVersionVO getVersionDetail(Long versionId);

    /**
     * 根据版本号获取版本
     */
    WorkflowVersionVO getVersionByNumber(Long workflowId, Integer versionNumber);

    /**
     * 获取当前版本
     */
    WorkflowVersionVO getCurrentVersion(Long workflowId);

    /**
     * 回滚到指定版本
     */
    WorkflowEntity rollbackToVersion(Long workflowId, Long versionId);

    /**
     * 比较两个版本
     */
    WorkflowVersionServiceImpl.VersionComparisonResult compareVersions(Long versionId1, Long versionId2);

    /**
     * 删除版本
     */
    boolean deleteVersion(Long versionId);

    /**
     * 批量删除版本
     */
    boolean batchDeleteVersions(List<Long> versionIds);

    /**
     * 标记版本（如：stable, draft, archived）
     */
    boolean tagVersion(Long versionId, String tag);

    /**
     * 搜索版本
     */
    List<WorkflowVersionVO> searchVersions(Long workflowId, WorkflowVersionServiceImpl.VersionSearchCriteria criteria);

    /**
     * 导出版本数据
     */
    String exportVersion(Long versionId);

    /**
     * 导入版本数据
     */
    WorkflowVersionVO importVersion(String versionData);
}