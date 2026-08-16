<script setup lang="ts">
/**
 * 地铁图视图：把 A→B 的多条路径画成地铁线路图
 * - 每条路径一条独立颜色车道，平行不交叉（车道行 y 分开）
 * - 公共节点（出现 ≥2 次）= 换乘站：大双圈 + 多色弧描边，一眼看出分叉/汇合点
 * - 标签策略：唯一节点 ≤ 15 → 全部显示名称；否则只标换乘站与起终点，其余 <title> 悬停查看
 * - 起点/终点：大圆环 + 「起点/终点」徽章 + 光晕
 * - 点击站点 → emit('focus-node', id)，由外部在画布聚焦
 */
import { computed } from 'vue';
import type { GraphPathResult } from '@/api';

const props = defineProps<{
  paths: GraphPathResult[];
}>();

const emit = defineEmits<{
  (e: 'focus-node', id: string): void;
}>();

/* —— 布局常量 —— */
const COL_W = 150;       // 换乘站列间距
const LANE_H = 92;       // 车道行间距（行距大 → 线不重叠不交叉）
const PAD_X = 60;        // SVG 左右留白
const PAD_Y_TOP = 52;    // 顶部留白（容纳起点徽章）
const PAD_Y_BOT = 60;    // 底部留白（容纳标签）
const LABEL_LIMIT = 15;  // 唯一节点数 ≤ 此值 → 全部节点显示名称

/** 车道调色板：高亮度、深色底可辨 */
const PALETTE = [
  '#60a5fa', '#22d3ee', '#a78bfa', '#fbbf24',
  '#34d399', '#fb7185', '#38bdf8', '#c084fc',
  '#f97316', '#4ade80', '#f472b6', '#2dd4bf',
];

interface Pt { id: string; name: string; x: number; y: number }
interface Lane { li: number; color: string; pathName: string; pts: Pt[]; ptsStr: string }
interface PStation {
  key: string;
  id: string;
  label: string;
  x: number;
  y: number;
  ringR: number;
  dotR: number;
  colors: string[];
  transfer: boolean;
  isStart: boolean;
  isEnd: boolean;
  badge: string | null;
  badgeColor: string;
  showLabel: boolean;
  tip: string;
}
interface LayoutData {
  lanes: Lane[];
  columns: string[];
  startId: string;
  endId: string;
  transferIds: Set<string>;
  uniqueCount: number;
}

/** 名称截断：长名省略号，完整名走 <title> 悬停查看 */
function truncate(name: string, max: number): string {
  return name.length > max ? `${name.slice(0, max - 1)}…` : name;
}

/** 换乘站多色环：每色等分一段圆环弧 */
function dashArray(ringR: number, n: number): string {
  const circ = 2 * Math.PI * ringR;
  const seg = circ / n;
  return `${Math.max(seg - 2, 2)} ${Math.max(circ - seg + 2, 2)}`;
}
function dashOffset(i: number, ringR: number, n: number): number {
  return -((2 * Math.PI * ringR) / n) * i;
}

