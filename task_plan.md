# 任务计划：图来源 G1~G5 实现

## 目标
实现文档书第 15 章「图来源与自动/半自动分析」——三条路线（引擎/大模型/人工）+ 三层视图 + 校正闭环，按 G1→G5 顺序全部落地，并同步更新文档书第 0 章与 HANDOFF。

## 当前阶段
阶段 G5：校正闭环（G4 已完成，G5 待开始）

## 各阶段

### 阶段 G1：三层视图定型（前端投影）
- [x] 节点类型 → 视图映射规则（数据流/业务流/融合）
- [x] GraphView 加视图切换 tabs（前端投影，不另起"层"概念）
- [x] 按视图过滤/分组节点展示
- [x] npm run build 通过
- [x] 浏览器/无头 Chrome 复核三层视图渲染（CDP 程序化：融合 18 节点/20 边 · 数据流 6/2 · 业务流 12/10，可回退）
- [x] 文档书第 0 章 G1 勾选（v1.4，整体 99.2%）+ HANDOFF 同步
- **状态：** complete ✅

### 阶段 G2：图来源管线原型（前端交互）
- [x] 图来源入口页/面板：三条路线选择（引擎分析 主干 / 大模型分析 增强 / 人工创建 兜底 + 6 步流程条）
- [x] 引擎草稿展示（假数据：8 节点/12 边 + 候选清单 6 项含置信度/低徽标）
- [x] 主动分流交互：够用 / 加大模型 / 作废重来（可回退：加大模型→回纯引擎草稿、作废→回入口）
- [x] 统一人工校正清单 + 确认生成准底图（toast + 自动复位）
- [x] npm run build 通过
- [x] 无头 Chrome 程序化复核全流程状态机（入口→扫描→引擎草稿→加大模型→回退→作废→再引擎→人工校正→确认→复位）
- [x] 文档书第 0 章升 v1.5（G2 ✅，整体 99.4%，图来源 40%）+ HANDOFF 同步
- **状态：** complete ✅

### 阶段 G3：引擎最小可行版（后端 + 前端）
- [x] 复用 DbDialect + ConnectionPoolRegistry 连库扫描
- [x] 单据模式库识别：主键 / 编码号 / 状态 / 时间 / 引用链 / 主子表（5 信号打分 + 单号前缀归属判定 + H2 内部表过滤）
- [x] 产出候选单据清单 + 置信度 + 状态机 + 流程模板（主表置信度 60~85 实测回填：fee_order 85 / reg_order 75 / refund_apply 65 / settle_bill 60 / pay_record 60 / prescription_detail 40）
- [x] /api/analyze 接口（GET，已登录即可访问，SecurityConfig 放行）
- [x] 拿真实 H2 演示库实测，回填置信度；集成测试 6/6 通过；后端测试基线 20 全绿
- [x] 前端 types 定义（EngineCandidate/Flow/Draft）
- [x] G3e 前端接线（api/index.ts 追加 `fetchEngineAnalyze`；GraphSourceView.vue 接入真实数据源 + 兜底假数据）
- [x] G3f 验证基线（npm run build 全绿 + CDP 复核草稿渲染数字一致：7 节点/10 边 + 6 候选 + 1 流程模板，全流程真实数据）
- [x] G3g 文档同步（第 0 章 v1.6 / 15.3 / 15.10 / HANDOFF 已更新到此节点，明日收尾）
- **状态：** complete ✅（2026-08-18）

### 阶段 G4：大模型接入层（后端 + 前端）
- [x] ModelProvider 可插拔接口（`llm` 包：`name()/available()/refine()`；Noop 兜底 + OpenAI 兼容实现，DeepSeek/通义/Claude/GPT 靠配置切换）
- [x] 配置 API Key/地址/参数（`datalink.llm.*` 全走环境变量 `${LLM_API_KEY:}` 等，不硬编码；有 key 条件装配真实 Provider，无 key 装配 Noop）
- [x] 引擎草稿 + 大模型细化接口（`POST /api/analyze/refine`：传 connectorId 复跑引擎 → 语义补全 → 骨架 base + 增量 addedNodes/addedEdges/renameMap + 五类细化清单 + provider 标识；异常降级不抛出）
- [x] 前端 GraphSourceView「加大模型细化」换真实接口（删 LLM 假数据；骨架改名 + 增量合并 + 一键回退 + Noop 提示条）
- [x] 验证：后端 24 测试类 94 用例全绿（新增 4 测试类）；refine 接口实测 Noop 路径；无头 Chrome 复核全链路
- [ ] 至少一个真实供应商验证 + 切换模型对比（⏳ 待用户提供 API key：设 `LLM_API_KEY`/`LLM_BASE_URL`/`LLM_MODEL` 即可）
- **状态：** complete ✅（2026-08-18；真实供应商验证待 key 后补）

