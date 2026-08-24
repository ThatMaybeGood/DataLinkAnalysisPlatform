<script setup lang="ts">
/** 告警中心：等级驱动的预警与工单处置 */
import { computed, onMounted, ref } from 'vue';
import { createAlert, fetchAlerts, resolveAlert } from '@/api';
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
const toast = ref('');

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

function showToast(message: string) {
  toast.value = message;
  window.setTimeout(() => (toast.value = ''), 2000);
}

/* —— 处理（关闭）告警，成功后刷新列表 —— */
async function handleResolve(alert: AlertItem) {
  resolvingId.value = alert.id;
  loadError.value = '';
  try {
    await resolveAlert(alert.id);
    await loadAlerts();
    showToast('告警已处理');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '告警处理失败';
  } finally {
    resolvingId.value = '';
  }
}

/* —— 导出告警 CSV —— */
function exportAlerts() {
  const rows = filtered.value.map((a) => ({
    级别: a.severity,
    类型: typeMeta[a.type]?.label ?? a.type,
    目标类型: targetTypeLabel[a.targetType] ?? a.targetType,
    目标: a.targetName,
    消息: a.message,
    等级: a.level,
    时间: a.time,
    状态: a.status,
  }));
  if (rows.length === 0) {
    showToast('没有可导出的告警');
    return;
  }
  const headers = Object.keys(rows[0]);
  const csv = [headers.join(','), ...rows.map((r) => headers.map((h) => `"${String((r as Record<string, string>)[h]).replace(/"/g, '""')}"`).join(','))].join('\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `alerts_${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
  showToast('告警记录已导出');
}

/* —— 新建告警（规则） —— */
const showCreate = ref(false);
const newAlert = ref({
  type: 'FAIL' as AlertItem['type'],
  severity: 'P2' as AlertItem['severity'],
  targetType: 'NODE',
  targetId: '',
  targetName: '',
  message: '',
});
const typeOptions: { label: string; value: AlertItem['type'] }[] = [
  { label: '卡住', value: 'STUCK' },
  { label: '失败', value: 'FAIL' },
  { label: '超时', value: 'TIMEOUT' },
  { label: '校验失败', value: 'CHECK_FAIL' },
];
const targetTypeOptions = ['NODE', 'INSTANCE', 'ROUTE', 'PROCESS'];

async function handleCreate() {
  if (!newAlert.value.targetId.trim() || !newAlert.value.message.trim()) return;
  try {
    await createAlert({
      type: newAlert.value.type,
      severity: newAlert.value.severity,
      targetType: newAlert.value.targetType,
      targetId: newAlert.value.targetId,
      message: newAlert.value.message,
    });
    showCreate.value = false;
    newAlert.value = { type: 'FAIL', severity: 'P2', targetType: 'NODE', targetId: '', targetName: '', message: '' };
    await loadAlerts();
    showToast('告警已创建');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '创建失败';
  }
}

/* —— 查看详情 —— */
const viewing = ref<AlertItem | null>(null);
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">告警中心</h1>
        <p class="page-subtitle">等级驱动的预警与工单处置</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-ghost" @click="exportAlerts">
          <Icon name="export" :size="15" />导出
        </button>
        <button class="btn btn-primary" @click="showCreate = true">
          <Icon name="plus" :size="15" />新建告警规则
        </button>
      </div>
    </header>

    <div v-if="toast" class="toast">
      <Icon name="check" :size="14" />{{ toast }}
    </div>

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
                    </template>
                    <button class="btn btn-ghost btn-sm" @click="viewing = alert">查看</button>
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

    <!-- 新建告警弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">新建告警规则</div>
          <button class="btn btn-ghost btn-sm" @click="showCreate = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="grid form-grid">
            <div class="field">
              <label class="label">告警类型</label>
              <select v-model="newAlert.type" class="select">
                <option v-for="t in typeOptions" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </div>
            <div class="field">
              <label class="label">级别</label>
              <select v-model="newAlert.severity" class="select">
                <option value="P0">P0</option>
                <option value="P1">P1</option>
                <option value="P2">P2</option>
                <option value="P3">P3</option>
              </select>
            </div>
            <div class="field">
              <label class="label">目标类型</label>
              <select v-model="newAlert.targetType" class="select">
                <option v-for="t in targetTypeOptions" :key="t" :value="t">{{ targetTypeLabel[t] ?? t }}</option>
              </select>
            </div>
            <div class="field">
              <label class="label">目标 ID *</label>
              <input v-model="newAlert.targetId" class="input" placeholder="节点/实例/路线 ID" />
            </div>
          </div>
          <div class="field">
            <label class="label">告警消息 *</label>
            <input v-model="newAlert.message" class="input" placeholder="说明告警原因…" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showCreate = false">取消</button>
          <button
            class="btn btn-primary"
            :disabled="!newAlert.targetId.trim() || !newAlert.message.trim()"
            @click="handleCreate"
          >创建</button>
        </div>
      </div>
    </div>

    <!-- 查看详情弹窗 -->
    <div v-if="viewing" class="modal-overlay" @click.self="viewing = null">
      <div class="modal modal--sm">
        <div class="modal-header">
          <div class="modal-title">告警详情</div>
          <button class="btn btn-ghost btn-sm" @click="viewing = null">关闭</button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <span class="detail-label">级别</span>
            <span class="tag" :class="severityMeta[viewing.severity]">{{ viewing.severity }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">类型</span>
            <span class="tag" :class="typeMeta[viewing.type].cls">{{ typeMeta[viewing.type].label }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">目标</span>
            <span>{{ viewing.targetName }}（{{ targetTypeLabel[viewing.targetType] ?? viewing.targetType }}）</span>
          </div>
          <div class="detail-row detail-row--block">
            <span class="detail-label">消息</span>
            <span class="detail-msg">{{ viewing.message }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">节点等级</span>
            <span class="lv" :class="`lv--${viewing.level}`">{{ viewing.level }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">时间</span>
            <span class="cell-mono">{{ viewing.time }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态</span>
            <Tag :status="viewing.status" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="viewing = null">关闭</button>
          <button
            v-if="viewing.status === 'OPEN'" class="btn btn-primary"
            :disabled="resolvingId === viewing.id"
            @click="handleResolve(viewing); viewing = null"
          >{{ resolvingId === viewing.id ? '处理中…' : '标记已处理' }}</button>
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
.toast {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 12px; margin-bottom: 12px;
  color: var(--success); background: var(--success-soft);
  border-radius: var(--radius-sm); font-size: 13px;
}

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; z-index: 50;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}
.modal {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius-lg); width: 520px; max-width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .2);
}
.modal--sm { width: 420px; }
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid var(--border);
}
.modal-title { font-weight: 600; }
.modal-body { padding: 20px; }
.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 20px; border-top: 1px solid var(--border);
}
.form-grid { grid-template-columns: repeat(2, 1fr); gap: 16px; }
.field { margin-bottom: 14px; }
.field:last-child { margin-bottom: 0; }
.label { display: block; font-size: 12px; color: var(--fg-muted); margin-bottom: 6px; }
.input, .select {
  width: 100%; padding: 8px 10px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: var(--surface);
  color: var(--fg); font-size: 13px;
}
.detail-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0; border-bottom: 1px solid var(--border);
}
.detail-row:last-child { border-bottom: none; }
.detail-row--block { flex-direction: column; align-items: flex-start; gap: 6px; }
.detail-label { font-size: 12px; color: var(--fg-faint); min-width: 70px; }
.detail-msg { color: var(--fg); line-height: 1.5; }
</style>
