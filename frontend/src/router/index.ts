import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
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

export default router;
