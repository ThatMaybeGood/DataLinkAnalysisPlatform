package com.datalink.platform.datasource.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.controller.ConnectorController;
import com.datalink.platform.datasource.dto.CandidateNodeVO;
import com.datalink.platform.datasource.dto.TestResult;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.model.entity.Node;
import com.datalink.platform.model.mapper.NodeMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CMDB 连接器集成测试。
 * 使用 JDK 自带 com.sun.net.httpserver.HttpServer 起内嵌 HTTP 桩（随机端口），
 * 验证 fetchAssets 采集映射、候选缓存、ConnectorService.test 分支、以及候选导入 node 表与重复判重。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_cmdb_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Transactional
class CmdbServiceTest {

    @Autowired private CmdbService cmdbService;
    @Autowired private ConnectorService connectorService;
    @Autowired private ConnectorMapper connectorMapper;
    @Autowired private NodeMapper nodeMapper;
    @Autowired private ConnectorController connectorController;

    /** 起内嵌 HTTP 桩，返回固定 JSON 响应体 */
    private HttpServer startStub(String responseBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        return server;
    }

    /** 构造 CMDB 连接器实体 */
    private Connector cmdbConnector(String apiUrl) {
        Connector c = new Connector();
        c.setConnectorType("CMDB");
        c.setName("cmdb-conn");
        c.setEnabled(1);
        c.setConfig("{\"apiUrl\":\"" + apiUrl + "\"}");
        return c;
    }

    @Test
    void fetchAssets_returns_candidates_from_array() throws Exception {
        HttpServer server = startStub("[{\"name\":\"资产A\",\"type\":\"SYSTEM\",\"description\":\"descA\"},{\"name\":\"资产B\",\"type\":\"DATABASE\"}]");
        try {
            String apiUrl = "http://localhost:" + server.getAddress().getPort() + "/";
            List<CandidateNodeVO> list = cmdbService.fetchAssets(cmdbConnector(apiUrl));
            assertEquals(2, list.size());
            assertEquals("资产A", list.get(0).getName());
            assertEquals("SYSTEM", list.get(0).getType());
            assertEquals("descA", list.get(0).getDescription());
            assertEquals("", list.get(0).getOwner(), "owner 缺失应给默认空串");
            assertEquals("资产B", list.get(1).getName());
            assertEquals("DATABASE", list.get(1).getType());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchAssets_handles_data_wrapped_response() throws Exception {
        HttpServer server = startStub("{\"data\":[{\"name\":\"资产C\",\"type\":\"SYSTEM\"}]}");
        try {
            String apiUrl = "http://localhost:" + server.getAddress().getPort() + "/";
            List<CandidateNodeVO> list = cmdbService.fetchAssets(cmdbConnector(apiUrl));
            assertEquals(1, list.size());
            assertEquals("资产C", list.get(0).getName());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchAssets_sends_api_key_header_when_configured() throws Exception {
        String[] receivedKey = {null};
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            receivedKey[0] = exchange.getRequestHeaders().getFirst("X-API-Key");
            byte[] body = "[]".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        try {
            String apiUrl = "http://localhost:" + server.getAddress().getPort() + "/";
            Connector c = cmdbConnector(apiUrl);
            c.setConfig("{\"apiUrl\":\"" + apiUrl + "\",\"apiKey\":\"secret-key\"}");
            cmdbService.fetchAssets(c);
            assertEquals("secret-key", receivedKey[0], "配置 apiKey 时应携带 X-API-Key 请求头");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchAssets_missing_apiUrl_throws() {
        Connector c = cmdbConnector("http://localhost:1/");
        c.setConfig("{}");
        BusinessException ex = assertThrows(BusinessException.class, () -> cmdbService.fetchAssets(c));
        assertEquals(400, ex.getCode());
        assertEquals("CMDB 连接未配置 apiUrl", ex.getMessage());
    }

    @Test
    void store_and_get_candidates_roundtrip() {
        cmdbService.storeCandidates(99L, Arrays.asList(
                new CandidateNodeVO("资产A", "SYSTEM", "descA", "张三"),
                new CandidateNodeVO("资产B", "DATABASE", "", "")));
        assertEquals(2, cmdbService.getCandidates(99L).size());
        cmdbService.clear(99L);
        assertTrue(cmdbService.getCandidates(99L).isEmpty(), "clear 后候选应为空");
    }

    @Test
    void test_cmdb_connector_ok() throws Exception {
        HttpServer server = startStub("[{\"name\":\"资产A\",\"type\":\"SYSTEM\"}]");
        try {
            String apiUrl = "http://localhost:" + server.getAddress().getPort() + "/";
            Connector c = cmdbConnector(apiUrl);
            connectorMapper.insert(c);
            TestResult result = connectorService.test(c.getId());
            assertTrue(result.isOk(), "CMDB 连通测试应成功: " + result.getMessage());
            assertEquals("CMDB API", result.getDbVersion());
            assertNotNull(result.getLatencyMs());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void import_creates_nodes_and_dedup() {
        Connector c = cmdbConnector("http://localhost:1/");
        connectorMapper.insert(c);
        Long id = c.getId();
        List<CandidateNodeVO> candidates = Arrays.asList(
                new CandidateNodeVO("资产A", "SYSTEM", "descA", "张三"),
                new CandidateNodeVO("资产B", null, null, null));

        cmdbService.storeCandidates(id, candidates);
        int first = connectorController.importNodes(id).getData();
        assertEquals(2, first, "首次导入应导入 2 条");

        List<Node> nodes = nodeMapper.selectList(Wrappers.lambdaQuery(Node.class).likeRight(Node::getCode, "CMDB_"));
        assertEquals(2, nodes.size(), "node 表应新增 2 条候选站点");
        Node a = nodes.stream().filter(n -> "资产A".equals(n.getName())).findFirst().orElseThrow();
        assertEquals("SYSTEM", a.getNodeType());
        assertEquals("L3", a.getLevel());
        assertEquals("ACTIVE", a.getStatus());
        assertEquals("descA", a.getDescription());
        assertEquals("张三", a.getOwner());
        assertNotNull(a.getCreatedAt());
        assertTrue(a.getCode().startsWith("CMDB_"), "code 应以 CMDB_ 开头");
        Node b = nodes.stream().filter(n -> "资产B".equals(n.getName())).findFirst().orElseThrow();
        assertEquals("SYSTEM", b.getNodeType(), "候选 type 缺失应回退 SYSTEM");

        // 重复导入：重新缓存同一批候选，应全部按 code 判重跳过
        cmdbService.storeCandidates(id, candidates);
        int second = connectorController.importNodes(id).getData();
        assertEquals(0, second, "重复导入不应新增节点");
        long total = nodeMapper.selectCount(Wrappers.lambdaQuery(Node.class).likeRight(Node::getCode, "CMDB_"));
        assertEquals(2L, total);
        assertTrue(cmdbService.getCandidates(id).isEmpty(), "导入后应清空候选缓存");
    }
}
