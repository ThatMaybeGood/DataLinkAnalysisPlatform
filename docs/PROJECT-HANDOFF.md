# 项目续接文档（HANDOFF）

> 本文件是「对话打包」：**新会话先读本文件即可无缝续接**。主文档是项目文档书 v1.1，本文件用于快速恢复进度、记录已验证事实与下一步起点。

## 一、这是什么项目

**数据关联与业务流程监控分析平台**（DataLinkAnalysisPlatform）—— 企业级 Web 平台，把系统/数据库/部门/业务动作画成「血管网式」关系网，给同一起终点提供多条路线选择，系统出问题时能「顺藤摸瓜」找到根因和影响面。

**核心三件事**：陈列（关系网展示）· 择路（多路线）· 摸瓜（问题排查）。

## 二、关键文档（都在 `docs/`）

| 文档 | 路径 | 用途 |
| --- | --- | --- |
| **项目文档书 v1.1** | `docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md` | ⭐ 交付基准：14 章 + 4 附录，含完整 DDL/RBAC/用户故事/页面清单 |
| 讨论草稿 | `docs/superpowers/specs/2026-08-15-数据关联流向分析平台-设计方案.md` | 需求演进过程记录 |
| 前端说明 | `frontend/README.md`、`frontend/DEPLOY.md` | 前端开发与部署 |
| 后端说明 | `backend/README.md`、`backend/DEPLOY.md` | **后端开发与部署（含双数据库模式）** |
| 本文件 | `docs/PROJECT-HANDOFF.md` | 续接入口 |

## 三、已确认的技术栈与决策

- **前端（已完成）**：Vue 3 + TypeScript + Vite + AntV G6 5.x（2D），远期 3d-force-graph（3D）
- **后端（已完成 M0）**：**Java 17 + Spring Boot 3.3.5 + MyBatis-Plus 3.5.7 + Flyway + JWT**
- **数据库双模式**（用户 2026-08-16 定稿）：
  - **离线本地模式（默认）**：嵌入式 H2 文件库，零安装、开箱即用 → `mvn spring-boot:run`
  - **部署模式**：MySQL 8 → `--spring.profiles.active=mysql`
- **数据池模块（M1 已交付）**：独立的外部数据库连接管理（MySQL/PostgreSQL/H2，方言可插拔），前端「数据接入」页可视化增删改/测试/切换/浏览；配置存平台库，预留分布式拓展（用户明确「数据池接入不同数据库」）
- **图来源多路线 + 大模型辅助（v1.2 新增决策，2026-08-16）**：新增「自动/半自动分析出图」能力——图来源三条路线（引擎分析 / 大模型分析 / 人工创建），入口处选择；引擎分析后由用户在前端**主动选择**是否加一道大模型细化（够用直接校正 / 不够加大模型 / 太乱作废重来）；统一人工校正为准；大模型通过可插拔接入层随意切换；引擎定骨架、大模型管语义、人拍板，不接大模型也照跑。**详见文档书第 15 章**——含三层视图（数据流/业务流/融合）、校正闭环沉淀、实施顺序 G1→G5（先三层视图定型 → 前端原型 → 引擎最小可行版 → 大模型接入层 → 校正闭环）。
- **监控时效**：准实时轮询（分钟级，Spring @Scheduled）
- **命名体系**：所有对象主显示名 + 多别名，全局搜索单号/别名通吃
- 15 项已确认决策详见文档书第 14 节

## 四、已完成进度

### 前端（✅ 完成）
- [x] 8 页面 + G6 画布 + 路线高亮 + 详情面板，`npm run build` 全绿

### 后端（✅ M0 完成，双数据库模式均已真实验证）
- [x] `backend/` Maven 工程（Spring Boot 3.3.5 / Java 17）
- [x] 统一响应 `Result<T>` + 全局异常 + 分页 `PageResult`（HTTP 200，业务码在 body.code）
- [x] 双 profile：`local`（H2 文件库 `./.data/h2`）与 `mysql`（MySQL 8，环境变量覆盖连接）
- [x] Flyway 双迁移目录：`db/migration/h2` + `db/migration/mysql` + 共用种子 `db/migration/common`
  - 21 张表（附录 A）两套方言均已建表验证；H2 保留字 `value` 用反引号 + `NON_KEYWORDS=VALUE`
  - ⚠️ **修正源文档 DDL 缺陷**：`config_version` 原缺 `PRIMARY KEY(id)`，MySQL 报错 1075，已在迁移与文档书两处补上
