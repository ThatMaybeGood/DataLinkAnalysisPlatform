/* ============================================================
   DataLink 平台 · Mock 数据（演示用）
   后端接入后替换为真实 API（见 api/index.ts）
   场景：付款流程（默认/风控备选/支付备选三条路线）、订单流程、对账流程
   ============================================================ */

import type {
  AlertItem, Checkpoint, Connector, DashboardStats, GraphEdge,
  GraphNode, Instance, ProcessDef, Route, VersionRecord,
} from '@/types';

/* ---------- 检测点 ---------- */
const cp = (id: string, name: string, kind: 'DEFAULT' | 'CUSTOM', checkType: string, status: Checkpoint['status'], lastCheck: string, detail?: string): Checkpoint =>
  ({ id, name, kind, checkType, status, lastCheck, detail });

/* ---------- 节点（站点） ---------- */
export const mockNodes: GraphNode[] = [
  // 付款流程
  { id: 'n1', name: '付款发起', code: 'PAY_START', nodeType: 'ACTION', level: 'L1', status: 'ACTIVE', owner: '交易产品', description: '用户/业务发起一笔付款', checkpoints: [cp('cp1', '动作是否完成', 'DEFAULT', 'ACTION_STATUS', 'PASS', '2 分钟前')] },
  { id: 'n2', name: '交易系统', code: 'TRADE_SYS', nodeType: 'SYSTEM', level: 'L1', status: 'ACTIVE', owner: '交易组', description: '交易核心系统，负责订单与交易处理', checkpoints: [cp('cp2', '服务状态', 'DEFAULT', 'SERVICE_STATUS', 'PASS', '1 分钟前'), cp('cp3', '接口耗时 P95', 'CUSTOM', 'THRESHOLD', 'PASS', '5 分钟前', '≤ 300ms')] },
  { id: 'n3', name: '订单表', code: 'DB_ORDER', nodeType: 'DATABASE', level: 'L1', status: 'ACTIVE', owner: '数据组', description: 'MySQL 订单主库 order 表', checkpoints: [cp('cp4', '数据量变化', 'DEFAULT', 'DATA_VOLUME', 'PASS', '3 分钟前'), cp('cp5', '数据新鲜度', 'DEFAULT', 'FRESHNESS', 'FAIL', '3 分钟前', '最新数据 42 分钟前')] },
  { id: 'n4', name: '风控系统', code: 'RISK_SYS', nodeType: 'SYSTEM', level: 'L1', status: 'WARNING', owner: '风控组', description: '交易风险识别与拦截', checkpoints: [cp('cp6', '服务状态', 'DEFAULT', 'SERVICE_STATUS', 'WARNING', '1 分钟前', '响应偏慢')] },
  { id: 'n5', name: '风控表', code: 'DB_RISK', nodeType: 'DATABASE', level: 'L2', status: 'WARNING', owner: '数据组', description: '风控决策明细表', checkpoints: [cp('cp7', '数据同步延迟', 'DEFAULT', 'DELAY', 'FAIL', '4 分钟前', '同步延迟 28 分钟')] },
  { id: 'n6', name: '支付系统', code: 'PAY_SYS', nodeType: 'SYSTEM', level: 'L1', status: 'FAIL', owner: '支付组', description: '资金支付与渠道对接', checkpoints: [cp('cp8', '服务状态', 'DEFAULT', 'SERVICE_STATUS', 'FAIL', '刚刚', '支付渠道异常')] },
  { id: 'n7', name: '流水表', code: 'DB_FLOW', nodeType: 'DATABASE', level: 'L2', status: 'WARNING', owner: '数据组', description: '支付流水明细表', checkpoints: [cp('cp9', '数据新鲜度', 'DEFAULT', 'FRESHNESS', 'TIMEOUT', '5 分钟前', '检测超时')] },
  { id: 'n8', name: '结算部门', code: 'DEP_SETTLE', nodeType: 'DEPARTMENT', level: 'L1', status: 'ACTIVE', owner: '财务部', description: '结算与对账业务部门', checkpoints: [cp('cp10', '处理耗时', 'CUSTOM', 'THRESHOLD', 'PASS', '6 分钟前', '均值 4.2h')] },
  { id: 'n9', name: '记账', code: 'ACT_BOOK', nodeType: 'ACTION', level: 'L2', status: 'ACTIVE', description: '生成记账凭证', checkpoints: [] },
  { id: 'n10', name: '对账', code: 'ACT_RECON', nodeType: 'ACTION', level: 'L2', status: 'ACTIVE', description: '渠道对账核对', checkpoints: [] },
  { id: 'n11', name: '付款完成', code: 'PAY_DONE', nodeType: 'ACTION', level: 'L1', status: 'ACTIVE', description: '流程终点', checkpoints: [] },
  // 订单流程
  { id: 'n12', name: '下单', code: 'ORD_CREATE', nodeType: 'ACTION', level: 'L2', status: 'ACTIVE', description: '客户提交订单', checkpoints: [] },
  { id: 'n13', name: '库存系统', code: 'INV_SYS', nodeType: 'SYSTEM', level: 'L2', status: 'ACTIVE', owner: '供应链组', description: '库存管理与扣减', checkpoints: [cp('cp11', '库存扣减正确性', 'CUSTOM', 'SQL', 'PASS', '8 分钟前', '行数比对通过')] },
  { id: 'n14', name: '库存表', code: 'DB_INV', nodeType: 'DATABASE', level: 'L2', status: 'ACTIVE', owner: '数据组', description: '库存明细表', checkpoints: [] },
  { id: 'n15', name: '履约部门', code: 'DEP_FULFILL', nodeType: 'DEPARTMENT', level: 'L2', status: 'ACTIVE', owner: '运营部', description: '订单履约与出库', checkpoints: [] },
  { id: 'n16', name: '发货', code: 'ORD_SHIP', nodeType: 'ACTION', level: 'L2', status: 'ACTIVE', description: '仓库发货', checkpoints: [] },
  { id: 'n17', name: '签收', code: 'ORD_SIGN', nodeType: 'ACTION', level: 'L3', status: 'ACTIVE', description: '客户签收，流程终点', checkpoints: [] },
];

