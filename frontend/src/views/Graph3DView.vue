<script setup lang="ts">
/** M4 · 3D 视图：3d-force-graph 立体关系网（粒子流动 / 类型配色 / 点击高亮 / 路线高亮） */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import ForceGraph3D from '3d-force-graph';
import type { ForceGraph3DInstance } from '3d-force-graph';
import type { LinkObject, NodeObject } from '3d-force-graph';
import { fetchEdges, fetchNodes, fetchRoutes } from '@/api';
import type { GraphEdge, GraphNode, Route } from '@/types';
import { nodeTypeLabel, statusLabel } from '@/api/mockData';
import Icon from '@/components/Icon.vue';

/** 3D 节点（在 NodeObject 基础上附加业务字段，val 控制体积） */
type FgNode = NodeObject & {
  id: string;
  name: string;
  nodeType: string;
  level: string;
  status: string;
  val: number;
  color: string;
};
/** 3D 边（source/target 初始为 id，three-forcegraph 渲染后替换为节点对象） */
type FgLink = LinkObject<FgNode> & { id: string };

/** 节点类型配色（与 GraphView 一致，暗色下微调亮度） */
const nodeColorMap: Record<string, string> = {
  SYSTEM: '#3b82f6', SUBSYSTEM: '#60a5fa',
  DATABASE: '#8b5cf6', TABLE: '#a78bfa',
  DEPARTMENT: '#06b6d4', ROLE: '#22d3ee',
  ACTION: '#f59e0b', EVENT: '#fb923c',
  DEVICE: '#34d399', WORKSTATION: '#2dd4bf',
};
const FALLBACK_NODE_COLOR = '#94a3b8';
const DIM_NODE_COLOR = 'rgba(148,163,184,0.1)';   // 非高亮节点（透明度 ~0.1）
const DIM_LINK_COLOR = 'rgba(148,163,184,0.08)';  // 非高亮边
const BASE_LINK_COLOR = 'rgba(148,163,184,0.42)'; // 默认边
const HIGHLIGHT_LINK_COLOR = 'rgba(56,189,248,0.9)'; // 高亮边（青蓝主色）
const ROUTE_NODE_COLOR = '#38bdf8';                // 路线节点高亮主色
/** 图例（仅展示当前数据使用的核心类型） */
const legendKeys = ['SYSTEM', 'DATABASE', 'DEPARTMENT', 'ACTION'];

/** 等级 → 体积权重 */
function levelVal(level: string): number {
  if (level === 'L1') return 30;
  if (level === 'L2') return 22;
  if (level === 'L3') return 14;
  return 8;
}

const containerRef = ref<HTMLDivElement | null>(null);
const loading = ref(true);
const loadError = ref('');
const autoRotate = ref(true);

const nodes = ref<GraphNode[]>([]);
const edges = ref<GraphEdge[]>([]);
const routes = ref<Route[]>([]);

const selectedNodeId = ref<string | null>(null); // 节点高亮：选中节点
const activeRouteId = ref('');                    // 路线高亮：选中路线
const adjacencyMap = ref(new Map<string, Set<string>>());

const routeOptions = computed(() => routes.value);
/** 节点高亮集合：选中节点 + 其相邻节点 */
const nodeFocusSet = computed(() => {
  const set = new Set<string>();
  const sel = selectedNodeId.value;
  if (sel) {
    set.add(sel);
    adjacencyMap.value.get(sel)?.forEach((id) => set.add(id));
  }
  return set;
});
/** 路线节点集合 */
const routeNodeSet = computed(() => {
  const r = routes.value.find((x) => x.id === activeRouteId.value);
  return new Set(r?.nodeIds ?? []);
});
/** 路线相邻边 key（source->target） */
const routeLinkKeys = computed(() => {
  const r = routes.value.find((x) => x.id === activeRouteId.value);
  const keys = new Set<string>();
  if (!r) return keys;
  for (let i = 0; i < r.nodeIds.length - 1; i++) {
    keys.add(`${r.nodeIds[i]}->${r.nodeIds[i + 1]}`);
  }
  return keys;
});

let graph: ForceGraph3DInstance | null = null;
let resizeObserver: ResizeObserver | null = null;
let resizeRaf = 0;

