/* ============================================================
   DataLink 平台 · 领域类型定义
   与 v1.1 项目文档书的数据模型对应
   ============================================================ */

/** 站点类型（4 类混合 + 制造预留） */
export type NodeType =
  | 'SYSTEM' | 'SUBSYSTEM'      // 系统/子系统
  | 'DATABASE' | 'TABLE'        // 数据库/表
  | 'DEPARTMENT' | 'ROLE'       // 部门/角色/岗位
  | 'ACTION' | 'EVENT'          // 业务动作/事件
  | 'DEVICE' | 'WORKSTATION';   // 制造：设备/工位（远期）

/** 节点等级（L1 核心 ~ L4 一般） */
export type Level = 'L1' | 'L2' | 'L3' | 'L4';

/** 运行状态 */
export type RunStatus = 'ACTIVE' | 'WARNING' | 'FAIL' | 'DISABLED';

/** 关系类型 */
export type RelationType =
  | 'DATA_FLOW' | 'API' | 'SUBSCRIBE' | 'APPROVAL' | 'DEPEND';

/** 路线优先级 */
export type RoutePriority = 'DEFAULT' | 'RECOMMENDED' | 'ALTERNATE';

/** 检测点状态 */
export type CheckStatus = 'PASS' | 'FAIL' | 'TIMEOUT' | 'WARNING';

/** 实例状态 */
export type InstanceStatus = 'RUNNING' | 'SUCCESS' | 'FAIL' | 'STUCK' | 'TIMEOUT';

/** 告警 */
export interface AlertItem {
  id: string;
  type: 'STUCK' | 'FAIL' | 'TIMEOUT' | 'CHECK_FAIL';
  severity: 'P0' | 'P1' | 'P2' | 'P3';
  targetName: string;       // 显示名/别名
  targetType: string;       // NODE/INSTANCE/ROUTE/PROCESS
  message: string;
  status: 'OPEN' | 'RESOLVED';
  time: string;
  level: Level;
}

/** 检测点 */
export interface Checkpoint {
  id: string;
  name: string;
  kind: 'DEFAULT' | 'CUSTOM';
  checkType: string;        // DATA_VOLUME/FRESHNESS/SERVICE_STATUS/SQL/...
  status: CheckStatus;
  lastCheck: string;
  detail?: string;
}

/** 站点节点 */
export interface GraphNode {
  id: string;
  name: string;             // 显示名（主别名）
  code?: string;            // 底层编码
  nodeType: NodeType;
  level: Level;
  status: RunStatus;
  owner?: string;
  description?: string;
  checkpoints: Checkpoint[];
}

/** 路网边（有向） */
export interface GraphEdge {
  id: string;
  source: string;
  target: string;
  relationType: RelationType;
}

/** 路线 */
export interface Route {
  id: string;
  processId: string;
  name: string;
  priority: RoutePriority;
  nodeIds: string[];        // 有序站点
  status: RunStatus;
  totalDuration?: string;
}

/** 流程 */
export interface ProcessDef {
  id: string;
  name: string;
  scene: 'DATA' | 'BUSINESS' | 'MANUFACTURING';
  level: Level;
  description: string;
  startNodeName: string;
  endNodeName: string;
  nodeCount: number;
  routeCount: number;
  instanceStats: { running: number; success: number; fail: number };
  updatedAt: string;
}

/** 实例 */
export interface Instance {
  id: string;
  bizNo: string;
  bizName: string;          // 业务别名（显示名）
  processName: string;
  routeName: string;
  status: InstanceStatus;
  progress: number;
  currentNode?: string;     // 当前所在站点
  currentNodeId?: string;
  startTime: string;
  duration: string;
  source: 'INFER' | 'MANUAL' | 'REPORT' | 'API';
}

/** 版本记录 */
export interface VersionRecord {
  id: string;
  targetType: string;
  targetName: string;
  version: number;
  operator: string;
  changeNote: string;
  status: 'PUBLISHED' | 'PENDING_APPROVAL' | 'ROLLED_BACK';
  time: string;
}

/** 连接器 */
export interface Connector {
  id: string;
  name: string;
  type: 'DB' | 'EXCEL' | 'CMDB' | 'API' | 'LOG' | 'IOT';
  dbType?: string;
  host?: string;
  enabled: boolean;
  lastRun: string;
  status: 'OK' | 'ERROR' | 'RUNNING' | 'IDLE';
}

/** 看板统计 */
export interface DashboardStats {
  processCount: number;
  runningInstances: number;
  doneToday: number;
  openAlerts: number;
  stuckCount: number;
  checkpointCoverage: number;   // %
  avgDuration: string;
  topSlowNodes: { name: string; duration: string }[];
  instanceTrend: { label: string; value: number }[];
}

/** 后端运行信息（来自 /api/health） */
export interface HealthInfo {
  status: 'UP' | 'DEGRADED' | 'DOWN';
  app: string;
  version: string;
  mode: 'h2' | 'mysql';          // h2=离线本地，mysql=部署
  db: 'UP' | 'DOWN';
  time: string;
}

/* ============================================================
   数据池：连接器管理（真实 DTO，对应后端 ConnectorVO）
   ============================================================ */

/** 数据源连接器（对应后端 ConnectorVO，密码字段永不出现） */
export interface DataSourceConnector {
  id: string;
  connectorType: string;               // 固定 "DB"
  dbType: 'mysql' | 'postgresql' | 'h2';
  name: string;
  host?: string;
  port?: number;
  username: string;
  databaseName: string;
  schemaName?: string;
  enabled: number;                     // 1/0 是否启用
  isActive: number;                    // 1/0 是否为当前连接
  lastTestStatus?: 'OK' | 'FAIL';      // 最近测试结果
  lastTestTime?: string;
  createdAt?: string;
}

/** 连接测试结果 */
export interface ConnectorTestResult {
  ok: boolean;
  latencyMs?: number;                  // 测试耗时（毫秒）
  dbVersion?: string;                  // 数据库产品版本
  message?: string;                    // 失败原因
}

/** 库表信息 */
export interface TableInfo {
  name: string;
  type: string;                        // TABLE / VIEW
}

/** 表数据预览 */
export interface TablePreview {
  columns: string[];
  rows: unknown[][];                   // 前 50 行
  rowCount: number;                    // 实际返回行数
}

/** 新建/编辑连接器请求体（password 新建必填，编辑留空 = 不改） */
export interface ConnectorSavePayload {
  name: string;
  dbType: 'mysql' | 'postgresql' | 'h2';
  host?: string;                       // H2 可空
  port?: number;                       // H2 可空
  databaseName: string;
  schemaName?: string;
  username: string;
  password?: string;
  config?: string;                     // JSON 扩展参数
  enabled?: number;                    // 默认 1
}
