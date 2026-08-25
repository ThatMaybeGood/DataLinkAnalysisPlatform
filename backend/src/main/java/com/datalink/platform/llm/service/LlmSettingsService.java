package com.datalink.platform.llm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.util.AesUtil;
import com.datalink.platform.llm.config.LlmProperties;
import com.datalink.platform.llm.dto.LlmSettingsRequest;
import com.datalink.platform.llm.dto.LlmSettingsView;
import com.datalink.platform.llm.dto.LlmTestResult;
import com.datalink.platform.llm.entity.LlmSetting;
import com.datalink.platform.llm.mapper.LlmSettingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 大模型接入配置服务：支持多配置 + 切换启用（cc-switch 式）。
 * <p>每个 DB 行是一个配置（name 区分），is_active=1 为当前启用（全局唯一）；
 * 未配置任何 DB 配置时回退环境变量（application.yml datalink.llm.*）。
 * <p>无缓存、实时读 DB，保存即生效；apiKey AES-GCM 加密存储、绝不回传明文。
 * <p>启动顺序安全：构造器不查库，首次读取惰性 + try/catch 兜底（Flyway 未就绪只回退 env）。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LlmSettingsService {

    private final LlmSettingMapper llmSettingMapper;
    private final LlmProperties llmProperties;
    private final AesUtil aesUtil;
    private final RestClient.Builder restClientBuilder;

    /** 当前生效配置：优先 DB 中启用的配置，无则环境变量 */
    public LlmSettingsView getView() {
        return buildView(safeSelectActive());
    }

    /** 全部配置列表（当前启用排前；无 DB 启用项时，首条为内置「默认配置」env 兜底） */
    public List<LlmSettingsView> list() {
        try {
            List<LlmSetting> all = llmSettingMapper.selectList(
                    Wrappers.lambdaQuery(LlmSetting.class)
                            .orderByDesc(LlmSetting::getIsActive)
                            .orderByAsc(LlmSetting::getId));
            List<LlmSettingsView> views = new ArrayList<>();
            boolean anyActive = all.stream()
                    .anyMatch(s -> s.getIsActive() != null && s.getIsActive() == 1);
            if (!anyActive) {
                views.add(envView());   // 内置「默认配置」：无 DB 启用项时 env 兜底即当前生效
            }
            all.stream().map(this::buildView).forEach(views::add);
            return views;
        } catch (Exception e) {
            log.warn("读取 llm_config 列表失败: {}", e.getMessage());
            List<LlmSettingsView> fallback = new ArrayList<>();
            fallback.add(envView());
            return fallback;
        }
    }

    /** 新建配置（name 必填）；首个配置自动启用 */
    @Transactional
    public LlmSettingsView create(LlmSettingsRequest req) {
        if (!StringUtils.hasText(req.getName())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "配置名不能为空");
        }
        LlmSetting s = new LlmSetting();
        s.setConfigKey("custom");
        apply(s, req);
        s.setIsActive(safeCount() == 0 ? 1 : 0);
        s.setUpdatedBy(currentOperator());
        llmSettingMapper.insert(s);
        return buildView(s);
    }

    /** 更新指定配置（apiKey 留空 = 不改） */
    @Transactional
    public LlmSettingsView update(Long id, LlmSettingsRequest req) {
        LlmSetting s = require(id);
        apply(s, req);
        s.setUpdatedBy(currentOperator());
        llmSettingMapper.updateById(s);
        return buildView(s);
    }

    /** 删除配置 */
    @Transactional
    public void delete(Long id) {
        llmSettingMapper.deleteById(id);
    }

    /** 切换启用：目标配置置 1，其余置 0 */
    @Transactional
    public LlmSettingsView activate(Long id) {
        LlmSetting s = require(id);
        llmSettingMapper.update(null, Wrappers.lambdaUpdate(LlmSetting.class)
                .set(LlmSetting::getIsActive, 0).eq(LlmSetting::getIsActive, 1));
        s.setIsActive(1);
        s.setUpdatedBy(currentOperator());
        llmSettingMapper.updateById(s);
        return buildView(s);
    }

    /** 连通性测试：id=null → 内置默认（env 兜底）；否则对应 DB 配置。最小 chat/completions ping。不写库、不改 is_active。 */
    public LlmTestResult test(Long id) {
        LlmSettingsView view = id == null ? envView() : buildView(require(id));
        if (!StringUtils.hasText(view.getApiKey())) {
            return new LlmTestResult(false, null, "未配置 API Key");
        }
        if (!StringUtils.hasText(view.getBaseUrl())) {
            return new LlmTestResult(false, null, "未配置 Base URL");
        }
        if (!StringUtils.hasText(view.getModel())) {
            return new LlmTestResult(false, null, "未配置模型名");
        }
        RestClient client = buildPingClient(view);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", view.getModel());
        body.put("max_tokens", 1);
        body.put("messages", List.of(Map.of("role", "user", "content", "ping")));
        long start = System.currentTimeMillis();
        try {
            String resp = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            long latency = System.currentTimeMillis() - start;
            if (!StringUtils.hasText(resp)) {
                return new LlmTestResult(false, latency, "响应为空");
            }
            return new LlmTestResult(true, latency, "连接成功");
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("LLM 连通性测试失败: {}", e.getMessage());
            return new LlmTestResult(false, latency, "连接失败: " + e.getClass().getSimpleName());
        }
    }

    private RestClient buildPingClient(LlmSettingsView view) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeout = view.getTimeoutMs() != null ? view.getTimeoutMs() : 30000;
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return restClientBuilder.clone()
                .baseUrl(view.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + view.getApiKey())
                .requestFactory(factory)
                .build();
    }

    private LlmSetting safeSelectActive() {
        try {
            return llmSettingMapper.selectOne(Wrappers.lambdaQuery(LlmSetting.class)
                    .eq(LlmSetting::getIsActive, 1).last("LIMIT 1"));
        } catch (Exception e) {
            log.warn("读取 llm_config 失败，回退环境变量配置: {}", e.getMessage());
            return null;
        }
    }

    private LlmSetting require(Long id) {
        LlmSetting s = llmSettingMapper.selectById(id);
        if (s == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "大模型配置不存在: " + id);
        }
        return s;
    }

    private long safeCount() {
        try {
            return llmSettingMapper.selectCount(null);
        } catch (Exception e) {
            return 0;
        }
    }

    private void apply(LlmSetting s, LlmSettingsRequest req) {
        if (StringUtils.hasText(req.getName())) s.setName(req.getName().trim());
        if (StringUtils.hasText(req.getBaseUrl())) s.setBaseUrl(req.getBaseUrl().trim());
        if (StringUtils.hasText(req.getModel())) s.setModel(req.getModel().trim());
        if (req.getTimeoutMs() != null) s.setTimeoutMs(req.getTimeoutMs());
        if (req.getMaxTokens() != null) s.setMaxTokens(req.getMaxTokens());
        if (req.getTemperature() != null) s.setTemperature(req.getTemperature());
        if (StringUtils.hasText(req.getApiKey())) {
            s.setEncryptedApiKey(aesUtil.encrypt(req.getApiKey().trim()));
        }
    }

    /** DB 配置 → 视图；null → 环境变量视图 */
    private LlmSettingsView buildView(LlmSetting s) {
        if (s == null) {
            return envView();
        }
        String apiKey = null;
        if (StringUtils.hasText(s.getEncryptedApiKey())) {
            try {
                apiKey = aesUtil.decrypt(s.getEncryptedApiKey());
            } catch (Exception e) {
                log.warn("LLM API Key 解密失败: {}", e.getMessage());
            }
        }
        boolean hasKey = StringUtils.hasText(apiKey);
        return LlmSettingsView.builder()
                .id(s.getId())
                .name(s.getName())
                .isActive(s.getIsActive() != null && s.getIsActive() == 1)
                .baseUrl(s.getBaseUrl())
                .apiKey(apiKey)
                .model(s.getModel())
                .timeoutMs(s.getTimeoutMs())
                .maxTokens(s.getMaxTokens())
                .temperature(s.getTemperature())
                .apiKeyMasked(hasKey ? mask(apiKey) : "")
                .hasKey(hasKey)
                .source("db")
                .updatedAt(s.getUpdatedAt())
                .updatedBy(s.getUpdatedBy())
                .build();
    }

    private LlmSettingsView envView() {
        String apiKey = llmProperties.getApiKey();
        boolean hasKey = StringUtils.hasText(apiKey);
        return LlmSettingsView.builder()
                .id(null)
                .name("默认配置")
                .isActive(true)             // env 兜底即当前生效（无 DB 启用项时）
                .baseUrl(llmProperties.getBaseUrl())
                .apiKey(apiKey)
                .model(llmProperties.getModel())
                .timeoutMs(llmProperties.getTimeoutMs())
                .maxTokens(llmProperties.getMaxTokens())
                .temperature(llmProperties.getTemperature())
                .apiKeyMasked(hasKey ? mask(apiKey) : "")
                .hasKey(hasKey)
                .source("env")
                .build();
    }

    /** 掩码：len<=8 全隐藏，否则前4 + **** + 后4 */
    private String mask(String key) {
        if (key == null) return "";
        if (key.length() <= 8) return "****";
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    private String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return "system";
    }
}
