-- ============================================================
-- HIS 演示业务库（图来源引擎 G3 扫描对象）
-- 位置：classpath:engine/his_demo_schema.sql
-- 由 DemoBizDbInitializer 启动时灌入独立内存库：
--   jdbc:h2:mem:datalink_demo;MODE=MySQL;DB_CLOSE_DELAY=-1
-- 连接参数必须与 H2Dialect.buildJdbcUrl 完全一致，连接器才能连入同一内存库。
--
-- 设计意图：覆盖「通用业务单据模式库」识别信号
--   ① 主键     —— id 单列主键
--   ② 业务编码号 —— *_no / *_code（reg_no/fee_no/refund_no/settle_no/pay_no）
--   ③ 状态字段 —— status（值随流程变化）
--   ④ 时间字段 —— *_time（reg_time/fee_time/refund_time/settle_time/pay_time）
--   ⑤ 单号引用链 —— 跨表 *_no（fee_order.reg_no→reg_order、refund_apply.fee_no→fee_order…）
--   ⑥ 主子表   —— 明细行外键 *_id（prescription_detail.presc_id→主表）
-- ============================================================

-- 挂号单（主表：主键 + 单号 + 状态 + 时间）
CREATE TABLE reg_order (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  reg_no       VARCHAR(32) NOT NULL,
  patient_name VARCHAR(50),
  dept_name    VARCHAR(50),
  status       VARCHAR(20),
  reg_time     DATETIME,
  create_time  DATETIME
);
COMMENT ON TABLE reg_order IS '挂号单';

-- 收费单（主表：主键 + 单号 + 状态 + 时间 + 引用挂号单号）
CREATE TABLE fee_order (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  fee_no       VARCHAR(32) NOT NULL,
  reg_no       VARCHAR(32),
  total_amount DECIMAL(12,2),
  status       VARCHAR(20),
  fee_time     DATETIME,
  create_time  DATETIME
);
COMMENT ON TABLE fee_order IS '收费单';

-- 退费申请单（主键 + 单号 + 状态 + 时间 + 引用收费单号）
CREATE TABLE refund_apply (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  refund_no   VARCHAR(32) NOT NULL,
  fee_no      VARCHAR(32),
  reason      VARCHAR(200),
  status      VARCHAR(20),
  refund_time DATETIME
);
COMMENT ON TABLE refund_apply IS '退费申请单';

-- 结算单（主键 + 单号 + 时间，无状态、无引用 → 置信度中等）
CREATE TABLE settle_bill (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  settle_no   VARCHAR(32) NOT NULL,
  settle_time DATETIME
);
COMMENT ON TABLE settle_bill IS '结算单';

-- 支付流水（流水表：主键 + 单号 + 时间 + 引用收费单号，缺状态 → 置信度中等偏低）
CREATE TABLE pay_record (
  id       BIGINT PRIMARY KEY AUTO_INCREMENT,
  pay_no   VARCHAR(32),
  fee_no   VARCHAR(32),
  channel  VARCHAR(20),
  amount   DECIMAL(12,2),
  pay_time DATETIME
);
COMMENT ON TABLE pay_record IS '支付流水';

-- 处方明细（主子表明细行：主键 + 外键 presc_id → 主表，无单号/状态/时间 → 低置信）
CREATE TABLE prescription_detail (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  presc_id   BIGINT NOT NULL,
  drug_name  VARCHAR(50),
  qty        INT,
  unit_price DECIMAL(10,2)
);
COMMENT ON TABLE prescription_detail IS '处方明细';

-- ============ 演示数据 ============
INSERT INTO reg_order (reg_no, patient_name, dept_name, status, reg_time, create_time) VALUES
('REG20260801001','张三','内科','已完成','2026-08-01 08:30:00','2026-08-01 08:30:00'),
('REG20260801002','李四','外科','已完成','2026-08-01 09:10:00','2026-08-01 09:10:00'),
('REG20260801003','王五','内科','待就诊','2026-08-01 10:00:00','2026-08-01 10:00:00');

INSERT INTO fee_order (fee_no, reg_no, total_amount, status, fee_time, create_time) VALUES
('FEE20260801001','REG20260801001',120.50,'已支付','2026-08-01 09:00:00','2026-08-01 09:00:00'),
('FEE20260801002','REG20260801002',80.00,'已支付','2026-08-01 10:20:00','2026-08-01 10:20:00');

INSERT INTO refund_apply (refund_no, fee_no, reason, status, refund_time) VALUES
('RFN20260801001','FEE20260801001','重复缴费','待审批','2026-08-01 15:00:00');

INSERT INTO settle_bill (settle_no, settle_time) VALUES
('STL20260801001','2026-08-01 18:00:00');

INSERT INTO pay_record (pay_no, fee_no, channel, amount, pay_time) VALUES
('PAY20260801001','FEE20260801001','微信',120.50,'2026-08-01 09:01:00'),
('PAY20260801002','FEE20260801002','支付宝',80.00,'2026-08-01 10:21:00');

INSERT INTO prescription_detail (presc_id, drug_name, qty, unit_price) VALUES
(1,'阿莫西林',2,12.50),
(1,'感冒灵',1,20.00),
(2,'布洛芬',3,5.00);
