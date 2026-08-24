<script setup lang="ts">
/**
 * 图来源 · 自动/半自动分析出图（G5 校正闭环）
 *
 * 对应文档书第 15 章：
 * - 15.2 三条路线入口：引擎分析（主干）/ 大模型分析（增强）/ 人工创建（兜底）
 * - 15.4 主动分流：引擎出草稿后用户主动选择「够用→人工校正 / 加大模型细化 / 作废重来」
 * - 15.6 流程：图来源入口 → 选路线 → 出草稿 → 主动分流 → 人工校正（唯一拍板）→ 准底图
 *
 * G3 引擎、G4 大模型已接真实后端，G5 接入校正记录与模式库沉淀。
 * 原则：主动权在人不在系统——系统只清楚呈现草稿，不做自检/裁决。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import type {
  CorrectionOperation, CorrectionPayload, CorrectionRecord, CorrectionTargetType,
  EngineCandidate, EngineDraft, EngineFlow, EngineRefineResult, GraphEdge, GraphNode,
  PatternPayload, RefinementItem,
} from '@/types';
import {
  fetchEngineAnalyze, fetchConnectors, postEngineRefine,
  submitCorrection, listCorrections, confirmCorrection, createPattern,
} from '@/api';
import GraphCanvas from '@/components/GraphCanvas.vue';
import Icon from '@/components/Icon.vue';

type RouteKey = 'engine' | 'llm' | 'manual';
type Stage = 'entry' | 'scanning' | 'draft' | 'llm' | 'manual';
type CorrectionTab = 'pending' | 'history';

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

/* —— G3 真实引擎分析草稿 —— */
const engineData = ref<EngineDraft | null>(null);
const engineCandidates = ref<EngineCandidate[]>([]);
const engineError = ref('');
const engineConnectorName = ref('');
const engineConnectorId = ref('');

/* —— G4 大模型细化结果 —— */
const refineResult = ref<EngineRefineResult | null>(null);
const refineLoading = ref(false);
const refineError = ref('');

/* —— G5 校正闭环 —— */
const correctionTab = ref<CorrectionTab>('pending');
const stagedCorrections = ref<CorrectionPayload[]>([]);
const correctionHistory = ref<CorrectionRecord[]>([]);
const correctionLoading = ref(false);
const saveAsPattern = ref(false);
const patternType = ref('NODE_NAME');
const patternName = ref('');
const patternDescription = ref('');
const selectedForHistory = ref<{ targetType: CorrectionTargetType; targetId: string }>({ targetType: 'NODE', targetId: '' });
const renameInput = ref<Record<string, string>>({});
const mergeInput = ref<Record<string, string>>({});
const reorderInput = ref<Record<string, string>>({});

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

const REFINE_TYPE_LABEL: Record<string, string> = {
  rename: '改名', chain: '动作链', party: '参与方', relation: '关系', flow: '流程',
  noop: '兜底', error: '异常',
};

function refineTypeLabel(t: string) {
  return REFINE_TYPE_LABEL[t] ?? t;
}

/**
 * 引擎分析（G3 真实数据接入）：
 * 优先取已启用的 DB 连接器调用 /api/analyze；失败时展示错误空态，不再默认回退假数据。
 */
async function acquireEngineDraft() {
  engineError.value = '';
  try {
    const { records } = await fetchConnectors(1, 50);
    const dbConn = records.find((c) => c.connectorType === 'DB' && c.enabled === 1);
    if (!dbConn) throw new Error('未找到已启用的 DB 连接器');
    const draft = await fetchEngineAnalyze(dbConn.id);
    engineData.value = draft;
    engineCandidates.value = draft.candidates ?? [];
    engineConnectorName.value = dbConn.name;
    engineConnectorId.value = dbConn.id;
  } catch (err) {
    console.warn('[G3] 引擎分析调用失败：', err);
    engineData.value = null;
    engineCandidates.value = [];
    engineError.value = err instanceof Error ? err.message : String(err);
  }
}

function startRoute(key: RouteKey) {
  routeKey.value = key;
  confirmedBaseGraph.value = false;
  if (key === 'manual') {
    stage.value = 'manual';
    return;
  }
  stage.value = 'scanning';
  scanningText.value = SCANNING_TEXT[key];
  if (scanTimer !== null) window.clearTimeout(scanTimer);
  scanTimer = window.setTimeout(() => {
    if (key === 'llm') {
      void startLlmRoute();
      return;
    }
    void acquireEngineDraft().then(() => {
      stage.value = 'draft';
    });
  }, 1200);
}