- [x] 种子数据：5 角色 + admin（admin123）+ 示例「订单支付流程」路网（8 节点/7 关系/1 流程/2 路线/2 别名）
- [x] 认证鉴权：`POST /api/auth/login`（JWT + BCrypt + RBAC join），M0 阶段 Security **全放行**（前端仍用 mock）
- [x] 健康探活：`GET /api/health`（含 db 探活）
- [x] `mvn test` 通过；H2 与 MySQL 两模式均实测：health UP / 登录签发 JWT / 种子数据正确
- [x] Docker：`backend/Dockerfile` + 根 `docker-compose.yml`（nginx+backend+mysql8 三件套）
- [x] 部署文档：`backend/DEPLOY.md`（H2/MySQL/容器化/多库扩展/FAQ）

### 数据池模块（✅ M1 完成，H2 + MySQL 双实测通过）
- [x] 独立 `datasource` 包：方言可插拔（`DbDialect`：MySQL/PostgreSQL/H2）+ HikariCP 连接池注册表 + AES-GCM 密码加密（`datalink.crypto.key`）
- [x] `connector` 表 V3 扩展（`db/migration/common/V3__connector_expand.sql`：database_name/schema_name/is_active/last_test_status/last_test_time）
- [x] 9 个 REST 接口：`GET/POST/PUT/DELETE /api/connectors`、`/{id}/test`、`/{id}/activate`、`/{id}/tables`、`/{id}/tables/{table}/preview`
- [x] 前端「数据接入」页接真实接口（列表/新建编辑/测试连通/设为当前/浏览库表/数据预览）
- [x] 19 个后端测试全绿；H2 与 MySQL 均实测：建连接→测试→唯一激活→浏览22表→预览行数据→错误密码拒绝
- [x] PostgreSQL：驱动+方言+测试连接路径与另两者同构，**待实机验证**（本地无 PG 服务）
- [x] 分布式预留：配置存平台库（全局共享），各节点各自建连接池（无状态）；广播机制留扩展点未实现

### 建模域（✅ M1 完成，端到端实测通过）
- [x] `model` 包：Node/Relation/Process/Route/RouteNode/Alias 实体+Mapper+DTO（**id 统一字符串**，对齐前端契约）
- [x] 读接口（装配）：`GET /api/nodes`（18）、`GET /api/edges`（20）、`GET /api/processes`（2，含 start/end 名、nodeCount/routeCount/instanceStats）、`GET /api/routes`（5，nodeIds 有序）
- [x] 建模 CRUD：node/relation/process/route 增删改（route 级联写 route_node，deleteProcess 级联删路线）
- [x] 全局搜索：`GET /api/search?q=`（节点/流程/路线按 code/name/alias 通吃，实测别名命中）
- [x] V4 种子扩充：付款流程（风控/支付分支）路网，与前端原型场景对齐
- [x] 前端 GraphView/ProcessListView 已接真实接口（异步加载+加载/错误态），`npm run build` 全绿
- [x] 后端 35 个测试全绿；端到端实测：读接口/搜索/CRUD 级联/前端代理全部通过

### 监控域 · 摸瓜（✅ M2 第一批完成，端到端实测通过）
- [x] `monitor` 包：Instance/InstanceNode/Checkpoint/CheckResult/Alert/Ticket 实体+Mapper+DTO+Service+Controller
- [x] 实例追踪：`GET /api/instances`（分页+状态过滤，含流程/路线/进度/当前站）、`GET /api/instances/{id}/nodes`、POST/PUT
- [x] 检测点：`GET /api/checkpoints?nodeId=`（含最近检测状态）、POST/PUT/DELETE
- [x] 告警：`GET /api/alerts?status=`（含目标名）、`POST /api/alerts/{id}/resolve`
- [x] 工单：`GET /api/tickets`、POST（从告警生成）
- [x] **★顺藤摸瓜图算法**：`GET /api/graph/{nodeId}/trace`（有向 BFS 上下游各 2 层，节点附检测点状态）、`GET /api/graph/{nodeId}/routes`（所在路线）
- [x] 看板聚合：`GET /api/dashboard/stats`（流程/运行中/今日完成/告警/卡住/检测覆盖/均耗时/慢节点Top/8h趋势）
- [x] V5 种子：6 实例/39 站/11 检测点/11 结果/5 告警/3 工单（H2 实测验证）
- [x] 前端：工作台看板/告警中心/检测点接真实接口；关系网**排查模式**（点节点显示上游绿/下游红 + 侧栏列表）
- [x] 后端 **49 测试全绿**；端到端：摸瓜 trace/看板/告警/实例/检测点全部通过（检测点 FAIL→告警→实例卡住→trace 溯源闭环）

