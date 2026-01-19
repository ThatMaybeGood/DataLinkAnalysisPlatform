package com.workflow.platform.model.entity;

import java.time.LocalDateTime;
import java.util.Map;

import com.workflow.platform.util.JsonAttributeConverterUtil;
import com.workflow.platform.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * @author Mine
 * @version 1.0
 * 描述: 工作流执行记录实体
 * @date 2026/1/16 00:40
 */
@Data
@Builder
@NoArgsConstructor    // 必须：JPA 实例化实体需要无参构造函数
@AllArgsConstructor   // 必须：使 @Builder 生成的构造函数变为 public
@Entity               // 必须：告诉 JPA 这是一个数据库映射实体
@Table(name = "executions") // 建议：明确指定数据库表名
public class ExecutionEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // 建议：主键自增
	private Long id;

//	@Column(name = "workflow_id")
//	private String workflowId;

	// 关键点 1：添加这个多对一关联
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "workflow_id") // 映射到数据库中的字段名
	private WorkflowEntity workflow;   // 变量名必须叫 workflow，对应 WorkflowEntity 里的 mappedBy

	private String status;

	@Lob // 如果数据较大，建议标记为大对象
	@Convert(converter = JsonAttributeConverterUtil.class) // 如果数据库不支持直接存Object，需转换
	private Object inputData;

	@Transient // 如果该字段不需要存入数据库，加此注解；如果要存，需配置 Map 转换器
	private Map<String, Object> outputData;

	private String errorMessage;

	private LocalDateTime createTime;

	private LocalDateTime startTime;

	private LocalDateTime endTime;

	// 可以在持久化前自动设置时间
	@PrePersist
	protected void onCreate() {
		createTime = LocalDateTime.now();
	}
}