<script setup lang="ts">
/**
 * 大模型接入（cc-switch 式多配置 + 「启动」切换）。
 * 独立于系统管理页；内置「默认配置」env 兜底（无 DB 启用项时列表首条、当前生效）。
 * 本页不做自动连接测试（用户明确）；可手动「测试」行级配置。
 */
import { onMounted, ref } from 'vue';
import {
  activateLlmConfig, createLlmConfig, deleteLlmConfig, fetchLlmConfig,
  fetchLlmConfigs, testLlmConfig, updateLlmConfig,
} from '@/api';
import type { LlmConfigInfo, LlmConfigSavePayload } from '@/types';
import Icon from '@/components/Icon.vue';

const llmConfigs = ref<LlmConfigInfo[]>([]);
const currentLlm = ref<LlmConfigInfo | null>(null);
const llmSaving = ref(false);
const llmSaved = ref(false);
const llmAdminOnly = ref(false);
const llmError = ref('');
const llmTip = ref('');

const llmFormOpen = ref(false);
const editingId = ref<number | null>(null);
const llmFormName = ref('');
const llmFormBaseUrl = ref('');
const llmFormModel = ref('');
const llmFormTimeout = ref('30000');
const llmFormMaxTokens = ref('2048');
const llmFormTemp = ref('0.2');
const llmFormApiKey = ref('');

/** 手动测试进行中的配置 key（防重入） */
const llmTestingKey = ref<string | null>(null);

/** v-for key：env 默认配置 id 为 null，统一成 'default' 防撞 */
function llmKey(c: LlmConfigInfo): string {
  return c.id != null ? String(c.id) : 'default';
}

/** 内置「默认配置」env 兜底行（不可编辑/删除/启动） */
function isEnvDefault(c: LlmConfigInfo): boolean {
  return c.source === 'env' && c.id == null;
}

async function loadLlm() {
  llmAdminOnly.value = false;
  llmError.value = '';
  try {
    llmConfigs.value = await fetchLlmConfigs();
  } catch (e) {
    const msg = (e as Error).message || '';
    if (msg.includes('无权限') || msg.includes('403')) {
      llmAdminOnly.value = true;
    } else {
      llmError.value = msg || '加载失败';
    }
  }
}

async function loadCurrentLlm() {
  try {
    currentLlm.value = await fetchLlmConfig();
  } catch {
    currentLlm.value = null;
  }
}

/** 新建：打开空表单 */
function startNewLlm() {
  editingId.value = null;
  llmFormName.value = '';
  llmFormBaseUrl.value = '';
  llmFormModel.value = '';
  llmFormTimeout.value = '30000';
  llmFormMaxTokens.value = '2048';
  llmFormTemp.value = '0.2';
  llmFormApiKey.value = '';
  llmFormOpen.value = true;
}

/** 编辑：填充表单（apiKey 留空 = 不改） */
function editLlm(c: LlmConfigInfo) {
  editingId.value = c.id ?? null;
  llmFormName.value = c.name ?? '';
  llmFormBaseUrl.value = c.baseUrl ?? '';
  llmFormModel.value = c.model ?? '';
  llmFormTimeout.value = String(c.timeoutMs ?? 30000);
  llmFormMaxTokens.value = String(c.maxTokens ?? 2048);
  llmFormTemp.value = String(c.temperature ?? 0.2);
  llmFormApiKey.value = '';
  llmFormOpen.value = true;
}

async function saveLlm() {
  llmSaving.value = true;
  llmSaved.value = false;
  llmError.value = '';
  llmTip.value = '';
  try {
    const payload: LlmConfigSavePayload = {};
    if (llmFormName.value.trim()) payload.name = llmFormName.value.trim();
    if (llmFormBaseUrl.value.trim()) payload.baseUrl = llmFormBaseUrl.value.trim();
    if (llmFormModel.value.trim()) payload.model = llmFormModel.value.trim();
    const t = Number(llmFormTimeout.value);
    if (!Number.isNaN(t) && t > 0) payload.timeoutMs = t;
    const tk = Number(llmFormMaxTokens.value);
    if (!Number.isNaN(tk) && tk > 0) payload.maxTokens = tk;
    const tp = Number(llmFormTemp.value);
    if (!Number.isNaN(tp)) payload.temperature = tp;
    if (llmFormApiKey.value.trim()) payload.apiKey = llmFormApiKey.value.trim();
    if (editingId.value == null) {
      if (!payload.name) {
        llmError.value = '请填写配置名';
        return;
      }
      await createLlmConfig(payload);
    } else {
      await updateLlmConfig(editingId.value, payload);
    }
    llmSaved.value = true;
    window.setTimeout(() => (llmSaved.value = false), 3000);
    llmFormOpen.value = false;
    await loadLlm();
    await loadCurrentLlm();
  } catch (e) {
    llmError.value = (e as Error).message || '保存失败';
  } finally {
    llmSaving.value = false;
  }
}

