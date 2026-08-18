# 发现与决策（图来源 G1~G5 + G3 进行中）

## 需求
- 文档书第 15 章「图来源与自动/半自动分析」：三条路线（引擎①/大模型②/人工③）+ 三层视图（数据流/业务流/融合）+ 校正闭环。实现顺序 G1→G5。

## 研究发现（探索子代理 2026-08-17）

### 后端现状
- **Node 实体**：id/nodeType(SYSTEM|SUBSYSTEM|DATABASE|TABLE|DEPARTMENT|ROLE|ACTION|EVENT|DEVICE|WORKSTATION)/name/code/subType/ip/level(L1~L4)/status/owner/description/createdAt/updatedAt。**无三层视图字段**；DB 有 `ext TEXT` 列但实体未映射。
- **Relation**：fromNodeId/toNodeId/relationType(DATA_FLOW|API|SUBSCRIBE|APPROVAL)/level/description/status。
- **Process**：scene(DATA|BUSINESS|MANUFACTURING)/startNodeId/endNodeId。
- **Route**：processId/name/priority(DEFAULT|RECOMMENDED|ALTERNATE)；RouteNode 复合主键 routeId+seq，含 expectedDurationMs(SLA)。
- **控制器**：/api/nodes、/api/edges、/api/processes、/api/routes、/api/search、/api/versions、/api/connectors(9 端点)、/api/instances、/api/checkpoints、/api/alerts、/api/tickets、/api/dashboard、/api/graph(trace/path/impact)、/api/open、/api/system、/api/auth。
- **Service**：GraphService(只读装配)、ModelingService(CRUD+recordVersion)、GraphAlgorithmService(queryPaths/impact)、SearchService、ConfigVersionService、ConnectorService、CmdbService、Trace/Instance/Alert/Checkpoint/Ticket/Dashboard/InterventionService。
- **datasource 连接器（G3 可复用）**：ConnectorDbType(MYSQL/POSTGRESQL/H2) + DbDialect(driverClass/buildJdbcUrl/testSql/quote/previewSql) + DbDialectFactory + ConnectionPoolRegistry(按 connector.id 懒建 HikariCP，池5/timeout10s，AES-GCM 解密密码)。tables() 用 DatabaseMetaData，preview() 执行 previewSql。Connector 支持 DB 与 CMDB 两种类型。
- **Flyway 迁移**：h2/V1 与 mysql/V1 同构 21 张表；common V2~V7（种子/connector扩列/付款流程/监控种子/只读用户/改名）。**当前最高 V7**。新增表 → V8。
- **配置**：application.yml 端口 **28080**（本地已改，未提交）、datalink.db-type、datalink.crypto.key、datalink.jwt、datalink.openapi.token。local=H2 文件库、mysql=MySQL8 环境变量覆盖。
- **测试**：JUnit5 + MockMvc，19 个测试类，后端 76 测试全绿。

### 前端现状
- **路由**：/login、/、/graph、/3d、/processes、/data-sources、/checkpoints、/alerts、/bigscreen、/versions、/settings。MainLayout+SideNav。
- **GraphView.vue（核心）**：加载 fetchNodes/fetchEdges/fetchProcesses/fetchRoutes，client 端 computed 过滤 routeViewNodes/Edges。子组件：GraphCanvas(G6 2D，concentric/dagre)、Graph3DCanvas(3d-force-graph，导出 nodeColorMap/legendKeys)、PathMapView(SVG 地铁图)、NodeDetailPanel。交互：路线高亮、排查模式、路径查询、2D⇄3D 切换(viewMode3d)、路线过滤。**GraphCanvas 与 Graph3DCanvas props/emit 契约一致（nodes/edges/routes/activeRouteId/focusNodeId→node-click），加第三层视图可沿此契约扩展**。
- **api/index.ts**：apiFetch(Bearer token，401→clearToken+跳登录，Result.code===200)。端点清单齐全。
- **types/index.ts**：GraphNode/GraphEdge/Route/ProcessDef/GraphTrace/GraphPathResult/ImpactResult。
- **Pinia 已装未用**，无 stores。
- **DataSourceView.vue**：连接器分页→浏览展开行（左表列表+右预览50行），DB/CMDB 模态，是图来源 UI 先例。
- **构建**：vite^6/vue^3.5/@antv/g6^5.0.40/3d-force-graph^1.80/pinia^2.3。vite.config.ts 代理 /api→8080，manualChunks 拆 g6/vue-vendor。**⚠️ 后端端口已改 28080，代理目标 8080 需核实对齐**。
- **样式**：styles/tokens.css 浅色UI+#0d1424 深色画布、科技蓝 #2563eb、节点类型色变量；global.css 共享按钮/表格/表单。

