# DataLink 数据关联与业务流程监控分析平台

数据关联与业务流程监控分析平台：以「血管网式关系网」为核心，提供**关系建模、流程监控、顺藤摸瓜式问题排查、图来源自动生成**的一体化分析能力。

## 功能总览

| 模块 | 说明 |
| --- | --- |
| **关系网 / 流程建模** | 站点、路网边、流程、路线全生命周期管理；配置变更自动留痕（版本历史 + 回滚） |
| **业务监控** | 实例跟踪、站点检测点、告警中心（L1–L4 等级自动处置）、工单流转 |
| **问题排查** | 顺藤摸瓜（单节点上下游 BFS 溯源）、A→B 多路径查询、节点影响面分析 |
| **图来源 · 多来源分析** | 数据池连接器即引擎来源；单选单来源 / 多选合并分析（跨库节点自动区分），切换来源各自独立；选择来源时**自动测速、失败不选中**，已选横向卡片并列 |
| **图来源 · 引擎分析（G3）** | 扫描外部数据库连接器，自动抽取单据关系，产出草稿节点/边/流程模板 |
| **图来源 · 大模型细化（G4）** | 多配置切换启用（cc-switch 式，侧边栏「大模型接入」独立页 `/llm`）；切换/选中自动测速、失败不切换，未配 Key 自动降级引擎原稿 |
| **图来源 · 分析任务** | 每次分析落一条任务（含草稿快照），可查看 / 重跑，多来源历史各自隔离 |
| **图来源 · 人工校正闭环（G5）** | 校正记录 → 确认生效 → 沉淀模式库，形成「引擎→大模型→人工」三路线 |
| **数据接入（数据池）** | 注册管理外部数据库连接（MySQL/PostgreSQL/H2，方言可插拔），测试连通、浏览库表、数据预览 |
| **开放 API** | 外部系统集成（X-API-Key 鉴权）：实例上报、流程/站点查询、触发检测 |

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.3.5 · Java 17 · MyBatis-Plus 3.5.7 · Flyway · Spring Security + JWT · springdoc-openapi |
| 前端 | Vue 3 + TypeScript · Vite 6 · AntV G6 5.x（关系网画布）· Pinia · Vue Router |
| 数据库 | 离线本地：嵌入式 **H2**（零安装）｜ 部署：**MySQL 8**（Flyway 自动迁移建表） |

## 快速开始（离线模式，默认）

> 默认 profile 为 `local`（嵌入式 H2），**无需安装任何数据库**，开箱即用。

```bash
# 1. 启动后端（Java 17+ / Maven 3.8+）
cd backend
mvn spring-boot:run
```

```bash
# 2. 另开终端启动前端（Node ≥ 20）
cd frontend
npm install
npm run dev
```

启动后访问：**http://localhost:5173**

| 入口 | 地址 |
| --- | --- |
| 前端 | <http://localhost:5173> |
| 后端 | <http://localhost:28080> |
| 后端健康探活 | <http://localhost:28080/api/health> |
| Swagger API 文档 | <http://localhost:28080/swagger-ui.html> |

**默认账号**：`admin / admin123`（全权 ADMIN+MODELER+OPERATOR）；`viewer / viewer123`（只读 VIEWER）。

> 说明：前端开发服务器已将 `/api` 代理到 `http://localhost:28080`（`frontend/vite.config.ts`），故浏览器侧为同源请求，无跨域问题。后端开发阶段 CORS 放开任意 Origin（`CorsConfig`）。

## 部署方式

### 方式一：本地离线（开发 / 演示，H2）

见上方「快速开始」。H2 数据文件保存在启动目录下 `.data/h2/datalink.mv.db`（已 git 忽略），**重启不丢**，删除即重置数据。

### 方式二：单机部署（MySQL 8）

```bash
# 1. 建库建账号（MySQL 8）
mysql -uroot -p -e "CREATE DATABASE datalink DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 打包并运行
cd backend
mvn clean package -DskipTests
java -jar target/datalink-backend-0.1.0.jar --spring.profiles.active=mysql
```

首次启动 Flyway 自动建表并写入种子数据。连接参数均可通过环境变量覆盖（见下「配置项」）。

### 方式三：Docker Compose 全栈一键（nginx + backend + mysql8）

```bash
docker compose up -d --build
```

| 服务 | 地址 |
| --- | --- |
| 前端 | <http://localhost>（nginx 托管静态资源，`/api` 反代 backend） |
| 后端 | <http://localhost:8080/api/health> |
| MySQL | localhost:3306 |

**生产必改**：`docker-compose.yml` 中的 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`DATALINK_JWT_SECRET`、`DATALINK_CRYPTO_KEY`、`DATALINK_OPENAPI_TOKEN`。

## 配置项（环境变量覆盖）

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | `local`=离线 H2；`mysql`=部署 MySQL |
| `SERVER_PORT` | `28080` | 服务端口（容器内设为 8080 与反代对齐） |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | 见 `application-mysql.yml` | MySQL 连接参数（仅 mysql profile） |
| `DATALINK_JWT_SECRET` | 开发默认串 | JWT 签名密钥，生产必须覆盖（≥32 字节随机串） |
| `DATALINK_CRYPTO_KEY` | 开发默认串 | 数据池连接器密码 AES-GCM 密钥，必须 16/24/32 字节 |
| `DATALINK_OPENAPI_TOKEN` | 开发默认串 | 开放 API 的 X-API-Key，生产必须覆盖 |
| `LLM_BASE_URL` / `LLM_API_KEY` / `LLM_MODEL` | DeepSeek / 空 / deepseek-chat | G4 大模型细化（OpenAI 兼容协议）；不配 Key 自动降级 |

## 文档索引

| 文档 | 内容 |
| --- | --- |
| [backend/README.md](backend/README.md) | 后端技术栈、目录结构、快速开始 |
| [backend/DEPLOY.md](backend/DEPLOY.md) | 后端部署全流程、环境变量、接口约定、常见问题 |
| [frontend/README.md](frontend/README.md) | 前端技术栈、目录结构、开发约定 |
| [frontend/DEPLOY.md](frontend/DEPLOY.md) | 前端构建、nginx 部署、HTTPS |
| [docs/PROJECT-HANDOFF.md](docs/PROJECT-HANDOFF.md) | 项目交接与进展 |
| [docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md](docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md) | 需求与设计文档书 |
