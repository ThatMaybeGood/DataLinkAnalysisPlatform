<script setup lang="ts">
/** 检测点：站点上的哨兵——左侧站点列表，点击查看该站点检测点清单 */
import { computed, onMounted, ref } from 'vue';
import { fetchCheckpoints, fetchNodes } from '@/api';
import { nodeTypeLabel } from '@/api/mockData';
import type { Checkpoint, GraphNode } from '@/types';
import Icon from '@/components/Icon.vue';
import Tag from '@/components/Tag.vue';
import StatCard from '@/components/StatCard.vue';

const nodes = ref<GraphNode[]>([]);
const selectedNode = ref<GraphNode | null>(null);
const checkpoints = ref<Checkpoint[]>([]);
const loading = ref(true);
const loadError = ref('');
const cpLoading = ref(false);
const cpError = ref('');

const keyword = ref('');
const statusFilter = ref('');
const kindFilter = ref('');

/** 加载站点列表（默认选中第一个） */
async function loadNodes() {
  loading.value = true;
  loadError.value = '';
  try {
    nodes.value = await fetchNodes();
    if (nodes.value.length > 0) await selectNode(nodes.value[0]);
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '站点加载失败';
  } finally {
    loading.value = false;
  }
}

/** 选中站点 → 拉取该站点的检测点 */
async function selectNode(node: GraphNode) {
  selectedNode.value = node;
  keyword.value = '';
  statusFilter.value = '';
  kindFilter.value = '';
  cpLoading.value = true;
  cpError.value = '';
  checkpoints.value = [];
  try {
    checkpoints.value = await fetchCheckpoints(node.id);
  } catch (err) {
    cpError.value = err instanceof Error ? err.message : '检测点加载失败';
  } finally {
    cpLoading.value = false;
  }
}

onMounted(loadNodes);

/* —— 过滤（基于当前站点检测点） —— */
const filtered = computed(() => {
  const kw = keyword.value.trim();
  return checkpoints.value.filter((item) => {
    if (kw && !item.name.includes(kw)) return false;
    if (statusFilter.value && item.status !== statusFilter.value) return false;
    if (kindFilter.value && item.kind !== kindFilter.value) return false;
    return true;
  });
});

/* —— 统计（基于当前站点） —— */
const total = computed(() => checkpoints.value.length);
const abnormal = computed(() => checkpoints.value.filter((c) => c.status === 'FAIL' || c.status === 'TIMEOUT').length);
const passed = computed(() => checkpoints.value.filter((c) => c.status === 'PASS').length);
const passRate = computed(() => (total.value ? Math.round((passed.value / total.value) * 100) : 0));

/* —— 类型徽标 —— */
const kindMeta: Record<string, { label: string; cls: string }> = {
  DEFAULT: { label: '默认', cls: 'tag--accent' },
  CUSTOM: { label: '自定义', cls: 'tag--neutral' },
};

