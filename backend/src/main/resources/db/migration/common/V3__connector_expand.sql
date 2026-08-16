-- V3__connector_expand.sql · connector 表扩展（数据池模块）
ALTER TABLE connector ADD COLUMN database_name VARCHAR(100) NULL;
ALTER TABLE connector ADD COLUMN schema_name    VARCHAR(100) NULL;
ALTER TABLE connector ADD COLUMN is_active      TINYINT DEFAULT 0;
ALTER TABLE connector ADD COLUMN last_test_status VARCHAR(20) NULL;
ALTER TABLE connector ADD COLUMN last_test_time   DATETIME NULL;