/** 「启动」：目标配置成为当前生效（不做自动连接测试，与图来源页切换一致但此处无门禁） */
async function activateConfig(id: number) {
  llmError.value = '';
  llmTip.value = '';
  try {
    await activateLlmConfig(id);
    await loadLlm();
    await loadCurrentLlm();
  } catch (e) {
    llmError.value = (e as Error).message || '启用失败';
  }
}

async function removeConfig(c: LlmConfigInfo) {
  if (isEnvDefault(c) || c.id == null) return;
  if (!window.confirm(`确认删除大模型配置「${c.name}」？`)) return;
  llmError.value = '';
  llmTip.value = '';
  try {
    await deleteLlmConfig(c.id);
    if (editingId.value === c.id) llmFormOpen.value = false;
    await loadLlm();
    await loadCurrentLlm();
  } catch (e) {
    llmError.value = (e as Error).message || '删除失败';
  }
}

/** 手动测试连通性（不改变启用状态） */
async function testConfig(c: LlmConfigInfo) {
  if (llmTestingKey.value) return;
  llmTestingKey.value = llmKey(c);
  llmError.value = '';
  llmTip.value = '';
  try {
    const r = await testLlmConfig(c.id);
    if (r.ok) {
      llmTip.value = `「${c.name || '默认配置'}」连接成功 · ${r.latencyMs ?? '—'} ms`;
    } else {
      llmError.value = `「${c.name || '默认配置'}」连接失败：${r.message || '未知错误'}`;
    }
  } catch (e) {
    llmError.value = `测试异常：${(e as Error).message || '未知错误'}`;
  } finally {
    llmTestingKey.value = null;
  }
}

