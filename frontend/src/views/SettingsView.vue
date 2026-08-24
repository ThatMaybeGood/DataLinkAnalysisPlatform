<script setup lang="ts">
/** 系统管理页：运行信息、开放 API、用户与角色、系统配置、数据权限说明 */
import { onMounted, ref } from 'vue';
import { fetchHealth, fetchOpenApiInfo } from '@/api';
import type { HealthInfo } from '@/types';
import Tag from '@/components/Tag.vue';
import Icon from '@/components/Icon.vue';

/* —— 运行信息（真实数据，来自后端 /api/health）—— */
const health = ref<HealthInfo | null>(null);
const healthFailed = ref(false);
onMounted(async () => {
  try {
    health.value = await fetchHealth();
  } catch {
    healthFailed.value = true;
  }
  loadOpenApi();
});

/* —— 开放 API（仅管理员，来自后端 /api/system/openapi）—— */
interface OpenApiEndpoint { method: string; path: string; desc: string; }
interface OpenApiInfo { token: string; basePath: string; endpoints: OpenApiEndpoint[]; }

const openApi = ref<OpenApiInfo | null>(null);
const openApiFailed = ref(false);
const openApiError = ref('');
const copiedTip = ref(false);

async function loadOpenApi() {
  openApiFailed.value = false;
  openApiError.value = '';
  try {
    openApi.value = await fetchOpenApiInfo();
  } catch (e) {
    openApiFailed.value = true;
    openApiError.value = (e as Error).message || '加载失败';
  }
}

async function copyToken() {
  if (!openApi.value?.token) return;
  try {
    await navigator.clipboard.writeText(openApi.value.token);
    copiedTip.value = true;
    window.setTimeout(() => (copiedTip.value = false), 2000);
  } catch {
    /* 剪贴板不可用时静默 */
  }
}

/** HTTP 方法徽标配色 */
function methodTone(m: string): string {
  switch (m.toUpperCase()) {
    case 'GET': return 'api-method--get';
    case 'POST': return 'api-method--post';
    case 'PUT':
    case 'PATCH': return 'api-method--put';
    case 'DELETE': return 'api-method--delete';
    default: return '';
  }
}

interface RoleItem {
  name: string;
  code: string;
  desc: string;
  count: number;
  tone: string;
}

interface UserItem {
  name: string;
  displayName: string;
  role: string;
  status: 'ENABLED' | 'DISABLED';
  email: string;
  lastLogin: string;
}

/* —— 本地 mock：角色 —— */
const roles: RoleItem[] = [
  { name: '管理员', code: 'ADMIN', desc: '平台全部功能与权限管理', count: 3, tone: 'tag--accent' },
  { name: '建模师', code: 'MODELER', desc: '配置流程、路线与检测点等业务模型', count: 6, tone: 'tag--info' },
  { name: '运维监控', code: 'OPERATOR', desc: '日常巡检、告警处置与实例排查', count: 12, tone: 'tag--warning' },
  { name: '值班人', code: 'ONCALL', desc: 'SLA 超时处置与告警值守', count: 8, tone: 'tag--danger' },
  { name: '管理层', code: 'VIEWER', desc: '只读查看看板与各类报表', count: 15, tone: 'tag--neutral' },
];

const roleTone: Record<string, string> = Object.fromEntries(roles.map((r) => [r.code, r.tone]));
const roleName = (code: string) => roles.find((r) => r.code === code)?.name ?? code;
const toneOf = (code: string) => roleTone[code] ?? 'tag--neutral';

/* —— 本地 mock：用户 —— */
const users: UserItem[] = [
  { name: 'zhangwei', displayName: '张伟', role: 'ADMIN', status: 'ENABLED', email: 'zhangwei@corp.com', lastLogin: '2026-08-16 09:12' },
  { name: 'liqiang', displayName: '李强', role: 'MODELER', status: 'ENABLED', email: 'liqiang@corp.com', lastLogin: '2026-08-16 08:47' },
  { name: 'wangfang', displayName: '王芳', role: 'OPERATOR', status: 'ENABLED', email: 'wangfang@corp.com', lastLogin: '2026-08-16 08:02' },
  { name: 'zhaomin', displayName: '赵敏', role: 'ONCALL', status: 'ENABLED', email: 'zhaomin@corp.com', lastLogin: '2026-08-16 07:30' },
  { name: 'chenlu', displayName: '陈璐', role: 'VIEWER', status: 'ENABLED', email: 'chenlu@corp.com', lastLogin: '2026-08-15 18:20' },
  { name: 'sunlei', displayName: '孙磊', role: 'OPERATOR', status: 'DISABLED', email: 'sunlei@corp.com', lastLogin: '2026-08-10 15:44' },
];

/* —— 本地 mock：系统配置（后端暂无系统配置接口，先本地演示） —— */
const retentionDays = ref('180');
const alertChannel = ref('站内+邮件');
const collectMode = ref('特征推断+人工标记');
const versionKeep = ref('20');
const savedTip = ref(false);

