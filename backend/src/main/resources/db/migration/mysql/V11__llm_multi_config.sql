-- ============================================================
-- V11__llm_multi_config.sql · MySQL 8
-- 大模型接入支持多配置 + 切换启用（cc-switch 式）
-- ============================================================

ALTER TABLE llm_config ADD COLUMN name VARCHAR(100) COMMENT '配置名';
ALTER TABLE llm_config ADD COLUMN is_active TINYINT DEFAULT 0 COMMENT '是否当前启用（全局唯一 1）';
UPDATE llm_config SET name = '默认配置', is_active = 1 WHERE config_key = 'default';
ALTER TABLE llm_config DROP INDEX uk_llm_config_key;
