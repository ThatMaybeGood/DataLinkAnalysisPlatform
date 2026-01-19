工作流可视化平台 - 最终项目结构图
📁 完整项目结构总览
text
workflow-visualization-platform/           # 项目根目录
├── 📂 backend/                            # Spring Boot 后端项目
│   ├── 📂 src/main/java/com/workflow/platform/
│   │   ├── 📂 annotation/                 # 自定义注解
│   │   │   └── RequireMode.java
│   │   ├── 📂 aspect/                     # AOP切面
│   │   │   └── ModeAspect.java
│   │   ├── 📂 config/                     # 配置类
│   │   │   ├── ModeCondition.java
│   │   │   ├── ConditionalOnMode.java
│   │   │   ├── ModeConfiguration.java
│   │   │   ├── WebConfig.java
│   │   │   ├── SecurityConfig.java
│   │   │   └── DatabaseConfig.java
│   │   ├── 📂 controller/                 # 控制器层
│   │   │   ├── WorkflowController.java    # ✅ 工作流统一控制器
│   │   │   ├── NodeController.java        # 节点控制器
│   │   │   ├── ValidationController.java  # 验证规则控制器
│   │   │   ├── ConnectorController.java   # 连接器控制器
│   │   │   ├── ExecutionController.java   # 执行记录控制器
│   │   │   └── FileController.java        # 文件管理控制器
│   │   ├── 📂 exception/                  # 异常处理
│   │   │   ├── ModeNotAllowedException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   ├── 📂 model/                      # 数据模型层
│   │   │   ├── 📂 entity/                 # 数据库实体
│   │   │   │   ├── WorkflowEntity.java
│   │   │   │   ├── NodeEntity.java
│   │   │   │   ├── ValidationRuleEntity.java
│   │   │   │   ├── ConnectorEntity.java
│   │   │   │   └── ExecutionEntity.java
│   │   │   ├── 📂 dto/                    # 数据传输对象
│   │   │   │   ├── WorkflowDTO.java
│   │   │   │   ├── NodeDTO.java
│   │   │   │   ├── ValidationRuleDTO.java
│   │   │   │   └── ModeConfigDTO.java
│   │   │   └── 📂 vo/                     # 视图对象
│   │   │       ├── WorkflowVO.java
│   │   │       ├── WorkflowFileInfoVO.java
│   │   │       └── ExecutionResultVO.java
│   │   ├── 📂 repository/                 # 数据访问层
│   │   │   ├── WorkflowRepository.java    # JPA接口（在线模式）
│   │   │   ├── OfflineWorkflowRepository.java  # ✅ 文件存储仓库（离线模式）
│   │   │   ├── NodeRepository.java
│   │   │   └── OfflineNodeRepository.java
│   │   ├── 📂 service/                    # 服务层
│   │   │   ├── WorkflowService.java       # ✅ 工作流服务接口
│   │   │   ├── WorkflowServiceFactory.java # ✅ 服务工厂
│   │   │   ├── 📂 impl/                   # 服务实现
│   │   │   │   ├── OnlineWorkflowServiceImpl.java     # ✅ 在线模式实现
│   │   │   │   └── OfflineWorkflowServiceImpl.java    # ✅ 离线模式实现
│   │   │   ├── NodeService.java
│   │   │   ├── ValidationService.java
│   │   │   ├── ConnectorService.java
│   │   │   ├── ExecutionService.java
│   │   │   └── FileStorageService.java    # 文件存储服务
│   │   ├── 📂 util/                       # 工具类
│   │   │   ├── ModeContext.java           # ✅ 模式上下文
│   │   │   ├── FileUtil.java
│   │   │   ├── JsonUtil.java
│   │   │   └── CryptUtil.java
│   │   └── WorkflowPlatformApplication.java  # ✅ 主启动类
│   ├── 📂 src/main/resources/
│   │   ├── application.yml                # ✅ 主配置文件
│   │   ├── application-online.yml         # ✅ 在线模式配置
│   │   ├── application-offline.yml        # ✅ 离线模式配置
│   │   ├── data-online-mysql.sql                       # ✅ 数据库初始化脚本
│   │   └── logback-spring.xml             # ✅ 日志配置
│   ├── Dockerfile                         # ✅ 在线模式Dockerfile
│   ├── Dockerfile.offline                 # ✅ 离线模式Dockerfile
│   └── pom.xml                            # ✅ Maven配置文件
│
├── 📂 frontend/                           # React 前端项目
│   ├── 📂 public/
│   │   ├── index.html
│   │   └── favicon.ico
│   ├── 📂 src/
│   │   ├── 📂 assets/                     # 静态资源
│   │   │   ├── images/
│   │   │   └── styles/
│   │   ├── 📂 components/                 # 组件
│   │   │   ├── 📂 common/                 # 通用组件
│   │   │   │   ├── Layout/
│   │   │   │   │   ├── Header.js
│   │   │   │   │   ├── Sider.js
│   │   │   │   │   └── Footer.js
│   │   │   │   ├── ModeIndicator.js       # 模式指示器
│   │   │   │   └── NetworkStatus.js       # 网络状态
│   │   │   └── 📂 workflow/               # 工作流组件
│   │   │       ├── WorkflowManager.js     # ✅ 工作流管理器
│   │   │       ├── WorkflowDesigner.js    # 工作流设计器
│   │   │       ├── OfflineWorkflowDesigner.js  # 离线工作流设计器
│   │   │       ├── NodeConfigEditor.js    # 节点配置编辑器
│   │   │       ├── ValidationRuleEditor.js # 验证规则编辑器
│   │   │       ├── DataDiffViewer.js      # 数据比对器
│   │   │       ├── WorkflowMapView.js     # 工作流地图视图
│   │   │       └── WorkflowTimeline.js    # 工作流时间线
│   │   ├── 📂 config/                     # 配置文件
│   │   │   ├── appConfig.js               # ✅ 应用配置
│   │   │   ├── modeConfig.js              # ✅ 模式配置
│   │   │   └── apiConfig.js               # API配置
│   │   ├── 📂 hooks/                      # 自定义Hooks
│   │   │   ├── useMode.js                 # ✅ 模式钩子
│   │   │   ├── useWorkflow.js             # 工作流钩子
│   │   │   ├── useOfflineStorage.js       # 离线存储钩子
│   │   │   └── useNetworkStatus.js        # 网络状态钩子
│   │   ├── 📂 pages/                      # 页面组件
│   │   │   ├── Dashboard.js               # 仪表盘
│   │   │   ├── WorkflowList.js            # 工作流列表
│   │   │   ├── WorkflowDetail.js          # 工作流详情
│   │   │   ├── ExecutionMonitor.js        # 执行监控
│   │   │   ├── DataAnalytics.js           # 数据分析
│   │   │   └── Settings.js                # 设置页面
│   │   ├── 📂 services/                   # 服务层
│   │   │   ├── ServiceFactory.js          # ✅ 服务工厂
│   │   │   ├── BaseWorkflowService.js     # ✅ 基础服务类
│   │   │   ├── onlineWorkflowService.js   # ✅ 在线工作流服务
│   │   │   ├── offlineWorkflowService.js  # ✅ 离线工作流服务
│   │   │   ├── onlineNodeService.js       # 在线节点服务
│   │   │   ├── offlineNodeService.js      # 离线节点服务
│   │   │   ├── onlineValidationService.js # 在线验证服务
│   │   │   ├── offlineValidationService.js # 离线验证服务
│   │   │   ├── api.js                     # ✅ API基础服务
│   │   │   ├── fileService.js             # 文件服务
│   │   │   └── offlineManager.js          # 离线管理器
│   │   ├── 📂 stores/                     # 状态管理
│   │   │   ├── workflowStore.js           # 工作流状态
│   │   │   ├── connectorStore.js          # 连接器状态
│   │   │   ├── executionStore.js          # 执行状态
│   │   │   └── modeStore.js               # 模式状态
│   │   ├── 📂 utils/                      # 工具函数
│   │   │   ├── offlineStorage.js          # ✅ IndexedDB封装
│   │   │   ├── fileUtils.js               # 文件工具
│   │   │   ├── validationUtils.js         # 验证工具
│   │   │   ├── dataTransform.js           # 数据转换
│   │   │   ├── networkMonitor.js          # 网络监控
│   │   │   └── modeDetector.js            # 模式检测
│   │   ├── App.js                         # 根组件
│   │   ├── App.css                        # 全局样式
│   │   ├── index.js                       # 入口文件
│   │   └── routes.js                      # 路由配置
│   ├── package.json                       # ✅ npm配置
│   ├── craco.config.js                    # ✅ Craco配置
│   ├── .env                               # ✅ 环境变量（主）
│   ├── .env.online                        # ✅ 在线模式环境变量
│   ├── .env.offline                       # ✅ 离线模式环境变量
│   └── .gitignore
│
├── 📂 scripts/                            # 脚本目录
│   ├── start-online.sh                    # ✅ 启动在线模式
│   ├── start-offline.sh                   # ✅ 启动离线模式
│   ├── stop-all.sh                        # ✅ 停止所有服务
│   ├── build-all.sh                       # ✅ 完整构建脚本
│   ├── deploy-docker.sh                   # Docker部署脚本
│   └── backup-data.sh                     # 数据备份脚本
│
├── 📂 docker/                             # Docker配置
│   ├── docker-compose.yml                 # 主Docker配置
│   ├── docker-compose-online.yml          # 在线模式Docker配置
│   ├── docker-compose-offline.yml         # 离线模式Docker配置
│   ├── nginx.conf                         # Nginx配置
│   └── init.sql                           # 数据库初始化
│
├── 📂 docs/                               # 文档
│   ├── 📂 api/                            # API文档
│   │   ├── workflow-api.md
│   │   └── connector-api.md
│   ├── 📂 guide/                          # 使用指南
│   │   ├── getting-started.md             # 快速开始
│   │   ├── online-mode.md                 # 在线模式指南
│   │   ├── offline-mode.md                # 离线模式指南
│   │   └── deployment.md                  # 部署指南
│   ├── 📂 examples/                       # 示例
│   │   ├── order-process.json             # 订单处理示例
│   │   ├── user-registration.json         # 用户注册示例
│   │   └── data-validation.json           # 数据验证示例
│   └── architecture.md                    # 架构设计文档
│
├── 📂 data/                               # 数据目录（运行时生成）
│   ├── 📂 online/                         # 在线模式数据
│   │   ├── database/                      # 数据库文件
│   │   └── exports/                       # 导出文件
│   └── 📂 offline/                        # 离线模式数据
│       ├── workflows/                     # 工作流文件
│       ├── nodes/                         # 节点文件
│       ├── rules/                         # 规则文件
│       ├── exports/                       # 导出文件
│       └── backups/                       # 备份文件
│
├── 📂 logs/                               # 日志目录（运行时生成）
│   ├── backend.log                        # 后端日志
│   ├── frontend.log                       # 前端日志
│   ├── backend-offline.log                # 离线后端日志
│   └── frontend-offline.log               # 离线前端日志
│
├── README.md                              # 项目说明
├── LICENSE                                # 许可证
├── .gitignore                             # Git忽略配置
├── pom.xml                                # 根pom（多模块项目可选）
└── build-all.sh                           # 顶层构建脚本
🏗️ 核心模块说明
1. 后端模块 (backend/)
   text
   backend/
   ├── config/           # 配置类 - 模式切换、安全、数据库等
   ├── controller/       # REST API 控制器 - 统一入口，模式感知
   ├── service/         # 业务服务层 - 接口+两种模式实现
   ├── repository/      # 数据访问层 - 数据库+文件系统
   ├── model/          # 数据模型 - 实体、DTO、VO
   ├── util/           # 工具类 - 模式上下文、文件操作等
   ├── aspect/         # AOP切面 - 模式验证、日志等
   └── resources/      # 配置文件 - 在线/离线模式配置
