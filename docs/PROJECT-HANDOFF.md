# 项目续接文档（HANDOFF）

> 本文件是「对话打包」：当会话因上下文上限需重开时，新会话先读本文件即可无缝续接。**主文档是项目文档书 v1.1**，本文件用于快速恢复进度。

## 一、这是什么项目

**数据关联与业务流程监控分析平台**（DataLinkAnalysisPlatform）—— 企业级 Web 平台，把系统/数据库/部门/业务动作画成「血管网式」关系网，给同一起终点提供多条路线选择，系统出问题时能「顺藤摸瓜」找到根因和影响面。

**核心三件事**：陈列（关系网展示）· 择路（多路线）· 摸瓜（问题排查）。

## 二、关键文档（都在 `docs/`）

| 文档 | 路径 | 用途 |
| --- | --- | --- |
| **项目文档书 v1.1** | `docs/数据关联与业务流程监控分析平台-项目文档书-v1.0.md` | ⭐ 交付基准：14 章 + 4 附录，含完整 DDL/RBAC/用户故事/页面清单 |
| 讨论草稿 | `docs/superpowers/specs/2026-08-15-数据关联流向分析平台-设计方案.md` | 需求演进过程记录 |
| 前端说明 | `frontend/README.md`、`frontend/DEPLOY.md` | 开发与部署 |
| 本文件 | `docs/PROJECT-HANDOFF.md` | 续接入口 |

## 三、已确认的技术栈与决策

- **前端**：Vue 3 + TypeScript + Vite + **AntV G6 5.x**（2D）→ 远期 3d-force-graph（3D）
- **后端**（未启动）：Java 17 + Spring Boot 3 + MyBatis-Plus + MySQL 8 + Flyway + Docker Compose
- **监控时效**：准实时轮询（分钟级，Spring @Scheduled）
- **实例数据**：特征推断 + 人工标记（进度上报表二期）
- **干预**：通知到人 + 生成工单 + 系统自动动作（三动作组合，L1~L4 分级）
- **命名体系**：所有对象主显示名 + 多别名，全局搜索单号/别名通吃
- 13 项已确认决策详见文档书第 14 节

## 四、当前进度

- [x] 需求分析（8 轮讨论）
- [x] 项目文档书 v1.0 → v1.1（补全 DDL/RBAC/用户故事/页面清单）
- [x] git 重开为全新仓库
- [x] 前端工程骨架 + 设计系统 + 核心画布（GraphCanvas G6）+ 关系网页（GraphView）+ 节点详情
- [x] 4 个子 agent 并行完成 7 个页面（Dashboard/流程列表/数据接入/检测点/告警/版本/系统管理）
- [x] **集成验证通过**：`npm run build` 全绿（类型检查 + 打包），`npm run dev` 运行中 http://localhost:5173
- [x] 前端已 commit（`2e40dc4` 前端 MVP；`b1cb475` 文档 v1.1）
- [ ] **清理误提交**：`frontend/vite.config.js`、`frontend/vite.config.d.ts`（vue-tsc 产物）待 `git rm --cached` + 二次 commit（.gitignore 已加忽略规则）
- [ ] **推送仓库**：当前仓库**无 remote**，需用户提供远程仓库地址（GitHub/Gitee）后 `git push`
- [ ] 后端启动（Spring Boot，M0/M1）

> 权限说明：`~/.claude/settings.local.json` 已设 `defaultMode: bypassPermissions`（全权模式），重启会话后所有命令不再弹确认。

## 五、前端工程当前结构

```
frontend/src/
├── styles/（tokens.css 设计令牌 + global.css 组件类）
├── types/index.ts（领域类型）
├── api/（mockData.ts + index.ts 数据访问层，可切真实 API）
├── components/（Icon/Tag/StatCard/SideNav/TopNav/GraphCanvas/NodeDetailPanel）
├── layouts/MainLayout.vue
├── views/（8 个页面，其中 7 个由子 agent 并行开发中）
└── router/index.ts
```

## 六、下一步指令

```bash
cd frontend
npm install        # 已装
npm run dev        # 起 dev server，浏览器打开 http://localhost:5173
```

集成验证要点：
1. `npm run build`（vue-tsc 类型检查 + vite 构建）必须通过，逐个修复子 agent 页面的 TS/模板报错
2. `npm run dev` 起服务，肉眼检查每个页面：布局、样式一致性（复用 tokens）、mock 数据渲染、跳转
3. 核心页 GraphView：确认 G6 画布渲染节点/边、路线高亮、节点点击弹详情、路线条（地铁式）正常
4. 页面间跳转与侧边导航高亮正常

## 七、用户工作偏好（重要）

- 项目内非删除操作**自主执行，不逐次确认**；**删除文件必须先确认**
- 开发顺序：先前端看效果 → 再后端
- 中文沟通

## 八、续接起点

若子 agent 已全部完成且未集成：直接进入「下一步指令」的集成验证。若尚未完成：等待 4 个子 agent 通知，再集成。
