<script setup lang="ts">
/** 关系网核心页：路网视图 + 路线高亮 + 站点详情 + 路线条（地铁式） */
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { fetchEdges, fetchGraphTrace, fetchNodes, fetchProcesses, fetchRoutes } from '@/api';
import type { GraphTrace } from '@/api';
import type { GraphEdge, GraphNode, ProcessDef, Route } from '@/types';
import GraphCanvas from '@/components/GraphCanvas.vue';
import NodeDetailPanel from '@/components/NodeDetailPanel.vue';
import Icon from '@/components/Icon.vue';

const route = useRoute();
const selectedProcessId = ref('p1');
const activeRouteId = ref<string | null>('r1');
const viewMode = ref<'network' | 'route'>('route');
const selectedNode = ref<GraphNode | null>(null);
const keyword = ref('');
const focusNodeId = ref<string | null>(null);

const nodes = ref<GraphNode[]>([]);
const edges = ref<GraphEdge[]>([]);
const processes = ref<ProcessDef[]>([]);
const routes = ref<Route[]>([]);
const loading = ref(true);
const loadError = ref('');

const nodeTypeLegend: Record<string, string> = {
  SYSTEM: '系统', DATABASE: '数据库/表', DEPARTMENT: '部门/岗位', ACTION: '业务动作',
};

const colorOf: Record<string, string> = {
  SYSTEM: '#3b82f6', DATABASE: '#8b5cf6', DEPARTMENT: '#06b6d4', ACTION: '#f59e0b',
};

const routesOfProcess = computed(() => routes.value.filter((r) => r.processId === selectedProcessId.value));
const activeRoute = computed(() => routes.value.find((r) => r.id === activeRouteId.value) ?? null);
const canvasActiveRouteId = computed(() => (viewMode.value === 'route' ? activeRouteId.value : null));

function selectProcess(id: string) {
  selectedProcessId.value = id;
  const rs = routes.value.filter((r) => r.processId === id);
  const def = rs.find((r) => r.priority === 'DEFAULT') ?? rs[0];
  activeRouteId.value = def?.id ?? null;
}

function selectRoute(id: string) {
  activeRouteId.value = id;
  viewMode.value = 'route';
  selectedNode.value = null;
}

function nodeName(id: string) {
  return nodes.value.find((n) => n.id === id)?.name ?? id;
}

function stationClass(id: string) {
  const n = nodes.value.find((x) => x.id === id);
  if (!n) return '';
  if (n.status === 'FAIL') return 'station--fail';
  if (n.status === 'WARNING') return 'station--warn';
  return '';
}

/* —— 排查模式（顺藤摸瓜）：选中节点 → 请求上游 / 下游 —— */
const trace = ref<GraphTrace | null>(null);
const traceLoading = ref(false);

const traceNode = computed(() => nodes.value.find((n) => n.id === trace.value?.nodeId) ?? null);
const upstreamIds = computed(() => new Set((trace.value?.upstream ?? []).map((n) => n.id)));
const downstreamIds = computed(() => new Set((trace.value?.downstream ?? []).map((n) => n.id)));

/** 点击/选中节点：打开详情并请求上下游追踪 */
async function onNodeClick(node: GraphNode) {
  selectedNode.value = node;
  focusNodeId.value = node.id;
  traceLoading.value = true;
  try {
    trace.value = await fetchGraphTrace(node.id);
  } catch {
    trace.value = null; // 追踪失败不影响原有布局
  } finally {
    traceLoading.value = false;
  }
}

function closeTrace() {
  trace.value = null;
  traceLoading.value = false;
}

/** 路线条站点：命中上游 / 下游时附加高亮类 */
function traceStationClass(id: string) {
  if (upstreamIds.value.has(id)) return 'station--trace-up';
  if (downstreamIds.value.has(id)) return 'station--trace-down';
  return '';
}

function focusStation(id: string) {
  const n = nodes.value.find((x) => x.id === id) ?? null;
  focusNodeId.value = id;
  selectedNode.value = n;
  if (n) void onNodeClick(n);
}

