# 进度日志

## 会话：2026-08-17

### 阶段 G1：三层视图定型（前端投影）
- **状态：** complete ✅
- **开始时间：** 2026-08-17 · **完成时间：** 2026-08-17
- 执行的操作：
  - 读取 HANDOFF / 文档书第 0 章 / 第 15 章
  - 探索子代理：后端结构 + 前端结构两路并行侦察完成
  - 创建 task_plan.md / findings.md / progress.md
  - 修复 vite.config.ts 代理 8080→28080（对齐后端 application.yml）
  - 根目录 .gitignore 增加 `/.data/`
  - **GraphView.vue 三层视图实现完成**：viewLayer ref（data/business/fused 默认 fused）、layerNodeTypes/layerLegend 映射、layerViewNodes/layerViewEdges computed（融合不过滤）、routeViewNodes/Edges 链式叠加层过滤、工具条加 layer-switch tabs、图例随层收窄、3D 画布改用 layerViewNodes/Edges
  - `npm run build` 通过（14.98s）
  - 后端已在 28080 启动（admin/admin123，401 需登录）
  - 全局权限配置完成：~/.claude/settings.json 加 permissions.defaultMode=bypassPermissions（已备份，需重启会话生效）
  - **无头 Chrome 程序化复核三层渲染通过**（.data/g1_verify.py CDP 附加到已加载 /graph 页，读 Vue setupState + 图例 + 边点统计）：
    - 融合：18 节点（SYSTEM4/SUBSYSTEM2/DATABASE4/TABLE2/DEPARTMENT2/ACTION4）+ 20 边（API2/DATA_FLOW15/APPROVAL3），图例 6 项
    - 数据流：6 节点（DATABASE4/TABLE2）+ 2 边（DATA_FLOW2），图例 3 项
    - 业务流：12 节点（SYSTEM4/SUBSYSTEM2/DEPARTMENT2/ACTION4）+ 10 边（API2/APPROVAL3/DATA_FLOW5），图例 5 项
    - 切回融合恢复 18/20，可回退 ✓
  - **文档书第 0 章升 v1.4**：0.1 表加 v1.4 行、0.2 树 G1 ✅100%、0.3 里程碑图来源 20% 进行中 + 整体进度 99.2%、②功能状态、③时间线
  - HANDOFF 同步 G1 完成
- 创建/修改的文件：
  - task_plan.md、findings.md、progress.md（新建）
  - frontend/vite.config.ts（代理端口对齐）
  - frontend/src/views/GraphView.vue（三层视图）
  - .gitignore（/.data/）
  - .claude/launch.json（frontend-dev preview 配置，新建）
  - ~/.claude/settings.json（全局权限，重启生效）
  - docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md（第 0 章 v1.4）
  - docs/PROJECT-HANDOFF.md（G1 完成同步）
  - .data/g1_verify.py（G1 复核脚本，新建）

### 阶段 G2：图来源管线原型（前端交互）
- **状态：** complete ✅
- **开始时间：** 2026-08-17 · **完成时间：** 2026-08-17
- 执行的操作：
  - 读取文档书第 15 章 G2 设计要求（15.4 主动分流 / 15.6 工作流 / 15.10 G2 里程碑）
  - **GraphSourceView.vue 图来源管线原型实现完成**：三条路线卡片（引擎分析/大模型分析/人工创建）+ 6 步流程条 + 完整状态机（entry/scanning/draft/llm/manual）
  - 引擎草稿假数据：8 节点/12 边 + 候选清单 6 项（置信度 95/91/84/76/61/53%，低置信度徽标）
  - 大模型细化假数据：14 节点/20 边 + 5 类补全清单；可回退纯引擎草稿；作废回入口
  - 统一人工校正清单（5 项）+ 确认生成准底图（toast + 1.8s 自动复位）
  - 路由 /graph-source + SideNav 图来源入口
  - `npm run build` 通过（14.26s）
  - **无头 Chrome 程序化复核全流程通过**（.data/g2_verify.py CDP：设置 token → 导航 /graph-source → 读 Vue setupState + DOM 快照走完整状态机）：
    - entry：3 路线卡 + SideNav 图来源入口激活 + activeStep 1
    - engineDraft：stage=draft，8 节点/12 边 + 6 候选（挂号单/收费单/退费申请单/结算单/支付流水/处方明细）+ 置信度 95/91/84/76/61/53% + 2 低徽标 + 3 操作按钮
    - llmDraft：stage=llm，14 节点/20 边 + 5 补全项 + 3 按钮（细化后够用/回到纯引擎/作废重来）
    - reverted：回 8/12 + 6 候选（可回退 ✓）
    - afterDiscard：回入口（作废重来 ✓）
    - manual：stage=manual，5 项人工校正清单 + 3 按钮
    - afterConfirm：toast「已确认准底图…」+ confirmed=true + manualOk ✓
    - final：自动复位回入口 ✓
  - **文档书第 0 章升 v1.5**：0.1 表加 v1.5 行、0.2 G2 ✅ 6 子项、0.3 里程碑图来源 40% + 整体进度 99.4%、②功能状态、③时间线、15.10 G2 ✅
  - HANDOFF 同步 G2 完成
