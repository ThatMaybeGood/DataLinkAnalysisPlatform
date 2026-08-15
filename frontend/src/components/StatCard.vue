<script setup lang="ts">
/** 统计卡片 */
withDefaults(defineProps<{
  label: string;
  value: string | number;
  sub?: string;
  color?: 'accent' | 'success' | 'warning' | 'danger' | 'info';
  icon?: string;
}>(), { color: 'accent' });

const colorMap: Record<string, string> = {
  accent: 'var(--accent)',
  success: 'var(--success)',
  warning: 'var(--warning)',
  danger: 'var(--danger)',
  info: 'var(--info)',
};
</script>

<template>
  <div class="card stat-card reveal">
    <div class="stat-label">{{ label }}</div>
    <div class="stat-value" :style="{ color: colorMap[color] }">{{ value }}</div>
    <div class="stat-sub">{{ sub }}</div>
    <div v-if="icon" class="stat-icon" :style="{ background: colorMap[color] }">
      <Icon :name="icon" :size="19" />
    </div>
  </div>
</template>
