# 项目续接文档（HANDOFF）

> 本文件是「对话打包」：**新会话（尤其后端开发）先读本文件即可无缝续接**。主文档是项目文档书 v1.1，本文件用于快速恢复进度与后端起点。

## 一、这是什么项目

**数据关联与业务流程监控分析平台**（DataLinkAnalysisPlatform）—— 企业级 Web 平台，把系统/数据库/部门/业务动作画成「血管网式」关系网，给同一起终点提供多条路线选择，系统出问题时能「顺藤摸瓜」找到根因和影响面。

**核心三件事**：陈列（关系网展示）· 择路（多路线）· 摸瓜（问题排查）。

## 二、关键文档（都在 `docs/`）

| 文档 | 路径 | 用途 |
| --- | --- | --- |
| **项目文档书 v1.1** | `docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md` | ⭐ 交付基准：14 章 + 4 附录，含完整 DDL/RBAC/用户故事/页面清单 |
| 讨论草稿 | `docs/superpowers/specs/2026-08-15-数据关联流向分析平台-设计方案.md` | 需求演进过程记录 |
| 前端说明 | `frontend/README.md`、`frontend/DEPLOY.md` | 前端开发与部署 |
| 本文件 | `docs/PROJECT-HANDOFF.md` | 续接入口 |

## 三、已确认的技术栈与决策

- **前端（已完成）**：Vue 3 + TypeScript + Vite + AntV G6 5.x（2D），远期 3d-force-graph（3D）
- **后端（本轮任务）**：**Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8 + Flyway + Docker Compose**
- **监控时效**：准实时轮询（分钟级，Spring @Scheduled）
- **实例数据**：特征推断 + 人工标记（进度上报表二期）
- **干预**：通知到人 + 生成工单 + 系统自动动作（三动作组合，L1~L4 分级）
- **命名体系**：所有对象主显示名 + 多别名，全局搜索单号/别名通吃
- 13 项已确认决策详见文档书第 14 节

## 四、已完成进度（前端 + 交付）

- [x] 需求分析（8 轮讨论）
- [x] 项目文档书 v1.1（补全 DDL/RBAC/用户故事/页面清单）
- [x] git 重开为全新仓库，**已推送到 GitHub**：`git@github.com:ThatMaybeGood/DataLinkAnalysisPlatform.git`（`main`，5 个 commit）
- [x] 前端 MVP 完成：8 页面 + G6 画布 + 路线高亮 + 详情面板，`npm run build` 全绿
- [x] 前端权限全权模式已配（`Bash(*)` + `bypassPermissions`，含删除不再弹确认）

**远程仓库当前历史**：
```
03c06c4 更新 .gitignore 忽略 vite 产物与续接文档进度
0e825a9 移除 vue-tsc 误提交的 vite.config 产物
2e40dc4 前端 MVP：Vue3 + G6 关系网平台
b1cb475 文档补全 v1.1
3d9890f 初始化：项目文档书 v1.0 与 .gitignore
```

## 五、后端开发起点（本轮核心任务）

### 5.1 技术栈落地
- 建 `backend/` 目录，Spring Boot 3（Java 17）Maven 工程
- 依赖：spring-boot-starter-web、mybatis-plus-spring-boot3-starter、mysql-connector-j、flyway-core、lombok、spring-security（JWT）、hutool（工具，可选）、spring-boot-starter-validation
- 配置 `application.yml`：数据源、MyBatis-Plus、Flyway、端口 8080

### 5.2 数据模型
- **建表以文档书「附录 A」的 20 张表为准**（node/relation/process/route/route_node/checkpoint/check_result/instance/instance_node/alert/ticket/config_version/alias/connector/import_job/import_log/sys_user/sys_role/sys_user_role/sys_grant/operation_log）
- 用 Flyway `V1__init.sql` 落地 DDL（进入实现阶段以 Flyway 迁移脚本为准）

### 5.3 里程碑
- **M0（本轮先做）**：Spring Boot 脚手架 + Flyway 初始化 + MySQL 连接 + MyBatis-Plus + 统一响应/异常 + CORS（允许前端 5173）+ `/api/health` 探活
- **M1（后续）**：建模域 CRUD（节点/流程/路线/检测点）+ 命名别名 + 等级/版本 + 数据接入连接器

### 5.4 前后端对接约定
- 后端所有接口统一 `/api` 前缀
- 前端 `vite.config.ts` 已配代理：`/api` → `http://localhost:8080`（无需改）
- 前端 `src/api/index.ts` 当前返回 mock，后端就绪后逐个替换为 `fetch('/api/...')`，函数签名不变（页面无需改动）
- 字段/枚举与前端 `src/types/index.ts` 对齐（nodeType、Level L1-L4、状态、priority 等）

### 5.5 环境检查（新会话先做）
```bash
java -version          # 需 Java 17+
mvn -version           # Maven 3.8+
mysql --version        # 本地 MySQL 8；无则用 Docker：docker run -p 3306:3306 -e MYSQL_ROOT_PASSWORD=... mysql:8
```

## 六、下一步指令（新会话）

1. 读本文档 + 文档书 v1.1（重点附录 A DDL、第 7 章技术方案、第 11 章里程碑）
2. 检查 Java/Maven/MySQL 环境
3. 创建 `backend/` Spring Boot 工程，先完成 **M0 脚手架 + Flyway 建表 + health 接口**
4. 提交并推送后端到同一远程仓库

## 七、用户工作偏好（重要）

- 项目内操作**自主执行，不逐次确认**（含删除——用户已明确"所有权限都给你，包括删除"）
- 开发顺序：先前端看效果 → 再后端（当前在后端阶段）
- 中文沟通；用户习惯用类比描述需求

## 八、续接起点

当前任务 = **后端开发（M0 起步）**。前端已完成并运行（`cd frontend && npm run dev`，http://localhost:5173）。
