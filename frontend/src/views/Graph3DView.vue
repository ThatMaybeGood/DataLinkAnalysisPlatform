<script setup lang="ts">
/** M4 · 3D 视图：星系感空间关系网
 *  度数定大小 / 力导向聚簇 / 星空背景 / 点击高亮 / 路线聚焦 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue';
import ForceGraph3D from '3d-force-graph';
import type { ForceGraph3DInstance } from '3d-force-graph';
import type { LinkObject, NodeObject } from '3d-force-graph';
import { fetchEdges, fetchNodes, fetchRoutes } from '@/api';
import type { GraphEdge, GraphNode, Route } from '@/types';
import { nodeTypeLabel, statusLabel } from '@/api/mockData';
import Icon from '@/components/Icon.vue';

/** 3D 节点（degree 为关联度，val 控制体积） */
type FgNode = NodeObject & {
  id: string;
  name: string;
  nodeType: string;
  level: string;
  status: string;
  degree: number; // 关联度（度数）
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
const DIM_NODE_COLOR = 'rgba(148,163,184,0.1)';       // 点击后非高亮节点
const ROUTE_DIM_NODE_COLOR = 'rgba(148,163,184,0.12)'; // 路线聚焦非路线节点
const DIM_LINK_COLOR = 'rgba(120,150,190,0.08)';      // 点击后非高亮边
const BASE_LINK_COLOR = 'rgba(140,200,255,0.30)';     // 默认边：半透明发光青白
const HIGHLIGHT_LINK_COLOR = 'rgba(103,232,249,0.95)'; // 点击高亮边
const ROUTE_LINK_COLOR = 'rgba(125,211,252,0.95)';    // 路线边
const ROUTE_DIM_LINK_COLOR = 'rgba(148,163,184,0.06)'; // 路线聚焦非路线边
const ROUTE_NODE_COLOR = '#38bdf8';                   // 路线节点高亮主色
const HUB_DEGREE = 4;                                 // 度数 ≥ 该值视为「枢纽」（当前数据 n6/n8）
/** 图例（仅展示当前数据使用的核心类型） */
const legendKeys = ['SYSTEM', 'DATABASE', 'DEPARTMENT', 'ACTION'];

/** hex 颜色向白混合（枢纽节点提亮、光晕感） */
function lightenColor(hex: string, amt: number): string {
  const c = parseInt(hex.slice(1), 16);
  const r = Math.round(((c >> 16) & 255) + (255 - ((c >> 16) & 255)) * amt);
  const g = Math.round(((c >> 8) & 255) + (255 - ((c >> 8) & 255)) * amt);
  const b = Math.round((c & 255) + (255 - (c & 255)) * amt);
  return `rgb(${r},${g},${b})`;
}
const HUB_NODE_COLOR = lightenColor(nodeColorMap.SYSTEM, 0.35); // 图例「枢纽」用色

/** 度数 → 体积：3D 半径 = cbrt(val) * nodeRelSize，
 *  超线性放大让「枢纽 vs 叶子」差异肉眼可见（最小可点、最大不过分） */
function degreeVal(degree: number): number {
  return 6 + Math.pow(degree, 2) * 7;
}

/** 生成星空背景：一组随机径向渐变星点（深色星云底上微亮星点） */
function buildStarfield(count = 80): string {
  const layers: string[] = [];
  for (let i = 0; i < count; i++) {
    const x = (Math.random() * 100).toFixed(2);
    const y = (Math.random() * 100).toFixed(2);
    const size = Math.random() < 0.82 ? 0.7 + Math.random() : 1.6 + Math.random() * 1.2;
    const alpha = 0.3 + Math.random() * 0.6;
    const tint = Math.random() < 0.8 ? '255,255,255' : Math.random() < 0.5 ? '170,205,255' : '205,180,255';
    layers.push(
      `radial-gradient(circle at ${x}% ${y}%, rgba(${tint},${alpha.toFixed(2)}) 0, rgba(${tint},0) ${size.toFixed(2)}px)`,
    );
  }
  return layers.join(', ');
}
const starfieldStyle = ref(buildStarfield());

const containerRef = ref<HTMLDivElement | null>(null);
const loading = ref(true);
const loadError = ref('');
const autoRotate = ref(true);

const nodes = ref<GraphNode[]>([]);
const edges = ref<GraphEdge[]>([]);
const routes = ref<Route[]>([]);

const selectedNodeId = ref<string | null>(null); // 点击节点高亮
const activeRouteId = ref('');                    // 路线聚焦
const adjacencyMap = ref(new Map<string, Set<string>>());

const routeOptions = computed(() => routes.value);
/** 点击高亮集合：选中节点 + 其相邻节点 */
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

/** 边是否属于当前聚焦路线（双向） */
function linkOnRoute(src: string, tgt: string): boolean {
  return routeLinkKeys.value.has(`${src}->${tgt}`) || routeLinkKeys.value.has(`${tgt}->${src}`);
}

/** 节点着色：默认按类型（枢纽提亮）；点击/路线聚焦时其余淡化 */
function nodeColorFn(n: NodeObject): string {
  const node = n as FgNode;
  if (selectedNodeId.value) {
    return nodeFocusSet.value.has(node.id) ? node.color : DIM_NODE_COLOR;
  }
  if (activeRouteId.value) {
    return routeNodeSet.value.has(node.id) ? ROUTE_NODE_COLOR : ROUTE_DIM_NODE_COLOR;
  }
  return node.color;
}

/** 节点体积：路线聚焦时路线节点放大、其余缩小 */
function nodeValFn(n: NodeObject): number {
  const node = n as FgNode;
  if (activeRouteId.value) {
    return routeNodeSet.value.has(node.id) ? node.val * 1.6 + 15 : node.val * 0.4;
  }
  return node.val;
}

/** 边着色：默认发光青白；高亮/路线边更亮，其余淡化 */
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
    return linkOnRoute(src, tgt) ? ROUTE_LINK_COLOR : ROUTE_DIM_LINK_COLOR;
  }
  return BASE_LINK_COLOR;
}

