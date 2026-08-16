/* ============================================================
   DataLink 平台 · 数据访问层
   关系网 / 流程、数据池（连接器管理）、监控域（实例 / 告警 / 看板 /
   检测点 / 上下游追踪）与健康探活均已接入真实 HTTP 接口；
   版本记录仍为 Mock 数据（演示用）。
   接口路径与 v1.1 项目文档书第 5 章模块一致。
   ============================================================ */

import { mockVersions } from './mockData';
import type {
  AlertItem, Checkpoint, ConnectorSavePayload, ConnectorTestResult, DashboardStats,
  DataSourceConnector, GraphEdge, GraphNode, HealthInfo, Instance,
  ProcessDef, Route, TableInfo, TablePreview, VersionRecord,
} from '@/types';

/** 模拟网络延迟，便于观察加载状态（接入后端后删除） */
const delay = (ms = 120) => new Promise((r) => setTimeout(r, ms));

/** 关系网 / 流程数据（真实接口，返回 Result<T>，code === 200 成功） */
export async function fetchNodes(): Promise<GraphNode[]> {
  return unwrap<GraphNode[]>(await fetch('/api/nodes'));
}
export async function fetchEdges(): Promise<GraphEdge[]> {
  return unwrap<GraphEdge[]>(await fetch('/api/edges'));
}
export async function fetchProcesses(): Promise<ProcessDef[]> {
  return unwrap<ProcessDef[]>(await fetch('/api/processes'));
}
export async function fetchRoutes(): Promise<Route[]> {
  return unwrap<Route[]>(await fetch('/api/routes'));
}
export async function fetchVersions(): Promise<VersionRecord[]> { await delay(); return mockVersions; }

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
  const res = await fetch(`/api/instances?${qs.toString()}`);
  return unwrap<{ records: Instance[]; total: number }>(res);
}

/** 实例经过的节点序列（顺藤摸瓜定位实例当前所在站点） */
export async function fetchInstanceNodes(id: string): Promise<InstanceNodeVO[]> {
  const res = await fetch(`/api/instances/${encodeURIComponent(id)}/nodes`);
  return unwrap<InstanceNodeVO[]>(res);
}

/** 告警列表（含已解决，前端负责过滤） */
export async function fetchAlerts(): Promise<AlertItem[]> {
  const res = await fetch('/api/alerts');
  return unwrap<AlertItem[]>(res);
}

/** 关闭告警 */
export async function resolveAlert(id: string): Promise<void> {
  const res = await fetch(`/api/alerts/${encodeURIComponent(id)}/resolve`, { method: 'POST' });
  await unwrap<void>(res);
}

/** 看板统计 */
export async function fetchDashboardStats(): Promise<DashboardStats> {
  const res = await fetch('/api/dashboard/stats');
  return unwrap<DashboardStats>(res);
}

/** 站点检测点清单 */
export async function fetchCheckpoints(nodeId: string): Promise<Checkpoint[]> {
  const res = await fetch(`/api/checkpoints?nodeId=${encodeURIComponent(nodeId)}`);
  return unwrap<Checkpoint[]>(res);
}

/** 上下游追踪（顺藤摸瓜：某节点与其上游 / 下游） */
export async function fetchGraphTrace(nodeId: string): Promise<GraphTrace> {
  const res = await fetch(`/api/graph/${encodeURIComponent(nodeId)}/trace`);
  return unwrap<GraphTrace>(res);
}

/** 健康探活（真实接口，非 mock）：返回运行模式 / 数据库状态 / 版本 */
export async function fetchHealth(): Promise<HealthInfo> {
  const res = await fetch('/api/health');
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
  const res = await fetch(`/api/connectors?page=${page}&size=${size}&keyword=${encodeURIComponent(keyword)}`);
  return unwrap<{ records: DataSourceConnector[]; total: number }>(res);
}

/** 连接器详情 */
export async function fetchConnector(id: string): Promise<DataSourceConnector> {
  const res = await fetch(`/api/connectors/${id}`);
  return unwrap<DataSourceConnector>(res);
}

/** 新建连接器 */
export async function createConnector(payload: ConnectorSavePayload): Promise<DataSourceConnector> {
  const res = await fetch('/api/connectors', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<DataSourceConnector>(res);
}

/** 更新连接器（password 留空 = 不改密码） */
export async function updateConnector(id: string, payload: ConnectorSavePayload): Promise<DataSourceConnector> {
  const res = await fetch(`/api/connectors/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  return unwrap<DataSourceConnector>(res);
}

/** 删除连接器 */
export async function deleteConnector(id: string): Promise<void> {
  const res = await fetch(`/api/connectors/${id}`, { method: 'DELETE' });
  await unwrap<void>(res);
}

/** 测试连接连通性（ok / 延迟 / 数据库版本 / 失败原因） */
export async function testConnector(id: string): Promise<ConnectorTestResult> {
  const res = await fetch(`/api/connectors/${id}/test`, { method: 'POST' });
  return unwrap<ConnectorTestResult>(res);
}

/** 设为当前连接（保证 is_active 全局唯一） */
export async function activateConnector(id: string): Promise<void> {
  const res = await fetch(`/api/connectors/${id}/activate`, { method: 'POST' });
  await unwrap<void>(res);
}

/** 浏览库表清单 */
export async function fetchConnectorTables(id: string): Promise<TableInfo[]> {
  const res = await fetch(`/api/connectors/${id}/tables`);
  return unwrap<TableInfo[]>(res);
}

/** 表数据预览（前 50 行） */
export async function fetchTablePreview(id: string, table: string): Promise<TablePreview> {
  const res = await fetch(`/api/connectors/${id}/tables/${encodeURIComponent(table)}/preview`);
  return unwrap<TablePreview>(res);
}
