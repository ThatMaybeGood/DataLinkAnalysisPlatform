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
  AnalysisTask, ConnectorTestResult, CorrectionOperation, CorrectionPayload, CorrectionRecord, CorrectionTargetType,
  DataSourceConnector, EngineCandidate, EngineDraft, EngineFlow, EngineRefineResult, GraphEdge,
  GraphNode, LlmConfigInfo, PatternPayload, RefinementItem,
} from '@/types';
import {
  activateLlmConfig, createPattern, confirmCorrection, fetchAnalyzeTask, fetchAnalyzeTasks,
  fetchConnectors, fetchEngineAnalyzeBatch, fetchLlmConfig, fetchLlmConfigs, listCorrections,
  postEngineRefineBatch, submitCorrection, testConnector, testLlmConfig,
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

/* —— 数据来源（DB 连接器，支持多选合并分析） —— */
const sourceConnectors = ref<DataSourceConnector[]>([]);
const selectedSourceIds = ref<string[]>([]);
const sourceKeyword = ref('');
/** 空输入时的默认预览条数（输入关键词后展示全部匹配） */
const SOURCE_PREVIEW_COUNT = 8;
const sourcePickerOpen = ref(false);
const sourceTestingId = ref('');
const sourceTestResults = ref<Record<string, ConnectorTestResult>>({});
const sourceTestError = ref('');

/* —— G3 真实引擎分析草稿 —— */
const engineData = ref<EngineDraft | null>(null);
const engineCandidates = ref<EngineCandidate[]>([]);
const engineError = ref('');
const engineConnectorName = ref('');
const engineConnectorId = ref('');

/* —— 大模型当前配置状态（cc-switch 式多配置切换 + 自动测速门禁） —— */
const llmHasKey = ref(false);
const llmConfigs = ref<LlmConfigInfo[]>([]);
const llmSelectKey = ref('');
const llmTesting = ref(false);
const llmTestError = ref('');

/** v-for key：env「默认配置」id 为 null，统一 'default' 防撞 */
function llmKey(c: LlmConfigInfo): string {
  return c.id != null ? String(c.id) : 'default';
}

function llmName(c: LlmConfigInfo): string {
  return c.id != null ? (c.name || '未命名') : '默认配置（环境变量）';
}

async function loadLlmProvider() {
  try {
    const info = await fetchLlmConfig();
    llmHasKey.value = info.hasKey;
    llmConfigs.value = await fetchLlmConfigs();
    const cur = llmConfigs.value.find((c) => c.isActive)
      ?? llmConfigs.value.find((c) => c.source === 'env');
    llmSelectKey.value = cur ? llmKey(cur) : '';
  } catch {
    llmConfigs.value = [];
    llmSelectKey.value = '';
  }
}

/** 切换「当前大模型」：先自动测速，失败不切换（回退原选择） */
async function onLlmSelect() {
  const target = llmConfigs.value.find((c) => llmKey(c) === llmSelectKey.value);
  if (!target) return;
  const cur = llmConfigs.value.find((c) => c.isActive);
  if (cur && llmKey(cur) === llmKey(target)) return;   // 选的就是当前，跳过
  llmTesting.value = true;
  llmTestError.value = '';
  try {
    const r = await testLlmConfig(target.id);
    if (!r.ok) {
      llmTestError.value = `切换失败：${r.message || '连接测试未通过'}`;
      llmSelectKey.value = cur ? llmKey(cur) : '';
      return;
    }
    if (target.id != null) await activateLlmConfig(target.id);
    await loadLlmProvider();
    toast.value = `已切换当前大模型为「${llmName(target)}」`;
  } catch (e) {
    llmTestError.value = (e as Error).message || '切换失败';
    llmSelectKey.value = cur ? llmKey(cur) : '';
  } finally {
    llmTesting.value = false;
  }
}

/* —— 分析任务历史 —— */
const tasks = ref<AnalysisTask[]>([]);
const taskTotal = ref(0);
const taskPage = ref(1);
const taskSize = ref(8);
const taskLoading = ref(false);

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
 * 引擎分析（G3 真实数据接入）：对已选来源调用 /api/analyze/batch。
 * 单选 = 单来源，多选 = 合并分析；失败时展示错误空态，不回退假数据。
 */
async function acquireEngineDraft() {
  engineError.value = '';
  const ids = selectedSourceIds.value;
  if (!ids.length) {
    engineData.value = null;
    engineCandidates.value = [];
    engineError.value = '请先在入口选择数据来源';
    return;
  }
  try {
    const draft = await fetchEngineAnalyzeBatch(ids);
    engineData.value = draft;
    engineCandidates.value = draft.candidates ?? [];
    engineConnectorName.value = sourceNames(ids);
    engineConnectorId.value = String(ids[0]);
  } catch (err) {
    console.warn('[G3] 引擎分析调用失败：', err);
    engineData.value = null;
    engineCandidates.value = [];
    engineError.value = err instanceof Error ? err.message : String(err);
  }
}

async function startRoute(key: RouteKey) {
  if (key === 'manual') {
    routeKey.value = key;
    stage.value = 'manual';
    return;
  }
  // 引擎/大模型分析都需要连库：开始前对选中来源做实时连接预检
  const failed = await precheckSources();
  if (failed.length) {
    routeKey.value = null;
    engineError.value = `以下来源连接失败，无法开始分析，请先在「数据接入」测试修复：${failed.join('、')}`;
    return;
  }
  await refreshSourceStatus();
  if (key === 'llm' && !llmHasKey.value) {
    toast.value = '未启用大模型配置，细化将返回引擎原稿（可在侧边栏「大模型接入」配置）';
  }
  routeKey.value = key;
  confirmedBaseGraph.value = false;
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

/** 对选中的每个来源实时测试连接，返回连接失败的来源名列表 */
async function precheckSources(): Promise<string[]> {
  const failed: string[] = [];
  for (const id of selectedSourceIds.value) {
    const c = sourceConnectors.value.find((x) => x.id === id);
    try {
      const r = await testConnector(id);
      if (!r.ok) failed.push(c?.name ?? id);
    } catch {
      failed.push(c?.name ?? id);
    }
  }
  return failed;
}

/** 重新拉取连接器刷新状态点（保留已选，不重置） */
async function refreshSourceStatus() {
  try {
    const { records } = await fetchConnectors(1, 50);
    sourceConnectors.value = records.filter((c) => c.connectorType === 'DB' && c.enabled === 1);
  } catch {
    /* 静默：状态点保持原样 */
  }
}

/** 大模型路线：先确保有引擎草稿，再调真实细化接口 */
async function startLlmRoute() {
  if (engineData.value === null) {
    await acquireEngineDraft();
  }
  if (!selectedSourceIds.value.length) {
    refineError.value = '请先选择数据来源，无法调用大模型细化';
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

/** 主动分流 ②：草稿不够 → 加大模型细化（G4 真实接口，单/多来源） */
async function refineWithLlm() {
  refineError.value = '';
  const ids = selectedSourceIds.value;
  if (!ids.length) {
    refineError.value = '请先选择数据来源，无法调用大模型细化';
    return;
  }
  refineLoading.value = true;
  try {
    refineResult.value = await postEngineRefineBatch(ids);
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
  // 保留来源选择；刷新任务列表
  void refreshTasks();
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
  void loadSources();
  void refreshTasks();
  void loadLlmProvider();
});

onBeforeUnmount(() => {
  if (scanTimer !== null) window.clearTimeout(scanTimer);
});

/* —— 数据来源：加载 / 多选切换 —— */

async function loadSources() {
  try {
    const { records } = await fetchConnectors(1, 50);
    sourceConnectors.value = records.filter((c) => c.connectorType === 'DB' && c.enabled === 1);
    // 不自动预选：所有来源均由带自动测速门禁的 picker 选择
    selectedSourceIds.value = [];
  } catch {
    sourceConnectors.value = [];
    selectedSourceIds.value = [];
  }
}

/** 来源下拉列表：空输入展示默认前几条预览，有输入做模糊过滤（名称/库名/主机） */
const filteredSources = computed<DataSourceConnector[]>(() => {
  const kw = sourceKeyword.value.trim().toLowerCase();
  if (!kw) return sourceConnectors.value.slice(0, SOURCE_PREVIEW_COUNT);
  return sourceConnectors.value.filter((c) =>
    (c.name || '').toLowerCase().includes(kw)
    || (c.databaseName || '').toLowerCase().includes(kw)
    || (c.host || '').toLowerCase().includes(kw));
});

/** 来源连接状态点：ok=上次测试成功 / fail=失败 / none=未测试 */
function sourceStatus(c: DataSourceConnector): 'ok' | 'fail' | 'none' {
  return c.lastTestStatus === 'OK' ? 'ok' : c.lastTestStatus === 'FAIL' ? 'fail' : 'none';
}

function sourceStatusText(c: DataSourceConnector): string {
  return c.lastTestStatus === 'OK'
    ? `上次测试通过 ${c.lastTestTime ?? ''}`
    : c.lastTestStatus === 'FAIL' ? `上次测试失败 ${c.lastTestTime ?? ''}` : '未测试过，开始分析时会自动预检';
}

/** 已选来源中上次连接失败的 */
const selectedFailed = computed(() =>
  sourceConnectors.value.filter((c) => selectedSourceIds.value.includes(c.id) && c.lastTestStatus === 'FAIL'));

/** 本地时间 yyyy-MM-ddTHH:mm:ss */
function nowIso(): string {
  const d = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}

/** dbType → tag 色调（mysql 蓝 / postgresql 青 / h2 灰） */
function dbTypeTone(c?: DataSourceConnector): string {
  if (!c) return 'tag--neutral';
  const map: Record<string, string> = { mysql: 'tag--accent', postgresql: 'tag--info', h2: 'tag--neutral' };
  return map[c.dbType ?? ''] ?? 'tag--neutral';
}

/** 选择来源：先实时测速（搜索列表状态可能不是最新），通过才加入选中；失败不选中并提示 */
async function pickSource(c: DataSourceConnector) {
  if (sourceTestingId.value === c.id) return;
  if (selectedSourceIds.value.includes(c.id)) {
    sourcePickerOpen.value = false;
    return;
  }
  sourceTestingId.value = c.id;
  sourceTestError.value = '';
  try {
    const r = await testConnector(c.id);
    c.lastTestStatus = r.ok ? 'OK' : 'FAIL';
    c.lastTestTime = nowIso();
    if (r.ok) {
      sourceTestResults.value[c.id] = r;
      const set = new Set(selectedSourceIds.value);
      set.add(c.id);
      selectSource([...set]);          // 复用现有 selectSource → resetAll()（保留来源）+ refreshTasks()
      // 下拉保持打开：方便连续多选，点外部才关闭
    } else {
      sourceTestError.value = `连接测试失败：${r.message || '无法连接'}`;
    }
  } catch (e) {
    c.lastTestStatus = 'FAIL';
    c.lastTestTime = nowIso();
    sourceTestError.value = `连接测试失败：${(e as Error).message || '无法连接'}`;
  } finally {
    sourceTestingId.value = '';
  }
}

/** 移除已选来源（不重测） */
function removeSource(id: string) {
  selectSource(selectedSourceIds.value.filter((x) => x !== id));
}

/** 失焦延迟关闭下拉：给行点击 120ms 落袋时间 */
function closePicker() {
  window.setTimeout(() => {
    sourcePickerOpen.value = false;
  }, 120);
}

/** 设置来源选择并重置草稿 */
function selectSource(ids: string[]) {
  selectedSourceIds.value = ids;
  resetAll();
}

/** 来源名拼接（单选=名，多选=A + B） */
function sourceNames(ids: string[]): string {
  return ids
    .map((id) => sourceConnectors.value.find((c) => c.id === id)?.name ?? String(id))
    .join(' + ');
}

/* —— 分析任务历史 —— */

async function refreshTasks() {
  taskLoading.value = true;
  try {
    const filter = selectedSourceIds.value.length ? selectedSourceIds.value[0] : undefined;
    const { records, total } = await fetchAnalyzeTasks(taskPage.value, taskSize.value, filter);
    tasks.value = records;
    taskTotal.value = total;
  } catch {
    tasks.value = [];
    taskTotal.value = 0;
  } finally {
    taskLoading.value = false;
  }
}

/** 查看历史任务：把草稿快照载入画布（ENGINE→draft / LLM→细化视图） */
async function viewTask(rec: AnalysisTask) {
  try {
    const detail = await fetchAnalyzeTask(rec.id);
    const snap = detail.draftSnapshot;
    if (!snap) return;
    // 同步来源选择到该任务来源
    selectedSourceIds.value = (detail.connectorIds ?? String(detail.connectorId))
      .split(',').map((s) => s.trim()).filter(Boolean);
    engineConnectorName.value = detail.connectorName ?? '';
    engineConnectorId.value = String(detail.connectorId);
    if (rec.taskType === 'LLM' && 'base' in snap) {
      engineData.value = snap.base;
      refineResult.value = snap;
      stage.value = 'llm';
    } else {
      engineData.value = snap as EngineDraft;
      refineResult.value = null;
      stage.value = 'draft';
    }
    engineCandidates.value = engineData.value?.candidates ?? [];
    routeKey.value = rec.taskType === 'LLM' ? 'llm' : 'engine';
  } catch (err) {
    toast.value = err instanceof Error ? err.message : '加载任务失败';
  }
}

/** 重跑历史任务：以该任务来源重新发起分析 */
function rerunTask(rec: AnalysisTask) {
  const ids = (rec.connectorIds ?? String(rec.connectorId))
    .split(',').map((s) => s.trim()).filter(Boolean);
  selectSource(ids.length ? ids : [String(rec.connectorId)]);
  void startRoute(rec.taskType === 'LLM' ? 'llm' : 'engine');
}

function statusLabel(s: string): string {
  return { RUNNING: '进行中', SUCCESS: '成功', FAILED: '失败' }[s] ?? s;
}

function statusClass(s: string): string {
  return s === 'SUCCESS' ? 'tag--success' : s === 'FAILED' ? 'tag--danger' : 'tag--warning';
}

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

    <!-- ═══ 入口：数据来源选择 + 三条路线选择 + 分析任务历史（15.2） ═══ -->
    <div v-if="stage === 'entry'" class="gs-body gs-body--entry">
      <!-- 数据来源：搜索 + 紧凑列表 + 连接状态 + 选中摘要（单选=单来源，多选=合并分析） -->
      <div class="gs-source-block">
        <div class="gs-source-head">
          <div class="gs-source-title"><Icon name="database" :size="15" /> 选择数据来源</div>
          <div class="gs-source-hint">
            已选 {{ selectedSourceIds.length }} 个 · 单选=单一分析，多选=合并分析
            <router-link class="gs-source-link" to="/data-sources">管理来源</router-link>
          </div>
        </div>

        <div v-if="!sourceConnectors.length" class="gs-source-empty">
          暂无启用的 DB 连接器，请先到「<router-link class="gs-source-link" to="/data-sources">数据接入</router-link>」新建并启用。
        </div>
        <template v-else>
          <div
            class="gs-source-picker"
            @focusin="sourcePickerOpen = true"
            @focusout="closePicker"
          >
            <div class="gs-source-toolbar">
              <div class="gs-source-search">
                <Icon name="search" :size="14" />
                <input v-model="sourceKeyword" class="input" placeholder="搜索名称 / 库名 / 主机" @focus="sourcePickerOpen = true" />
              </div>
              <span v-if="sourceKeyword.trim()" class="faint gs-source-filtered">
                匹配 {{ filteredSources.length }} / {{ sourceConnectors.length }}
              </span>
              <span v-else-if="sourceConnectors.length > SOURCE_PREVIEW_COUNT" class="faint gs-source-filtered">
                共 {{ sourceConnectors.length }} 个来源，已展示前 {{ SOURCE_PREVIEW_COUNT }} 条
              </span>
            </div>

            <div v-if="sourcePickerOpen" class="gs-source-dropdown">
              <div
                v-for="c in filteredSources" :key="c.id"
                class="gs-source-row" :class="{ 'gs-source-row--active': selectedSourceIds.includes(c.id) }"
                @click="pickSource(c)"
              >
                <div class="gs-source-info">
                  <div class="gs-source-name">
                    {{ c.name }}
                    <span v-if="c.isActive === 1" class="tag tag--accent gs-source-current">当前</span>
                    <span v-if="selectedSourceIds.includes(c.id)" class="tag tag--success gs-source-current">已选</span>
                  </div>
                  <div class="gs-source-meta">
                    {{ (c.dbType || 'db').toUpperCase() }} · {{ c.host || '本地' }}:{{ c.port || '-' }}/{{ c.databaseName || '-' }}
                  </div>
                </div>
                <span v-if="sourceTestingId === c.id" class="faint gs-source-status">测试中…</span>
                <span v-else class="gs-source-status" :title="sourceStatusText(c)">
                  <span class="gs-source-dot" :class="'gs-source-dot--' + sourceStatus(c)" />
                  {{ sourceStatus(c) === 'ok' ? '正常' : sourceStatus(c) === 'fail' ? '失败' : '未测' }}
                </span>
              </div>
              <div v-if="!filteredSources.length" class="empty"><div class="empty-title">无匹配的来源</div></div>
            </div>
          </div>

          <div v-if="sourceTestError" class="gs-source-warn">
            <Icon name="alert" :size="13" />{{ sourceTestError }}
          </div>

          <div v-if="selectedSourceIds.length" class="gs-source-cards">
            <div v-for="id in selectedSourceIds" :key="id" class="gs-source-card">
              <div class="gs-source-card-main">
                <div class="gs-source-card-name">{{ sourceConnectors.find((c) => c.id === id)?.name ?? id }}</div>
                <span class="tag" :class="dbTypeTone(sourceConnectors.find((c) => c.id === id))">
                  {{ (sourceConnectors.find((c) => c.id === id))?.dbType?.toUpperCase() || 'DB' }}
                </span>
                <span class="tag tag--success gs-source-card-ok">
                  测试通过{{ sourceTestResults[id]?.latencyMs != null ? ` · ${sourceTestResults[id].latencyMs}ms` : '' }}
                </span>
              </div>
              <button class="btn btn-xs btn-ghost gs-source-card-remove" title="移除" @click="removeSource(id)">×</button>
            </div>
            <span v-if="selectedSourceIds.length > 1" class="tag tag--info gs-source-merge">
              合并分析（{{ selectedSourceIds.length }} 个来源）
            </span>
          </div>

          <div v-if="selectedFailed.length" class="gs-source-warn">
            <Icon name="alert" :size="13" />
            以下已选来源上次连接失败，开始分析会被预检阻断：{{ selectedFailed.map((c) => c.name).join('、') }}
          </div>
          <div v-if="engineError && stage === 'entry'" class="gs-source-warn">
            <Icon name="alert" :size="13" />{{ engineError }}
          </div>
        </template>
      </div>

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
          <div v-if="r.key === 'llm'" class="gs-llm-state" :class="{ 'gs-llm-state--ok': llmHasKey }">
            <Icon name="link" :size="12" />
            <template v-if="llmConfigs.length">
              <span class="gs-llm-label">当前大模型：</span>
              <select v-model="llmSelectKey" class="gs-llm-select" :disabled="llmTesting" @change="onLlmSelect">
                <option v-for="c in llmConfigs" :key="llmKey(c)" :value="llmKey(c)">{{ llmName(c) }}</option>
              </select>
              <span v-if="llmTesting" class="gs-llm-testing">测试中…</span>
            </template>
            <template v-else>
              <span>未配置大模型 · 细化将返回引擎原稿</span>
            </template>
          </div>
          <div v-if="r.key === 'llm' && llmTestError" class="gs-llm-error">
            <Icon name="alert" :size="12" />{{ llmTestError }}
          </div>
          <button class="btn btn-primary gs-route-btn" @click="startRoute(r.key)">开始{{ r.name }}</button>
        </div>
      </div>

      <div class="gs-flow-note">
        <div class="gs-flow-title"><Icon name="flow" :size="15" /> 出图流程</div>
        <div class="gs-flow-line">图来源入口 → 选路线 → 引擎出草稿 → 主动分流（够用 / 加大模型 / 作废重来）→ 人工校正（唯一拍板）→ 准底图</div>
        <div class="gs-flow-hint">原则：主动权在人不在系统——系统只清楚呈现草稿，不做自检/裁决。</div>
      </div>

      <!-- 分析任务历史：每次分析一条，可查看 / 重跑 -->
      <div class="card gs-task-card">
        <div class="card-header">
          <div class="card-title">分析任务</div>
          <span class="card-sub faint">每次对来源发起一次分析即一条任务，可查看草稿 / 重跑</span>
        </div>
        <div class="card-body">
          <div v-if="taskLoading" class="empty"><div class="empty-title">加载中…</div></div>
          <div v-else-if="!tasks.length" class="empty"><div class="empty-title">暂无分析任务</div></div>
          <div v-else class="gs-task-table-wrap">
            <table class="data-table">
              <thead>
                <tr>
                  <th>类型</th>
                  <th>来源</th>
                  <th>状态</th>
                  <th>操作人</th>
                  <th>创建时间</th>
                  <th class="text-right">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="t in tasks" :key="t.id">
                  <td>
                    <span class="tag" :class="t.taskType === 'LLM' ? 'tag--info' : 'tag--accent'">
                      {{ t.taskType === 'LLM' ? '大模型' : '引擎' }}
                    </span>
                  </td>
                  <td class="cell-muted">{{ t.connectorName }}</td>
                  <td><span class="tag" :class="statusClass(t.status)">{{ statusLabel(t.status) }}</span></td>
                  <td class="cell-muted">{{ t.operator }}</td>
                  <td class="cell-mono cell-muted">{{ t.createdAt }}</td>
                  <td class="text-right">
                    <button class="btn btn-xs btn-ghost" @click="viewTask(t)">查看</button>
                    <button class="btn btn-xs btn-ghost" @click="rerunTask(t)">重跑</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="taskTotal > taskSize" class="gs-task-pager">
            <button class="btn btn-xs btn-ghost" :disabled="taskPage <= 1" @click="taskPage--; refreshTasks()">上一页</button>
            <span class="faint">第 {{ taskPage }} 页 · 共 {{ taskTotal }} 条</span>
            <button class="btn btn-xs btn-ghost" :disabled="taskPage * taskSize >= taskTotal" @click="taskPage++; refreshTasks()">下一页</button>
          </div>
        </div>
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
.gs-llm-state {
  display: flex; align-items: center; gap: 6px;
  font-size: 11.5px; color: var(--warning, #f59e0b);
  background: rgba(245, 158, 11, .08); border: 1px solid rgba(245, 158, 11, .25);
  border-radius: var(--radius-sm); padding: 6px 9px; margin-top: 10px; line-height: 1.5;
}
.gs-llm-state--ok { color: var(--success); background: rgba(16, 185, 129, .08); border-color: rgba(16, 185, 129, .25); }

.gs-flow-note {
  max-width: 1100px; margin: 28px auto 0; padding: 16px 20px;
  background: var(--surface); border: 1px dashed var(--border-strong);
  border-radius: var(--radius-lg); color: var(--fg-muted); font-size: 12.5px;
}
.gs-flow-title { display: flex; align-items: center; gap: 6px; font-weight: 600; color: var(--fg); margin-bottom: 6px; }
.gs-flow-line { line-height: 1.7; }
.gs-flow-hint { margin-top: 6px; font-size: 12px; color: var(--fg-faint); }

/* —— 入口：数据来源选择 —— */
.gs-source-block { max-width: 1100px; margin: 0 auto 22px; }
.gs-source-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 10px; flex-wrap: wrap; }
.gs-source-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; }
.gs-source-hint { font-size: 11.5px; color: var(--fg-faint); display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.gs-source-link { color: var(--accent); text-decoration: none; }
.gs-source-link:hover { text-decoration: underline; }
.gs-source-empty { padding: 16px; border: 1px dashed var(--border-strong); border-radius: var(--radius); font-size: 12.5px; color: var(--fg-muted); }
.gs-source-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.gs-source-search { display: flex; align-items: center; gap: 6px; flex: 1; max-width: 380px; }
.gs-source-search .input { height: 32px; font-size: 12.5px; }
.gs-source-search svg { color: var(--fg-faint); flex-shrink: 0; }
.gs-source-filtered { font-size: 11.5px; }
.gs-source-list {
  max-height: 220px; overflow-y: auto;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--surface);
}
.gs-source-row {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; cursor: pointer; transition: background .12s;
  border-bottom: 1px solid var(--border);
}
.gs-source-row:last-child { border-bottom: none; }
.gs-source-row:hover { background: var(--surface-2); }
.gs-source-row--active { background: var(--accent-soft); }
.gs-source-checkbox { font-size: 15px; color: var(--fg-muted); flex-shrink: 0; width: 16px; text-align: center; }
.gs-source-row--active .gs-source-checkbox { color: var(--accent); }
.gs-source-info { flex: 1; min-width: 0; }
.gs-source-name { font-size: 13px; font-weight: 600; display: flex; align-items: center; gap: 6px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.gs-source-current { height: 18px; font-size: 10px; padding: 0 6px; flex-shrink: 0; }
.gs-source-meta { font-size: 11px; color: var(--fg-faint); font-family: var(--font-mono); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.gs-source-status { display: inline-flex; align-items: center; gap: 5px; font-size: 11px; color: var(--fg-muted); flex-shrink: 0; }
.gs-source-dot { width: 8px; height: 8px; border-radius: 50%; }
.gs-source-dot--ok { background: var(--success); }
.gs-source-dot--fail { background: var(--danger); }
.gs-source-dot--none { background: var(--border-strong); }
.gs-source-summary {
  display: flex; align-items: center; gap: 10px; flex-wrap: wrap;
  margin-top: 10px; font-size: 12.5px; color: var(--fg-muted);
}
.gs-source-warn {
  display: flex; align-items: center; gap: 6px;
  margin-top: 8px; font-size: 12px; color: var(--danger);
  background: var(--danger-soft); border: 1px solid rgba(244, 63, 94, .25);
  border-radius: var(--radius-sm); padding: 7px 10px; line-height: 1.5;
}

/* —— 来源下拉 + 已选横向卡片 —— */
.gs-source-picker { position: relative; }
.gs-source-dropdown {
  position: absolute; top: 38px; left: 0; right: 0; z-index: 30;
  max-height: 240px; overflow-y: auto;
  border: 1px solid var(--border); border-radius: var(--radius-sm);
  background: var(--surface); box-shadow: 0 4px 16px rgba(15, 23, 42, .12);
}
.gs-source-dropdown .gs-source-row { padding: 8px 12px; }
.gs-source-dropdown .gs-source-row:last-child { border-bottom: none; }
.gs-source-cards { display: flex; flex-wrap: wrap; gap: 10px; margin-top: 12px; align-items: center; }
.gs-source-card {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; border: 1px solid var(--border); border-radius: var(--radius);
  background: var(--surface-2);
}
.gs-source-card-main { display: flex; align-items: center; gap: 8px; min-width: 0; }
.gs-source-card-name { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 220px; }
.gs-source-card-ok { height: 18px; font-size: 10.5px; padding: 0 7px; }
.gs-source-card-remove { color: var(--fg-faint); font-size: 14px; padding: 0 6px; height: 22px; }
.gs-source-card-remove:hover { color: var(--danger); }
.gs-source-merge { height: 20px; font-size: 11px; }

/* —— LLM 当前大模型选择器 —— */
.gs-llm-label { white-space: nowrap; }
.gs-llm-select {
  height: 26px; padding: 0 6px; font-size: 12px; font-family: inherit;
  border: 1px solid var(--border-strong); border-radius: var(--radius-sm);
  background: var(--surface); color: var(--fg); max-width: 190px;
}
.gs-llm-testing { color: var(--fg-muted); white-space: nowrap; }
.gs-llm-error { font-size: 11.5px; color: var(--danger); margin-top: 6px; display: flex; align-items: center; gap: 4px; }

/* —— 入口：分析任务历史 —— */
.gs-task-card { max-width: 1100px; margin: 22px auto 0; }
.gs-task-table-wrap { overflow-x: auto; }
.gs-task-pager { display: flex; align-items: center; justify-content: flex-end; gap: 12px; margin-top: 12px; font-size: 12px; }
.btn-xs { height: 24px; padding: 0 8px; font-size: 11px; border-radius: 6px; display: inline-flex; align-items: center; gap: 4px; }

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
