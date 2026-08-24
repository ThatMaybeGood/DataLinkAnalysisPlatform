package com.datalink.platform.monitor.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.model.dto.CheckpointVO;
import com.datalink.platform.monitor.dto.SaveCheckpointRequest;
import com.datalink.platform.monitor.entity.Checkpoint;
import com.datalink.platform.monitor.entity.CheckResult;
import com.datalink.platform.monitor.mapper.CheckpointMapper;
import com.datalink.platform.monitor.mapper.CheckResultMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 检测点服务：按节点查询（附带最近检测状态）/ 创建 / 更新 / 删除。
 */
@Service
@RequiredArgsConstructor
public class CheckpointService {

    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CheckpointMapper checkpointMapper;
    private final CheckResultMapper checkResultMapper;

    /** 按节点查检测点，status 取最近一条检测结果、lastCheck 取检测时间（相对化） */
    public List<CheckpointVO> listByNode(Long nodeId) {
        List<Checkpoint> list = checkpointMapper.selectList(Wrappers.lambdaQuery(Checkpoint.class)
                .eq(Checkpoint::getNodeId, nodeId).orderByAsc(Checkpoint::getId));
        List<CheckpointVO> result = new ArrayList<>(list.size());
        for (Checkpoint cp : list) {
            result.add(toVO(cp));
        }
        return result;
    }

    /** 创建检测点：kind 默认 CUSTOM、freq 默认 5m、level 默认 L3、enabled 默认 1 */
    public CheckpointVO create(SaveCheckpointRequest req) {
        Checkpoint cp = new Checkpoint();
        cp.setNodeId(req.getNodeId());
        cp.setName(req.getName());
        cp.setCheckType(req.getCheckType());
        cp.setKind(req.getKind() == null || req.getKind().isBlank() ? "CUSTOM" : req.getKind());
        cp.setFreq(req.getFreq() == null || req.getFreq().isBlank() ? "5m" : req.getFreq());
        cp.setLevel(req.getLevel() == null || req.getLevel().isBlank() ? "L3" : req.getLevel());
        cp.setEnabled(1);
        checkpointMapper.insert(cp);
        return toVO(cp);
    }

    /** 更新检测点：非空字段覆盖 */
    public CheckpointVO update(Long id, SaveCheckpointRequest req) {
        Checkpoint cp = checkpointMapper.selectById(id);
        if (cp == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "检测点不存在");
        }
        if (req.getNodeId() != null) {
            cp.setNodeId(req.getNodeId());
        }
        if (req.getName() != null && !req.getName().isBlank()) {
            cp.setName(req.getName());
        }
        if (req.getCheckType() != null && !req.getCheckType().isBlank()) {
            cp.setCheckType(req.getCheckType());
        }
        if (req.getKind() != null && !req.getKind().isBlank()) {
            cp.setKind(req.getKind());
        }
        if (req.getFreq() != null && !req.getFreq().isBlank()) {
            cp.setFreq(req.getFreq());
        }
        if (req.getLevel() != null && !req.getLevel().isBlank()) {
            cp.setLevel(req.getLevel());
        }
        checkpointMapper.updateById(cp);
        return toVO(cp);
    }

    /** 删除检测点 */
    public void delete(Long id) {
        checkpointMapper.deleteById(id);
    }

    /** 立即执行一次检测并写入结果 */
    public CheckpointVO run(Long id) {
        Checkpoint cp = checkpointMapper.selectById(id);
        if (cp == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "检测点不存在");
        }
        String status = executeCheck(cp);
        CheckResult result = new CheckResult();
        result.setCheckpointId(id);
        result.setStatus(status);
        result.setCheckTime(LocalDateTime.now());
        result.setMessage(mockDetail(cp.getCheckType(), status));
        checkResultMapper.insert(result);
        return toVO(cp);
    }

    /** 模拟检测逻辑（按类型给出不同结果，便于演示） */
    private String executeCheck(Checkpoint cp) {
        return switch (cp.getCheckType()) {
            case "FRESHNESS" -> "WARNING";
            case "DELAY" -> "FAIL";
            case "THRESHOLD" -> Math.random() > 0.3 ? "PASS" : "WARNING";
            default -> "PASS";
        };
    }

    private String mockDetail(String checkType, String status) {
        if ("PASS".equals(status)) {
            return "检测通过";
        }
        return switch (checkType) {
            case "FRESHNESS" -> "最新数据滞后";
            case "DELAY" -> "同步延迟超过阈值";
            case "THRESHOLD" -> "指标超出阈值";
            default -> "检测异常";
        };
    }

    private CheckpointVO toVO(Checkpoint cp) {
        CheckpointVO vo = new CheckpointVO();
        vo.setId(String.valueOf(cp.getId()));
        vo.setName(cp.getName());
        vo.setKind(cp.getKind());
        vo.setCheckType(cp.getCheckType());
        CheckResult latest = latestResult(cp.getId());
        vo.setStatus(latest == null ? null : latest.getStatus());
        vo.setLastCheck(latest == null ? null : formatLastCheck(latest.getCheckTime()));
        return vo;
    }

    private CheckResult latestResult(Long checkpointId) {
        List<CheckResult> list = checkResultMapper.selectList(Wrappers.lambdaQuery(CheckResult.class)
                .eq(CheckResult::getCheckpointId, checkpointId)
                .orderByDesc(CheckResult::getCheckTime)
                .last("LIMIT 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    /** 检测时间相对化：「X 分钟前 / X 小时前 / 刚刚」，过久或未来则原样格式化 */
    private String formatLastCheck(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        long minutes = Duration.between(time, LocalDateTime.now()).toMinutes();
        if (minutes < 1) {
            return "刚刚";
        }
        if (minutes < 60) {
            return minutes + " 分钟前";
        }
        if (minutes < 1440) {
            return (minutes / 60) + " 小时前";
        }
        return time.format(DATETIME_FMT);
    }
}
