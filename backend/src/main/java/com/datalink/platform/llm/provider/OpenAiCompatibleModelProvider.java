package com.datalink.platform.llm.provider;

import com.datalink.platform.engine.dto.EngineCandidateVO;
import com.datalink.platform.engine.dto.EngineFlowVO;
import com.datalink.platform.llm.config.LlmProperties;
import com.datalink.platform.llm.dto.LlmRefineRequest;
import com.datalink.platform.llm.dto.LlmRefineResult;
import com.datalink.platform.llm.dto.RefinementItem;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.NodeVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 兼容协议 Provider（DeepSeek / 通义千问等 chat completions 接口通用）。
 *
 * <p>普通类，Bean 由 LlmConfig 装配（仅当 datalink.llm.api-key 非空）。
 * 任何异常均降级为 error refinement，绝不抛出。
 */
@Slf4j
public class OpenAiCompatibleModelProvider implements ModelProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "你是数据链路分析助手，负责润色数据链路识别引擎产出的草稿（候选单据、流程模板），"
            + "补充缺失的站点与连线、修正命名。只输出符合指定 JSON schema 的结果，不要输出任何多余文字。";

    private final LlmProperties props;
    private final RestClient restClient;

    public OpenAiCompatibleModelProvider(LlmProperties props, RestClient.Builder builder) {
        this.props = props;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(props.getTimeoutMs());
        factory.setReadTimeout(props.getTimeoutMs());
        this.restClient = builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .requestFactory(factory)
                .build();
    }

    @Override
    public String name() {
        return "openai-compatible";
    }

    @Override
    public boolean available() {
        return StringUtils.hasText(props.getApiKey());
    }

    @Override
    public LlmRefineResult refine(LlmRefineRequest req) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", props.getModel());
            body.put("temperature", props.getTemperature());
            body.put("max_tokens", props.getMaxTokens());
            body.put("response_format", Map.of("type", "json_object"));
            body.put("messages", List.of(
                    Map.of("role", "system", "content", SYSTEM_PROMPT),
                    Map.of("role", "user", "content", buildUserPrompt(req))));

            String resp = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode root = MAPPER.readTree(resp);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (!contentNode.isTextual()) {
                return fallback("响应缺少 choices[0].message.content");
            }
            return parseContent(contentNode.asText(), name());
        } catch (Exception e) {
            log.warn("[G4-llm] 大模型调用失败，回退引擎草稿: {}", e.getMessage());
            return fallback("调用失败: " + e.getClass().getSimpleName());
        }
    }

    /**
     * 解析大模型输出的 content（可能包裹 ```json 代码围栏）为润色结果。
     * 任何解析异常都降级为 error 结果，绝不抛出。
     */
    static LlmRefineResult parseContent(String json, String providerName) {
        try {
            String cleaned = stripCodeFence(json);
            JsonNode root = MAPPER.readTree(cleaned);

            List<NodeVO> addedNodes = new ArrayList<>();
            for (JsonNode n : root.path("addedNodes")) {
                NodeVO node = new NodeVO();
                String id = n.path("id").asText("");
                if (!StringUtils.hasText(id)) {
                    continue;
                }
                node.setId(id.startsWith("llm-") ? id : "llm-" + id);
                node.setName(n.path("name").asText(null));
                node.setCode(n.path("code").asText(null));
                node.setNodeType(n.path("nodeType").asText(null));
                node.setLevel(n.path("level").asText(null));
                node.setStatus(n.path("status").asText(null));
                node.setOwner(n.path("owner").asText(null));
                node.setDescription(n.path("description").asText(null));
                addedNodes.add(node);
            }

            List<EdgeVO> addedEdges = new ArrayList<>();
            int edgeIdx = 0;
            for (JsonNode e : root.path("addedEdges")) {
                EdgeVO edge = new EdgeVO();
                String id = e.path("id").asText("");
                edge.setId(StringUtils.hasText(id) ? id : "llm-e" + (++edgeIdx));
                edge.setSource(e.path("source").asText(null));
                edge.setTarget(e.path("target").asText(null));
                edge.setRelationType(e.path("relationType").asText(null));
                addedEdges.add(edge);
            }

            Map<String, String> renameMap = new LinkedHashMap<>();
            JsonNode renameNode = root.path("renameMap");
            if (renameNode.isObject()) {
                renameNode.fields().forEachRemaining(en -> renameMap.put(en.getKey(), en.getValue().asText()));
            }

            List<RefinementItem> refinements = new ArrayList<>();
            for (JsonNode r : root.path("refinements")) {
                String type = r.path("type").asText(null);
                String text = r.path("text").asText(null);
                if (StringUtils.hasText(type) && StringUtils.hasText(text)) {
                    refinements.add(new RefinementItem(type, text));
                }
            }

            String message = root.path("message").asText(null);
            return LlmRefineResult.builder()
                    .addedNodes(addedNodes)
                    .addedEdges(addedEdges)
                    .renameMap(renameMap)
                    .refinements(refinements)
                    .provider(providerName)
                    .message(message)
                    .build();
        } catch (Exception e) {
            log.warn("[G4-llm] 大模型输出解析失败，回退引擎草稿: {}", e.getMessage());
            return LlmRefineResult.builder()
                    .addedNodes(List.of())
                    .addedEdges(List.of())
                    .renameMap(Map.of())
                    .refinements(List.of(new RefinementItem("error",
                            "大模型输出异常，已回退引擎草稿：内容解析失败")))
                    .provider(providerName)
                    .message("大模型输出非合法 JSON，已回退引擎草稿")
                    .build();
        }
    }

    /** 剥离 ```json ... ``` 代码围栏（大模型常见包裹习惯） */
    private static String stripCodeFence(String content) {
        String s = content == null ? "" : content.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) {
                s = s.substring(firstNewline + 1);
            }
            if (s.endsWith("```")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.trim();
        }
        return s;
    }

    private LlmRefineResult fallback(String reason) {
        return LlmRefineResult.builder()
                .addedNodes(List.of())
                .addedEdges(List.of())
                .renameMap(Map.of())
                .refinements(List.of(new RefinementItem("error",
                        "大模型输出异常，已回退引擎草稿：" + reason)))
                .provider(name())
                .message("大模型调用失败，已回退引擎草稿")
                .build();
    }

    private String buildUserPrompt(LlmRefineRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("数据库：").append(req.getDatabase() == null ? "(未命名)" : req.getDatabase()).append("\n\n");

        sb.append("引擎识别的候选单据：\n");
        if (req.getCandidates() == null || req.getCandidates().isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (EngineCandidateVO c : req.getCandidates()) {
                sb.append("- name=").append(c.getName())
                        .append(", table=").append(c.getTable())
                        .append(", confidence=").append(c.getConfidence())
                        .append(", marks=").append(c.getMarks())
                        .append("\n");
            }
        }

        sb.append("\n引擎推导的流程模板：\n");
        if (req.getFlows() == null || req.getFlows().isEmpty()) {
            sb.append("（无）\n");
        } else {
            for (EngineFlowVO f : req.getFlows()) {
                sb.append("- name=").append(f.getName())
                        .append(", tables=").append(f.getTableNames())
                        .append("\n");
            }
        }

        sb.append("\n请基于以上引擎草稿做润色与补充，输出严格 JSON（不要输出其它任何文字）：\n")
                .append("{\n")
                .append("  \"addedNodes\": [{\"id\":\"llm-xxx\",\"name\":\"...\",\"code\":\"...\",")
                .append("\"nodeType\":\"...\",\"level\":\"...\",\"status\":\"...\"}],\n")
                .append("  \"addedEdges\": [{\"id\":\"llm-e1\",\"source\":\"...\",\"target\":\"...\",\"relationType\":\"...\"}],\n")
                .append("  \"renameMap\": {\"原显示名\": \"新显示名\"},\n")
                .append("  \"refinements\": [{\"type\":\"rename|chain|party|relation|flow\",\"text\":\"说明\"}],\n")
                .append("  \"message\": \"整体说明\"\n")
                .append("}\n")
                .append("要求：\n")
                .append("1. addedNodes 的 id 必须以 \"llm-\" 前缀；nodeType 取值参考站点类型（如 DATABASE / TABLE）；\n")
                .append("2. addedEdges 的 relationType 取值如 DATA_FLOW；\n")
                .append("3. renameMap 仅包含确有必要改名的条目；\n")
                .append("4. 无补充时对应字段返回空数组或空对象。\n");
        return sb.toString();
    }
}
