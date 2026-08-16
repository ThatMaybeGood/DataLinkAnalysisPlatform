<script setup lang="ts">
/** M4 · 3D 视图：复用 Graph3DCanvas（星系感空间关系网）
 *  保留页面自身数据加载 + 工具条（路线下拉 / 自动旋转 / 重置视角） */
import { onMounted, ref } from 'vue';
import { fetchEdges, fetchNodes, fetchRoutes } from '@/api';
import type { GraphEdge, GraphNode, Route } from '@/types';
import { nodeTypeLabel } from '@/api/mockData';
import Graph3DCanvas, {
  HUB_NODE_COLOR, ROUTE_NODE_COLOR, legendKeys, nodeColorMap,
} from '@/components/Graph3DCanvas.vue';
import Icon from '@/components/Icon.vue';

const loading = ref(true);
const loadError = ref('');
const autoRotate = ref(true);

const nodes = ref<GraphNode[]>([]);
const edges = ref<GraphEdge[]>([]);
const routes = ref<Route[]>([]);

const activeRouteId = ref(''); // 路线聚焦（下拉选择，组件内部 watch 生效）

const canvasRef = ref<InstanceType<typeof Graph3DCanvas> | null>(null);

function toggleAutoRotate() {
  autoRotate.value = !autoRotate.value;
}

function resetView() {
  canvasRef.value?.resetView();
}

async function loadData() {
  loading.value = true;
  loadError.value = '';
  try {
    const [n, e, r] = await Promise.all([fetchNodes(), fetchEdges(), fetchRoutes()]);
    nodes.value = n;
    edges.value = e;
    routes.value = r;
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
        <select v-model="activeRouteId" class="select route-select">
          <option value="">全路网</option>
          <option v-for="r in routes" :key="r.id" :value="r.id">{{ r.name }}</option>
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

      <Graph3DCanvas
        ref="canvasRef"
        :nodes="nodes" :edges="edges" :routes="routes"
        :active-route-id="activeRouteId"
        :auto-rotate="autoRotate"
      />

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
}

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
