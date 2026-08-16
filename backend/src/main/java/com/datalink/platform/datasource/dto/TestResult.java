package com.datalink.platform.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 连通性测试结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {
    private boolean ok;
    private Long latencyMs;
    private String dbVersion;
    private String message;
}