2. 前端模块 (frontend/)
   text
   frontend/
   ├── components/     # 组件库 - 通用+工作流专用
   ├── services/      # 服务层 - 服务工厂+两种模式实现
   ├── hooks/         # 自定义Hooks - 模式、网络状态等
   ├── stores/        # 状态管理 - Zustand/Redux
   ├── utils/         # 工具函数 - IndexedDB、文件操作等
   ├── config/        # 配置管理 - 应用配置、模式配置
   └── pages/         # 页面组件 - 各个功能页面
3. 数据流向图
   text
   配置文件 (.env / application.yml)
   ↓
   模式检测器 (ModeDetector)
   ↓
   服务工厂 (ServiceFactory) → 创建对应模式的服务
   ↓
   控制器 (Controller) → 调用对应模式的服务
   ↓
   服务层 (Service) → 在线/离线实现
   ↓
   数据访问层 (Repository) → 数据库/文件系统
   🔄 模式切换流程
   在线模式流程
   text
   .env (REACT_APP_MODE=online)
   ↓
   前端服务工厂创建 OnlineWorkflowService
   ↓
   API请求 → 后端 WorkflowController
   ↓
   后端服务工厂创建 OnlineWorkflowServiceImpl
   ↓
   WorkflowRepository (JPA) → MySQL数据库
   ↓
   返回结果 → 前端展示
   离线模式流程
   text
   .env (REACT_APP_MODE=offline)
   ↓
   前端服务工厂创建 OfflineWorkflowService
   ↓
   调用 offlineStorage.js (IndexedDB) → 浏览器存储
   ↓
   文件导入/导出 → fileService.js
   ↓
   后端服务工厂创建 OfflineWorkflowServiceImpl
   ↓
   OfflineWorkflowRepository → 本地文件系统
   ↓
   数据同步队列 → 网络恢复后同步
   📦 关键文件列表（已提供代码的）
   ✅ 后端关键文件
   backend/pom.xml - Maven配置

