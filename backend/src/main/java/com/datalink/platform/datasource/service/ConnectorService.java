package com.datalink.platform.datasource.service;

import com.datalink.platform.common.PageResult;
import com.datalink.platform.datasource.dto.ConnectorVO;
import com.datalink.platform.datasource.dto.PreviewResult;
import com.datalink.platform.datasource.dto.SaveConnectorRequest;
import com.datalink.platform.datasource.dto.TableInfo;
import com.datalink.platform.datasource.dto.TestResult;

import java.util.List;

/**
 * 数据池连接器服务：CRUD / 连通性测试 / 唯一激活 / 库表浏览 / 数据预览
 */
public interface ConnectorService {

    /** 分页查询（keyword 匹配 name/host/databaseName） */
    PageResult<ConnectorVO> page(int page, int size, String keyword);

    /** 按 id 查询（不存在抛 404） */
    ConnectorVO getById(Long id);

    /** 新建连接器（密码必填，AES 加密落库） */
    ConnectorVO create(SaveConnectorRequest req);

    /** 更新连接器（密码留空=不改，改后重建连接池） */
    ConnectorVO update(Long id, SaveConnectorRequest req);

    /** 删除连接器（关闭连接池 + 物理删除） */
    void delete(Long id);

    /** 连通性测试（不走连接池，DriverManager 直连） */
    TestResult test(Long id);

    /** 设为当前连接（全局唯一激活） */
    void activate(Long id);

    /** 浏览目标库表清单 */
    List<TableInfo> tables(Long id);

    /** 预览目标表前 50 行 */
    PreviewResult preview(Long id, String table);
}
