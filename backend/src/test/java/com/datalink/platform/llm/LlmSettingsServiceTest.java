package com.datalink.platform.llm;

import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.util.AesUtil;
import com.datalink.platform.llm.config.LlmProperties;
import com.datalink.platform.llm.dto.LlmSettingsView;
import com.datalink.platform.llm.dto.LlmTestResult;
import com.datalink.platform.llm.entity.LlmSetting;
import com.datalink.platform.llm.mapper.LlmSettingMapper;
import com.datalink.platform.llm.service.LlmSettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * LlmSettingsService 纯单测（Mockito，不起 Spring 上下文、不打真实 HTTP）。
 * 覆盖：list() 内置「默认配置」注入规则、test() 无 Key 快速失败与未知 id 抛错。
 */
@ExtendWith(MockitoExtension.class)
class LlmSettingsServiceTest {

    @Mock private LlmSettingMapper mapper;
    @Mock private AesUtil aesUtil;

    private LlmSettingsService service;
    private LlmProperties props;

    @BeforeEach
    void setUp() {
        props = new LlmProperties();
        service = new LlmSettingsService(mapper, props, aesUtil, RestClient.builder());
    }

    private LlmSetting dbRow(Long id, String name, int isActive) {
        LlmSetting s = new LlmSetting();
        s.setId(id);
        s.setName(name);
        s.setConfigKey("custom");
        s.setBaseUrl("https://api.example.com/v1");
        s.setModel("deepseek-chat");
        s.setTimeoutMs(30000);
        s.setMaxTokens(2048);
        s.setTemperature(0.2);
        s.setIsActive(isActive);
        return s;
    }

    @Test
    void list_prepends_env_default_when_no_active_db_row() {
        when(mapper.selectList(any())).thenReturn(List.of(dbRow(1L, "备用配置", 0)));

        List<LlmSettingsView> list = service.list();

        assertEquals(2, list.size());
        LlmSettingsView first = list.get(0);
        assertEquals("env", first.getSource());
        assertEquals("默认配置", first.getName());
        assertTrue(first.isActive());
        assertEquals("db", list.get(1).getSource());
    }

    @Test
    void list_omits_env_default_when_db_active() {
        when(mapper.selectList(any())).thenReturn(List.of(dbRow(1L, "生产配置", 1)));

        List<LlmSettingsView> list = service.list();

        assertEquals(1, list.size());
        assertEquals("db", list.get(0).getSource());
        assertEquals("生产配置", list.get(0).getName());
        assertTrue(list.get(0).isActive());
    }

    @Test
    void test_env_default_no_key_fails_fast() {
        // props.apiKey 默认为空 → env 兜底无 Key，不发 HTTP 直接失败
        LlmTestResult result = service.test(null);

        assertFalse(result.isOk());
        assertTrue(result.getMessage().contains("API Key"));
    }

    @Test
    void test_unknown_id_throws_business_exception() {
        when(mapper.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.test(999L));
    }
}
