<script setup lang="ts">
/** 工作台（看板首页）：全局运行态势总览，顺藤摸瓜定位问题 */
import { mockAlerts, mockDashboardStats, mockInstances } from '@/api/mockData';
import Icon from '@/components/Icon.vue';
import StatCard from '@/components/StatCard.vue';
import Tag from '@/components/Tag.vue';

const stats = mockDashboardStats;

/* —— 流程运行概览：纯 CSS 横向柱状图 —— */
const trend = stats.instanceTrend;
const trendMax = Math.max(0, ...trend.map((t) => t.value));
function barWidth(v: number) {
  return `${trendMax ? Math.round((v / trendMax) * 100) : 0}%`;
}

/* —— 告警速览：未处理（OPEN）前 3 条 —— */
const openAlerts = mockAlerts.filter((a) => a.status === 'OPEN').slice(0, 3);
function severityClass(sev: string) {
  if (sev === 'P0' || sev === 'P1') return 'tag--danger';
  if (sev === 'P2') return 'tag--warning';
  return 'tag--neutral';
}
function shortTime(t: string) {
  return t.slice(5, 16); // MM-DD HH:mm
}

/* —— 最近实例：前 4 条 —— */
const recentInstances = mockInstances.slice(0, 4);
function relativeTime(t: string): string {
  const date = new Date(t.replace(' ', 'T'));
  const diff = Date.now() - date.getTime();
  const min = Math.floor(diff / 60000);
  if (min < 1) return '刚刚';
  if (min < 60) return `${min} 分钟前`;
  const hour = Math.floor(min / 60);
  if (hour < 24) return `${hour} 小时前`;
  const day = Math.floor(hour / 24);
  if (day < 30) return `${day} 天前`;
  return t.slice(0, 10);
}

/* —— TOP 慢环节 —— */
const topSlowNodes = stats.topSlowNodes;
</script>

<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <h1 class="page-title">工作台</h1>
        <p class="page-subtitle">跨系统、跨部门、跨流程的业务运行总览，顺藤摸瓜定位问题</p>
      </div>
      <div class="page-actions">
        <router-link to="/graph" class="btn btn-primary">
          <Icon name="search" :size="15" />立即排查问题
        </router-link>
      </div>
    </div>

    <!-- 顶部统计卡 -->
    <div class="stat-grid">
      <StatCard label="总流程" :value="stats.processCount" icon="process" sub="已建模流程" />
      <StatCard label="运行中实例" :value="stats.runningInstances" icon="activity" color="accent" sub="跨 2 条流程" />
      <StatCard label="今日完成" :value="stats.doneToday" icon="check" color="success" sub="成功率 92%" />
      <StatCard label="未处理告警" :value="stats.openAlerts" icon="alert" color="danger" sub="含 1 条 P1" />
    </div>

    <!-- 双列面板 -->
    <div class="grid dash-grid mt-lg">
      <!-- 左列 -->
      <div class="dash-col">
        <div class="card">
          <div class="card-header">
            <h2 class="card-title">流程运行概览</h2>
            <router-link to="/processes" class="card-link">查看全部<Icon name="chevron" :size="13" /></router-link>
          </div>
          <div class="card-body">
            <div v-for="t in trend" :key="t.label" class="trend-row">
              <span class="trend-label mono">{{ t.label }}</span>
              <div class="trend-track"><div class="trend-bar" :style="{ width: barWidth(t.value) }" /></div>
              <span class="trend-value mono">{{ t.value }}</span>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h2 class="card-title">告警速览</h2>
          </div>
          <div class="card-body">
            <div v-if="openAlerts.length === 0" class="empty">
              <Icon name="check" :size="22" />
              <div class="empty-title">暂无未处理告警</div>
            </div>
            <div v-for="a in openAlerts" :key="a.id" class="alert-row">
              <div class="row-between">
                <div class="row">
                  <span class="tag tag--plain" :class="severityClass(a.severity)">{{ a.severity }}</span>
                  <span class="alert-target">{{ a.targetName }}</span>
                </div>
                <span class="alert-time mono faint">{{ shortTime(a.time) }}</span>
              </div>
              <div class="alert-msg">{{ a.message }}</div>
            </div>
            <router-link to="/alerts" class="btn btn-outline btn-block mt-md">进入告警中心</router-link>
          </div>
        </div>
      </div>

      <!-- 右列 -->
      <div class="dash-col">
        <div class="card">
          <div class="card-header">
            <h2 class="card-title">最近实例</h2>
          </div>
          <div class="card-body">
            <router-link v-for="inst in recentInstances" :key="inst.id" to="/graph" class="inst-row">
              <div class="row-between">
                <span class="inst-name">{{ inst.bizName }}</span>
                <Tag :status="inst.status" />
              </div>
              <div class="inst-meta muted">
                当前站点：{{ inst.currentNode ?? '—' }}
                <span class="sep">·</span>
                <span class="mono">{{ relativeTime(inst.startTime) }}</span>
              </div>
            </router-link>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h2 class="card-title">TOP 慢环节</h2>
          </div>
          <div class="card-body">
            <div v-for="n in topSlowNodes" :key="n.name" class="slow-row row-between">
              <span class="slow-name">{{ n.name }}</span>
              <span class="slow-duration mono">{{ n.duration }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dash-grid {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-lg);
}
.dash-col {
  display: flex; flex-direction: column; gap: var(--space-lg);
  min-width: 0;
}