### M2 收尾 + M3 择路深化（✅ 完成，端到端实测通过）
- [x] **任意两点路径查询**：`GET /api/graph/path?from=&to=`（DFS 简单路径，实测付款发起→付款完成 6 条走法）
- [x] **影响面分析**：`GET /api/graph/{nodeId}/impact`（下游节点 + 受影响实例 + 受影响路线，实测支付系统→3 路线+3 实例）
- [x] **等级预警/干预**：`POST /api/alerts` 触发 L1-L4 处置规则（L1→AUTO_ACTION,TICKET,NOTIFY / L2→TICKET,NOTIFY / L3→NOTIFY / L4→RECORD）+ **自动生成工单**（实测新建 L1 告警→自动建单）
- [x] **工单状态流转**：`PUT /api/tickets/{id}`（OPEN→PROCESSING→RESOLVED + 指派，实测流转并置 resolvedAt）
- [x] **配置版本留痕**：建模 CRUD（node/process/route）自动写 config_version 快照；`GET /api/versions`（分页+targetType 过滤，实测递增）
- [x] 前端：版本页真实数据 + 关系网路径查询（点击逐站聚焦）/影响面面板 + 工单 API 对齐
- [x] 后端 **56 测试全绿**；前端 `npm run build` 全绿
- ⏸ 延后（诚实说明）：**CMDB 连接器 / 开放 API / 流向动画 / 3D**（远期）

### RBAC 安全收紧（✅ 完成，端到端实测通过）
- [x] SecurityConfig：建模/数据池写操作→ADMIN/MODELER，告警/工单写→ADMIN/OPERATOR/ONCALL，其余需登录；login/health/swagger/actuator 公开
- [x] 401/403 统一 JSON（`{"code":401/403,...}`）；JWT 过滤器加载用户角色（`ROLE_<code>`）
- [x] `GET /api/auth/me`；版本留痕 operator 取当前登录用户
- [x] 前端：登录页 + 路由守卫 + `apiFetch` 自动带 token + 401 跳登录 + 顶栏用户/登出
- [x] V6 种子：只读用户 viewer/viewer123（VIEWER 角色）
- [x] 后端 **61 测试全绿**（含 AuthSecurityTest：无 token 401 / admin 200 / viewer 写 403 / viewer 读 200 / me 角色）；端到端实测通过

### M3 补充（✅ 完成，端到端实测通过）
- [x] **CMDB 连接器**：数据池新增 HTTP/API 型连接器（connectorType=CMDB）——`POST /{id}/test`（HTTP 连通，dbVersion=CMDB API）、`/sync`（采集资产生成候选）、`/candidates`（预览）、`/import`（一键导入 node，按 code 判重）；实测本地 CMDB 桩采集 3 资产→导入 3 节点
- [x] **开放 API**：`/api/open/**`（POST 上报实例（按 bizNo 幂等）、GET 流程/节点、POST 触发检测）+ `X-API-Key` Token 鉴权（`datalink.openapi.token`，独立于登录 JWT，无效 key→401）；实测无 key 401 / 有效 key 200 / 上报幂等
- [x] 修复：SaveConnectorRequest 支持 connectorType、密码仅 DB 类型必填；SecurityConfig 过滤器 order
- [x] 前端可视化：数据接入页 CMDB 类型表单（apiUrl/apiKey）+ 同步/候选/导入；系统管理页开放 API 卡（Token/接口清单，仅管理员，`GET /api/system/openapi`）
- [x] 后端 **76 测试全绿**（新增 OpenApi 5 + CmdbService 7 + SystemController 3）

### 文档
- [x] 项目文档书 v1.1 + config_version 主键修正

**远程仓库当前历史**（本次将新增后端 commit）：
```
(待更新) 后端 M0：Spring Boot 脚手架 + H2/MySQL 双模式 + 21 表 Flyway + JWT 登录
03c06c4 更新 .gitignore 忽略 vite 产物与续接文档进度
0e825a9 移除 vue-tsc 误提交的 vite.config 产物
2e40dc4 前端 MVP：Vue3 + G6 关系网平台
b1cb475 文档补全 v1.1
3d9890f 初始化：项目文档书 v1.0 与 .gitignore
```

