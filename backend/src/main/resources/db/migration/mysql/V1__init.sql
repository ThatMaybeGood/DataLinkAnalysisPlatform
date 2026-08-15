-- ============================================================
-- V1__init.sql · MySQL 8 建表迁移（Flyway）
-- 来源：《数据关联与业务流程监控分析平台-项目文档书-v1.0》附录 A：数据模型 DDL
-- 内容：21 张表（建模域 / 监控域 / 配置域 / 接入域 / 系统域）
-- 说明：与附录 A 原样一致，未做任何结构变更
-- ============================================================

-- ==================== 建模域 ====================

-- 站点节点（系统/库表/部门岗位/业务动作/制造设备等）
CREATE TABLE node (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  node_type     VARCHAR(20)  NOT NULL,  -- SYSTEM/SUBSYSTEM/DATABASE/TABLE/DEPARTMENT/ROLE/ACTION/EVENT/DEVICE/WORKSTATION
  name          VARCHAR(100) NOT NULL,  -- 显示名（主别名）
  code          VARCHAR(50),            -- 唯一编码
  sub_type      VARCHAR(50),            -- 库类型 MYSQL/ORACLE/REDIS... 或系统类别
  ip            VARCHAR(100),
  level         VARCHAR(4)   DEFAULT 'L3',  -- L1核心/L2重要/L3普通/L4一般
  status        VARCHAR(20)  DEFAULT 'ACTIVE',
  owner         VARCHAR(50),            -- 责任人
  description   VARCHAR(500),
  ext           JSON,
  created_at    DATETIME     NOT NULL,
  updated_at    DATETIME     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_node_code (code),
  KEY idx_node_type (node_type),
  KEY idx_node_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 路网边（站点间有向连接，支持分岔/汇合）
CREATE TABLE relation (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  from_node_id  BIGINT       NOT NULL,
  to_node_id    BIGINT       NOT NULL,
  relation_type VARCHAR(30),            -- DATA_FLOW/API/SUBSCRIBE/APPROVAL...
  level         VARCHAR(4)   DEFAULT 'L3',
  description   VARCHAR(500),
  status        VARCHAR(20)  DEFAULT 'ACTIVE',
  created_at    DATETIME     NOT NULL,
  updated_at    DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_relation_from (from_node_id),
  KEY idx_relation_to (to_node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 流程（起点→终点）
CREATE TABLE process (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  name           VARCHAR(100) NOT NULL,
  code           VARCHAR(50),
  scene          VARCHAR(20)  DEFAULT 'BUSINESS',  -- DATA/BUSINESS/MANUFACTURING
  start_node_id  BIGINT,
  end_node_id    BIGINT,
  level          VARCHAR(4)   DEFAULT 'L3',
  description    VARCHAR(500),
  status         VARCHAR(20)  DEFAULT 'ACTIVE',
  created_at     DATETIME     NOT NULL,
  updated_at     DATETIME     NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_process_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 路线（从路网定义的一条完整路径）
CREATE TABLE route (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  process_id  BIGINT       NOT NULL,
  name        VARCHAR(100),
  priority    VARCHAR(20)  DEFAULT 'ALTERNATE',  -- DEFAULT/RECOMMENDED/ALTERNATE
  level       VARCHAR(4)   DEFAULT 'L3',
  description VARCHAR(500),
  status      VARCHAR(20)  DEFAULT 'ACTIVE',
  created_at  DATETIME     NOT NULL,
  updated_at  DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_route_process (process_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 路线站点（有序，含该环节标准时长 SLA，卡点判定依据）
CREATE TABLE route_node (
  route_id     BIGINT NOT NULL,
  seq          INT    NOT NULL,
  node_id      BIGINT NOT NULL,
  expected_duration_ms BIGINT,          -- SLA：该环节标准时长
  PRIMARY KEY (route_id, seq),
  KEY idx_route_node_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 检测点（每个站点可挂多个）
CREATE TABLE checkpoint (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  node_id    BIGINT       NOT NULL,
  name       VARCHAR(100),
  kind       VARCHAR(20)  DEFAULT 'CUSTOM',  -- DEFAULT 系统默认 / CUSTOM 自定义
  check_type VARCHAR(30),   -- DATA_VOLUME/FRESHNESS/DELAY/INTEGRITY/SERVICE_STATUS/SQL/SCRIPT/THRESHOLD
  params     JSON,          -- 规则参数（阈值/SQL/频率等）
  freq       VARCHAR(20)  DEFAULT '5m',
  level      VARCHAR(4)   DEFAULT 'L3',
  enabled    TINYINT      DEFAULT 1,
  created_at DATETIME     NOT NULL,
  updated_at DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_checkpoint_node (node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 检测结果（留痕）
CREATE TABLE check_result (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  checkpoint_id BIGINT      NOT NULL,
  instance_id   BIGINT,
  check_time    DATETIME    NOT NULL,
  status        VARCHAR(20),            -- PASS/FAIL/TIMEOUT/SKIP
  value         JSON,
  message       VARCHAR(500),
  PRIMARY KEY (id),
  KEY idx_result_ckpt (checkpoint_id),
  KEY idx_result_time (check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 监控域 ====================

-- 流程实例（一次真实运行）
CREATE TABLE instance (
  id               BIGINT      NOT NULL AUTO_INCREMENT,
  process_id       BIGINT,
  route_id         BIGINT,
  biz_no           VARCHAR(50),         -- 业务单号/流水号
  biz_name         VARCHAR(100),        -- 业务别名（显示名）
  start_time       DATETIME,
  end_time         DATETIME,
  status           VARCHAR(20),         -- RUNNING/SUCCESS/FAIL/STUCK/TIMEOUT
  current_node_id  BIGINT,
  total_duration_ms BIGINT,
  source           VARCHAR(20)  DEFAULT 'MANUAL',  -- INFER特征推断/MANUAL人工/REPORT进度上报/API
  created_at       DATETIME    NOT NULL,
  updated_at       DATETIME    NOT NULL,
  PRIMARY KEY (id),
  KEY idx_instance_bizno (biz_no),
  KEY idx_instance_status (status),
  KEY idx_instance_route (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 实例经过的站点（每环节耗时/状态）
CREATE TABLE instance_node (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  instance_id   BIGINT      NOT NULL,
  node_id       BIGINT      NOT NULL,
  seq           INT,
  arrive_time   DATETIME,
  leave_time    DATETIME,
  duration_ms   BIGINT,
  status        VARCHAR(20),
  message       VARCHAR(500),
  PRIMARY KEY (id),
  KEY idx_inst_node_instance (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 告警
CREATE TABLE alert (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  alert_type    VARCHAR(30),            -- STUCK/FAIL/TIMEOUT/CHECK_FAIL
  target_type   VARCHAR(20),            -- NODE/INSTANCE/ROUTE/PROCESS
  target_id     BIGINT,
  message       VARCHAR(500),
  severity      VARCHAR(10),
  disposition   VARCHAR(50),            -- NOTIFY/TICKET/AUTO_ACTION（逗号组合）
  status        VARCHAR(20)  DEFAULT 'OPEN',
  created_at    DATETIME    NOT NULL,
  resolved_at   DATETIME,
  PRIMARY KEY (id),
  KEY idx_alert_status (status),
  KEY idx_alert_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 工单
CREATE TABLE ticket (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  alert_id      BIGINT      NOT NULL,
  assignee      VARCHAR(50),
  priority      VARCHAR(10),
  status        VARCHAR(20) DEFAULT 'OPEN',  -- OPEN/PROCESSING/RESOLVED
  description   VARCHAR(500),
  created_at    DATETIME   NOT NULL,
  resolved_at   DATETIME,
  PRIMARY KEY (id),
  KEY idx_ticket_alert (alert_id),
  KEY idx_ticket_assignee (assignee)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 配置域 ====================

-- 配置版本（全配置对象留痕）
CREATE TABLE config_version (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  target_type   VARCHAR(30) NOT NULL,   -- PROCESS/NODE/ROUTE/CHECKPOINT/CONNECTOR...
  target_id     BIGINT      NOT NULL,
  version       INT         NOT NULL,
  content       JSON,                   -- 该版本完整配置快照
  change_note   VARCHAR(500),
  operator      VARCHAR(50),
  status        VARCHAR(20),            -- DRAFT/PENDING_APPROVAL/PUBLISHED/ROLLED_BACK
  created_at    DATETIME   NOT NULL,
  -- 修正：源文档附录 A 遗漏主键，MySQL 要求自增列必须作为键，否则报错 1075
  PRIMARY KEY (id),
  UNIQUE KEY uk_version (target_type, target_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 命名别名（通用，任意对象可加）
CREATE TABLE alias (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  target_type   VARCHAR(30)  NOT NULL,  -- PROCESS/ROUTE/INSTANCE/NODE/VIEW/CHECKPOINT
  target_id     BIGINT       NOT NULL,
  name          VARCHAR(100) NOT NULL,
  is_primary    TINYINT      DEFAULT 0, -- 是否主显示名
  created_at    DATETIME     NOT NULL,
  PRIMARY KEY (id),
  KEY idx_alias_target (target_type, target_id),
  KEY idx_alias_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 接入域 ====================

-- 连接器（数据源接入，可插拔）
CREATE TABLE connector (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  connector_type VARCHAR(20),           -- DB/EXCEL/CMDB/API/LOG/IOT
  name          VARCHAR(100),
  db_type       VARCHAR(20),            -- MYSQL/ORACLE/PG...
  host          VARCHAR(100),
  port          INT,
  username      VARCHAR(100),
  encrypted_pwd VARCHAR(255),           -- AES 加密存储
  config        JSON,
  enabled       TINYINT      DEFAULT 1,
  created_at    DATETIME     NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 采集任务
CREATE TABLE import_job (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  connector_id  BIGINT,
  job_type      VARCHAR(20),            -- MANUAL/SCHEDULED
  cron          VARCHAR(50),
  status        VARCHAR(20),            -- RUNNING/SUCCESS/FAIL
  summary       VARCHAR(500),
  start_time    DATETIME,
  end_time      DATETIME,
  PRIMARY KEY (id),
  KEY idx_job_connector (connector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 采集日志
CREATE TABLE import_log (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  job_id        BIGINT,
  level         VARCHAR(10),
  message       VARCHAR(500),
  created_at    DATETIME   NOT NULL,
  PRIMARY KEY (id),
  KEY idx_log_job (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==================== 系统域（RBAC / 审计） ====================

CREATE TABLE sys_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL,
  password_hash VARCHAR(255) NOT NULL,  -- BCrypt
  display_name  VARCHAR(100),
  email         VARCHAR(100),
  status        TINYINT      DEFAULT 1,
  created_at    DATETIME    NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_role (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  role_code     VARCHAR(30) NOT NULL,   -- ADMIN/MODELER/OPERATOR/ONCALL/VIEWER
  role_name     VARCHAR(50),
  description   VARCHAR(200),
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE sys_user_role (
  user_id       BIGINT NOT NULL,
  role_id       BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 流程/关系网级授权（数据权限）
CREATE TABLE sys_grant (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  role_id       BIGINT      NOT NULL,
  target_type   VARCHAR(20) NOT NULL,   -- PROCESS/VIEW
  target_id     BIGINT      NOT NULL,
  PRIMARY KEY (id),
  KEY idx_grant_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 操作审计
CREATE TABLE operation_log (
  id            BIGINT      NOT NULL AUTO_INCREMENT,
  operator      VARCHAR(50),
  action        VARCHAR(50),            -- CREATE/UPDATE/DELETE/ROLLBACK/APPROVE...
  target_type   VARCHAR(30),
  target_id     BIGINT,
  detail        JSON,
  ip            VARCHAR(50),
  created_at    DATETIME   NOT NULL,
  PRIMARY KEY (id),
  KEY idx_oper_time (created_at),
  KEY idx_oper_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
