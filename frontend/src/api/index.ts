/* ============================================================
   DataLink 平台 · 数据访问层
   当前全部使用 Mock 数据（演示）。后端（Spring Boot）就绪后，
   将下方函数逐个替换为真实 HTTP 调用（fetch / axios），
   接口路径与 v1.1 项目文档书第 5 章模块一致。
   ============================================================ */

import {
  mockAlerts, mockConnectors, mockDashboardStats, mockEdges,
  mockInstances, mockNodes, mockProcesses, mockRoutes, mockVersions,
} from './mockData';
import type {
  AlertItem, Connector, DashboardStats, GraphEdge, GraphNode,
  Instance, ProcessDef, Route, VersionRecord,
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
export async function fetchConnectors(): Promise<Connector[]> { await delay(); return mockConnectors; }
export async function fetchDashboardStats(): Promise<DashboardStats> { await delay(); return mockDashboardStats; }

// 后端接入后的替换示例：
// export async function fetchNodes(): Promise<GraphNode[]> {
//   return (await fetch('/api/nodes')).json();
// }
