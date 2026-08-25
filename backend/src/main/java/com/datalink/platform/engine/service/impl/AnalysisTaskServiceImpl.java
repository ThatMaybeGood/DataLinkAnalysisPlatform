package com.datalink.platform.engine.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.platform.common.PageResult;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.engine.dto.AnalysisTaskDetailVO;
import com.datalink.platform.engine.dto.AnalysisTaskVO;
import com.datalink.platform.engine.entity.AnalysisTask;
import com.datalink.platform.engine.mapper.AnalysisTaskMapper;
import com.datalink.platform.engine.service.AnalysisTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分析任务服务实现：start/finish/fail 均 @Transactional；operator 取当前登录用户（兜底 system）。
 */
@Service
@RequiredArgsConstructor
public class AnalysisTaskServiceImpl implements AnalysisTaskService {

    private final AnalysisTaskMapper analysisTaskMapper;
    private final ConnectorMapper connectorMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public AnalysisTaskVO start(Long connectorId, String connectorIds, String connectorName, String taskType) {
        Connector c = connectorMapper.selectById(connectorId);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "连接不存在: " + connectorId);
        }
        AnalysisTask t = new AnalysisTask();
        t.setConnectorId(connectorId);
        t.setConnectorIds(StringUtils.hasText(connectorIds) ? connectorIds : String.valueOf(connectorId));
        t.setConnectorName(StringUtils.hasText(connectorName) ? connectorName : c.getName());
        t.setTaskType(taskType);
        t.setStatus("RUNNING");
        t.setOperator(currentOperator());
        analysisTaskMapper.insert(t);
        return toVO(t);
    }

    @Override
    @Transactional
    public void finish(Long id, String snapshotJson) {
        AnalysisTask t = new AnalysisTask();
        t.setId(id);
        t.setStatus("SUCCESS");
        t.setDraftSnapshot(snapshotJson);
        t.setFinishedAt(LocalDateTime.now());
        analysisTaskMapper.updateById(t);
    }

    @Override
    @Transactional
    public void fail(Long id, String errorMessage) {
        AnalysisTask t = new AnalysisTask();
        t.setId(id);
        t.setStatus("FAILED");
        t.setErrorMessage(errorMessage);
        t.setFinishedAt(LocalDateTime.now());
        analysisTaskMapper.updateById(t);
    }

    @Override
    public PageResult<AnalysisTaskVO> page(int page, int size, Long connectorId) {
        IPage<AnalysisTask> p = analysisTaskMapper.selectPageLight(new Page<>(page, size), connectorId);
        List<AnalysisTaskVO> vos = p.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public AnalysisTaskDetailVO detail(Long id) {
        AnalysisTask t = analysisTaskMapper.selectById(id);
        if (t == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "分析任务不存在: " + id);
        }
        AnalysisTaskDetailVO vo = new AnalysisTaskDetailVO();
        copy(t, vo);
        if (StringUtils.hasText(t.getDraftSnapshot())) {
            try {
                vo.setDraftSnapshot(objectMapper.readTree(t.getDraftSnapshot()));
            } catch (Exception e) {
                vo.setDraftSnapshot(null);
            }
        }
        return vo;
    }

    private AnalysisTaskVO toVO(AnalysisTask t) {
        AnalysisTaskVO vo = new AnalysisTaskVO();
        copy(t, vo);
        return vo;
    }

    private void copy(AnalysisTask t, AnalysisTaskVO vo) {
        vo.setId(t.getId());
        vo.setConnectorId(t.getConnectorId());
        vo.setConnectorIds(t.getConnectorIds());
        vo.setConnectorName(t.getConnectorName());
        vo.setTaskType(t.getTaskType());
        vo.setStatus(t.getStatus());
        vo.setErrorMessage(t.getErrorMessage());
        vo.setOperator(t.getOperator());
        vo.setCreatedAt(t.getCreatedAt());
        vo.setFinishedAt(t.getFinishedAt());
    }

    private String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return "system";
    }
}
