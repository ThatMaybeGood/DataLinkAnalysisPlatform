-- 工作流平台数据库初始化脚本
-- 在线模式使用

-- 工作流表
CREATE TABLE IF NOT EXISTS workflows (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '工作流名称',
    alias VARCHAR(100) UNIQUE COMMENT '工作流别名',
    description TEXT COMMENT '描述',
    category VARCHAR(100) COMMENT '分类',
    tags JSON COMMENT '标签',

    -- 配置
    config JSON COMMENT '工作流配置',
    status VARCHAR(20) DEFAULT 'draft' COMMENT '状态: draft, active, inactive, archived',
    version VARCHAR(20) DEFAULT '1.0' COMMENT '版本',

    -- 统计信息
    node_count INT DEFAULT 0 COMMENT '节点数量',
    execution_count INT DEFAULT 0 COMMENT '执行次数',
    success_rate DECIMAL(5,2) DEFAULT 0 COMMENT '成功率',
    avg_duration INT DEFAULT 0 COMMENT '平均耗时(ms)',

    -- 模式信息
    mode VARCHAR(20) DEFAULT 'online' COMMENT '模式: online, offline',

    -- 权限控制
    tenant_id VARCHAR(50) COMMENT '租户ID',
    created_by VARCHAR(100) COMMENT '创建人',
    updated_by VARCHAR(100) COMMENT '更新人',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_name (name),
    CREATE INDEX IF NOT EXISTS idx_alias (alias),
    CREATE INDEX IF NOT EXISTS idx_category (category),
    CREATE INDEX IF NOT EXISTS idx_status (status),
    CREATE INDEX IF NOT EXISTS idx_tenant (tenant_id),
    CREATE INDEX IF NOT EXISTS idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工作流表';

-- 先删除可能存在的索引
DROP CREATE INDEX IF NOT EXISTS IF EXISTS IDX_CREATED_AT;
DROP CREATE INDEX IF NOT EXISTS IF EXISTS IDX_WORKFLOW_ID;
DROP CREATE INDEX IF NOT EXISTS IF EXISTS IDX_TYPE;
-- 节点表
CREATE TABLE IF NOT EXISTS nodes (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    workflow_id VARCHAR(50) NOT NULL COMMENT '工作流ID',
    name VARCHAR(200) NOT NULL COMMENT '节点名称',
    type VARCHAR(50) NOT NULL COMMENT '节点类型: start, end, validation, database, api, script, etc',
    description TEXT COMMENT '节点描述',

    -- 位置信息
    position_x INT COMMENT 'X坐标',
    position_y INT COMMENT 'Y坐标',

    -- 配置
    config JSON COMMENT '节点配置',
    connector_id VARCHAR(50) COMMENT '连接器ID',

    -- 执行信息
    timeout_seconds INT DEFAULT 30 COMMENT '超时时间(秒)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    retry_interval INT DEFAULT 5 COMMENT '重试间隔(秒)',

    -- 关联信息
    next_nodes JSON COMMENT '下一个节点ID数组',
    prev_nodes JSON COMMENT '上一个节点ID数组',

    -- 统计信息
    execution_count INT DEFAULT 0 COMMENT '执行次数',
    success_count INT DEFAULT 0 COMMENT '成功次数',
    avg_duration INT DEFAULT 0 COMMENT '平均耗时(ms)',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_workflow_id (workflow_id),
    CREATE INDEX IF NOT EXISTS idx_type (type),
    CREATE INDEX IF NOT EXISTS idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点表';


-- 验证规则表
CREATE TABLE IF NOT EXISTS validation_rules (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    node_id VARCHAR(50) NOT NULL COMMENT '节点ID',
    name VARCHAR(200) NOT NULL COMMENT '规则名称',
    type VARCHAR(50) NOT NULL COMMENT '规则类型: sql, javascript, regex, range, etc',
    description TEXT COMMENT '规则描述',

    -- 规则配置
    rule_content TEXT NOT NULL COMMENT '规则内容',
    expected_type VARCHAR(50) COMMENT '期望值类型: exact, regex, range, etc',
    expected_value TEXT COMMENT '期望值',

    -- 错误处理
    error_message VARCHAR(500) COMMENT '错误信息',
    error_code VARCHAR(50) COMMENT '错误代码',

    -- 执行配置
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    order_num INT DEFAULT 0 COMMENT '执行顺序',

    -- 统计信息
    execution_count INT DEFAULT 0 COMMENT '执行次数',
    success_count INT DEFAULT 0 COMMENT '成功次数',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    FOREIGN KEY (node_id) REFERENCES nodes(id) ON DELETE CASCADE,

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_node_id (node_id),
    CREATE INDEX IF NOT EXISTS idx_type (type),
    CREATE INDEX IF NOT EXISTS idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='验证规则表';

-- 连接器表
CREATE TABLE IF NOT EXISTS connectors (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '连接器名称',
    type VARCHAR(50) NOT NULL COMMENT '连接器类型: mysql, postgresql, oracle, http, kafka, etc',
    description TEXT COMMENT '连接器描述',

    -- 连接配置
    config JSON COMMENT '连接配置',

    -- 状态信息
    status VARCHAR(20) DEFAULT 'inactive' COMMENT '状态: active, inactive, error',
    last_test_time DATETIME COMMENT '最后测试时间',
    last_test_result JSON COMMENT '最后测试结果',

    -- 统计信息
    connection_count INT DEFAULT 0 COMMENT '连接次数',
    success_count INT DEFAULT 0 COMMENT '成功次数',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_type (type),
    CREATE INDEX IF NOT EXISTS idx_status (status),
    CREATE INDEX IF NOT EXISTS idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='连接器表';

-- 执行记录表
CREATE TABLE IF NOT EXISTS executions (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    workflow_id VARCHAR(50) NOT NULL COMMENT '工作流ID',
    workflow_name VARCHAR(200) COMMENT '工作流名称',

    -- 执行信息
    status VARCHAR(20) NOT NULL COMMENT '状态: pending, running, completed, failed, cancelled',
    trigger_type VARCHAR(50) COMMENT '触发类型: manual, api, schedule, webhook',
    trigger_user VARCHAR(100) COMMENT '触发用户',

    -- 输入输出
    input_data JSON COMMENT '输入数据',
    output_data JSON COMMENT '输出数据',
    error_message TEXT COMMENT '错误信息',
    error_stack TEXT COMMENT '错误堆栈',

    -- 性能指标
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms INT COMMENT '持续时间(ms)',

    -- 环境信息
    mode VARCHAR(20) DEFAULT 'online' COMMENT '执行模式: online, offline',
    host_name VARCHAR(100) COMMENT '主机名',
    ip_address VARCHAR(50) COMMENT 'IP地址',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE,

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_workflow_id (workflow_id),
    CREATE INDEX IF NOT EXISTS idx_status (status),
    CREATE INDEX IF NOT EXISTS idx_trigger_type (trigger_type),
    CREATE INDEX IF NOT EXISTS idx_created_at (created_at),
    CREATE INDEX IF NOT EXISTS idx_duration (duration_ms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行记录表';

-- 节点执行详情表
CREATE TABLE IF NOT EXISTS node_executions (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    execution_id VARCHAR(50) NOT NULL COMMENT '执行记录ID',
    node_id VARCHAR(50) NOT NULL COMMENT '节点ID',
    node_name VARCHAR(200) COMMENT '节点名称',
    node_type VARCHAR(50) COMMENT '节点类型',

    -- 执行信息
    status VARCHAR(20) NOT NULL COMMENT '状态: pending, running, completed, failed, skipped',
    order_num INT COMMENT '执行顺序',

    -- 输入输出
    input_data JSON COMMENT '输入数据',
    output_data JSON COMMENT '输出数据',
    error_message TEXT COMMENT '错误信息',

    -- 性能指标
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms INT COMMENT '持续时间(ms)',

    -- 验证结果
    validation_results JSON COMMENT '验证结果',
    data_snapshots JSON COMMENT '数据快照',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 外键约束
    FOREIGN KEY (execution_id) REFERENCES executions(id) ON DELETE CASCADE,

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_execution_id (execution_id),
    CREATE INDEX IF NOT EXISTS idx_node_id (node_id),
    CREATE INDEX IF NOT EXISTS idx_status (status),
    CREATE INDEX IF NOT EXISTS idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='节点执行详情表';

-- 数据快照表
CREATE TABLE IF NOT EXISTS data_snapshots (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    execution_id VARCHAR(50) NOT NULL COMMENT '执行记录ID',
    node_id VARCHAR(50) NOT NULL COMMENT '节点ID',
    snapshot_type VARCHAR(20) COMMENT '快照类型: before, after',

    -- 数据信息
    table_name VARCHAR(100) COMMENT '表名',
    operation VARCHAR(20) COMMENT '操作: select, insert, update, delete',

    -- 快照数据
    before_data JSON COMMENT '变更前数据',
    after_data JSON COMMENT '变更后数据',
    diff_data JSON COMMENT '差异数据',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 外键约束
    FOREIGN KEY (execution_id) REFERENCES executions(id) ON DELETE CASCADE,

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_execution_id (execution_id),
    CREATE INDEX IF NOT EXISTS idx_node_id (node_id),
    CREATE INDEX IF NOT EXISTS idx_snapshot_type (snapshot_type),
    CREATE INDEX IF NOT EXISTS idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据快照表';

-- 用户表（权限管理）
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    username VARCHAR(100) UNIQUE NOT NULL COMMENT '用户名',
    email VARCHAR(200) UNIQUE NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',

    -- 认证信息
    password VARCHAR(200) NOT NULL COMMENT '密码',
    salt VARCHAR(50) COMMENT '盐值',

    -- 个人信息
    real_name VARCHAR(100) COMMENT '真实姓名',
    avatar_url VARCHAR(500) COMMENT '头像URL',

    -- 状态信息
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active, inactive, locked',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_username (username),
    CREATE INDEX IF NOT EXISTS idx_email (email),
    CREATE INDEX IF NOT EXISTS idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS roles (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) UNIQUE NOT NULL COMMENT '角色名称',
    code VARCHAR(50) UNIQUE NOT NULL COMMENT '角色代码',
    description TEXT COMMENT '角色描述',

    -- 权限
    permissions JSON COMMENT '权限列表',

    -- 状态
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS user_roles (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    role_id VARCHAR(50) NOT NULL COMMENT '角色ID',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE,

    -- 唯一约束
    UNIQUE KEY uk_user_role (user_id, role_id),

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_user_id (user_id),
    CREATE INDEX IF NOT EXISTS idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 租户表（多租户支持）
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '租户名称',
    code VARCHAR(100) UNIQUE NOT NULL COMMENT '租户代码',

    -- 配置信息
    config JSON COMMENT '租户配置',

    -- 状态信息
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active, inactive, suspended',
    expiration_time DATETIME COMMENT '过期时间',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_code (code),
    CREATE INDEX IF NOT EXISTS idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(100) UNIQUE NOT NULL COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(50) DEFAULT 'string' COMMENT '配置类型: string, json, number, boolean',

    -- 描述信息
    description TEXT COMMENT '配置描述',
    category VARCHAR(100) COMMENT '配置分类',

    -- 权限控制
    editable BOOLEAN DEFAULT TRUE COMMENT '是否可编辑',
    visible BOOLEAN DEFAULT TRUE COMMENT '是否可见',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_config_key (config_key),
    CREATE INDEX IF NOT EXISTS idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id VARCHAR(50) PRIMARY KEY COMMENT '主键ID',

    -- 操作信息
    operation_type VARCHAR(100) NOT NULL COMMENT '操作类型',
    operation_target VARCHAR(200) COMMENT '操作目标',
    operation_detail TEXT COMMENT '操作详情',

    -- 用户信息
    user_id VARCHAR(50) COMMENT '用户ID',
    username VARCHAR(100) COMMENT '用户名',
    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',

    -- 结果信息
    success BOOLEAN DEFAULT TRUE COMMENT '是否成功',
    error_message TEXT COMMENT '错误信息',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 索引
    CREATE INDEX IF NOT EXISTS idx_operation_type (operation_type),
    CREATE INDEX IF NOT EXISTS idx_user_id (user_id),
    CREATE INDEX IF NOT EXISTS idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 初始化数据
INSERT INTO system_configs (id, config_key, config_value, config_type, description, category) VALUES
('1', 'app.mode', 'online', 'string', '应用模式: online/offline', 'system'),
('2', 'app.name', 'Workflow Platform', 'string', '应用名称', 'system'),
('3', 'app.version', '1.0.0', 'string', '应用版本', 'system'),
('4', 'storage.type', 'database', 'string', '存储类型: database/file', 'storage'),
('5', 'export.format', 'json', 'string', '导出格式: json/xml/csv', 'export');

-- 初始化管理员用户
INSERT INTO users (id, username, email, password, real_name, status) VALUES
('admin', 'admin', 'admin@workflow.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKryQ6Gq3k6b4j6hW.YVWwV8QcK2', '系统管理员', 'active');

-- 初始化角色
INSERT INTO roles (id, name, code, description, permissions) VALUES
('admin_role', '系统管理员', 'ADMIN', '系统管理员角色', '["*:*:*"]'),
('user_role', '普通用户', 'USER', '普通用户角色', '["workflow:read:*", "workflow:create:*", "workflow:update:own", "workflow:delete:own"]');

-- 分配角色
INSERT INTO user_roles (id, user_id, role_id) VALUES
('1', 'admin', 'admin_role');