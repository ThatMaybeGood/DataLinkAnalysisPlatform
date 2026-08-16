package com.datalink.platform.datasource.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datalink.platform.common.PageResult;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.ConnectorDbType;
import com.datalink.platform.datasource.dialect.DbDialect;
import com.datalink.platform.datasource.dialect.DbDialectFactory;
import com.datalink.platform.datasource.dto.ConnectorVO;
import com.datalink.platform.datasource.dto.PreviewResult;
import com.datalink.platform.datasource.dto.SaveConnectorRequest;
import com.datalink.platform.datasource.dto.TableInfo;
import com.datalink.platform.datasource.dto.TestResult;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.datasource.pool.ConnectionPoolRegistry;
import com.datalink.platform.datasource.service.ConnectorService;
import com.datalink.platform.datasource.util.AesUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 连接器服务实现。
 * 密码 AES 加密存储；VO 脱敏；测试/浏览走真实 JDBC（H2/MySQL/PG 方言可插拔）。
 */
@Service
@RequiredArgsConstructor
public class ConnectorServiceImpl implements ConnectorService {

    private static final int PREVIEW_LIMIT = 50;

    private final ConnectorMapper connectorMapper;
    private final AesUtil aesUtil;
    private final ConnectionPoolRegistry poolRegistry;

    @Override
    public PageResult<ConnectorVO> page(int page, int size, String keyword) {
        Page<Connector> p = new Page<>(page, size);
        connectorMapper.selectPageByKeyword(p, keyword);
        List<ConnectorVO> vos = p.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return PageResult.of(vos, p.getTotal(), p.getCurrent(), p.getSize());
    }

    @Override
    public ConnectorVO getById(Long id) {
        return toVO(require(id));
    }