function linkEndpointId(v: string | number | NodeObject | undefined): string {
  if (v === undefined) return '';
  return typeof v === 'object' ? String(v.id ?? '') : String(v);
}

/** 节点着色：默认按类型；高亮时非相邻节点淡化 */
function nodeColorFn(n: NodeObject): string {
  const node = n as FgNode;
  if (selectedNodeId.value) {
    return nodeFocusSet.value.has(node.id) ? node.color : DIM_NODE_COLOR;
  }
  if (activeRouteId.value) {
    return routeNodeSet.value.has(node.id) ? ROUTE_NODE_COLOR : 'rgba(148,163,184,0.14)';
  }
  return node.color;
}

/** 边着色：默认淡灰；高亮相邻边 / 路线边为青蓝主色 */
function linkColorFn(l: LinkObject<NodeObject>): string {
  const src = linkEndpointId(l.source);
  const tgt = linkEndpointId(l.target);
  if (selectedNodeId.value) {
    const sel = selectedNodeId.value;
    const linked =
      (src === sel && nodeFocusSet.value.has(tgt)) || (tgt === sel && nodeFocusSet.value.has(src));
    return linked ? HIGHLIGHT_LINK_COLOR : DIM_LINK_COLOR;
  }
  if (activeRouteId.value) {
    const onRoute =
      routeLinkKeys.value.has(`${src}->${tgt}`) || routeLinkKeys.value.has(`${tgt}->${src}`);
    return onRoute ? HIGHLIGHT_LINK_COLOR : 'rgba(148,163,184,0.1)';
  }
  return BASE_LINK_COLOR;
}

/** 悬停标注：名称 + 类型 + 等级 + 状态 */
function nodeLabelFn(n: NodeObject): string {
  const node = n as FgNode;
  const type = nodeTypeLabel[node.nodeType] ?? node.nodeType;
  const status = statusLabel[node.status] ?? node.status;
  return `<div style="padding:5px 9px;font:12px/1.6 var(--font-sans),sans-serif;color:#e2e8f0;white-space:nowrap">
    <div style="font-weight:700">${node.name}</div>
    <div style="color:#94a3b8">${type} · ${node.level} · ${status}</div>
  </div>`;
}

/** 重新触发渲染：高亮状态变化后按需刷新配色 */
function applyVisuals() {
  if (!graph) return;
  graph.nodeColor(nodeColorFn);
  graph.linkColor(linkColorFn);
  graph.linkDirectionalArrowColor(linkColorFn);
  graph.linkDirectionalParticleColor(linkColorFn);
}

/** 点击节点：高亮相邻节点/边，再次点击还原；与路线高亮互斥 */
function handleNodeClick(node: NodeObject) {
  const fg = node as FgNode;
  if (selectedNodeId.value === fg.id) {
    selectedNodeId.value = null;
  } else {
    selectedNodeId.value = fg.id;
    activeRouteId.value = '';
  }
  applyVisuals();
}

function toggleAutoRotate() {
  autoRotate.value = !autoRotate.value;
  const controls = graph?.controls() as unknown as { autoRotate: boolean } | undefined;
  if (controls) controls.autoRotate = autoRotate.value;
}

function resetView() {
  graph?.cameraPosition({ x: 230, y: 170, z: 260 }, { x: 0, y: 0, z: 0 }, 800);
}

/** 路线下拉：高亮该路线节点/边并聚焦（其余淡化） */
function onRouteChange() {
  selectedNodeId.value = null;
  applyVisuals();
  const r = routes.value.find((x) => x.id === activeRouteId.value);
  if (r && graph) {
    const ids = new Set(r.nodeIds);
    graph.zoomToFit(800, 70, (n) => ids.has(String(n.id)));
  }
}

function destroyGraph() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  if (resizeRaf) window.cancelAnimationFrame(resizeRaf);
  resizeRaf = 0;
  graph?._destructor();
  graph = null;
}

