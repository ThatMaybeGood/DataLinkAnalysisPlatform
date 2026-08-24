<script setup lang="ts">
/** 实例列表：全量业务实例追踪，支持状态过滤与分页 */
import { computed, onMounted, ref } from 'vue';
import { fetchInstances } from '@/api';
import type { Instance } from '@/types';
import Icon from '@/components/Icon.vue';
import Tag from '@/components/Tag.vue';

const instances = ref<Instance[]>([]);
const total = ref(0);
const page = ref(1);
const size = 10;
const loading = ref(true);
const loadError = ref('');
const statusFilter = ref('');

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '运行中', value: 'RUNNING' },
  { label: '成功', value: 'SUCCESS' },
  { label: '失败', value: 'FAIL' },
  { label: '卡住', value: 'STUCK' },
  { label: '超时', value: 'TIMEOUT' },
];

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)));

async function loadInstances() {
  loading.value = true;
  loadError.value = '';
  try {
    const data = await fetchInstances(page.value, size, statusFilter.value);
    instances.value = data.records;
    total.value = data.total;
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '实例加载失败';
  } finally {
    loading.value = false;
  }
}

function changePage(delta: number) {
  page.value += delta;
  void loadInstances();
}

function onStatusChange() {
  page.value = 1;
  void loadInstances();
}

onMounted(loadInstances);
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">实例列表</div>
        <div class="page-subtitle">全量业务实例追踪：单号、路线、当前站点与耗时</div>
      </div>
    </div>

    <div class="card card--table">
      <div class="card-header">
        <div class="card-title">全部实例</div>
        <div class="row">
          <select v-model="statusFilter" class="select filter-select" @change="onStatusChange">
            <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
          <span class="faint count-text">共 {{ total }} 条</span>
        </div>
      </div>

      <div v-if="loading" class="empty">
        <Icon name="activity" :size="32" />
        <div class="empty-title">实例加载中…</div>
      </div>
      <div v-else-if="loadError" class="empty">
        <Icon name="alert" :size="32" />
        <div class="empty-title">加载失败：{{ loadError }}</div>
        <button class="btn btn-outline btn-sm" @click="loadInstances">重试</button>
      </div>

      <template v-else>
        <table v-if="instances.length" class="data-table">
          <thead>
            <tr>
              <th>业务单号</th>
              <th>业务别名</th>
              <th>流程 / 路线</th>
              <th>状态</th>
              <th>进度</th>
              <th>当前站点</th>
              <th>开始时间</th>
              <th>耗时</th>
              <th>来源</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in instances" :key="item.id">
              <td class="cell-mono">{{ item.bizNo }}</td>
              <td class="cell-strong">{{ item.bizName }}</td>
              <td>
                <div>{{ item.processName }}</div>
                <div class="cell-muted">{{ item.routeName }}</div>
              </td>
              <td><Tag :status="item.status" /></td>
              <td>
                <div class="progress-track">
                  <div class="progress-bar" :style="{ width: `${item.progress}%` }" />
                </div>
                <span class="cell-muted">{{ item.progress }}%</span>
              </td>
              <td>{{ item.currentNode ?? '—' }}</td>
              <td class="cell-mono cell-muted">{{ item.startTime }}</td>
              <td class="cell-muted">{{ item.duration }}</td>
              <td><span class="tag tag--neutral">{{ item.source }}</span></td>
            </tr>
          </tbody>
        </table>

        <div v-else class="empty">
          <Icon name="search" :size="40" />
          <div class="empty-title">没有匹配的实例</div>
          <div class="empty-desc">试试调整状态筛选条件</div>
        </div>

        <div class="pagination">
          <span class="faint">第 {{ page }} / {{ totalPages }} 页</span>
          <button class="btn btn-outline btn-sm" :disabled="page <= 1" @click="changePage(-1)">上一页</button>
          <button class="btn btn-outline btn-sm" :disabled="page >= totalPages" @click="changePage(1)">下一页</button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.card--table { overflow: hidden; }
.filter-select { width: 140px; }
.count-text { font-size: 12px; margin-left: 8px; }
.progress-track {
  width: 80px; height: 6px; border-radius: 999px;
  background: var(--surface-2); overflow: hidden; display: inline-block; vertical-align: middle;
}
.progress-bar { height: 100%; border-radius: 999px; background: var(--accent); }
.pagination {
  display: flex; align-items: center; justify-content: flex-end; gap: 8px;
  padding: 12px 16px; border-top: 1px solid var(--border);
}
</style>
