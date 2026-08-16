package com.datalink.platform.system.controller;

import com.datalink.platform.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统信息接口：供前端「开放 API」管理卡展示 Token 与接口清单（仅 ADMIN 可访问）。
 */
@RestController
@RequestMapping("/api/system")
public class SystemController {

    @Value("${datalink.openapi.token}")
    private String openApiToken;

    /**
     * 开放 API 信息：Token、基础路径与接口清单
     */
    @GetMapping("/openapi")
    public Result<Map<String, Object>> openApi() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", openApiToken);
        data.put("basePath", "/api/open");
        data.put("endpoints", buildEndpoints());
        return Result.ok(data);
    }

    /** 开放 API 接口清单 */
    private List<Map<String, String>> buildEndpoints() {
        List<Map<String, String>> endpoints = new ArrayList<>();
        endpoints.add(endpoint("POST", "/api/open/instances", "上报/更新实例（按 bizNo 幂等）"));
        endpoints.add(endpoint("GET", "/api/open/processes", "查询流程"));
        endpoints.add(endpoint("GET", "/api/open/nodes", "查询站点"));
        endpoints.add(endpoint("POST", "/api/open/checkpoints/{id}/trigger", "触发检测"));
        return endpoints;
    }

    private Map<String, String> endpoint(String method, String path, String desc) {
        Map<String, String> e = new LinkedHashMap<>();
        e.put("method", method);
        e.put("path", path);
        e.put("desc", desc);
        return e;
    }
}