/** 组装 3D 图数据：节点按等级缩放（val）、按类型配色；边含流向粒子与方向箭头 */
function initGraph() {
  destroyGraph();
  if (!containerRef.value) return;

  const adj = new Map<string, Set<string>>();
  const fgNodes: FgNode[] = nodes.value.map((n) => {
    const node: FgNode = {
      id: n.id,
      name: n.name,
      nodeType: n.nodeType,
      level: n.level,
      status: n.status,
      val: levelVal(n.level),
      color: nodeColorMap[n.nodeType] ?? FALLBACK_NODE_COLOR,
    };
    return node;
  });
  const fgLinks: FgLink[] = edges.value.map((e) => {
    if (!adj.has(e.source)) adj.set(e.source, new Set());
    adj.get(e.source)!.add(e.target);
    if (!adj.has(e.target)) adj.set(e.target, new Set());
    adj.get(e.target)!.add(e.source);
    return { id: e.id, source: e.source, target: e.target };
  });
  adjacencyMap.value = adj;

  const el = containerRef.value;
  const g = new ForceGraph3D(el);
  graph = g;
  g
    .width(el.clientWidth || 800)
    .height(el.clientHeight || 600)
    .backgroundColor('#0b1020')
    .showNavInfo(false)
    .nodeRelSize(4.5)
    .nodeResolution(16)
    .nodeOpacity(1)
    .nodeLabel(nodeLabelFn)
    .nodeColor(nodeColorFn)
    .linkColor(linkColorFn)
    .linkWidth(1.3)
    .linkOpacity(1)
    .linkDirectionalArrowLength(3)
    .linkDirectionalArrowRelPos(0.92)
    .linkDirectionalArrowColor(linkColorFn)
    .linkDirectionalParticles(2)
    .linkDirectionalParticleSpeed(0.006)
    .linkDirectionalParticleWidth(1.6)
    .linkDirectionalParticleColor(linkColorFn)
    .enableNodeDrag(true)
    .onNodeClick(handleNodeClick)
    .graphData({ nodes: fgNodes, links: fgLinks });

  const controls = g.controls() as unknown as { autoRotate: boolean; autoRotateSpeed?: number };
  controls.autoRotate = autoRotate.value;
  controls.autoRotateSpeed = 1.1;
  g.cameraPosition({ x: 230, y: 170, z: 260 }, { x: 0, y: 0, z: 0 }, 0);

  // 容器尺寸变化自适应
  resizeObserver = new ResizeObserver(() => {
    if (resizeRaf) window.cancelAnimationFrame(resizeRaf);
    resizeRaf = window.requestAnimationFrame(() => {
      if (!el) return;
      g.width(el.clientWidth || 800).height(el.clientHeight || 600);
    });
  });
  resizeObserver.observe(el);
}

async function loadData() {
  loading.value = true;
  loadError.value = '';
  try {
    const [n, e, r] = await Promise.all([fetchNodes(), fetchEdges(), fetchRoutes()]);
    nodes.value = n;
    edges.value = e;
    routes.value = r;
    await nextTick();
    initGraph();
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '3D 视图数据加载失败';
  } finally {
    loading.value = false;
  }
}

function retry() {
  void loadData();
}

onMounted(() => {
  void loadData();
});

onBeforeUnmount(() => {
  destroyGraph();
});
</script>

<template>
  <div class="graph3d-page">
    <!-- 顶部工具条：自动旋转 / 重置视角 / 路线下拉 -->
    <div class="graph3d-toolbar">
      <div class="toolbar-group">
        <button
          class="btn toolbar-btn" :class="{ 'toolbar-btn--active': autoRotate }"
          @click="toggleAutoRotate"
        >
          <Icon name="refresh" :size="14" />自动旋转
        </button>
        <button class="btn toolbar-btn" @click="resetView">
          <Icon name="target" :size="14" />重置视角
        </button>
      </div>
      <div class="toolbar-group toolbar-group--right">
        <span class="toolbar-hint">点击节点高亮相邻关系，再次点击还原</span>
        <select v-model="activeRouteId" class="select route-select" @change="onRouteChange">
          <option value="">全路网</option>
          <option v-for="r in routeOptions" :key="r.id" :value="r.id">{{ r.name }}</option>
        </select>
      </div>
    </div>

    <!-- 画布 -->
    <div class="graph3d-body">
      <div v-if="loading" class="graph3d-status">加载 3D 关系网数据…</div>
      <div v-else-if="loadError" class="graph3d-status graph3d-status--error">
        <span>加载失败：{{ loadError }}</span>
        <button class="retry-btn" @click="retry">重试</button>
      </div>

      <div ref="containerRef" class="graph3d-canvas" />

      <!-- 图例 -->
      <div class="graph3d-legend">
        <div class="legend-title">节点类型</div>
        <div class="legend-item" v-for="key in legendKeys" :key="key">
          <span class="legend-dot" :style="{ background: nodeColorMap[key] }" />{{ nodeTypeLabel[key] ?? key }}
        </div>
        <div class="legend-divider" />
        <div class="legend-item"><span class="legend-dot" :style="{ background: ROUTE_NODE_COLOR }" />路线高亮</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.graph3d-page {
  display: flex; flex-direction: column;
  height: calc(100vh - var(--topbar-h));
  background: #0b1020;
  color: #dbe4f5;
}