/** 边粗细：路线聚焦时路线边加粗、其余变细 */
function linkWidthFn(l: LinkObject<NodeObject>): number {
  if (activeRouteId.value) {
    return linkOnRoute(linkEndpointId(l.source), linkEndpointId(l.target)) ? 2.8 : 0.5;
  }
  return 1.3;
}

/** 粒子数：路线聚焦时路线边增强、其余关闭 */
function linkParticlesFn(l: LinkObject<NodeObject>): number {
  if (activeRouteId.value) {
    return linkOnRoute(linkEndpointId(l.source), linkEndpointId(l.target)) ? 5 : 0;
  }
  return 2;
}

/** 粒子速度：路线边略快 */
function linkParticleSpeedFn(l: LinkObject<NodeObject>): number {
  if (activeRouteId.value && linkOnRoute(linkEndpointId(l.source), linkEndpointId(l.target))) {
    return 0.012;
  }
  return 0.006;
}

/** 悬停标注：名称 + 类型/等级/状态 + 度数；路线聚焦时附带路线名 */
function nodeLabelFn(n: NodeObject): string {
  const node = n as FgNode;
  const type = nodeTypeLabel[node.nodeType] ?? node.nodeType;
  const status = statusLabel[node.status] ?? node.status;
  const route = routes.value.find((r) => r.id === activeRouteId.value);
  const routeLine = route
    ? `<div style="color:#7dd3fc;font-weight:700">聚焦路线：${route.name}</div>`
    : '';
  return `<div style="padding:5px 9px;font:12px/1.6 var(--font-sans),sans-serif;color:#e2e8f0;white-space:nowrap">
    ${routeLine}
    <div style="font-weight:700">${node.name}</div>
    <div style="color:#94a3b8">${type} · ${node.level} · ${status}</div>
    <div style="color:#7dd3fc">关联度 ${node.degree}</div>
  </div>`;
}

/** 重新触发渲染：点击/路线状态变化后刷新全部 accessor */
function applyVisuals() {
  if (!graph) return;
  graph.nodeVal(nodeValFn);
  graph.nodeColor(nodeColorFn);
  graph.linkColor(linkColorFn);
  graph.linkWidth(linkWidthFn);
  graph.linkDirectionalArrowColor(linkColorFn);
  graph.linkDirectionalParticles(linkParticlesFn);
  graph.linkDirectionalParticleSpeed(linkParticleSpeedFn);
  graph.linkDirectionalParticleColor(linkColorFn);
}

/** 点击节点：高亮相邻节点/边，再次点击还原；与路线聚焦互斥 */
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

