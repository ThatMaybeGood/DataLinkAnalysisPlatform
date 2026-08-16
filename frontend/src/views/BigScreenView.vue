<script setup lang="ts">
/** M4 · 大屏：深色科技投屏风实时监控（KPI + 趋势 / 告警 / 流程状态，30s 轮询） */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { fetchAlerts, fetchDashboardStats, fetchHealth, fetchProcesses } from '@/api';
import type { AlertItem, DashboardStats, ProcessDef } from '@/types';
import Icon from '@/components/Icon.vue';

const stats = ref<DashboardStats | null>(null);
const alerts = ref<AlertItem[]>([]);
const processes = ref<ProcessDef[]>([]);
const version = ref('—');
const loading = ref(true);
const loadError = ref('');

const now = ref(new Date());
let clockTimer: number | null = null;
let pollTimer: number | null = null;

async function loadScreen() {
  try {
    const [s, a, p] = await Promise.all([fetchDashboardStats(), fetchAlerts(), fetchProcesses()]);
    stats.value = s;
    alerts.value = a;
    processes.value = p;
    loadError.value = '';
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '大屏数据加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  loading.value = true;
  await loadScreen();
  try {
    const h = await fetchHealth();
    if (h.version) version.value = h.version;
  } catch {
    /* 健康探活失败不影响大屏主体 */
  }
  clockTimer = window.setInterval(() => { now.value = new Date(); }, 1000);
  pollTimer = window.setInterval(() => { void loadScreen(); }, 30000);
});

onBeforeUnmount(() => {
  if (clockTimer !== null) window.clearInterval(clockTimer);
  if (pollTimer !== null) window.clearInterval(pollTimer);
});

