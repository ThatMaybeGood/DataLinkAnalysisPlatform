package com.datalink.platform.model.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 建模域 Controller 集成测试（MockMvc，内存 H2 应用库）。
 * @Transactional 回滚 POST 写入，避免污染其他测试。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_model_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@Transactional
class ModelingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void nodes_list_returns_all() throws Exception {
        mockMvc.perform(get("/api/nodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(18)));
    }

    @Test
    void processes_list_returns_all() throws Exception {
        mockMvc.perform(get("/api/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    void routes_list_returns_all() throws Exception {
        mockMvc.perform(get("/api/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(5)));
    }

    @Test
    void create_route_returns_node_ids() throws Exception {
        mockMvc.perform(post("/api/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试路线\",\"processId\":2,\"priority\":\"ALTERNATE\",\"status\":\"ACTIVE\",\"nodeIds\":[9,18]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.nodeIds", hasSize(2)))
                .andExpect(jsonPath("$.data.nodeIds[0]").value("9"))
                .andExpect(jsonPath("$.data.nodeIds[1]").value("18"));
    }

    @Test
    void search_returns_non_empty() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "付款"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0]").exists());
    }
}