/* —— 站点状态圆点 —— */
const nodeDot: Record<string, string> = {
  ACTIVE: 'dot--ok', WARNING: 'dot--warn', FAIL: 'dot--fail', DISABLED: 'dot--off',
};
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">检测点</h1>
        <p class="page-subtitle">站点上的哨兵：左侧选择站点，查看其检测点清单</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary">
          <Icon name="plus" :size="15" />新建检测点
        </button>
      </div>
    </header>

    <!-- 加载 / 错误态 -->
    <div v-if="loading" class="page-status">站点加载中…</div>
    <div v-else-if="loadError" class="page-status page-status--error">
      <span>加载失败：{{ loadError }}</span>
      <button class="btn btn-outline btn-sm" @click="loadNodes">重试</button>
    </div>

    <template v-else>
      <div class="stat-grid mb-md">
        <StatCard label="检测点总数" :value="total" :sub="`站点：${selectedNode?.name ?? '—'}`" icon="target" />
        <StatCard label="异常 / 失败" :value="abnormal" sub="需关注的检测项" icon="alert" color="danger" />
        <StatCard label="通过率" :value="`${passRate}%`" :sub="`通过 ${passed} / ${total}`" icon="check" color="success" />
      </div>

      <div class="grid check-grid">
        <!-- 站点列表 -->
        <div class="card check-side">
          <div class="card-header">
            <h2 class="card-title">站点（{{ nodes.length }}）</h2>
          </div>
          <div class="card-body node-list">
            <button
              v-for="n in nodes" :key="n.id"
              class="node-item" :class="{ 'node-item--active': selectedNode?.id === n.id }"
              @click="selectNode(n)"
            >
              <span class="node-dot" :class="nodeDot[n.status] ?? 'dot--off'" />
              <span class="node-name">{{ n.name }}</span>
              <span class="node-meta">{{ nodeTypeLabel[n.nodeType] ?? n.nodeType }}</span>
              <span class="node-count mono">{{ n.checkpoints.length }}</span>
            </button>
            <div v-if="nodes.length === 0" class="empty">
              <Icon name="search" :size="24" />
              <div class="empty-title">暂无站点</div>
            </div>
          </div>
        </div>

        <!-- 检测点明细 -->
        <div class="card check-main">
          <div class="card-header">
            <h2 class="card-title">{{ selectedNode?.name ?? '检测点' }} · 检测点</h2>
          </div>
          <div class="card-body">
            <div class="row-between filter-bar">
              <div class="row">
                <div class="search">
                  <span class="search-icon"><Icon name="search" :size="14" /></span>
                  <input v-model="keyword" placeholder="搜索检测点…" />
                </div>
                <select v-model="statusFilter" class="select filter-select">
                  <option value="">全部状态</option>
                  <option value="PASS">通过</option>
                  <option value="FAIL">失败</option>
                  <option value="TIMEOUT">超时</option>
                  <option value="WARNING">异常</option>
                </select>
                <select v-model="kindFilter" class="select filter-select">
                  <option value="">全部类型</option>
                  <option value="DEFAULT">默认</option>
                  <option value="CUSTOM">自定义</option>
                </select>
              </div>
              <div class="faint">共 {{ filtered.length }} 项</div>
            </div>

            <div v-if="cpLoading" class="empty">
              <Icon name="activity" :size="28" />
              <div class="empty-title">检测点加载中…</div>
            </div>
            <div v-else-if="cpError" class="empty">
              <Icon name="alert" :size="28" />
              <div class="empty-title">{{ cpError }}</div>
              <button class="btn btn-outline btn-sm mt-sm" @click="selectedNode && selectNode(selectedNode)">重试</button>
            </div>
            <template v-else>
              <table class="data-table">
                <thead>
                  <tr>
                    <th>检测点名称</th>
                    <th>类型</th>
                    <th>检测类型</th>
                    <th>状态</th>
                    <th>最近检测时间</th>
                    <th>操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="item in filtered" :key="item.id">
                    <td>{{ item.name }}</td>
                    <td><span class="tag" :class="kindMeta[item.kind].cls">{{ kindMeta[item.kind].label }}</span></td>
                    <td><span class="cell-mono">{{ item.checkType }}</span></td>
                    <td><Tag :status="item.status" /></td>
                    <td class="cell-muted">{{ item.lastCheck }}</td>
                    <td>
                      <div class="row">
                        <button class="btn btn-ghost btn-sm">配置</button>
                        <button class="btn btn-outline btn-sm">立即检测</button>
                      </div>
                    </td>
                  </tr>
                </tbody>
              </table>

              <div v-if="filtered.length === 0" class="empty">
                <Icon name="search" :size="28" />
                <div class="empty-title">该站点暂无匹配的检测点</div>
                <div class="empty-desc">试试调整搜索关键词或筛选条件</div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.filter-select { width: 130px; flex-shrink: 0; }

/* 加载 / 错误态 */
.page-status {
  display: flex; align-items: center; justify-content: center; gap: 12px;
  padding: 48px 24px; background: var(--surface);
  border: 1px solid var(--border); border-radius: var(--radius-lg);
  color: var(--fg-muted); font-size: 13.5px;
}
.page-status--error { color: var(--danger); }

/* 左站点 / 右检测点 双栏布局 */
.check-grid {
  grid-template-columns: 280px minmax(0, 1fr);
  align-items: start;
}
.check-side, .check-main { min-width: 0; }
.filter-bar { margin-bottom: var(--space-md); }
.mt-sm { margin-top: var(--space-sm); }

.node-list {
  display: flex; flex-direction: column; gap: 2px;
  max-height: calc(100vh - 300px); overflow-y: auto;
  padding: 10px;
}
.node-item {
  display: flex; align-items: center; gap: 8px;
  width: 100%; padding: 9px 10px; border-radius: var(--radius-sm);
  border: 1px solid transparent; background: transparent;
  font-size: 13px; color: var(--fg); cursor: pointer; text-align: left;
  transition: background .12s ease, border-color .12s ease;
}
.node-item:hover { background: var(--surface-2); }
.node-item--active { background: var(--accent-soft); border-color: var(--accent-border); }
.node-name { font-weight: 600; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.node-meta { font-size: 11px; color: var(--fg-faint); flex-shrink: 0; }
.node-count { font-size: 11px; color: var(--fg-muted); flex-shrink: 0; min-width: 22px; text-align: right; }
.node-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.dot--ok { background: var(--success); }
.dot--warn { background: var(--warning); }
.dot--fail { background: var(--danger); }
.dot--off { background: var(--fg-faint); }
</style>
