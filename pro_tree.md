workflow-visualization-platform/
├── frontend/
│   ├── package.json
│   ├── .env
│   ├── .env.online
│   ├── .env.offline
│   ├── public/
│   │   └── index.html
│   └── src/
│       ├── App.js
│       ├── index.js
│       ├── config/
│       │   ├── appConfig.js           # ✅ 已提供
│       │   └── modeConfig.js          # ✅ 已提供
│       ├── services/
│       │   ├── ServiceFactory.js      # ✅ 已提供
│       │   ├── BaseWorkflowService.js # ✅ 已提供基类
│       │   ├── onlineWorkflowService.js # ✅ 已提供
│       │   ├── offlineWorkflowService.js # ✅ 已提供
│       │   ├── onlineNodeService.js   # 类似workflowService
│       │   ├── offlineNodeService.js  # 类似workflowService
│       │   ├── onlineValidationService.js
│       │   ├── offlineValidationService.js
│       │   ├── onlineConnectorService.js
│       │   ├── offlineConnectorService.js
│       │   ├── onlineFileService.js
│       │   └── offlineFileService.js
│       ├── components/
│       │   └── workflow/
│       │       ├── WorkflowManager.js # ✅ 已提供
│       │       ├── WorkflowDesigner.js # 工作流设计器
│       │       ├── NodeConfigEditor.js # 节点配置编辑器
│       │       └── ValidationRuleEditor.js # 验证规则编辑器
│       ├── hooks/
│       │   └── useMode.js             # ✅ 已提供
│       └── utils/
│           ├── offlineStorage.js      # ✅ 已提供
│           ├── fileUtils.js
│           └── validationUtils.js
├── backend/
│   ├── pom.xml
│   ├── src/main/java/com/workflow/platform/
│   │   ├── WorkflowPlatformApplication.java # ✅ 已提供
│   │   ├── config/
│   │   │   ├── ModeCondition.java     # ✅ 已提供
│   │   │   ├── ConditionalOnMode.java # ✅ 已提供
│   │   │   ├── ModeConfiguration.java # ✅ 已提供
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   ├── WorkflowController.java # ✅ 已提供
│   │   │   ├── NodeController.java    # 类似WorkflowController
│   │   │   ├── ValidationController.java
│   │   │   ├── ConnectorController.java
│   │   │   ├── ExecutionController.java
│   │   │   └── FileController.java    # 文件导入导出
│   │   ├── service/
│   │   │   ├── WorkflowService.java   # ✅ 已提供接口
│   │   │   ├── WorkflowServiceFactory.java # ✅ 已提供
│   │   │   ├── impl/
│   │   │   │   ├── OnlineWorkflowServiceImpl.java # ✅ 已提供
│   │   │   │   └── OfflineWorkflowServiceImpl.java # ✅ 已提供
│   │   │   ├── NodeService.java
│   │   │   ├── ValidationService.java
│   │   │   ├── ConnectorService.java
│   │   │   ├── ExecutionService.java
│   │   │   └── FileService.java
│   │   ├── repository/
│   │   │   ├── WorkflowRepository.java # JPA接口
│   │   │   ├── OfflineWorkflowRepository.java # ✅ 已提供
│   │   │   ├── NodeRepository.java
│   │   │   └── OfflineNodeRepository.java
│   │   ├── model/
│   │   │   ├── entity/
│   │   │   │   ├── WorkflowEntity.java
│   │   │   │   ├── NodeEntity.java
│   │   │   │   └── ValidationRuleEntity.java
│   │   │   ├── dto/
│   │   │   │   ├── WorkflowDTO.java
│   │   │   │   ├── NodeDTO.java
│   │   │   │   └── ValidationRuleDTO.java
│   │   │   └── vo/
│   │   │       ├── WorkflowVO.java
│   │   │       ├── NodeVO.java
│   │   │       └── ValidationRuleVO.java
│   │   ├── util/
│   │   │   ├── ModeContext.java       # ✅ 已提供
│   │   │   ├── FileUtil.java
│   │   │   └── JsonUtil.java
│   │   └── aspect/
│   │       └── ModeAspect.java        # ✅ 已提供
│   └── src/main/resources/
│       ├── application.yml            # ✅ 已提供
│       ├── application-online.yml     # ✅ 已提供
│       ├── application-offline.yml    # ✅ 已提供
│       ├── data.sql                   # 初始化数据（在线模式）
│       └── logback-spring.xml         # 日志配置
└── scripts/
├── start-online.sh                # ✅ 已提供
├── start-offline.sh               # ✅ 已提供
├── stop-all.sh                    # ✅ 已提供
└── build-all.sh                   # 构建脚本