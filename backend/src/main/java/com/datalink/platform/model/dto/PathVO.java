package com.datalink.platform.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图路径视图对象：一条简单路径的有序站点序列。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathVO {
    /** 有序站点节点 id（String） */
    private List<String> nodeIds;
    /** 有序站点节点名（与 nodeIds 对齐） */
    private List<String> nodeNames;
    /** 路径长度（节点数） */
    private int length;
}