backend/src/main/java/com/workflow/platform/WorkflowPlatformApplication.java - 启动类

backend/src/main/java/com/workflow/platform/config/ModeConfiguration.java - 模式配置

backend/src/main/java/com/workflow/platform/controller/WorkflowController.java - 统一控制器

backend/src/main/java/com/workflow/platform/service/WorkflowServiceFactory.java - 服务工厂

backend/src/main/java/com/workflow/platform/service/impl/OnlineWorkflowServiceImpl.java - 在线实现

backend/src/main/java/com/workflow/platform/service/impl/OfflineWorkflowServiceImpl.java - 离线实现

backend/src/main/java/com/workflow/platform/repository/OfflineWorkflowRepository.java - 离线仓库

backend/src/main/java/com/workflow/platform/util/ModeContext.java - 模式上下文

backend/src/main/java/com/workflow/platform/aspect/ModeAspect.java - 模式切面

backend/src/main/resources/application.yml - 主配置

backend/src/main/resources/application-online.yml - 在线配置

backend/src/main/resources/application-offline.yml - 离线配置

backend/src/main/resources/data-online-mysql.sql - 数据库脚本

backend/src/main/resources/logback-spring.xml - 日志配置

backend/Dockerfile - Docker配置

backend/Dockerfile.offline - 离线Docker配置

