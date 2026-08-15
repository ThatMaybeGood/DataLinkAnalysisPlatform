<script setup lang="ts">
/** 流程列表：统一管理业务流程与数据链路，支持场景 / 等级 / 关键字筛选 */
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { mockProcesses } from '@/api/mockData';
import Icon from '@/components/Icon.vue';

const router = useRouter();

const keyword = ref('');
const sceneFilter = ref('');
const levelFilter = ref('');

/** 场景 → 展示文案与配色（复用全局 tag 色板） */
const sceneMeta: Record<string, { label: string; tone: string }> = {
  BUSINESS: { label: '业务', tone: 'tag--info' },
  DATA: { label: '数据', tone: 'tag--accent' },
};

const filtered = computed(() =>
  mockProcesses.filter((p) => {
    const kw = keyword.value.trim().toLowerCase();
    const hitKw = !kw || p.name.toLowerCase().includes(kw);
    const hitScene = !sceneFilter.value || p.scene === sceneFilter.value;
    const hitLevel = !levelFilter.value || p.level === levelFilter.value;
    return hitKw && hitScene && hitLevel;
  }),
);

function sceneMetaOf(scene: string) {
  return sceneMeta[scene] ?? { label: scene, tone: 'tag--neutral' };
}

function goGraph() {
  router.push('/graph');
}
</script>

<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <div class="page-title">流程列表</div>
        <div class="page-subtitle">统一管理业务流程与数据链路</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary">
          <Icon name="plus" :size="15" />新建流程
        </button>
        <button class="btn btn-ghost">
          <Icon name="export" :size="15" />导入
        </button>
      </div>
    </div>

    <!-- 筛选行 -->
    <div class="row filters mb-md">
      <div class="search">
        <span class="search-icon"><Icon name="search" :size="14" /></span>
        <input v-model="keyword" placeholder="搜索流程名称…" />
      </div>
      <select v-model="sceneFilter" class="select filter-select">
        <option value="">全部场景</option>
        <option value="DATA">数据流</option>
        <option value="BUSINESS">业务流程</option>
      </select>
      <select v-model="levelFilter" class="select filter-select">
        <option value="">全部等级</option>
        <option value="L1">L1</option>
        <option value="L2">L2</option>
        <option value="L3">L3</option>
      </select>
      <span class="faint filter-count">共 {{ filtered.length }} 个流程</span>
    </div>

    <!-- 流程表格 -->
    <div class="card table-card">
      <div class="card-header">
        <div class="card-title">全部流程</div>
        <span class="faint">按场景 / 等级 / 名称筛选</span>
      </div>

      <template v-if="filtered.length">
        <table class="data-table">
          <thead>
            <tr>
              <th>流程名</th>
              <th>场景</th>
              <th>等级</th>
              <th>节点数</th>
              <th>路线数</th>
              <th>实例状态</th>
              <th>最近更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in filtered" :key="p.id">
              <td class="cell-strong">{{ p.name }}</td>
              <td>
                <span class="tag" :class="sceneMetaOf(p.scene).tone">{{ sceneMetaOf(p.scene).label }}</span>
              </td>
              <td><span class="lv" :class="`lv--${p.level}`">{{ p.level }}</span></td>
              <td>{{ p.nodeCount }}</td>
              <td>{{ p.routeCount }}</td>
              <td>
                <div class="row inst-stats">
                  <span class="inst">
                    <b class="inst-num inst-num--run">{{ p.instanceStats.running }}</b>运行
                  </span>
                  <span class="inst">
                    <b class="inst-num inst-num--ok">{{ p.instanceStats.success }}</b>成功
                  </span>
                  <span class="inst">
                    <b class="inst-num inst-num--fail">{{ p.instanceStats.fail }}</b>失败
                  </span>
                </div>
              </td>
              <td class="cell-muted mono">{{ p.updatedAt }}</td>
              <td>
                <div class="row">
                  <button class="btn btn-outline btn-sm" @click="goGraph">查看</button>
                  <button class="btn btn-ghost btn-sm">配置</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </template>

      <div v-else class="empty">
        <Icon name="search" :size="30" />
        <div class="empty-title">未找到匹配的流程</div>
        <div class="empty-desc">请调整筛选条件或搜索关键词</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.filters { flex-wrap: wrap; }
.filter-select { width: 140px; flex-shrink: 0; }
.filter-count { font-size: 12px; margin-left: auto; white-space: nowrap; }
.table-card { overflow: hidden; }
.inst-stats { gap: 14px; }
.inst { font-size: 12px; color: var(--fg-faint); white-space: nowrap; }
.inst-num { font-size: 13px; font-weight: 700; margin-right: 3px; }
.inst-num--run { color: var(--accent); }
.inst-num--ok { color: var(--success); }
.inst-num--fail { color: var(--danger); }
</style>
