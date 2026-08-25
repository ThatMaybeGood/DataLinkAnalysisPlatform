-- ============================================================
-- V10__analysis_task.sql · MySQL 8
-- 图来源分析任务：对某来源（可多来源合并）发起的一次分析留痕
-- ============================================================

CREATE TABLE analysis_task (
  id               BIGINT       NOT NULL AUTO_INCREMENT,
  connector_id     BIGINT       NOT NULL COMMENT '首个来源连接器 id（过滤/兼容单来源）',
  connector_ids    VARCHAR(500) COMMENT '多来源合并时的全部来源 id（逗号分隔）',
  connector_name   VARCHAR(255) COMMENT '来源名快照（删库后仍可显示）',
  task_type        VARCHAR(20)  NOT NULL COMMENT 'ENGINE/LLM',
  status           VARCHAR(20)  NOT NULL DEFAULT 'RUNNING' COMMENT 'RUNNING/SUCCESS/FAILED',
  draft_snapshot   MEDIUMTEXT   COMMENT '草稿/细化结果 JSON',
  error_message    VARCHAR(500),
  operator         VARCHAR(100),
  created_at       DATETIME     NOT NULL,
  finished_at      DATETIME,
  PRIMARY KEY (id),
  KEY idx_task_connector (connector_id),
  KEY idx_task_created (created_at),
  KEY idx_task_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图来源分析任务';
