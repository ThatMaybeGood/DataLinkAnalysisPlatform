# DataLink 平台 · 后端部署文档

本文档覆盖后端从开发到生产部署的全部方式，按「本地离线 → 单机部署 → 容器化」三层组织。项目文档书与技术方案见仓库根目录 `docs/`。

---

## 0. 前置环境

| 方式 | 要求 |
| --- | --- |
| 本地离线（推荐开发用） | Java 17+、Maven 3.8+（无需任何数据库） |
| 单机部署（MySQL） | Java 17+、Maven 3.8+、MySQL 8.0+ |
| 容器化（Docker Compose） | Docker 20+、docker compose v2 |

> Java 版本检查：`java -version`；本工程编译目标为 17，运行在 17 及以上均可（实测 18 正常）。

---

## 1. 本地离线模式（默认，零安装）

后端默认 profile 为 `local`，使用**嵌入式 H2 文件库**，不需要安装任何数据库：

```bash
cd backend
mvn spring-boot:run
```

启动成功后：

| 入口 | 地址 |
| --- | --- |
| 健康探活 | <http://localhost:8080/api/health> |
| Swagger API 文档 | <http://localhost:8080/swagger-ui.html> |
| Spring Boot 管理端点 | <http://localhost:8080/actuator/health> |

- 本地数据文件保存在 `backend/.data/h2/datalink.mv.db`，**重启不丢**；删除该文件即重置数据。
- 内置管理员：`admin / admin123`（登录 `POST /api/auth/login`）。
- 离线模式当前启用 `permitAll`（接口全放行），便于前端联调；M1 阶段将按 RBAC 收紧。

### 1.1 前端联调

前端 Vite 开发服务器已配置代理 `/api → http://localhost:8080`（见 `frontend/vite.config.ts`），
先启动后端、再 `cd frontend && npm run dev` 即可前后端联调，前端无需改动。

---

## 2. 单机部署（MySQL 8）

### 2.1 准备数据库

```sql
CREATE DATABASE datalink DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'datalink'@'%' IDENTIFIED BY '你的强密码';
GRANT ALL PRIVILEGES ON datalink.* TO 'datalink'@'%';
FLUSH PRIVILEGES;
```

### 2.2 打包

```bash
cd backend
mvn clean package -DskipTests
# 产物：target/datalink-backend-0.1.0.jar
```

### 2.3 运行

```bash
java -jar target/datalink-backend-0.1.0.jar \
  --spring.profiles.active=mysql
```

连接参数全部可用**环境变量**覆盖（生产建议走环境变量，避免改代码）：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` | 生产固定 `mysql` |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/datalink?...` | JDBC 连接串 |
| `DB_USER` | `datalink` | 数据库账号 |
| `DB_PASSWORD` | `datalink123` | 数据库密码（生产必改） |
| `DATALINK_JWT_SECRET` | 开发默认串 | JWT 签名密钥，**必须 ≥32 字节随机串**，生产必改 |
| `DATALINK_CRYPTO_KEY` | 开发默认串 | 数据池连接器密码 AES-GCM 加密密钥，**必须 16/24/32 字节**，生产必改 |
| `SERVER_PORT` | 8080 | 服务端口（Spring 标准 `server.port`，可用 `SERVER_PORT` 覆盖） |

例：

```bash
export SPRING_PROFILES_ACTIVE=mysql
export DB_URL='jdbc:mysql://db.internal:3306/datalink?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true'
export DB_USER=datalink
export DB_PASSWORD=xxxx
export DATALINK_JWT_SECRET=$(openssl rand -base64 48)
java -jar datalink-backend-0.1.0.jar
```

### 2.4 首次启动行为

- Flyway 自动执行 `db/migration/mysql` + `db/migration/common` 下的迁移：建 21 张表 + 写入种子数据（角色、admin 账号、示例订单支付路网）。
- 表结构变更一律通过**新增 Flyway 迁移脚本**（`V3__xxx.sql`）完成，禁止手工改表导致迁移校验失败。

---

## 3. 容器化部署（Docker Compose，全栈一键）

仓库根目录 `docker-compose.yml` 编排了 **nginx(前端) + backend + mysql8** 三件套：

```bash
docker compose up -d --build
```

| 服务 | 地址 | 说明 |
| --- | --- | --- |
| 前端 | <http://localhost> | nginx 托管静态资源，`/api` 反代到 backend |
| 后端 | <http://localhost:8080/api/health> | Spring Boot |
| MySQL | localhost:3306 | 数据卷 `mysql-data` 持久化 |