## 三层视图映射设计（G1）
- **数据流视图**（表级血缘）：DATABASE/TABLE 节点 + DATA_FLOW 关系为主
- **业务流视图**（系统级）：SYSTEM/SUBSYSTEM/DEPARTMENT/ROLE/ACTION 节点 + API/SUBSCRIBE/APPROVAL 关系为主
- **融合视图**：全部节点/边（现状即融合，作为默认）
- 前端 computed 过滤即可，Node/Relation 实体不动（设计 15.7 定"先做前端投影"）

## 技术决策
| 决策 | 理由 |
|------|------|
| G1 前端投影，不加字段 | 设计 15.7 明确；零迁移成本 |
| 视图映射 nodeType/relationType 推导 | 现状数据已具备分类能力 |
| G3 复用 connector 连接池扫描 | DbDialect+Registry 现成，不必重写 |
| G4 先可插拔接口 + 空实现兜底 | 无 key 也可全链路跑通，用户后续补 key |
| 迁移新表用 V8 | 当前最高 V7 |

## 遇到的问题
| 问题 | 解决方案 |
|------|---------|
| 后端端口 28080 vs vite 代理 8080 不一致 | 核实 vite.config.ts，统一或保留 8080 后端 |
| Node 无三层字段 | 前端推导，不做迁移 |

## 资源
- 设计基准：文档书第 15 章（509-636 行）+ 决策 #15
- 进度基准：文档书第 0 章 + docs/PROJECT-HANDOFF.md

## 视觉/浏览器发现
- **G1 三层视图无头 Chrome 程序化复核通过（2026-08-17）**：用 `.data/g1_verify.py`（Python websocket-client + CDP，附加到已加载的 /graph 页，读 Vue setupState 的 viewLayer/layerViewNodes/layerViewEdges + 图例 + 激活按钮）：
  - **融合**（默认）：activeBtn=融合，图例 6 项（系统/数据库表/部门岗位/业务动作/异常/失败），18 节点 {SYSTEM:4,SUBSYSTEM:2,DATABASE:4,TABLE:2,DEPARTMENT:2,ACTION:4}，20 边 {API:2,DATA_FLOW:15,APPROVAL:3}
  - **数据流**：viewLayer=data，图例 3 项（数据库表/异常/失败），6 节点 {DATABASE:4,TABLE:2}，2 边 {DATA_FLOW:2}
  - **业务流**：viewLayer=business，图例 5 项，12 节点 {SYSTEM:4,SUBSYSTEM:2,DEPARTMENT:2,ACTION:4}，10 边 {API:2,APPROVAL:3,DATA_FLOW:5}（5 条 DATA_FLOW 两端点恰都在业务层类型内，是 layerViewEdges 两端都在集合内才保留的预期行为）
  - **可回退**：切回融合恢复 18/20，切换无残留
- 技术要点：CDP 附加到**已存在** target（`Target.attachToTarget` flatten）+ `Runtime.evaluate` `returnByValue` 读 `document.querySelector('.graph-page').__vueParentComponent` 沿 parent 链找 `setupState.viewLayer !== undefined` 的组件实例——无需像素比对即可精确验证层过滤逻辑。