function saveConfig() {
  // TODO: 后端补充 /api/system/config 后改为真实接口调用
  savedTip.value = true;
  window.setTimeout(() => (savedTip.value = false), 3000);
}
</script>

<template>
  <div class="page">
    <div class="page-header">
      <div>
        <div class="page-title">系统管理</div>
        <div class="page-subtitle">平台基础设置：账号权限、全局参数与数据权限基线</div>
      </div>
    </div>

    <!-- 运行信息（真实数据，来自后端 /api/health） -->
    <div class="card mb-lg">
      <div class="card-header">
        <div class="card-title">运行信息</div>
        <span class="card-sub faint">来自后端健康探活 /api/health</span>
      </div>
      <div class="card-body">
        <div v-if="healthFailed" class="muted">
          后端未连接（当前为前端独立预览）。请先启动后端：
          <code class="code-inline">cd backend && mvn spring-boot:run</code>
        </div>
        <div v-else-if="health" class="mode-grid">
          <div class="mode-cell">
            <div class="mode-label">运行模式</div>
            <div class="mode-value">
              <span class="mode-dot" :class="health.mode === 'mysql' ? 'dot--up' : 'dot--accent'"></span>
              {{ health.mode === 'mysql' ? 'MySQL 部署' : '离线本地 H2' }}
            </div>
          </div>
          <div class="mode-cell">
            <div class="mode-label">数据库状态</div>
            <div class="mode-value" :class="health.db === 'UP' ? 'text-ok' : 'text-bad'">
              {{ health.db === 'UP' ? '正常' : '异常' }}
            </div>
          </div>
          <div class="mode-cell">
            <div class="mode-label">后端版本</div>
            <div class="mode-value mono">{{ health.app }} v{{ health.version }}</div>
          </div>
          <div class="mode-cell">
            <div class="mode-label">探活时间</div>
            <div class="mode-value mono">{{ health.time }}</div>
          </div>
        </div>
        <div v-else class="muted">连接中…</div>
      </div>
    </div>

    <!-- 开放 API（外部系统集成，仅管理员） -->
    <div class="card mb-lg">
      <div class="card-header">
        <div class="card-title">开放 API（外部系统集成）</div>
        <span class="card-sub faint">供外部系统集成调用 · 仅管理员可查看</span>
      </div>
      <div class="card-body">
        <div v-if="openApiFailed" class="openapi-alert">
          <Icon name="alert" :size="15" />
          <span class="flex-1">{{ openApiError || '仅管理员可查看' }}</span>
          <button class="btn btn-ghost btn-sm" @click="loadOpenApi">重试</button>
        </div>
        <template v-else-if="openApi">
          <div class="api-token-row">
            <div class="api-token-label">API Token</div>
            <div class="row api-token-box">
              <code class="api-token">{{ openApi.token }}</code>
              <button class="btn btn-ghost btn-sm" @click="copyToken">
                <Icon name="export" :size="13" />{{ copiedTip ? '已复制' : '复制' }}
              </button>
            </div>
          </div>
          <div class="api-base muted">Base Path：<code class="code-inline">{{ openApi.basePath }}</code></div>
          <div class="api-list">
            <table class="data-table">
              <thead>
                <tr>
                  <th>方法</th>
                  <th>路径</th>
                  <th>说明</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(ep, i) in openApi.endpoints" :key="i">
                  <td><span class="api-method" :class="methodTone(ep.method)">{{ ep.method }}</span></td>
                  <td><code class="code-inline api-path">{{ ep.path }}</code></td>
                  <td class="cell-muted">{{ ep.desc }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
        <div v-else class="muted">连接中…</div>
      </div>
    </div>

    <!-- 用户与角色 -->
    <div class="card mb-lg">
      <div class="card-header">
        <div class="card-title">用户与角色</div>
        <span class="card-sub faint">{{ users.length }} 名用户 · {{ roles.length }} 个角色</span>
      </div>
      <div class="card-body">
        <div class="role-list">
          <div v-for="r in roles" :key="r.code" class="role-row">
            <div class="row role-main">
              <span class="role-name">{{ r.name }}</span>
              <span class="mono role-code">{{ r.code }}</span>
            </div>
            <div class="role-desc muted">{{ r.desc }}</div>
            <span class="tag role-count" :class="r.tone">{{ r.count }} 人</span>
          </div>
        </div>

        <div class="mt-lg">
          <table class="data-table">
            <thead>
              <tr>
                <th>用户名</th>
                <th>显示名</th>
                <th>角色</th>
                <th>状态</th>
                <th>邮箱</th>
                <th>最近登录</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.name">
                <td class="cell-strong">{{ u.name }}</td>
                <td class="cell-muted">{{ u.displayName }}</td>
                <td><span class="tag" :class="toneOf(u.role)">{{ roleName(u.role) }}</span></td>
                <td>
                  <Tag :status="u.status" :label="u.status === 'ENABLED' ? '启用' : '停用'" />
                </td>
                <td><span class="cell-mono">{{ u.email }}</span></td>
                <td><span class="cell-mono">{{ u.lastLogin }}</span></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 系统配置 -->
    <div class="card mb-lg">
      <div class="card-header">
        <div class="card-title">系统配置</div>
      </div>
      <div class="card-body">
        <div class="grid form-grid">
          <div class="field">
            <label class="label">数据保留天数</label>
            <input v-model="retentionDays" class="input" type="number" min="30" max="3650" />
          </div>
          <div class="field">
            <label class="label">告警渠道</label>
            <select v-model="alertChannel" class="select">
              <option>站内+邮件</option>
              <option>仅站内</option>
              <option>仅邮件</option>
            </select>
          </div>
          <div class="field">
            <label class="label">实例采集方式</label>
            <select v-model="collectMode" class="select">
              <option>特征推断+人工标记</option>
              <option>特征推断</option>
              <option>人工标记</option>
            </select>
          </div>
          <div class="field">
            <label class="label">默认版本保留数</label>
            <input v-model="versionKeep" class="input" type="number" min="5" max="100" />
          </div>
        </div>
        <div class="row">
          <button class="btn btn-primary" @click="saveConfig">
            <Icon name="check" :size="14" />保存配置
          </button>
          <span v-if="savedTip" class="saved-tip"><Icon name="check" :size="14" />已保存（本地演示，后端接口待实现）</span>
        </div>
      </div>
    </div>

    <!-- 数据权限说明 -->
    <div class="card">
      <div class="card-header">
        <div class="card-title">数据权限说明</div>
      </div>
      <div class="card-body">
        <div class="perm-list">
          <div class="row perm-item muted">
            <Icon name="database" :size="15" class="perm-icon" />
            <span>生产库连接使用只读最小权限，不授予写库能力</span>
          </div>
          <div class="row perm-item muted">
            <Icon name="shield" :size="15" class="perm-icon" />
            <span>数据源密码采用 AES 加密存储，链路内明文不落盘</span>
          </div>
          <div class="row perm-item muted">
            <Icon name="server" :size="15" class="perm-icon" />
            <span>敏感系统访问全程审计，操作留痕可回溯</span>
          </div>
          <div class="row perm-item muted">
            <Icon name="link" :size="15" class="perm-icon" />
            <span>授权按流程 / 关系网维度下发，不做全库放开</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.mb-lg { margin-bottom: var(--space-lg); }
.card-sub { font-size: 12px; }

/* 运行信息卡 */
.mode-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px 24px; }
.mode-label { font-size: 12px; color: var(--fg-faint); margin-bottom: 6px; }
.mode-value { display: inline-flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 600; }
.mode-dot { width: 8px; height: 8px; border-radius: 50%; }
.dot--up { background: var(--success); }
.dot--accent { background: var(--accent); }
.text-ok { color: var(--success); }
.text-bad { color: var(--danger); }
.code-inline {
  font-family: ui-monospace, "SF Mono", Menlo, monospace;
  font-size: 12px; padding: 1px 5px; border-radius: 4px;
  background: var(--surface-2); border: 1px solid var(--border);
}

