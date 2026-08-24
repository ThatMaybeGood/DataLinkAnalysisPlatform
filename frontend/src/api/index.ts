/* ============================================================
   DataLink 平台 · 数据访问层
   关系网 / 流程、数据池（连接器管理）、监控域（实例 / 告警 / 看板 /
   检测点 / 上下游追踪 / 版本记录 / 路径 / 影响面 / 工单）与健康探活
   均已接入真实 HTTP 接口（Result<T>，code === 200 成功）。
   接口路径与 v1.1 项目文档书第 5 章模块一致。
   ============================================================ */

import type {
  AlertItem, CandidateNode, Checkpoint, ConnectorSavePayload, ConnectorTestResult, CorrectionPayload,
  CorrectionRecord, DashboardStats, DataSourceConnector, EngineDraft, EngineRefineResult, GraphEdge,
  GraphNode, HealthInfo, Instance, Level, LoginResult, Pattern, PatternPayload, ProcessDef, Route, TableInfo,
  TablePreview, Ticket, TicketPayload, VersionRecord,
} from '@/types';

/** 保存流程请求体 */
export interface ProcessSavePayload {
  name: string;
  scene?: 'DATA' | 'BUSINESS' | 'MANUFACTURING';
  level?: Level;
  description?: string;
  startNodeId?: number;
  endNodeId?: number;
}

/** 保存检测点请求体 */
export interface CheckpointSavePayload {
  nodeId: number;
  name: string;
  checkType: string;
  kind?: 'DEFAULT' | 'CUSTOM';
  freq?: string;
  level?: Level;
}

// ============================================================
// 鉴权：token 存取 / 登录 / 当前用户
// 契约：POST /api/auth/login 与 GET /api/auth/me 均返回
//   { code, message, data: { token, displayName, roles } }
//   其余 /api/** 需 Authorization: Bearer <token>，否则 HTTP 401
// ============================================================

/** token 存储键（localStorage） */
export const TOKEN_KEY = 'datalink_token';
/** 当前用户显示名存储键（localStorage） */
export const DISPLAY_NAME_KEY = 'datalink_display_name';
/** 当前用户角色存储键（localStorage，JSON 数组） */
export const ROLES_KEY = 'datalink_roles';

/** 读取登录 token（未登录返回 null） */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

/** 保存登录 token */
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

/** 清除登录 token */
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

/** 登录（POST /api/auth/login，本身不需要 token） */
export async function login(username: string, password: string): Promise<LoginResult> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  });
  return unwrap<LoginResult>(res);
}

/** 获取当前登录用户（GET /api/auth/me，带 Bearer token） */
export async function fetchMe(): Promise<LoginResult> {
  return unwrap<LoginResult>(await apiFetch('/api/auth/me'));
}

/** 统一请求封装：自动附加 Bearer token；401 清 token 并回登录页 */
async function apiFetch(path: string, options: RequestInit = {}): Promise<Response> {
  const token = getToken();
  const headers = new Headers(options.headers);
  if (token) headers.set('Authorization', `Bearer ${token}`);
  const res = await fetch(path, { ...options, headers });
  if (res.status === 401) {
    clearToken();
    if (window.location.pathname !== '/login') window.location.href = '/login';
    throw new Error('未认证或登录已过期');
  }
  return res;
}

