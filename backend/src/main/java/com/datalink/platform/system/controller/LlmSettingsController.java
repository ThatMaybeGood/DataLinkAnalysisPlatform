package com.datalink.platform.system.controller;

import com.datalink.platform.common.Result;
import com.datalink.platform.llm.dto.LlmSettingsRequest;
import com.datalink.platform.llm.dto.LlmSettingsView;
import com.datalink.platform.llm.dto.LlmTestResult;
import com.datalink.platform.llm.service.LlmSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 大模型接入配置接口（位于 /api/system/**，SecurityConfig 已限仅 ADMIN）。
 * 支持多配置 + 切换启用（cc-switch 式）。
 */
@RestController
@RequestMapping("/api/system/llm")
@RequiredArgsConstructor
public class LlmSettingsController {

    private final LlmSettingsService llmSettingsService;

    /** 当前生效配置（DB 启用项，无则环境变量；apiKey 只回掩码） */
    @GetMapping
    public Result<LlmSettingsView> get() {
        return Result.ok(llmSettingsService.getView());
    }

    /** 全部配置列表（含各配置 id / 启用状态 / Key 掩码） */
    @GetMapping("/list")
    public Result<List<LlmSettingsView>> list() {
        return Result.ok(llmSettingsService.list());
    }

    /** 新建配置（name 必填；首个配置自动启用） */
    @PostMapping
    public Result<LlmSettingsView> create(@RequestBody LlmSettingsRequest request) {
        return Result.ok(llmSettingsService.create(request));
    }

    /** 更新指定配置（apiKey 留空 = 不改），保存即生效 */
    @PutMapping("/{id}")
    public Result<LlmSettingsView> update(@PathVariable Long id, @RequestBody LlmSettingsRequest request) {
        return Result.ok(llmSettingsService.update(id, request));
    }

    /** 删除配置 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        llmSettingsService.delete(id);
        return Result.ok();
    }

    /** 切换启用（目标置 1，其余置 0） */
    @PostMapping("/{id}/activate")
    public Result<LlmSettingsView> activate(@PathVariable Long id) {
        return Result.ok(llmSettingsService.activate(id));
    }

    /** 连通性测试（指定 DB 配置；不改变启用状态） */
    @PostMapping("/{id}/test")
    public Result<LlmTestResult> test(@PathVariable Long id) {
        return Result.ok(llmSettingsService.test(id));
    }

    /** 连通性测试（内置「默认配置」env 兜底；字面量段优先于 {id}，不与上者冲突） */
    @PostMapping("/default/test")
    public Result<LlmTestResult> testDefault() {
        return Result.ok(llmSettingsService.test(null));
    }
}