/* ---------- 路网边 ---------- */
const edge = (id: string, source: string, target: string, relationType: GraphEdge['relationType']): GraphEdge =>
  ({ id, source, target, relationType });

export const mockEdges: GraphEdge[] = [
  // 付款路网（分岔/汇合）
  edge('e1', 'n1', 'n2', 'DATA_FLOW'), edge('e2', 'n1', 'n4', 'DATA_FLOW'), edge('e3', 'n1', 'n6', 'DATA_FLOW'),
  edge('e4', 'n2', 'n3', 'DATA_FLOW'), edge('e5', 'n4', 'n5', 'DATA_FLOW'), edge('e6', 'n6', 'n7', 'DATA_FLOW'),
  edge('e7', 'n3', 'n8', 'DATA_FLOW'), edge('e8', 'n5', 'n8', 'DATA_FLOW'), edge('e9', 'n7', 'n8', 'DATA_FLOW'),
  edge('e10', 'n8', 'n9', 'APPROVAL'), edge('e11', 'n8', 'n10', 'APPROVAL'),
  edge('e12', 'n9', 'n11', 'DATA_FLOW'), edge('e13', 'n10', 'n11', 'DATA_FLOW'),
  // 订单路网
  edge('e14', 'n12', 'n13', 'DATA_FLOW'), edge('e15', 'n12', 'n6', 'DATA_FLOW'),
  edge('e16', 'n13', 'n14', 'DATA_FLOW'), edge('e17', 'n14', 'n15', 'DATA_FLOW'),
  edge('e18', 'n15', 'n16', 'DATA_FLOW'), edge('e19', 'n16', 'n17', 'DATA_FLOW'),
];

/* ---------- 流程 ---------- */
export const mockProcesses: ProcessDef[] = [
  { id: 'p1', name: '付款流程', scene: 'BUSINESS', level: 'L1', description: '从付款发起到付款完成的完整业务流程，含默认/风控/支付三条路线', startNodeName: '付款发起', endNodeName: '付款完成', nodeCount: 11, routeCount: 3, instanceStats: { running: 1, success: 8, fail: 1 }, updatedAt: '2026-08-16 09:20' },
  { id: 'p2', name: '订单流程', scene: 'BUSINESS', level: 'L2', description: '从下单到签收的订单履约流程', startNodeName: '下单', endNodeName: '签收', nodeCount: 7, routeCount: 1, instanceStats: { running: 1, success: 4, fail: 0 }, updatedAt: '2026-08-15 18:04' },
  { id: 'p3', name: '对账流程', scene: 'BUSINESS', level: 'L2', description: '结算部门发起的渠道对账核对', startNodeName: '结算部门', endNodeName: '对账', nodeCount: 3, routeCount: 1, instanceStats: { running: 0, success: 3, fail: 0 }, updatedAt: '2026-08-14 11:30' },
  { id: 'p4', name: '数据同步链路', scene: 'DATA', level: 'L3', description: '交易数据从业务库同步到数仓（演示数据流场景）', startNodeName: '交易系统', endNodeName: '流水表', nodeCount: 4, routeCount: 1, instanceStats: { running: 0, success: 6, fail: 0 }, updatedAt: '2026-08-13 22:10' },
];