/** 关系网 / 流程数据（真实接口，返回 Result<T>，code === 200 成功） */
export async function fetchNodes(): Promise<GraphNode[]> {
  return unwrap<GraphNode[]>(await apiFetch('/api/nodes'));
}
export async function fetchEdges(): Promise<GraphEdge[]> {
  return unwrap<GraphEdge[]>(await apiFetch('/api/edges'));
}
export async function fetchProcesses(): Promise<ProcessDef[]> {
  return unwrap<ProcessDef[]>(await apiFetch('/api/processes'));
}
/** 新建流程 */
export async function createProcess(payload: ProcessSavePayload): Promise<ProcessDef> {
  const res = await apiFetch('/api/processes', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<ProcessDef>(res);
}
/** 更新流程 */
export async function updateProcess(id: string, payload: ProcessSavePayload): Promise<ProcessDef> {
  const res = await apiFetch(`/api/processes/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<ProcessDef>(res);
}
/** 删除流程 */
export async function deleteProcess(id: string): Promise<void> {
  const res = await apiFetch(`/api/processes/${id}`, { method: 'DELETE' });
  await unwrap<void>(res);
}

export async function fetchRoutes(): Promise<Route[]> {
  return unwrap<Route[]>(await apiFetch('/api/routes'));
}

/** 配置版本分页查询（page 从 1 起；targetType 为空 = 全部类型） */
export async function fetchVersions(
  page = 1, size = 10, targetType = '',
): Promise<{ records: VersionRecord[]; total: number }> {
  const qs = new URLSearchParams({ page: String(page), size: String(size) });
  if (targetType) qs.set('targetType', targetType);
  const res = await apiFetch(`/api/versions?${qs.toString()}`);
  return unwrap<{ records: VersionRecord[]; total: number }>(res);
}

/** 版本回滚：复制指定版本内容生成新的已发布版本 */
export async function rollbackVersion(id: string): Promise<VersionRecord> {
  const res = await apiFetch(`/api/versions/${id}/rollback`, { method: 'POST' });
  return unwrap<VersionRecord>(res);
}

// ============================================================
// 监控域：实例 / 告警 / 看板 / 检测点 / 上下游追踪（真实接口）
// 统一约定：后端返回 Result<T>，code === 200 表示成功，data 为业务数据
// ============================================================

/** 实例经过的节点（对应后端 InstanceNodeVO） */
export interface InstanceNodeVO {
  nodeId: string;
  nodeName: string;
  seq: number;
  status: string;
}

/** 上下游追踪结果（对应后端 TraceVO，顺藤摸瓜） */
export interface GraphTrace {
  nodeId: string;
  upstream: GraphNode[];
  downstream: GraphNode[];
}

/** 实例分页查询（page 从 1 起；status 为空 = 全部状态） */
export async function fetchInstances(
  page = 1, size = 10, status = '',
): Promise<{ records: Instance[]; total: number }> {
  const qs = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) qs.set('status', status);
  const res = await apiFetch(`/api/instances?${qs.toString()}`);
  return unwrap<{ records: Instance[]; total: number }>(res);
}

/** 实例经过的节点序列（顺藤摸瓜定位实例当前所在站点） */
export async function fetchInstanceNodes(id: string): Promise<InstanceNodeVO[]> {
  const res = await apiFetch(`/api/instances/${encodeURIComponent(id)}/nodes`);
  return unwrap<InstanceNodeVO[]>(res);
}

/** 告警列表（含已解决，前端负责过滤） */
export async function fetchAlerts(): Promise<AlertItem[]> {
  const res = await apiFetch('/api/alerts');
  return unwrap<AlertItem[]>(res);
}

/** 关闭告警 */
export async function resolveAlert(id: string): Promise<void> {
  const res = await apiFetch(`/api/alerts/${encodeURIComponent(id)}/resolve`, { method: 'POST' });
  await unwrap<void>(res);
}

/** 工单更新请求体（assignee / status / description 均可选） */
export interface TicketUpdatePayload {
  assignee?: string;
  status?: string;
  description?: string;
}

/** 更新工单 */
export async function updateTicket(id: string, payload: TicketUpdatePayload): Promise<void> {
  const res = await apiFetch(`/api/tickets/${encodeURIComponent(id)}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  await unwrap<void>(res);
}

/** 工单列表 */
export async function fetchTickets(): Promise<Ticket[]> {
  return unwrap<Ticket[]>(await apiFetch('/api/tickets'));
}

/** 创建工单 */
export async function createTicket(payload: TicketPayload): Promise<Ticket> {
  const res = await apiFetch('/api/tickets', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<Ticket>(res);
}

/** 新建告警请求体（type / targetType / targetId / message / severity） */
export interface AlertCreatePayload {
  type: AlertItem['type'];
  targetType: string;
  targetId: string;
  message: string;
  severity: AlertItem['severity'];
}

/** 新建告警（后端自动应用等级处置） */
export async function createAlert(payload: AlertCreatePayload): Promise<void> {
  const res = await apiFetch('/api/alerts', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  await unwrap<void>(res);
}

/** 看板统计 */
export async function fetchDashboardStats(): Promise<DashboardStats> {
  const res = await apiFetch('/api/dashboard/stats');
  return unwrap<DashboardStats>(res);
}

/** 站点检测点清单 */
export async function fetchCheckpoints(nodeId: string): Promise<Checkpoint[]> {
  const res = await apiFetch(`/api/checkpoints?nodeId=${encodeURIComponent(nodeId)}`);
  return unwrap<Checkpoint[]>(res);
}
/** 新建检测点 */
export async function createCheckpoint(payload: CheckpointSavePayload): Promise<Checkpoint> {
  const res = await apiFetch('/api/checkpoints', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<Checkpoint>(res);
}
/** 更新检测点 */
export async function updateCheckpoint(id: string, payload: CheckpointSavePayload): Promise<Checkpoint> {
  const res = await apiFetch(`/api/checkpoints/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<Checkpoint>(res);
}
/** 删除检测点 */
export async function deleteCheckpoint(id: string): Promise<void> {
  const res = await apiFetch(`/api/checkpoints/${id}`, { method: 'DELETE' });
  await unwrap<void>(res);
}

/** 立即执行一次检测点 */
export async function runCheckpoint(id: string): Promise<Checkpoint> {
  const res = await apiFetch(`/api/checkpoints/${id}/run`, { method: 'POST' });
  return unwrap<Checkpoint>(res);
}

/** 上下游追踪（顺藤摸瓜：某节点与其上游 / 下游） */
export async function fetchGraphTrace(nodeId: string): Promise<GraphTrace> {
  const res = await apiFetch(`/api/graph/${encodeURIComponent(nodeId)}/trace`);
  return unwrap<GraphTrace>(res);
}

/** 多路径查询结果（对应后端 PathVO：A→B 的一条可达路径） */
export interface GraphPathResult {
  nodeIds: string[];
  nodeNames: string[];
  length: number;
}

/** A→B 多路径查询（maxDepth 默认 8，返回全部可达路径） */
export async function queryGraphPaths(from: string, to: string, maxDepth = 8): Promise<GraphPathResult[]> {
  const qs = new URLSearchParams({ from, to, maxDepth: String(maxDepth) });
  const res = await apiFetch(`/api/graph/path?${qs.toString()}`);
  return unwrap<GraphPathResult[]>(res);
}

/** 影响面（对应后端 ImpactVO：下游节点 / 受影响实例 / 受影响路线） */
export interface ImpactResult {
  downstream: GraphNode[];
  affectedInstances: Instance[];
  affectedRoutes: Route[];
}

/** 节点影响面分析 */
export async function fetchImpact(nodeId: string): Promise<ImpactResult> {
  const res = await apiFetch(`/api/graph/${encodeURIComponent(nodeId)}/impact`);
  return unwrap<ImpactResult>(res);
}

/** 健康探活（真实接口，非 mock）：返回运行模式 / 数据库状态 / 版本 */
export async function fetchHealth(): Promise<HealthInfo> {
  const res = await apiFetch('/api/health');
  if (!res.ok) throw new Error(`health http ${res.status}`);
  const json = await res.json();
  return json.data as HealthInfo;
}

// ============================================================
// 数据池：连接器管理（真实接口，非 mock）
// 统一约定：后端返回 Result<T>，code === 200 表示成功，data 为业务数据
// ============================================================

/** 统一解包后端 Result<T>：code !== 200 抛错，否则返回 data */
async function unwrap<T>(res: Response): Promise<T> {
  const json = await res.json();
  if (json.code !== 200) throw new Error(json.message);
  return json.data as T;
}

/** 分页查询连接器（page 从 1 起；keyword 匹配 name/host/databaseName） */
export async function fetchConnectors(
  page = 1, size = 10, keyword = '',
): Promise<{ records: DataSourceConnector[]; total: number }> {
  const res = await apiFetch(`/api/connectors?page=${page}&size=${size}&keyword=${encodeURIComponent(keyword)}`);
  return unwrap<{ records: DataSourceConnector[]; total: number }>(res);
}

/** 连接器详情 */
export async function fetchConnector(id: string): Promise<DataSourceConnector> {
  const res = await apiFetch(`/api/connectors/${id}`);
  return unwrap<DataSourceConnector>(res);
}

/** 新建连接器 */
export async function createConnector(payload: ConnectorSavePayload): Promise<DataSourceConnector> {
  const res = await apiFetch('/api/connectors', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<DataSourceConnector>(res);
}

/** 更新连接器（password 留空 = 不改密码） */
export async function updateConnector(id: string, payload: ConnectorSavePayload): Promise<DataSourceConnector> {
  const res = await apiFetch(`/api/connectors/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<DataSourceConnector>(res);
}

/** 删除连接器 */
export async function deleteConnector(id: string): Promise<void> {
  const res = await apiFetch(`/api/connectors/${id}`, { method: 'DELETE' });
  await unwrap<void>(res);
}

/** 测试连接连通性（ok / 延迟 / 数据库版本 / 失败原因） */
export async function testConnector(id: string): Promise<ConnectorTestResult> {
  const res = await apiFetch(`/api/connectors/${id}/test`, { method: 'POST' });
  return unwrap<ConnectorTestResult>(res);
}

/** 设为当前连接（保证 is_active 全局唯一） */
export async function activateConnector(id: string): Promise<void> {
  const res = await apiFetch(`/api/connectors/${id}/activate`, { method: 'POST' });
  await unwrap<void>(res);
}

/** 浏览库表清单 */
export async function fetchConnectorTables(id: string): Promise<TableInfo[]> {
  const res = await apiFetch(`/api/connectors/${id}/tables`);
  return unwrap<TableInfo[]>(res);
}

/** 表数据预览（前 50 行） */
export async function fetchTablePreview(id: string, table: string): Promise<TablePreview> {
  const res = await apiFetch(`/api/connectors/${id}/tables/${encodeURIComponent(table)}/preview`);
  return unwrap<TablePreview>(res);
}

// ============================================================
// CMDB 连接器：同步候选 / 候选清单 / 导入（真实接口）
// ============================================================

/** CMDB 连接器：同步候选节点（返回候选数） */
export async function syncConnector(id: string): Promise<number> {
  const res = await apiFetch(`/api/connectors/${encodeURIComponent(id)}/sync`, { method: 'POST' });
  return unwrap<number>(res);
}

/** CMDB 连接器：候选节点清单 */
export async function fetchConnectorCandidates(id: string): Promise<CandidateNode[]> {
  const res = await apiFetch(`/api/connectors/${encodeURIComponent(id)}/candidates`);
  return unwrap<CandidateNode[]>(res);
}

/** CMDB 连接器：导入全部候选节点（返回导入数） */
export async function importConnectorCandidates(id: string): Promise<number> {
  const res = await apiFetch(`/api/connectors/${encodeURIComponent(id)}/import`, { method: 'POST' });
  return unwrap<number>(res);
}

// ============================================================
// 开放 API（外部系统集成，仅管理员可查看）
// ============================================================

/** 开放 API 接口清单项 */
export interface OpenApiEndpoint {
  method: string;
  path: string;
  desc: string;
}

/** 开放 API 信息 */
export interface OpenApiInfo {
  token: string;
  basePath: string;
  endpoints: OpenApiEndpoint[];
}

/** 开放 API 信息（403 时提示仅管理员可查看） */
export async function fetchOpenApiInfo(): Promise<OpenApiInfo> {
  const res = await apiFetch('/api/system/openapi');
  if (res.status === 403) throw new Error('仅管理员可查看');
  return unwrap<OpenApiInfo>(res);
}

// ============================================================
// 图来源 · 引擎分析（G3）：GET /api/analyze?connectorId=
// 返回 EngineDraftVO（草稿节点/边 + 候选单据 + 流程模板）
// ============================================================

/** 引擎分析：对指定 DB 连接器扫描单据模式，产出草稿（需登录） */
export async function fetchEngineAnalyze(connectorId: string): Promise<EngineDraft> {
  const res = await apiFetch(`/api/analyze?connectorId=${encodeURIComponent(connectorId)}`);
  return unwrap<EngineDraft>(res);
}

// ============================================================
// 图来源 · 大模型细化（G4）：POST /api/analyze/refine
// 输入连接器 id（后端复跑引擎取草稿再调大模型），返回引擎骨架 + 细化增量；
// 未配置大模型 API Key 时 provider='noop'，返回引擎原稿兜底。
// ============================================================

/** 大模型细化：引擎草稿 → 语义补全（需登录） */
export async function postEngineRefine(connectorId: string): Promise<EngineRefineResult> {
  const res = await apiFetch('/api/analyze/refine', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ connectorId: Number(connectorId) }),
  });
  return unwrap<EngineRefineResult>(res);
}

// ============================================================
// 图来源 · 人工校正闭环（G5）
// ============================================================

/** 提交一条校正记录 */
export async function submitCorrection(data: CorrectionPayload): Promise<CorrectionRecord> {
  const res = await apiFetch('/api/corrections', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return unwrap<CorrectionRecord>(res);
}

/** 查询某对象的校正历史 */
export async function listCorrections(params: { targetType: string; targetId: string }): Promise<CorrectionRecord[]> {
  const qs = new URLSearchParams({ targetType: params.targetType, targetId: params.targetId });
  const res = await apiFetch(`/api/corrections?${qs.toString()}`);
  return unwrap<CorrectionRecord[]>(res);
}

/** 确认一条校正生效 */
export async function confirmCorrection(id: number): Promise<void> {
  const res = await apiFetch(`/api/corrections/${id}/confirm`, { method: 'POST' });
  await unwrap<void>(res);
}

/** 模式库列表（可按类型/关键词过滤） */
export async function listPatterns(params: { patternType?: string; keyword?: string } = {}): Promise<Pattern[]> {
  const qs = new URLSearchParams();
  if (params.patternType) qs.set('patternType', params.patternType);
  if (params.keyword) qs.set('keyword', params.keyword);
  const res = await apiFetch(`/api/patterns?${qs.toString()}`);
  return unwrap<Pattern[]>(res);
}

/** 沉淀为模式 */
export async function createPattern(data: PatternPayload): Promise<Pattern> {
  const res = await apiFetch('/api/patterns', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return unwrap<Pattern>(res);
}