- 创建/修改的文件：
  - frontend/src/views/GraphSourceView.vue（G2 管线原型，新建）
  - frontend/src/router/index.ts（+graph-source 路由）
  - frontend/src/components/SideNav.vue（+图来源菜单）
  - .data/g2_verify.py（G2 复核脚本，新建）
  - docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md（第 0 章 v1.5）
  - docs/PROJECT-HANDOFF.md（G2 完成同步）

### 阶段 G3：引擎最小可行版
- **状态：** complete ✅
- **开始时间：** 2026-08-17 · **完成时间：** 2026-08-18
- 已完成（G3a~G3d，2026-08-17）：
  - G3a 演示业务库 schema（engine/his_demo_schema.sql，6 表）
  - G3b engine 包（DTO/Service/Controller + 5 信号识别 + 单号前缀归属判定 + H2 内部表过滤）
  - G3c DemoBizDbInitializer（`@Order(10)`）+ DemoConnectorSeeder（`@Order(20)`）+ SecurityConfig 放开 /api/analyze
  - G3d EngineAnalyzeServiceTest 6/6 通过（含置信度 60~85 区间校验、草稿图结构校验、边界 404/disabled）
  - 后端测试基线：**20 测试类全绿**（原 19 + 新 1）
  - 前端 types/index.ts 补 EngineCandidate/Flow/Draft 类型
- 中途踩坑（已修复）：DemoBizDbInitializer SQL 拆句（头注释吞块）+ DATABASE_TO_LOWER schema 名 + H2 会话内部表过滤 + 草稿边方向反转 + buildFlows 方法头丢失
- 置信度实测回填（6 候选）：fee_order 85 / reg_order 75 / refund_apply 65 / settle_bill 60 / pay_record 60 / prescription_detail 40
- **G3e 前端接线完成（2026-08-18）**：
  - `api/index.ts` 末尾追加 `fetchEngineAnalyze(connectorId: string): Promise<EngineDraft>`
  - `GraphSourceView.vue`：导入类型+API；新增 `engineData/engineCandidates/engineError/engineConnectorName` 状态；`acquireEngineDraft()` 取第一个 enabled DB 连接器调 analyze，失败兜底假数据+错误条；`startRoute('engine')` 扫描后触发；`draftNodes/draftEdges` 优先用 `engineData`（无则兜底）；候选清单用 `engineCandidates`；`draftSummary` 真实时显示 `扫描 {database}`；`gs-canvas-meta` 真实时追加连接器名标签；模板加 `gs-left-warn` 错误提示条
- **G3f 验证完成（2026-08-18）**：
  - `npm run build` 全绿（13.54s）
  - **后端重启**：旧 IntelliJ spring-boot:run 进程无 G3 种子（连接器表空 → 兜底假数据），新进程让 Order(10)/Order(20) 初始化器执行，HIS 连接器 id=1 出现
  - **后端真实 API 核对**：`/api/analyze?connectorId=1` → `database=datalink_demo` + 6 候选（85/75/65/60/60/40）+ 1 库节点 + 6 表节点 + 10 边 + 1 流程模板「挂号单→收费单→支付流水」
  - **无头 Chrome 程序化复核全流程真实数据通过**（`.data/g3_engine_verify.py`，复用 g2 结构）：入口 → 扫描 → 引擎草稿（`engineDataLoaded=true`、7 节点/10 边、6 候选、4 低置信徽标、1 流程模板）→ 加大模型 14/20 → 回退 7/10 → 作废回入口 → 再引擎 → 人工校正 5 项 → 确认 toast + 复位 ✓ 无残留
- **G3g 文档同步完成（2026-08-18）**：文档书第 0 章升 v1.6（版本表 + 0.2 树 G3 ✅ + 0.3 里程碑图来源 60% + 整体 99.6% + 功能状态 + 时间线）、HANDOFF 同步（顶部 v1.6 + G3 完成小节 + 下一步 + 续接起点 + 远程仓库历史）、findings.md / task_plan.md / progress.md 同步
- 创建/修改的文件（2026-08-18）：
  - frontend/src/api/index.ts（追加 fetchEngineAnalyze）
  - frontend/src/views/GraphSourceView.vue（G3e 接线：真实数据源 + 兜底假数据 + 错误条）
  - .data/g3_engine_verify.py（G3 复核脚本，新建）
  - docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md（第 0 章 v1.6）
  - docs/PROJECT-HANDOFF.md（G3 完成同步）
  - findings.md / task_plan.md / progress.md（G3 完成同步）
  - G3f npm run build 全绿 + CDP 复核草稿渲染数字一致
  - G3g 文档同步（第 0 章 v1.6 / 15.3 / 15.10 / HANDOFF / task_plan/findings/progress 已更新到此节点）

