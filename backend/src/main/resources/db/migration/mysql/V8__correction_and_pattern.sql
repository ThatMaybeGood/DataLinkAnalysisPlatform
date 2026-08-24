-- ============================================================
-- V8__correction_and_pattern.sql · MySQL 8
-- 图来源 G5 校正闭环：校正记录 + 模式库
-- ============================================================

CREATE TABLE correction_record (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  target_type   VARCHAR(20)  NOT NULL COMMENT 'NODE/EDGE/ROUTE/PATTERN',
  target_id     VARCHAR(255) NOT NULL COMMENT '被校正对象 id',
  target_name   VARCHAR(255) COMMENT '对象显示名',
  operation     VARCHAR(20)  NOT NULL COMMENT 'RENAME/CONFIRM/MERGE/ADD/DELETE/REORDER',
  old_value     TEXT COMMENT '原值',
  new_value     TEXT COMMENT '新值',
  merge_target_id VARCHAR(255) COMMENT '合并目标 id',
  reorder_node_ids TEXT COMMENT '排序后的节点 id 列表 JSON',
  status        VARCHAR(20)  DEFAULT 'PENDING' COMMENT 'PENDING/APPLIED/REJECTED',
  source        VARCHAR(50)  DEFAULT 'MANUAL' COMMENT 'ENGINE/LLM/MANUAL',
  operator      VARCHAR(100),
  remark        TEXT,
  created_at    DATETIME     NOT NULL,
  updated_at    DATETIME,
  PRIMARY KEY (id),
  KEY idx_correction_target (target_type, target_id),
  KEY idx_correction_status (status),
  KEY idx_correction_operator (operator)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校正记录表';

CREATE TABLE pattern_library (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  pattern_type  VARCHAR(20)  NOT NULL COMMENT 'NODE_NAME/EDGE_NAME/ROUTE_TEMPLATE',
  pattern_key   VARCHAR(500) NOT NULL COMMENT '模式匹配键',
  pattern_value TEXT COMMENT '模式值/模板',
  source_type   VARCHAR(20) COMMENT 'NODE/EDGE/ROUTE/PATTERN',
  source_id     VARCHAR(255) COMMENT '来源对象 id',
  source_operation VARCHAR(20) COMMENT '来源操作类型',
  hit_count     INT          DEFAULT 0 COMMENT '命中次数',
  confirmed     TINYINT      DEFAULT 0 COMMENT '是否已确认',
  created_by    VARCHAR(100),
  created_at    DATETIME     NOT NULL,
  updated_at    DATETIME,
  PRIMARY KEY (id),
  KEY idx_pattern_type (pattern_type),
  KEY idx_pattern_key (pattern_key),
  KEY idx_pattern_confirmed (confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模式库';
