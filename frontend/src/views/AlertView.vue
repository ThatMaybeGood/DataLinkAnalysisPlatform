<script setup lang="ts">
/** 告警中心：等级驱动的预警与工单处置 */
import { computed, onMounted, ref } from 'vue';
import { fetchAlerts, resolveAlert } from '@/api';
import type { AlertItem } from '@/types';
import Icon from '@/components/Icon.vue';
import Tag from '@/components/Tag.vue';
import StatCard from '@/components/StatCard.vue';

const statusFilter = ref('');
const severityFilter = ref('');

const alerts = ref<AlertItem[]>([]);
const loading = ref(true);
const loadError = ref('');
const resolvingId = ref('');

async function loadAlerts() {
  loading.value = true;
  loadError.value = '';
  try {
    alerts.value = await fetchAlerts();
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '告警数据加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadAlerts);

const filtered = computed(() =>
  alerts.value.filter((a) => {
    if (statusFilter.value && a.status !== statusFilter.value) return false;
    if (severityFilter.value && a.severity !== severityFilter.value) return false;
    return true;
  }),
);

/* —— 统计 —— */
const openCount = computed(() => alerts.value.filter((a) => a.status === 'OPEN').length);
const resolvedCount = computed(() => alerts.value.filter((a) => a.status === 'RESOLVED').length);
const todayNew = 5;

/* —— 徽标映射 —— */
const severityMeta: Record<string, string> = {
  P0: 'tag--danger', P1: 'tag--danger', P2: 'tag--warning', P3: 'tag--neutral',
};

const typeMeta: Record<string, { label: string; cls: string }> = {
  STUCK: { label: '卡住', cls: 'tag--danger' },
  FAIL: { label: '失败', cls: 'tag--danger' },
  TIMEOUT: { label: '超时', cls: 'tag--warning' },
  CHECK_FAIL: { label: '校验失败', cls: 'tag--warning' },
};

const targetTypeLabel: Record<string, string> = {
  NODE: '节点', INSTANCE: '实例', ROUTE: '路线', PROCESS: '流程',
};

/* —— 处理（关闭）告警，成功后刷新列表 —— */
async function handleResolve(alert: AlertItem) {
  resolvingId.value = alert.id;
  loadError.value = '';
  try {
    await resolveAlert(alert.id);
    await loadAlerts();
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '告警处理失败';
  } finally {
    resolvingId.value = '';
  }
}
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">告警中心</h1>
        <p class="page-subtitle">等级驱动的预警与工单处置</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-ghost">
          <Icon name="export" :size="15" />导出
        </button>
        <button class="btn btn-primary">
          <Icon name="plus" :size="15" />新建告警规则
        </button>
      </div>
    </header>

    <div class="stat-grid mb-md">
      <StatCard label="未处理" :value="openCount" sub="需及时跟进处置" icon="alert" color="danger" />
      <StatCard label="今日新增" :value="todayNew" sub="较昨日 +2" icon="activity" />
      <StatCard label="已解决" :value="resolvedCount" sub="今日已完成处置" icon="check" color="success" />
    </div>

    <div class="card mb-md">
      <div class="card-body">
        <div class="row-between">
          <div class="row">
            <select v-model="statusFilter" class="select filter-select">
              <option value="">全部状态</option>
              <option value="OPEN">待处理</option>
              <option value="RESOLVED">已解决</option>
            </select>
            <select v-model="severityFilter" class="select filter-select">
              <option value="">全部级别</option>
              <option value="P0">P0</option>
              <option value="P1">P1</option>
              <option value="P2">P2</option>
              <option value="P3">P3</option>
            </select>
          </div>
          <div class="faint">共 {{ filtered.length }} 条</div>
        </div>
      </div>
    </div>

    <!-- 加载 / 错误提示 -->
    <div v-if="loadError" class="alert-banner">
      <Icon name="alert" :size="14" />
      <span>{{ loadError }}</span>
      <button class="btn btn-ghost btn-sm" @click="loadAlerts">重试</button>
    </div>

    <div class="card">
      <div class="card-body">
        <div v-if="loading" class="empty">
          <Icon name="activity" :size="28" />
          <div class="empty-title">告警加载中…</div>
        </div>
        <template v-else>
          <table class="data-table">
            <thead>
              <tr>
                <th>级别</th>
                <th>类型</th>
                <th>目标</th>
                <th>消息</th>
                <th>等级</th>
                <th>时间</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="alert in filtered" :key="alert.id">
                <td><span class="tag" :class="severityMeta[alert.severity]">{{ alert.severity }}</span></td>
                <td><span class="tag" :class="typeMeta[alert.type].cls">{{ typeMeta[alert.type].label }}</span></td>
                <td>
                  <div class="cell-strong">{{ alert.targetName }}</div>
                  <div class="cell-muted">{{ targetTypeLabel[alert.targetType] ?? alert.targetType }}</div>
                </td>
                <td><div class="msg-cell">{{ alert.message }}</div></td>
                <td><span class="lv" :class="`lv--${alert.level}`">{{ alert.level }}</span></td>
                <td><span class="cell-mono cell-muted">{{ alert.time }}</span></td>
                <td><Tag :status="alert.status" /></td>
                <td>
                  <div class="row">
                    <template v-if="alert.status === 'OPEN'">
                      <button
                        class="btn btn-outline btn-sm" :disabled="resolvingId === alert.id"
                        @click="handleResolve(alert)"
                      >{{ resolvingId === alert.id ? '处理中…' : '处理' }}</button>
                      <button class="btn btn-ghost btn-sm">生成工单</button>
                    </template>
                    <template v-else>
                      <button class="btn btn-ghost btn-sm">查看</button>
                    </template>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div v-if="filtered.length === 0" class="empty">
            <Icon name="check" :size="28" />
            <div class="empty-title">当前没有符合条件的告警</div>
            <div class="empty-desc">调整筛选条件，或所有告警均已处理</div>
          </div>
        </template>
      </div>
    </div>

    <div class="card mt-md">
      <div class="card-body">
        <div class="row muted note">
          <Icon name="alert" :size="14" />
          <span>告警按节点等级分级：L1 立即告警+升级，L2 失败/超时告警，L3 汇总提醒，L4 仅记录</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filter-select { width: 130px; flex-shrink: 0; }
.msg-cell {
  max-width: 340px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.note { font-size: 12.5px; }

/* 错误提示条 */
.alert-banner {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; margin-bottom: 12px;
  border-radius: var(--radius-sm);
  background: var(--danger-soft); color: var(--danger);
  border: 1px solid rgba(220, 38, 38, 0.35);
  font-size: 12.5px;
}
</style>
