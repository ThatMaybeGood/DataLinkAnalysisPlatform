<script setup lang="ts">
/** 配置版本页：真实 /api/versions 分页展示；对象类型走服务端筛选，状态走客户端筛选 */
import { computed, onMounted, ref } from 'vue';
import { fetchVersions } from '@/api';
import type { VersionRecord } from '@/types';
import Tag from '@/components/Tag.vue';
import Icon from '@/components/Icon.vue';

/** 对象类型下拉：展示值 → 后端枚举码（空 = 全部） */
const typeOptions: { label: string; value: string }[] = [
  { label: '全部', value: '' },
  { label: '流程', value: 'PROCESS' },
  { label: '节点', value: 'NODE' },
  { label: '路线', value: 'ROUTE' },
  { label: '检测点', value: 'CHECKPOINT' },
  { label: '连接器', value: 'CONNECTOR' },
];
/** 后端枚举码 → 中文标签 */
const targetTypeLabel: Record<string, string> = {
  PROCESS: '流程', NODE: '节点', ROUTE: '路线', CHECKPOINT: '检测点', CONNECTOR: '连接器',
};

const statusOptions = ['全部', '已发布', '待审批', '已回滚'];
const statusValue: Record<string, VersionRecord['status']> = {
  已发布: 'PUBLISHED',
  待审批: 'PENDING_APPROVAL',
  已回滚: 'ROLLED_BACK',
};

const typeFilter = ref('');
const statusFilter = ref('');

const records = ref<VersionRecord[]>([]);
const total = ref(0);
const page = ref(1);
const size = 10;
const loading = ref(true);
const loadError = ref('');

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size)));
/** 状态为客户端过滤（作用于当前页数据） */
const filtered = computed(() =>
  records.value.filter(
    (v) => statusFilter.value === '全部' || v.status === statusValue[statusFilter.value],
  ),
);

async function loadVersions() {
  loading.value = true;
  loadError.value = '';
  try {
    const data = await fetchVersions(page.value, size, typeFilter.value);
    records.value = data.records;
    total.value = data.total;
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '版本数据加载失败';
  } finally {
    loading.value = false;
  }
}

function changePage(delta: number) {
  page.value += delta;
  void loadVersions();
}
function onTypeChange() {
  page.value = 1;
  void loadVersions();
}

onMounted(loadVersions);
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">配置版本</div>
        <div class="page-subtitle">全平台配置留痕：可对比、可回滚、高等级变更需审批</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-outline btn-sm"><Icon name="export" :size="14" />导出记录</button>
      </div>
    </div>

    <!-- 顶部说明条 -->
    <div class="card mb-md">
      <div class="card-body">
        <div class="row muted">
          <Icon name="history" :size="15" />
          <span>版本留最近 20 版；L1/L2 配置变更默认需审批后发布</span>
        </div>
      </div>
    </div>

    <!-- 版本列表 -->
    <div class="card card--table">
      <div class="card-header version-header">
        <div class="row">
          <div class="card-title">版本列表</div>
          <span class="count-text muted faint">共 {{ total }} 条</span>
        </div>
        <div class="row">
          <select v-model="typeFilter" class="select filter-select" @change="onTypeChange">
            <option v-for="t in typeOptions" :key="t.value" :value="t.value">对象类型：{{ t.label }}</option>
          </select>
          <select v-model="statusFilter" class="select filter-select">
            <option v-for="s in statusOptions" :key="s" :value="s">状态：{{ s }}</option>
          </select>
        </div>
      </div>

      <!-- 加载 / 错误态 -->
      <div v-if="loading" class="empty">
        <Icon name="activity" :size="32" />
        <div class="empty-title">版本记录加载中…</div>
      </div>
      <div v-else-if="loadError" class="empty">
        <Icon name="alert" :size="32" />
        <div class="empty-title">加载失败：{{ loadError }}</div>
        <div class="empty-desc">请确认后端服务可用后重试</div>
        <button class="btn btn-outline btn-sm" @click="loadVersions">重试</button>
      </div>

      <template v-else>
        <table v-if="filtered.length" class="data-table">
          <thead>
            <tr>
              <th>对象类型</th>
              <th>对象名</th>
              <th>版本号</th>
              <th>操作人</th>
              <th>改动说明</th>
              <th>状态</th>
              <th>时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filtered" :key="item.id">
              <td><span class="tag tag--neutral">{{ targetTypeLabel[item.targetType] ?? item.targetType }}</span></td>
              <td class="cell-strong">{{ item.targetName }}</td>
              <td><span class="cell-mono">v{{ item.version }}</span></td>
              <td class="cell-muted">{{ item.operator }}</td>
              <td><span class="cell-truncate" :title="item.changeNote">{{ item.changeNote }}</span></td>
              <td><Tag :status="item.status" /></td>
              <td><span class="cell-mono">{{ item.time }}</span></td>
              <td>
                <div class="row ops">
                  <button class="btn btn-outline btn-sm">查看</button>
                  <button class="btn btn-ghost btn-sm">对比</button>
                  <button v-if="item.status === 'PENDING_APPROVAL'" class="btn btn-primary btn-sm">审批</button>
                  <button v-else-if="item.status === 'ROLLED_BACK'" class="btn btn-danger btn-sm">重新发布</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- 空结果 -->
        <div v-else class="empty">
          <Icon name="history" :size="40" />
          <div class="empty-title">没有匹配的版本记录</div>
          <div class="empty-desc">试试调整对象类型或状态筛选条件</div>
        </div>

        <!-- 分页 -->
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
.version-header { flex-wrap: wrap; gap: var(--space-sm); }
.count-text { font-size: 12px; margin-left: 2px; }
.filter-select { width: 170px; }
.cell-truncate {
  display: inline-block; max-width: 280px; vertical-align: middle;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ops { white-space: nowrap; }
.pagination {
  display: flex; align-items: center; justify-content: flex-end; gap: 8px;
  padding: 12px 16px; border-top: 1px solid var(--border);
}
</style>