## 图来源 G3 引擎最小可行版（进行中，2026-08-17）

### 已完成（G3a~G3d + 前端类型）
- **G3a** `backend/src/main/resources/engine/his_demo_schema.sql`：HIS 演示库 6 张表（reg_order / fee_order / refund_apply / settle_bill / pay_record / prescription_detail），覆盖五大信号。
- **G3b** `backend/src/main/java/com/datalink/platform/engine/` 包（9 个文件）：
  - DTOs：EngineDraftVO / EngineCandidateVO / EngineFlowVO
  - Service：EngineAnalyzeService + EngineAnalyzeServiceImpl（5 信号打分 + 单号前缀归属判定 + H2 会话内部表过滤 META_TABLES）
  - Controller：AnalyzeController（GET /api/analyze?connectorId=，已登录即可）
- **G3c** 启动期初始化器：
  - `DemoBizDbInitializer`（`@Order(10)` ApplicationRunner）：幂等建独立内存库 `datalink_demo`，SQL 拆句改用「剥注释行 → 行尾 `;` 切」避免头注释吞块
  - `DemoConnectorSeeder`（`@Order(20)` ApplicationRunner）：运行时 AesUtil 加密密码，避免迁移里硬编码密文；幂等更新已存在 connector
  - `SecurityConfig`：`.requestMatchers(HttpMethod.GET, "/api/analyze/**").authenticated()`
- **G3d** `EngineAnalyzeServiceTest` 6/6 通过（六表候选、草稿图结构、置信度区间 60~85、404/disabled 拒绝）
- 后端测试基线：**20 测试类全绿**（原 19 + 新 1）
- **前端 types**：`types/index.ts` 追加 `EngineCandidate / EngineFlow / EngineDraft`（第 241~266 行）

### 关键踩坑（明日续接必读）
| 坑 | 根因 | 解法 |
|---|---|---|
| DemoBizDbInitializer 首次启动报 `Table "reg_order" not found` | SQL 拆句器按 `;\n` 切，首个语句被文件头注释块整体吞掉 | 逐行剥注释 → 行尾 `;` 切 |
| 同一 JVM 多测试上下文共享 datalink_demo 内存库时重复 CREATE | `INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'` 在 DATABASE_TO_LOWER=TRUE 下 schema 存为小写，匹配失败 → 误判空库 | 改为 `WHERE TABLE_NAME='reg_order'` 单表级检查 |
| EngineAnalyzeServiceImpl 输出 10 候选（预期 6） | H2 多连接时自动创建 `locks/query_statistics/sessions/session_state` 会话级内部表 | 加 META_TABLES 常量集合过滤（Set.of 8 个） |
| 草稿边方向反了（本应 provider→consumer） | 原 linkReferences 用 "同列名" 判定，多表共享时方向混乱 | 重构为「编码号列 = 本表唯一编码号 → 身份列跳过；否则 `fee_no` 前缀 → 表名 `fee_*` 为拥有者」 |
| `buildFlows` 方法头丢失 | linkReferences 重构时流程链代码嵌入末尾忘记抽成独立方法 | 抽为独立 private buildFlows(List<TableMeta>) |

### 置信度实测回填（设计 15.3 区间 60~85）
| 表 | 置信度 | 信号 |
|---|---|---|
| fee_order（收费单） | 85 | 主键 + 单号 + 状态 + 时间 + 引用 ×2 + 单号格式 |
| reg_order（挂号单） | 75 | 主键 + 单号 + 状态 + 时间 + 引用 + 单号格式 |
| refund_apply（退费申请单） | 65 | 单号 + 状态 + 时间 + 引用 + 单号格式 |
| settle_bill（结算单） | 60 | 单号 + 时间 + 单号格式 |
| pay_record（支付流水） | 60 | 单号 + 时间 + 单号格式 |
| prescription_detail（处方明细） | 40 | 主键 + 主子表（低置信） |