生产必改项：`docker-compose.yml` 中的 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`DATALINK_JWT_SECRET`。

### 3.1 仅容器化后端

```bash
docker build -t datalink-backend ./backend
docker run -d -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=mysql \
  -e DB_URL='jdbc:mysql://host:3306/datalink?...' \
  -e DB_USER=datalink -e DB_PASSWORD=xxx \
  -e DATALINK_JWT_SECRET=xxx \
  datalink-backend
```

---

## 4. 数据库多类型支持（MySQL / H2 / 未来 Oracle、PG）

平台存储层设计为**多数据库可切换**，原理有三点：

1. **Flyway 每库一套迁移目录**（已按此组织，可继续扩展）：

   ```
   src/main/resources/db/migration/
   ├── h2/      # 本地离线
   ├── mysql/   # 部署（当前生产目标）
   ├── oracle/  # 预留：接 Oracle 时在此新增对应 DDL，并在新 profile 里指定 locations
   └── common/  # 各库共用的种子数据
   ```

2. **MyBatis-Plus 方言自动适配**：分页插件按配置项 `datalink.db-type` 切换（`h2` / `mysql`），
   后续加 Oracle 只需把该配置支持 `oracle` 值并引入 `com.oracle.database.jdbc:ojdbc` 依赖。

3. **不写死方言 SQL**：业务查询尽量使用通用 SQL；确需差异的部分封装在对应 Mapper XML。

> 注意：此机制针对的是**平台自身存储库**。「数据池」连接器（对接外部业务库）为独立模块，见 4.1，二者互不影响。

### 4.1 数据池模块（外部数据库连接管理）

平台可注册管理多个外部数据库连接（**数据池**），支持 MySQL 8 / PostgreSQL / H2（方言可插拔，后续加 Oracle 只需新增方言适配器 + JDBC 驱动）。配置存平台库（全局共享，多节点各自建连接池，**预留分布式拓展**）。

**前端入口**：「数据接入」页（左侧导航）。支持：连接列表 / 新建·编辑 / 测试连通（延迟+版本）/ 设为当前（全局唯一）/ 浏览库表 / 数据预览（前 50 行）。

**REST 接口**（`/api/connectors`，统一 `/api` 前缀）：
```
GET    /api/connectors                    分页+关键字
POST   /api/connectors                    新建（密码必填，AES 加密入库）
PUT    /api/connectors/{id}               修改（密码留空=不改）
DELETE /api/connectors/{id}               删除
POST   /api/connectors/{id}/test          测试连通 → {ok, latencyMs, dbVersion, message}
POST   /api/connectors/{id}/activate      设为当前（自动取消其它）
GET    /api/connectors/{id}/tables        浏览库表清单
GET    /api/connectors/{id}/tables/{table}/preview  数据预览
```

**安全**：连接器密码 AES-GCM 加密存 `connector.encrypted_pwd`，密钥来自 `DATALINK_CRYPTO_KEY`；接口永不返回密码；浏览/预览为只读连接、行数受限。

**数据池 vs 平台自身存储**：数据池用于「连接外部业务库」（M2 采集/监控将使用），平台自身建模/监控/配置数据仍存于 H2 或 MySQL（第 1、2 节），二者隔离。

### 4.2 建模域接口（陈列 / 关系网）

平台建模（站点/路网/流程/路线）REST 接口，id 统一为字符串，对接前端画布：

```
GET    /api/nodes                    全部站点（GraphNode，含 checkpoints）
POST   /api/nodes                    新建站点（PUT /{id} 修改，DELETE 删除）
GET    /api/edges                    全部路网边（GraphEdge）
POST   /api/edges                    新建边（DELETE /{id}）
GET    /api/processes                流程列表（含起点/终点名、节点数、路线数、实例统计）
POST   /api/processes                新建流程（PUT/DELETE 同）
GET    /api/routes                   路线列表（?processId= 过滤，nodeIds 有序）
POST   /api/routes                   新建路线（nodeIds 级联写 route_node；PUT 级联重建、DELETE 级联删）
GET    /api/search?q=                全局搜索（节点/流程/路线按 code/name/alias 通吃）
```

> 建模数据的种子示例（V2/V4 迁移）包含「订单支付流程」「付款流程（风控/支付分支）」两套路网，启动即自动建好，可直接在关系网画布查看。

### 4.3 监控域接口（摸瓜：实例 / 检测点 / 告警 / 工单 / 排查）

