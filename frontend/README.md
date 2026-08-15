# 数据关联与业务流程监控分析平台 · 前端

企业级 Web 前端，核心是「血管网式关系展示 + 多路线选择 + 顺藤摸瓜式问题排查」。

## 技术栈

- **Vue 3 + TypeScript** + Vite 6
- **AntV G6 5.x** —— 关系网画布（2D 节点/边渲染、路线高亮）
- Pinia（状态）、Vue Router（路由）
- 深色侧边栏 + 浅色内容区 + 科技蓝强调色

## 快速开始

```bash
# 1. 安装依赖（Node ≥ 20）
npm install

# 2. 开发启动（默认 http://localhost:5173）
npm run dev

# 3. 生产构建（产物输出到 dist/）
npm run build

# 4. 本地预览构建产物
npm run preview
```

## 目录结构

```
frontend/
├── index.html
├── vite.config.ts            # 构建/开发代理（/api → localhost:8080）
├── public/favicon.svg
└── src/
    ├── main.ts               # 入口
    ├── App.vue
    ├── router/index.ts       # 路由（8 个页面）
    ├── styles/
    │   ├── tokens.css        # 设计令牌（颜色/间距/圆角等 CSS 变量）
    │   └── global.css        # 全局组件类（按钮/表格/卡片/表单…）
    ├── types/index.ts        # 领域类型（与项目文档书数据模型对应）
    ├── api/
    │   ├── mockData.ts       # Mock 数据（演示）
    │   └── index.ts          # 数据访问层（后端接入后替换）
    ├── layouts/MainLayout.vue
    ├── components/
    │   ├── Icon.vue          # 线性图标集
    │   ├── Tag.vue           # 状态标签
    │   ├── StatCard.vue      # 统计卡
    │   ├── SideNav.vue       # 深色侧边导航
    │   ├── TopNav.vue        # 顶部栏（全局搜索）
    │   ├── GraphCanvas.vue   # ⭐ G6 关系网画布
    │   └── NodeDetailPanel.vue
    └── views/
        ├── DashboardView.vue    # 工作台
        ├── GraphView.vue        # ⭐ 关系网（核心页）
        ├── ProcessListView.vue  # 流程列表
        ├── DataSourceView.vue   # 数据接入
        ├── CheckpointView.vue   # 检测点
        ├── AlertView.vue        # 告警中心
        ├── VersionView.vue      # 配置版本
        └── SettingsView.vue     # 系统管理
```

## Mock 与真实 API

当前 `src/api/index.ts` 全部返回 Mock 数据，便于前端独立演示。后端（Spring Boot）就绪后，把该文件里的函数逐个替换为 `fetch('/api/...')` 即可，函数签名与返回类型不变，页面无需改动。

## 代码约定

- 复用 `tokens.css` 的设计令牌与 `global.css` 的组件类，不要另起一套样式体系
- 状态统一用 `<Tag>`，等级用 `lv--L1~L4`
- 组件文件命名 PascalCase，视图放 `src/views/`

## 部署

见 [DEPLOY.md](./DEPLOY.md)。
