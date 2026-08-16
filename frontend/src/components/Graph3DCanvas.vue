<script lang="ts">
/** 3D 画布同源常量导出：供页面级图例复用（与画布渲染配色一致，避免两处不一致） */
import { nodeTypeLabel, statusLabel } from '@/api/mockData';

/** 节点类型霓虹配色（青/紫/蓝为主，暗底更亮） */
export const nodeColorMap: Record<string, string> = {
  SYSTEM: '#38bdf8', SUBSYSTEM: '#22d3ee',
  DATABASE: '#a78bfa', TABLE: '#c084fc',
  DEPARTMENT: '#60a5fa', ROLE: '#818cf8',
  ACTION: '#34d399', EVENT: '#2dd4bf',
  DEVICE: '#67e8f9', WORKSTATION: '#7dd3fc',
};

/** 路线节点高亮主色 */
export const ROUTE_NODE_COLOR = '#7dd3fc';
/** 图例（仅展示当前数据使用的核心类型） */
export const legendKeys = ['SYSTEM', 'DATABASE', 'DEPARTMENT', 'ACTION'];

/** hex 颜色向白混合（枢纽节点提亮、光晕感） */
export function lightenColor(hex: string, amt: number): string {
  const c = parseInt(hex.slice(1), 16);
  const r = Math.round(((c >> 16) & 255) + (255 - ((c >> 16) & 255)) * amt);
  const g = Math.round(((c >> 8) & 255) + (255 - ((c >> 8) & 255)) * amt);
  const b = Math.round((c & 255) + (255 - (c & 255)) * amt);
  return `rgb(${r},${g},${b})`;
}

/** 图例「枢纽」用色 */
export const HUB_NODE_COLOR = lightenColor(nodeColorMap.SYSTEM, 0.35);
</script>

<script setup lang="ts">
/**
 * 可复用 3D 行星系关系网画布（基于 3d-force-graph）
 * - 小发光星点 + 光晕、度数定大小、星空背景、力导向散开
 * - 路线聚焦（activeRouteId）：放大提亮 + 其余淡化 + zoomToFit
 * - 节点聚焦（focusNodeId）：镜头移动到该节点
 * - 点击节点：高亮相邻并 emit node-click；hover 显示名称 / 关联度
 * props / emit 与 GraphCanvas 对齐，便于 2D ⇄ 3D 切换
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import ForceGraph3D from '3d-force-graph';
import type { ForceGraph3DInstance } from '3d-force-graph';
import type { LinkObject, NodeObject } from '3d-force-graph';
import type { GraphEdge, GraphNode, Route } from '@/types';
// @ts-ignore 复用 3d-force-graph 的传递依赖 three（@types/three 未装，按 any 使用）
import * as THREE from 'three';

const props = withDefaults(defineProps<{
  nodes: GraphNode[];
  edges: GraphEdge[];
  routes: Route[];
  activeRouteId?: string | null;
  focusNodeId?: string | null;
  autoRotate?: boolean;
}>(), {
  activeRouteId: null,
  focusNodeId: null,
  autoRotate: true,
});

const emit = defineEmits<{
  (e: 'node-click', node: GraphNode): void;
}>();

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

const FALLBACK_NODE_COLOR = '#94a3b8';
const DIM_NODE_COLOR = 'rgba(148,163,184,0.08)';       // 点击后非高亮节点：暗成淡星点
const ROUTE_DIM_NODE_COLOR = 'rgba(148,163,184,0.10)';  // 路线聚焦非路线节点
const DIM_LINK_COLOR = 'rgba(120,150,190,0.06)';        // 点击后非高亮边
const BASE_LINK_COLOR = 'rgba(125,211,252,0.25)';       // 默认边：细、半透明发光青
const HIGHLIGHT_LINK_COLOR = 'rgba(103,232,249,0.9)';   // 点击高亮边
const ROUTE_LINK_COLOR = 'rgba(125,211,252,0.95)';      // 路线边
const ROUTE_DIM_LINK_COLOR = 'rgba(148,163,184,0.05)';  // 路线聚焦非路线边
const HUB_DEGREE = 3;                                   // 度数 ≥ 3 视为「枢纽」（n1/n6/n8 作星系焦点）

/** 度数 → 体积：3D 半径 = cbrt(val) * nodeRelSize，星点化：整体很小 */
function degreeVal(degree: number): number {
  return 2 + degree * 1.5;
}