```
GET    /api/instances?page=&size=&status=   实例分页（含流程/路线/进度/当前站）
GET    /api/instances/{id}/nodes            实例经过站点明细
POST   /api/instances                       新建实例（nodeIds 生成链路）
PUT    /api/instances/{id}                  更新进度/状态
GET    /api/checkpoints?nodeId=             站点检测点（含最近检测状态）
POST/PUT/DELETE /api/checkpoints[/{id}]     检测点维护
GET    /api/alerts?status=                  告警列表（含目标名）
POST   /api/alerts/{id}/resolve             关闭告警
GET    /api/tickets                         工单列表
POST   /api/tickets                         从告警生成工单
GET    /api/graph/{nodeId}/trace            ★顺藤摸瓜：上游/下游（BFS，附检测点状态）
GET    /api/graph/{nodeId}/routes           该节点所在路线
GET    /api/dashboard/stats                 看板聚合（流程/运行/今日完成/告警/卡住/覆盖/均耗时/慢节点/趋势）
```

> 监控示例数据（V5 迁移）含 6 实例 / 11 检测点 / 5 告警 / 3 工单；「支付系统检测 FAIL → 告警 → 实例卡住 → 顺藤摸瓜溯源」闭环可直接在界面体验。

### 4.4 M3 择路 + M2 收尾接口

```
GET    /api/graph/path?from=&to=&maxDepth=8   任意两点路径查询（DFS，最多 20 条）
GET    /api/graph/{nodeId}/impact             影响面：下游节点 + 受影响实例/路线
POST   /api/alerts                            新建告警（自动应用等级处置 L1-L4）
PUT    /api/tickets/{id}                      工单状态流转（OPEN→PROCESSING→RESOLVED + 指派）
GET    /api/versions?page=&size=&targetType=  配置版本历史（建模 CRUD 自动留痕）
```

- 等级处置规则：L1→`AUTO_ACTION,TICKET,NOTIFY`、L2→`TICKET,NOTIFY`、L3→`NOTIFY`、L4→`RECORD`（含 TICKET 自动生成工单）
- 配置版本：node/process/route 增删改自动写入 `config_version` 快照（operator 取当前登录用户）
- **鉴权（RBAC 已启用）**：所有接口需 `Authorization: Bearer <token>`（`POST /api/auth/login` 换 token）；`/api/auth/me` 查当前用户。角色：建模/数据池写=ADMIN/MODELER，告警/工单写=ADMIN/OPERATOR/ONCALL，其余登录即可读。401/403 返回统一 JSON。内置账号：`admin/admin123`（全权）、`viewer/viewer123`（只读 VIEWER）

---

## 5. 数据库备份与运维建议

```bash
# MySQL 逻辑备份
mysqldump -udatalink -p datalink > datalink-$(date +%F).sql

# 恢复
mysql -udatalink -p datalink < datalink-2026-08-16.sql
```

- 生产库使用**最小权限**账号（仅 `datalink.*`），平台永不写入业务系统。
- 建议定时备份 + 定期恢复演练（文档书第 7.5 节）。
- 实例/检测结果按策略归档（文档书 180 天归档约定），避免表膨胀。

---

## 6. 常见问题排查

| 现象 | 处理 |
| --- | --- |
| `mvn spring-boot:run` 启动慢 | 首次需下载依赖，属正常；之后有本地缓存 |
| 端口 8080 被占用 | 换端口：`--server.port=8081` 或环境变量 `SERVER_PORT` |
| H2 文件被锁 | 确认没有残留后端进程，删除 `backend/.data/h2/*.lock` 后重试 |
| MySQL 连接报 `Public Key Retrieval is not allowed` | URL 已带 `allowPublicKeyRetrieval=true`；如自建 URL 需保留该参数 |
| Flyway 报 `Validate failed` | 表结构被手工改动过；回滚改动或按迁移规范新增脚本 |
| CORS 跨域 | 开发环境前端 5173 已在白名单；如换端口，在 `CorsConfig` 补充 |
| 未装 MySQL 又要体验 mysql profile | 用本仓库 `backend/scripts/start-mysql.sh` 启动项目本地 MySQL，或直接走 H2 本地模式 |

---

## 7. 接口约定

- 所有接口统一 `/api` 前缀。
- 统一响应结构（HTTP 200，业务码在 body）：

  ```json
  { "code": 200, "message": "success", "data": { } }
  ```

  `code != 200` 表示业务错误（400 参数 / 401 未认证 / 403 无权限 / 404 不存在 / 500 内部错误）。
- M0 已提供接口：`GET /api/health`（探活）、`POST /api/auth/login`（登录）。