/** 大模型路线：先确保有引擎草稿，再调真实细化接口 */
async function startLlmRoute() {
  if (engineData.value === null) {
    await acquireEngineDraft();
  }
  if (!engineConnectorId.value) {
    refineError.value = '未找到已启用的 DB 连接器，无法调用大模型细化';
    stage.value = engineData.value ? 'draft' : 'entry';
    return;
  }
  await refineWithLlm();
}

/** 主动分流 ①：草稿够用 → 直接人工校正 */
function goManual() {
  stage.value = 'manual';
  void refreshCorrectionHistory();
}

/** 主动分流 ②：草稿不够 → 加大模型细化（G4 真实接口） */
async function refineWithLlm() {
  refineError.value = '';
  if (!engineConnectorId.value) {
    refineError.value = '未找到已启用的 DB 连接器，无法调用大模型细化';
    return;
  }
  refineLoading.value = true;
  try {
    refineResult.value = await postEngineRefine(engineConnectorId.value);
    stage.value = 'llm';
  } catch (err) {
    console.warn('[G4] 大模型细化调用失败：', err);
    refineError.value = err instanceof Error ? err.message : String(err);
  } finally {
    refineLoading.value = false;
  }
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
  engineData.value = null;
  engineCandidates.value = [];
  engineError.value = '';
  engineConnectorName.value = '';
  engineConnectorId.value = '';
  refineResult.value = null;
  refineError.value = '';
  refineLoading.value = false;
  stagedCorrections.value = [];
  correctionHistory.value = [];
  correctionTab.value = 'pending';
  saveAsPattern.value = false;
  patternType.value = 'NODE_NAME';
  patternName.value = '';
  patternDescription.value = '';
  renameInput.value = {};
  mergeInput.value = {};
  reorderInput.value = {};
}

/** 人工校正拍板：确认准底图 */
function confirmBaseGraph() {
  confirmedBaseGraph.value = true;
  toast.value = '已确认准底图，流程骨架将写入平台关系网（G5 校正闭环已保留本次校正记录）';
  window.setTimeout(() => {
    resetAll();
  }, 1800);
}

onMounted(() => {
  void acquireEngineDraft();
});

onBeforeUnmount(() => {
  if (scanTimer !== null) window.clearTimeout(scanTimer);
});

/* —— 派生数据 —— */
const isDraftStage = computed(() => stage.value === 'draft' || stage.value === 'llm');
const engineBaseNodes = computed<GraphNode[]>(() => engineData.value?.draftNodes ?? []);
const engineBaseEdges = computed<GraphEdge[]>(() => engineData.value?.draftEdges ?? []);

function applyRename(nodes: GraphNode[], rename: Record<string, string>): GraphNode[] {
  return nodes.map((n) => {
    const newName = (n.code && rename[n.code]) || rename[n.id] || rename[n.name];
    return newName ? { ...n, name: newName } : n;
  });
}

const draftNodes = computed<GraphNode[]>(() => {
  if (stage.value !== 'llm' || !refineResult.value) return engineBaseNodes.value;
  const renamed = applyRename(engineBaseNodes.value, refineResult.value.renameMap ?? {});
  return [...renamed, ...(refineResult.value.addedNodes ?? [])];
});

const draftEdges = computed<GraphEdge[]>(() => {
  if (stage.value !== 'llm' || !refineResult.value) return engineBaseEdges.value;
  return [...engineBaseEdges.value, ...(refineResult.value.addedEdges ?? [])];
});

const llmRefinements = computed<RefinementItem[]>(() => refineResult.value?.refinements ?? []);
const isNoopProvider = computed(() => refineResult.value?.provider === 'noop');
const isLlmRefined = computed(() => stage.value === 'llm');
const isManualRoute = computed(() => routeKey.value === 'manual');
const draftTitle = computed(() => (isLlmRefined.value ? '大模型细化草稿' : '引擎分析草稿'));
const draftSummary = computed(() => {
  const n = draftNodes.value.length;
  const e = draftEdges.value.length;
  if (isLlmRefined.value) {
    const tag = isNoopProvider.value ? '引擎原稿（未配置大模型）' : '引擎骨架 + 大模型语义补全';
    return `${tag} · ${n} 节点 / ${e} 关系`;
  }
  const src = engineData.value ? `扫描 ${engineData.value.database} · ${engineCandidates.value.length} 候选单据` : '等待引擎扫描';
  return `${src} · ${n} 节点 / ${e} 关系`;
});

