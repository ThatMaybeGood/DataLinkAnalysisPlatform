<script setup lang="ts">
/** 流程列表：统一管理业务流程与数据链路，支持场景 / 等级 / 关键字筛选 */
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { createProcess, deleteProcess, fetchProcesses, updateProcess } from '@/api';
import type { ProcessDef } from '@/types';
import Icon from '@/components/Icon.vue';

const router = useRouter();

const keyword = ref('');
const sceneFilter = ref('');
const levelFilter = ref('');

const processes = ref<ProcessDef[]>([]);
const loading = ref(true);
const loadError = ref('');
const toast = ref('');
const actionLoading = ref('');

/** 场景 → 展示文案与配色（复用全局 tag 色板） */
const sceneMeta: Record<string, { label: string; tone: string }> = {
  BUSINESS: { label: '业务', tone: 'tag--info' },
  DATA: { label: '数据', tone: 'tag--accent' },
  MANUFACTURING: { label: '制造', tone: 'tag--warning' },
};

const filtered = computed(() =>
  processes.value.filter((p) => {
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

async function loadProcesses() {
  loading.value = true;
  loadError.value = '';
  try {
    processes.value = await fetchProcesses();
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '流程数据加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadProcesses);

function showToast(message: string) {
  toast.value = message;
  window.setTimeout(() => (toast.value = ''), 2000);
}

/* —— 新建流程 —— */
const showCreate = ref(false);
const newProcess = ref({
  name: '',
  scene: 'BUSINESS' as ProcessDef['scene'],
  level: 'L2' as ProcessDef['level'],
  description: '',
});

async function handleCreate() {
  if (!newProcess.value.name.trim()) return;
  actionLoading.value = 'create';
  try {
    await createProcess({
      name: newProcess.value.name,
      scene: newProcess.value.scene,
      level: newProcess.value.level,
      description: newProcess.value.description,
    });
    showCreate.value = false;
    newProcess.value = { name: '', scene: 'BUSINESS', level: 'L2', description: '' };
    await loadProcesses();
    showToast('流程已创建');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '创建失败';
  } finally {
    actionLoading.value = '';
  }
}

/* —— 配置（编辑）流程 —— */
const editing = ref<ProcessDef | null>(null);
const editForm = ref({ name: '', scene: 'BUSINESS' as ProcessDef['scene'], level: 'L2' as ProcessDef['level'], description: '' });

function openEdit(p: ProcessDef) {
  editing.value = p;
  editForm.value = {
    name: p.name,
    scene: p.scene,
    level: p.level,
    description: p.description,
  };
}

async function handleEdit() {
  if (!editing.value || !editForm.value.name.trim()) return;
  actionLoading.value = 'edit';
  try {
    await updateProcess(editing.value.id, {
      name: editForm.value.name,
      scene: editForm.value.scene,
      level: editForm.value.level,
      description: editForm.value.description,
    });
    editing.value = null;
    await loadProcesses();
    showToast('流程已更新');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '更新失败';
  } finally {
    actionLoading.value = '';
  }
}

async function handleDelete(p: ProcessDef) {
  if (!confirm(`确定删除流程「${p.name}」？其下路线将一并清理。`)) return;
  actionLoading.value = `del-${p.id}`;
  try {
    await deleteProcess(p.id);
    await loadProcesses();
    showToast('流程已删除');
  } catch (err) {
    loadError.value = err instanceof Error ? err.message : '删除失败';
  } finally {
    actionLoading.value = '';
  }
}

/* —— 导入流程（占位：读取本地 JSON/Excel 后提示） —— */
const showImport = ref(false);
const importText = ref('');
function handleImport() {
  showImport.value = false;
  importText.value = '';
  showToast('导入功能需后端批量接口，当前为演示占位');
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
        <button class="btn btn-primary" @click="showCreate = true">
          <Icon name="plus" :size="15" />新建流程
        </button>
        <button class="btn btn-ghost" @click="showImport = true">
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
        <option value="MANUFACTURING">制造</option>
      </select>
      <select v-model="levelFilter" class="select filter-select">
        <option value="">全部等级</option>
        <option value="L1">L1</option>
        <option value="L2">L2</option>
        <option value="L3">L3</option>
        <option value="L4">L4</option>
      </select>
      <span class="faint filter-count">共 {{ filtered.length }} 个流程</span>
    </div>

    <!-- 错误提示 -->
    <div v-if="loadError" class="alert-banner">
      <Icon name="alert" :size="14" />
      <span>{{ loadError }}</span>
      <button class="btn btn-ghost btn-sm" @click="loadProcesses">重试</button>
    </div>

    <div v-if="toast" class="toast">
      <Icon name="check" :size="14" />{{ toast }}
    </div>

    <!-- 流程表格 -->
    <div class="card table-card">
      <div class="card-header">
        <div class="card-title">全部流程</div>
        <span class="faint">按场景 / 等级 / 名称筛选</span>
      </div>

      <div v-if="loading" class="empty">
        <div class="empty-title">加载流程数据…</div>
      </div>

      <template v-else-if="filtered.length">
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
                  <button class="btn btn-ghost btn-sm" @click="openEdit(p)">配置</button>
                  <button class="btn btn-danger btn-sm" :disabled="actionLoading === `del-${p.id}`" @click="handleDelete(p)">删除</button>
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

    <!-- 新建流程弹窗 -->
    <div v-if="showCreate" class="modal-overlay" @click.self="showCreate = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">新建流程</div>
          <button class="btn btn-ghost btn-sm" @click="showCreate = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label class="label">流程名称 *</label>
            <input v-model="newProcess.name" class="input" placeholder="如：付款流程" />
          </div>
          <div class="grid form-grid">
            <div class="field">
              <label class="label">场景</label>
              <select v-model="newProcess.scene" class="select">
                <option value="BUSINESS">业务</option>
                <option value="DATA">数据</option>
                <option value="MANUFACTURING">制造</option>
              </select>
            </div>
            <div class="field">
              <label class="label">等级</label>
              <select v-model="newProcess.level" class="select">
                <option value="L1">L1</option>
                <option value="L2">L2</option>
                <option value="L3">L3</option>
                <option value="L4">L4</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label class="label">描述</label>
            <input v-model="newProcess.description" class="input" placeholder="流程说明…" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showCreate = false">取消</button>
          <button class="btn btn-primary" :disabled="actionLoading === 'create' || !newProcess.name.trim()" @click="handleCreate">
            {{ actionLoading === 'create' ? '保存中…' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 配置流程弹窗 -->
    <div v-if="editing" class="modal-overlay" @click.self="editing = null">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">配置流程 · {{ editing.name }}</div>
          <button class="btn btn-ghost btn-sm" @click="editing = null">关闭</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label class="label">流程名称</label>
            <input v-model="editForm.name" class="input" />
          </div>
          <div class="grid form-grid">
            <div class="field">
              <label class="label">场景</label>
              <select v-model="editForm.scene" class="select">
                <option value="BUSINESS">业务</option>
                <option value="DATA">数据</option>
                <option value="MANUFACTURING">制造</option>
              </select>
            </div>
            <div class="field">
              <label class="label">等级</label>
              <select v-model="editForm.level" class="select">
                <option value="L1">L1</option>
                <option value="L2">L2</option>
                <option value="L3">L3</option>
                <option value="L4">L4</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label class="label">描述</label>
            <input v-model="editForm.description" class="input" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="editing = null">取消</button>
          <button class="btn btn-primary" :disabled="actionLoading === 'edit' || !editForm.name.trim()" @click="handleEdit">
            {{ actionLoading === 'edit' ? '保存中…' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 导入弹窗 -->
    <div v-if="showImport" class="modal-overlay" @click.self="showImport = false">
      <div class="modal">
        <div class="modal-header">
          <div class="modal-title">导入流程</div>
          <button class="btn btn-ghost btn-sm" @click="showImport = false">关闭</button>
        </div>
        <div class="modal-body">
          <div class="field">
            <label class="label">JSON / Excel 内容（占位）</label>
            <textarea v-model="importText" class="input textarea" rows="6" placeholder="粘贴流程定义数据…" />
          </div>
          <p class="muted note">提示：批量导入需要后端解析接口，当前仅做前端交互占位。请按项目实际格式提供数据。</p>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showImport = false">取消</button>
          <button class="btn btn-primary" @click="handleImport">导入</button>
        </div>
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

.alert-banner {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; margin-bottom: 12px;
  border-radius: var(--radius-sm);
  background: var(--danger-soft); color: var(--danger);
  border: 1px solid rgba(220, 38, 38, 0.35);
  font-size: 12.5px;
}
.toast {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 8px 12px; margin-bottom: 12px;
  color: var(--success); background: var(--success-soft);
  border-radius: var(--radius-sm); font-size: 13px;
}

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0; z-index: 50;
  background: rgba(0, 0, 0, .45);
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}
.modal {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: var(--radius-lg); width: 520px; max-width: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, .2);
}
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid var(--border);
}
.modal-title { font-weight: 600; }
.modal-body { padding: 20px; }
.modal-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 20px; border-top: 1px solid var(--border);
}
.form-grid { grid-template-columns: repeat(2, 1fr); gap: 16px; }
.field { margin-bottom: 14px; }
.field:last-child { margin-bottom: 0; }
.label { display: block; font-size: 12px; color: var(--fg-muted); margin-bottom: 6px; }
.input, .select, .textarea {
  width: 100%; padding: 8px 10px; border-radius: var(--radius-sm);
  border: 1px solid var(--border); background: var(--surface);
  color: var(--fg); font-size: 13px;
}
.textarea { resize: vertical; min-height: 80px; }
.note { font-size: 12px; margin-top: 8px; }
.btn-danger {
  background: var(--danger-soft); color: var(--danger);
  border: 1px solid rgba(220, 38, 38, 0.35);
}
.btn-danger:hover { background: var(--danger); color: #fff; }
</style>
