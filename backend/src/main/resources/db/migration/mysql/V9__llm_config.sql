-- ============================================================
-- V9__llm_config.sql · MySQL 8
-- G4 大模型接入配置（单行，可界面配置；DB 值覆盖环境变量默认值）
-- ============================================================

CREATE TABLE llm_config (
  id                 BIGINT       NOT NULL AUTO_INCREMENT,
  config_key         VARCHAR(20)  NOT NULL DEFAULT 'default',
  base_url           VARCHAR(255) COMMENT 'OpenAI 兼容服务地址',
  encrypted_api_key  VARCHAR(512) COMMENT 'API Key AES-GCM 加密（AesUtil）',
  model              VARCHAR(100),
  timeout_ms         INT,
  max_tokens         INT,
  temperature        DOUBLE,
  updated_by         VARCHAR(100),
  updated_at         DATETIME,
  PRIMARY KEY (id),
  UNIQUE KEY uk_llm_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大模型接入配置';
