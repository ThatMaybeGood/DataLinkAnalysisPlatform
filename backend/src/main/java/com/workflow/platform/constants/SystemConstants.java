package com.workflow.platform.constants;
//系统常量

public class SystemConstants {

    // 系统配置
    public static final String APP_NAME = "Workflow Visualization Platform";
    public static final String VERSION = "1.0.0";
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    // 文件路径常量
    public static final String OFFLINE_DATA_DIR = "data/offline";
    public static final String WORKFLOW_DIR = "/workflows";
    public static final String NODE_DIR = "/nodes";
    public static final String RULE_DIR = "/rules";
    public static final String EXPORT_DIR = "/exports";
    public static final String BACKUP_DIR = "/backups";

    // 文件扩展名
    public static final String JSON_EXT = ".json";
    public static final String XML_EXT = ".xml";
    public static final String YML_EXT = ".yml";
    public static final String BAK_EXT = ".bak";

    // 缓存常量
    public static final String CACHE_WORKFLOWS = "workflows";
    public static final String CACHE_NODES = "nodes";
    public static final String CACHE_RULES = "rules";
    public static final int CACHE_TTL_SECONDS = 3600;

    // 分页常量
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // 验证常量
    public static final int MAX_WORKFLOW_NAME_LENGTH = 100;
    public static final int MAX_NODE_NAME_LENGTH = 50;
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    // 时间常量
    public static final long DEFAULT_TIMEOUT_MS = 30000L;
    public static final long MAX_TIMEOUT_MS = 300000L;

    // 编码常量
    public static final String DEFAULT_CHARSET = "UTF-8";
    public static final String JSON_CONTENT_TYPE = "application/json";

    // 密钥配置（实际项目中应从配置中心获取）
    public static final String ENCRYPTION_KEY = "workflow-platform-2024";
    public static final String JWT_SECRET = "workflow-jwt-secret-2024";

    // 模式配置
    public static final String MODE_ONLINE = "online";
    public static final String MODE_OFFLINE = "offline";
    public static final String MODE_MIXED = "mixed";

    private SystemConstants() {
        throw new IllegalStateException("常量工具类");
    }
}