<script setup lang="ts">
/** 数据接入：数据池（连接器管理）—— 接真实后端接口
 *  列表分页 / 新建 / 编辑 / 测试 / 设为当前 / 删除 / 浏览库表与数据预览 */
import { computed, onMounted, reactive, ref } from 'vue';
import {
  activateConnector, createConnector, deleteConnector, fetchConnectorTables,
  fetchConnectors, fetchTablePreview, testConnector, updateConnector,
} from '@/api';
import type {
  ConnectorSavePayload, ConnectorTestResult, DataSourceConnector, TableInfo, TablePreview,
} from '@/types';
import Icon from '@/components/Icon.vue';
import Tag from '@/components/Tag.vue';

/* —— 列表状态 —— */
const records = ref<DataSourceConnector[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const keyword = ref('');
const loading = ref(false);
const error = ref('');

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

/** 类型 → 徽标（mysql 蓝 / postgresql 青 / h2 灰） */
const dbTypeMeta: Record<string, { label: string; tone: string }> = {
  mysql: { label: 'MySQL', tone: 'tag--accent' },
  postgresql: { label: 'PostgreSQL', tone: 'tag--info' },
  h2: { label: 'H2', tone: 'tag--neutral' },
};

/** H2 无 host/port，地址列单独展示 */
function hostPort(c: DataSourceConnector): string {
  if (c.dbType === 'h2') return 'H2 内存库';
  return [c.host, c.port].filter((x) => x !== undefined && x !== null && x !== '').join(':') || '—';
}

/** 最近测试 → 徽标（OK 绿 / FAIL 红 / 未测灰） */
function lastTestMeta(c: DataSourceConnector): { status: string; label: string } {
  if (c.lastTestStatus === 'OK') return { status: 'OK', label: '正常' };
  if (c.lastTestStatus === 'FAIL') return { status: 'FAIL', label: '失败' };
  return { status: 'IDLE', label: '未测' };
}

function fmt(t?: string): string {
  return t ? t.slice(0, 19).replace('T', ' ') : '';
}

/** 拉取指定页；目标页超出范围时回退到最后一页 */
async function fetchPage(p: number) {
  loading.value = true;
  error.value = '';
  try {
    const res = await fetchConnectors(p, size.value, keyword.value.trim());
    total.value = res.total;
    records.value = res.records;
    const tp = Math.max(1, Math.ceil(total.value / size.value));
    if (p > tp) {
      page.value = tp;
      const last = await fetchConnectors(tp, size.value, keyword.value.trim());
      records.value = last.records;
      total.value = last.total;
    } else {
      page.value = p;
    }
  } catch (e) {
    error.value = (e as Error).message || '加载连接器列表失败';
  } finally {
    loading.value = false;
  }
}

function loadList(p?: number) {
  fetchPage(p ?? page.value);
}

/* —— 测试连接 —— */
const testingId = ref('');
const testResultMap = ref<Record<string, ConnectorTestResult>>({});

/** 本地时间 yyyy-MM-ddTHH:mm:ss */
function nowIso(): string {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

async function testConn(c: DataSourceConnector) {
  if (testingId.value === c.id) return;
  testingId.value = c.id;
  try {
    const r = await testConnector(c.id);
    c.lastTestStatus = r.ok ? 'OK' : 'FAIL';
    c.lastTestTime = nowIso();
    testResultMap.value[c.id] = r;
  } catch (e) {
    c.lastTestStatus = 'FAIL';
    c.lastTestTime = nowIso();
    testResultMap.value[c.id] = { ok: false, message: (e as Error).message || '连接失败' };
  } finally {
    testingId.value = '';
  }
}

function testDetail(c: DataSourceConnector): string {
  const r = testResultMap.value[c.id];
  if (!r) return '';
  return r.ok
    ? `连接成功 · ${r.latencyMs ?? '—'} ms${r.dbVersion ? ` · ${r.dbVersion}` : ''}`
    : `失败：${r.message ?? '连接失败'}`;
}

/* —— 设为当前 / 删除 —— */
async function setActive(c: DataSourceConnector) {
  if (c.isActive === 1) return;
  if (!window.confirm(`确认将「${c.name}」设为当前连接？`)) return;
  try {
    await activateConnector(c.id);
    records.value.forEach((r) => (r.isActive = 0));
    c.isActive = 1;
  } catch (e) {
    window.alert((e as Error).message || '设为当前失败');
  }
}

async function removeConnector(c: DataSourceConnector) {
  if (!window.confirm(`确认删除连接「${c.name}」？该操作不可恢复。`)) return;
  try {
    await deleteConnector(c.id);
    if (browseId.value === c.id) browseId.value = '';
    await loadList();
  } catch (e) {
    window.alert((e as Error).message || '删除失败');
  }
}

/* —— 浏览：表清单 + 数据预览 —— */
const browseId = ref('');
const tables = ref<TableInfo[]>([]);
const tablesLoading = ref(false);
const tablesError = ref('');
const previewTable = ref('');
const preview = ref<TablePreview | null>(null);
const previewLoading = ref(false);
const previewError = ref('');

async function toggleBrowse(c: DataSourceConnector) {
  if (browseId.value === c.id) {
    browseId.value = '';
    return;
  }
  browseId.value = c.id;
  preview.value = null;
  previewTable.value = '';
  previewError.value = '';
  tablesLoading.value = true;
  tablesError.value = '';
  try {
    tables.value = await fetchConnectorTables(c.id);
  } catch (e) {
    tablesError.value = (e as Error).message || '加载表清单失败';
    tables.value = [];
  } finally {
    tablesLoading.value = false;
  }
}

async function loadPreview(table: string) {
  if (!browseId.value) return;
  previewTable.value = table;
  previewLoading.value = true;
  previewError.value = '';
  try {
    preview.value = await fetchTablePreview(browseId.value, table);
  } catch (e) {
    previewError.value = (e as Error).message || '加载预览失败';
    preview.value = null;
  } finally {
    previewLoading.value = false;
  }
}

function cellText(v: unknown): string {
  if (v === null || v === undefined) return 'NULL';
  if (typeof v === 'object') return JSON.stringify(v);
  return String(v);
}

/* —— 新建 / 编辑弹层 —— */
const showModal = ref(false);
const editing = ref<DataSourceConnector | null>(null);
const saving = ref(false);
const formError = ref('');
const form = reactive({
  name: '',
  dbType: 'mysql' as string,
  host: '',
  port: '',
  databaseName: '',
  username: '',
  password: '',
  config: '',
});

function openCreate() {
  editing.value = null;
  form.name = '';
  form.dbType = 'mysql';
  form.host = '';
  form.port = '';
  form.databaseName = '';
  form.username = '';
  form.password = '';
  form.config = '';
  formError.value = '';
  showModal.value = true;
}

function openEdit(c: DataSourceConnector) {
  editing.value = c;
  form.name = c.name;
  form.dbType = c.dbType;
  form.host = c.host ?? '';
  form.port = c.port ? String(c.port) : '';
  form.databaseName = c.databaseName;
  form.username = c.username;
  form.password = '';
  form.config = '';
  formError.value = '';
  showModal.value = true;
}

function closeModal() {
  if (saving.value) return;
  showModal.value = false;
}

async function submitForm() {
  formError.value = '';
  if (!form.name.trim()) { formError.value = '名称不能为空'; return; }
  if (!form.databaseName.trim()) { formError.value = '库名不能为空'; return; }
  if (!form.username.trim()) { formError.value = '用户名不能为空'; return; }
  if (!editing.value && !form.password) { formError.value = '新建连接时密码必填'; return; }
  if (form.dbType !== 'h2') {
    if (!form.host.trim()) { formError.value = '主机不能为空'; return; }
    if (!form.port) { formError.value = '端口不能为空'; return; }
  }
  if (form.port && Number.isNaN(Number(form.port))) { formError.value = '端口需为数字'; return; }

  const payload: ConnectorSavePayload = {
    name: form.name.trim(),
    dbType: form.dbType as ConnectorSavePayload['dbType'],
    host: form.host.trim() || undefined,
    port: form.port ? Number(form.port) : undefined,
    databaseName: form.databaseName.trim(),
    username: form.username.trim(),
    password: form.password || undefined,
    config: form.config.trim() || undefined,
    enabled: 1,
  };

  saving.value = true;
  try {
    if (editing.value) await updateConnector(editing.value.id, payload);
    else await createConnector(payload);
    showModal.value = false;
    await loadList();
  } catch (e) {
    formError.value = (e as Error).message || '保存失败';
  } finally {
    saving.value = false;
  }
}

onMounted(() => loadList(1));
</script>

<template>
  <div class="page">
    <!-- 页头 -->
    <div class="page-header">
      <div>
        <div class="page-title">数据接入</div>
        <div class="page-subtitle">连接器可插拔，统一采集外部数据图来源</div>
      </div>
      <div class="page-actions">
        <button class="btn btn-primary" @click="openCreate">
          <Icon name="plus" :size="15" />新建连接
        </button>
      </div>
    </div>

    <!-- 列表错误提示 -->
    <div v-if="error" class="alert alert--danger mb-md">
      <Icon name="alert" :size="15" />
      <span class="flex-1">{{ error }}</span>
      <button class="btn btn-ghost btn-sm" @click="loadList()">重试</button>
    </div>

    <!-- 连接列表 -->
    <div class="card card--table">
      <div class="card-header list-header">
        <div class="row">
          <div class="card-title">连接列表</div>
          <span class="count-text muted faint">共 {{ total }} 条</span>
        </div>
        <div class="search">
          <span class="search-icon"><Icon name="search" :size="14" /></span>
          <input v-model="keyword" placeholder="搜索名称 / 主机 / 库名" @keyup.enter="loadList(1)" />
        </div>
      </div>

      <!-- 加载态 -->
      <div v-if="loading" class="empty">
        <Icon name="database" :size="40" />
        <div class="empty-title">正在加载连接器…</div>
      </div>

      <!-- 空态 -->
      <div v-else-if="records.length === 0" class="empty">
        <Icon name="database" :size="40" />
        <div class="empty-title">暂无数据连接</div>
        <div class="empty-desc">点击右上角「新建连接」注册第一个数据源</div>
      </div>

      <template v-else>
        <table class="data-table">
          <thead>
            <tr>
              <th>名称</th>
              <th>类型</th>
              <th>地址</th>
              <th>库名</th>
              <th>当前连接</th>
              <th>最近测试</th>
              <th class="text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="c in records" :key="c.id">
              <tr>
                <td class="cell-strong">{{ c.name }}</td>
                <td>
                  <span class="tag" :class="dbTypeMeta[c.dbType]?.tone ?? 'tag--neutral'">
                    {{ dbTypeMeta[c.dbType]?.label ?? c.dbType }}
                  </span>
                </td>
                <td><span class="cell-mono">{{ hostPort(c) }}</span></td>
                <td><span class="cell-mono">{{ c.databaseName }}</span></td>
                <td>
                  <span v-if="c.isActive === 1" class="tag tag--accent tag--plain">当前</span>
                  <span v-else class="faint">—</span>
                </td>
                <td>
                  <Tag :status="lastTestMeta(c).status" :label="lastTestMeta(c).label" />
                  <div v-if="c.lastTestTime" class="cell-muted mono mt-xs">{{ fmt(c.lastTestTime) }}</div>
                  <div v-if="testResultMap[c.id]" class="test-detail" :class="testResultMap[c.id].ok ? 'ok' : 'fail'">
                    <Icon :name="testResultMap[c.id].ok ? 'check' : 'alert'" :size="12" />
                    <span>{{ testDetail(c) }}</span>
                  </div>
                </td>
                <td>
                  <div class="row ops">
                    <button class="btn btn-ghost btn-sm" :disabled="testingId === c.id" @click="testConn(c)">
                      <Icon v-if="testingId !== c.id" name="refresh" :size="13" />
                      {{ testingId === c.id ? '测试中…' : '测试' }}
                    </button>
                    <button v-if="c.isActive !== 1" class="btn btn-ghost btn-sm" @click="setActive(c)">
                      <Icon name="target" :size="13" />设为当前
                    </button>
                    <button class="btn btn-ghost btn-sm" @click="openEdit(c)">
                      <Icon name="settings" :size="13" />编辑
                    </button>
                    <button class="btn btn-danger btn-sm" @click="removeConnector(c)">删除</button>
                    <button class="btn btn-outline btn-sm" @click="toggleBrowse(c)">
                      <Icon name="database" :size="13" />{{ browseId === c.id ? '收起' : '浏览' }}
                    </button>
                  </div>
                </td>
              </tr>

              <!-- 浏览展开行：表清单 + 数据预览 -->
              <tr v-if="browseId === c.id" class="browse-row">
                <td :colspan="7" class="browse-cell">
                  <div v-if="tablesLoading" class="empty">
                    <Icon name="database" :size="28" />
                    <div class="empty-title">正在加载表清单…</div>
                  </div>
                  <div v-else-if="tablesError" class="alert alert--danger">
                    <Icon name="alert" :size="15" />{{ tablesError }}
                  </div>
                  <div v-else class="browse-body">
                    <div class="browse-side">
                      <div class="browse-head">表清单（{{ tables.length }}）</div>
                      <ul v-if="tables.length" class="table-list">
                        <li v-for="t in tables" :key="t.name">
                          <button
                            class="table-item"
                            :class="{ active: previewTable === t.name }"
                            @click="loadPreview(t.name)"
                          >
                            <Icon name="database" :size="13" />{{ t.name }}
                            <span class="ttype">{{ t.type }}</span>
                          </button>
                        </li>
                      </ul>
                      <div v-else class="faint">未发现表</div>
                    </div>
                    <div class="browse-main">
                      <div v-if="previewLoading" class="empty">
                        <div class="empty-title">加载预览数据…</div>
                      </div>
                      <div v-else-if="previewError" class="alert alert--danger">
                        <Icon name="alert" :size="15" />{{ previewError }}
                      </div>
                      <div v-else-if="preview" class="preview-wrap">
                        <div class="preview-head">
                          <span class="browse-head">预览 {{ previewTable }} · 前 {{ preview.rows.length }} 行</span>
                        </div>
                        <table class="data-table preview-table">
                          <thead>
                            <tr><th v-for="col in preview.columns" :key="col">{{ col }}</th></tr>
                          </thead>
                          <tbody>
                            <tr v-for="(row, i) in preview.rows" :key="i">
                              <td v-for="(cell, j) in row" :key="j" class="cell-mono">{{ cellText(cell) }}</td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                      <div v-else class="empty">
                        <Icon name="database" :size="28" />
                        <div class="empty-title">点击左侧表名查看数据预览</div>
                        <div class="empty-desc">展示前 50 行</div>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>

        <!-- 分页条 -->
        <div class="pager">
          <div class="faint">共 {{ total }} 条</div>
          <div class="row">
            <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="loadList(page - 1)">上一页</button>
            <span class="mono muted">{{ page }} / {{ totalPages }}</span>
            <button class="btn btn-ghost btn-sm" :disabled="page >= totalPages" @click="loadList(page + 1)">下一页</button>
            <select v-model.number="size" class="select pager-size" @change="loadList(1)">
              <option :value="10">10 条/页</option>
              <option :value="20">20 条/页</option>
              <option :value="50">50 条/页</option>
            </select>
          </div>
        </div>
      </template>
    </div>

    <!-- 新建 / 编辑弹层 -->
    <div v-if="showModal" class="modal-mask" @click.self="closeModal">
      <div class="modal">
        <div class="modal-head">
          <div class="card-title">{{ editing ? '编辑连接' : '新建连接' }}</div>
          <button class="modal-close" title="关闭" @click="closeModal">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="submitForm">
            <div class="form-grid">
              <div class="field">
                <label class="label">名称 <i class="req">*</i></label>
                <input v-model="form.name" class="input" placeholder="如：生产订单库" />
              </div>
              <div class="field">
                <label class="label">类型</label>
                <select v-model="form.dbType" class="select">
                  <option value="mysql">MySQL</option>
                  <option value="postgresql">PostgreSQL</option>
                  <option value="h2">H2</option>
                </select>
              </div>
              <div class="field">
                <label class="label">主机</label>
                <input v-model="form.host" class="input" placeholder="如：127.0.0.1（H2 可空）" />
              </div>
              <div class="field">
                <label class="label">端口</label>
                <input v-model="form.port" class="input" inputmode="numeric" placeholder="如：3306（H2 可空）" />
              </div>
              <div class="field">
                <label class="label">库名 <i class="req">*</i></label>
                <input v-model="form.databaseName" class="input" placeholder="如：datalink" />
              </div>
              <div class="field">
                <label class="label">用户名 <i class="req">*</i></label>
                <input v-model="form.username" class="input" placeholder="如：datalink" />
              </div>
              <div class="field">
                <label class="label">密码<template v-if="!editing"> <i class="req">*</i></template></label>
                <input v-model="form.password" type="password" class="input" :placeholder="editing ? '留空则不修改' : '新建必填'" />
              </div>
              <div class="field">
                <label class="label">备注</label>
                <input v-model="form.config" class="input" placeholder="备注信息" />
              </div>
            </div>

            <div v-if="formError" class="alert alert--danger mb-md">
              <Icon name="alert" :size="15" />{{ formError }}
            </div>

            <div class="modal-foot">
              <button type="button" class="btn btn-ghost" :disabled="saving" @click="closeModal">取消</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <Icon name="check" :size="15" />{{ saving ? '保存中…' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card--table { overflow: hidden; }
.list-header { flex-wrap: wrap; gap: var(--space-sm); }
.count-text { font-size: 12px; margin-left: 2px; }
.ops { white-space: nowrap; }
.flex-1 { flex: 1; }
.mt-xs { margin-top: 4px; }
.test-detail { display: flex; align-items: center; gap: 4px; font-size: 12px; margin-top: 4px; }
.test-detail.ok { color: var(--success); }
.test-detail.fail { color: var(--danger); }

/* 局部错误条（复用状态色变量） */
.alert { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: var(--radius-sm); font-size: 13px; }
.alert--danger { color: var(--danger); background: var(--danger-soft); }

/* 分页条 */
.pager {
  display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm);
  padding: 12px 20px; border-top: 1px solid var(--border);
}
.pager-size { width: 108px; height: 28px; font-size: 12px; }

/* 浏览：表清单 + 预览 */
.browse-cell { padding: 16px 20px; background: var(--surface-2); }
.browse-body { display: flex; gap: 16px; align-items: flex-start; }
.browse-side { width: 220px; flex-shrink: 0; }
.browse-head { font-size: 12px; font-weight: 600; color: var(--fg-muted); margin-bottom: 8px; }
.table-list {
  list-style: none; max-height: 320px; overflow-y: auto;
  border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface);
}
.table-item {
  display: flex; align-items: center; gap: 8px; width: 100%;
  padding: 8px 12px; border: none; border-bottom: 1px solid var(--border);
  background: none; cursor: pointer; font-size: 13px; color: var(--fg);
  text-align: left; font-family: inherit;
}
.table-item:last-child { border-bottom: none; }
.table-item:hover { background: var(--surface-3); }
.table-item.active { background: var(--accent-soft); color: var(--accent); }
.table-item .ttype { margin-left: auto; font-size: 11px; color: var(--fg-faint); }
.browse-main { flex: 1; min-width: 0; }
.preview-wrap { overflow-x: auto; background: var(--surface); border: 1px solid var(--border); border-radius: var(--radius-sm); }
.preview-head { display: flex; align-items: center; justify-content: space-between; padding: 10px 14px; border-bottom: 1px solid var(--border); }
.preview-head .browse-head { margin-bottom: 0; }
.preview-table { min-width: 640px; }
.preview-table th, .preview-table td { white-space: nowrap; }

/* 弹层 */
.modal-mask {
  position: fixed; inset: 0; z-index: 100;
  background: rgba(13, 20, 36, 0.45);
  display: flex; align-items: flex-start; justify-content: center;
  padding: 64px 16px; overflow-y: auto;
}
.modal {
  width: 100%; max-width: 620px; background: var(--surface);
  border: 1px solid var(--border); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg); animation: fadeUp 0.2s ease both;
}
.modal-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 20px; border-bottom: 1px solid var(--border); }
.modal-close {
  border: none; background: none; cursor: pointer; font-family: inherit;
  font-size: 18px; line-height: 1; color: var(--fg-faint);
  width: 28px; height: 28px; border-radius: var(--radius-sm);
}
.modal-close:hover { background: var(--surface-2); color: var(--fg); }
.modal-body { padding: 20px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 0 16px; }
.modal-foot { display: flex; justify-content: flex-end; gap: var(--space-sm); margin-top: 4px; }
.req { color: var(--danger); font-style: normal; }

@media (max-width: 720px) {
  .form-grid { grid-template-columns: 1fr; }
  .browse-body { flex-direction: column; }
  .browse-side { width: 100%; }
}
</style>