## 五、后端当前状态（M0 已交付）

### 5.1 本地离线跑起来（最快）
```bash
cd backend && mvn spring-boot:run        # 默认 local profile → 嵌入式 H2，无需任何数据库
# 探活：curl http://localhost:8080/api/health
# 文档：http://localhost:8080/swagger-ui.html
# 登录：POST /api/auth/login  {"username":"admin","password":"admin123"}
# 只读账号：viewer / viewer123（VIEWER 角色）；RBAC 已收紧（建模写=ADMIN/MODELER，告警工单=ADMIN/OPERATOR/ONCALL）
# 前端联调：后端先起，再 cd frontend && npm run dev（vite 已代理 /api→8080；前端先到登录页）
```

### 5.2 部署模式（MySQL 8）
```bash
cd backend
mvn clean package -DskipTests
java -jar target/datalink-backend-0.1.0.jar --spring.profiles.active=mysql
# 连接参数环境变量：DB_URL / DB_USER / DB_PASSWORD / DATALINK_JWT_SECRET（生产必改）
```

### 5.3 目录结构
```
backend/src/main/java/com/datalink/platform/
├── common/     # Result / ResultCode / BusinessException / GlobalExceptionHandler / PageResult
├── config/     # MybatisPlusConfig / SecurityConfig / JwtUtil / CorsConfig / JacksonConfig / OpenApiConfig
├── system/     # SysUser 实体/服务/AuthController（登录）
└── monitor/    # HealthController / HealthService
backend/src/main/resources/
├── application.yml + application-local.yml + application-mysql.yml
└── db/migration/{h2,mysql,common}/
```

### 5.4 多数据库扩展点（后续接 Oracle 等）
- Flyway 每库一套迁移目录：加 `db/migration/oracle/` + 新 profile 指定 `spring.flyway.locations`
- MyBatis-Plus 方言：`MybatisPlusConfig` 按 `datalink.db-type` 切换，加 `oracle` 分支即可
- 此机制针对**平台自身存储库**；数据池连接器（对接外部业务库）属 M1，二者不混

## 六、下一步（M4 + 远期）

M3 剩余（CMDB 连接器 + 开放 API）已交付。剩余：

**M4**：
1. **3D 视图 + 大屏**：3d-force-graph 粒子流向、深色科技风投屏
2. **排查模式 3D 深化**、流向动画

**补充/远期**：
3. Excel/日志/IoT 连接器类型、开放 API 生态（Token 管理/轮换页）
4. PostgreSQL 实机验证（方言已就绪待实机）、数据池接 Oracle（新增方言+驱动）
5. 邮件/钉钉通知渠道、定时检测调度、自动动作总开关与可回滚、路线对比页
6. 自动血缘解析、AI 辅助排查、制造场景模板
7. 制造场景模板、开放 API 生态

## 七、用户工作偏好（重要）

- 项目内操作**自主执行，不逐次确认**；**删除文件先确认**
- 开发顺序：先前端看效果 → 再后端
- 中文沟通；用户习惯用类比（导航/血管/地铁）描述需求
- 用户要求：后端开发多用子代理并行提速；对话接近上限时先「打包对话」（即更新本文档）再继续，避免半途接不上
- **文档进度记录（重要）**：每完成一个功能 / 阶段，回**文档书第 0 章**更新记录——勾选 `⬜→✅`、填百分比与完成时间、标注对应代码版本；新增需求则加一行/一组。**第 0 章是项目的「进度页码」**，新会话/换电脑都以它为进度基准。

## 八、续接起点

**当前任务 = M4（3D 视图 / 大屏）+ 补充（开放 API 生态、更多连接器类型）**。M0~M3 + RBAC + M3 补充（CMDB/开放 API 含前端可视化）均已交付并推送到 GitHub（后端 76 测试全绿）。
新会话第一步：读本文档 → `cd backend && mvn spring-boot:run` 起服务 → 浏览器打开 http://localhost:5173 登录（admin/admin123），从 M4 3D/大屏 开始。
前端已完整可用：登录页 + 关系网画布（排查/路径/影响面）+ 看板 + 告警 + 检测点 + 版本 + 数据接入（DB + CMDB 连接器可视化）+ 系统管理（运行信息/开放 API 卡）。
