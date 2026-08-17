<script setup lang="ts">
import { useRoute } from 'vue-router';
import { useRouter } from 'vue-router';
import { computed, ref } from 'vue';
import { mockAlerts } from '@/api/mockData';

const route = useRoute();
const router = useRouter();
const openCount = computed(() => mockAlerts.filter((a) => a.status === 'OPEN').length);

const menus = [
  { path: '/', label: '工作台', icon: 'activity' },
  { path: '/graph', label: '关系网', icon: 'flow' },
  { path: '/graph-source', label: '图来源', icon: 'target' },
  { path: '/3d', label: '3D 视图', icon: 'box' },
  { path: '/processes', label: '流程列表', icon: 'process' },
  { path: '/data-sources', label: '数据接入', icon: 'database' },
  { path: '/checkpoints', label: '检测点', icon: 'target' },
  { path: '/alerts', label: '告警中心', icon: 'alert', badge: openCount },
  { path: '/bigscreen', label: '大屏', icon: 'fullscreen' },
  { path: '/versions', label: '配置版本', icon: 'history' },
  { path: '/settings', label: '系统管理', icon: 'shield' },
];

function isActive(path: string) {
  return route.path === path || (path !== '/' && route.path.startsWith(path));
}

function go(path: string) {
  router.push(path);
}
</script>

<template>
  <aside class="side-nav">
    <div class="brand" @click="go('/')">
      <img src="/favicon.svg" width="26" height="26" alt="logo" />
      <div class="brand-text">
        <div class="brand-name">数据关联分析平台</div>
        <div class="brand-sub">DataLink Platform</div>
      </div>
    </div>

    <nav class="nav">
      <div class="nav-label">导航</div>
      <router-link
        v-for="m in menus" :key="m.path" :to="m.path"
        class="nav-item" :class="{ 'nav-item--active': isActive(m.path) }"
      >
        <Icon :name="m.icon" :size="17" />
        <span class="nav-label-text">{{ m.label }}</span>
        <span v-if="m.badge" class="nav-badge">{{ m.badge }}</span>
      </router-link>
    </nav>

    <div class="nav-footer">
      <div class="user">
        <div class="avatar">管</div>
        <div class="user-info">
          <div class="user-name">管理员</div>
          <div class="user-role">ADMIN</div>
        </div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.side-nav {
  width: var(--sidebar-w);
  height: 100vh;
  position: fixed;
  top: 0; left: 0;
  background: var(--canvas-bg);
  border-right: 1px solid var(--canvas-border);
  display: flex;
  flex-direction: column;
  color: var(--canvas-fg);
  z-index: 20;
}

.brand {
  display: flex; align-items: center; gap: 10px;
  padding: 16px 18px; cursor: pointer;
  border-bottom: 1px solid var(--canvas-border);
}
.brand-name { font-size: 14px; font-weight: 700; color: #fff; letter-spacing: 0.01em; }
.brand-sub { font-size: 10px; color: var(--canvas-fg-dim); letter-spacing: 0.04em; }

.nav { flex: 1; padding: 14px 10px; overflow-y: auto; }
.nav-label { font-size: 10px; color: var(--canvas-fg-dim); padding: 4px 10px 8px; letter-spacing: 0.08em; }

.nav-item {
  display: flex; align-items: center; gap: 10px;
  height: 36px; padding: 0 12px; margin-bottom: 2px;
  border-radius: var(--radius-sm); color: var(--canvas-fg);
  font-size: 13px; cursor: pointer; transition: background .14s, color .14s;
  text-decoration: none;
}
.nav-item:hover { background: rgba(255,255,255,.06); color: #fff; }
.nav-item--active { background: var(--accent); color: #fff; font-weight: 500; box-shadow: 0 2px 10px rgba(37,99,235,.35); }
.nav-label-text { flex: 1; }
.nav-badge {
  min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px;
  background: var(--danger); color: #fff; font-size: 11px; font-weight: 600;
  display: flex; align-items: center; justify-content: center;
}

.nav-footer { padding: 12px; border-top: 1px solid var(--canvas-border); }
.user { display: flex; align-items: center; gap: 10px; }
.avatar {
  width: 32px; height: 32px; border-radius: 9px; background: var(--accent);
  color: #fff; font-weight: 600; font-size: 14px;
  display: flex; align-items: center; justify-content: center;
}
.user-name { font-size: 13px; color: #fff; font-weight: 500; }
.user-role { font-size: 10px; color: var(--canvas-fg-dim); }
</style>
