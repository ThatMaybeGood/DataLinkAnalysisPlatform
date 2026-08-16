package com.datalink.platform.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 系统信息接口集成测试：不跳过过滤器，走真实安全过滤链（JWT + RBAC）。
 * 使用独立内存 H2 库（V1~V6 迁移全量执行，含 admin/viewer 种子账号）。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_system_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class SystemControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    /** 登录并返回 JWT 令牌 */
    private String login(String username, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("token").asText();
    }

    @Test
    void no_token_401() throws Exception {
        mockMvc.perform(get("/api/system/openapi"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void admin_200() throws Exception {
        String token = login("admin", "admin123");
        assertNotNull(token, "admin 登录应返回 token");
        mockMvc.perform(get("/api/system/openapi").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token", containsString("openapi")))
                .andExpect(jsonPath("$.data.basePath").value("/api/open"))
                .andExpect(jsonPath("$.data.endpoints", hasSize(4)));
    }

    @Test
    void viewer_403() throws Exception {
        String token = login("viewer", "viewer123");
        assertNotNull(token, "viewer 登录应返回 token");
        mockMvc.perform(get("/api/system/openapi").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }
}