/* ---------- 发光星点：Three.js 核心球 + 加法混合光晕精灵 ---------- */
const NODE_REL_SIZE = 2;      // 星点半径基准（大幅缩小）
const HALO_MULT = 5;          // 普通光晕 = 核心半径 × 5
const HUB_HALO_MULT = 7.5;    // 枢纽光晕更大，作星系焦点
const HALO_OPACITY = 0.55;    // 光晕透明度
const NODE_OPACITY = 1;       // 星点主体不透明度（淡化由颜色 alpha 控制）

let sharedUnitGeo: THREE.SphereGeometry | null = null; // 共享单位球（radius=1，用 scale 控大小）
let sharedGlowTex: THREE.CanvasTexture | null = null;  // 共享径向渐变光晕贴图

function getUnitSphere(): THREE.SphereGeometry {
  if (!sharedUnitGeo) sharedUnitGeo = new THREE.SphereGeometry(1, 16, 16);
  return sharedUnitGeo;
}

/** 生成柔和径向渐变光晕贴图（白心 → 透明），Sprite 上染色后加法混合 */
function getGlowTexture(): THREE.CanvasTexture {
  if (sharedGlowTex) return sharedGlowTex;
  const size = 128;
  const cv = document.createElement('canvas');
  cv.width = cv.height = size;
  const ctx = cv.getContext('2d');
  if (ctx) {
    const g = ctx.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2);
    g.addColorStop(0, 'rgba(255,255,255,1)');
    g.addColorStop(0.22, 'rgba(255,255,255,0.5)');
    g.addColorStop(1, 'rgba(255,255,255,0)');
    ctx.fillStyle = g;
    ctx.fillRect(0, 0, size, size);
  }
  sharedGlowTex = new THREE.CanvasTexture(cv);
  return sharedGlowTex;
}

/** 解析 #hex / rgb() / rgba() 颜色为分量（用于材质染色与透明度） */
function parseColor(c: string): { r: number; g: number; b: number; a: number } {
  if (c.startsWith('#')) {
    const n = parseInt(c.slice(1), 16);
    return { r: (n >> 16) & 255, g: (n >> 8) & 255, b: n & 255, a: 1 };
  }
  const m = c.match(/rgba?\(([^)]+)\)/);
  if (m) {
    const p = m[1].split(',').map((s) => Number(s.trim()));
    return { r: p[0] || 0, g: p[1] || 0, b: p[2] || 0, a: p[3] ?? 1 };
  }
  return { r: 148, g: 163, b: 184, a: 1 };
}

/** 每个节点对应的发光对象（点击/路线聚焦时由 applyNodeVisuals 统一刷新） */
const nodeObjs = new Map<string, { core: THREE.Mesh; halo: THREE.Sprite }>();
const fgNodesById = new Map<string, FgNode>();

/** 生成「发光星点」：小核心球（MeshBasicMaterial 自发光）+ 加法光晕精灵 */
function nodeThreeObjectFn(node: NodeObject): THREE.Group {
  const fg = node as FgNode;
  const radius = Math.cbrt(fg.val) * NODE_REL_SIZE;
  const col = parseColor(fg.color);
  const core = new THREE.Mesh(
    getUnitSphere(),
    new THREE.MeshBasicMaterial({
      color: new THREE.Color(col.r / 255, col.g / 255, col.b / 255),
      transparent: true,
      opacity: NODE_OPACITY,
      depthWrite: false, // 允许远景星点不被近处遮蔽
    }),
  );
  core.scale.setScalar(radius);
  const halo = new THREE.Sprite(
    new THREE.SpriteMaterial({
      map: getGlowTexture(),
      color: new THREE.Color(col.r / 255, col.g / 255, col.b / 255),
      transparent: true,
      opacity: HALO_OPACITY,
      blending: THREE.AdditiveBlending,
      depthWrite: false,
    }),
  );
  const haloMult = fg.degree >= HUB_DEGREE ? HUB_HALO_MULT : HALO_MULT;
  halo.scale.setScalar(radius * haloMult);
  const grp = new THREE.Group();
  grp.add(core, halo);
  nodeObjs.set(fg.id, { core, halo });
  return grp;
}

