package com.datalink.platform.openapi;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.monitor.entity.CheckResult;
import com.datalink.platform.monitor.entity.Instance;
import com.datalink.platform.monitor.mapper.CheckResultMapper;
import com.datalink.platform.monitor.mapper.InstanceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 开放 API 集成测试（MockMvc 真实过滤链：X-API-Key 鉴权 + 四个接口）。
 * 使用独立内存 H2 库（V1~V6 迁移全量执行，含种子数据）。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_openapi_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class OpenApiTest {

    private static final String TOKEN = "datalink-openapi-dev-token-001";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InstanceMapper instanceMapper;
    @Autowired
    private CheckResultMapper checkResultMapper;

    @Test
    void no_key_401() throws Exception {
        mockMvc.perform(get("/api/open/processes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("无效的 API Token"));
    }

    @Test
    void bad_key_401() throws Exception {
        mockMvc.perform(get("/api/open/processes").header("X-API-Key", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void valid_key_200() throws Exception {
        mockMvc.perform(get("/api/open/processes").header("X-API-Key", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void report_instance() throws Exception {
        // 首次上报：新建
        mockMvc.perform(post("/api/open/instances")
                        .header("X-API-Key", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizNo\":\"EXT-0001\",\"bizName\":\"外部单号示例\",\"status\":\"RUNNING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bizNo").value("EXT-0001"));
        // 同 bizNo 再次上报：走更新分支
        mockMvc.perform(post("/api/open/instances")
                        .header("X-API-Key", TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bizNo\":\"EXT-0001\",\"bizName\":\"外部单号示例\",\"status\":\"SUCCESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.bizNo").value("EXT-0001"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));
        // 幂等：该 bizNo 仅一条实例
        Long count = instanceMapper.selectCount(Wrappers.lambdaQuery(Instance.class)
                .eq(Instance::getBizNo, "EXT-0001"));
        assertEquals(1L, count);
    }

    @Test
    void trigger_checkpoint() throws Exception {
        Long before = checkResultMapper.selectCount(Wrappers.lambdaQuery(CheckResult.class)
                .eq(CheckResult::getCheckpointId, 1L));
        mockMvc.perform(post("/api/open/checkpoints/1/trigger").header("X-API-Key", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.checkpointId").value(1))
                .andExpect(jsonPath("$.data.status").value("PASS"));
        Long after = checkResultMapper.selectCount(Wrappers.lambdaQuery(CheckResult.class)
                .eq(CheckResult::getCheckpointId, 1L));
        assertEquals(before + 1, after);
    }
}
