package com.datalink.platform.engine.controller;

import com.datalink.platform.engine.dto.EngineDraftVO;
import com.datalink.platform.engine.dto.RefineResultVO;
import com.datalink.platform.engine.service.EngineAnalyzeService;
import com.datalink.platform.llm.dto.RefinementItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * G4 大模型细化接口 MockMvc 测试。
 *
 * <p>认证处理方式与 ModelingControllerTest 一致：
 * {@code @AutoConfigureMockMvc(addFilters = false)} 关闭安全过滤器，直接打 controller。
 * EngineAnalyzeService 以 @MockBean 替换，不触库、不调真实大模型。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_refine_ctrl_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc(addFilters = false)
class AnalyzeRefineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EngineAnalyzeService engineAnalyzeService;

    @Test
    void refine_returns_merged_result() throws Exception {
        EngineDraftVO base = new EngineDraftVO();
        base.setDatabase("datalink_demo");
        base.setMessage("扫描 6 张表");
        when(engineAnalyzeService.refine(1L)).thenReturn(
                RefineResultVO.builder()
                        .base(base)
                        .addedNodes(Collections.emptyList())
                        .addedEdges(Collections.emptyList())
                        .renameMap(Collections.emptyMap())
                        .refinements(List.of(new RefinementItem("note", "细化完成")))
                        .provider("mock")
                        .message("细化完成")
                        .build());

        mockMvc.perform(post("/api/analyze/refine")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"connectorId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.provider").value("mock"))
                .andExpect(jsonPath("$.data.base.database").value("datalink_demo"))
                .andExpect(jsonPath("$.data.refinements[0].type").value("note"));
    }

    @Test
    void refine_without_body_is_rejected() throws Exception {
        // 项目 GlobalExceptionHandler 统一以 HTTP 200 + body.code 表达错误，
        // 缺 body 走兜底 Exception 处理器 → HTTP 200 且 code != 200
        mockMvc.perform(post("/api/analyze/refine")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", not(200)));
    }
}