/* —— 布局算法（地铁图）：换乘站列 × 车道行 —— */
const layout = computed<LayoutData | null>(() => {
  const paths = props.paths;
  if (!paths.length || !paths[0].nodeIds.length) return null;

  // 1) 统计每个节点出现在几条路径（按路径去重，循环路径不重复计）
  const occurrence = new Map<string, number>();
  paths.forEach((p) => {
    new Set(p.nodeIds).forEach((id) => occurrence.set(id, (occurrence.get(id) ?? 0) + 1));
  });

  const startId = paths[0].nodeIds[0];
  const endId = paths[0].nodeIds[paths[0].nodeIds.length - 1];
  const uniqueCount = occurrence.size;

  // 2) 换乘站 = 出现 ≥2 次的节点（起终点天然命中）
  const transferIds = new Set<string>();
  occurrence.forEach((cnt, id) => { if (cnt >= 2) transferIds.add(id); });

  // 3) 中间换乘站按「平均位置」排序 → 得到沿 A→B 的先后次序
  const mid = [...transferIds].filter((id) => id !== startId && id !== endId);
  const avgPos = (id: string): number => {
    let sum = 0; let n = 0;
    paths.forEach((p) => { const i = p.nodeIds.indexOf(id); if (i >= 0) { sum += i; n += 1; } });
    return n ? sum / n : Number.MAX_SAFE_INTEGER;
  };
  mid.sort((a, b) => avgPos(a) - avgPos(b));

  // 4) 列序 = 起点 + 中间换乘站 + 终点（去重，各占一列）
  const columns: string[] = [];
  const seen = new Set<string>();
  [startId, ...mid, endId].forEach((id) => {
    if (id !== undefined && !seen.has(id)) { seen.add(id); columns.push(id); }
  });
  const colIndex = new Map<string, number>(columns.map((id, i) => [id, i] as [string, number]));
  const colX = (ci: number) => PAD_X + ci * COL_W;
  const laneY = (li: number) => PAD_Y_TOP + li * LANE_H;

  // 5) 每条路径一条车道：换乘站对齐列 x；两列之间的非公共节点沿 x 均匀排布
  const lanes: Lane[] = paths.map((p, li) => {
    const color = PALETTE[li % PALETTE.length];
    const nodes = p.nodeIds.map((id, i) => ({ id, name: p.nodeNames[i] ?? id }));
    const pts: Pt[] = [];
    let pending: { id: string; name: string }[] = [];
    let lastX: number | null = null;

    const flush = (anchorX: number) => {
      if (!pending.length) return;
      const prevX = lastX;
      if (prevX !== null) {
        pending.forEach((q, k) => {
          const t = (k + 1) / (pending.length + 1);
          pts.push({ ...q, x: prevX + (anchorX - prevX) * t, y: laneY(li) });
        });
      } else {
        pending.forEach((q) => pts.push({ ...q, x: anchorX, y: laneY(li) }));
      }
      pending = [];
    };

    nodes.forEach((n) => {
      const ci = colIndex.get(n.id);
      if (ci !== undefined) {
        const x = colX(ci);
        flush(x);
        pts.push({ ...n, x, y: laneY(li) });
        lastX = x;
      } else {
        pending.push(n);
      }
    });
    if (pending.length) { // 兜底：路径尾部未对齐到终点列（正常不会发生）
      const base = lastX ?? PAD_X;
      pending.forEach((q, k) => pts.push({ ...q, x: base + (k + 1) * 16, y: laneY(li) }));
    }

    return {
      li,
      color,
      pathName: p.nodeNames.join(' → '),
      pts,
      ptsStr: pts.map((q) => `${q.x},${q.y}`).join(' '),
    };
  });

  return { lanes, columns, startId, endId, transferIds, uniqueCount };
});

const hasData = computed(() => layout.value !== null);
const lanes = computed<Lane[]>(() => layout.value?.lanes ?? []);
const stationList = computed<PStation[]>(() => {
  const L = layout.value;
  if (!L) return [];
  const stations: PStation[] = [];
  const labeled = new Set<string>(); // 同名站点标签只渲染一次（首个经过车道）
  const badged = new Set<string>();  // 起/终徽章只渲染一次

  L.lanes.forEach((lane) => {
    lane.pts.forEach((pt) => {
      const isStart = pt.id === L.startId;
      const isEnd = pt.id === L.endId;
      const transfer = L.transferIds.has(pt.id);
      // 换乘站描边 = 所有经过车道的颜色；单路线节点 = 本车道颜色
      const colors = transfer
        ? L.lanes.filter((l) => l.pts.some((q) => q.id === pt.id)).map((l) => l.color)
        : [lane.color];
      const ringR = isStart || isEnd ? 14 : transfer ? 12 : 7;
      const dotR = isStart || isEnd ? 6 : transfer ? 5 : 3;
      const wantLabel = L.uniqueCount <= LABEL_LIMIT ? true : transfer || isStart || isEnd;
      const firstLabel = wantLabel && !labeled.has(pt.id);
      if (firstLabel) labeled.add(pt.id);
      const badge = isStart ? '起点' : isEnd ? '终点' : null;
      const firstBadge = badge !== null && !badged.has(pt.id);
      if (firstBadge) badged.add(pt.id);

      const tipParts = [pt.name];
      if (isStart) tipParts.unshift('[起点]');
      if (isEnd) tipParts.unshift('[终点]');
      if (transfer) tipParts.push(`换乘站 · ${colors.length} 条路线经过`);

      stations.push({
        key: `${lane.li}-${pt.id}`,
        id: pt.id,
        label: truncate(pt.name, 14),
        x: pt.x,
        y: pt.y,
        ringR,
        dotR,
        colors,
        transfer,
        isStart,
        isEnd,
        badge: firstBadge ? badge : null,
        badgeColor: isStart ? '#059669' : '#e11d48',
        showLabel: firstLabel,
        tip: tipParts.join(' '),
      });
    });
  });
  return stations;
});

const svgW = computed(() => {
  const L = layout.value;
  return L ? PAD_X * 2 + Math.max(L.columns.length - 1, 1) * COL_W : 240;
});
const svgH = computed(() => {
  const L = layout.value;
  return L ? PAD_Y_TOP + Math.max(L.lanes.length - 1, 0) * LANE_H + PAD_Y_BOT : 120;
});

function onStationClick(id: string) {
  emit('focus-node', id);
}
</script>

