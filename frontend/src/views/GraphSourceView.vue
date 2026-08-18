<script setup lang="ts">
/**
 * 图来源 · 自动/半自动分析出图（G2 前端交互原型）
 *
 * 对应文档书第 15 章：
 * - 15.2 三条路线入口：引擎分析（主干）/ 大模型分析（增强）/ 人工创建（兜底）
 * - 15.4 主动分流：引擎出草稿后用户主动选择「够用→人工校正 / 加大模型细化 / 作废重来」
 * - 15.6 流程：图来源入口 → 选路线 → 出草稿 → 主动分流 → 人工校正（唯一拍板）→ 准底图
 *
 * G2 阶段用假数据模拟全流程手感；G3 引擎、G4 大模型、G5 校正闭环将替换为真实后端。
 * 原则：主动权在人不在系统——系统只清楚呈现草稿，不做自检/裁决。
 */
import { computed, onBeforeUnmount, ref } from 'vue';
import type { EngineCandidate, EngineDraft, GraphEdge, GraphNode } from '@/types';
import { fetchEngineAnalyze, fetchConnectors } from '@/api';
import GraphCanvas from '@/components/GraphCanvas.vue';
import Icon from '@/components/Icon.vue';

type RouteKey = 'engine' | 'llm' | 'manual';
type Stage = 'entry' | 'scanning' | 'draft' | 'llm' | 'manual';

/* —— 三条路线元信息（15.2） —— */
const ROUTES_META = [
  {
    key: 'engine' as RouteKey,
    tag: '主干',
    tagClass: 'gs-route-tag--engine',
    name: '引擎分析',
    icon: 'database',
    desc: '自动连接业务库，扫描表结构与引用关系，识别单据模式（主键/单号/状态/时间/引用链/主子表），产出流程骨架草稿。',
    material: '素材：数据库结构',
    accent: '#3b82f6',
  },
  {
    key: 'llm' as RouteKey,
    tag: '增强',
    tagClass: 'gs-route-tag--llm',
    name: '大模型分析',
    icon: 'link',
    desc: '在库结构基础上引入大模型语义理解，补全业务动作、参与方与业务语义，产出语义细化草稿（可回退到纯引擎草稿）。',
    material: '素材：库结构 / 界面入口 / 导入文档',
    accent: '#8b5cf6',
  },
  {
    key: 'manual' as RouteKey,
    tag: '兜底',
    tagClass: 'gs-route-tag--manual',
    name: '人工创建',
    icon: 'users',
    desc: '从零手工建模：手工录入节点与关系，或导入 Excel / 文件后人工整理。所有路线最终统一汇入人工校正拍板。',
    material: '素材：人工 / 文件',
    accent: '#06b6d4',
  },
];

/* —— 引擎草稿：兜底假数据（G3 真实接口不可用时模拟扫描「门诊收费库」） —— */
const ENGINE_NODES: GraphNode[] = [
  { id: 'db1', name: '门诊收费库', code: 'ORD', nodeType: 'DATABASE', level: 'L1', status: 'ACTIVE', checkpoints: [] },
  { id: 't1', name: '挂号单表', code: 'reg_order', nodeType: 'TABLE', level: 'L2', status: 'ACTIVE', checkpoints: [] },
  { id: 't2', name: '收费单表', code: 'fee_order', nodeType: 'TABLE', level: 'L2', status: 'ACTIVE', checkpoints: [] },
  { id: 't3', name: '处方明细表', code: 'prescription_detail', nodeType: 'TABLE', level: 'L3', status: 'ACTIVE', checkpoints: [] },
  { id: 't4', name: '支付流水表', code: 'pay_record', nodeType: 'TABLE', level: 'L3', status: 'ACTIVE', checkpoints: [] },
  { id: 't5', name: '退费申请单表', code: 'refund_apply', nodeType: 'TABLE', level: 'L3', status: 'ACTIVE', checkpoints: [] },
  { id: 't6', name: '结算单表', code: 'settle_bill', nodeType: 'TABLE', level: 'L2', status: 'ACTIVE', checkpoints: [] },
  { id: 's1', name: '结算中心', code: 'SETTLE', nodeType: 'SYSTEM', level: 'L2', status: 'ACTIVE', checkpoints: [] },
];

