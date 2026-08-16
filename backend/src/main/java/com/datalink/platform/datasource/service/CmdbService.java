package com.datalink.platform.datasource.service;

import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.dto.CandidateNodeVO;
import com.datalink.platform.datasource.entity.Connector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CMDB 连接器服务：通过 HTTP API 采集外部资产清单，映射为候选站点。
 * 候选按连接器 id 缓存于内存（ConcurrentHashMap），供导入 node 表使用。
 */
@Service
@RequiredArgsConstructor
public class CmdbService {

    private static final int HTTP_TIMEOUT_MS = 5000;

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    /** 候选缓存：connectorId → 资产候选 */
    private final ConcurrentHashMap<Long, List<CandidateNodeVO>> candidateCache = new ConcurrentHashMap<>();

    /**
     * 调用 CMDB HTTP API 采集资产候选。
     * config JSON：{"apiUrl":"...","apiKey":"..."}；apiUrl 必填，apiKey 可选（作为 X-API-Key 请求头）。
     */
    public List<CandidateNodeVO> fetchAssets(Connector c) {
        JsonNode config = parseConfig(c);
        String apiUrl = config.path("apiUrl").asText(null);
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new BusinessException(400, "CMDB 连接未配置 apiUrl");
        }
        String apiKey = config.path("apiKey").asText(null);

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(HTTP_TIMEOUT_MS);
        requestFactory.setReadTimeout(HTTP_TIMEOUT_MS);
        RestClient client = restClientBuilder.clone().requestFactory(requestFactory).build();
        try {
            String body = client.get().uri(apiUrl)
                    .headers(headers -> {
                        if (apiKey != null && !apiKey.isBlank()) {
                            headers.set("X-API-Key", apiKey);
                        }
                    })
                    .retrieve()
                    .body(String.class);
            return parseBody(body);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(400, "CMDB 采集失败: " + shortMessage(e));
        }
    }

    /** 读取候选缓存（无则空列表） */
    public List<CandidateNodeVO> getCandidates(Long id) {
        return candidateCache.getOrDefault(id, Collections.emptyList());
    }

    /** 写入候选缓存 */
    public void storeCandidates(Long id, List<CandidateNodeVO> candidates) {
        candidateCache.put(id, candidates == null ? Collections.emptyList() : candidates);
    }

    /** 清空候选缓存 */
    public void clear(Long id) {
        candidateCache.remove(id);
    }

    /** 解析 config JSON；非法 JSON 视为采集失败 */
    private JsonNode parseConfig(Connector c) {
        String config = c.getConfig();
        if (config == null || config.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(config);
        } catch (Exception e) {
            throw new BusinessException(400, "CMDB 采集失败: " + shortMessage(e));
        }
    }

    /** 解析响应体：兼容 JSON 数组或 {data:[...]} 两种结构 */
    private List<CandidateNodeVO> parseBody(String body) {
        if (body == null || body.isBlank()) {
            throw new BusinessException(400, "CMDB 采集失败: 响应为空");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            List<CandidateNodeVO> result = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode item : root) {
                    result.add(toCandidate(item));
                }
            } else {
                JsonNode data = root.path("data");
                if (!data.isArray()) {
                    throw new BusinessException(400, "CMDB 采集失败: 响应不是数组且缺少 data 数组");
                }
                for (JsonNode item : data) {
                    result.add(toCandidate(item));
                }
            }
            return result;
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            throw new BusinessException(400, "CMDB 采集失败: " + shortMessage(e));
        }
    }

    /** 单条资产映射，缺失字段给默认值（空串，type 缺省由导入层回退 SYSTEM） */
    private CandidateNodeVO toCandidate(JsonNode item) {
        return new CandidateNodeVO(
                item.path("name").asText(""),
                item.path("type").asText(""),
                item.path("description").asText(""),
                item.path("owner").asText(""));
    }

    /** 异常简短信息（截断，避免过长） */
    private String shortMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = e.getClass().getSimpleName();
        }
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}
