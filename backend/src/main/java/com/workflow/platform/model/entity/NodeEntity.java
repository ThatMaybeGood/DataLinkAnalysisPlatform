package com.workflow.platform.model.entity;

/**
 * @author Mine
 * @version 1.0
 *          描述:
 * @date 2026/1/16 00:36
 */

import javax.persistence.*;
import lombok.*;

@Data
@Entity  // <--- 必须有这个注解
@Table(name = "workflow_nodes") // 建议指定表名
public class NodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nodeName;
    private String nodeType;

    // 如果是双向关联，还需要加这个（可选，取决于你的业务）
    @ManyToOne
    @JoinColumn(name = "workflow_id")
    private WorkflowEntity workflow;
}