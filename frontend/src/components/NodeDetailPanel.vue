<script setup lang="ts">
/** 站点详情面板：属性 + 检测点 + 上下游（支撑「顺藤摸瓜」入口） */
import { computed } from 'vue';
import type { GraphEdge, GraphNode } from '@/types';
import Tag from '@/components/Tag.vue';
import Icon from '@/components/Icon.vue';
import { nodeTypeLabel } from '@/api/mockData';

const props = defineProps<{
  node: GraphNode | null;
  nodes: GraphNode[];
  edges: GraphEdge[];
}>();
const emit = defineEmits<{ (e: 'close'): void; (e: 'focus', nodeId: string): void }>();

const upstream = computed<GraphNode[]>(() => {
  if (!props.node) return [];
  return props.edges
    .filter((e) => e.target === props.node!.id)
    .map((e) => props.nodes.find((n) => n.id === e.source))
    .filter((n): n is GraphNode => !!n);
});

const downstream = computed<GraphNode[]>(() => {
  if (!props.node) return [];
  return props.edges
    .filter((e) => e.source === props.node!.id)
    .map((e) => props.nodes.find((n) => n.id === e.target))
    .filter((n): n is GraphNode => !!n);
});
</script>

<template>
  <Transition name="panel">
    <div v-if="node" class="detail-panel">
      <div class="panel-header">
        <div>
          <div class="panel-name">{{ node.name }}</div>
          <div class="panel-code mono">{{ node.code }}</div>
        </div>
        <button class="panel-close" @click="emit('close')" aria-label="关闭">
          <Icon name="chevron" :size="16" />
        </button>
      </div>

      <div class="panel-meta">
        <Tag :status="node.nodeType" :label="nodeTypeLabel[node.nodeType] ?? node.nodeType" />
        <span class="lv" :class="`lv--${node.level}`">{{ node.level }}</span>
        <Tag :status="node.status" />
      </div>

      <div class="panel-section">
        <div class="kv"><span class="kv-label">负责人</span><span class="kv-value">{{ node.owner || '—' }}</span></div>
        <div class="kv"><span class="kv-label">描述</span><span class="kv-value">{{ node.description || '—' }}</span></div>
      </div>

      <div class="panel-section">
        <div class="section-title">检测点（{{ node.checkpoints.length }}）</div>
        <div v-if="node.checkpoints.length === 0" class="section-empty">未配置检测点</div>
        <div v-for="cp in node.checkpoints" :key="cp.id" class="cp-row">
          <div class="cp-main">
            <span class="cp-name">{{ cp.name }}</span>
            <span class="cp-type mono">{{ cp.checkType }}</span>
          </div>
          <div class="cp-side">
            <Tag :status="cp.status" />
            <span class="cp-time">{{ cp.lastCheck }}</span>
          </div>
          <div v-if="cp.detail" class="cp-detail">{{ cp.detail }}</div>
        </div>
      </div>

      <div class="panel-section">
        <div class="section-title">上游（数据从哪来 · {{ upstream.length }}）</div>
        <div v-if="upstream.length === 0" class="section-empty">无上游</div>
        <div v-for="n in upstream" :key="n.id" class="neighbor-chip" @click="emit('focus', n.id)">
          <span class="neighbor-dot" :style="{ background: nodeTypeLabel[n.nodeType] ? 'var(--accent)' : '' }" />
          {{ n.name }} <span class="faint mono">{{ n.nodeType }}</span>
        </div>
      </div>

      <div class="panel-section">
        <div class="section-title">下游（影响谁 · {{ downstream.length }}）</div>
        <div v-if="downstream.length === 0" class="section-empty">无下游</div>
        <div v-for="n in downstream" :key="n.id" class="neighbor-chip" @click="emit('focus', n.id)">
          {{ n.name }} <span class="faint mono">{{ n.nodeType }}</span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.detail-panel {
  position: absolute; top: 12px; right: 12px; bottom: 12px; width: 320px;
  background: rgba(13, 20, 36, 0.96);
  border: 1px solid var(--canvas-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-canvas);
  overflow-y: auto; padding: 16px 18px; color: var(--canvas-fg);
  z-index: 5;
}
.panel-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px; }
.panel-name { font-size: 16px; font-weight: 700; color: #fff; }
.panel-code { color: var(--canvas-fg-dim); font-size: 11px; margin-top: 2px; }
.panel-close {
  width: 26px; height: 26px; border-radius: 6px; border: none; background: rgba(255,255,255,.06);
  color: var(--canvas-fg); cursor: pointer; display: flex; align-items: center; justify-content: center;
  transform: rotate(180deg);
}
.panel-close:hover { background: rgba(255,255,255,.14); color: #fff; }
.panel-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.panel-section { border-top: 1px solid var(--canvas-border); padding: 12px 0; }
.section-title { font-size: 11px; font-weight: 600; color: var(--canvas-fg-dim); letter-spacing: .06em; margin-bottom: 8px; }
.section-empty { font-size: 12px; color: var(--canvas-fg-dim); }
.kv { display: flex; gap: 10px; font-size: 12.5px; margin-bottom: 4px; }
.kv-label { color: var(--canvas-fg-dim); flex-shrink: 0; width: 48px; }
.kv-value { color: var(--canvas-fg); }
.cp-row { padding: 7px 9px; border-radius: var(--radius-sm); background: rgba(255,255,255,.04); margin-bottom: 6px; }
.cp-main { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.cp-name { font-size: 13px; color: #fff; }
.cp-type { font-size: 10px; color: var(--canvas-fg-dim); }
.cp-side { display: flex; align-items: center; gap: 6px; margin-top: 5px; }
.cp-time { font-size: 10px; color: var(--canvas-fg-dim); }
.cp-detail { font-size: 11px; color: var(--warning); margin-top: 4px; }
.neighbor-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px; margin: 0 6px 6px 0; border-radius: 999px;
  background: rgba(255,255,255,.06); font-size: 12px; color: var(--canvas-fg); cursor: pointer;
  transition: background .14s;
}
.neighbor-chip:hover { background: var(--accent); color: #fff; }
.neighbor-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--canvas-fg-dim); }

.panel-enter-active, .panel-leave-active { transition: transform .22s ease, opacity .22s ease; }
.panel-enter-from, .panel-leave-to { transform: translateX(24px); opacity: 0; }
</style>
