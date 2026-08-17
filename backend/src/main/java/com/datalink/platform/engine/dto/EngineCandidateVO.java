package com.datalink.platform.engine.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 引擎识别出的候选单据 VO（与前端 G2 草稿页 DraftCandidate 契约对齐）
 */
@Data
public class EngineCandidateVO {
    /** 显示名（优先取表注释，如「挂号单」） */
    private String name;
    /** 表名（如 reg_order） */
    private String table;
    /** 置信度 0~100（设计 15.3 主表识别区间 60~85） */
    private int confidence;
    /** 命中信号标签（主键/单号/状态/时间/引用/被引用/主子表/单号格式） */
    private List<String> marks = new ArrayList<>();
    /** 低置信标记（&lt;70） */
    private boolean low;
    /** 采样的状态字段值（状态机初步素材，无状态为空） */
    private List<String> statusValues = new ArrayList<>();
}