<template>
  <div class="pm">
    <template v-if="hasData">
      <div class="pm-scroll">
        <svg :width="svgW" :height="svgH" class="pm-svg" role="img" aria-label="路径地铁图">
          <!-- 车道线：每条路径一条水平线，粗线 + 圆角 -->
          <g v-for="lane in lanes" :key="'line-' + lane.li">
            <polyline
              :points="lane.ptsStr" :stroke="lane.color"
              fill="none" stroke-width="4.5" stroke-linecap="round" stroke-linejoin="round"
              class="pm-line"
            />
          </g>

          <!-- 站点：换乘站大双圈（多色弧）/ 普通站小圆点 -->
          <g
            v-for="s in stationList" :key="s.key" class="pm-station"
            :transform="`translate(${s.x},${s.y})`" @click="onStationClick(s.id)"
          >
            <title>{{ s.tip }}</title>
            <rect class="pm-hit" :x="-20" :y="-20" :width="40" :height="40" rx="10" />

            <!-- 起/终点光晕 -->
            <circle
              v-if="s.isStart || s.isEnd"
              r="19" :stroke="s.colors[0]" stroke-opacity=".22" stroke-width="2" fill="none"
            />
            <!-- 换乘站底环（深色衬底，盖住下方线路） -->
            <circle v-if="s.transfer" :r="s.ringR" fill="var(--canvas-bg)" stroke="#8b96b0" stroke-width="2" />
            <!-- 多色弧：每种经过颜色占一段环 -->
            <template v-if="s.transfer">
              <circle
                v-for="(c, ci) in s.colors" :key="'arc' + ci"
                :r="s.ringR" :stroke="c" fill="none" stroke-width="4" stroke-linecap="round"
                :stroke-dasharray="dashArray(s.ringR, s.colors.length)"
                :stroke-dashoffset="dashOffset(ci, s.ringR, s.colors.length)"
              />
            </template>
            <!-- 内圈 + 中心点 -->
            <circle v-if="s.transfer" :r="s.dotR" fill="#0d1424" stroke="#e2e8f0" stroke-width="2" />
            <circle v-if="s.transfer" r="2.2" fill="#e2e8f0" />
            <!-- 普通节点：本路线颜色小圆点 -->
            <circle v-else :r="4.5" :fill="s.colors[0]" stroke="#0d1424" stroke-width="1.5" />

            <!-- 站点名称（策略见顶部注释） -->
            <text
              v-if="s.showLabel" :y="s.ringR + 16" text-anchor="middle"
              class="pm-label" :class="{ 'pm-label--em': s.isStart || s.isEnd }"
            >{{ s.label }}</text>

            <!-- 起点 / 终点徽章 -->
            <g
              v-if="s.badge" class="pm-badge"
              :transform="`translate(0,${-s.ringR - 22})`"
            >
              <rect :x="-16" :y="-11" :width="32" :height="22" rx="11" :fill="s.badgeColor" stroke="rgba(255,255,255,.28)" />
              <text y="4" text-anchor="middle" font-size="10" fill="#fff" font-weight="700">{{ s.badge }}</text>
            </g>
          </g>
        </svg>
      </div>

      <!-- 图例：每条线颜色 → 序号 + 路径名 -->
      <div class="pm-legend">
        <span class="pm-legend-title">地铁图 · {{ lanes.length }} 条路线 · {{ layout?.uniqueCount }} 个节点</span>
        <span
          v-for="(lane, i) in lanes" :key="'lg-' + lane.li"
          class="pm-legend-item" :title="lane.pathName"
        >
          <span class="pm-legend-line" :style="{ background: lane.color }" />
          <span class="pm-legend-name">路线 {{ i + 1 }} · {{ lane.pathName }}</span>
        </span>
      </div>
    </template>
    <div v-else class="pm-empty">暂无路径数据，请先执行路径查询</div>
  </div>
</template>

<style scoped>
.pm { width: 100%; }
.pm-scroll { overflow-x: auto; }
.pm-svg { display: block; }
.pm-line { opacity: .95; }
.pm-station { cursor: pointer; }
.pm-hit { fill: transparent; }
.pm-station:hover .pm-label { fill: #fff; }

.pm-label {
  font-size: 11px; fill: var(--canvas-fg);
  font-family: var(--font-sans); user-select: none;
}
.pm-label--em { fill: #fff; font-weight: 600; }

.pm-legend {
  display: flex; flex-wrap: wrap; align-items: center; gap: 6px 18px;
  margin-top: 10px; padding: 9px 12px;
  background: rgba(13, 20, 36, .6); border: 1px solid var(--canvas-border);
  border-radius: var(--radius); color: var(--canvas-fg);
}
.pm-legend-title { font-size: 11px; color: var(--canvas-fg-dim); letter-spacing: .04em; flex-basis: 100%; }
.pm-legend-item { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; max-width: 340px; }
.pm-legend-line { width: 18px; height: 4px; border-radius: 2px; flex-shrink: 0; }
.pm-legend-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.pm-empty { padding: 18px; font-size: 13px; color: var(--fg-muted); text-align: center; }
</style>