onMounted(() => {
  loadLlm();
  loadCurrentLlm();
});
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">大模型接入</div>
        <div class="page-subtitle">OpenAI 兼容协议（DeepSeek / 通义千问等）· 多配置切换启用 · 内置「默认配置」兜底</div>
      </div>
    </div>

    <div v-if="llmAdminOnly" class="openapi-alert mb-lg">
      <Icon name="alert" :size="15" />仅管理员可配置大模型接入
    </div>

    <template v-else>
      <!-- 当前大模型 -->
      <div class="card mb-lg">
        <div class="card-header">
          <div class="card-title">当前大模型</div>
          <button class="btn btn-ghost btn-sm" @click="startNewLlm">
            <Icon name="plus" :size="13" />新建配置
          </button>
        </div>
        <div class="card-body">
          <div class="row llm-status">
            <span class="llm-status-label">当前大模型：</span>
            <span v-if="currentLlm" class="tag" :class="currentLlm.source === 'db' ? 'tag--accent' : 'tag--neutral'">
              {{ currentLlm.name || '未命名' }}（{{ currentLlm.source === 'db' ? 'DB 配置' : '内置默认' }}）
            </span>
            <span v-else class="tag tag--neutral">未配置</span>
            <span v-if="currentLlm?.hasKey" class="mono llm-masked">Key：{{ currentLlm.apiKeyMasked }}</span>
            <span v-else-if="currentLlm" class="tag tag--warning">未配置 Key（图来源细化走 Noop）</span>
          </div>
          <div v-if="llmError" class="openapi-alert mt-lg">
            <Icon name="alert" :size="15" />{{ llmError }}
          </div>
          <div v-if="llmTip" class="saved-tip mt-lg">
            <Icon name="check" :size="14" />{{ llmTip }}
          </div>
        </div>
      </div>

      <!-- 配置列表 -->
      <div class="card mb-lg">
        <div class="card-header">
          <div class="card-title">配置列表</div>
          <span class="card-sub faint">「启动」切换当前生效配置（本页不自动测速，可手动「测试」）</span>
        </div>
        <div class="card-body">
          <div class="llm-list">
            <div v-for="c in llmConfigs" :key="llmKey(c)" class="llm-item" :class="{ 'llm-item--active': c.isActive }">
              <div class="llm-item-main">
                <div class="llm-item-name">
                  {{ c.name || '未命名' }}
                  <span v-if="c.isActive" class="tag tag--success">启用中</span>
                  <span v-else-if="isEnvDefault(c)" class="tag tag--neutral">内置默认</span>
                </div>
                <div class="llm-item-meta">{{ c.baseUrl }} · {{ c.model }}</div>
                <div class="llm-item-key mono">{{ c.hasKey ? c.apiKeyMasked : '未配置 Key' }}</div>
              </div>
              <div class="llm-item-actions">
                <span v-if="isEnvDefault(c)" class="faint llm-env-note">内置默认，不可编辑</span>
                <template v-else>
                  <button v-if="!c.isActive" class="btn btn-sm btn-primary" @click="activateConfig(c.id!)">启动</button>
                  <button class="btn btn-xs btn-ghost" :disabled="llmTestingKey != null" @click="testConfig(c)">
                    {{ llmTestingKey === llmKey(c) ? '测试中…' : '测试' }}
                  </button>
                  <button class="btn btn-xs btn-ghost" @click="editLlm(c)">编辑</button>
                  <button class="btn btn-xs btn-danger" @click="removeConfig(c)">删除</button>
                </template>
              </div>
            </div>
            <div v-if="!llmConfigs.length" class="empty">
              <div class="empty-title">暂无配置，点击「新建配置」添加</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 新建/编辑表单 -->
      <div v-if="llmFormOpen" class="card mb-lg">
        <div class="card-header">
          <div class="card-title">{{ editingId == null ? '新建配置' : '编辑配置' }}</div>
        </div>
        <div class="card-body">
          <div class="grid form-grid">
            <div class="field">
              <label class="label">配置名<span class="faint">（必填）</span></label>
              <input v-model="llmFormName" class="input" placeholder="如 DeepSeek 生产" />
            </div>
            <div class="field">
              <label class="label">API Base URL</label>
              <input v-model="llmFormBaseUrl" class="input" placeholder="https://api.deepseek.com/v1" />
            </div>
            <div class="field">
              <label class="label">API Key</label>
              <input v-model="llmFormApiKey" class="input" type="password" :placeholder="editingId != null ? '已配置（留空 = 不改）' : '请输入 API Key'" autocomplete="off" />
            </div>
            <div class="field">
              <label class="label">模型名</label>
              <input v-model="llmFormModel" class="input" placeholder="deepseek-chat" />
            </div>
            <div class="field">
              <label class="label">超时（毫秒）</label>
              <input v-model="llmFormTimeout" class="input" type="number" min="1000" />
            </div>
            <div class="field">
              <label class="label">最大输出 Tokens</label>
              <input v-model="llmFormMaxTokens" class="input" type="number" min="1" />
            </div>
            <div class="field">
              <label class="label">采样温度</label>
              <input v-model="llmFormTemp" class="input" type="number" step="0.1" min="0" max="2" />
            </div>
          </div>
          <div class="row mt-lg">
            <button class="btn btn-primary" :disabled="llmSaving" @click="saveLlm">
              <Icon name="check" :size="14" />{{ llmSaving ? '保存中…' : '保存' }}
            </button>
            <button class="btn btn-ghost" @click="llmFormOpen = false">取消</button>
            <span v-if="llmSaved" class="saved-tip"><Icon name="check" :size="14" />已保存（立即生效）</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.mb-lg { margin-bottom: var(--space-lg); }
.mt-lg { margin-top: var(--space-lg); }
.card-sub { font-size: 12px; }
.form-grid { grid-template-columns: repeat(2, 1fr); }
.saved-tip {
  display: inline-flex; align-items: center; gap: 5px;
  color: var(--success); font-size: 13px;
}

.openapi-alert {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: var(--radius-sm);
  font-size: 13px; color: var(--danger); background: var(--danger-soft);
}

.llm-status { gap: var(--space-sm); flex-wrap: wrap; align-items: center; }
.llm-status-label { font-size: 12.5px; color: var(--fg-faint); }
.llm-masked { color: var(--fg-faint); font-size: 12px; }
.llm-env-note { font-size: 12px; }

.llm-list { display: flex; flex-direction: column; gap: 10px; }
.llm-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 14px; border: 1px solid var(--border); border-radius: var(--radius);
  background: var(--surface-2);
}
.llm-item--active { border-color: var(--accent-border); background: var(--accent-soft); }
.llm-item-main { flex: 1; min-width: 0; }
.llm-item-name { display: flex; align-items: center; gap: 8px; font-size: 13.5px; font-weight: 700; }
.llm-item-meta { font-size: 11.5px; color: var(--fg-muted); margin-top: 3px; font-family: var(--font-mono); }
.llm-item-key { font-size: 11.5px; color: var(--fg-faint); margin-top: 2px; }
.llm-item-actions { display: flex; gap: 6px; flex-shrink: 0; flex-wrap: wrap; justify-content: flex-end; align-items: center; }
.btn-xs { height: 24px; padding: 0 8px; font-size: 11px; border-radius: 6px; display: inline-flex; align-items: center; gap: 4px; }
</style>
