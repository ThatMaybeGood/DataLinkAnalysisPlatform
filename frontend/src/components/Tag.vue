<script setup lang="ts">
/** 状态标签：根据状态值自动映射颜色（成功/警告/危险/信息/强调/中性） */
import { computed } from 'vue';

const props = defineProps<{
  status: string;
  label?: string;
  dot?: boolean; // 默认显示圆点
}>();

const map: Record<string, string> = {
  // 正向
  ACTIVE: 'success', SUCCESS: 'success', PASS: 'success', OK: 'success',
  NORMAL: 'success', RESOLVED: 'success', PUBLISHED: 'success',
  ENABLED: 'success', RUNNING: 'info', INFER: 'accent',
  // 警告
  WARNING: 'warning', TIMEOUT: 'warning', PENDING_APPROVAL: 'warning',
  // 危险
  FAIL: 'danger', STUCK: 'danger', ERROR: 'danger', DISABLED: 'neutral',
  ROLLED_BACK: 'danger', OPEN: 'warning', IDLE: 'neutral',
};

const tone = computed(() => map[props.status] ?? 'neutral');

const text: Record<string, string> = {
  ACTIVE: '正常', SUCCESS: '成功', PASS: '通过', OK: '正常', NORMAL: '正常',
  RESOLVED: '已解决', PUBLISHED: '已发布', ENABLED: '已启用', RUNNING: '运行中',
  INFER: '特征推断', WARNING: '异常', TIMEOUT: '超时', PENDING_APPROVAL: '待审批',
  FAIL: '失败', STUCK: '卡住', ERROR: '错误', DISABLED: '停用',
  ROLLED_BACK: '已回滚', OPEN: '待处理', IDLE: '空闲',
};
</script>

<template>
  <span class="tag" :class="[`tag--${tone}`, { 'tag--plain': dot === false }]">
    {{ label ?? text[status] ?? status }}
  </span>
</template>
