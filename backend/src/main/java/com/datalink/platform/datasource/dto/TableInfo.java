package com.datalink.platform.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 库表信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {
    private String name;
    private String type;
}
