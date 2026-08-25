# 前端部署文档

本平台前端为纯静态资源（Vite 构建产物），可部署到任意静态服务器 / nginx / CDN。后端为独立的 Spring Boot 服务，二者通过 `/api` 通信。

## 1. 构建

```bash
cd frontend
npm install
npm run build
# 产物在 frontend/dist/
```

## 2. 后端地址约定

前端代码中 API 基址**硬编码为相对路径 `/api`**（同源经反向代理转发，无跨域问题）。切换后端只需改 nginx `proxy_pass` 目标，无需重新构建前端。

- 开发环境：Vite 代理 `/api → http://localhost:28080`（`vite.config.ts`）。
- 生产环境：nginx `location /api/` 反代到后端实际端口（见第 3 节）。

## 3. 方式一：nginx 部署（推荐）

`dist/` 拷贝到服务器任意目录，nginx 配置示例：

```nginx
server {
    listen 80;
    server_name datalink.corp.local;

    # 前端静态资源
    root /var/www/datalink/dist;
    index index.html;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 静态资源缓存（带 hash 的产物可长缓存）
    location /assets/ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }

    # 反向代理后端 API（后端默认监听 28080，见 backend/application.yml）
    location /api/ {
        proxy_pass http://127.0.0.1:28080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

> 说明：`proxy_pass http://127.0.0.1:28080;`（不带 `/`）会保留 `/api` 前缀透传给后端。后端以 `/api/**` 为 Controller 前缀时即此写法；若后端不以 `/api` 为前缀，改为 `proxy_pass http://127.0.0.1:28080/;`。后端单机用 `--server.port` / `SERVER_PORT` 改过端口时，此处同步调整。

## 4. 方式二：Docker Compose（前后端一体化，推荐正式环境）

项目根目录提供 `docker-compose.yml`（由后端阶段补充完善）。前端镜像示例：

```dockerfile
# frontend/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

配套 `frontend/nginx.conf` 使用第 3 节的配置（静态 + API 反代 + SPA 回退）。

> 端口对齐：`docker-compose.yml` 中 backend 服务设 `SERVER_PORT=8080`，与 nginx 反代目标 `backend:8080` 及宿主映射 `8080:8080` 一致。改动 compose 端口时需三处同步。

## 5. HTTPS

生产环境建议在 nginx 或前置网关启用 TLS，并将 80 端口 301 跳转到 443：

```nginx
server {
    listen 443 ssl http2;
    server_name datalink.corp.local;
    ssl_certificate     /etc/nginx/ssl/datalink.crt;
    ssl_certificate_key /etc/nginx/ssl/datalink.key;
    # ... 其余同第 3 节
}
```

## 6. 常见问题

- **页面刷新 404**：SPA 路由需要 `try_files $uri $uri/ /index.html;` 回退，确认已配置。
- **接口 404/502**：检查 `/api` 反代目标端口是否与后端一致。
- **跨域**：生产走同域反代无需 CORS；开发环境由 `vite.config.ts` 代理解决。
- **图不显示**：确认浏览器为 Chrome/Edge 最新两个大版本（G6 依赖 Canvas/WebGL）。
