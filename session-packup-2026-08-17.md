# 会话打包记录：2026-08-17 晚间（续承接点）

> 本文件由当前会话自动生成，作为「对话打包」附录，与 docs/PROJECT-HANDOFF.md 同步。
> 明日起读本文件 → 读 findings.md → 读 task_plan.md 即可无缝续接。

## 1. 我在哪里

**当前阶段 = 图来源 G3（引擎最小可行版）进行中。**
- G1 ✅、G2 ✅ 已完成并推送 GitHub。
- G3 后端 G3a~G3d **已完成**（含 20 个后端测试全绿）；前端 G3e **半途**（types 已加，api 函数未追加，GraphSourceView 未改）。
- G4 大模型接入层、G5 校正闭环尚未启动。

## 2. 已完成的 G3 产物（文件清单）

### 后端（9 新文件 + 1 改动）
```
backend/src/main/resources/engine/his_demo_schema.sql          # HIS 演示库 6 表
backend/src/main/java/com/datalink/platform/engine/
  config/DemoBizDbInitializer.java      # Order(10) 幂等建内存库
  config/DemoConnectorSeeder.java       # Order(20) 运行时加密连接器
  controller/AnalyzeController.java     # GET /api/analyze
  dto/EngineDraftVO.java
  dto/EngineCandidateVO.java
  dto/EngineFlowVO.java
  service/EngineAnalyzeService.java
  service/impl/EngineAnalyzeServiceImpl.java   # 核心识别逻辑
backend/src/test/java/com/datalink/platform/engine/
  service/EngineAnalyzeServiceTest.java  # 6 用例
backend/src/main/java/com/datalink/platform/config/
  SecurityConfig.java                   # + /api/analyze/** GET authenticated
```

### 前端（1 改动，2 半成品）
```
frontend/src/types/index.ts              # ✅ 已加 EngineCandidate/Flow/Draft（第 241~266 行）
frontend/src/api/index.ts                # 🔄 import EngineDraft 已加，fetchEngineAnalyze 函数未追加（末尾 line 355）
frontend/src/views/GraphSourceView.vue   # ❌ 未动（G2 假数据版本，671 行）
```

### 规划文档
```
task_plan.md         # G3 子任务已标记；错误日志已补 6 条
findings.md          # 已追加 G3 已做/踩坑/置信度/前端状态 4 节
progress.md          # 已追加 G3 进行中条目
docs/PROJECT-HANDOFF.md  # 已追加 G3 进行中小节
```

## 3. 关键数值（验证基线）

| 项目 | 当前值 | 说明 |
|---|---|---|
| 后端测试类数 | **20** | 原 19 + EngineAnalyzeServiceTest 1 |
| 后端测试用例总数 | 76+6=82+ | 通过 |
| 前端端口 | vite 5173，vite.config.ts 代理 /api→28080 | |
| 后端端口 | application.yml 28080 | |
| 演示库名 | `jdbc:h2:mem:datalink_demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1` | |
| DemoConnector id | 运行时种子创建（非迁移，非确定 id）；前端需按 database_name 筛选 | |
| 演示表 | 6 张（reg_order / fee_order / refund_apply / settle_bill / pay_record / prescription_detail） | |

## 4. 关键踩坑记录（续接必读）

1. **DemoBizDbInitializer SQL 拆句**：原按 `;\n` 切，首个 CREATE 块因文件头 `-- ...` 被整块吞掉。改用「逐行剥注释行 → 行尾 `;` 切」。
2. **演示库存在性检查**：`INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'` 在 `DATABASE_TO_LOWER=TRUE` 下 schema 存小写，匹配失败 → 重复建表。改为 `WHERE TABLE_NAME='reg_order'`。
3. **H2 内部表污染候选**：H2 多连接场景自动创建 `locks/query_statistics/sessions/session_state`。解法：常量 `META_TABLES` Set.of 8 项过滤。
4. **草稿边方向反转**：原 linkReferences 按「同列名」判定，多表共享时方向乱。改为编码号列前缀匹配（`fee_no` → 表名 `fee_*` = 拥有者 = provider，数据方向 provider→consumer）。
5. **buildFlows 方法头丢失**：重构时流程链代码嵌入 linkReferences 末尾未抽方法 → 编译失败。独立为 private buildFlows(List)。
6. **前端 api/index.ts Edit 失败**：未先 Read 文件就 Edit → 失败；后续改用追加到末尾。

## 5. 明日起手命令（复制粘贴即用）

```bash
# 后端验证
cd D:/JetBrains/DataLinkAnalysisPlatform/backend
mvn test                                    # 期望 20 测试类全绿
mvn spring-boot:run                         # 启动后端 28080（G3 种子自动灌库）

# 前端构建
cd D:/JetBrains/DataLinkAnalysisPlatform/frontend
npm run build                               # 期望全绿

# CDP 复核（可选）
python .data/g3_engine_verify.py            # 需先写复核脚本
```

## 6. 明日接续路径（按顺序）

```
1. 读 findings.md  → 回忆技术上下文
2. 读 task_plan.md → 确认 G3 未完成项
3. frontend/src/api/index.ts 追加 fetchEngineAnalyze
4. frontend/src/views/GraphSourceView.vue 改 script setup 接入真实数据（兜底假数据）
5. npm run build 全绿
6. CDP 复核草稿渲染数字（1 库节点 + 6 表节点 + 6 候选 + 流程 1+ 条）
7. 文档书第 0 章升到 v1.6（图来源 60%，整体 99.7%）
8. HANDOFF + task_plan/findings/progress 同步到「G3 完成」
```

## 7. 未做事项（G3~G5 概览）

| 里程碑 | 状态 | 下次接续点 |
|---|---|---|
| G1 三层视图 | ✅ | — |
| G2 管线原型 | ✅ | — |
| G3 引擎 MVP | 🔄 后端完成，前端接线未完成 | api 函数 + GraphSourceView 脚本段 |
| G4 大模型接入 | ⬜ 未开始 | ModelProvider 可插拔接口（DeepSeek/通义/Claude/GPT） |
| G5 校正闭环 | ⬜ 未开始 | 校正记录表 + 模式库沉淀 |
| 收尾推送 | ⬜ 未开始 | commit/push 到 GitHub |

---
*本文件为自动生成的续接快照，下次会话读 findings.md + task_plan.md + progress.md 即可恢复上下文。*
