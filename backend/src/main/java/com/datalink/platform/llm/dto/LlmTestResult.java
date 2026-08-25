package com.datalink.platform.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型连通性测试结果（用于「当前大模型」切换前校验，以及配置页手动测试）。
 * 不持久化、不改变 is_active。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LlmTestResult {
    private boolean ok;
    private Long latencyMs;
    private String message;
}