const ENGINE_EDGES: GraphEdge[] = [
  { id: '1', source: 'db1', target: 't1', relationType: 'DATA_FLOW' },
  { id: '2', source: 'db1', target: 't2', relationType: 'DATA_FLOW' },
  { id: '3', source: 'db1', target: 't3', relationType: 'DATA_FLOW' },
  { id: '4', source: 'db1', target: 't4', relationType: 'DATA_FLOW' },
  { id: '5', source: 'db1', target: 't5', relationType: 'DATA_FLOW' },
  { id: '6', source: 'db1', target: 't6', relationType: 'DATA_FLOW' },
  { id: '7', source: 't1', target: 't2', relationType: 'DATA_FLOW' },
  { id: '8', source: 't2', target: 't3', relationType: 'DATA_FLOW' },
  { id: '9', source: 't2', target: 't4', relationType: 'DATA_FLOW' },
  { id: '10', source: 't2', target: 't5', relationType: 'DATA_FLOW' },
  { id: '11', source: 't2', target: 't6', relationType: 'DATA_FLOW' },
  { id: '12', source: 't6', target: 's1', relationType: 'DATA_FLOW' },
];

/* —— 大模型细化草稿：引擎骨架 + 语义补全（动作/参与方/业务关系） —— */
const LLM_NODES: GraphNode[] = [
  ...ENGINE_NODES,
  { id: 'dept1', name: '财务科', code: 'FIN', nodeType: 'DEPARTMENT', level: 'L2', status: 'ACTIVE', checkpoints: [] },
  { id: 'role1', name: '收费员', code: 'CASHIER', nodeType: 'ROLE', level: 'L3', status: 'ACTIVE', checkpoints: [] },
  { id: 'a1', name: '挂号', code: 'ACT_REG', nodeType: 'ACTION', level: 'L2', status: 'ACTIVE', checkpoints: [] },
  { id: 'a2', name: '收费', code: 'ACT_FEE', nodeType: 'ACTION', level: 'L2', status: 'ACTIVE', checkpoints: [] },
  { id: 'a3', name: '退费审批', code: 'ACT_REFUND', nodeType: 'ACTION', level: 'L3', status: 'ACTIVE', checkpoints: [] },
  { id: 'a4', name: '结算', code: 'ACT_SETTLE', nodeType: 'ACTION', level: 'L3', status: 'ACTIVE', checkpoints: [] },
];

const LLM_EDGES: GraphEdge[] = [
  ...ENGINE_EDGES,
  { id: '13', source: 'role1', target: 'a1', relationType: 'API' },
  { id: '14', source: 'a1', target: 't1', relationType: 'DATA_FLOW' },
  { id: '15', source: 'role1', target: 'a2', relationType: 'API' },
  { id: '16', source: 'a2', target: 't2', relationType: 'DATA_FLOW' },
  { id: '17', source: 'dept1', target: 'a3', relationType: 'APPROVAL' },
  { id: '18', source: 'a3', target: 't5', relationType: 'DATA_FLOW' },
  { id: '19', source: 's1', target: 'a4', relationType: 'API' },
  { id: '20', source: 'a4', target: 't6', relationType: 'DATA_FLOW' },
];

/* —— 引擎识别出的候选单据（15.3 置信度展示） —— */
interface DraftCandidate {
  name: string;
  table: string;
  confidence: number;
  marks: string[];
  low?: boolean;
}

/* 兜底候选清单（真实接口不可用时展示，G2 原型数据） */
const ENGINE_CANDIDATES_FALLBACK: DraftCandidate[] = [
  { name: '挂号单', table: 'reg_order', confidence: 95, marks: ['主键', '单号', '状态', '时间', '主子表'] },
  { name: '收费单', table: 'fee_order', confidence: 91, marks: ['主键', '单号', '状态', '时间', '主子表', '引用'] },
  { name: '退费申请单', table: 'refund_apply', confidence: 84, marks: ['单号', '状态', '引用'] },
  { name: '结算单', table: 'settle_bill', confidence: 76, marks: ['单号', '时间'] },
  { name: '支付流水', table: 'pay_record', confidence: 61, marks: ['时间', '引用'], low: true },
  { name: '处方明细', table: 'prescription_detail', confidence: 53, marks: ['主子表'], low: true },
];

/* —— G3 真实引擎分析草稿（acquireEngineDraft 赋值；失败回退兜底） —— */
const engineData = ref<EngineDraft | null>(null);
const engineCandidates = ref<DraftCandidate[]>([]);
const engineError = ref('');
const engineConnectorName = ref('');

/* —— 大模型语义补全清单 —— */
interface LlmRefinement {
  type: string;
  text: string;
}

const LLM_REFINEMENTS: LlmRefinement[] = [
  { type: '改名', text: '表 fee_order → 业务名「收费单」，识别为收费动作的主单据' },
  { type: '动作链', text: '补全业务动作链：挂号 → 收费 → 退费审批 → 结算' },
  { type: '参与方', text: '识别参与方：收费员（岗位）、财务科（部门）' },
  { type: '关系', text: '补全关系：退费审批由财务科负责（APPROVAL）、收费员发起收费（API）' },
  { type: '流程', text: '给出候选流程「门诊收费流程」，默认路线：挂号→收费→结算' },
];

