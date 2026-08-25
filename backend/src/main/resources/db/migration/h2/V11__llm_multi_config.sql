-- ============================================================
-- V11__llm_multi_config.sql · H2
-- 大模型接入支持多配置 + 切换启用（cc-switch 式）
-- ============================================================

ALTER TABLE llm_config ADD COLUMN name VARCHAR(100);
ALTER TABLE llm_config ADD COLUMN is_active TINYINT DEFAULT 0;
UPDATE llm_config SET name = '默认配置', is_active = 1 WHERE config_key = 'default';
DROP INDEX IF EXISTS uk_llm_config_key;
