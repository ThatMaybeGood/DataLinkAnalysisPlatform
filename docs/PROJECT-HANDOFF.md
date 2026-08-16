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
- **监控时效**：准实时轮询（分钟级，Spring @Scheduled）
- **命名体系**：所有对象主显示名 + 多别名，全局搜索单号/别名通吃
- 13 项已确认决策详见文档书第 14 节

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
# 前端联调：后端先起，再 cd frontend && npm run dev（vite 已代理 /api→8080）
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

## 六、下一步（M2 · 摸瓜：监控 + 排查）

M1 陈列已交付：数据池 + 建模域 CRUD + 关系网接口 + 前端画布接真实数据。剩余前端 mock（实例/告警/看板/版本）属 M2。

M2 按顺序：
1. **检测点**：checkpoint/check_result 接口 + 默认/自定义检测点配置（前端 CheckpointView 接真实数据）
2. **实例追踪**：instance/instance_node 接口 + 卡点定位（前端实例 mock 替换；feature 推断/人工标记）
3. **顺藤摸瓜排查**：问题站点上下游高亮 + 溯源/影响面（图算法在 `graph` 服务扩展）
4. **告警 + 工单**：alert/ticket 接口 + 告警中心接真实数据（前端 AlertView）
5. **看板**：`/api/dashboard/stats` 聚合接口（前端 DashboardView 替换 mockDashboardStats）
6. **等级预警/干预**：level L1-L4 驱动（M2 末）
7. M1 末/ M2 初收紧 Security：按 RBAC 授权（当前 `anyRequest().permitAll()`）
8. 远期：PostgreSQL 实机验证、数据池接 Oracle、版本留痕 config_version 完善

## 七、用户工作偏好（重要）

- 项目内操作**自主执行，不逐次确认**；**删除文件先确认**
- 开发顺序：先前端看效果 → 再后端（当前后端 M0 完成，进 M1）
- 中文沟通；用户习惯用类比（导航/血管/地铁）描述需求
- 用户要求：后端开发多用子代理并行提速；对话接近上限时先「打包对话」（即更新本文档）再继续，避免半途接不上

## 八、续接起点

**当前任务 = M2 · 摸瓜（监控 + 排查）**。M0（脚手架/双库）+ M1（数据池 + 建模域 + 关系网接口对接画布）均已交付并推送到 GitHub。
新会话第一步：读本文档 → `cd backend && mvn spring-boot:run` 起服务 → 从检测点/实例追踪开始。
前端已可用：http://localhost:5173（关系网画布显示真实种子数据，数据接入页管理数据池）。
