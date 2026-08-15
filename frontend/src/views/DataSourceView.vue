<script setup lang="ts">
/** 数据接入：连接器可插拔，统一采集外部数据图来源 */
import { computed, ref } from 'vue';
import { mockConnectors } from '@/api/mockData';
import type { Connector } from '@/types';
import Icon from '@/components/Icon.vue';
import Tag from '@/components/Tag.vue';
import StatCard from '@/components/StatCard.vue';

/** 本地副本：开关 / 采集交互不污染 mock 源数据 */
const connectors = ref<Connector[]>(mockConnectors.map((c) => ({ ...c })));

const enabledCount = computed(() => connectors.value.filter((c) => c.enabled).length);
const okCount = computed(() => connectors.value.filter((c) => c.status === 'OK').length);
const disabledCount = computed(() => connectors.value.filter((c) => !c.enabled).length);

/** 类型 → 展示文案与配色（复用全局 tag 色板） */
const typeMeta: Record<string, { label: string; tone: string }> = {
  DB: { label: '数据库', tone: 'tag--accent' },
  EXCEL: { label: '文件导入', tone: 'tag--info' },
  CMDB: { label: 'CMDB', tone: 'tag--warning' },
};

/** 连接器状态 → 中文文案（Tag 组件自动映射配色） */
const statusText: Record<string, string> = {
  OK: '正常', ERROR: '异常', RUNNING: '采集中', IDLE: '空闲',
};

/** 采集任务状态 → 中文文案 */
const taskStatusText: Record<string, string> = {
  RUNNING: '采集中', SUCCESS: '成功', FAIL: '失败',
};

/** 各连接器最近一次任务的结果行数（mock） */
const rowCounts: Record<string, number> = { c1: 12840, c2: 9203, c3: 186, c4: 0 };

/** 用连接器派生「定时采集」任务 mock */
const taskRows = computed(() =>
  connectors.value.map((c) => {
    const status: 'RUNNING' | 'SUCCESS' | 'FAIL' =
      c.status === 'RUNNING' ? 'RUNNING' : c.status === 'ERROR' ? 'FAIL' : 'SUCCESS';
    return {
      id: c.id,
      name: `${c.name} · 定时采集`,
      status,
      lastRun: c.lastRun,
      rows: status === 'FAIL' ? 0 : (rowCounts[c.id] ?? 0),
    };
  }),
);

function typeOf(t: string) {
  return typeMeta[t] ?? { label: t, tone: 'tag--neutral' };
}

function collectNow(c: Connector) {
  if (c.status === 'RUNNING') return;
  c.status = 'RUNNING';
  window.setTimeout(() => {
    c.status = 'OK';
    c.lastRun = '刚刚';
  }, 1600);
}
</script>

<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <div class="page-title">数据接入</div>
        <div class="page-subtitle">连接器可插拔，统一采集外部数据图来源</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary">
          <Icon name="plus" :size="15" />新建连接器
        </button>
      </div>
    </div>

    <!-- 统计 -->
    <div class="stat-grid mb-md">
      <StatCard label="已启用连接器" :value="enabledCount" sub="当前接入的数据源" color="success" icon="database" />
      <StatCard label="采集成功" :value="okCount" sub="最近一次采集状态正常" color="success" icon="check" />
      <StatCard label="待处理 / 停用" :value="disabledCount" sub="未启用或待配置" color="warning" icon="box" />
    </div>

    <!-- 连接器卡片网格 -->
    <div class="grid conn-grid">
      <div v-for="c in connectors" :key="c.id" class="card card--hover conn-card">
        <div class="conn-head">
          <div class="row conn-title-row">
            <span class="conn-name">{{ c.name }}</span>
            <span class="tag" :class="typeOf(c.type).tone">{{ typeOf(c.type).label }}</span>
          </div>
          <label class="switch" :title="c.enabled ? '点击停用' : '点击启用'">
            <input type="checkbox" v-model="c.enabled" />
            <span class="switch-slider"></span>
          </label>
        </div>

        <div class="conn-info">
          <div class="conn-row">
            <span class="conn-key">连接地址</span>
            <span class="mono muted">{{ c.host || '—' }}</span>
          </div>
          <div class="conn-row">
            <span class="conn-key">状态</span>
            <Tag :status="c.status" :label="statusText[c.status]" />
          </div>
          <div class="conn-row">
            <span class="conn-key">上次采集</span>
            <span class="mono muted">{{ c.lastRun }}</span>
          </div>
        </div>

        <div class="row conn-actions">
          <button class="btn btn-outline btn-sm" @click="collectNow(c)">
            <Icon name="refresh" :size="13" />立即采集
          </button>
          <button class="btn btn-ghost btn-sm">
            <Icon name="settings" :size="13" />配置
          </button>
        </div>
      </div>
    </div>

    <!-- 采集任务 -->
    <div class="card table-card mt-lg">
      <div class="card-header">
        <div class="card-title">采集任务</div>
        <span class="faint">最近运行状态</span>
      </div>
      <table class="data-table">
        <thead>
          <tr>
            <th>任务名</th>
            <th>状态</th>
            <th>最近运行时间</th>
            <th>结果行数</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="t in taskRows" :key="t.id">
            <td class="cell-strong">{{ t.name }}</td>
            <td><Tag :status="t.status" :label="taskStatusText[t.status]" /></td>
            <td class="cell-muted mono">{{ t.lastRun }}</td>
            <td class="cell-mono">{{ t.rows.toLocaleString() }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.conn-grid {
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  align-items: stretch;
}
.conn-card {
  display: flex; flex-direction: column; gap: 14px;
  padding: 18px 20px;
}
.conn-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.conn-title-row { flex-wrap: wrap; }
.conn-name { font-size: 14px; font-weight: 600; }
.conn-info {
  display: flex; flex-direction: column; gap: 10px;
  margin: 0 -20px; padding: 12px 20px;
  border-top: 1px solid var(--border); border-bottom: 1px solid var(--border);
}
.conn-row { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.conn-key { font-size: 12px; color: var(--fg-faint); flex-shrink: 0; }
.conn-actions { margin-top: auto; }

/* 开关（checkbox 样式化） */
.switch { position: relative; display: inline-block; width: 36px; height: 20px; flex-shrink: 0; cursor: pointer; }
.switch input { position: absolute; opacity: 0; width: 0; height: 0; }
.switch-slider {
  position: absolute; inset: 0; background: var(--border-strong);
  border-radius: 999px; transition: background 0.18s ease;
}
.switch-slider::before {
  content: ''; position: absolute; width: 16px; height: 16px; left: 2px; top: 2px;
  background: #fff; border-radius: 50%;
  box-shadow: 0 1px 2px rgba(16, 24, 40, 0.25); transition: transform 0.18s ease;
}
.switch input:checked + .switch-slider { background: var(--accent); }
.switch input:checked + .switch-slider::before { transform: translateX(16px); }

.table-card { overflow: hidden; }
</style>
