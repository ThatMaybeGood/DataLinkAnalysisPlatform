package com.datalink.platform.datasource.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 连接器 Controller 集成测试（MockMvc，内存 H2 应用库）。
 * 与 ConnectorServiceTest 使用相同数据源属性（同一库名与凭据，多上下文兼容共存）。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:datalink_conn_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=secret"
})
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class ConnectorControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private static final String APP_DB = "datalink_conn_test";

    private long createConnector(String name) throws Exception {
        String body = mockMvc.perform(post("/api/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"dbType\":\"h2\","
                                + "\"databaseName\":\"" + APP_DB + "\",\"username\":\"sa\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asLong();
    }

    @Test
    void create_returns_vo_without_password() throws Exception {
        mockMvc.perform(post("/api/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"new-conn\",\"dbType\":\"h2\","
                                + "\"databaseName\":\"" + APP_DB + "\",\"username\":\"sa\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void test_connector_ok() throws Exception {
        long id = createConnector("test-conn");
        mockMvc.perform(post("/api/connectors/{id}/test", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ok").value(true));
    }

    @Test
    void activate_makes_unique() throws Exception {
        long a = createConnector("act-A");
        long b = createConnector("act-B");
        mockMvc.perform(post("/api/connectors/{id}/activate", a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/connectors/{id}/activate", b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        String listBody = mockMvc.perform(get("/api/connectors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        int activeCount = 0;
        for (JsonNode rec : objectMapper.readTree(listBody).path("data").path("records")) {
            if (rec.path("isActive").asInt() == 1) {
                activeCount++;
            }
        }
        assertEquals(1, activeCount, "激活后列表 isActive=1 应仅 1 条");
    }

    @Test
    void tables_contains_connector() throws Exception {
        long id = createConnector("tables-conn");
        mockMvc.perform(get("/api/connectors/{id}/tables", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[?(@.name=='connector')]").isNotEmpty());
    }

    @Test
    void preview_returns_columns() throws Exception {
        long id = createConnector("preview-conn");
        mockMvc.perform(get("/api/connectors/{id}/tables/connector/preview", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.columns[0]").exists());
    }

    @Test
    void unknown_dbtype_returns_400() throws Exception {
        mockMvc.perform(post("/api/connectors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"dbType\":\"oracle\","
                                + "\"databaseName\":\"y\",\"username\":\"sa\",\"password\":\"p\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