/** 路线下拉：聚焦该路线（放大高亮 + 镜头框选居中），清除则恢复全图 */
function onRouteChange() {
  selectedNodeId.value = null;
  applyVisuals();
  if (!graph) return;
  const r = routes.value.find((x) => x.id === activeRouteId.value);
  if (r) {
    const ids = new Set(r.nodeIds);
    graph.zoomToFit(500, 90, (n) => ids.has(String(n.id)));
  } else {
    graph.zoomToFit(650, 60); // 恢复全图
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

/** 星系感初始散布：球面均匀分布 + 按度数径向分层（枢纽向中心聚、叶子在外围） */
function seedGalaxyPosition(node: FgNode, i: number, count: number, maxDeg: number) {
  const outerR = 120;
  const phi = Math.acos(1 - (2 * (i + 0.5)) / count); // 纬度均匀
  const theta = i * 2.399963;                        // 黄金角错开经度
  const radius = outerR * (0.2 + 0.8 * (1 - node.degree / maxDeg));
  node.x = radius * Math.sin(phi) * Math.cos(theta);
  node.y = radius * Math.cos(phi);
  node.z = radius * Math.sin(phi) * Math.sin(theta);
}

/** 组装 3D 图数据：度数定大小 / 力导向聚簇 / 透明画布透出星空 */
function initGraph() {
  destroyGraph();
  if (!containerRef.value) return;

  // 度数 = 节点在 edges 中作为 source/target 出现的次数（关联度）
  const degreeMap = new Map<string, number>();
  edges.value.forEach((e) => {
    degreeMap.set(e.source, (degreeMap.get(e.source) ?? 0) + 1);
    degreeMap.set(e.target, (degreeMap.get(e.target) ?? 0) + 1);
  });
  const maxDeg = Math.max(1, ...[...degreeMap.values()]);

  const adj = new Map<string, Set<string>>();
  const fgNodes: FgNode[] = nodes.value.map((n, i) => {
    const degree = degreeMap.get(n.id) ?? 0;
    const node: FgNode = {
      id: n.id,
      name: n.name,
      nodeType: n.nodeType,
      level: n.level,
      status: n.status,
      degree,
      val: degreeVal(degree),
      color:
        degree >= HUB_DEGREE
          ? lightenColor(nodeColorMap[n.nodeType] ?? FALLBACK_NODE_COLOR, 0.35)
          : (nodeColorMap[n.nodeType] ?? FALLBACK_NODE_COLOR),
    };
    seedGalaxyPosition(node, i, nodes.value.length, maxDeg);
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
    .backgroundColor('rgba(4,7,17,0)') // 透明画布，透出 CSS 星空层
    .showNavInfo(false)
    .nodeRelSize(4.5)
    .nodeResolution(20)
    .nodeOpacity(1)
    .nodeLabel(nodeLabelFn)
    .nodeColor(nodeColorFn)
    .nodeVal(nodeValFn)
    .linkColor(linkColorFn)
    .linkWidth(linkWidthFn)
    .linkOpacity(1)
    .linkDirectionalArrowLength(3)
    .linkDirectionalArrowRelPos(0.92)
    .linkDirectionalArrowColor(linkColorFn)
    .linkDirectionalParticles(linkParticlesFn)
    .linkDirectionalParticleSpeed(linkParticleSpeedFn)
    .linkDirectionalParticleWidth(1.6)
    .linkDirectionalParticleColor(linkColorFn)
    .enableNodeDrag(true)
    .onNodeClick(handleNodeClick);

  // 星系力场：适度排斥（默认 3D 为 -60）+ 中等边长，喂数据前配置
  g.d3Force('charge')?.strength(-40);
  g.d3Force('link')?.distance(42).strength(0.5);

  g.graphData({ nodes: fgNodes, links: fgLinks });

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

      <!-- 星空背景层（透明画布透出） -->
      <div class="graph3d-stars" :style="{ backgroundImage: starfieldStyle }" />
      <div ref="containerRef" class="graph3d-canvas" />

      <!-- 图例 -->
      <div class="graph3d-legend">
        <div class="legend-title">节点类型</div>
        <div class="legend-item" v-for="key in legendKeys" :key="key">
          <span class="legend-dot" :style="{ background: nodeColorMap[key] }" />{{ nodeTypeLabel[key] ?? key }}
        </div>
        <div class="legend-divider" />
        <div class="legend-item"><span class="legend-dot" :style="{ background: HUB_NODE_COLOR }" />枢纽节点（高关联度）</div>
        <div class="legend-item"><span class="legend-dot" :style="{ background: ROUTE_NODE_COLOR }" />路线高亮</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.graph3d-page {
  display: flex; flex-direction: column;
  height: calc(100vh - var(--topbar-h));
  background: #05070f;
  color: #dbe4f5;
}

.graph3d-toolbar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 10px 16px; flex-shrink: 0;
  background: rgba(7, 10, 20, .95);
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
.route-select option { background: #0a0e18; color: #dbe4f5; }

.graph3d-body {
  position: relative; flex: 1; min-height: 0; overflow: hidden;
  /* 深色星云底：蓝/紫/青三处微光 */
  background:
    radial-gradient(ellipse at 22% 28%, rgba(56, 189, 248, .10), transparent 52%),
    radial-gradient(ellipse at 76% 62%, rgba(139, 92, 246, .09), transparent 50%),
    radial-gradient(ellipse at 50% 108%, rgba(34, 211, 238, .06), transparent 45%),
    #05070f;
}
.graph3d-stars {
  position: absolute; inset: 0;
  pointer-events: none; /* 不拦截画布交互 */
}
.graph3d-canvas { position: absolute; inset: 0; }

.graph3d-status {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  z-index: 6; display: flex; align-items: center; gap: 12px;
  padding: 12px 18px; border-radius: 8px;
  background: rgba(9, 13, 26, .92); border: 1px solid rgba(56, 189, 248, .3);
  color: #c7d2e8; font-size: 13px; box-shadow: 0 8px 28px rgba(5, 7, 15, .5);
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
  background: rgba(9, 13, 26, .88); border: 1px solid rgba(56, 189, 248, .25);
  border-radius: 8px; padding: 10px 12px; font-size: 12px; color: #c7d2e8;
  box-shadow: 0 8px 28px rgba(5, 7, 15, .5);
}
.legend-title { font-size: 11px; color: #7583a4; letter-spacing: .06em; margin-bottom: 6px; }
.legend-item { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; box-shadow: 0 0 6px currentColor; }
.legend-divider { border-top: 1px solid rgba(56, 189, 248, .2); margin: 6px 0; }
</style>
