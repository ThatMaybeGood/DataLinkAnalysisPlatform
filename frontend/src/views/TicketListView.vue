<script setup lang="ts">
/** 工单列表：告警处置闭环，支持状态流转与处理人指派 */
import { computed, onMounted, ref } from 'vue';
import { createTicket, fetchTickets, updateTicket } from '@/api';
import type { Ticket } from '@/types';
import Icon from '@/components/Icon.vue';

const tickets = ref<Ticket[]>([]);
const loading = ref(true);
const loadError = ref('');
const statusFilter = ref('');
const updatingId = ref('');
const toast = ref('');

const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理', value: 'OPEN' },
  { label: '处理中', value: 'PROCESSING' },
  { label: '已解决', value: 'RESOLVED' },
];

const priorityOptions = ['P0', 'P1', 'P2', 'P3'];

const filtered = computed(() => {
  if (!statusFilter.value) return tickets.value;
  return tickets.value.filter((t) => t.status === statusFilter.value);
});

async function loadTickets() {
  loading.value = true;
  loadError.value = '';
  try {
    tickets.value = await fetchTickets();
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '工单加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadTickets);

function showToast(message: string) {
  toast.value = message;
  window.setTimeout(() => (toast.value = ''), 2000);
}

async function changeStatus(ticket: Ticket, status: Ticket['status']) {
  updatingId.value = ticket.id;
  try {
    await updateTicket(ticket.id, { status });
    await loadTickets();
    showToast('状态已更新');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '更新失败';
  } finally {
    updatingId.value = '';
  }
}

async function changeAssignee(ticket: Ticket, assignee: string) {
  updatingId.value = ticket.id;
  try {
    await updateTicket(ticket.id, { assignee });
    await loadTickets();
    showToast('处理人已更新');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '更新失败';
  } finally {
    updatingId.value = '';
  }
}

const newTicket = ref({ assignee: '', priority: 'P2', description: '' });
const creating = ref(false);

async function handleCreate() {
  if (!newTicket.value.description.trim()) return;
  creating.value = true;
  try {
    await createTicket({
      assignee: newTicket.value.assignee || undefined,
      priority: newTicket.value.priority,
      description: newTicket.value.description,
    });
    newTicket.value = { assignee: '', priority: 'P2', description: '' };
    await loadTickets();
    showToast('工单已创建');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '创建失败';
  } finally {
    creating.value = false;
  }
}

function statusLabel(status: string) {
  const map: Record<string, string> = { OPEN: '待处理', PROCESSING: '处理中', RESOLVED: '已解决' };
  return map[status] ?? status;
}

function statusClass(status: string) {
  const map: Record<string, string> = { OPEN: 'tag--danger', PROCESSING: 'tag--warning', RESOLVED: 'tag--success' };
  return map[status] ?? 'tag--neutral';
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">工单列表</div>
        <div class="page-subtitle">告警处置闭环：状态流转、指派处理人、追踪解决进度</div>
      </div>
    </div>

    <div class="card mb-md">
      <div class="card-header">
        <div class="card-title">新建工单</div>
      </div>
      <div class="card-body">
        <div class="grid form-grid">
          <div class="field">
            <label class="label">处理人</label>
            <input v-model="newTicket.assignee" class="input" placeholder="可留空" />
          </div>
          <div class="field">
            <label class="label">优先级</label>
            <select v-model="newTicket.priority" class="select">
              <option v-for="p in priorityOptions" :key="p" :value="p">{{ p }}</option>
            </select>
          </div>
          <div class="field field--wide">
            <label class="label">描述</label>
            <input v-model="newTicket.description" class="input" placeholder="描述告警或问题…" />
          </div>
        </div>
        <div class="row">
          <button class="btn btn-primary" :disabled="creating || !newTicket.description.trim()" @click="handleCreate">
            <Icon name="plus" :size="14" />{{ creating ? '创建中…' : '创建工单' }}
          </button>
          <span v-if="toast" class="toast"><Icon name="check" :size="13" />{{ toast }}</span>
        </div>
      </div>
    </div>

    <div class="card card--table">
      <div class="card-header">
        <div class="card-title">全部工单</div>
        <div class="row">
          <select v-model="statusFilter" class="select filter-select">
            <option v-for="s in statusOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
          </select>
          <span class="faint count-text">共 {{ filtered.length }} 条</span>
        </div>
      </div>

      <div v-if="loading" class="empty">
        <Icon name="activity" :size="32" />
        <div class="empty-title">工单加载中…</div>
      </div>
      <div v-else-if="loadError" class="empty">
        <Icon name="alert" :size="32" />
        <div class="empty-title">加载失败：{{ loadError }}</div>
        <button class="btn btn-outline btn-sm" @click="loadTickets">重试</button>
      </div>

      <template v-else>
        <table v-if="filtered.length" class="data-table">
          <thead>
            <tr>
              <th>工单号</th>
              <th>优先级</th>
              <th>状态</th>
              <th>处理人</th>
              <th>描述</th>
              <th>创建时间</th>
              <th>解决时间</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="ticket in filtered" :key="ticket.id">
              <td class="cell-mono">{{ ticket.id }}</td>
              <td><span class="tag" :class="`tag--${ticket.priority?.toLowerCase() === 'p0' || ticket.priority?.toLowerCase() === 'p1' ? 'danger' : ticket.priority?.toLowerCase() === 'p2' ? 'warning' : 'neutral'}`">{{ ticket.priority ?? '—' }}</span></td>
              <td><span class="tag" :class="statusClass(ticket.status)">{{ statusLabel(ticket.status) }}</span></td>
              <td>
                <input
                  :value="ticket.assignee ?? ''" class="input input-sm" placeholder="未指派"
                  @blur="(e) => changeAssignee(ticket, (e.target as HTMLInputElement).value)"
                  @keyup.enter="(e) => changeAssignee(ticket, (e.target as HTMLInputElement).value)"
                />
              </td>
              <td><span class="cell-truncate" :title="ticket.description">{{ ticket.description }}</span></td>
              <td class="cell-mono cell-muted">{{ ticket.createdAt }}</td>
              <td class="cell-mono cell-muted">{{ ticket.resolvedAt ?? '—' }}</td>
              <td>
                <div class="row ops">
                  <button
                    v-if="ticket.status !== 'PROCESSING'" class="btn btn-outline btn-sm"
                    :disabled="updatingId === ticket.id"
                    @click="changeStatus(ticket, 'PROCESSING')"
                  >处理中</button>
                  <button
                    v-if="ticket.status !== 'RESOLVED'" class="btn btn-primary btn-sm"
                    :disabled="updatingId === ticket.id"
                    @click="changeStatus(ticket, 'RESOLVED')"
                  >解决</button>
                  <button
                    v-if="ticket.status === 'RESOLVED'" class="btn btn-ghost btn-sm"
                    :disabled="updatingId === ticket.id"
                    @click="changeStatus(ticket, 'OPEN')"
                  >重开</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-else class="empty">
          <Icon name="check" :size="40" />
          <div class="empty-title">没有符合条件的工单</div>
          <div class="empty-desc">调整筛选条件，或新建一条工单</div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.card--table { overflow: hidden; }
.filter-select { width: 140px; }
.count-text { font-size: 12px; margin-left: 8px; }
.form-grid { grid-template-columns: repeat(3, 1fr); gap: var(--space-md); }
.field--wide { grid-column: span 2; }
.input-sm { width: 110px; padding: 4px 8px; font-size: 12px; }
.cell-truncate {
  display: inline-block; max-width: 260px; vertical-align: middle;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ops { white-space: nowrap; gap: 6px; }
.toast {
  display: inline-flex; align-items: center; gap: 5px;
  color: var(--success); font-size: 13px;
}
@media (max-width: 960px) {
  .form-grid { grid-template-columns: 1fr; }
  .field--wide { grid-column: span 1; }
}
</style>