/** 依据当前 nodeColor / nodeVal 状态，统一刷新所有星点（尺寸 / 颜色 / 淡化） */
function applyNodeVisuals() {
  for (const [id, obj] of nodeObjs) {
    const node = fgNodesById.get(id);
    if (!node) continue;
    const col = parseColor(nodeColorFn(node));
    const radius = Math.cbrt(nodeValFn(node)) * NODE_REL_SIZE;
    const opacity = col.a * NODE_OPACITY;
    obj.core.scale.setScalar(radius);
    obj.core.material.color.setRGB(col.r / 255, col.g / 255, col.b / 255);
    obj.core.material.opacity = opacity;
    obj.halo.material.color.setRGB(col.r / 255, col.g / 255, col.b / 255);
    obj.halo.material.opacity = opacity * HALO_OPACITY;
    obj.halo.scale.setScalar(radius * (node.degree >= HUB_DEGREE ? HUB_HALO_MULT : HALO_MULT));
  }
}

/** 生成星空背景：细密星点 + 少量大星云斑（深色星云底上发光星野） */
function buildStarfield(count = 140): string {
  const layers: string[] = [];
  for (let i = 0; i < count; i++) {
    const x = (Math.random() * 100).toFixed(2);
    const y = (Math.random() * 100).toFixed(2);
    const nebula = i % 12 === 0; // 周期性插入大星云斑
    const size = nebula ? 5 + Math.random() * 15 : 0.6 + Math.random() * 1.7;
    const alpha = nebula ? 0.05 + Math.random() * 0.09 : 0.3 + Math.random() * 0.65;
    const tint = Math.random() < 0.5
      ? '255,255,255'
      : Math.random() < 0.5 ? '170,205,255' : '205,180,255';
    layers.push(
      `radial-gradient(circle at ${x}% ${y}%, rgba(${tint},${alpha.toFixed(2)}) 0, rgba(${tint},0) ${size.toFixed(2)}px)`,
    );
  }
  return layers.join(', ');
}
const starfieldStyle = ref(buildStarfield());

const containerRef = ref<HTMLDivElement | null>(null);

