-- ============================================================
-- V2__seed.sql · 初始基础数据（MySQL 与 H2 双库均兼容，放置于 db/migration/common）
-- 内容：角色 / 管理员账号 / 最小示例路网（订单支付流程）
-- 管理员默认密码：admin123（首次登录后请修改）
-- ============================================================

-- 角色（RBAC）
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
  (1, 'ADMIN',    '管理员',   '系统管理：用户/角色/授权、变更审批、版本回滚、告警渠道配置'),
  (2, 'MODELER',  '建模师',   '流程/站点/路网/路线/检测点编辑、数据接入管理、检测点配置'),
  (3, 'OPERATOR', '运维监控', '查看、实例追踪、标记实例、发起排查、处理告警/工单'),
  (4, 'ONCALL',   '值班人',   '看板、接收告警、处理工单'),
  (5, 'VIEWER',   '管理层',   '只读查看流程/路线/报表/大屏');

-- 管理员账号（BCrypt，密码 admin123）
INSERT INTO sys_user (id, username, password_hash, display_name, email, status, created_at) VALUES
  (1, 'admin', '$2b$10$uqXUhCUqpPsUWqpKrNvbeOqcvX6Wds8xr71hl73aWRg02m/zTJvlW', '管理员', 'admin@datalink.local', 1, '2026-08-16 00:00:00');

-- 用户-角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (1, 2), (1, 3);

-- 最小示例路网：订单支付流程（供关系网画布演示与 M1 开发使用）

-- 站点节点
INSERT INTO node (id, node_type, name, code, level, status, owner, description, created_at, updated_at) VALUES
  (1, 'SYSTEM',      '订单门户',   'ORDER_PORTAL',  'L2', 'ACTIVE', '张三', '客户下单入口',        '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (2, 'SUBSYSTEM',   '订单服务',   'ORDER_SERVICE', 'L2', 'ACTIVE', '李四', '订单业务逻辑',        '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (3, 'DATABASE',    '订单数据库', 'ORDER_DB',      'L2', 'ACTIVE', '王五', '订单主库',            '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (4, 'TABLE',       '订单表',     'T_ORDER',       'L3', 'ACTIVE', '王五', '订单明细',            '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (5, 'SUBSYSTEM',   '支付服务',   'PAY_SERVICE',   'L1', 'ACTIVE', '赵六', '支付与对账',          '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (6, 'DATABASE',    '支付数据库', 'PAY_DB',        'L1', 'ACTIVE', '钱七', '支付主库',            '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (7, 'TABLE',       '支付流水表', 'T_PAY',         'L1', 'ACTIVE', '钱七', '支付流水',            '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (8, 'DEPARTMENT',  '结算部门',   'FINANCE_DEPT',  'L3', 'ACTIVE', '孙八', '结算对账',            '2026-08-16 00:00:00', '2026-08-16 00:00:00');

-- 路网边（有向）
INSERT INTO relation (id, from_node_id, to_node_id, relation_type, level, description, status, created_at, updated_at) VALUES
  (1, 1, 2, 'API',       'L2', '门户调用订单服务', 'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (2, 2, 3, 'DATA_FLOW', 'L2', '订单服务写主库',   'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (3, 3, 4, 'DATA_FLOW', 'L3', '库内表',           'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (4, 2, 5, 'API',       'L1', '订单服务调用支付', 'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (5, 5, 6, 'DATA_FLOW', 'L1', '支付服务写主库',   'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (6, 6, 7, 'DATA_FLOW', 'L1', '库内表',           'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (7, 5, 8, 'APPROVAL',  'L3', '支付结果送结算',   'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00');

-- 流程
INSERT INTO process (id, name, code, scene, start_node_id, end_node_id, level, description, status, created_at, updated_at) VALUES
  (1, '订单支付流程', 'ORDER_PAY', 'BUSINESS', 1, 8, 'L2', '从下单到结算的完整流程', 'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00');

-- 路线
INSERT INTO route (id, process_id, name, priority, level, description, status, created_at, updated_at) VALUES
  (1, 1, '默认路线', 'DEFAULT',   'L2', '走订单表明细', 'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00'),
  (2, 1, '快捷路线', 'ALTERNATE', 'L2', '跳过订单表',   'ACTIVE', '2026-08-16 00:00:00', '2026-08-16 00:00:00');

-- 路线站点（有序，含标准环节时长 SLA）
INSERT INTO route_node (route_id, seq, node_id, expected_duration_ms) VALUES
  (1, 1, 1, 1000), (1, 2, 2, 2000), (1, 3, 3, 500), (1, 4, 4, 300),
  (1, 5, 5, 3000), (1, 6, 6, 500),  (1, 7, 7, 300), (1, 8, 8, 1000),
  (2, 1, 1, 1000), (2, 2, 2, 2000), (2, 3, 5, 3000), (2, 4, 6, 500),
  (2, 5, 7, 300),  (2, 6, 8, 1000);

-- 别名示例（全局搜索通吃）
INSERT INTO alias (id, target_type, target_id, name, is_primary, created_at) VALUES
  (1, 'NODE', 4, '订单明细表', 0, '2026-08-16 00:00:00'),
  (2, 'NODE', 7, '收银流水',   0, '2026-08-16 00:00:00');
