package com.datalink.platform.monitor;

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
 * 监控域 Controller 集成测试（MockMvc，内存 H2 应用库）。
 * @Transactional 回滚 POST 写入，避免污染其他测试。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_monitor_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboard_stats_returns_expected() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.processCount").value(2));
    }

    @Test
    void alerts_returns_five() throws Exception {
        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(5)));
    }

    @Test
    void instances_returns_six() throws Exception {
        mockMvc.perform(get("/api/instances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(6));
    }

    @Test
    void trace_returns_upstream_and_downstream() throws Exception {
        mockMvc.perform(get("/api/graph/13/trace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.upstream", hasSize(1)))
                .andExpect(jsonPath("$.data.downstream", hasSize(2)));
    }

    @Test
    void create_ticket_succeeds() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alertId\":1,\"assignee\":\"测试\",\"description\":\"测试工单\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }
}