/* —— 人工校正待确认清单（基于当前草稿） —— */
const MANUAL_NODES = [
  { name: '收费单', type: '单据', note: '引擎 91% 置信，大模型确认业务名「收费单」' },
  { name: '挂号单', type: '单据', note: '引擎 95% 置信' },
  { name: '结算单', type: '单据', note: '引擎 76% 置信' },
  { name: '退费审批', type: '动作', note: '大模型补全，财务科负责' },
  { name: '结算中心', type: '系统', note: '引擎从结算单归属推断' },
];

/* —— 状态机 —— */
const stage = ref<Stage>('entry');
const routeKey = ref<RouteKey | null>(null);
const scanningText = ref('');
const confirmedBaseGraph = ref(false);
const toast = ref('');
let scanTimer: number | null = null;

const SCANNING_TEXT: Record<RouteKey, string> = {
  engine: '引擎正在连接业务库并扫描表结构…识别主键 / 单号 / 状态 / 时间 / 引用链 / 主子表…',
  llm: '大模型正在读取库结构与文档素材，理解业务语义…',
  manual: '准备人工创建画布…',
};

/**
 * 引擎分析（G3 真实数据接入）：
 * 优先取已启用的 DB 连接器调用 /api/analyze；失败或未配置连接器时
 * 回退到 G2 兜底假数据（ENGINE_NODES/ENGINE_CANDIDATES_FALLBACK）。
 */
async function acquireEngineDraft() {
  engineError.value = '';
  try {
    const { records } = await fetchConnectors(1, 50);
    const dbConn = records.find((c) => c.connectorType === 'DB' && c.enabled === 1);
    if (!dbConn) throw new Error('未找到已启用的 DB 连接器，使用演示数据展示');
    const draft = await fetchEngineAnalyze(dbConn.id);
    engineData.value = draft;
    engineCandidates.value = (draft.candidates ?? []) as DraftCandidate[];
    engineConnectorName.value = dbConn.name;
  } catch (err) {
    console.warn('[G3] 引擎分析调用失败，回退演示数据：', err);
    engineData.value = null;
    engineCandidates.value = ENGINE_CANDIDATES_FALLBACK;
    engineError.value = err instanceof Error ? err.message : String(err);
  }
}

function startRoute(key: RouteKey) {
  routeKey.value = key;
  confirmedBaseGraph.value = false;
  if (key === 'manual') {
    // 人工创建：直接进入人工环节（G2 原型占位）
    stage.value = 'manual';
    return;
  }
  stage.value = 'scanning';
  scanningText.value = SCANNING_TEXT[key];
  if (scanTimer !== null) window.clearTimeout(scanTimer);
  scanTimer = window.setTimeout(() => {
    if (key === 'llm') {
      stage.value = 'llm';
      return;
    }
    stage.value = 'draft';
    if (engineData.value === null && engineCandidates.value.length === 0) {
      void acquireEngineDraft();
    }
  }, 1500);
}

/** 主动分流 ①：草稿够用 → 直接人工校正 */
function goManual() {
  stage.value = 'manual';
}

/** 主动分流 ②：草稿不够 → 加大模型细化（引擎路线内可回退） */
function refineWithLlm() {
  stage.value = 'llm';
}

/** 主动分流 ③：草稿太乱 → 作废重来（回到入口，可回退） */
function discardAll() {
  resetAll();
}

/** 可回退：细化不满意 → 回到纯引擎草稿 */
function revertToEngine() {
  stage.value = 'draft';
}

function resetAll() {
  if (scanTimer !== null) window.clearTimeout(scanTimer);
  scanTimer = null;
  stage.value = 'entry';
  routeKey.value = null;
  confirmedBaseGraph.value = false;
  engineError.value = '';
}

/** 人工校正拍板：确认准底图 */
function confirmBaseGraph() {
  confirmedBaseGraph.value = true;
  toast.value = '已确认准底图，流程骨架将写入平台关系网（G5 校正闭环将保留本次校正记录）';
  window.setTimeout(() => {
    resetAll();
  }, 1800);
}

onBeforeUnmount(() => {
  if (scanTimer !== null) window.clearTimeout(scanTimer);
});