/** 点击高亮集合：选中节点 + 其相邻节点（内部状态，优先于路线聚焦） */
const selectedNodeId = ref<string | null>(null);
const adjacencyMap = ref(new Map<string, Set<string>>());

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
  const r = props.routes.find((x) => x.id === props.activeRouteId);
  return new Set(r?.nodeIds ?? []);
});
/** 路线相邻边 key（source->target） */
const routeLinkKeys = computed(() => {
  const r = props.routes.find((x) => x.id === props.activeRouteId);
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
let destroyed = false;
let initToken = 0; // 并发 init 守卫：新 init 开始时作废旧 init 的异步续体

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
  if (props.activeRouteId) {
    return routeNodeSet.value.has(node.id) ? ROUTE_NODE_COLOR : ROUTE_DIM_NODE_COLOR;
  }
  return node.color;
}

/** 节点体积：路线聚焦时路线节点放大、其余缩小 */
function nodeValFn(n: NodeObject): number {
  const node = n as FgNode;
  if (props.activeRouteId) {
    return routeNodeSet.value.has(node.id) ? node.val * 2 + 4 : node.val * 0.4;
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
  if (props.activeRouteId) {
    return linkOnRoute(src, tgt) ? ROUTE_LINK_COLOR : ROUTE_DIM_LINK_COLOR;
  }
  return BASE_LINK_COLOR;
}

/** 边粗细：路线聚焦时路线边加粗、其余变细 */
function linkWidthFn(l: LinkObject<NodeObject>): number {
  if (props.activeRouteId) {
    return linkOnRoute(linkEndpointId(l.source), linkEndpointId(l.target)) ? 1.8 : 0.3;
  }
  return 0.8; // 细线光丝
}

/** 粒子数：路线聚焦时路线边增强、其余关闭 */
function linkParticlesFn(l: LinkObject<NodeObject>): number {
  if (props.activeRouteId) {
    return linkOnRoute(linkEndpointId(l.source), linkEndpointId(l.target)) ? 5 : 0;
  }
  return 2;
}

/** 粒子速度：路线边略快 */
function linkParticleSpeedFn(l: LinkObject<NodeObject>): number {
  if (props.activeRouteId && linkOnRoute(linkEndpointId(l.source), linkEndpointId(l.target))) {
    return 0.01;
  }
  return 0.004; // 缓慢流动的数据光子
}

/** 悬停标注：名称 + 类型/等级/状态 + 度数；路线聚焦时附带路线名 */
function nodeLabelFn(n: NodeObject): string {
  const node = n as FgNode;
  const type = nodeTypeLabel[node.nodeType] ?? node.nodeType;
  const status = statusLabel[node.status] ?? node.status;
  const route = props.routes.find((r) => r.id === props.activeRouteId);
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

/** 重新触发渲染：点击/路线状态变化后刷新星点与边 */
function applyVisuals() {
  if (!graph) return;
  applyNodeVisuals();
  graph.linkColor(linkColorFn);
  graph.linkWidth(linkWidthFn);
  graph.linkDirectionalArrowColor(linkColorFn);
  graph.linkDirectionalParticles(linkParticlesFn);
  graph.linkDirectionalParticleSpeed(linkParticleSpeedFn);
  graph.linkDirectionalParticleColor(linkColorFn);
}

/** 点击节点：高亮相邻节点/边（再次点击还原），并上抛 node-click */
function handleNodeClick(node: NodeObject) {
  const fg = node as FgNode;
  selectedNodeId.value = fg.id === selectedNodeId.value ? null : fg.id;
  applyVisuals();
  const gn = props.nodes.find((n) => n.id === fg.id);
  if (gn) emit('node-click', gn);
}

/** 聚焦某节点：镜头移动并注视该节点 */
function focusNode(id: string) {
  const g = graph;
  if (!g) return;
  const n = fgNodesById.get(id);
  if (!n) return;
  const pos = { x: n.x ?? 0, y: n.y ?? 0, z: n.z ?? 0 };
  g.cameraPosition({ x: pos.x + 240, y: pos.y + 240, z: pos.z + 240 }, pos, 800);
}

/** 重置视角：恢复初始全景机位 */
function resetView() {
  graph?.cameraPosition({ x: 340, y: 240, z: 380 }, { x: 0, y: 0, z: 0 }, 800);
}

defineExpose({ resetView });

/** 星系感初始散布：球面均匀分布 + 按度数径向分层（枢纽向中心聚、叶子在外围） */
function seedGalaxyPosition(node: FgNode, i: number, count: number, maxDeg: number) {
  const outerR = 150; // 初始散布更开，配合大边长形成星系盘
  const phi = Math.acos(1 - (2 * (i + 0.5)) / count); // 纬度均匀
  const theta = i * 2.399963;                        // 黄金角错开经度
  const radius = outerR * (0.2 + 0.8 * (1 - node.degree / maxDeg));
  node.x = radius * Math.sin(phi) * Math.cos(theta);
  node.y = radius * Math.cos(phi);
  node.z = radius * Math.sin(phi) * Math.sin(theta);
}

/** 读取容器实际尺寸（clientWidth/Height 优先，offset 兜底） */
function measureSize(): { width: number; height: number } {
  const el = containerRef.value;
  if (!el) return { width: 0, height: 0 };
  return {
    width: el.clientWidth || el.offsetWidth || 0,
    height: el.clientHeight || el.offsetHeight || 0,
  };
}

/** 等待布局后取容器尺寸：nextTick → 首帧重取 → 父容器/视口兜底，避免 0 尺寸空白 */
async function ensureContainerSize(): Promise<{ width: number; height: number }> {
  await nextTick();
  let size = measureSize();
  if (size.width === 0 || size.height === 0) {
    // 首帧可能尚未布局，等一个 rAF 再量
    await new Promise<void>((r) => requestAnimationFrame(() => r()));
    size = measureSize();
  }
  if (size.width === 0 || size.height === 0) {
    // 兜底：父容器或视口尺寸，避免 0 尺寸画布空白；ResizeObserver 随后校正
    const parent = containerRef.value?.parentElement;
    size = {
      width: parent?.clientWidth || window.innerWidth || 800,
      height: parent?.clientHeight || window.innerHeight || 600,
    };
  }
  return size;
}

/** 组装 3D 图数据：度数定大小 / 力导向聚簇 / 透明画布透出星空 */
async function initGraph() {
  destroyGraph();
  const el = containerRef.value;
  if (!el || props.nodes.length === 0) return;
  const token = ++initToken;
  const size = await ensureContainerSize();
  if (token !== initToken || destroyed || !containerRef.value) return;

  selectedNodeId.value = null; // 数据重建，清除点击高亮

  // 度数 = 节点在 edges 中作为 source/target 出现的次数（关联度）
  const degreeMap = new Map<string, number>();
  props.edges.forEach((e) => {
    degreeMap.set(e.source, (degreeMap.get(e.source) ?? 0) + 1);
    degreeMap.set(e.target, (degreeMap.get(e.target) ?? 0) + 1);
  });
  const maxDeg = Math.max(1, ...[...degreeMap.values()]);

  const adj = new Map<string, Set<string>>();
  const fgNodes: FgNode[] = props.nodes.map((n, i) => {
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
    fgNodesById.set(node.id, node);
    seedGalaxyPosition(node, i, props.nodes.length, maxDeg);
    return node;
  });
  const fgLinks: FgLink[] = props.edges.map((e) => {
    if (!adj.has(e.source)) adj.set(e.source, new Set());
    adj.get(e.source)!.add(e.target);
    if (!adj.has(e.target)) adj.set(e.target, new Set());
    adj.get(e.target)!.add(e.source);
    return { id: e.id, source: e.source, target: e.target };
  });
  adjacencyMap.value = adj;

  const g = new ForceGraph3D(el);
  graph = g;
  g
    .width(size.width)
    .height(size.height)
    .backgroundColor('rgba(4,7,17,0)') // 透明画布，透出 CSS 星空层
    .showNavInfo(false)
    .nodeRelSize(NODE_REL_SIZE) // 星点基准（大幅缩小）
    .nodeResolution(16)
    .nodeOpacity(1)
    .nodeLabel(nodeLabelFn)
    .nodeColor(nodeColorFn)
    .nodeVal(nodeValFn)
    .nodeThreeObject(nodeThreeObjectFn) // 发光星点：核心球 + 光晕精灵
    .linkColor(linkColorFn)
    .linkWidth(linkWidthFn)
    .linkOpacity(1)
    .linkDirectionalArrowLength(3)
    .linkDirectionalArrowRelPos(0.92)
    .linkDirectionalArrowColor(linkColorFn)
    .linkDirectionalParticles(linkParticlesFn)
    .linkDirectionalParticleSpeed(linkParticleSpeedFn)
    .linkDirectionalParticleWidth(1.1)
    .linkDirectionalParticleColor(linkColorFn)
    .enableNodeDrag(true)
    .onNodeClick(handleNodeClick);

  // 星系力场：负电荷加大让星点互相排斥散开 + 边长拉长（不再聚成一坨）
  g.d3Force('charge')?.strength(-70);
  g.d3Force('link')?.distance(95).strength(0.4);
  g.d3AlphaDecay(0.03).d3AlphaMin(0.001); // 约 3~4 秒冷却，随后缩放框景

  g.graphData({ nodes: fgNodes, links: fgLinks });
  applyVisuals(); // 依据初始状态刷新星点与边外观

  // 星点渲染完成后再按当前状态刷新（路线聚焦 / 点击高亮的淡化）
  window.setTimeout(() => {
    if (token !== initToken || destroyed || !graph) return;
    applyVisuals();
  }, 120);

  // 极淡雾效：远景星点略带朦胧，增强立体深度
  if (g.scene()) {
    g.scene()!.fog = new THREE.FogExp2(0x060913, 0.0009);
  }

  const controls = g.controls() as unknown as { autoRotate: boolean; autoRotateSpeed?: number };
  controls.autoRotate = props.autoRotate;
  controls.autoRotateSpeed = 1.1;
  g.cameraPosition({ x: 340, y: 240, z: 380 }, { x: 0, y: 0, z: 0 }, 0);

  // 力导向冷却后，一次性框景：节点聚焦 > 路线聚焦 > 全景
  let framedOnce = false;
  g.onEngineStop(() => {
    if (framedOnce || token !== initToken || destroyed) return;
    framedOnce = true;
    const fid = props.focusNodeId;
    if (fid && fgNodesById.has(fid)) {
      focusNode(fid);
    } else if (props.activeRouteId) {
      const r = props.routes.find((x) => x.id === props.activeRouteId);
      if (r) {
        const ids = new Set(r.nodeIds);
        g.zoomToFit(500, 90, (n) => ids.has(String(n.id)));
      } else {
        g.zoomToFit(800, 80);
      }
    } else {
      g.zoomToFit(800, 80);
    }
  });

  // 容器尺寸变化自适应
  resizeObserver = new ResizeObserver(() => {
    if (resizeRaf) window.cancelAnimationFrame(resizeRaf);
    resizeRaf = window.requestAnimationFrame(() => {
      const c = containerRef.value;
      if (!c) return;
      g.width(c.clientWidth || size.width).height(c.clientHeight || size.height);
    });
  });
  resizeObserver.observe(el);
}

function destroyGraph() {
  resizeObserver?.disconnect();
  resizeObserver = null;
  if (resizeRaf) window.cancelAnimationFrame(resizeRaf);
  resizeRaf = 0;
  // 释放每个星点的材质（几何体为共享单位球，不在此释放）
  for (const { core, halo } of nodeObjs.values()) {
    core.material?.dispose?.();
    halo.material?.dispose?.();
  }
  nodeObjs.clear();
  fgNodesById.clear();
  graph?._destructor();
  graph = null;
}

/** 数据变化 → 重建（nodes/edges/routes 引用变化） */
watch(() => [props.nodes, props.edges, props.routes], () => { void initGraph(); });

/** 路线变化 → 路线聚焦（放大高亮 + 镜头框选居中；清除则恢复全图） */
watch(() => props.activeRouteId, (id) => {
  selectedNodeId.value = null;
  applyVisuals();
  if (!graph) return;
  if (id) {
    const r = props.routes.find((x) => x.id === id);
    if (r) {
      const ids = new Set(r.nodeIds);
      graph.zoomToFit(500, 90, (n) => ids.has(String(n.id)));
    }
  } else {
    graph.zoomToFit(650, 60); // 恢复全图
  }
});

/** 节点聚焦 → 镜头移动到该节点 */
watch(() => props.focusNodeId, (id) => { if (id) focusNode(id); });

/** 自动旋转开关（父级控制，prop 驱动） */
watch(() => props.autoRotate, (v) => {
  const controls = graph?.controls() as unknown as { autoRotate?: boolean } | undefined;
  if (controls) controls.autoRotate = v;
});

onMounted(() => { void initGraph(); });

onBeforeUnmount(() => {
  destroyed = true;
  destroyGraph();
});
</script>

<template>
  <div class="g3d-root">
    <!-- 星空背景层（透明画布透出） -->
    <div class="g3d-stars" :style="{ backgroundImage: starfieldStyle }" />
    <div ref="containerRef" class="g3d-canvas" />
  </div>
</template>

<style scoped>
.g3d-root {
  position: absolute; inset: 0; overflow: hidden;
  /* 星云底：蓝/紫/青多团渐变 + 中央星系光晕 */
  background:
    radial-gradient(ellipse at 22% 28%, rgba(56, 189, 248, .13), transparent 52%),
    radial-gradient(ellipse at 76% 62%, rgba(139, 92, 246, .12), transparent 50%),
    radial-gradient(ellipse at 38% 78%, rgba(34, 211, 238, .07), transparent 42%),
    radial-gradient(ellipse at 50% 40%, rgba(96, 165, 250, .05), transparent 55%),
    radial-gradient(ellipse at 50% 115%, rgba(103, 232, 249, .08), transparent 45%),
    #05070f;
}
.g3d-root::after {
  /* 暗角：聚焦中央星系，四周渐暗增加深邃感 */
  content: '';
  position: absolute; inset: 0;
  pointer-events: none;
  background: radial-gradient(ellipse at center, transparent 52%, rgba(2, 4, 10, .55) 100%);
}
.g3d-stars {
  position: absolute; inset: 0;
  pointer-events: none; /* 不拦截画布交互 */
}
.g3d-canvas { position: absolute; inset: 0; }
</style>
