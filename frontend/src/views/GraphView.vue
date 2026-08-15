<script setup lang="ts">
/** 关系网核心页：路网视图 + 路线高亮 + 站点详情 + 路线条（地铁式） */
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { mockEdges, mockNodes, mockProcesses, mockRoutes } from '@/api/mockData';
import type { GraphNode } from '@/types';
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

const nodeTypeLegend: Record<string, string> = {
  SYSTEM: '系统', DATABASE: '数据库/表', DEPARTMENT: '部门/岗位', ACTION: '业务动作',
};

const colorOf: Record<string, string> = {
  SYSTEM: '#3b82f6', DATABASE: '#8b5cf6', DEPARTMENT: '#06b6d4', ACTION: '#f59e0b',
};

const routesOfProcess = computed(() => mockRoutes.filter((r) => r.processId === selectedProcessId.value));
const activeRoute = computed(() => mockRoutes.find((r) => r.id === activeRouteId.value) ?? null);
const canvasActiveRouteId = computed(() => (viewMode.value === 'route' ? activeRouteId.value : null));

function selectProcess(id: string) {
  selectedProcessId.value = id;
  const routes = mockRoutes.filter((r) => r.processId === id);
  const def = routes.find((r) => r.priority === 'DEFAULT') ?? routes[0];
  activeRouteId.value = def?.id ?? null;
}

function selectRoute(id: string) {
  activeRouteId.value = id;
  viewMode.value = 'route';
  selectedNode.value = null;
}

function nodeName(id: string) {
  return mockNodes.find((n) => n.id === id)?.name ?? id;
}

function stationClass(id: string) {
  const n = mockNodes.find((x) => x.id === id);
  if (!n) return '';
  if (n.status === 'FAIL') return 'station--fail';
  if (n.status === 'WARNING') return 'station--warn';
  return '';
}

function focusStation(id: string) {
  focusNodeId.value = id;
  selectedNode.value = mockNodes.find((n) => n.id === id) ?? null;
}

function onSearch() {
  const kw = keyword.value.trim();
  if (!kw) { focusNodeId.value = null; selectedNode.value = null; return; }
  const hit = mockNodes.find(
    (n) => n.name.includes(kw) || (n.code ?? '').includes(kw) || n.checkpoints.some((c) => c.name.includes(kw)),
  );
  if (hit) focusStation(hit.id);
}

onMounted(() => {
  const q = route.query.q as string | undefined;
  if (q) { keyword.value = q; onSearch(); }
});
</script>

<template>
  <div class="graph-page">
    <!-- 工具条 -->
    <div class="graph-toolbar">
      <div class="row">
        <select v-model="selectedProcessId" class="select process-select" @change="selectProcess(selectedProcessId)">
          <option v-for="p in mockProcesses" :key="p.id" :value="p.id">{{ p.name }}</option>
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
      <GraphCanvas
        :nodes="mockNodes" :edges="mockEdges" :routes="mockRoutes"
        :active-route-id="canvasActiveRouteId"
        :focus-node-id="focusNodeId"
        @node-click="selectedNode = $event"
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

      <!-- 详情抽屉 -->
      <NodeDetailPanel
        :node="selectedNode" :nodes="mockNodes" :edges="mockEdges"
        @close="selectedNode = null"
        @focus="(id) => { focusNodeId = id; selectedNode = mockNodes.find(n => n.id === id) ?? null }"
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
          <button class="station" :class="stationClass(nid)" @click="focusStation(nid)">
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
</style>