/* —— 派生数据 —— */
const isDraftStage = computed(() => stage.value === 'draft' || stage.value === 'llm');
/** 引擎草稿节点：优先真实接口草稿，否则 G2 兜底假数据 */
const draftNodes = computed<GraphNode[]>(() => {
  if (stage.value === 'llm') return LLM_NODES;
  return engineData.value?.draftNodes?.length ? engineData.value.draftNodes : ENGINE_NODES;
});
/** 引擎草稿边 */
const draftEdges = computed<GraphEdge[]>(() => {
  if (stage.value === 'llm') return LLM_EDGES;
  return engineData.value?.draftEdges?.length ? engineData.value.draftEdges : ENGINE_EDGES;
});
const isLlmRefined = computed(() => stage.value === 'llm');
const isManualRoute = computed(() => routeKey.value === 'manual');
const draftTitle = computed(() => (isLlmRefined.value ? '大模型细化草稿' : '引擎分析草稿'));
const draftSummary = computed(() => {
  const n = draftNodes.value.length;
  const e = draftEdges.value.length;
  if (isLlmRefined.value) return `引擎骨架 + 大模型语义补全 · ${n} 节点 / ${e} 关系`;
  const src = engineData.value ? `扫描 ${engineData.value.database} · ${engineCandidates.value.length} 候选单据` : '引擎扫描 5 张表识别候选单据';
  return `${src} · ${n} 节点 / ${e} 关系`;
});

const steps = [
  { key: 'entry', label: '入口' },
  { key: 'route', label: '选路线' },
  { key: 'draft', label: '出草稿' },
  { key: 'split', label: '主动分流' },
  { key: 'correct', label: '人工校正' },
  { key: 'base', label: '准底图' },
];

const activeStepIdx = computed(() => {
  switch (stage.value) {
    case 'entry': return 0;
    case 'scanning': return 2;
    case 'draft': return 3;
    case 'llm': return 3;
    case 'manual': return confirmedBaseGraph.value ? 5 : 4;
    default: return 0;
  }
});

function confColor(c: number) {
  if (c >= 85) return '#10b981';
  if (c >= 70) return '#f59e0b';
  return '#f43f5e';
}

function nodeTypeLabel(t: string) {
  return (
    { SYSTEM: '系统', DATABASE: '数据库', TABLE: '表', DEPARTMENT: '部门', ROLE: '角色/岗位', ACTION: '业务动作' } as Record<string, string>
  )[t] ?? t;
}

const manualTypeLabel: Record<string, string> = {
  单据: 'info', 动作: 'accent', 系统: 'neutral',
};
</script>

