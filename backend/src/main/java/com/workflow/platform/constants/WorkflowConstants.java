package com.workflow.platform.constants;
//工作流常量

public class WorkflowConstants {

    // 工作流类型
    public static final String WORKFLOW_TYPE_BUSINESS = "business";
    public static final String WORKFLOW_TYPE_TECHNICAL = "technical";
    public static final String WORKFLOW_TYPE_DATA = "data";
    public static final String WORKFLOW_TYPE_INTEGRATION = "integration";

    // 节点属性
    public static final String NODE_PROP_REQUIRED = "required";
    public static final String NODE_PROP_OPTIONAL = "optional";
    public static final String NODE_PROP_CONDITIONAL = "conditional";

    // 连接器类型
    public static final String CONNECTOR_TYPE_SEQUENCE = "sequence";
    public static final String CONNECTOR_TYPE_PARALLEL = "parallel";
    public static final String CONNECTOR_TYPE_CONDITION = "condition";
    public static final String CONNECTOR_TYPE_ERROR = "error";

    // 执行策略
    public static final String EXECUTION_SYNC = "sync";
    public static final String EXECUTION_ASYNC = "async";
    public static final String EXECUTION_BATCH = "batch";

    // 验证级别
    public static final String VALIDATION_LEVEL_WARNING = "warning";
    public static final String VALIDATION_LEVEL_ERROR = "error";
    public static final String VALIDATION_LEVEL_INFO = "info";

    // 数据格式
    public static final String DATA_FORMAT_JSON = "json";
    public static final String DATA_FORMAT_XML = "xml";
    public static final String DATA_FORMAT_YAML = "yaml";
    public static final String DATA_FORMAT_CSV = "csv";

    // 模板路径
    public static final String TEMPLATE_WORKFLOW = "templates/workflow-template.json";
    public static final String TEMPLATE_NODE = "templates/node-template.json";
    public static final String TEMPLATE_VALIDATION = "templates/validation-template.json";

    // 最大限制
    public static final int MAX_NODES_PER_WORKFLOW = 100;
    public static final int MAX_CONNECTORS_PER_NODE = 10;
    public static final int MAX_EXECUTION_HISTORY = 1000;

    private WorkflowConstants() {
        throw new IllegalStateException("工作流常量类");
    }
}