function onSearch() {
  const kw = keyword.value.trim();
  if (!kw) { focusNodeId.value = null; selectedNode.value = null; return; }
  const hit = nodes.value.find(
    (n) => n.name.includes(kw) || (n.code ?? '').includes(kw) || n.checkpoints.some((c) => c.name.includes(kw)),
  );
  if (hit) focusStation(hit.id);
}

/** 加载关系网数据（供重试复用） */
async function loadGraph() {
  loading.value = true;
  loadError.value = '';
  try {
    const [n, e, p, r] = await Promise.all([fetchNodes(), fetchEdges(), fetchProcesses(), fetchRoutes()]);
    nodes.value = n;
    edges.value = e;
    processes.value = p;
    routes.value = r;
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '关系网数据加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  await loadGraph();
  const q = route.query.q as string | undefined;
  if (q && !loadError.value) { keyword.value = q; onSearch(); }
});
</script>

<template>
  <div class="graph-page">
    <!-- 工具条 -->
    <div class="graph-toolbar">
      <div class="row">
        <select v-model="selectedProcessId" class="select process-select" @change="selectProcess(selectedProcessId)">
          <option v-for="p in processes" :key="p.id" :value="p.id">{{ p.name }}</option>
        </select>

        <div class="route-chips">
          <button
            v-for="r in routesOfProcess" :key="r.id"
            class="route-chip" :class="{ 'route-chip--active': activeRouteId === r.id && viewMode === 'route' }"
            @click="selectRoute(r.id)"
          >
            <span class="chip-tag" :class="{ 'chip-tag--alt': r.priority === 'ALTERNATE' }">
              {{ r.priority === 'DEFAULT' ? '默认' : r.priority === 'RECOMMENDED' ? '推荐' : '备选' }}
            </span>
            {{ r.name }}
          </button>
          <button
            class="route-chip" :class="{ 'route-chip--active': viewMode === 'network' }"
            @click="viewMode = 'network'; activeRouteId = null; selectedNode = null"
          >全路网</button>
        </div>
      </div>

      <div class="row">
        <div class="search">
          <span class="search-icon"><Icon name="search" :size="14" /></span>
          <input v-model="keyword" placeholder="搜索站点 / 别名…" @keyup.enter="onSearch" />
        </div>
      </div>
    </div>

    <!-- 画布区 -->
    <div class="graph-body">
      <!-- 加载 / 错误态 -->
      <div v-if="loading" class="graph-status">加载关系网数据…</div>
      <div v-else-if="loadError" class="graph-status graph-status--error">
        <span>加载失败：{{ loadError }}</span>
        <button class="retry-btn" @click="loadGraph">重试</button>
      </div>

      <GraphCanvas
        :nodes="nodes" :edges="edges" :routes="routes"
        :active-route-id="canvasActiveRouteId"
        :focus-node-id="focusNodeId"
        @node-click="onNodeClick"
      />

      <!-- 图例 -->
      <div class="legend">
        <div class="legend-title">图例</div>
        <div class="legend-item" v-for="(label, key) in nodeTypeLegend" :key="key">
          <span class="legend-dot" :style="{ background: colorOf[key] }" />{{ label }}
        </div>
        <div class="legend-divider" />
        <div class="legend-item"><span class="legend-ring legend-ring--warn" />异常</div>
        <div class="legend-item"><span class="legend-ring legend-ring--fail" />失败</div>
      </div>

      <!-- 排查浮层（顺藤摸瓜）：上游 / 下游 -->
      <div v-if="trace" class="trace-panel">
        <div class="trace-header">
          <span class="trace-name">排查 · {{ traceNode?.name ?? trace.nodeId }}</span>
          <button class="trace-close" @click="closeTrace" aria-label="关闭排查">
            <Icon name="chevron" :size="12" />
          </button>
        </div>
        <div v-if="traceLoading" class="trace-meta">上下游追踪中…</div>
        <template v-else>
          <div class="trace-meta">上游 {{ trace.upstream.length }} 个 · 下游 {{ trace.downstream.length }} 个</div>
          <div v-if="trace.upstream.length" class="trace-group">
            <div class="trace-group-title trace-group-title--up">上游</div>
            <button v-for="n in trace.upstream" :key="n.id" class="trace-node trace-node--up" @click="focusStation(n.id)">
              <span class="trace-node-name">{{ n.name }}</span>
              <span class="faint mono">{{ n.nodeType }}</span>
            </button>
          </div>
          <div v-if="trace.downstream.length" class="trace-group">
            <div class="trace-group-title trace-group-title--down">下游</div>
            <button v-for="n in trace.downstream" :key="n.id" class="trace-node trace-node--down" @click="focusStation(n.id)">
              <span class="trace-node-name">{{ n.name }}</span>
              <span class="faint mono">{{ n.nodeType }}</span>
            </button>
          </div>
          <div v-if="!trace.upstream.length && !trace.downstream.length" class="trace-meta">该节点无上下游关系</div>
        </template>
      </div>

      <!-- 详情抽屉 -->
      <NodeDetailPanel
        :node="selectedNode" :nodes="nodes" :edges="edges"
        @close="selectedNode = null"
        @focus="focusStation"
      />
    </div>

    <!-- 路线条（地铁式） -->
    <div v-if="activeRoute && viewMode === 'route'" class="route-strip">
      <div class="route-strip-name">
        {{ activeRoute.name }}
        <span class="strip-sub">· {{ activeRoute.nodeIds.length }} 站 · {{ activeRoute.totalDuration }}</span>
      </div>
      <div class="strip">
        <template v-for="(nid, idx) in activeRoute.nodeIds" :key="nid">
          <button class="station" :class="[stationClass(nid), traceStationClass(nid)]" @click="focusStation(nid)">
            <span class="station-name">{{ nodeName(nid) }}</span>
          </button>
          <span v-if="idx < activeRoute.nodeIds.length - 1" class="station-link" />
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.graph-page {
  display: flex; flex-direction: column;
  height: calc(100vh - var(--topbar-h));
}