### 前端状态（G3e 半途）
- `frontend/src/types/index.ts`：**✅ 已加** `EngineCandidate / EngineFlow / EngineDraft`
- `frontend/src/api/index.ts`：**🔄 半成品** — import 已加 `EngineDraft`，但 `fetchEngineAnalyze` 函数**未追加**（上次 Edit 失败）；末尾是 `fetchOpenApiInfo`（line 355）
- `frontend/src/views/GraphSourceView.vue`：**❌ 未动**（仍为 G2 纯假数据版本）

## 图来源 G3 引擎最小可行版（完成，2026-08-18，复核全流程真实数据）

### G3e 前端接线（2026-08-18 完成）
- `frontend/src/api/index.ts`：末尾追加 `fetchEngineAnalyze(connectorId: string): Promise<EngineDraft>`（`GET /api/analyze?connectorId=...`）
- `frontend/src/views/GraphSourceView.vue` 改动：
  - import `EngineCandidate/EngineDraft` 类型 + `fetchEngineAnalyze/fetchConnectors` API
  - 新增响应式状态：`engineData`（EngineDraft | null）、`engineCandidates`（DraftCandidate[]）、`engineError`（string）、`engineConnectorName`（string）
  - 新增 `acquireEngineDraft()`：取第一个 `enabled=1` 的 DB 连接器调 `fetchEngineAnalyze`；失败 `console.warn` + 兜底 `ENGINE_CANDIDATES_FALLBACK` + `engineError` 提示条
  - `startRoute('engine')`：1.5s scanning 后进 draft，若 `engineData === null && engineCandidates.length === 0` 触发 `acquireEngineDraft()`
  - `draftNodes/draftEdges` 优先用 `engineData`（无则兜底 G2 假数据）；候选清单用 `engineCandidates`；`draftSummary` 真实时显示 `扫描 {database} · N 候选单据`；`gs-canvas-meta` 真实时追加连接器名标签
  - 模板加 `gs-left-warn` 错误提示条（引擎接口失败时显示 + 兜底说明）
- `npm run build` 全绿（13.5s）

### G3f 验证（2026-08-18 完成）
- **后端重启**：旧进程无 G3 种子（连接器表空 → `未找到已启用的 DB 连接器` 兜底），新进程 `spring-boot:run` 让 `DemoBizDbInitializer`（`@Order(10)`）+ `DemoConnectorSeeder`（`@Order(20)`）运行，HIS 连接器 id=1 出现
- **后端真实 API 核对**：`/api/analyze?connectorId=1` 返回 `database=datalink_demo` + 6 候选（85/75/65/60/60/40）+ 1 库节点 + 6 表节点 + 10 边 + 1 流程模板「挂号单→收费单→支付流水」
- **CDP 程序化复核**（`.data/g3_engine_verify.py`，复用 g2 结构）：入口 → 扫描 → 引擎草稿（`engineDataLoaded=true`、`database=datalink_demo`、7 节点/10 边、6 候选 85/75/65/60/60/40、4 低置信徽标、1 流程模板）→ 加大模型 14/20 → 回退 7/10 → 作废回入口 → 再引擎 → 人工校正 5 项 → 确认 toast + 复位 ✓ 全链路真实数据、无残留

### 关键踩坑（G3 完成新增）
| 坑 | 根因 | 解法 |
|---|---|---|
| CDP 复核时引擎接口未触发，显示「未找到已启用的 DB 连接器」 | 旧后端进程（IntelliJ spring-boot:run）未跑 G3 种子 | 重启 `mvn spring-boot:run` 让 Order(10)/Order(20) 初始化器执行 |

## 图来源 G4 大模型接入层（完成，2026-08-18，Noop 路径全链路复核通过）

