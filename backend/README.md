# DataLink 平台 · 后端

数据关联与业务流程监控分析平台的后端服务（Spring Boot 3 + MyBatis-Plus）。

## 技术栈

| 用途 | 选型 |
| --- | --- |
| 框架 | Spring Boot 3.3.x · Java 17 |
| ORM | MyBatis-Plus 3.5.7 |
| 数据库 | 离线本地模式：**嵌入式 H2**（零安装）｜ 部署模式：**MySQL 8** |
| 数据库迁移 | Flyway（每数据库一套迁移目录，可扩展 Oracle/PG） |
| 鉴权 | Spring Security + JWT（M0 阶段接口全放行，M1 收紧） |
| API 文档 | springdoc-openapi（Swagger UI） |

## 快速开始（离线本地模式，默认）

无需安装任何数据库，开箱即用：

```bash
cd backend
mvn spring-boot:run
```

- 后端地址：<http://localhost:8080>
- 健康探活：<http://localhost:8080/api/health>
- Swagger 文档：<http://localhost:8080/swagger-ui.html>
- 本地数据：嵌入式 H2 文件库，保存在 `backend/.data/h2/`（已 git 忽略，重启不丢）

内置管理员账号：`admin / admin123`（登录接口 `POST /api/auth/login`）。

## 部署模式（MySQL 8）

```bash
# 先建库与账号（MySQL 8）
mysql -uroot -p -e "CREATE DATABASE datalink DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 打包并运行
cd backend
mvn -DskipTests package
java -jar target/datalink-backend-0.1.0.jar --spring.profiles.active=mysql
```

连接参数均可通过环境变量覆盖：`DB_URL`、`DB_USER`、`DB_PASSWORD`、`DATALINK_JWT_SECRET`。
首次启动 Flyway 自动建表并写入种子数据。

## 目录结构

```
backend/
├── pom.xml
├── Dockerfile
├── scripts/                 # 项目本地 MySQL 启停脚本（可选，供 mysql profile 本地联调）
├── src/main/java/com/datalink/platform/
│   ├── DataLinkApplication.java
│   ├── common/              # 统一响应 Result / 异常处理
│   ├── config/              # MyBatis-Plus / CORS / Jackson / Swagger / Security / JWT
│   ├── system/              # 用户·角色·认证（登录）
│   └── monitor/             # 健康探活
└── src/main/resources/
    ├── application.yml          # 通用配置（默认 profile=local）
    ├── application-local.yml    # 离线 H2
    ├── application-mysql.yml    # MySQL 8
    └── db/migration/
        ├── h2/                  # H2 建表脚本
        ├── mysql/               # MySQL 建表脚本
        └── common/              # 两库共用种子数据
```

## 详细部署

见 [DEPLOY.md](DEPLOY.md)。