<template>
  <div class="graph-source-page">
    <!-- 顶栏：标题 + 流程步骤条 -->
    <div class="gs-header">
      <div class="gs-heading">
        <div class="gs-title">图来源</div>
        <div class="gs-subtitle">自动 / 半自动分析出图 · 三条路线 + 主动分流 + 人工校正拍板</div>
      </div>
      <div class="gs-steps">
        <div
          v-for="(s, i) in steps" :key="s.key"
          class="gs-step" :class="{ 'gs-step--active': i === activeStepIdx, 'gs-step--done': i < activeStepIdx }"
        >
          <span class="gs-step-idx">{{ i + 1 }}</span>{{ s.label }}
        </div>
      </div>
    </div>

    <!-- ═══ 入口：三条路线选择（15.2） ═══ -->
    <div v-if="stage === 'entry'" class="gs-body gs-body--entry">
      <div class="gs-route-grid">
        <div
          v-for="r in ROUTES_META" :key="r.key"
          class="card gs-route-card reveal" :style="{ '--route-accent': r.accent }"
        >
          <div class="gs-route-top">
            <span class="gs-route-tag" :class="r.tagClass">{{ r.tag }}</span>
            <span class="gs-route-icon"><Icon :name="r.icon" :size="22" /></span>
          </div>
          <div class="gs-route-name">{{ r.name }}</div>
          <div class="gs-route-desc">{{ r.desc }}</div>
          <div class="gs-route-material">{{ r.material }}</div>
          <button class="btn btn-primary gs-route-btn" @click="startRoute(r.key)">开始{{ r.name }}</button>
        </div>
      </div>

      <div class="gs-flow-note">
        <div class="gs-flow-title"><Icon name="flow" :size="15" /> 出图流程</div>
        <div class="gs-flow-line">图来源入口 → 选路线 → 引擎出草稿 → 主动分流（够用 / 加大模型 / 作废重来）→ 人工校正（唯一拍板）→ 准底图</div>
        <div class="gs-flow-hint">原则：主动权在人不在系统——系统只清楚呈现草稿，不做自检/裁决。</div>
      </div>
    </div>

    <!-- ═══ 扫描中 ═══ -->
    <div v-else-if="stage === 'scanning'" class="gs-body gs-body--center">
      <div class="gs-scanning">
        <div class="gs-spinner" />
        <div class="gs-scanning-text">{{ scanningText }}</div>
        <button class="btn btn-ghost btn-sm" @click="discardAll">取消</button>
      </div>
    </div>

    <!-- ═══ 草稿 / 大模型细化：左清单 + 右画布 + 底部主动分流（15.4） ═══ -->
    <div v-else-if="isDraftStage" class="gs-body gs-body--draft">
      <!-- 左：候选清单 / 语义补全清单 -->
      <aside class="gs-left">
        <template v-if="!isLlmRefined">
          <div class="gs-left-header">
            <div class="gs-left-title">引擎扫描 · 候选单据</div>
            <span class="tag tag--accent gs-left-count">{{ engineCandidates.length }} 项</span>
          </div>
          <div class="gs-left-sub">识别置信度（单号/状态/时间/引用链/主子表命中）</div>
          <div v-if="engineError" class="gs-left-warn">
            <Icon name="alert" :size="13" />{{ engineError }}（已回退演示数据）
          </div>
          <div class="gs-cand-list">
            <div v-for="c in engineCandidates" :key="c.table" class="gs-cand">
              <div class="gs-cand-row">
                <span class="gs-cand-name">{{ c.name }}</span>
                <span class="mono gs-cand-table">{{ c.table }}</span>
              </div>
              <div class="gs-cand-bar">
                <div class="gs-cand-bar-fill" :style="{ width: c.confidence + '%', background: confColor(c.confidence) }" />
              </div>
              <div class="gs-cand-foot">
                <span class="gs-cand-conf" :style="{ color: confColor(c.confidence) }">{{ c.confidence }}%</span>
                <span v-if="c.low" class="tag tag--danger gs-cand-low">低置信</span>
                <span class="gs-cand-marks">{{ c.marks.join(' · ') }}</span>
              </div>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="gs-left-header">
            <div class="gs-left-title">大模型语义补全</div>
            <span class="tag tag--info gs-left-count">{{ LLM_REFINEMENTS.length }} 项</span>
          </div>
          <div class="gs-left-sub">在引擎骨架之上补全业务语义（可回退到纯引擎草稿）</div>
          <div class="gs-llm-list">
            <div v-for="(r, idx) in LLM_REFINEMENTS" :key="idx" class="gs-llm-item">
              <span class="tag tag--plain gs-llm-type">{{ r.type }}</span>
              <div class="gs-llm-text">{{ r.text }}</div>
            </div>
          </div>
          <div class="gs-left-note">
            <Icon name="refresh" :size="13" />
            不满意？可一键回到纯引擎草稿
          </div>
        </template>
      </aside>

      <!-- 右：画布 + 主动分流 -->
      <div class="gs-main">
        <div class="gs-canvas-head">
          <div class="gs-canvas-title">{{ draftTitle }}</div>
          <div class="gs-canvas-sub">{{ draftSummary }}</div>
          <div class="gs-canvas-meta">
            <span v-if="engineConnectorName && !isLlmRefined" class="tag tag--plain">{{ engineConnectorName }}</span>
            <span class="tag" :class="isLlmRefined ? 'tag--info' : 'tag--accent'">
              {{ isLlmRefined ? '引擎 + 大模型' : '纯引擎' }}
            </span>
          </div>
        </div>
        <div class="gs-canvas">
          <GraphCanvas
            :nodes="draftNodes" :edges="draftEdges"
            :routes="[]" :active-route-id="null" layout-type="network"
          />
          <div class="gs-legend">
            <div class="gs-legend-title">图例</div>
            <div class="gs-legend-item" v-for="k in ['DATABASE', 'TABLE', 'SYSTEM', 'DEPARTMENT', 'ROLE', 'ACTION']" :key="k">
              <span class="gs-legend-dot" :style="{ background: { DATABASE: '#8b5cf6', TABLE: '#8b5cf6', SYSTEM: '#3b82f6', DEPARTMENT: '#06b6d4', ROLE: '#06b6d4', ACTION: '#f59e0b' }[k] }" />
              {{ nodeTypeLabel(k) }}
            </div>
          </div>
        </div>

        <!-- 主动分流（15.4）：够用 / 加大模型 / 作废重来 -->
        <div class="gs-actions">
          <div class="gs-actions-hint">
            <Icon name="target" :size="14" />
            {{ isLlmRefined ? '大模型细化已叠加，主动权仍在你：' : '草稿已就绪，主动权在你：' }}
          </div>
          <div class="gs-actions-btns">
            <template v-if="!isLlmRefined">
              <button class="btn btn-primary" @click="goManual">
                <Icon name="check" :size="15" />草稿够用 · 直接人工校正
              </button>
              <button class="btn btn-outline" @click="refineWithLlm">
                <Icon name="link" :size="15" />加大模型细化
              </button>
              <button class="btn btn-danger" @click="discardAll">
                <Icon name="refresh" :size="15" />作废重来
              </button>
            </template>
            <template v-else>
              <button class="btn btn-primary" @click="goManual">
                <Icon name="check" :size="15" />细化后够用 · 进人工校正
              </button>
              <button class="btn btn-ghost" @click="revertToEngine">
                <Icon name="refresh" :size="15" />回到纯引擎草稿
              </button>
              <button class="btn btn-danger" @click="discardAll">
                <Icon name="refresh" :size="15" />作废重来
              </button>
            </template>
          </div>
        </div>
      </div>
    </div>

    <!-- ═══ 人工校正 / 人工创建（15.6 唯一拍板） ═══ -->
    <div v-else-if="stage === 'manual'" class="gs-body gs-body--center">
      <div class="gs-manual card">
        <template v-if="isManualRoute">
          <div class="gs-manual-title">人工创建</div>
          <div class="gs-manual-desc">从零手工建模。G2 原型阶段先展示入口；G3/G5 将接入建模 CRUD 与校正记录。</div>
          <div class="gs-manual-placeholder">
            <Icon name="users" :size="40" />
            <div>拖入节点 / 绘制关系</div>
          </div>
          <button class="btn btn-ghost" @click="resetAll">返回入口</button>
        </template>

        <template v-else>
          <div class="gs-manual-title">人工校正 <span class="gs-manual-route-tag">{{ isLlmRefined ? '引擎 + 大模型草稿' : '引擎草稿' }}</span></div>
          <div class="gs-manual-desc">所有路线统一在此拍板——这是唯一裁决环节。逐项确认后生成准底图。</div>

          <div v-if="confirmedBaseGraph" class="gs-manual-ok">
            <Icon name="check" :size="18" />已确认准底图，正在返回入口…
          </div>

          <template v-else>
            <div class="gs-manual-list">
              <div v-for="(n, idx) in MANUAL_NODES" :key="idx" class="gs-manual-item">
                <span class="tag" :class="'tag--' + (manualTypeLabel[n.type] ?? 'neutral')">{{ n.type }}</span>
                <div class="gs-manual-item-body">
                  <div class="gs-manual-item-name">{{ n.name }}</div>
                  <div class="gs-manual-item-note">{{ n.note }}</div>
                </div>
                <Icon name="check" :size="16" class="gs-manual-item-ok" />
              </div>
            </div>
            <div class="gs-manual-actions">
              <button class="btn btn-primary" @click="confirmBaseGraph">
                <Icon name="check" :size="15" />确认并生成准底图
              </button>
              <button class="btn btn-ghost" @click="revertToEngine">
                <Icon name="refresh" :size="15" />返回草稿
              </button>
              <button class="btn btn-danger" @click="discardAll">
                <Icon name="refresh" :size="15" />作废重来
              </button>
            </div>
          </template>
        </template>
      </div>
    </div>

    <!-- 轻提示 -->
    <transition name="gs-toast">
      <div v-if="toast" class="gs-toast"><Icon name="check" :size="15" />{{ toast }}</div>
    </transition>
  </div>
