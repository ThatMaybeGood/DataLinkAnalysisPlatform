/* ============================================================
   DataLink 平台 · 数据访问层
   大部分仍为 Mock 数据（演示，供 Dashboard 等页面使用）；
   数据池（连接器管理）与健康探活已接入真实 HTTP 接口。
   接口路径与 v1.1 项目文档书第 5 章模块一致。
   ============================================================ */

import {
  mockAlerts, mockDashboardStats, mockEdges,
  mockInstances, mockNodes, mockProcesses, mockRoutes, mockVersions,
} from './mockData';
import type {
  AlertItem, ConnectorSavePayload, ConnectorTestResult, DashboardStats,
  DataSourceConnector, GraphEdge, GraphNode, HealthInfo, Instance,
  ProcessDef, Route, TableInfo, TablePreview, VersionRecord,
} from '@/types';

/** 模拟网络延迟，便于观察加载状态（接入后端后删除） */
const delay = (ms = 120) => new Promise((r) => setTimeout(r, ms));

export async function fetchNodes(): Promise<GraphNode[]> { await delay(); return mockNodes; }
export async function fetchEdges(): Promise<GraphEdge[]> { await delay(); return mockEdges; }
export async function fetchProcesses(): Promise<ProcessDef[]> { await delay(); return mockProcesses; }
export async function fetchRoutes(): Promise<Route[]> { await delay(); return mockRoutes; }
export async function fetchInstances(): Promise<Instance[]> { await delay(); return mockInstances; }
export async function fetchAlerts(): Promise<AlertItem[]> { await delay(); return mockAlerts; }
export async function fetchVersions(): Promise<VersionRecord[]> { await delay(); return mockVersions; }
export async function fetchDashboardStats(): Promise<DashboardStats> { await delay(); return mockDashboardStats; }

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