/* 卡片头部链接 */
.card-link {
  display: inline-flex; align-items: center; gap: 2px;
  font-size: 12px; color: var(--fg-faint); transition: color .15s;
}
.card-link:hover { color: var(--accent); }

/* 流程运行概览 · 横向柱状图 */
.trend-row {
  display: grid; grid-template-columns: 44px 1fr 28px; align-items: center; gap: 10px;
  padding: 7px 0;
}
.trend-label { color: var(--fg-muted); font-size: 11px; text-align: right; }
.trend-track {
  height: 6px; border-radius: 999px; background: var(--surface-2); overflow: hidden;
}
.trend-bar {
  height: 100%; border-radius: 999px; background: var(--accent);
  opacity: .85; transition: width .4s ease, opacity .15s;
}
.trend-row:hover .trend-bar { opacity: 1; }
.trend-value { color: var(--fg); font-size: 11px; }

/* 告警速览 */
.alert-row { padding: 10px 0; border-bottom: 1px solid var(--border); }
.alert-row:first-child { padding-top: 0; }
.alert-row:last-child { border-bottom: none; padding-bottom: 0; }
.alert-target { font-weight: 600; font-size: 13px; }
.alert-msg {
  margin-top: 4px; font-size: 12.5px; color: var(--fg-muted);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.alert-time { font-size: 11px; }

/* 最近实例 */
.inst-row {
  display: block; padding: 10px 12px; margin: 0 -12px;
  border-radius: var(--radius-sm); color: inherit;
  border-bottom: 1px solid var(--border); transition: background .12s ease;
}
.inst-row:hover { background: var(--surface-3); }
.inst-row:first-child { padding-top: 0; }
.inst-row:last-child { border-bottom: none; padding-bottom: 0; }
.inst-name { font-weight: 600; font-size: 13px; color: var(--fg); }
.inst-meta { margin-top: 4px; font-size: 12px; }
.sep { margin: 0 6px; color: var(--border-strong); }

/* TOP 慢环节 */
.slow-row { padding: 10px 0; border-bottom: 1px solid var(--border); }
.slow-row:first-child { padding-top: 0; }
.slow-row:last-child { border-bottom: none; padding-bottom: 0; }
.slow-name { font-size: 13px; color: var(--fg); }
.slow-duration { color: var(--warning); font-weight: 700; font-size: 12px; }
</style>