const draftFlows = computed<EngineFlow[]>(() => {
  if (stage.value === 'llm' && refineResult.value) {
    return refineResult.value.base.flows ?? [];
  }
  return engineData.value?.flows ?? [];
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

function nodeName(id: string): string {
  const n = draftNodes.value.find((x) => x.id === id);
  return n?.name ?? id;
}

/* —— G5 校正操作 —— */
function stageCorrection(payload: CorrectionPayload) {
  stagedCorrections.value.push(payload);
}

function removeStaged(idx: number) {
  stagedCorrections.value.splice(idx, 1);
}

async function submitStagedCorrections() {
  if (!stagedCorrections.value.length) return;
  correctionLoading.value = true;
  try {
    for (const c of stagedCorrections.value) {
      await submitCorrection(c);
      if (saveAsPattern.value) {
        await createPattern({
          patternType: c.patternType || 'NODE_NAME',
          patternKey: `${c.targetType}:${c.targetName}`,
          patternValue: c.newValue,
          sourceType: c.targetType,
          sourceId: c.targetId,
          sourceOperation: c.operation,
        });
      }
    }
    toast.value = `已提交 ${stagedCorrections.value.length} 条校正记录`;
    stagedCorrections.value = [];
    await refreshCorrectionHistory();
  } catch (err) {
    toast.value = err instanceof Error ? err.message : '提交失败';
  } finally {
    correctionLoading.value = false;
  }
}

async function refreshCorrectionHistory() {
  const s = selectedForHistory.value;
  if (!s) return;
  try {
    correctionHistory.value = await listCorrections({ targetType: s.targetType, targetId: s.targetId });
  } catch {
    correctionHistory.value = [];
  }
}

async function doConfirmCorrection(id: number) {
  try {
    await confirmCorrection(id);
    toast.value = '校正已生效';
    await refreshCorrectionHistory();
  } catch (err) {
    toast.value = err instanceof Error ? err.message : '确认失败';
  }
}

function selectHistory(targetType: CorrectionTargetType, targetId: string) {
  selectedForHistory.value = { targetType, targetId };
  correctionTab.value = 'history';
  void refreshCorrectionHistory();
}

function nodeCorrectionPayload(n: GraphNode): CorrectionPayload {
  return {
    targetType: 'NODE',
    targetId: n.id,
    targetName: n.name,
    operation: 'CONFIRM',
  };
}

function edgeCorrectionPayload(e: GraphEdge): CorrectionPayload {
  return {
    targetType: 'EDGE',
    targetId: e.id,
    targetName: `${nodeName(e.source)} → ${nodeName(e.target)}`,
    operation: 'CONFIRM',
  };
}

function routeCorrectionPayload(f: EngineFlow): CorrectionPayload {
  return {
    targetType: 'ROUTE',
    targetId: f.name,
    targetName: f.name,
    operation: 'CONFIRM',
  };
}

function doRenameNode(n: GraphNode) {
  const v = renameInput.value[n.id];
  if (!v || v === n.name) return;
  stageCorrection({
    targetType: 'NODE',
    targetId: n.id,
    targetName: n.name,
    operation: 'RENAME',
    oldValue: n.name,
    newValue: v,
    patternType: 'NODE_NAME',
    patternName: patternName.value || `${n.name} → ${v}`,
    patternDescription: patternDescription.value,
  });
  renameInput.value[n.id] = '';
}

function doMergeNode(n: GraphNode) {
  const v = mergeInput.value[n.id];
  if (!v) return;
  stageCorrection({
    targetType: 'NODE',
    targetId: n.id,
    targetName: n.name,
    operation: 'MERGE',
    mergeTargetId: v,
  });
  mergeInput.value[n.id] = '';
}

function doReorderRoute(f: EngineFlow) {
  const v = reorderInput.value[f.name];
  if (!v) return;
  stageCorrection({
    targetType: 'ROUTE',
    targetId: f.name,
    targetName: f.name,
    operation: 'REORDER',
    reorderNodeIds: v.split(',').map((s) => s.trim()).filter(Boolean),
  });
  reorderInput.value[f.name] = '';
}

const manualTypeLabel: Record<string, string> = {
  单据: 'info', 动作: 'accent', 系统: 'neutral',
};

const nodesForCorrection = computed(() => draftNodes.value.filter((n) => n.nodeType === 'TABLE' || n.nodeType === 'SYSTEM' || n.nodeType === 'ACTION'));
const edgesForCorrection = computed(() => draftEdges.value);

function correctionLabel(op: CorrectionOperation): string {
  return { RENAME: '改名', CONFIRM: '确认', MERGE: '合并', ADD: '新增', DELETE: '删除', REORDER: '排序' }[op] ?? op;
}

function stagedText(c: CorrectionPayload): string {
  let s = `[${correctionLabel(c.operation)}] ${c.targetName}`;
  if (c.newValue) s += ` → ${c.newValue}`;
  if (c.mergeTargetId) s += ` 合并到 ${nodeName(c.mergeTargetId)}`;
  if (c.reorderNodeIds?.length) s += ` 重排节点`;
  return s;
}
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
            <Icon name="alert" :size="13" />{{ engineError }}
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
            <span class="tag tag--info gs-left-count">{{ llmRefinements.length }} 项</span>
          </div>
          <div class="gs-left-sub">在引擎骨架之上补全业务语义（可回退到纯引擎草稿）</div>
          <div v-if="isNoopProvider" class="gs-left-warn">
            <Icon name="alert" :size="13" />未配置大模型 API Key，当前为引擎原稿（Noop 兜底）。配置 LLM_API_KEY 环境变量后重试。
          </div>
          <div class="gs-llm-list">
            <div v-for="(r, idx) in llmRefinements" :key="idx" class="gs-llm-item">
              <span class="tag tag--plain gs-llm-type">{{ refineTypeLabel(r.type) }}</span>
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
              {{ isLlmRefined ? (isNoopProvider ? '引擎原稿（Noop）' : '引擎 + 大模型') : '纯引擎' }}
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

        <!-- 主动分流（15.4） -->
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
              <button class="btn btn-outline" :disabled="refineLoading" @click="refineWithLlm">
                <Icon name="link" :size="15" />{{ refineLoading ? '大模型细化中…' : '加大模型细化' }}
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
    <div v-else-if="stage === 'manual'" class="gs-body gs-body--manual">
      <div class="gs-manual-main">
        <div class="gs-manual-canvas card">
          <div class="gs-canvas-head">
            <div class="gs-canvas-title">人工校正 · {{ isLlmRefined ? '引擎 + 大模型草稿' : '引擎草稿' }}</div>
            <div class="gs-canvas-sub">{{ draftSummary }}</div>
            <div class="gs-canvas-meta">
              <button class="btn btn-sm btn-ghost" @click="revertToEngine">
                <Icon name="refresh" :size="13" />返回草稿
              </button>
              <button class="btn btn-sm btn-danger" @click="discardAll">作废重来</button>
            </div>
          </div>
          <div class="gs-canvas gs-canvas--manual">
            <GraphCanvas
              :nodes="draftNodes" :edges="draftEdges"
              :routes="[]" :active-route-id="null" layout-type="network"
            />
          </div>
          <div class="gs-actions">
            <div class="gs-actions-hint"><Icon name="target" :size="14" /> 右侧逐项校正后，确认生成准底图</div>
            <div class="gs-actions-btns">
              <button class="btn btn-primary" :disabled="correctionLoading" @click="confirmBaseGraph">
                <Icon name="check" :size="15" />确认并生成准底图
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- G5 校正面板 -->
      <aside class="gs-right">
        <div class="gs-right-tabs">
          <button :class="{ active: correctionTab === 'pending' }" @click="correctionTab = 'pending'">待校正</button>
          <button :class="{ active: correctionTab === 'history' }" @click="correctionTab = 'history'">历史</button>
        </div>

        <div v-if="correctionTab === 'pending'" class="gs-correction-body">
          <div class="gs-correct-section">
            <div class="gs-correct-title">节点 {{ nodesForCorrection.length }}</div>
            <div v-for="n in nodesForCorrection" :key="n.id" class="gs-correct-item">
              <div class="gs-correct-row">
                <span class="tag tag--neutral tag--plain">{{ nodeTypeLabel(n.nodeType) }}</span>
                <span class="gs-correct-name">{{ n.name }}</span>
              </div>
              <div class="gs-correct-actions">
                <input
                  v-model="renameInput[n.id]" class="input input-sm gs-correct-input"
                  placeholder="新名称" @keyup.enter="doRenameNode(n)"
                />
                <button class="btn btn-sm btn-ghost" @click="doRenameNode(n)"><Icon name="edit" :size="13" />改名</button>
                <button class="btn btn-sm btn-ghost" @click="stageCorrection(nodeCorrectionPayload(n))"><Icon name="check" :size="13" />确认</button>
                <input
                  v-model="mergeInput[n.id]" class="input input-sm gs-correct-input"
                  placeholder="合并目标 id" @keyup.enter="doMergeNode(n)"
                />
                <button class="btn btn-sm btn-ghost" @click="doMergeNode(n)"><Icon name="merge" :size="13" />合并</button>
                <button class="btn btn-sm btn-danger" @click="stageCorrection({ ...nodeCorrectionPayload(n), operation: 'DELETE' })"><Icon name="trash" :size="13" />删除</button>
              </div>
            </div>
          </div>

          <div class="gs-correct-section">
            <div class="gs-correct-title">关系 {{ edgesForCorrection.length }}</div>
            <div v-for="e in edgesForCorrection" :key="e.id" class="gs-correct-item">
              <span class="gs-correct-name">{{ nodeName(e.source) }} → {{ nodeName(e.target) }}</span>
              <div class="gs-correct-actions">
                <button class="btn btn-sm btn-ghost" @click="stageCorrection(edgeCorrectionPayload(e))"><Icon name="check" :size="13" />确认</button>
                <button class="btn btn-sm btn-danger" @click="stageCorrection({ ...edgeCorrectionPayload(e), operation: 'DELETE' })"><Icon name="trash" :size="13" />删除</button>
              </div>
            </div>
          </div>

          <div class="gs-correct-section">
            <div class="gs-correct-title">路线 {{ draftFlows.length }}</div>
            <div v-for="f in draftFlows" :key="f.name" class="gs-correct-item">
              <span class="gs-correct-name">{{ f.name }}</span>
              <div class="gs-correct-actions">
                <button class="btn btn-sm btn-ghost" @click="stageCorrection(routeCorrectionPayload(f))"><Icon name="check" :size="13" />确认</button>
                <input
                  v-model="reorderInput[f.name]" class="input input-sm gs-correct-input"
                  placeholder="节点 id 顺序，逗号分隔" @keyup.enter="doReorderRoute(f)"
                />
                <button class="btn btn-sm btn-ghost" @click="doReorderRoute(f)"><Icon name="sort" :size="13" />排序</button>
                <button class="btn btn-sm btn-danger" @click="stageCorrection({ ...routeCorrectionPayload(f), operation: 'DELETE' })"><Icon name="trash" :size="13" />删除</button>
              </div>
            </div>
          </div>

          <div class="gs-pattern-opts">
            <label class="gs-pattern-check">
              <input v-model="saveAsPattern" type="checkbox" />
              <span>命中/确认后沉淀为模式</span>
            </label>
            <div v-if="saveAsPattern" class="gs-pattern-fields">
              <select v-model="patternType" class="input input-sm">
                <option value="NODE_NAME">节点名</option>
                <option value="EDGE_NAME">关系名</option>
                <option value="ROUTE_TEMPLATE">路线模板</option>
              </select>
              <input v-model="patternName" class="input input-sm" placeholder="模式名称（可选）" />
              <input v-model="patternDescription" class="input input-sm" placeholder="模式描述（可选）" />
            </div>
          </div>

          <div v-if="stagedCorrections.length" class="gs-staged-list">
            <div class="gs-correct-title">待提交 {{ stagedCorrections.length }}</div>
            <div v-for="(c, idx) in stagedCorrections" :key="idx" class="gs-staged-item">
              <span class="gs-staged-text">{{ stagedText(c) }}</span>
              <button class="btn btn-xs btn-ghost" @click="removeStaged(idx)">移除</button>
            </div>
          </div>

          <button
            class="btn btn-primary btn-block" :disabled="!stagedCorrections.length || correctionLoading"
            @click="submitStagedCorrections"
          >
            <Icon name="check" :size="14" />提交 {{ stagedCorrections.length }} 项校正
          </button>
        </div>

        <div v-else class="gs-correction-body">
          <div class="gs-history-target">
            <select v-model="selectedForHistory.targetType" class="input input-sm">
              <option value="NODE">节点</option>
              <option value="EDGE">关系</option>
              <option value="ROUTE">路线</option>
            </select>
            <input v-model="selectedForHistory.targetId" class="input input-sm" placeholder="对象 id" />
            <button class="btn btn-sm btn-primary" @click="refreshCorrectionHistory">查询</button>
          </div>
          <div v-if="!correctionHistory.length" class="empty">
            <div class="empty-title">暂无校正历史</div>
          </div>
          <div v-for="h in correctionHistory" :key="h.id" class="gs-history-item">
            <div class="gs-history-head">
              <span class="tag tag--accent tag--plain">{{ correctionLabel(h.operation) }}</span>
              <span class="gs-history-name">{{ h.targetName }}</span>
            </div>
            <div class="gs-history-meta">
              {{ h.status }} · {{ h.operator || 'system' }} · {{ h.createdAt }}
            </div>
            <button v-if="h.status === 'PENDING'" class="btn btn-sm btn-primary" @click="doConfirmCorrection(h.id)">确认生效</button>
          </div>
        </div>
      </aside>
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
.gs-canvas--manual { border-radius: 0; }

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

/* —— 人工校正（G5） —— */
.gs-body--manual { display: flex; }
.gs-manual-main { flex: 1; min-width: 0; padding: 16px; display: flex; flex-direction: column; }
.gs-manual-canvas { flex: 1; min-height: 0; display: flex; flex-direction: column; padding: 0; overflow: hidden; }

.gs-right {
  width: 340px; flex-shrink: 0; background: var(--surface);
  border-left: 1px solid var(--border); padding: 16px; overflow-y: auto;
}
.gs-right-tabs {
  display: flex; gap: 8px; border-bottom: 1px solid var(--border); margin-bottom: 12px;
}
.gs-right-tabs button {
  padding: 8px 12px; font-size: 12.5px; font-weight: 600; color: var(--fg-muted);
  background: transparent; border: none; cursor: pointer; border-bottom: 2px solid transparent;
}
.gs-right-tabs button.active { color: var(--accent); border-bottom-color: var(--accent); }
.gs-correction-body { display: flex; flex-direction: column; gap: 14px; }
.gs-correct-section { display: flex; flex-direction: column; gap: 8px; }
.gs-correct-title { font-size: 12px; font-weight: 700; color: var(--fg-muted); letter-spacing: .04em; }
.gs-correct-item {
  padding: 10px 11px; border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--surface-2); display: flex; flex-direction: column; gap: 8px;
}
.gs-correct-row { display: flex; align-items: center; gap: 8px; }
.gs-correct-name { font-size: 12.5px; font-weight: 600; }
.gs-correct-actions { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; }
.gs-correct-input { width: 110px; }

.gs-pattern-opts { padding: 10px; border: 1px dashed var(--border-strong); border-radius: var(--radius-sm); }
.gs-pattern-check { display: flex; align-items: center; gap: 8px; font-size: 12px; cursor: pointer; }
.gs-pattern-fields { display: flex; flex-direction: column; gap: 6px; margin-top: 8px; }

.gs-staged-list { padding: 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface-2); }
.gs-staged-item { display: flex; align-items: center; justify-content: space-between; gap: 8px; font-size: 11.5px; padding: 4px 0; }
.gs-staged-text { color: var(--fg-muted); }

.gs-history-target { display: flex; gap: 6px; }
.gs-history-item {
  padding: 10px 11px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--surface-2);
}
.gs-history-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.gs-history-name { font-size: 12.5px; font-weight: 600; }
.gs-history-meta { font-size: 11px; color: var(--fg-faint); margin-bottom: 6px; }

.empty { text-align: center; padding: 24px 0; color: var(--fg-faint); }
.empty-title { font-size: 13px; }

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
