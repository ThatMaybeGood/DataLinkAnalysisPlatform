package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.platform.common.PageResult;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.model.dto.VersionVO;
import com.datalink.platform.model.entity.ConfigVersion;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.entity.Process;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.mapper.ConfigVersionMapper;
import com.datalink.platform.model.mapper.NodeMapper;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.monitor.entity.Checkpoint;
import com.datalink.platform.monitor.mapper.CheckpointMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置版本服务：写版本快照（version 自增）/ 分页查询（附带目标名）。
 */
@Service
@RequiredArgsConstructor
public class ConfigVersionService {

    private final ConfigVersionMapper configVersionMapper;
    private final NodeMapper nodeMapper;
    private final ProcessMapper processMapper;
    private final RouteMapper routeMapper;
    private final CheckpointMapper checkpointMapper;
    private final ConnectorMapper connectorMapper;

    /** 记录一个版本：version = 该目标已有最大 version + 1，status 默认 PUBLISHED */
    public void record(String targetType, Long targetId, String contentJson, String changeNote, String operator) {
        List<ConfigVersion> latest = configVersionMapper.selectList(Wrappers.lambdaQuery(ConfigVersion.class)
                .eq(ConfigVersion::getTargetType, targetType)
                .eq(ConfigVersion::getTargetId, targetId)
                .orderByDesc(ConfigVersion::getVersion)
                .last("LIMIT 1"));
        int nextVersion = latest.isEmpty() ? 1 : latest.get(0).getVersion() + 1;
        ConfigVersion cv = new ConfigVersion();
        cv.setTargetType(targetType);
        cv.setTargetId(targetId);
        cv.setVersion(nextVersion);
        cv.setContent(contentJson);
        cv.setChangeNote(changeNote);
        cv.setOperator(operator);
        cv.setStatus("PUBLISHED");
        configVersionMapper.insert(cv);
    }

    /** 分页查询（创建时间倒序，可按目标类型过滤） */
    public PageResult<VersionVO> page(int page, int size, String targetType) {
        Page<ConfigVersion> p = new Page<>(page, size);
        IPage<ConfigVersion> result = configVersionMapper.selectPage(p, Wrappers.lambdaQuery(ConfigVersion.class)
                .eq(targetType != null && !targetType.isBlank(), ConfigVersion::getTargetType, targetType)
                .orderByDesc(ConfigVersion::getCreatedAt));
        List<VersionVO> records = new ArrayList<>(result.getRecords().size());
        for (ConfigVersion cv : result.getRecords()) {
            records.add(toVO(cv));
        }
        return PageResult.of(records, result.getTotal(), page, size);
    }

    /** NODE→节点名 / PROCESS→流程名 / ROUTE→路线名 / CHECKPOINT→检测点名 / CONNECTOR→连接器名，查不到返回 null */
    private String resolveTargetName(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return null;
        }
        switch (targetType) {
            case "PROCESS": {
                Process p = processMapper.selectById(targetId);
                return p == null ? null : p.getName();
            }
            case "NODE": {
                Node n = nodeMapper.selectById(targetId);
                return n == null ? null : n.getName();
            }
            case "ROUTE": {
                Route r = routeMapper.selectById(targetId);
                return r == null ? null : r.getName();
            }
            case "CHECKPOINT": {
                Checkpoint cp = checkpointMapper.selectById(targetId);
                return cp == null ? null : cp.getName();
            }
            case "CONNECTOR": {
                Connector c = connectorMapper.selectById(targetId);
                return c == null ? null : c.getName();
            }
            default:
                return null;
        }
    }

    private VersionVO toVO(ConfigVersion cv) {
        VersionVO vo = new VersionVO();
        vo.setId(String.valueOf(cv.getId()));
        vo.setTargetType(cv.getTargetType());
        vo.setVersion(cv.getVersion());
        vo.setTargetName(resolveTargetName(cv.getTargetType(), cv.getTargetId()));
        vo.setOperator(cv.getOperator());
        vo.setChangeNote(cv.getChangeNote());
        vo.setStatus(cv.getStatus());
        vo.setTime(cv.getCreatedAt());
        return vo;
    }
}
