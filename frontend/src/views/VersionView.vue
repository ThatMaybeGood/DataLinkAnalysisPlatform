<script setup lang="ts">
/** 配置版本页：真实 /api/versions 分页展示；对象类型走服务端筛选，状态走客户端筛选 */
import { computed, onMounted, ref } from 'vue';
import { fetchVersions, rollbackVersion } from '@/api';
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
const toast = ref('');
const actingId = ref('');

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

function showToast(message: string) {
  toast.value = message;
  window.setTimeout(() => (toast.value = ''), 2000);
}

/** 导出记录 CSV */
function exportRecords() {
  const rows = filtered.value.map((v) => ({
    对象类型: targetTypeLabel[v.targetType] ?? v.targetType,
    对象名: v.targetName,
    版本号: `v${v.version}`,
    操作人: v.operator,
    改动说明: v.changeNote,
    状态: v.status,
    时间: v.time,
  }));
  if (rows.length === 0) {
    showToast('没有可导出的记录');
    return;
  }
  const headers = Object.keys(rows[0]);
  const csv = [headers.join(','), ...rows.map((r) => headers.map((h) => `"${String((r as Record<string, string>)[h]).replace(/"/g, '""')}"`).join(','))].join('\n');
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `versions_${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
  showToast('版本记录已导出');
}

/** 查看详情 */
const viewing = ref<VersionRecord | null>(null);

/** 对比（占位） */
function compare(item: VersionRecord) {
  showToast(`对比功能待后端提供版本快照：${item.targetName} v${item.version}`);
}

/** 审批（占位） */
async function approve(item: VersionRecord) {
  actingId.value = item.id;
  try {
    // 后端暂无审批端点，先占位提示
    await new Promise((r) => setTimeout(r, 300));
    showToast(`「${item.targetName}」已审批（演示）`);
  } finally {
    actingId.value = '';
  }
}

/** 重新发布 / 回滚 */
async function republish(item: VersionRecord) {
  actingId.value = item.id;
  try {
    await rollbackVersion(item.id);
    await loadVersions();
    showToast(`「${item.targetName}」已回滚并重新发布`);
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '回滚失败';
  } finally {
    actingId.value = '';
  }
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">配置版本</div>
        <div class="page-subtitle">全平台配置留痕：可对比、可回滚、高等级变更需审批</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-outline btn-sm" @click="exportRecords">
          <Icon name="export" :size="14" />导出记录
        </button>
      </div>
    </div>

    <div v-if="toast" class="toast">
      <Icon name="check" :size="14" />{{ toast }}
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
                  <button class="btn btn-outline btn-sm" @click="viewing = item">查看</button>
                  <button class="btn btn-ghost btn-sm" @click="compare(item)">对比</button>
                  <button
                    v-if="item.status === 'PENDING_APPROVAL'" class="btn btn-primary btn-sm"
                    :disabled="actingId === item.id"
                    @click="approve(item)"
                  >{{ actingId === item.id ? '处理中…' : '审批' }}</button>
                  <button
                    v-else-if="item.status === 'ROLLED_BACK'" class="btn btn-danger btn-sm"
                    :disabled="actingId === item.id"
                    @click="republish(item)"
                  >{{ actingId === item.id ? '处理中…' : '重新发布' }}
                  </button>
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

    <!-- 查看详情弹窗 -->
    <div v-if="viewing" class="modal-overlay" @click.self="viewing = null">
      <div class="modal modal--sm">
        <div class="modal-header">
          <div class="modal-title">版本详情</div>
          <button class="btn btn-ghost btn-sm" @click="viewing = null">关闭</button>
        </div>
        <div class="modal-body">
          <div class="detail-row">
            <span class="detail-label">对象类型</span>
            <span class="tag tag--neutral">{{ targetTypeLabel[viewing.targetType] ?? viewing.targetType }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">对象名</span>
            <span class="cell-strong">{{ viewing.targetName }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">版本号</span>
            <span class="cell-mono">v{{ viewing.version }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">操作人</span>
            <span>{{ viewing.operator }}</span>
          </div>
          <div class="detail-row detail-row--block">
            <span class="detail-label">改动说明</span>
            <span>{{ viewing.changeNote }}</span>
          </div>
          <div class="detail-row">
            <span class="detail-label">状态</span>
            <Tag :status="viewing.status" />
          </div>
          <div class="detail-row">
            <span class="detail-label">时间</span>
            <span class="cell-mono">{{ viewing.time }}</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="viewing = null">关闭</button>
          <button
            v-if="viewing.status === 'PENDING_APPROVAL'" class="btn btn-primary"
            :disabled="actingId === viewing.id"
            @click="approve(viewing); viewing = null"
          >{{ actingId === viewing.id ? '处理中…' : '审批' }}</button>
          <button
            v-else-if="viewing.status === 'ROLLED_BACK'" class="btn btn-danger"
            :disabled="actingId === viewing.id"
            @click="republish(viewing); viewing = null"
          >{{ actingId === viewing.id ? '处理中…' : '重新发布' }}
          </button>
        </div>
      </div>
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
.ops { white-space: nowrap; gap: 6px; }
.pagination {
  display: flex; align-items: center; justify-content: flex-end; gap: 8px;
  padding: 12px 16px; border-top: 1px solid var(--border);
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
  border-radius: var(--radius-lg); width: 460px; max-width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .2);
}
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
.detail-row {
  display: flex; align-items: center; gap: 12px;
  padding: 8px 0; border-bottom: 1px solid var(--border);
}
.detail-row:last-child { border-bottom: none; }
.detail-row--block { flex-direction: column; align-items: flex-start; gap: 6px; }
.detail-label { font-size: 12px; color: var(--fg-faint); min-width: 70px; }
.btn-danger {
  background: var(--danger-soft); color: var(--danger);
  border: 1px solid rgba(220, 38, 38, 0.35);
}
.btn-danger:hover { background: var(--danger); color: #fff; }
</style>
