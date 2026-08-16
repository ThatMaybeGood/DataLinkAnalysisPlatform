package com.datalink.platform.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 数据预览结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResult {
    private List<String> columns;
    private List<List<Object>> rows;
    private int rowCount;
}
