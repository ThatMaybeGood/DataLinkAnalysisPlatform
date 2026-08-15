<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { mockAlerts } from '@/api/mockData';

const route = useRoute();
const router = useRouter();
const pageTitle = computed(() => (route.meta.title as string) || '');
const openCount = computed(() => mockAlerts.filter((a) => a.status === 'OPEN').length);

function onSearch(keyword: string) {
  if (!keyword.trim()) return;
  // 全局反查：单号/别名/站点名 → 跳转到关系网并高亮（演示）
  router.push({ path: '/graph', query: { q: keyword.trim() } });
}
</script>

<template>
  <header class="top-bar">
    <div class="crumb">
      <span class="crumb-root">平台</span>
      <Icon name="chevron" :size="13" class="crumb-sep" />
      <span class="crumb-current">{{ pageTitle }}</span>
    </div>

    <div class="top-right">
      <div class="search">
        <span class="search-icon"><Icon name="search" :size="14" /></span>
        <input
          placeholder="搜索流程 / 站点 / 业务单号…"
          @keyup.enter="(e) => onSearch((e.target as HTMLInputElement).value)"
        />
      </div>

      <button class="icon-btn" title="告警中心" @click="router.push('/alerts')">
        <Icon name="alert" :size="18" />
        <span v-if="openCount" class="icon-btn-badge">{{ openCount }}</span>
      </button>

      <div class="top-user">
        <div class="avatar-sm">管</div>
        <span class="top-user-name">管理员</span>
      </div>
    </div>
  </header>
</template>

<style scoped>
.top-bar {
  height: var(--topbar-h);
  background: var(--surface);
  border-bottom: 1px solid var(--border);
  display: flex; align-items: center; justify-content: space-between;
  padding: 0 24px; position: sticky; top: 0; z-index: 10;
}
.crumb { display: flex; align-items: center; gap: 6px; font-size: 13px; }
.crumb-root { color: var(--fg-faint); }
.crumb-sep { color: var(--fg-faint); }
.crumb-current { font-weight: 600; }

.top-right { display: flex; align-items: center; gap: 16px; }

.icon-btn {
  position: relative; width: 34px; height: 34px; border-radius: var(--radius-sm);
  border: none; background: transparent; color: var(--fg-muted); cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: background .15s, color .15s;
}
.icon-btn:hover { background: var(--surface-2); color: var(--fg); }
.icon-btn-badge {
  position: absolute; top: 2px; right: 2px; min-width: 15px; height: 15px; padding: 0 4px;
  border-radius: 999px; background: var(--danger); color: #fff; font-size: 10px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}

.top-user { display: flex; align-items: center; gap: 8px; padding-left: 8px; border-left: 1px solid var(--border); }
.avatar-sm {
  width: 28px; height: 28px; border-radius: 8px; background: var(--accent);
  color: #fff; font-size: 12px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}
.top-user-name { font-size: 13px; font-weight: 500; }
</style>
