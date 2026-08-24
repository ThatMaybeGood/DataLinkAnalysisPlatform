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
  connectorType: 'DB' | 'CMDB';        // DB 数据库 / CMDB 配置管理
  dbType?: 'mysql' | 'postgresql' | 'h2';   // 仅 DB 类型需要
  name: string;
  host?: string;
  port?: number;
  username?: string;                   // 仅 DB 类型需要
  databaseName?: string;               // 仅 DB 类型需要
  schemaName?: string;
  config?: string;                     // CMDB 存 JSON：{apiUrl, apiKey}
  enabled: number;                     // 1/0 是否启用
  isActive: number;                    // 1/0 是否为当前连接
  lastTestStatus?: 'OK' | 'FAIL';      // 最近测试结果
  lastTestTime?: string;
  createdAt?: string;
}

/** CMDB 候选节点（来自连接器候选清单） */
export interface CandidateNode {
  name: string;
  type?: string;
  description?: string;
  owner?: string;
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

/** 新建/编辑连接器请求体（password 新建必填，编辑留空 = 不改；CMDB 无需 DB 字段） */
export interface ConnectorSavePayload {
  name: string;
  connectorType?: 'DB' | 'CMDB';       // 缺省为 DB
  dbType?: 'mysql' | 'postgresql' | 'h2';
  host?: string;                       // H2 可空
  port?: number;                       // H2 可空
  databaseName?: string;
  schemaName?: string;
  username?: string;
  password?: string;
  config?: string;                     // JSON 扩展参数；CMDB 存 {apiUrl, apiKey}
  enabled?: number;                    // 默认 1
}

/** 登录结果（来自 /api/auth/login 与 /api/auth/me） */
export interface LoginResult {
  token: string;                       // JWT（/me 恒为 null）
  displayName: string;                 // 显示名
  roles: string[];                     // 角色集合
}

/** 引擎识别候选单据 */
export interface EngineCandidate {
  table: string;
  name: string;
  confidence: number;                  // 0~100，命中设计区间 60~85（主表）
  marks: string[];                     // 主键/单号/状态/时间/引用/主子表/单号格式
  low?: boolean;                       // confidence < 70 为低置信
  statusValues?: string[];
}

/** 引擎分析流程模板（数据方向 provider→consumer） */
export interface EngineFlow {
  name: string;                        // 业务名用 → 连接的链，如「挂号单→收费单」
  nodeIds: string[];
  tableNames: string[];
}

/** 引擎分析返回草稿（对应后端 EngineDraftVO） */
export interface EngineDraft {
  database: string;
  candidates: EngineCandidate[];       // 按置信度降序
  draftNodes: GraphNode[];             // 库节点 + 表节点（id 形如 db-xxx / t-xxx）
  draftEdges: GraphEdge[];             // 库→表 承载边 + 引用方向 DATA_FLOW 边
  flows: EngineFlow[];                 // 流程模板
  message?: string;
}

/** 大模型细化清单项（rename/chain/party/relation/flow + noop/error） */
export interface RefinementItem {
  type: string;
  text: string;
}

/** 引擎草稿 + 大模型细化返回（对应后端 RefineResultVO） */
export interface EngineRefineResult {
  base: EngineDraft;                   // 引擎骨架原样返回（前端回退快照）
  addedNodes: GraphNode[];             // 大模型增量节点（id 带 llm- 前缀）
  addedEdges: GraphEdge[];             // 大模型增量边
  renameMap: Record<string, string>;   // 改名映射（表名/节点id → 业务名）
  refinements: RefinementItem[];       // 语义补全清单
  provider: string;                    // noop=未配置大模型 / error=调用异常
  message?: string;
}

/** 工单 */
export interface Ticket {
  id: string;
  alertId?: string;
  assignee?: string;
  priority?: string;
  status: 'OPEN' | 'PROCESSING' | 'RESOLVED';
  description?: string;
  createdAt: string;
  resolvedAt?: string;
}

/** 工单创建/更新请求 */
export interface TicketPayload {
  alertId?: string;
  assignee?: string;
  priority?: string;
  status?: 'OPEN' | 'PROCESSING' | 'RESOLVED';
  description?: string;
}

// ============================================================
// 图来源 · 人工校正闭环（G5）
// ============================================================

/** 校正操作类型 */
export type CorrectionOperation =
  | 'RENAME'      // 改名
  | 'CONFIRM'     // 标记正确
  | 'MERGE'       // 合并到目标
  | 'ADD'         // 新增
  | 'DELETE'      // 标记废弃
  | 'REORDER';    // 排序（路线节点顺序调整）

/** 校正对象类型 */
export type CorrectionTargetType = 'NODE' | 'EDGE' | 'ROUTE' | 'PATTERN';

/** 校正记录 */
export interface CorrectionRecord {
  id: number;
  targetType: CorrectionTargetType;
  targetId: string;
  targetName: string;
  operation: CorrectionOperation;
  oldValue?: string;
  newValue?: string;
  mergeTargetId?: string;
  reorderNodeIds?: string[];
  status: 'PENDING' | 'APPLIED' | 'REJECTED';
  source: 'ENGINE' | 'LLM' | 'MANUAL';
  operator?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 提交校正请求体 */
export interface CorrectionPayload {
  targetType: CorrectionTargetType;
  targetId: string;
  targetName: string;
  operation: CorrectionOperation;
  oldValue?: string;
  newValue?: string;
  mergeTargetId?: string;
  reorderNodeIds?: string[];
  remark?: string;
  savePattern?: boolean;
  patternType?: string;
  patternName?: string;
  patternDescription?: string;
}

/** 模式库条目 */
export interface Pattern {
  id: number;
  patternType: string;
  patternKey: string;
  patternValue?: string;
  sourceType?: CorrectionTargetType;
  sourceId?: string;
  sourceOperation?: CorrectionOperation;
  hitCount: number;
  confirmed: number;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

/** 新建模式请求体 */
export interface PatternPayload {
  patternType: string;
  patternKey: string;
  patternValue?: string;
  sourceType: CorrectionTargetType;
  sourceId: string;
  sourceOperation?: CorrectionOperation;
}