</template>

<style scoped>
.graph-source-page {
  display: flex; flex-direction: column;
  height: calc(100vh - var(--topbar-h));
  background: var(--bg);
}

/* —— 顶栏 —— */
.gs-header {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 14px 24px; background: var(--surface);
  border-bottom: 1px solid var(--border); flex-shrink: 0; flex-wrap: wrap;
}
.gs-title { font-size: 18px; font-weight: 700; letter-spacing: -0.01em; }
.gs-subtitle { font-size: 12.5px; color: var(--fg-muted); margin-top: 2px; }
.gs-steps { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
.gs-step {
  display: flex; align-items: center; gap: 6px;
  font-size: 11.5px; color: var(--fg-faint); padding: 4px 10px;
  border-radius: 999px; white-space: nowrap; transition: all .15s;
}
.gs-step-idx {
  width: 17px; height: 17px; border-radius: 50%; display: flex; align-items: center; justify-content: center;
  background: var(--surface-3); color: var(--fg-faint); font-size: 10px; font-weight: 700;
}
.gs-step--active { background: var(--accent-soft); color: var(--accent); font-weight: 600; }
.gs-step--active .gs-step-idx { background: var(--accent); color: #fff; }
.gs-step--done { color: var(--fg-muted); }
.gs-step--done .gs-step-idx { background: var(--success-soft); color: var(--success); }

/* —— 主体骨架 —— */
.gs-body { flex: 1; min-height: 0; position: relative; }
.gs-body--center { display: flex; align-items: center; justify-content: center; }
.gs-body--entry { padding: 32px 48px; overflow-y: auto; }

/* —— 入口：三条路线卡片 —— */
.gs-route-grid {
  display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 20px;
  max-width: 1100px; margin: 0 auto;
}
.gs-route-card {
  padding: 22px 22px 20px; display: flex; flex-direction: column; gap: 8px;
  border-top: 3px solid var(--route-accent); position: relative;
}
.gs-route-top { display: flex; align-items: center; justify-content: space-between; }
.gs-route-tag {
  height: 20px; padding: 0 8px; border-radius: 999px; font-size: 11px; font-weight: 700;
  display: inline-flex; align-items: center;
}
.gs-route-tag--engine { background: var(--accent-soft); color: var(--accent); }
.gs-route-tag--llm { background: rgba(139, 92, 246, .14); color: #8b5cf6; }
.gs-route-tag--manual { background: rgba(6, 182, 212, .14); color: #06b6d4; }
.gs-route-icon {
  width: 38px; height: 38px; border-radius: 10px;
  background: color-mix(in srgb, var(--route-accent) 14%, transparent);
  color: var(--route-accent); display: flex; align-items: center; justify-content: center;
}
.gs-route-name { font-size: 16px; font-weight: 700; margin-top: 2px; }
.gs-route-desc { font-size: 12.5px; color: var(--fg-muted); line-height: 1.65; flex: 1; }
.gs-route-material { font-size: 11.5px; color: var(--fg-faint); }
.gs-route-btn { margin-top: 8px; align-self: flex-start; }

.gs-flow-note {
  max-width: 1100px; margin: 28px auto 0; padding: 16px 20px;
  background: var(--surface); border: 1px dashed var(--border-strong);
  border-radius: var(--radius-lg); color: var(--fg-muted); font-size: 12.5px;
}
.gs-flow-title { display: flex; align-items: center; gap: 6px; font-weight: 600; color: var(--fg); margin-bottom: 6px; }
.gs-flow-line { line-height: 1.7; }
.gs-flow-hint { margin-top: 6px; font-size: 12px; color: var(--fg-faint); }

/* —— 扫描动画 —— */
.gs-scanning { display: flex; flex-direction: column; align-items: center; gap: 18px; }
.gs-spinner {
  width: 46px; height: 46px; border-radius: 50%;
  border: 4px solid var(--border-strong); border-top-color: var(--accent);
  animation: gs-spin 0.9s linear infinite;
}
@keyframes gs-spin { to { transform: rotate(360deg); } }
.gs-scanning-text { max-width: 420px; text-align: center; color: var(--fg-muted); font-size: 13px; line-height: 1.7; }

/* —— 草稿 / 细化：左右分栏 —— */
.gs-body--draft { display: flex; }
.gs-left {
  width: 290px; flex-shrink: 0; background: var(--surface);
  border-right: 1px solid var(--border); padding: 16px; overflow-y: auto;
}
.gs-left-header { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.gs-left-title { font-size: 13.5px; font-weight: 600; }
.gs-left-count { flex-shrink: 0; }
.gs-left-sub { font-size: 11.5px; color: var(--fg-faint); margin: 6px 0 12px; }

.gs-left-warn {
  display: flex; align-items: center; gap: 6px;
  font-size: 11.5px; color: var(--warning, #f59e0b);
  background: rgba(245, 158, 11, .08); border: 1px solid rgba(245, 158, 11, .25);
  border-radius: var(--radius-sm); padding: 6px 9px; margin-bottom: 10px; line-height: 1.5;
}
.gs-cand-list { display: flex; flex-direction: column; gap: 12px; }
.gs-cand { padding: 10px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface-2); }
.gs-cand-row { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.gs-cand-name { font-size: 12.5px; font-weight: 600; }
.gs-cand-table { font-size: 11px; color: var(--fg-faint); }
.gs-cand-bar { height: 5px; border-radius: 999px; background: var(--border-strong); margin: 7px 0 5px; overflow: hidden; }
.gs-cand-bar-fill { height: 100%; border-radius: 999px; transition: width .4s ease; }
.gs-cand-foot { display: flex; align-items: center; gap: 8px; }
.gs-cand-conf { font-size: 11.5px; font-weight: 700; }
.gs-cand-low { height: 18px; font-size: 10px; padding: 0 6px; }
.gs-cand-marks { font-size: 10.5px; color: var(--fg-faint); }

.gs-llm-list { display: flex; flex-direction: column; gap: 8px; }
.gs-llm-item { padding: 9px 11px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface-2); }
.gs-llm-type {
  display: inline-flex; height: 18px; padding: 0 7px; border-radius: 5px;
  font-size: 10.5px; font-weight: 700; color: #8b5cf6; background: rgba(139, 92, 246, .13); margin-bottom: 4px;
}
.gs-llm-text { font-size: 12px; line-height: 1.6; color: var(--fg); }
.gs-left-note {
  display: flex; align-items: center; gap: 6px; margin-top: 14px;
  font-size: 11.5px; color: var(--fg-faint);
}

.gs-main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.gs-canvas-head {
  display: flex; align-items: center; gap: 10px; padding: 8px 16px;
  border-bottom: 1px solid var(--border); background: var(--surface); flex-shrink: 0;
}
.gs-canvas-title { font-size: 13px; font-weight: 600; }
.gs-canvas-sub { font-size: 11.5px; color: var(--fg-faint); }
.gs-canvas-meta { margin-left: auto; }
.gs-canvas {
  position: relative; flex: 1; min-height: 0;
  background: var(--canvas-bg);
  background-image: radial-gradient(rgba(117, 131, 164, .09) 1px, transparent 1px);
  background-size: 22px 22px;
}

.gs-legend {
  position: absolute; left: 14px; bottom: 14px; z-index: 4;
  background: rgba(13, 20, 36, .9); border: 1px solid var(--canvas-border);
  border-radius: var(--radius); padding: 9px 11px; font-size: 11.5px; color: var(--canvas-fg);
  box-shadow: var(--shadow-canvas);
}
.gs-legend-title { font-size: 10.5px; color: var(--canvas-fg-dim); letter-spacing: .06em; margin-bottom: 5px; }
.gs-legend-item { display: flex; align-items: center; gap: 7px; margin-bottom: 2px; }
.gs-legend-dot { width: 9px; height: 9px; border-radius: 50%; flex-shrink: 0; }

/* —— 主动分流（15.4） —— */
.gs-actions {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  padding: 10px 16px; background: var(--surface);
  border-top: 1px solid var(--border); flex-shrink: 0; flex-wrap: wrap;
}
.gs-actions-hint { display: flex; align-items: center; gap: 6px; font-size: 12px; color: var(--fg-muted); }
.gs-actions-btns { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* —— 人工校正 / 人工创建 —— */
.gs-manual { width: 560px; max-width: calc(100vw - 64px); padding: 26px 28px; }
.gs-manual-title { font-size: 16px; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.gs-manual-route-tag {
  height: 20px; padding: 0 9px; border-radius: 999px; font-size: 11px; font-weight: 600;
  background: var(--accent-soft); color: var(--accent); display: inline-flex; align-items: center;
}
.gs-manual-desc { font-size: 12.5px; color: var(--fg-muted); margin: 6px 0 16px; line-height: 1.7; }
.gs-manual-list { display: flex; flex-direction: column; gap: 8px; margin-bottom: 18px; max-height: 320px; overflow-y: auto; }
.gs-manual-item {
  display: flex; align-items: center; gap: 12px;
  padding: 10px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface-2);
}
.gs-manual-item-body { flex: 1; min-width: 0; }
.gs-manual-item-name { font-size: 13px; font-weight: 600; }
.gs-manual-item-note { font-size: 11.5px; color: var(--fg-faint); }
.gs-manual-item-ok { color: var(--success); flex-shrink: 0; }
.gs-manual-actions { display: flex; gap: 8px; flex-wrap: wrap; }
.gs-manual-ok {
  display: flex; align-items: center; gap: 8px; padding: 16px;
  border-radius: var(--radius); background: var(--success-soft); color: var(--success);
  font-weight: 600; margin-bottom: 16px;
}
.gs-manual-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px;
  height: 220px; border: 2px dashed var(--border-strong); border-radius: var(--radius-lg);
  color: var(--fg-faint); margin: 14px 0 18px;
}

/* —— 轻提示 —— */
.gs-toast {
  position: fixed; bottom: 28px; left: 50%; transform: translateX(-50%); z-index: 50;
  display: flex; align-items: center; gap: 8px;
  padding: 10px 18px; border-radius: 999px;
  background: var(--accent); color: #fff; font-size: 13px; font-weight: 500;
  box-shadow: 0 6px 20px rgba(37, 99, 235, .4);
}
.gs-toast-enter-active, .gs-toast-leave-active { transition: opacity .2s, transform .2s; }
.gs-toast-enter-from, .gs-toast-leave-to { opacity: 0; transform: translate(-50%, 8px); }
</style>
