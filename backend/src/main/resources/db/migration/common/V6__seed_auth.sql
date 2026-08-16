-- ============================================================
-- V6__seed_auth.sql · RBAC 只读账号（MySQL 与 H2 均兼容）
-- 内容：只读用户 viewer（仅 VIEWER 角色），用于 403 越权测试与只读演示
-- 默认密码：viewer123
-- ============================================================

INSERT INTO sys_user (id, username, password_hash, display_name, email, status, created_at) VALUES
  (2, 'viewer', '$2b$10$7I6f.4czwBxep2XGaMpiduUjmuGgE8p2oIRv25KzZJciaoZOfQiL.', '只读用户', 'viewer@datalink.local', 1, '2026-08-16 00:00:00');
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 5);  -- 5=VIEWER