.graph-toolbar {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 12px 24px; background: var(--surface);
  border-bottom: 1px solid var(--border); flex-shrink: 0; flex-wrap: wrap;
}
.process-select { width: 200px; }
.route-chips { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.route-chip {
  display: inline-flex; align-items: center; gap: 6px; height: 30px; padding: 0 12px;
  border-radius: 999px; border: 1px solid var(--border); background: var(--surface);
  font-size: 12.5px; color: var(--fg-muted); cursor: pointer; transition: all .15s;
}
.route-chip:hover { border-color: var(--accent-border); color: var(--accent); }
.route-chip--active { background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 500; }
.chip-tag {
  font-size: 10px; padding: 1px 6px; border-radius: 999px;
  background: var(--accent-soft); color: var(--accent); font-weight: 600;
}
.chip-tag--alt { background: var(--warning-soft); color: var(--warning); }
.route-chip--active .chip-tag { background: rgba(255,255,255,.22); color: #fff; }
.route-chip--active .chip-tag--alt { background: rgba(255,255,255,.22); color: #fff; }

.graph-body {
  position: relative; flex: 1; background: var(--canvas-bg);
  background-image: radial-gradient(rgba(117,131,164,.09) 1px, transparent 1px);
  background-size: 22px 22px;
  min-height: 0; overflow: hidden;
}

.graph-status {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%);
  z-index: 6; display: flex; align-items: center; gap: 12px;
  padding: 12px 18px; border-radius: var(--radius);
  background: rgba(13,20,36,.92); border: 1px solid var(--canvas-border);
  color: var(--canvas-fg); font-size: 13px; box-shadow: var(--shadow-canvas);
}
.graph-status--error { color: #fca5a5; }
.retry-btn {
  height: 28px; padding: 0 12px; border-radius: 6px; border: 1px solid var(--canvas-border);
  background: var(--accent); color: #fff; font-size: 12px; cursor: pointer; transition: opacity .15s;
}
.retry-btn:hover { opacity: .88; }

.legend {
  position: absolute; left: 16px; bottom: 16px; z-index: 4;
  background: rgba(13,20,36,.9); border: 1px solid var(--canvas-border);
  border-radius: var(--radius); padding: 10px 12px; font-size: 12px; color: var(--canvas-fg);
  box-shadow: var(--shadow-canvas);
}
.legend-title { font-size: 11px; color: var(--canvas-fg-dim); letter-spacing: .06em; margin-bottom: 6px; }
.legend-item { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.legend-dot { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.legend-ring { width: 14px; height: 14px; border-radius: 50%; flex-shrink: 0; border: 3px solid; background: transparent; }
.legend-ring--warn { border-color: var(--warning); }
.legend-ring--fail { border-color: var(--danger); }
.legend-divider { border-top: 1px solid var(--canvas-border); margin: 6px 0; }

.route-strip {
  flex-shrink: 0; background: var(--canvas-bg);
  border-top: 1px solid var(--canvas-border); padding: 12px 24px 16px;
}
.route-strip-name {
  font-size: 13px; font-weight: 600; color: #fff; margin-bottom: 10px; display: flex; align-items: center; gap: 6px;
}
.strip-sub { font-size: 12px; color: var(--canvas-fg-dim); font-weight: 400; }
.strip { display: flex; align-items: center; overflow-x: auto; padding-bottom: 4px; }
.station {
  display: flex; flex-direction: column; align-items: center; gap: 6px;
  border: none; background: transparent; cursor: pointer; padding: 0 6px; min-width: 96px;
  position: relative;
}
.station::before {
  content: ''; width: 14px; height: 14px; border-radius: 50%;
  background: var(--node-line); border: 3px solid var(--canvas-bg); box-sizing: content-box;
  box-shadow: 0 0 0 2px var(--node-line); transition: all .15s;
}
.station:hover::before { background: #fff; box-shadow: 0 0 0 2px #fff; }
.station--warn::before { background: var(--warning); box-shadow: 0 0 0 2px var(--warning); }
.station--fail::before { background: var(--danger); box-shadow: 0 0 0 2px var(--danger); }
.station-name { font-size: 11.5px; color: var(--canvas-fg); white-space: nowrap; }
.station:hover .station-name { color: #fff; }
.station--warn .station-name { color: #fcd34d; }
.station--fail .station-name { color: #fca5a5; }
.station-link {
  flex: 1; min-width: 40px; height: 3px; background: var(--node-line); border-radius: 2px;
}

/* 排查模式：路线条站点高亮（上游绿环 / 下游红环） */
.station--trace-up::before { box-shadow: 0 0 0 2px #10b981, 0 0 8px rgba(16, 185, 129, .55); }
.station--trace-down::before { box-shadow: 0 0 0 2px #ef4444, 0 0 8px rgba(239, 68, 68, .55); }
.station--trace-up .station-name { color: #6ee7b7; }
.station--trace-down .station-name { color: #fca5a5; }

/* 排查浮层（画布左上角，与图例同风格） */
.trace-panel {
  position: absolute; left: 16px; top: 16px; z-index: 4;
  width: 224px; max-height: calc(100% - 32px); overflow-y: auto;
  background: rgba(13, 20, 36, .92); border: 1px solid var(--canvas-border);
  border-radius: var(--radius); padding: 12px; font-size: 12px;
  color: var(--canvas-fg); box-shadow: var(--shadow-canvas);
}
.trace-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.trace-name {
  font-weight: 600; font-size: 12.5px; overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap;
}
.trace-close {
  width: 20px; height: 20px; border-radius: 5px; border: none; flex-shrink: 0;
  background: rgba(255, 255, 255, .06); color: var(--canvas-fg); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transform: rotate(180deg);
}
.trace-close:hover { background: rgba(255, 255, 255, .16); color: #fff; }
.trace-meta { margin-top: 8px; color: var(--canvas-fg-dim); }
.trace-group { margin-top: 10px; }
.trace-group-title { font-size: 11px; letter-spacing: .06em; margin-bottom: 4px; }
.trace-group-title--up { color: #6ee7b7; }
.trace-group-title--down { color: #fca5a5; }
.trace-node {
  display: flex; align-items: center; justify-content: space-between; gap: 6px;
  width: 100%; padding: 5px 8px; margin-bottom: 2px; border-radius: 6px; border: none;
  background: transparent; color: var(--canvas-fg); font-size: 12px;
  cursor: pointer; text-align: left; transition: background .12s ease;
}
.trace-node:hover { background: rgba(255, 255, 255, .08); }
.trace-node--up:hover { color: #6ee7b7; }
.trace-node--down:hover { color: #fca5a5; }
.trace-node-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