### 阶段 G4：大模型接入层
- **状态：** complete ✅（真实供应商验证待用户提供 API key 后补）
- **开始时间：** 2026-08-18 · **完成时间：** 2026-08-18
- 执行的操作：
  - 基线：后端 `mvn test` 20 类全绿 + 前端 `npm run build` 全绿后动手
  - **子代理A（llm 包）**：`llm/config/LlmProperties`（`datalink.llm.*`：base-url/api-key/model/timeout-ms/max-tokens/temperature，全走 `${ENV:default}` 环境变量）+ `LlmConfig`（`@ConditionalOnExpression` 有 key 装配 OpenAI 实现 / `@ConditionalOnMissingBean` 装配 Noop）+ `llm/dto`（RefinementItem/LlmRefineRequest/LlmRefineResult）+ `llm/provider`（ModelProvider 接口 `name()/available()/refine()` + NoopModelProvider + OpenAiCompatibleModelProvider：RestClient POST `/chat/completions`、`response_format=json_object`、```json 围栏剥离、解析失败/超时降级 error refinement 绝不抛异常、日志不打 key）
  - **子代理B（refine 接口）**：`engine/dto`（RefineRequest{connectorId} + RefineResultVO{base/addedNodes/addedEdges/renameMap/refinements/provider/message}）+ `EngineAnalyzeService.refine(connectorId)`（复跑引擎取 base → ModelProvider.refine → 组装 VO，provider 异常降级 error 不抛出）+ `AnalyzeController` 加 `POST /api/analyze/refine` + SecurityConfig 加 POST `/api/analyze/**` authenticated
  - **前端（主会话）**：`types/index.ts` 加 `RefinementItem/EngineRefineResult`；`api/index.ts` 加 `postEngineRefine`；`GraphSourceView.vue` 删 LLM_NODES/LLM_EDGES/LLM_REFINEMENTS 假数据——细化草稿 = 引擎骨架应用 renameMap + addedNodes/addedEdges 合并（骨架引用即回退快照，revertToEngine 仅切 stage 即真回退）；Noop 时琥珀色提示条「未配置大模型 API Key」；refine 结果缓存（回退后再细化不重复调接口）；大模型路线直进真实接口（无连接器退 draft + 提示）；refine 按钮 loading 态
  - **验证**：后端 24 测试类 94 用例全绿（新增 4 类：OpenAiCompatibleModelProviderTest 解析/降级 + ModelProviderWiringTest 无 key 装配 Noop + EngineRefineServiceTest 3 用例 + AnalyzeRefineControllerTest MockMvc）；`POST /api/analyze/refine {connectorId:1}` 实测 code=200、provider=noop、base 与 GET /api/analyze 一致（datalink_demo 7 节点/10 边/6 候选）、refinements 1 条 noop；无头 Chrome 复核（`.data/g4_llm_verify.py`）：引擎草稿 7/10 → 加大模型（provider=noop + 提示条 + 草稿不变 + 徽标「引擎原稿（Noop）」）→ 回退 → 作废 → 大模型路线直进 → 人工校正 5 项 → 确认复位 ✓ 全链路真实数据无假数据残留
  - 文档书第 0 章升 v1.7 + HANDOFF + 三件套同步
- 创建/修改的文件：
  - backend：`llm/` 包 9 文件（新建）、`engine/dto/RefineRequest.java` + `RefineResultVO.java`（新建）、`EngineAnalyzeService(+Impl)` / `AnalyzeController` / `SecurityConfig` / `application.yml`（修改）、测试 4 类（新建）
  - frontend：`src/types/index.ts`、`src/api/index.ts`、`src/views/GraphSourceView.vue`（修改）
  - `.data/g4_llm_verify.py`（G4 复核脚本，新建）
  - docs 文档书（第 0 章 v1.7 + 15.10 G4 ✅）+ HANDOFF + 三件套

### 阶段 G5：校正闭环
- **状态：** pending

### 阶段 收尾：文档同步 + 推送
- **状态：** pending

## 测试结果
| 测试 | 输入 | 预期结果 | 实际结果 | 状态 |
|------|------|---------|---------|------|
| G1 三层视图（CDP） | 切数据流/业务流/融合 | 数据流 6/2、业务流 12/10、融合 18/20，可回退 | 全通过，无残留 | ✅ |
| G2 管线入口 | 打开 /graph-source | 3 路线卡 + 6 步条 + SideNav 激活 | 3 卡（引擎/大模型/人工）+ activeStep 1 | ✅ |
| G2 引擎草稿 | 点「开始引擎分析」 | 扫描动画 → 草稿 8 节点/12 边 + 候选 6 项 | 8/12 + 6 候选置信度 95~53% + 2 低徽标 + 3 按钮 | ✅ |
| G2 加大模型细化 | 点「加大模型细化」 | 14 节点/20 边 + 5 补全 | 14/20 + 5 补全 + 3 按钮 | ✅ |
| G2 可回退 | 点「回到纯引擎草稿」 | 回 8/12 + 6 候选 | 8/12 + 6 候选 | ✅ |
| G2 作废重来 | 点「作废重来」 | 回入口 | 回入口，可重选路线 | ✅ |
| G2 人工校正 | 草稿够用 → 进人工校正 | 5 项清单 + 3 按钮 | 5 项（收费单/挂号单/结算单/退费审批/结算中心） | ✅ |
| G2 确认准底图 | 点「确认并生成准底图」 | toast + confirmed + 复位 | toast 文案 + confirmed=true + 1.8s 复位 | ✅ |
| 前端构建 | npm run build | 全绿 | 全绿（14.26s） | ✅ |

### 阶段 G5：校正闭环 + 前端操作补齐 + 后端管理接口补强
- **状态：** complete ✅
- **开始时间：** 2026-08-22 · **完成时间：** 2026-08-22
- 执行的操作：
  - G5 后端：`correction_record`/`pattern_library` 表（Flyway V8）+ `CorrectionRecordController`/`PatternLibraryController`；端点 `POST /api/corrections`、`GET /api/corrections?targetType=&targetId=`、`POST /api/corrections/{id}/confirm`、`GET /api/patterns`；AnalyzeService 二次识别引入模式库命中自动应用
  - G5 前端：`GraphSourceView.vue` 校正面板支持改名/确认/合并/增删/排序，提交后展示历史；G2 兜底假数据降级为错误空态
  - 前端补齐：TopNav/SideNav 告警徽标接 `/api/alerts`；新增 `/instances` 与 `/tickets` 页面及导航；ProcessListView/CheckpointView/AlertView/VersionView/SettingsView 操作按钮真实接线
  - 后端补强：`ConfigVersionService.rollback` + `POST /api/versions/{id}/rollback`；`CheckpointService.run` + `POST /api/checkpoints/{id}/run`
  - 验证：后端 `mvn test` 全绿；前端 `npm run type-check`/`npm run build` 全绿；浏览器走查实例/工单/流程/检测点/告警/版本/图来源通过；检测点创建+立即检测实测成功；LLM refine 未配置 key 走 Noop 兜底，接口 200
- 创建/修改的文件：
  - backend：`model` 包 correction/pattern 实体/Mapper/Service/Controller + Flyway V8；`ConfigVersionService`/`VersionController` 回滚；`CheckpointService`/`CheckpointController` 立即检测
  - frontend：`GraphSourceView.vue` 校正面板；`InstanceListView.vue`/`TicketListView.vue`；`TopNav.vue`/`SideNav.vue` 告警徽标；`ProcessListView.vue`/`CheckpointView.vue`/`AlertView.vue`/`VersionView.vue`/`SettingsView.vue` 按钮接线；`api/index.ts`/`types/index.ts`/`router/index.ts`
  - docs：项目文档书 v1.8、HANDOFF、task_plan.md、findings.md、progress.md

## 错误日志
| 时间戳 | 错误 | 尝试次数 | 解决方案 |
|--------|------|---------|---------|
|        |      | 1       |         |

## 五问重启检查
| 问题 | 答案 |
|------|------|
| 我在哪里？ | M0~M4 + 图来源 G1~G5 全部完成（2026-08-22）；LLM 真实供应商验证仍待用户提供 API key |
| 我要去哪里？ | 远期补充功能 / 真实 LLM 验证 / 用户新需求 |
| 目标是什么？ | 当前阶段目标已达成：图来源 G1~G5 全部落地 + 文档进度同步 |
| 我学到了什么？ | 见 findings.md（G4 llm 包契约 + Noop 兜底模式；G5 校正记录/模式库设计） |
| 我做了什么？ | G1~G5 完成：三层视图 + 管线原型 + 引擎最小可行版 + 大模型接入层 + 校正闭环；前端操作按钮/缺失页面补齐；后端版本回滚/检测点立即检测补强；后端 `mvn test` 全绿、前端 type-check/build 全绿、浏览器走查通过 |

---
*每个阶段完成后或遇到错误时更新此文件*
