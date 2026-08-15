<script setup lang="ts">
/**
 * 核心关系网画布（基于 AntV G6 5.x）
 * - 节点按类型配色，状态描边（正常/异常/失败）
 * - 路线高亮：选中路线 → 路线站点与连线高亮、其余变暗
 * - 点击节点 → 上抛事件（供详情面板/顺藤摸瓜使用）
 */
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
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
let graph: Graph | null = null;

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

async function render() {
  if (!graph) return;
  const data: GraphData = {
    nodes: props.nodes.map(toNodeData),
    edges: props.edges.map(toEdgeData),
  };
  graph.setData(data);
  await graph.render();
  applyHighlight(props.activeRouteId);
  if (props.focusNodeId) graph.focusElement(props.focusNodeId, { easing: 'linear', duration: 300 });
}

onMounted(async () => {
  if (!container.value) return;
  graph = new Graph({
    container: container.value,
    autoFit: 'view',
    behaviors: ['drag-canvas', 'zoom-canvas', 'drag-element'],
    layout: {
      type: 'force',
      linkDistance: 150,
      preventOverlap: true,
      nodeStrength: -300,
    } as unknown as never,
    node: {
      style: {
        size: 40,
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

  await render();

  graph.on('node:click', (evt: any) => {
    const target = evt?.target as any;
    const id = target?.id ?? target?.getElementId?.() ?? evt?.itemId;
    const node = props.nodes.find((n) => n.id === id);
    if (node) emit('node-click', node);
  });
});

// 切换路线 → 只更新高亮（不重排布局）
watch(() => props.activeRouteId, (id) => applyHighlight(id));
// 数据变化 → 重渲染
watch(() => [props.nodes, props.edges], () => render());
// 搜索聚焦
watch(() => props.focusNodeId, (id) => { if (id && graph) graph.focusElement(id, { easing: 'linear', duration: 300 }); });

onBeforeUnmount(() => { graph?.destroy(); graph = null; });
</script>

<template>
  <div ref="container" class="graph-canvas" />
</template>

<style scoped>
.graph-canvas { width: 100%; height: 100%; }
</style>