/* —— 顶部时间 —— */
const weekDays = ['日', '一', '二', '三', '四', '五', '六'];
const dateText = computed(() => {
  const d = now.value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 星期${weekDays[d.getDay()]}`;
});
const clockText = computed(() => {
  const d = now.value;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
});

/* —— KPI 行：流程数 / 运行中实例 / 今日完成 / 开放告警 / 卡住实例 —— */
const openAlertCount = computed(() => {
  const open = alerts.value.filter((a) => a.status === 'OPEN').length;
  return open > 0 ? open : (stats.value?.openAlerts ?? 0);
});
const kpis = computed(() => [
  { key: 'processCount', label: '流程数', value: stats.value?.processCount ?? 0, icon: 'process' },
  { key: 'running', label: '运行中实例', value: stats.value?.runningInstances ?? 0, icon: 'activity' },
  { key: 'doneToday', label: '今日完成', value: stats.value?.doneToday ?? 0, icon: 'check' },
  { key: 'openAlerts', label: '开放告警', value: openAlertCount.value, icon: 'alert' },
  { key: 'stuck', label: '卡住实例', value: stats.value?.stuckCount ?? 0, icon: 'target' },
]);

/* —— 左：实例趋势（纯 CSS 竖向柱状图） —— */
const trend = computed(() => stats.value?.instanceTrend ?? []);
const trendMax = computed(() => Math.max(1, ...trend.value.map((t) => t.value)));
function barHeight(v: number) {
  return `${Math.round((v / trendMax.value) * 100)}%`;
}

/* —— 左：TOP 慢节点 —— */
const slowNodes = computed(() => stats.value?.topSlowNodes ?? []);

/* —— 中：实时告警（OPEN 优先，最多 6 条） —— */
const alertList = computed(() => {
  const open = alerts.value.filter((a) => a.status === 'OPEN');
  const resolved = alerts.value.filter((a) => a.status !== 'OPEN');
  return [...open, ...resolved].slice(0, 6);
});
function severityClass(sev: string) {
  if (sev === 'P0' || sev === 'P1') return 'sev--p1';
  if (sev === 'P2') return 'sev--p2';
  return 'sev--p3';
}

/* —— 右：流程状态 —— */
function processStatus(p: ProcessDef): { label: string; cls: string } {
  if (p.instanceStats.fail > 0) return { label: '有失败', cls: 'st--danger' };
  if (p.instanceStats.running > 0) return { label: '运行中', cls: 'st--running' };
  return { label: '正常', cls: 'st--ok' };
}
</script>

<template>
  <div class="screen">
    <!-- 顶部大标题栏 -->
    <header class="screen-header">
      <div class="header-title">
        <span class="title-mark" />
        <h1 class="title-text">数据关联与业务流程监控分析平台</h1>
        <span class="title-sub">实时监控大屏</span>
      </div>
      <div class="header-time">
        <div class="live-pill">
          <span class="live-dot" />LIVE
        </div>
        <div class="clock-block">
          <span class="clock-date">{{ dateText }}</span>
          <span class="clock-time">{{ clockText }}</span>
        </div>
      </div>
    </header>

    <!-- 加载 / 错误态（已有数据时保留旧数据继续展示） -->
    <div v-if="loading" class="screen-status">大屏数据加载中…</div>
    <div v-else-if="loadError && !stats" class="screen-status screen-status--error">
      <span>加载失败：{{ loadError }}</span>
      <button class="retry-btn" @click="loadScreen">重试</button>
    </div>

    <template v-else>
      <!-- KPI 行 -->
      <section class="kpi-row">
        <div v-for="k in kpis" :key="k.key" class="kpi-card">
          <div class="kpi-icon"><Icon :name="k.icon" :size="18" /></div>
          <div class="kpi-meta">
            <div class="kpi-label">{{ k.label }}</div>
            <div class="kpi-value">{{ k.value }}</div>
          </div>
        </div>
      </section>

      <!-- 主体 3 列网格 -->
      <section class="main-grid">
        <!-- 左列：实例趋势 + TOP 慢节点 -->
        <div class="panel-col">
          <div class="panel">
            <div class="panel-title">
              <span class="panel-title-line" />实例趋势（近 8 小时）
            </div>
            <div class="trend-chart">
              <div v-for="t in trend" :key="t.label" class="trend-bar-wrap">
                <div class="trend-bar" :style="{ height: barHeight(t.value) }">
                  <span class="trend-bar-val">{{ t.value }}</span>
                </div>
                <span class="trend-bar-label">{{ t.label }}</span>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-title">
              <span class="panel-title-line" />TOP 慢节点
            </div>
            <div class="slow-list">
              <div v-for="(n, i) in slowNodes" :key="n.name" class="slow-row">
                <span class="slow-rank" :class="i === 0 ? 'rank--hot' : i === 1 ? 'rank--warn' : 'rank--info'">
                  {{ i + 1 }}
                </span>
                <span class="slow-name">{{ n.name }}</span>
                <span class="slow-duration">{{ n.duration }}</span>
              </div>
              <div v-if="!slowNodes.length" class="panel-empty">暂无慢节点数据</div>
            </div>
          </div>
        </div>

        <!-- 中列：实时告警 -->
        <div class="panel">
          <div class="panel-title">
            <span class="panel-title-line" />实时告警
            <span class="title-count">{{ openAlertCount }}</span>
          </div>
          <div class="alert-list">
            <div v-for="a in alertList" :key="a.id" class="alert-row" :class="{ 'alert-row--done': a.status !== 'OPEN' }">
              <span class="alert-dot" :class="a.status === 'OPEN' ? 'dot--live' : 'dot--muted'" />
              <span class="alert-badge" :class="severityClass(a.severity)">{{ a.severity }}</span>
              <div class="alert-body">
                <div class="alert-head">
                  <span class="alert-target">{{ a.targetName }}</span>
                  <span class="alert-time">{{ a.time.slice(5, 16) }}</span>
                </div>
                <div class="alert-msg">{{ a.message }}</div>
              </div>
            </div>
            <div v-if="!alertList.length" class="panel-empty">暂无告警</div>
          </div>
        </div>

        <!-- 右列：流程状态 + 系统信息 -->
        <div class="panel-col">
          <div class="panel">
            <div class="panel-title">
              <span class="panel-title-line" />流程状态
            </div>
            <div class="proc-list">
              <div v-for="p in processes" :key="p.id" class="proc-row">
                <div class="proc-head">
                  <span class="proc-name">{{ p.name }}</span>
                  <span class="proc-status" :class="processStatus(p).cls">{{ processStatus(p).label }}</span>
                </div>
                <div class="proc-stats">
                  运行 <b class="num-running">{{ p.instanceStats.running }}</b>
                  <span class="dot-sep">·</span>
                  成功 <b class="num-ok">{{ p.instanceStats.success }}</b>
                  <span class="dot-sep">·</span>
                  失败 <b class="num-fail">{{ p.instanceStats.fail }}</b>
                </div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-title">
              <span class="panel-title-line" />系统信息
            </div>
            <div class="sys-list">
              <div class="sys-row">
                <span class="sys-label">平台版本</span>
                <span class="sys-value">{{ version }}</span>
              </div>
              <div class="sys-row">
                <span class="sys-label">平均耗时</span>
                <span class="sys-value">{{ stats?.avgDuration ?? '—' }}</span>
              </div>
              <div class="sys-row">
                <span class="sys-label">检测点覆盖率</span>
                <span class="sys-value">{{ stats?.checkpointCoverage ?? 0 }}%</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
/* —— 深色科技投屏风 —— */
.screen {
  height: calc(100vh - var(--topbar-h));
  display: flex; flex-direction: column;
  padding: 14px 20px 18px; overflow: auto;
  color: #dbe4f5;
  background:
    radial-gradient(ellipse at 50% -10%, rgba(56, 189, 248, .10), transparent 55%),
    linear-gradient(rgba(56, 189, 248, .045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(56, 189, 248, .045) 1px, transparent 1px),
    #0b1020;
  background-size: 100% 100%, 38px 38px, 38px 38px, 100% 100%;
}

.screen-status {
  display: flex; align-items: center; justify-content: center; gap: 12px;
  flex: 1; color: #7583a4; font-size: 13.5px;
}
.screen-status--error { color: #fca5a5; }
.retry-btn {
  height: 28px; padding: 0 12px; border-radius: 6px;
  border: 1px solid rgba(56, 189, 248, .4); background: rgba(56, 189, 248, .15);
  color: #7dd3fc; font-size: 12px; cursor: pointer;
}

/* —— 顶栏 —— */
.screen-header {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding-bottom: 12px; margin-bottom: 12px; flex-shrink: 0;
  border-bottom: 1px solid rgba(56, 189, 248, .25);
  position: relative;
}
.screen-header::after {
  content: ''; position: absolute; left: 0; bottom: -1px; width: 180px; height: 1px;
  background: linear-gradient(90deg, rgba(56, 189, 248, .9), transparent);
}
.header-title { display: flex; align-items: baseline; gap: 12px; min-width: 0; }
.title-mark {
  align-self: center; width: 4px; height: 22px; flex-shrink: 0;
  background: linear-gradient(180deg, #22d3ee, #38bdf8); border-radius: 2px;
  box-shadow: 0 0 10px rgba(56, 189, 248, .6);
}
.title-text {
  font-size: 20px; font-weight: 700; letter-spacing: .02em; color: #f1f6ff;
  text-shadow: 0 0 18px rgba(56, 189, 248, .35);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.title-sub {
  font-size: 13px; letter-spacing: .22em; color: #38bdf8; flex-shrink: 0;
  border: 1px solid rgba(56, 189, 248, .45); border-radius: 999px; padding: 2px 12px;
  background: rgba(56, 189, 248, .1);
}
.header-time { display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.live-pill {
  display: inline-flex; align-items: center; gap: 6px;
  height: 26px; padding: 0 10px; border-radius: 999px;
  border: 1px solid rgba(52, 211, 153, .5); background: rgba(52, 211, 153, .1);
  color: #34d399; font-size: 12px; font-weight: 600; letter-spacing: .12em;
}
.live-dot {
  width: 7px; height: 7px; border-radius: 50%; background: #34d399;
  animation: blink 1.2s ease-in-out infinite;
}
.clock-block { display: flex; align-items: baseline; gap: 10px; }
.clock-date { font-size: 13px; color: #8aa0c0; }
.clock-time {
  font-family: var(--font-mono); font-size: 24px; font-weight: 700; color: #7dd3fc;
  letter-spacing: .04em; text-shadow: 0 0 14px rgba(56, 189, 248, .4);
}

/* —— KPI 行 —— */
.kpi-row {
  display: grid; grid-template-columns: repeat(5, 1fr); gap: 14px;
  margin-bottom: 14px; flex-shrink: 0;
}
.kpi-card {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 16px; border-radius: 8px;
  background: rgba(255, 255, 255, .05);
  border: 1px solid rgba(56, 189, 248, .3);
  position: relative; overflow: hidden;
}
.kpi-card::before {
  content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 3px;
  background: linear-gradient(180deg, #22d3ee, #38bdf8); opacity: .85;
}
.kpi-icon {
  width: 38px; height: 38px; flex-shrink: 0; border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(56, 189, 248, .14); color: #7dd3fc;
}
.kpi-label { font-size: 12px; color: #8aa0c0; letter-spacing: .04em; }
.kpi-value {
  font-family: var(--font-mono); font-size: 30px; font-weight: 700; line-height: 1.15;
  color: #eaf4ff; text-shadow: 0 0 16px rgba(56, 189, 248, .35);
}

/* —— 主体网格（3 列） —— */
.main-grid {
  flex: 1; display: grid; grid-template-columns: 1fr 1.15fr 1fr; gap: 14px;
  min-height: 0;
}
.panel-col { display: flex; flex-direction: column; gap: 14px; min-height: 0; }

/* —— 面板 —— */
.panel {
  display: flex; flex-direction: column; min-height: 0;
  background: rgba(255, 255, 255, .05);
  border: 1px solid rgba(56, 189, 248, .3); border-radius: 8px;
  padding: 12px 14px;
  position: relative;
}
.panel::before, .panel::after {
  content: ''; position: absolute; width: 14px; height: 14px; pointer-events: none;
  border-color: rgba(56, 189, 248, .75); border-style: solid;
}
.panel::before { left: -1px; top: -1px; border-width: 2px 0 0 2px; }
.panel::after { right: -1px; bottom: -1px; border-width: 0 2px 2px 0; }
.panel-title {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; font-weight: 600; letter-spacing: .08em; color: #9fd8ff;
  padding-bottom: 10px; margin-bottom: 10px; flex-shrink: 0;
  border-bottom: 1px solid rgba(56, 189, 248, .2);
}
.panel-title-line {
  width: 14px; height: 3px; border-radius: 2px;
  background: linear-gradient(90deg, #22d3ee, #38bdf8);
  box-shadow: 0 0 8px rgba(56, 189, 248, .6);
}
.title-count {
  margin-left: auto; font-family: var(--font-mono); font-size: 12px; color: #7dd3fc;
  background: rgba(56, 189, 248, .12); border-radius: 999px; padding: 0 8px;
}
.panel-empty { color: #5b6b85; font-size: 12.5px; padding: 18px 0; text-align: center; }

/* —— 左：实例趋势（竖向柱状图） —— */
.trend-chart {
  flex: 1; display: flex; align-items: stretch; gap: 10px;
  padding: 8px 4px 0; min-height: 120px;
}
.trend-bar-wrap {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 6px; min-width: 0;
}
.trend-bar {
  position: relative; width: 100%; max-width: 42px; border-radius: 4px 4px 2px 2px;
  background: linear-gradient(180deg, #22d3ee, #0e7490);
  box-shadow: 0 0 10px rgba(34, 211, 238, .35);
  min-height: 4px; transition: height .5s ease;
}
.trend-bar-val {
  position: absolute; top: -18px; left: 50%; transform: translateX(-50%);
  font-family: var(--font-mono); font-size: 11px; color: #9fd8ff;
}
.trend-bar-label { font-size: 11px; color: #8aa0c0; font-family: var(--font-mono); }

/* —— 左：TOP 慢节点 —— */
.slow-list { display: flex; flex-direction: column; gap: 2px; }
.slow-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 6px; border-radius: 6px;
  border-bottom: 1px solid rgba(56, 189, 248, .12);
}
.slow-rank {
  width: 22px; height: 22px; flex-shrink: 0; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-family: var(--font-mono); font-size: 12px; font-weight: 700;
}
.rank--hot { background: rgba(239, 68, 68, .18); color: #f87171; }
.rank--warn { background: rgba(251, 191, 36, .16); color: #fbbf24; }
.rank--info { background: rgba(56, 189, 248, .14); color: #7dd3fc; }
.slow-name { flex: 1; font-size: 13px; color: #dbe4f5; }
.slow-duration { font-family: var(--font-mono); font-size: 12px; color: #fbbf24; }

/* —— 中：实时告警 —— */
.alert-list { display: flex; flex-direction: column; gap: 2px; overflow-y: auto; }
.alert-row {
  display: flex; align-items: flex-start; gap: 10px;
  padding: 9px 8px; border-radius: 6px;
  border-bottom: 1px solid rgba(56, 189, 248, .12);
}
.alert-dot { width: 8px; height: 8px; border-radius: 50%; margin-top: 6px; flex-shrink: 0; }
.dot--live { background: #f87171; box-shadow: 0 0 8px rgba(248, 113, 113, .8); animation: blink 1.1s ease-in-out infinite; }
.dot--muted { background: #3f4d68; }
.alert-row--done { opacity: .55; }
.alert-badge {
  flex-shrink: 0; min-width: 30px; height: 20px; border-radius: 999px;
  display: inline-flex; align-items: center; justify-content: center;
  font-family: var(--font-mono); font-size: 11px; font-weight: 700; margin-top: 1px;
}
.sev--p1 { background: rgba(239, 68, 68, .18); color: #f87171; border: 1px solid rgba(239, 68, 68, .4); }
.sev--p2 { background: rgba(251, 191, 36, .16); color: #fbbf24; border: 1px solid rgba(251, 191, 36, .4); }
.sev--p3 { background: rgba(56, 189, 248, .14); color: #7dd3fc; border: 1px solid rgba(56, 189, 248, .35); }
.alert-body { flex: 1; min-width: 0; }
.alert-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.alert-target { font-size: 13px; font-weight: 600; color: #eaf4ff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.alert-time { font-family: var(--font-mono); font-size: 11px; color: #5b6b85; flex-shrink: 0; }
.alert-msg {
  margin-top: 2px; font-size: 12px; color: #9db0cd;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

/* —— 右：流程状态 —— */
.proc-list { display: flex; flex-direction: column; gap: 2px; }
.proc-row {
  padding: 8px 6px; border-radius: 6px;
  border-bottom: 1px solid rgba(56, 189, 248, .12);
}
.proc-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.proc-name { font-size: 13px; font-weight: 600; color: #eaf4ff; }
.proc-status {
  flex-shrink: 0; height: 20px; padding: 0 8px; border-radius: 999px;
  display: inline-flex; align-items: center; font-size: 11px; font-weight: 600;
}
.st--ok { background: rgba(52, 211, 153, .14); color: #34d399; border: 1px solid rgba(52, 211, 153, .35); }
.st--running { background: rgba(56, 189, 248, .14); color: #7dd3fc; border: 1px solid rgba(56, 189, 248, .35); }
.st--danger { background: rgba(239, 68, 68, .16); color: #f87171; border: 1px solid rgba(239, 68, 68, .4); }
.proc-stats { margin-top: 3px; font-size: 12px; color: #8aa0c0; }
.num-running { color: #7dd3fc; font-family: var(--font-mono); }
.num-ok { color: #34d399; font-family: var(--font-mono); }
.num-fail { color: #f87171; font-family: var(--font-mono); }
.dot-sep { margin: 0 6px; color: #3f4d68; }

/* —— 右：系统信息 —— */
.sys-list { display: flex; flex-direction: column; gap: 2px; }
.sys-row {
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 7px 6px; border-bottom: 1px solid rgba(56, 189, 248, .12);
}
.sys-label { font-size: 12.5px; color: #8aa0c0; }
.sys-value { font-family: var(--font-mono); font-size: 13px; color: #9fd8ff; }

/* —— 动效 —— */
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: .25; }
}

/* —— 响应式：窄屏退化为 1 列 —— */
@media (max-width: 1180px) {
  .kpi-row { grid-template-columns: repeat(3, 1fr); }
  .main-grid { grid-template-columns: 1fr; }
}
</style>
