<script setup lang="ts">
/**
 * 核心关系网画布（基于 AntV G6 5.x）
 * - 节点按类型配色，状态描边（正常/异常/失败）
 * - 路线高亮：选中路线 → 路线站点与连线高亮、其余变暗
 * - 点击节点 → 上抛事件（供详情面板/顺藤摸瓜使用）
 *
 * 渲染稳健性（防「画布空白」）：
 * - 显式容器尺寸：nextTick + rAF 量取，0 尺寸时兜底并靠 ResizeObserver 校正
 * - 去掉 autoFit:'view'（力导向布局期间会误适配到原点重叠处），布局稳定后手动 fitView
 * - ResizeObserver 监听容器变化 → graph.resize()，侧栏/窗口变化画布不空白
 * - render 出错 → 红色错误条 + 重试，不再静默空白
 */
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { Graph } from '@antv/g6';
import type { GraphData, NodeData, EdgeData } from '@antv/g6';
import type { GraphEdge, GraphNode, Route } from '@/types';

const props = defineProps<{
  nodes: GraphNode[];
  edges: GraphEdge[];
  routes: Route[];
  activeRouteId: string | null;
  focusNodeId?: string | null;
}>();

const emit = defineEmits<{
  (e: 'node-click', node: GraphNode): void;
}>();

const container = ref<HTMLDivElement | null>(null);
const renderError = ref<string | null>(null); // 渲染错误条

let graph: Graph | null = null;
let resizeObserver: ResizeObserver | null = null;
let fitTimer: number | null = null; // fitView 去抖定时器
let layoutFitted = false; // 本次渲染是否已成功适配过视图
let destroyed = false;

const nodeTypeColor: Record<string, string> = {
  SYSTEM: '#3b82f6', SUBSYSTEM: '#3b82f6', DATABASE: '#8b5cf6', TABLE: '#8b5cf6',
  DEPARTMENT: '#06b6d4', ROLE: '#06b6d4', ACTION: '#f59e0b', EVENT: '#f59e0b',
  DEVICE: '#f43f5e', WORKSTATION: '#f43f5e',
};
const statusStroke: Record<string, string> = {
  ACTIVE: '#1e2a4a', WARNING: '#f59e0b', FAIL: '#ef4444', DISABLED: '#3a4560',
};

function toNodeData(n: GraphNode): NodeData {
  return {
    id: n.id,
    data: {
      ...n,
      _color: nodeTypeColor[n.nodeType] ?? '#94a3b8',
      _stroke: statusStroke[n.status] ?? '#1e2a4a',
    },
  } as unknown as NodeData;
}

function toEdgeData(e: GraphEdge): EdgeData {
  return { id: e.id, source: e.source, target: e.target, data: e } as unknown as EdgeData;
}

/** 路线高亮：高亮路线内节点与边，其余变暗 */
function applyHighlight(routeId: string | null) {
  if (!graph) return;
  const reset: Record<string, string[]> = {};
  props.nodes.forEach((n) => (reset[n.id] = []));
  props.edges.forEach((e) => (reset[e.id] = []));
  graph.setElementState(reset);

  if (!routeId) return;
  const route = props.routes.find((r) => r.id === routeId);
  if (!route) return;
  const set = new Set(route.nodeIds);
  const edgeSet = new Set(
    props.edges.filter((e) => set.has(e.source) && set.has(e.target)).map((e) => e.id),
  );
  const states: Record<string, string[]> = {};
  props.nodes.forEach((n) => (states[n.id] = set.has(n.id) ? ['highlight'] : ['dimmed']));
  props.edges.forEach((e) => (states[e.id] = edgeSet.has(e.id) ? ['highlight'] : ['dimmed']));
  graph.setElementState(states);
}

/** 读取容器实际尺寸（clientWidth/Height 优先，offset 兜底） */
function measureSize(): { width: number; height: number } {
  const el = container.value;
  if (!el) return { width: 0, height: 0 };
  return {
    width: el.clientWidth || el.offsetWidth || 0,
    height: el.clientHeight || el.offsetHeight || 0,
  };
}

/** 等待布局完成后取容器尺寸：nextTick → 首帧后重取 → 父容器/视口兜底 */
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
    const parent = container.value?.parentElement;
    size = {
      width: parent?.clientWidth || window.innerWidth || 800,
      height: parent?.clientHeight || window.innerHeight || 600,
    };
  }
  return size;
}

/** 将全部元素适配到视口（居中铺满，padding 40 由 graph 配置提供） */
function doFitView() {
  if (!graph || destroyed || props.nodes.length === 0) return;
  try {
    graph.fitView().catch(() => {});
    layoutFitted = true;
  } catch (err) {
    console.warn('[GraphCanvas] fitView 失败:', err);
  }
}

/** 去抖后的 fitView：同一批次只适配一次 */
function scheduleFitView() {
  if (fitTimer !== null || destroyed) return;
  fitTimer = window.setTimeout(() => {
    fitTimer = null;
    doFitView();
  }, 60);
}