.role-list { display: flex; flex-direction: column; }
.role-row {
  display: flex; align-items: center; gap: var(--space-lg);
  padding: 12px 0; border-bottom: 1px solid var(--border);
}
.role-row:last-child { border-bottom: none; }
.role-main { flex-shrink: 0; width: 190px; }
.role-name { font-size: 14px; font-weight: 600; }
.role-code { color: var(--fg-faint); font-size: 11px; }
.role-desc { flex: 1; font-size: 12.5px; }
.role-count { flex-shrink: 0; }

.form-grid { grid-template-columns: repeat(2, 1fr); }
.saved-tip {
  display: inline-flex; align-items: center; gap: 5px;
  color: var(--success); font-size: 13px;
}

.perm-item { gap: var(--space-md); padding: 10px 0; }
.perm-item:last-child { padding-bottom: 0; }
.perm-icon { color: var(--fg-faint); flex-shrink: 0; }

/* 开放 API 卡 */
.flex-1 { flex: 1; }
.openapi-alert {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: var(--radius-sm);
  font-size: 13px; color: var(--danger); background: var(--danger-soft);
}
.api-token-row { margin-bottom: 14px; }
.api-token-label { font-size: 12px; color: var(--fg-faint); margin-bottom: 6px; }
.api-token-box { gap: var(--space-sm); flex-wrap: wrap; }
.api-token {
  font-family: var(--font-mono); font-size: 12px;
  padding: 5px 10px; border-radius: 4px; word-break: break-all;
  background: var(--surface-2); border: 1px solid var(--border);
}
.api-base { font-size: 12.5px; margin-bottom: 14px; }
.api-method {
  font-family: var(--font-mono); font-size: 11px; font-weight: 600;
  padding: 2px 8px; border-radius: 4px;
}
.api-method--get { color: var(--success); background: var(--success-soft); }
.api-method--post { color: var(--accent); background: var(--accent-soft); }
.api-method--put { color: var(--info); background: var(--info-soft); }
.api-method--delete { color: var(--danger); background: var(--danger-soft); }
.api-path { font-size: 12px; }
.api-list { overflow-x: auto; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.api-list .data-table { min-width: 480px; }
</style>