### 阶段 G5：校正闭环（后端）
- [ ] 校正记录表（改名/确认类型/合并/增删/顺序）
- [ ] 校正记录留存接口
- [ ] 模式库沉淀（最简版）
- [ ] 二次识别同一库校正量下降验证
- **状态：** pending

### 阶段 收尾：文档同步 + 推送
- [ ] 文档书第 0 章进度更新（G1~G5 勾选 + 百分比 + 时间）
- [ ] HANDOFF 同步
- [ ] 后端测试全绿 + 前端 build 全绿
- **状态：** pending

## 关键问题
1. 三层视图字段放哪？——设计定"先做前端投影"，不另起层概念，Node 实体可不动（用 nodeType/relationType/process.scene 推导）。
2. G3 引擎扫描目标库？——复用现有 connector 连接池，先扫种子里的 H2 业务库演示。
3. G4 供应商验证哪个？——国内环境先试 DeepSeek 或通义（key 由用户提供）；不可用时做可插拔空实现 + 假数据兜底。
4. 校正闭环 G5 最简版做到什么程度？——记录留存 + 沉淀接口，反馈学习后置。

## 已做决策
| 决策 | 理由 |
|------|------|
| G1 只做前端投影，不加 Node 字段 | 设计 15.7 明确"先做前端投影，代价最小；Layer 概念后置" |
| 视图映射靠 nodeType/relationType 推导 | Node 实体现无三层字段，推导零迁移成本 |
| 按 G1→G5 顺序，每阶段验证并更新文档 | 设计 15.10 明确顺序原则：先定型图怎么用→再看图怎么来→引擎→大模型→沉淀 |
| G3 复用 DbDialect+ConnectionPoolRegistry | 探索报告确认引擎可直接复用外部库扫描能力 |
| 后端实现多用子代理并行，前端我亲自做 | 前端改动需精确保留现有交互（G6/3D/地铁图契约一致），后端模块相对独立 |

## 遇到的错误
| 错误 | 尝试次数 | 解决方案 |
|------|---------|---------|
| DemoBizDbInitializer 首次启动报 `Table "reg_order" not found` | 1 | 拆句器按 `;\n` 切，头注释块被吞掉；改用逐行剥注释 → 行尾 `;` 切 |
| 同一 JVM 多测试上下文共享 datalink_demo 重复 CREATE | 1 | `INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'` 在 DATABASE_TO_LOWER=TRUE 下匹配失败；改单表级检查 |
| EngineAnalyzeServiceImpl 输出 10 候选（预期 6） | 1 | H2 会话内部表 locks/query_statistics/sessions/session_state 被当成业务表；加 META_TABLES 过滤 |
| 草稿边方向反了 | 1 | 原 "同列名" 判定多表共享时方向混乱；改用编码号前缀匹配（`fee_no` → 表名 `fee_*`） |
| buildFlows 方法头丢失导致编译失败 | 1 | linkReferences 重构时流程链代码嵌入末尾未抽方法；独立为 private buildFlows(List) |
| frontend src/api/index.ts Edit 失败（File has not been read yet） | 1 | 编辑前未 Read；改用 append 到文件末尾 |
| CDP 复核时引擎接口未触发，显示「未找到已启用的 DB 连接器」 | 1 | 旧后端进程（IntelliJ spring-boot:run）未跑 G3 种子；重启 `mvn spring-boot:run` 让 Order(10)/Order(20) 初始化器执行 |
| G4 CDP 脚本 attach 后报「Inspected target navigated or closed」 | 1 | attach 的目标页在导航后会话失效；改 `Target.createTarget` 新建 about:blank 页再 attach |
| G4 CDP 脚本在 about:blank 读 localStorage 报 SecurityError | 1 | 先导航到 5173 页面再执行登录写 token，最后重新导航让 SPA 带 token 加载 |
| AnalyzeRefineControllerTest 断言「无 body → 4xx」失败 | 1 | GlobalExceptionHandler 兜底 Exception 统一 HTTP 200 + body.code=500；改断言 `$.code != 200`（行为等价拒绝，不动全局处理器） |

## 备注
- 每完成一个 G：更新文档书第 0 章（勾选/百分比/时间）+ HANDOFF，再进下一个 G
- 验证基线：后端 `mvn test` 全绿、前端 `npm run build` 全绿、无头 Chrome 复核渲染
- 用户偏好：先前端看效果→再后端；项目内自主执行不逐次确认；删除文件先确认
- 前端 ports：vite dev 代理 /api→8080（注意 application.yml 已改成 28080，需核实 vite.config 代理目标）
