import { createRouter, createWebHistory } from 'vue-router';
import { getToken } from '@/api';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true, title: '登录' },
    },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        { path: '', name: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '工作台' } },
        { path: 'graph', name: 'graph', component: () => import('@/views/GraphView.vue'), meta: { title: '关系网' } },
        { path: 'processes', name: 'processes', component: () => import('@/views/ProcessListView.vue'), meta: { title: '流程列表' } },
        { path: 'data-sources', name: 'data-sources', component: () => import('@/views/DataSourceView.vue'), meta: { title: '数据接入' } },
        { path: 'checkpoints', name: 'checkpoints', component: () => import('@/views/CheckpointView.vue'), meta: { title: '检测点' } },
        { path: 'alerts', name: 'alerts', component: () => import('@/views/AlertView.vue'), meta: { title: '告警中心' } },
        { path: 'versions', name: 'versions', component: () => import('@/views/VersionView.vue'), meta: { title: '配置版本' } },
        { path: 'settings', name: 'settings', component: () => import('@/views/SettingsView.vue'), meta: { title: '系统管理' } },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

// 路由守卫：未登录访问受保护页 → 登录页；已登录访问登录页 → 工作台
router.beforeEach((to) => {
  if (!to.meta.public && !getToken()) {
    return { name: 'login' };
  }
  if (to.name === 'login' && getToken()) {
    return { path: '/' };
  }
  return true;
});

export default router;