    @Override
    public ConnectorVO create(SaveConnectorRequest req) {
        ConnectorDbType.from(req.getDbType()); // 校验类型
        if (req.getPassword() == null || req.getPassword().isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "密码不能为空");
        }
        Connector c = new Connector();
        c.setConnectorType("DB");
        c.setDbType(req.getDbType());
        c.setName(req.getName());
        c.setHost(req.getHost());
        c.setPort(req.getPort());
        c.setUsername(req.getUsername());
        c.setEncryptedPwd(aesUtil.encrypt(req.getPassword()));
        c.setDatabaseName(req.getDatabaseName());
        c.setSchemaName(req.getSchemaName());
        c.setConfig(req.getConfig());
        c.setEnabled(req.getEnabled() == null ? 1 : req.getEnabled());
        c.setIsActive(0);
        connectorMapper.insert(c);
        return toVO(c);
    }

    @Override
    public ConnectorVO update(Long id, SaveConnectorRequest req) {
        Connector c = require(id);
        ConnectorDbType.from(req.getDbType()); // 校验类型
        c.setDbType(req.getDbType());
        c.setName(req.getName());
        c.setHost(req.getHost());
        c.setPort(req.getPort());
        c.setUsername(req.getUsername());
        c.setDatabaseName(req.getDatabaseName());
        c.setSchemaName(req.getSchemaName());
        c.setConfig(req.getConfig());
        if (req.getEnabled() != null) {
            c.setEnabled(req.getEnabled());
        }
        // 密码留空 = 不改
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            c.setEncryptedPwd(aesUtil.encrypt(req.getPassword()));
        }
        connectorMapper.updateById(c);
        poolRegistry.evict(id); // 参数变化，重建连接池
        return toVO(c);
    }

    @Override
    public void delete(Long id) {
        poolRegistry.evict(id);
        connectorMapper.deleteById(id);
    }

    @Override
    public TestResult test(Long id) {
        Connector c = require(id);
        DbDialect d = DbDialectFactory.ofCode(c.getDbType());
        String url = d.buildJdbcUrl(c.getHost(), c.getPort() == null ? 0 : c.getPort(), c.getDatabaseName());
        long start = System.currentTimeMillis();
        try {
            DriverManager.setLoginTimeout(5);
            try (Connection conn = DriverManager.getConnection(url, c.getUsername(),
                    aesUtil.decrypt(c.getEncryptedPwd()))) {
                long latencyMs = System.currentTimeMillis() - start;
                try (Statement st = conn.createStatement()) {
                    st.execute(d.testSql());
                }
                String dbVersion = conn.getMetaData().getDatabaseProductName()
                        + "/" + conn.getMetaData().getDatabaseProductVersion();
                c.setLastTestStatus("OK");
                c.setLastTestTime(LocalDateTime.now());
                connectorMapper.updateById(c);
                return new TestResult(true, latencyMs, dbVersion, null);
            }
        } catch (Exception e) {
            // 失败只回传简短原因，绝不打印密码/URL
            c.setLastTestStatus("FAIL");
            c.setLastTestTime(LocalDateTime.now());
            connectorMapper.updateById(c);
            return new TestResult(false, null, null, shortMessage(e));
        }
    }

    @Override
    @Transactional
    public void activate(Long id) {
        // 目标存在且 enabled=1 才可设为当前，否则抛 400
        Connector c = connectorMapper.selectById(id);
        if (c == null || c.getEnabled() == null || c.getEnabled() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "连接不存在或未启用");
        }
        // 先清空旧激活，再设置目标
        connectorMapper.update(null, Wrappers.lambdaUpdate(Connector.class)
                .set(Connector::getIsActive, 0).eq(Connector::getIsActive, 1));
        connectorMapper.update(null, Wrappers.lambdaUpdate(Connector.class)
                .set(Connector::getIsActive, 1).eq(Connector::getId, id));
    }

    @Override
    public List<TableInfo> tables(Long id) {
        Connector c = require(id);
        try {
            DataSource ds = poolRegistry.get(c);
            try (Connection conn = ds.getConnection()) {
                conn.setReadOnly(true);
                DatabaseMetaData md = conn.getMetaData();
                String schema = (c.getSchemaName() == null || c.getSchemaName().isBlank())
                        ? null : c.getSchemaName();
                List<TableInfo> list = new ArrayList<>();
                try (ResultSet rs = md.getTables(null, schema, "%", new String[]{"TABLE", "VIEW"})) {
                    while (rs.next()) {
                        list.add(new TableInfo(rs.getString("TABLE_NAME"), rs.getString("TABLE_TYPE")));
                    }
                }
                return list;
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "连接失败: " + shortMessage(e));
        }
    }

    @Override
    public PreviewResult preview(Long id, String table) {
        Connector c = require(id);
        DbDialect d = DbDialectFactory.ofCode(c.getDbType());
        try {
            DataSource ds = poolRegistry.get(c);
            try (Connection conn = ds.getConnection()) {
                conn.setReadOnly(true);
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(d.previewSql(table, PREVIEW_LIMIT))) {
                    ResultSetMetaData rsm = rs.getMetaData();
                    int colCount = rsm.getColumnCount();
                    List<String> columns = new ArrayList<>(colCount);
                    for (int i = 1; i <= colCount; i++) {
                        columns.add(rsm.getColumnLabel(i));
                    }
                    List<List<Object>> rows = new ArrayList<>();
                    int rowCount = 0;
                    while (rowCount < PREVIEW_LIMIT && rs.next()) {
                        List<Object> row = new ArrayList<>(colCount);
                        for (int i = 1; i <= colCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        rows.add(row);
                        rowCount++;
                    }
                    return new PreviewResult(columns, rows, rowCount);
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "连接失败: " + shortMessage(e));
        }
    }

    /** 查询连接器，不存在抛 404 */
    private Connector require(Long id) {
        Connector c = connectorMapper.selectById(id);
        if (c == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "连接不存在");
        }
        return c;
    }

    /** 实体 → VO（脱敏，不含密码） */
    private ConnectorVO toVO(Connector c) {
        ConnectorVO vo = new ConnectorVO();
        vo.setId(c.getId());
        vo.setConnectorType(c.getConnectorType());
        vo.setDbType(c.getDbType());
        vo.setName(c.getName());
        vo.setHost(c.getHost());
        vo.setPort(c.getPort());
        vo.setUsername(c.getUsername());
        vo.setDatabaseName(c.getDatabaseName());
        vo.setSchemaName(c.getSchemaName());
        vo.setEnabled(c.getEnabled());
        vo.setIsActive(c.getIsActive());
        vo.setLastTestStatus(c.getLastTestStatus());
        vo.setLastTestTime(c.getLastTestTime());
        vo.setCreatedAt(c.getCreatedAt());
        return vo;
    }

    /** 异常简短信息（截断，避免过长） */
    private String shortMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