/* ---------- 路线 ---------- */
export const mockRoutes: Route[] = [
  { id: 'r1', processId: 'p1', name: '标准结算路线', priority: 'DEFAULT', status: 'ACTIVE', nodeIds: ['n1', 'n2', 'n3', 'n8', 'n9', 'n11'], totalDuration: '≈ 4.5h' },
  { id: 'r2', processId: 'p1', name: '风控拦截路线', priority: 'ALTERNATE', status: 'WARNING', nodeIds: ['n1', 'n4', 'n5', 'n8', 'n10', 'n11'], totalDuration: '≈ 6h' },
  { id: 'r3', processId: 'p1', name: '大额支付路线', priority: 'ALTERNATE', status: 'WARNING', nodeIds: ['n1', 'n6', 'n7', 'n8', 'n9', 'n11'], totalDuration: '≈ 5h' },
  { id: 'r4', processId: 'p2', name: '订单主链路', priority: 'DEFAULT', status: 'ACTIVE', nodeIds: ['n12', 'n13', 'n14', 'n15', 'n16', 'n17'], totalDuration: '≈ 26h' },
  { id: 'r5', processId: 'p3', name: '对账主链路', priority: 'DEFAULT', status: 'ACTIVE', nodeIds: ['n8', 'n10'], totalDuration: '≈ 3h' },
];

/* ---------- 实例 ---------- */
export const mockInstances: Instance[] = [
  { id: 'i1', bizNo: 'PAY-20260801-001', bizName: '华东区 8 月结算款', processName: '付款流程', routeName: '标准结算路线', status: 'RUNNING', progress: 66, currentNode: '结算部门', currentNodeId: 'n8', startTime: '2026-08-16 09:15', duration: '2h 12m', source: 'INFER' },
  { id: 'i2', bizNo: 'PAY-20260802-001', bizName: '华南区渠道返利', processName: '付款流程', routeName: '标准结算路线', status: 'SUCCESS', progress: 100, currentNode: '付款完成', currentNodeId: 'n11', startTime: '2026-08-16 08:00', duration: '4h 03m', source: 'INFER' },
  { id: 'i3', bizNo: 'PAY-20260803-001', bizName: '风控补录批次', processName: '付款流程', routeName: '风控拦截路线', status: 'STUCK', progress: 50, currentNode: '风控表', currentNodeId: 'n5', startTime: '2026-08-15 22:30', duration: '> 10h', source: 'INFER' },
  { id: 'i4', bizNo: 'PAY-20260804-001', bizName: '大额对公付款', processName: '付款流程', routeName: '大额支付路线', status: 'FAIL', progress: 50, currentNode: '支付系统', currentNodeId: 'n6', startTime: '2026-08-16 09:40', duration: '—', source: 'MANUAL' },
  { id: 'i5', bizNo: 'ORD-20260801-001', bizName: '618 大促订单补发', processName: '订单流程', routeName: '订单主链路', status: 'RUNNING', progress: 83, currentNode: '履约部门', currentNodeId: 'n15', startTime: '2026-08-15 14:00', duration: '19h', source: 'REPORT' },
  { id: 'i6', bizNo: 'ORD-20260801-002', bizName: '华东仓日常订单', processName: '订单流程', routeName: '订单主链路', status: 'SUCCESS', progress: 100, currentNode: '签收', currentNodeId: 'n17', startTime: '2026-08-15 10:20', duration: '28h', source: 'REPORT' },
];

