<script setup lang="ts">
/** 配置版本页：全平台配置留痕，支持对象类型 / 状态筛选，可对比、可回滚、高等级变更需审批 */
import { computed, ref } from 'vue';
import { mockVersions } from '@/api/mockData';
import type { VersionRecord } from '@/types';
import Tag from '@/components/Tag.vue';
import Icon from '@/components/Icon.vue';

const typeOptions = ['全部', '流程', '路线', '节点', '检测点', '连接器'];
const statusOptions = ['全部', '已发布', '待审批', '已回滚'];

const typeFilter = ref('全部');
const statusFilter = ref('全部');

const statusValue: Record<string, VersionRecord['status']> = {
  已发布: 'PUBLISHED',
  待审批: 'PENDING_APPROVAL',
  已回滚: 'ROLLED_BACK',
};

const filteredVersions = computed(() =>
  mockVersions.filter(
    (v) =>
      (typeFilter.value === '全部' || v.targetType === typeFilter.value) &&
      (statusFilter.value === '全部' || v.status === statusValue[statusFilter.value]),
  ),
);
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">配置版本</div>
        <div class="page-subtitle">全平台配置留痕：可对比、可回滚、高等级变更需审批</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-outline btn-sm"><Icon name="export" :size="14" />导出记录</button>
      </div>
    </div>

    <!-- 顶部说明条 -->
    <div class="card mb-md">
      <div class="card-body">
        <div class="row muted">
          <Icon name="history" :size="15" />
          <span>版本留最近 20 版；L1/L2 配置变更默认需审批后发布</span>
        </div>
      </div>
    </div>

    <!-- 版本列表 -->
    <div class="card card--table">
      <div class="card-header version-header">
        <div class="row">
          <div class="card-title">版本列表</div>
          <span class="count-text muted faint">共 {{ filteredVersions.length }} 条</span>
        </div>
        <div class="row">
          <select v-model="typeFilter" class="select filter-select">
            <option v-for="t in typeOptions" :key="t" :value="t">对象类型：{{ t }}</option>
          </select>
          <select v-model="statusFilter" class="select filter-select">
            <option v-for="s in statusOptions" :key="s" :value="s">状态：{{ s }}</option>
          </select>
        </div>
      </div>

      <table v-if="filteredVersions.length" class="data-table">
        <thead>
          <tr>
            <th>对象类型</th>
            <th>对象名</th>
            <th>版本号</th>
            <th>操作人</th>
            <th>改动说明</th>
            <th>状态</th>
            <th>时间</th>
            <th class="text-right">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in filteredVersions" :key="item.id">
            <td><span class="tag tag--neutral">{{ item.targetType }}</span></td>
            <td class="cell-strong">{{ item.targetName }}</td>
            <td><span class="cell-mono">v{{ item.version }}</span></td>
            <td class="cell-muted">{{ item.operator }}</td>
            <td><span class="cell-truncate" :title="item.changeNote">{{ item.changeNote }}</span></td>
            <td><Tag :status="item.status" /></td>
            <td><span class="cell-mono">{{ item.time }}</span></td>
            <td>
              <div class="row ops">
                <button class="btn btn-outline btn-sm">查看</button>
                <button class="btn btn-ghost btn-sm">对比</button>
                <button v-if="item.status === 'PENDING_APPROVAL'" class="btn btn-primary btn-sm">审批</button>
                <button v-else-if="item.status === 'ROLLED_BACK'" class="btn btn-danger btn-sm">重新发布</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 空结果 -->
      <div v-else class="empty">
        <Icon name="history" :size="40" />
        <div class="empty-title">没有匹配的版本记录</div>
        <div class="empty-desc">试试调整对象类型或状态筛选条件</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card--table { overflow: hidden; }
.version-header { flex-wrap: wrap; gap: var(--space-sm); }
.count-text { font-size: 12px; margin-left: 2px; }
.filter-select { width: 170px; }
.cell-truncate {
  display: inline-block; max-width: 280px; vertical-align: middle;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.ops { white-space: nowrap; }
</style>