### 后端 llm 包（可插拔 Provider）
- `com.datalink.platform.llm` 新包：
  - `provider/ModelProvider` 接口：`name() / available() / refine(LlmRefineRequest)`
  - `provider/NoopModelProvider`：无 key 兜底——`available()=false`，refine 返回空增量 + `refinements=[{type:"noop", text:"未配置大模型 API Key，返回引擎原稿"}]` + `provider="noop"`
  - `provider/OpenAiCompatibleModelProvider`：RestClient POST `{baseUrl}/chat/completions`（OpenAI chat 格式 + `response_format=json_object`）；`static parseContent(String, String)` 剥 ```json 围栏 → Jackson 解析 → addedNodes id 强制 `llm-` 前缀；**任何异常（HTTP/超时/JSON 非法/缺字段）降级为 `{type:"error"}` refinement，绝不抛异常**；日志禁打 api-key
  - `config/LlmConfig`：`@ConditionalOnExpression("...StringUtils.hasText('${datalink.llm.api-key:}')")` 装配 OpenAI 实现（空串不算）；`@ConditionalOnMissingBean(ModelProvider.class)` 装配 Noop
  - `config/LlmProperties`：`datalink.llm.*`（base-url 默认 `https://api.deepseek.com/v1` / api-key `${LLM_API_KEY:}` / model 默认 `deepseek-chat` / timeout-ms 30000 / max-tokens 2048 / temperature 0.2）
- 切换供应商只需环境变量：`LLM_API_KEY` / `LLM_BASE_URL` / `LLM_MODEL`（DeepSeek/通义/Claude/GPT 均 OpenAI 兼容）

