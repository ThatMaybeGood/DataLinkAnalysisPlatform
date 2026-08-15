<script setup lang="ts">
/** 检测点：全站点检测点清单（站点上的哨兵） */
import { computed, ref } from 'vue';
import { mockNodes, nodeTypeLabel } from '@/api/mockData';
import Icon from '@/components/Icon.vue';
import Tag from '@/components/Tag.vue';
import StatCard from '@/components/StatCard.vue';

/** 扁平化：每个检测点携带所属站点信息 */
const flat = computed(() =>
  mockNodes.flatMap((node) =>
    node.checkpoints.map((cp) => ({
      node,
      nodeName: node.name,
      nodeType: node.nodeType,
      level: node.level,
      ...cp,
    })),
  ),
);

const keyword = ref('');
const statusFilter = ref('');
const kindFilter = ref('');

const filtered = computed(() => {
  const kw = keyword.value.trim();
  return flat.value.filter((item) => {
    if (kw && !item.name.includes(kw) && !item.nodeName.includes(kw)) return false;
    if (statusFilter.value && item.status !== statusFilter.value) return false;
    if (kindFilter.value && item.kind !== kindFilter.value) return false;
    return true;
  });
});

/* —— 统计 —— */
const total = computed(() => flat.value.length);
const abnormal = computed(() => flat.value.filter((c) => c.status === 'FAIL' || c.status === 'TIMEOUT').length);
const passed = computed(() => flat.value.filter((c) => c.status === 'PASS').length);
const passRate = computed(() => (total.value ? Math.round((passed.value / total.value) * 100) : 0));
const siteCount = computed(() => mockNodes.filter((n) => n.checkpoints.length > 0).length);

/* —— 类型徽标 —— */
const kindMeta: Record<string, { label: string; cls: string }> = {
  DEFAULT: { label: '默认', cls: 'tag--accent' },
  CUSTOM: { label: '自定义', cls: 'tag--neutral' },
};
</script>

<template>
  <div class="page">
    <header class="page-header">
      <div>
        <h1 class="page-title">检测点</h1>
        <p class="page-subtitle">站点上的哨兵：默认检测点自动生成，支持自定义</p>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary">
          <Icon name="plus" :size="15" />新建检测点
        </button>
      </div>
    </header>

    <div class="stat-grid mb-md">
      <StatCard label="检测点总数" :value="total" :sub="`覆盖 ${siteCount} 个站点`" icon="target" />
      <StatCard label="异常 / 失败" :value="abnormal" sub="需关注的检测项" icon="alert" color="danger" />
      <StatCard label="通过率" :value="`${passRate}%`" :sub="`通过 ${passed} / ${total}`" icon="check" color="success" />
    </div>

    <div class="card mb-md">
      <div class="card-body">
        <div class="row-between">
          <div class="row">
            <div class="search">
              <span class="search-icon"><Icon name="search" :size="14" /></span>
              <input v-model="keyword" placeholder="搜索检测点 / 站点…" />
            </div>
            <select v-model="statusFilter" class="select filter-select">
              <option value="">全部状态</option>
              <option value="PASS">通过</option>
              <option value="FAIL">失败</option>
              <option value="TIMEOUT">超时</option>
            </select>
            <select v-model="kindFilter" class="select filter-select">
              <option value="">全部类型</option>
              <option value="DEFAULT">默认</option>
              <option value="CUSTOM">自定义</option>
            </select>
          </div>
          <div class="faint">共 {{ filtered.length }} 项</div>
        </div>
      </div>
    </div>

    <div class="card">
      <div class="card-body">
        <table class="data-table">
          <thead>
            <tr>
              <th>所属站点</th>
              <th>检测点名称</th>
              <th>类型</th>
              <th>检测类型</th>
              <th>等级</th>
              <th>状态</th>
              <th>最近检测时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in filtered" :key="item.id">
              <td>
                <div class="cell-strong">{{ item.nodeName }}</div>
                <div class="cell-muted">{{ nodeTypeLabel[item.nodeType] ?? item.nodeType }}</div>
              </td>
              <td>{{ item.name }}</td>
              <td><span class="tag" :class="kindMeta[item.kind].cls">{{ kindMeta[item.kind].label }}</span></td>
              <td><span class="cell-mono">{{ item.checkType }}</span></td>
              <td><span class="lv" :class="`lv--${item.level}`">{{ item.level }}</span></td>
              <td><Tag :status="item.status" /></td>
              <td class="cell-muted">{{ item.lastCheck }}</td>
              <td>
                <div class="row">
                  <button class="btn btn-ghost btn-sm">配置</button>
                  <button class="btn btn-outline btn-sm">立即检测</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="filtered.length === 0" class="empty">
          <Icon name="search" :size="28" />
          <div class="empty-title">未找到匹配的检测点</div>
          <div class="empty-desc">试试调整搜索关键词或筛选条件</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filter-select { width: 130px; flex-shrink: 0; }
</style>