.graph3d-toolbar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 16px; flex-shrink: 0;
  background: rgba(11, 16, 32, .95);
  border-bottom: 1px solid rgba(56, 189, 248, .25);
}
.toolbar-group { display: flex; align-items: center; gap: 10px; }
.toolbar-group--right { justify-content: flex-end; }
.toolbar-btn {
  height: 30px; padding: 0 12px; border-radius: 6px;
  background: rgba(255, 255, 255, .05); border: 1px solid rgba(56, 189, 248, .3);
  color: #c3d0e8; font-size: 12.5px; cursor: pointer;
  display: inline-flex; align-items: center; gap: 6px; transition: all .15s;
}
.toolbar-btn:hover { border-color: rgba(56, 189, 248, .65); color: #fff; }
.toolbar-btn--active {
  background: rgba(56, 189, 248, .18); border-color: rgba(56, 189, 248, .7); color: #7dd3fc;
  box-shadow: 0 0 12px rgba(56, 189, 248, .25);
}
.toolbar-hint { font-size: 12px; color: #7583a4; }
.route-select {
  width: 180px; height: 30px; font-size: 12.5px;
  background: rgba(255, 255, 255, .06); color: #dbe4f5;
  border: 1px solid rgba(56, 189, 248, .3); border-radius: 6px; padding: 0 10px;
}
.route-select:focus { outline: none; border-color: rgba(56, 189, 248, .7); box-shadow: 0 0 0 3px rgba(56, 189, 248, .15); }
.route-select option { background: #0d1424; color: #dbe4f5; }

.graph3d-body {
  position: relative; flex: 1; min-height: 0; overflow: hidden;
  background:
    radial-gradient(ellipse at 50% 0%, rgba(56, 189, 248, .07), transparent 55%),
    #0b1020;
}
.graph3d-canvas { position: absolute; inset: 0; }

.graph3d-status {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  z-index: 6; display: flex; align-items: center; gap: 12px;
  padding: 12px 18px; border-radius: 8px;
  background: rgba(13, 20, 36, .92); border: 1px solid rgba(56, 189, 248, .3);
  color: #c7d2e8; font-size: 13px; box-shadow: 0 8px 28px rgba(13, 20, 36, .35);
}
.graph3d-status--error { color: #fca5a5; }
.retry-btn {
  height: 28px; padding: 0 12px; border-radius: 6px; border: 1px solid rgba(56, 189, 248, .4);
  background: rgba(56, 189, 248, .15); color: #7dd3fc; font-size: 12px; cursor: pointer;
  transition: opacity .15s;
}
.retry-btn:hover { opacity: .88; }

.graph3d-legend {
  position: absolute; left: 14px; bottom: 14px; z-index: 4;
  background: rgba(13, 20, 36, .88); border: 1px solid rgba(56, 189, 248, .25);
  border-radius: 8px; padding: 10px 12px; font-size: 12px; color: #c7d2e8;
  box-shadow: 0 8px 28px rgba(13, 20, 36, .35);
}
.legend-title { font-size: 11px; color: #7583a4; letter-spacing: .06em; margin-bottom: 6px; }
.legend-item { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 6px currentColor; }
.legend-divider { border-top: 1px solid rgba(56, 189, 248, .2); margin: 6px 0; }
</style>