/* ---------- 告警 ---------- */
export const mockAlerts: AlertItem[] = [
  { id: 'a1', type: 'STUCK', severity: 'P1', targetType: 'INSTANCE', targetName: '风控补录批次', message: '流程卡在「风控表」超过 10 小时，超过 SLA（6h）', status: 'OPEN', time: '2026-08-16 08:31', level: 'L1' },
  { id: 'a2', type: 'FAIL', severity: 'P1', targetType: 'INSTANCE', targetName: '大额对公付款', message: '「支付系统」检测失败：支付渠道异常', status: 'OPEN', time: '2026-08-16 09:47', level: 'L1' },
  { id: 'a3', type: 'CHECK_FAIL', severity: 'P2', targetType: 'NODE', targetName: '流水表', message: '数据新鲜度检测超时（最新数据 42 分钟前）', status: 'OPEN', time: '2026-08-16 09:50', level: 'L2' },
  { id: 'a4', type: 'TIMEOUT', severity: 'P2', targetType: 'NODE', targetName: '风控系统', message: '接口响应超时，P95 超过阈值', status: 'RESOLVED', time: '2026-08-16 07:12', level: 'L1' },
  { id: 'a5', type: 'CHECK_FAIL', severity: 'P3', targetType: 'NODE', targetName: '订单表', message: '数据新鲜度偏低（42 分钟），建议关注同步任务', status: 'RESOLVED', time: '2026-08-15 23:05', level: 'L1' },
];

/* ---------- 版本记录 ---------- */
export const mockVersions: VersionRecord[] = [
  { id: 'v1', targetType: '流程', targetName: '付款流程', version: 12, operator: '张工', changeNote: '新增「大额支付路线」，调整支付系统等级为 L1', status: 'PUBLISHED', time: '2026-08-16 09:20' },
  { id: 'v2', targetType: '检测点', targetName: '流水表·数据新鲜度', version: 3, operator: '李工', changeNote: '阈值从 30 分钟调整为 20 分钟', status: 'PENDING_APPROVAL', time: '2026-08-16 09:05' },
  { id: 'v3', targetType: '路线', targetName: '风控拦截路线', version: 5, operator: '王工', changeNote: '调整风控表 SLA 为 6h', status: 'PUBLISHED', time: '2026-08-15 17:40' },
  { id: 'v4', targetType: '节点', targetName: '订单表', version: 8, operator: '张工', changeNote: '归属系统改为交易系统，补充负责人', status: 'PUBLISHED', time: '2026-08-15 10:12' },
  { id: 'v5', targetType: '连接器', targetName: '交易库 MySQL', version: 2, operator: '管理员', changeNote: '更新连接地址，迁移到新实例', status: 'ROLLED_BACK', time: '2026-08-14 16:30' },
];

/* ---------- 连接器 ---------- */
export const mockConnectors: Connector[] = [
  { id: 'c1', name: '交易库 MySQL', type: 'DB', dbType: 'MYSQL', host: '10.20.1.12:3306', enabled: true, lastRun: '2026-08-16 09:30', status: 'OK' },
  { id: 'c2', name: '对账库 Oracle', type: 'DB', dbType: 'ORACLE', host: '10.20.2.8:1521', enabled: true, lastRun: '2026-08-16 09:00', status: 'OK' },
  { id: 'c3', name: '流程导入 Excel', type: 'EXCEL', enabled: true, lastRun: '2026-08-15 11:20', status: 'IDLE' },
  { id: 'c4', name: 'CMDB 资产清单', type: 'CMDB', host: 'cmdb.corp.local', enabled: false, lastRun: '—', status: 'IDLE' },
];

/* ---------- 看板统计 ---------- */
export const mockDashboardStats: DashboardStats = {
  processCount: 4,
  runningInstances: 2,
  doneToday: 12,
  openAlerts: 3,
  stuckCount: 1,
  checkpointCoverage: 91,
  avgDuration: '4.8h',
  topSlowNodes: [
    { name: '风控表', duration: '28m 延迟' },
    { name: '结算部门', duration: '4.2h' },
    { name: '流水表', duration: '检测超时' },
  ],
  instanceTrend: [
    { label: '08:00', value: 2 }, { label: '09:00', value: 4 }, { label: '10:00', value: 5 },
    { label: '11:00', value: 7 }, { label: '12:00', value: 6 }, { label: '13:00', value: 9 },
    { label: '14:00', value: 8 }, { label: '15:00', value: 12 },
  ],
};

/** 节点 → 中文类型标签 */
export const nodeTypeLabel: Record<string, string> = {
  SYSTEM: '系统', SUBSYSTEM: '子系统', DATABASE: '数据库', TABLE: '表',
  DEPARTMENT: '部门', ROLE: '岗位', ACTION: '动作', EVENT: '事件',
  DEVICE: '设备', WORKSTATION: '工位',
};

/** 状态 → 中文标签 */
export const statusLabel: Record<string, string> = {
  ACTIVE: '正常', WARNING: '异常', FAIL: '失败', DISABLED: '停用',
  RUNNING: '运行中', SUCCESS: '成功', STUCK: '卡住', TIMEOUT: '超时',
};