✅ 前端关键文件
frontend/package.json - npm配置

frontend/craco.config.js - Craco配置

frontend/.env / .env.online / .env.offline - 环境变量

frontend/src/config/appConfig.js - 应用配置

frontend/src/config/modeConfig.js - 模式配置

frontend/src/services/ServiceFactory.js - 服务工厂

frontend/src/services/api.js - API服务

frontend/src/services/onlineWorkflowService.js - 在线服务

frontend/src/services/offlineWorkflowService.js - 离线服务

frontend/src/components/workflow/WorkflowManager.js - 工作流管理器

frontend/src/components/workflow/OfflineWorkflowDesigner.js - 离线设计器

frontend/src/hooks/useMode.js - 模式钩子

frontend/src/utils/offlineStorage.js - IndexedDB封装

frontend/App.js - 主应用

✅ 脚本文件
scripts/start-online.sh - 启动在线模式

scripts/start-offline.sh - 启动离线模式

scripts/stop-all.sh - 停止所有服务

scripts/build-all.sh - 完整构建

🚀 快速启动指南
1. 在线模式启动
   bash
# 方式1: 使用脚本
./scripts/start-online.sh

# 方式2: 手动启动
cd backend && mvn spring-boot:run -Ponline
cd frontend && npm run start:online
2. 离线模式启动
   bash
# 方式1: 使用脚本
./scripts/start-offline.sh

# 方式2: 手动启动
cd backend && mvn spring-boot:run -Poffline
cd frontend && npm run start:offline
3. Docker部署
   bash
# 在线模式
docker-compose -f docker/docker-compose-online.yml up -d

# 离线模式
docker-compose -f docker/docker-compose-offline.yml up -d
📊 模式对比表
特性	在线模式	离线模式
数据存储	MySQL数据库	本地文件系统
部署复杂度	较高（需要数据库）	较低（单机部署）
网络要求	必须联网	可完全离线
用户协作	支持多用户实时协作	单用户或异步协作
数据同步	实时同步	手动导入导出
适合场景	团队协作、企业部署	个人使用、演示、网络不稳定环境
🎯 功能模块说明
核心功能模块
工作流设计器 - 可视化拖拽设计业务流程

节点配置器 - 配置各个节点的验证规则

连接器管理 - 管理数据库、API等连接配置

执行监控 - 实时监控工作流执行状态

数据比对 - 对比节点前后的数据变化

文件管理 - 导入/导出工作流配置

模式切换 - 在线/离线模式无缝切换

扩展功能模块
模板市场 - 预置业务流程模板

版本控制 - 工作流版本管理和回滚

权限管理 - RBAC权限控制系统

性能监控 - 链路执行性能分析

告警通知 - 失败节点自动告警

数据回放 - 失败流程的数据回放和调试

📈 项目扩展建议
第一阶段（基础版）
实现核心的工作流设计器

完成在线/离线模式切换

实现基本的节点执行和验证

第二阶段（增强版）
添加更多节点类型和连接器

实现数据同步和冲突解决

添加用户管理和权限控制

第三阶段（企业版）
实现集群部署和高可用

添加审计日志和安全审计

集成第三方系统和API市场

这个项目结构提供了完整的通用业务流程链路可视化平台实现，支持在线和离线两种模式，适合各种业务场景。您可以根据实际需求选择实现其中的功能模块。