async function render() {
  renderError.value = null;
  if (!graph) return;
  try {
    const data: GraphData = {
      nodes: props.nodes.map(toNodeData),
      edges: props.edges.map(toEdgeData),
    };
    graph.setData(data);
    layoutFitted = false; // 等待本次 afterlayout 完成适配
    await graph.render();
    applyHighlight(props.activeRouteId);
    if (props.focusNodeId) graph.focusElement(props.focusNodeId, { easing: 'linear', duration: 300 });
    // 兜底：afterlayout 未触发（空图/事件丢失）时，延迟手动适配一次
    window.setTimeout(() => { if (!layoutFitted) doFitView(); }, 400);
  } catch (err) {
    renderError.value = err instanceof Error ? err.message : String(err);
    console.error('[GraphCanvas] 渲染失败:', err);
  }
}

/** 监听容器尺寸变化 → resize 画布并重新适配 */
function observeResize() {
  const el = container.value;
  if (!el || typeof ResizeObserver === 'undefined') return;
  resizeObserver = new ResizeObserver((entries) => {
    const entry = entries[0];
    if (!entry || !graph || destroyed) return;
    const { width, height } = entry.contentRect;
    if (width === 0 || height === 0) return;
    try {
      graph.resize(width, height);
    } catch (err) {
      console.warn('[GraphCanvas] resize 失败:', err);
      return;
    }
    scheduleFitView(); // 容器变化后重新适配
  });
  resizeObserver.observe(el);
}

/** 错误条上的重试 */
function retryRender() {
  render();
}

onMounted(async () => {
  const el = container.value;
  if (!el) return;
  try {
    const { width, height } = await ensureContainerSize();
    if (width === 0 || height === 0) {
      renderError.value = '画布容器尺寸为 0，无法渲染';
      return;
    }
    graph = new Graph({
      container: el,
      width,
      height,
      padding: 40, // fitView 适配时留 40 内边距
      behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
      layout: {
        type: 'force',
        linkDistance: 150,
        preventOverlap: true,
        nodeStrength: -300,
      } as unknown as never,
      node: {
        style: {
          size: 32,
          fill: (d: any) => d?.data?._color ?? '#94a3b8',
          stroke: (d: any) => d?.data?._stroke ?? '#1e2a4a',
          lineWidth: 4,
          labelText: (d: any) => d?.data?.name ?? d?.id,
          labelFill: '#ffffff',
          labelFontSize: 12,
          labelPlacement: 'center',
          cursor: 'pointer',
        },
        state: {
          highlight: {
            lineWidth: 5,
            stroke: '#fbbf24',
            shadowColor: '#2563eb', shadowBlur: 20, shadowOffsetX: 0, shadowOffsetY: 0,
          },
          dimmed: { opacity: 0.22, labelOpacity: 0.22 },
        },
      },
      edge: {
        style: {
          stroke: '#56617a',
          lineWidth: 1.2,
          endArrow: true,
          strokeOpacity: 0.9,
        },
        state: {
          highlight: { stroke: '#2563eb', lineWidth: 3.2 },
          dimmed: { opacity: 0.12 },
        },
      },
    });

    // 布局稳定后适配视图（G6 5.x afterlayout 事件，比 autoFit 更可靠）
    graph.on('afterlayout', () => { scheduleFitView(); });

    observeResize();

    await render();

    graph.on('node:click', (evt: any) => {
      const target = evt?.target as any;
      const id = target?.id ?? target?.getElementId?.() ?? evt?.itemId;
      const node = props.nodes.find((n) => n.id === id);
      if (node) emit('node-click', node);
    });
  } catch (err) {
    renderError.value = err instanceof Error ? err.message : String(err);
    console.error('[GraphCanvas] 初始化失败:', err);
  }
});

// 切换路线 → 只更新高亮（不重排布局）
watch(() => props.activeRouteId, (id) => applyHighlight(id));
// 数据变化 → 重渲染
watch(() => [props.nodes, props.edges], () => render());
// 搜索聚焦
watch(() => props.focusNodeId, (id) => { if (id && graph) graph.focusElement(id, { easing: 'linear', duration: 300 }); });

onBeforeUnmount(() => {
  destroyed = true;
  if (fitTimer !== null) window.clearTimeout(fitTimer);
  resizeObserver?.disconnect();
  resizeObserver = null;
  graph?.destroy();
  graph = null;
});
</script>

<template>
  <div class="graph-wrap">
    <div ref="container" class="graph-canvas" />
    <div v-if="renderError" class="graph-error" role="alert">
      <span class="graph-error__text">画布渲染失败：{{ renderError }}</span>
      <button class="graph-error__retry" @click="retryRender">重试</button>
    </div>
  </div>
</template>

<style scoped>
.graph-wrap { position: relative; width: 100%; height: 100%; }
.graph-canvas { width: 100%; height: 100%; }
.graph-error {
  position: absolute; top: 12px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 10px; z-index: 10; max-width: 80%;
  padding: 8px 14px; border-radius: 8px;
  background: #7f1d1d; color: #fff; font-size: 12.5px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, .35);
}
.graph-error__text { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.graph-error__retry {
  flex-shrink: 0; padding: 2px 10px; border: 1px solid rgba(255,255,255,.5);
  border-radius: 6px; background: transparent; color: #fff; cursor: pointer; font-size: 12px;
}
.graph-error__retry:hover { background: rgba(255,255,255,.15); }
</style>
