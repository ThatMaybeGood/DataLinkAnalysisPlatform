# 工作流可视化平台 - 最终项目结构图

## 📁 完整项目结构总览

text

```
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
```



## 🏗️ 核心模块说明

### 1. **后端模块 (`backend/`)**

text

```
backend/
├── config/           # 配置类 - 模式切换、安全、数据库等
├── controller/       # REST API 控制器 - 统一入口，模式感知
├── service/         # 业务服务层 - 接口+两种模式实现
├── repository/      # 数据访问层 - 数据库+文件系统
├── model/          # 数据模型 - 实体、DTO、VO
├── util/           # 工具类 - 模式上下文、文件操作等
├── aspect/         # AOP切面 - 模式验证、日志等
└── resources/      # 配置文件 - 在线/离线模式配置
```



### 2. **前端模块 (`frontend/`)**

text

```
frontend/
├── components/     # 组件库 - 通用+工作流专用
├── services/      # 服务层 - 服务工厂+两种模式实现
├── hooks/         # 自定义Hooks - 模式、网络状态等
├── stores/        # 状态管理 - Zustand/Redux
├── utils/         # 工具函数 - IndexedDB、文件操作等
├── config/        # 配置管理 - 应用配置、模式配置
└── pages/         # 页面组件 - 各个功能页面
```



### 3. **数据流向图**

text

```
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
```



## 🔄 模式切换流程

### **在线模式流程**

text

```
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
```



### **离线模式流程**

text

```
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
```