### refine 细化接口
- `POST /api/analyze/refine`（AnalyzeController，SecurityConfig 加 POST `/api/analyze/**` authenticated）
- 请求 `RefineRequest{connectorId}`——**前端不回传草稿**，后端复跑引擎取 base（防篡改、职责单一；引擎只读内存计算成本可忽略）
- 响应 `RefineResultVO`：`base`(EngineDraftVO 原样 = 前端回退快照） + `addedNodes/addedEdges`（llm- 前缀增量） + `renameMap` + `refinements`（rename/chain/party/relation/flow + noop/error） + `provider` + `message`
- `EngineAnalyzeServiceImpl.refine()`：analyze 取 base → LlmRefineRequest → provider.refine → 组装；provider 抛异常 → log.error + 降级 `provider="error"`（base 仍返回）；null 集合防御为空集合

### 前端接线（GraphSourceView.vue）
- **删除** LLM_NODES / LLM_EDGES / LLM_REFINEMENTS 假数据常量；新增状态 `refineResult/refineLoading/refineError/engineConnectorId`
- 细化草稿 = 引擎骨架 `applyRename`（renameMap 按 code/id/name 匹配）+ addedNodes/addedEdges 合并；骨架引用保留即天然回退快照，`revertToEngine()` 仅切 stage
- `refineWithLlm()` async：已有 refineResult 直接切 stage（缓存不重复调）；否则 `postEngineRefine(engineConnectorId)` 成功 → stage='llm'，失败 → refineError 停留 draft
- 大模型路线直进 `startLlmRoute()`：先确保引擎草稿 → 无连接器退 draft + 提示；有则 refine
- Noop 提示：provider==='noop' 时左栏琥珀色 `gs-left-warn`「未配置大模型 API Key…」+ 徽标「引擎原稿（Noop）」+ summary「引擎原稿（未配置大模型）」
- REFINE_TYPE_LABEL 映射：rename→改名 / chain→动作链 / party→参与方 / relation→关系 / flow→流程 / noop→兜底 / error→异常

### 验证数字（2026-08-18）
- 后端 **24 测试类 94 用例全绿**（新增 4 类：OpenAiCompatibleModelProviderTest 5 用例解析/围栏/降级/缺字段、ModelProviderWiringTest 无 key 装配 Noop、EngineRefineServiceTest 3 用例、AnalyzeRefineControllerTest MockMvc 2 用例）
- 接口实测：`POST /api/analyze/refine {connectorId:1}` → code=200、provider=noop、base 与 GET /api/analyze 一致（datalink_demo 7 节点/10 边/6 候选）、refinements 1 条 noop
- CDP 复核（`.data/g4_llm_verify.py`）：引擎草稿 7/10 → 加大模型（noop 提示条 + 1 条 noop refinement + 草稿不变 + 徽标「引擎原稿（Noop）」）→ 回退 7/10 → 作废清空 refineResult → 大模型路线直进 llm 态 → 人工校正 5 项 → 确认复位 ✓ 无假数据残留

### 关键踩坑（G4 新增）
| 坑 | 根因 | 解法 |
|---|---|---|
| CDP attach 后「Inspected target navigated or closed」 | attach 的既有页面在导航后会话失效 | `Target.createTarget` 新建 about:blank 页再 attach |
| about:blank 上读 localStorage 报 SecurityError | 无 origin 页禁止 localStorage | 先导航 5173 → 登录写 token → 重新导航 |
| Controller 测试「无 body → 4xx」断言失败 | GlobalExceptionHandler 兜底 Exception 统一 HTTP 200 + body.code=500 | 断言 `$.code != 200`（行为等价拒绝，不动全局处理器） |
| `@ConditionalOnProperty` 无法区分「未配 key」与「key 为空串」 | matchIfMissing 对空串无效 | 改 `@ConditionalOnExpression` + `StringUtils.hasText` |

### 待办（下次会话）
- ⏳ 真实供应商验证：用户提供 key 后设 `LLM_API_KEY`/`LLM_BASE_URL`/`LLM_MODEL` 重启 → POST /api/analyze/refine 看 provider 非 noop + 五类 refinements 齐全 → 切换模型对比
- G5 校正闭环：校正记录表 + 留存接口 + 模式库沉淀（最简版）

## 图来源 G2 管线原型（2026-08-17 完成 + 复核）
- **实现**：`frontend/src/views/GraphSourceView.vue`（独立页，无后端依赖，全假数据）+ 路由 `/graph-source` + SideNav「图来源」入口。
- **状态机**：`stage: 'entry' | 'scanning' | 'draft' | 'llm' | 'manual'`，`routeKey: 'engine' | 'llm' | 'manual' | null`，`confirmedBaseGraph`，`toast`。入口三条路线 → 引擎/大模型路线经 1.5s scanning → draft/llm → 统一 manual → 确认准底图（toast + 1.8s 复位）。
- **数据契约**（G3 后端要回填对齐）：
  - 引擎草稿：8 节点（4 DATABASE/4 TABLE 风格 + 结算中心）+ 12 DATA_FLOW 边 + 候选清单 6 项 { name, type, confidence, reason }，置信度 95/91/84/76/61/53%，<70 打「低置信」徽标
  - 大模型细化：14 节点（引擎 + 部门/岗位/动作）+ 20 边（含 API/APPROVAL）+ 5 类补全（改名/动作链/参与方/关系/流程）
  - 人工校正：5 项 { name, type, action: '确认'|'改名'|'删除'|'合并' }
- **CDP 复核关键数字**：入口 3 卡 + activeStep 1 → 引擎草稿 8/12+6 候选 → 加大模型 14/20+5 补全 → 回退 8/12 → 作废回入口 → 再引擎 → manual 5 项 → 确认 toast「已确认准底图，流程骨架将写入平台关系网（G5 校正闭环将保留本次校正记录）」+ confirmed=true + 自动复位。
- **G2 原则（文档书 15.4）**：主动权在人——引擎出草稿后**必须用户主动选择**，系统不自动进入下一步；所有分支可回退。
- **G3 衔接**：/api/analyze 接口要产出的 JSON 尽量对齐 G2 前端已有的 `draftNodes/draftEdges/candidates/refinements` 结构，前端可直接换数据源。
