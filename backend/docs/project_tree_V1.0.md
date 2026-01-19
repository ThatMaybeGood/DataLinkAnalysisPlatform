# 📁 最终完整项目结构

text

```
workflow-visualization-platform/
├── 📂 backend/                           # Spring Boot后端项目
│   ├── 📂 src/main/java/com/workflow/platform/
│   │   ├── 📄 Application.java           # 主启动类
│   │   ├── 📂 annotation/                # 自定义注解
│   │   │   ├── 📄 RequireMode.java
│   │   │   ├── 📄 OperationLog.java
│   │   │   ├── 📄 DataPermission.java
│   │   │   ├── 📄 RateLimiter.java
│   │   │   ├── 📄 DataEncrypt.java
│   │   │   └── 📄 ValidationCheck.java
│   │   ├── 📂 aspect/                    # AOP切面
│   │   │   ├── 📄 ModeAspect.java
│   │   │   ├── 📄 LogAspect.java
│   │   │   ├── 📄 PermissionAspect.java
│   │   │   ├── 📄 RateLimitAspect.java
│   │   │   ├── 📄 EncryptAspect.java
│   │   │   └── 📄 ValidationAspect.java
│   │   ├── 📂 component/                 # 核心组件（全部新增）
│   │   │   ├── 📄 ModeManager.java
│   │   │   ├── 📄 WorkflowGraphManager.java
│   │   │   ├── 📄 OfflineDataManager.java
│   │   │   ├── 📄 SyncQueueManager.java
│   │   │   ├── 📄 ConflictResolutionStrategy.java
│   │   │   ├── 📄 ModeConsistencyChecker.java
│   │   │   ├── 📄 HeartbeatManager.java
│   │   │   └── 📄 WebSocketHandler.java
│   │   ├── 📂 config/                    # 配置类
│   │   │   ├── 📄 WebConfig.java
│   │   │   ├── 📄 SecurityConfig.java
│   │   │   ├── 📄 DatabaseConfig.java
│   │   │   ├── 📄 ModeConfiguration.java
│   │   │   ├── 📄 CacheConfig.java
│   │   │   ├── 📄 AsyncConfig.java
│   │   │   ├── 📄 SwaggerConfig.java
│   │   │   ├── 📄 FileStorageConfig.java
│   │   │   └── 📄 WebSocketConfig.java
│   │   ├── 📂 constants/                 # 常量定义（新增）
│   │   │   ├── 📄 SystemConstants.java
│   │   │   ├── 📄 WorkflowConstants.java
│   │   │   ├── 📄 ErrorCodeConstants.java
│   │   │   ├── 📄 CacheConstants.java
│   │   │   ├── 📄 FileConstants.java
│   │   │   └── 📄 ValidationConstants.java
│   │   ├── 📂 controller/                # 控制器层
│   │   │   ├── 📄 WorkflowController.java
│   │   │   ├── 📄 NodeController.java
│   │   │   ├── 📄 ValidationController.java
│   │   │   ├── 📄 ConnectorController.java
│   │   │   ├── 📄 ExecutionController.java
│   │   │   ├── 📄 FileController.java
│   │   │   ├── 📄 CategoryController.java
│   │   │   ├── 📄 ImportExportController.java
│   │   │   ├── 📄 StatisticsController.java
│   │   │   ├── 📄 DashboardController.java
│   │   │   ├── 📄 SyncController.java
│   │   │   └── 📄 CoordinationController.java
│   │   ├── 📂 enums/                     # 枚举类（新增）
│   │   │   ├── 📄 ModeType.java
│   │   │   ├── 📄 WorkflowStatus.java
│   │   │   ├── 📄 NodeType.java
│   │   │   ├── 📄 ExecutionStatus.java
│   │   │   ├── 📄 ValidationType.java
│   │   │   ├── 📄 SyncStatus.java
│   │   │   ├── 📄 FileType.java
│   │   │   └── 📄 PermissionType.java
│   │   ├── 📂 exception/                 # 异常处理
│   │   │   ├── 📄 ModeNotAllowedException.java
│   │   │   ├── 📄 GlobalExceptionHandler.java
│   │   │   ├── 📄 WorkflowException.java
│   │   │   ├── 📄 NodeException.java
│   │   │   ├── 📄 ValidationException.java
│   │   │   ├── 📄 FileStorageException.java
│   │   │   ├── 📄 SyncException.java
│   │   │   ├── 📄 ConflictException.java
│   │   │   ├── 📄 ImportExportException.java
│   │   │   ├── 📄 PermissionException.java
│   │   │   ├── 📄 AuthenticationException.java
│   │   │   ├── 📄 BusinessException.java
│   │   │   ├── 📄 OfflineException.java
│   │   │   └── 📄 ModeConsistencyException.java
│   │   ├── 📂 filter/                    # 过滤器（新增）
│   │   │   ├── 📄 ModeCheckFilter.java
│   │   │   ├── 📄 AuthenticationFilter.java
│   │   │   ├── 📄 LogFilter.java
│   │   │   ├── 📄 RequestWrapperFilter.java
│   │   │   └── 📄 ClientInfoFilter.java
│   │   ├── 📂 interceptor/               # 拦截器（新增）
│   │   │   ├── 📄 ModeConsistencyInterceptor.java
│   │   │   └── 📄 RequestLogInterceptor.java
│   │   ├── 📂 listener/                  # 监听器（新增）
│   │   │   ├── 📄 ApplicationStartupListener.java
│   │   │   ├── 📄 ModeChangeListener.java
│   │   │   └── 📄 FileChangeListener.java
│   │   ├── 📂 model/                     # 数据模型层
│   │   │   ├── 📂 entity/                # 数据库实体
│   │   │   │   ├── 📄 WorkflowEntity.java
│   │   │   │   ├── 📄 NodeEntity.java
│   │   │   │   ├── 📄 ValidationRuleEntity.java
│   │   │   │   ├── 📄 ConnectorEntity.java
│   │   │   │   ├── 📄 ExecutionEntity.java
│   │   │   │   ├── 📄 CategoryEntity.java
│   │   │   │   ├── 📄 WorkflowAliasEntity.java
│   │   │   │   ├── 📄 UserEntity.java
│   │   │   │   ├── 📄 RoleEntity.java
│   │   │   │   ├── 📄 PermissionEntity.java
│   │   │   │   ├── 📄 OperationLogEntity.java
│   │   │   │   ├── 📄 SyncLogEntity.java
│   │   │   │   └── 📄 FileStorageEntity.java
│   │   │   ├── 📂 dto/                   # 数据传输对象
│   │   │   │   ├── 📄 WorkflowDTO.java
│   │   │   │   ├── 📄 NodeDTO.java
│   │   │   │   ├── 📄 ValidationRuleDTO.java
│   │   │   │   ├── 📄 ModeConfigDTO.java
│   │   │   │   ├── 📄 CategoryDTO.java
│   │   │   │   ├── 📄 WorkflowAliasDTO.java
│   │   │   │   ├── 📄 UserDTO.java
│   │   │   │   ├── 📄 LoginDTO.java
│   │   │   │   ├── 📄 ImportExportDTO.java
│   │   │   │   ├── 📄 SearchDTO.java
│   │   │   │   ├── 📄 PageDTO.java
│   │   │   │   ├── 📄 StatisticsDTO.java
│   │   │   │   ├── 📄 SyncTaskDTO.java
│   │   │   │   ├── 📄 HeartbeatDTO.java
│   │   │   │   ├── 📄 HeartbeatResponse.java
│   │   │   │   ├── 📄 ClientStatusDTO.java
│   │   │   │   ├── 📄 ModeConsistencyReportDTO.java
│   │   │   │   ├── 📄 SystemStatusDTO.java
│   │   │   │   ├── 📄 NotificationDTO.java
│   │   │   │   ├── 📄 CoordinationStatsDTO.java
│   │   │   │   └── 📄 WebSocketMessage.java
│   │   │   └── 📂 vo/                    # 视图对象
│   │   │       ├── 📄 WorkflowVO.java
│   │   │       ├── 📄 WorkflowFileInfoVO.java
│   │   │       ├── 📄 ExecutionResultVO.java
│   │   │       ├── 📄 CategoryVO.java
│   │   │       ├── 📄 DashboardVO.java
│   │   │       ├── 📄 UserVO.java
│   │   │       ├── 📄 StatisticsVO.java
│   │   │       ├── 📄 WorkflowGraphVO.java
│   │   │       ├── 📄 FileListVO.java
│   │   │       └── 📄 SystemStatusVO.java
│   │   ├── 📂 repository/                # 数据访问层
│   │   │   ├── 📄 WorkflowRepository.java
│   │   │   ├── 📄 OfflineWorkflowRepository.java
│   │   │   ├── 📄 NodeRepository.java
│   │   │   ├── 📄 OfflineNodeRepository.java
│   │   │   ├── 📄 ValidationRuleRepository.java
│   │   │   ├── 📄 OfflineValidationRepository.java
│   │   │   ├── 📄 CategoryRepository.java
│   │   │   ├── 📄 OfflineCategoryRepository.java
│   │   │   ├── 📄 WorkflowAliasRepository.java
│   │   │   ├── 📄 UserRepository.java
│   │   │   ├── 📄 OperationLogRepository.java
│   │   │   ├── 📄 SyncLogRepository.java
│   │   │   └── 📄 StatisticsRepository.java
│   │   ├── 📂 service/                   # 服务层
│   │   │   ├── 📂 impl/
│   │   │   │   ├── 📄 OnlineWorkflowServiceImpl.java
│   │   │   │   ├── 📄 OfflineWorkflowServiceImpl.java
│   │   │   │   ├── 📄 SyncServiceImpl.java
│   │   │   │   └── ...其他服务实现
│   │   │   ├── 📄 WorkflowService.java
│   │   │   ├── 📄 WorkflowServiceFactory.java
│   │   │   ├── 📄 NodeService.java
│   │   │   ├── 📄 ValidationService.java
│   │   │   ├── 📄 ConnectorService.java
│   │   │   ├── 📄 ExecutionService.java
│   │   │   ├── 📄 FileStorageService.java
│   │   │   ├── 📄 CategoryService.java
│   │   │   ├── 📄 WorkflowAliasService.java
│   │   │   ├── 📄 UserService.java
│   │   │   ├── 📄 AuthService.java
│   │   │   ├── 📄 StatisticsService.java
│   │   │   ├── 📄 ImportExportService.java
│   │   │   ├── 📄 SyncService.java
│   │   │   ├── 📄 CacheService.java
│   │   │   ├── 📄 ValidationRuleEngine.java
│   │   │   ├── 📄 WorkflowExecutionEngine.java
│   │   │   └── 📄 NodeExecutionEngine.java
│   │   ├── 📂 task/                      # 定时任务（新增）
│   │   │   ├── 📄 DataSyncTask.java
│   │   │   ├── 📄 FileCleanupTask.java
│   │   │   ├── 📄 StatisticsTask.java
│   │   │   └── 📄 BackupTask.java
│   │   ├── 📂 util/                      # 工具类
│   │   │   ├── 📄 ModeContext.java
│   │   │   ├── 📄 FileUtil.java
│   │   │   ├── 📄 JsonUtil.java
│   │   │   ├── 📄 CryptUtil.java
│   │   │   ├── 📄 WorkflowGraphUtil.java
│   │   │   ├── 📄 NodeConnectionUtil.java
│   │   │   ├── 📄 ValidationRuleUtil.java
│   │   │   ├── 📄 DataTransformUtil.java
│   │   │   ├── 📄 FilePathUtil.java
│   │   │   ├── 📄 WorkflowTemplateUtil.java
│   │   │   ├── 📄 OfflineSyncUtil.java
│   │   │   ├── 📄 WorkflowParserUtil.java
│   │   │   ├── 📄 ExpressionEvaluator.java
│   │   │   └── 📄 WorkflowValidator.java
│   │   ├── 📂 validation/                # 验证器（新增）
│   │   │   ├── 📄 WorkflowValidator.java
│   │   │   ├── 📄 NodeValidator.java
│   │   │   └── 📄 ValidationRuleValidator.java
│   │   └── 📂 websocket/                 # WebSocket相关（新增）
│   │       ├── 📄 WebSocketConfig.java
│   │       ├── 📄 WebSocketHandler.java
│   │       └── 📄 WebSocketMessageDispatcher.java
│   └── 📂 src/main/resources/
│       ├── 📄 application.yml            # 主配置文件
│       ├── 📄 application-common.yml     # 公共配置
│       ├── 📄 application-online.yml     # 在线模式配置
│       ├── 📄 application-offline.yml    # 离线模式配置
│       ├── 📄 application-security.yml   # 安全配置
│       ├── 📄 logback-spring.xml         # 日志配置
│       ├── 📄 data-online-mysql.sql                   # 数据初始化脚本
│       ├── 📂 templates/                 # 模板文件
│       │   ├── 📂 workflow/
│       │   │   ├── 📄 basic-workflow.json
│       │   │   ├── 📄 approval-workflow.json
│       │   │   └── 📄 data-processing-workflow.json
│       │   ├── 📂 nodeEntity/
│       │   │   ├── 📄 start-nodeEntity.json
│       │   │   ├── 📄 end-nodeEntity.json
│       │   │   ├── 📄 action-nodeEntity.json
│       │   │   └── 📄 decision-nodeEntity.json
│       │   └── 📂 validation/
│       │       ├── 📄 required-rule.json
│       │       ├── 📄 format-rule.json
│       │       └── 📄 range-rule.json
│       ├── 📂 scripts/                   # SQL脚本
│       │   ├── 📄 init-online.sql
│       │   ├── 📄 init-offline.sql
│       │   ├── 📄 init-test-data-online-mysql.sql
│       │   └── 📂 migration/
│       │       ├── 📄 V1_0_0__initial_schema.sql
│       │       ├── 📄 V1_1_0__add_offline_support.sql
│       │       └── 📄 V1_2_0__add_sync_tables.sql
│       ├── 📂 config/                    # 额外配置
│       │   ├── 📂 workflow-templates/
│       │   │   ├── 📄 order-process.json
│       │   │   ├── 📄 user-registration.json
│       │   │   └── 📄 data-validation.json
│       │   ├── 📂 validation-rules/
│       │   │   ├── 📄 email-validation.json
│       │   │   ├── 📄 phone-validation.json
│       │   │   └── 📄 date-validation.json
│       │   └── 📂 nodeEntity-templates/
│       │       ├── 📄 api-call-nodeEntity.json
│       │       ├── 📄 database-nodeEntity.json
│       │       └── 📄 notification-nodeEntity.json
│       ├── 📂 offline-templates/         # 离线模板
│       │   ├── 📄 workflow-template.json
│       │   ├── 📄 nodeEntity-template.json
│       │   ├── 📄 validation-template.json
│       │   └── 📄 sync-config-template.json
│       └── 📂 i18n/                      # 国际化
│           ├── 📄 messages.properties
│           ├── 📄 messages_en.properties
│           └── 📄 messages_zh_CN.properties
│
├── 📂 frontend/                          # React前端项目
│   ├── 📂 public/
│   │   ├── 📄 index.html
│   │   ├── 📄 favicon.ico
│   │   └── 📂 assets/
│   │       ├── 📂 images/
│   │       │   ├── 📄 logo.png
│   │       │   ├── 📄 workflow-icon.png
│   │       │   └── 📄 nodeEntity-icons/
│   │       └── 📂 fonts/
│   │           └── ...字体文件
│   ├── 📂 src/
│   │   ├── 📂 assets/                    # 静态资源
│   │   │   ├── 📂 images/
│   │   │   │   ├── 📄 workflow-bg.jpg
│   │   │   │   ├── 📄 nodeEntity-icons/
│   │   │   │   └── 📄 ui-icons/
│   │   │   └── 📂 styles/
│   │   │       ├── 📄 global.css
│   │   │       ├── 📄 variables.css
│   │   │       ├── 📄 antd-theme.less
│   │   │       └── 📄 components.css
│   │   ├── 📂 components/                # 组件
│   │   │   ├── 📂 common/                # 通用组件
│   │   │   │   ├── 📂 Layout/
│   │   │   │   │   ├── 📄 Header.js
│   │   │   │   │   ├── 📄 Sider.js
│   │   │   │   │   ├── 📄 Footer.js
│   │   │   │   │   └── 📄 Layout.css
│   │   │   │   ├── 📄 ModeIndicator.js
│   │   │   │   ├── 📄 NetworkStatus.js
│   │   │   │   ├── 📄 Loading.js
│   │   │   │   ├── 📄 ErrorBoundary.js
│   │   │   │   ├── 📄 NotificationCenter.js
│   │   │   │   └── 📄 SyncStatusIndicator.js
│   │   │   └── 📂 workflow/              # 工作流组件
│   │   │       ├── 📄 WorkflowManager.js
│   │   │       ├── 📄 WorkflowDesigner.js
│   │   │       ├── 📄 OfflineWorkflowDesigner.js
│   │   │       ├── 📄 NodeConfigEditor.js
│   │   │       ├── 📄 ValidationRuleEditor.js
│   │   │       ├── 📄 DataDiffViewer.js
│   │   │       ├── 📄 WorkflowMapView.js
│   │   │       ├── 📄 WorkflowTimeline.js
│   │   │       ├── 📄 WorkflowGraph.js
│   │   │       ├── 📄 NodePalette.js
│   │   │       ├── 📄 ConnectionLine.js
│   │   │       ├── 📄 PropertiesPanel.js
│   │   │       ├── 📄 ExecutionMonitor.js
│   │   │       └── 📄 CoordinationPanel.js
│   │   ├── 📂 config/                    # 配置文件
│   │   │   ├── 📄 appConfig.js
│   │   │   ├── 📄 modeConfig.js
│   │   │   ├── 📄 apiConfig.js
│   │   │   ├── 📄 routeConfig.js
│   │   │   └── 📄 themeConfig.js
│   │   ├── 📂 hooks/                     # 自定义Hooks
│   │   │   ├── 📄 useMode.js
│   │   │   ├── 📄 useWorkflow.js
│   │   │   ├── 📄 useOfflineStorage.js
│   │   │   ├── 📄 useNetworkStatus.js
│   │   │   ├── 📄 useHeartbeat.js
│   │   │   ├── 📄 useWebSocket.js
│   │   │   ├── 📄 useCoordination.js
│   │   │   ├── 📄 useSync.js
│   │   │   ├── 📄 useNotification.js
│   │   │   └── 📄 useValidation.js
│   │   ├── 📂 pages/                     # 页面组件
│   │   │   ├── 📄 Dashboard.js
│   │   │   ├── 📄 WorkflowList.js
│   │   │   ├── 📄 WorkflowDetail.js
│   │   │   ├── 📄 ExecutionMonitor.js
│   │   │   ├── 📄 DataAnalytics.js
│   │   │   ├── 📄 Settings.js
│   │   │   ├── 📄 CoordinationMonitor.js
│   │   │   ├── 📄 SystemStatus.js
│   │   │   ├── 📄 OfflineManager.js
│   │   │   ├── 📄 SyncManager.js
│   │   │   └── 📄 ConflictResolver.js
│   │   ├── 📂 services/                  # 服务层
│   │   │   ├── 📄 ServiceFactory.js
│   │   │   ├── 📄 BaseWorkflowService.js
│   │   │   ├── 📄 onlineWorkflowService.js
│   │   │   ├── 📄 offlineWorkflowService.js
│   │   │   ├── 📄 onlineNodeService.js
│   │   │   ├── 📄 offlineNodeService.js
│   │   │   ├── 📄 onlineValidationService.js
│   │   │   ├── 📄 offlineValidationService.js
│   │   │   ├── 📄 api.js
│   │   │   ├── 📄 fileService.js
│   │   │   ├── 📄 offlineManager.js
│   │   │   ├── 📄 coordinationService.ts
│   │   │   ├── 📄 heartbeatService.js
│   │   │   ├── 📄 webSocketService.js
│   │   │   ├── 📄 syncService.js
│   │   │   ├── 📄 conflictService.js
│   │   │   ├── 📄 authService.js
│   │   │   └── 📄 exportService.js
│   │   ├── 📂 stores/                    # 状态管理
│   │   │   ├── 📄 workflowStore.js
│   │   │   ├── 📄 connectorStore.js
│   │   │   ├── 📄 executionStore.js
│   │   │   ├── 📄 modeStore.js
│   │   │   ├── 📄 coordinationStore.js
│   │   │   ├── 📄 notificationStore.js
│   │   │   ├── 📄 syncStore.js
│   │   │   ├── 📄 conflictStore.js
│   │   │   └── 📄 userStore.js
│   │   ├── 📂 utils/                     # 工具函数
│   │   │   ├── 📄 offlineStorage.js
│   │   │   ├── 📄 fileUtils.js
│   │   │   ├── 📄 validationUtils.js
│   │   │   ├── 📄 dataTransform.js
│   │   │   ├── 📄 networkMonitor.js
│   │   │   ├── 📄 modeDetector.js
│   │   │   ├── 📄 heartbeatUtils.js
│   │   │   ├── 📄 webSocketUtils.js
│   │   │   ├── 📄 coordinationUtils.js
│   │   │   ├── 📄 syncUtils.js
│   │   │   ├── 📄 conflictUtils.js
│   │   │   ├── 📄 workflowParser.js
│   │   │   ├── 📄 graphLayout.js
│   │   │   └── 📄 dateUtils.js
│   │   ├── 📂 types/                     # TypeScript类型定义
│   │   │   ├── 📄 workflow.ts
│   │   │   ├── 📄 nodeEntity.ts
│   │   │   ├── 📄 coordination.ts
│   │   │   ├── 📄 sync.ts
│   │   │   └── 📄 common.ts
│   │   ├── 📄 App.js
│   │   ├── 📄 App.css
│   │   ├── 📄 index.js
│   │   ├── 📄 routes.js
│   │   └── 📄 setupProxy.js
│   ├── 📄 package.json
│   ├── 📄 craco.config.js
│   ├── 📄 .env
│   ├── 📄 .env.online
│   ├── 📄 .env.offline
│   ├── 📄 .eslintrc.js
│   ├── 📄 .prettierrc
│   ├── 📄 tsconfig.json
│   └── 📄 .gitignore
│
├── 📂 scripts/                           # 脚本目录
│   ├── 📄 start-online.sh
│   ├── 📄 start-offline.sh
│   ├── 📄 stop-all.sh
│   ├── 📄 build-all.sh
│   ├── 📄 deploy-docker.sh
│   ├── 📄 backup-data.sh
│   ├── 📄 reset-database.sh
│   ├── 📄 migrate-data.sh
│   └── 📄 health-check.sh
│
├── 📂 docker/                            # Docker配置
│   ├── 📄 docker-compose.yml
│   ├── 📄 docker-compose-online.yml
│   ├── 📄 docker-compose-offline.yml
│   ├── 📄 Dockerfile-backend
│   ├── 📄 Dockerfile-frontend
│   ├── 📄 Dockerfile-backend-offline
│   ├── 📄 nginx.conf
│   ├── 📄 init.sql
│   └── 📄 .env.docker
│
├── 📂 docs/                              # 文档
│   ├── 📂 api/                           # API文档
│   │   ├── 📄 workflow-api.md
│   │   ├── 📄 nodeEntity-api.md
│   │   ├── 📄 sync-api.md
│   │   ├── 📄 coordination-api.md
│   │   └── 📄 api-overview.md
│   ├── 📂 guide/                         # 使用指南
│   │   ├── 📄 getting-started.md
│   │   ├── 📄 quick-start.md
│   │   ├── 📄 online-mode-guide.md
│   │   ├── 📄 offline-mode-guide.md
│   │   ├── 📄 coordination-guide.md
│   │   ├── 📄 sync-guide.md
│   │   ├── 📄 conflict-resolution-guide.md
│   │   └── 📄 deployment-guide.md
│   ├── 📂 examples/                      # 示例
│   │   ├── 📂 workflows/
│   │   │   ├── 📄 order-process.json
│   │   │   ├── 📄 user-registration.json
│   │   │   ├── 📄 data-validation.json
│   │   │   └── 📄 approval-workflow.json
│   │   ├── 📂 nodes/
│   │   │   ├── 📄 api-call-nodeEntity.json
│   │   │   ├── 📄 database-nodeEntity.json
│   │   │   └── 📄 notification-nodeEntity.json
│   │   └── 📂 configurations/
│   │       ├── 📄 offline-config.json
│   │       ├── 📄 sync-config.json
│   │       └── 📄 conflict-config.json
│   ├── 📄 architecture.md
│   ├── 📄 database-schema.md
│   ├── 📄 component-diagram.md
│   └── 📄 api-design.md
│
├── 📂 data/                              # 数据目录（运行时生成）
│   ├── 📂 online/                        # 在线模式数据
│   │   ├── 📂 database/
│   │   │   ├── 📄 workflow_db.mv.db
│   │   │   └── 📄 workflow_db.trace.db
│   │   ├── 📂 exports/
│   │   │   ├── 📄 export-20240101.json
│   │   │   └── 📄 export-20240102.json
│   │   └── 📂 backups/
│   │       ├── 📄 backup-20240101.zip
│   │       └── 📄 backup-20240102.zip
│   └── 📂 offline/                       # 离线模式数据
│       ├── 📂 workflows/
│       │   ├── 📄 workflow_001.json
│       │   ├── 📄 workflow_002.json
│       │   └── 📄 workflow_003.json
│       ├── 📂 nodes/
│       │   ├── 📄 node_001.json
│       │   ├── 📄 node_002.json
│       │   └── 📄 node_003.json
│       ├── 📂 rules/
│       │   ├── 📄 rule_001.json
│       │   ├── 📄 rule_002.json
│       │   └── 📄 rule_003.json
│       ├── 📂 exports/
│       │   ├── 📄 export-offline-20240101.json
│       │   └── 📄 export-offline-20240102.json
│       ├── 📂 backups/
│       │   ├── 📄 backup-offline-20240101.zip
│       │   └── 📄 backup-offline-20240102.zip
│       ├── 📂 sync/
│       │   ├── 📄 sync-states.json
│       │   ├── 📄 sync-queue.json
│       │   └── 📄 conflict-records.json
│       └── 📄 file-index.json
│
├── 📂 logs/                              # 日志目录（运行时生成）
│   ├── 📄 backend.log
│   ├── 📄 frontend.log
│   ├── 📄 backend-offline.log
│   ├── 📄 frontend-offline.log
│   ├── 📄 sync.log
│   ├── 📄 coordination.log
│   └── 📄 audit.log
│
├── 📄 README.md
├── 📄 LICENSE
├── 📄 .gitignore
├── 📄 pom.xml
├── 📄 .mvn/
│   └── 📄 wrapper/
│       ├── 📄 maven-wrapper.jar
│       └── 📄 maven-wrapper.properties
├── 📄 mvnw
├── 📄 mvnw.cmd
├── 📄 CHANGELOG.md
└── 📄 ROADMAP.md
```



## 🏗️ 完整架构说明

### **1. 后端架构 (Backend)**

text

```
📁 backend/
├── 📂 config/           # 配置管理
│   ├── 模式配置 (online/offline/mixed)
│   ├── 安全配置 (JWT、权限)
│   ├── 数据库配置 (在线MySQL/离线H2)
│   ├── 缓存配置 (Redis/Caffeine)
│   └── WebSocket配置
│
├── 📂 component/        # 核心组件
│   ├── 模式管理器 (ModeManager)
│   ├── 工作流图管理器 (WorkflowGraphManager)
│   ├── 离线数据管理器 (OfflineDataManager)
│   ├── 同步队列管理器 (SyncQueueManager)
│   ├── 冲突解决策略 (ConflictResolutionStrategy)
│   ├── 模式一致性检查器 (ModeConsistencyChecker)
│   ├── 心跳管理器 (HeartbeatManager)
│   └── WebSocket处理器 (WebSocketHandler)
│
├── 📂 controller/       # REST API控制器
│   ├── 工作流管理 (Workflow/Nodes/Validation)
│   ├── 同步管理 (SyncController)
│   ├── 协调管理 (CoordinationController)
│   └── 文件管理 (FileController)
│
├── 📂 service/         # 业务服务层
│   ├── 服务工厂 (ServiceFactory)
│   ├── 在线/离线服务实现
│   ├── 同步服务 (SyncService)
│   ├── 协调服务
│   └── 验证引擎 (ValidationRuleEngine)
│
├── 📂 repository/      # 数据访问层
│   ├── JPA仓库 (在线模式)
│   ├── 文件仓库 (离线模式)
│   └── 混合仓库 (支持两种模式)
│
├── 📂 model/          # 数据模型
│   ├── 实体类 (Entity)
│   ├── DTO (数据传输对象)
│   └── VO (视图对象)
│
├── 📂 util/           # 工具类
│   ├── 工作流图工具 (WorkflowGraphUtil)
│   ├── 离线同步工具 (OfflineSyncUtil)
│   ├── 文件工具 (FileUtil)
│   └── JSON工具 (JsonUtil)
│
├── 📂 enums/          # 枚举类
├── 📂 constants/      # 常量定义
├── 📂 exception/      # 异常处理
├── 📂 filter/         # 过滤器
├── 📂 interceptor/    # 拦截器
├── 📂 listener/       # 监听器
├── 📂 task/          # 定时任务
└── 📂 validation/     # 验证器
```



### **2. 前端架构 (Frontend)**

text

```
📁 frontend/
├── 📂 components/     # 组件库
│   ├── 通用组件 (Layout、Loading、ErrorBoundary)
│   ├── 工作流组件 (Designer、Graph、Editor)
│   └── 协调组件 (ModeIndicator、NetworkStatus、CoordinationPanel)
│
├── 📂 pages/         # 页面组件
│   ├── 工作流页面 (List、Detail、Designer)
│   ├── 监控页面 (ExecutionMonitor、SystemStatus)
│   ├── 协调页面 (CoordinationMonitor)
│   └── 管理页面 (SyncManager、ConflictResolver)
│
├── 📂 services/      # 服务层
│   ├── 服务工厂 (ServiceFactory)
│   ├── 在线/离线服务
│   ├── 协调服务 (coordinationService.ts)
│   ├── 同步服务 (syncService.js)
│   └── WebSocket服务 (webSocketService.js)
│
├── 📂 hooks/         # 自定义Hooks
│   ├── 模式Hook (useMode)
│   ├── 工作流Hook (useWorkflow)
│   ├── 协调Hook (useCoordination)
│   ├── WebSocketHook (useWebSocket)
│   └── 心跳Hook (useHeartbeat)
│
├── 📂 stores/        # 状态管理
│   ├── 工作流状态 (workflowStore)
│   ├── 模式状态 (modeStore)
│   ├── 协调状态 (coordinationStore)
│   └── 同步状态 (syncStore)
│
├── 📂 utils/         # 工具函数
│   ├── 离线存储 (offlineStorage.js)
│   ├── 网络监控 (networkMonitor.js)
│   ├── 协调工具 (coordinationUtils.js)
│   └── 工作流解析 (workflowParser.js)
│
├── 📂 types/         # TypeScript类型定义
├── 📂 config/        # 应用配置
└── 📂 assets/        # 静态资源
```



### **3. 核心特性实现状态**

text

```
✅ 已完成的核心功能:
├── 第一阶段：基础框架
│   ├── 模式管理器 (ModeManager)
│   ├── 工作流图管理器 (WorkflowGraphManager)
│   ├── 完整的枚举和常量定义
│   ├── 配置文件体系 (在线/离线/公共/安全)
│   └── 工具类支持 (FileUtil、JsonUtil等)
│
├── 第二阶段：离线模式核心
│   ├── 离线数据管理器 (OfflineDataManager)
│   ├── 同步队列管理器 (SyncQueueManager)
│   ├── 冲突解决策略 (ConflictResolutionStrategy)
│   ├── 离线同步工具 (OfflineSyncUtil)
│   └── 完整的同步API和异常处理
│
├── 第三阶段：前后端协调
│   ├── 模式一致性检查器 (ModeConsistencyChecker)
│   ├── 心跳管理器 (HeartbeatManager)
│   ├── WebSocket实时通信 (WebSocketHandler)
│   ├── 协调控制器 (CoordinationController)
│   └── 前端协调服务 (TypeScript实现)
│
└── 完整的基础设施
    ├── 配置管理 (多环境配置)
    ├── 异常处理 (全局异常处理器)
    ├── 日志管理 (Logback配置)
    ├── 安全框架 (Spring Security + JWT)
    └── API文档 (Swagger/OpenAPI)
```



### **4. 数据流和控制流**

text

```
📋 在线模式数据流:
1. 客户端请求 → API Gateway → 控制器
2. 控制器 → 服务工厂 → OnlineWorkflowService
3. OnlineWorkflowService → JPA Repository → MySQL数据库
4. 返回结果 → 客户端

📋 离线模式数据流:
1. 客户端请求 → API Gateway → 控制器
2. 控制器 → 服务工厂 → OfflineWorkflowService
3. OfflineWorkflowService → OfflineDataManager → 本地文件系统
4. 返回结果 → 客户端
5. (可选) 同步队列 → SyncQueueManager → 批量同步到在线系统

📋 协调控制流:
1. 客户端启动 → 初始化协调服务 → 注册客户端模式
2. 定期心跳 → HeartbeatManager → 状态验证和模式一致性检查
3. WebSocket连接 → 实时通信和状态推送
4. 模式变更 → ModeConsistencyChecker → 一致性验证和冲突解决
5. 数据同步 → SyncQueueManager → 队列化同步任务
```



### **5. 部署和运维结构**

text

```
📁 部署结构:
├── 📂 docker/          # Docker容器化配置
├── 📂 scripts/         # 自动化脚本
├── 📂 logs/           # 日志目录
├── 📂 data/           # 数据存储目录
└── 📂 docs/           # 文档和指南

📋 支持多种部署方式:
1. 单体部署: 所有服务在一个JAR中
2. Docker部署: 使用docker-compose编排
3. 集群部署: 支持水平扩展
4. 离线部署: 完全断网环境运行
```



## 🎯 项目特性总结

### **✅ 已完成的核心功能**

1. **双重模式支持**
   - 在线模式 (MySQL + Redis)
   - 离线模式 (文件系统 + H2)
   - 混合模式 (自动切换)
2. **工作流可视化**
   - 图形化工作流设计器
   - 节点连接和布局
   - 实时预览和验证
3. **数据同步机制**
   - 智能同步队列
   - 冲突检测和解决
   - 断点续传和重试
4. **前后端协调**
   - 实时心跳检测
   - 模式一致性验证
   - WebSocket实时通信
   - 状态同步和通知
5. **完整的生态系统**
   - 安全认证和授权
   - 监控和日志
   - 备份和恢复
   - 导入导出功能

### **🔧 技术栈**

- **后端**: Spring Boot + Spring Security + JPA + WebSocket
- **前端**: React + TypeScript + Ant Design + Zustand
- **数据库**: MySQL (在线) / H2 (离线) / Redis (缓存)
- **部署**: Docker + Nginx + Shell脚本
- **监控**: Logback + 自定义监控

### **📈 可扩展性**

1. **插件化架构**: 服务工厂模式支持新功能扩展
2. **模块化设计**: 清晰的层次结构和接口定义
3. **配置驱动**: 通过配置文件控制行为和模式
4. **API优先**: 完整的REST API和WebSocket接口

### **🛡️ 可靠性和容错**

1. **数据完整性**: 事务支持和数据验证
2. **故障恢复**: 自动重试和备份机制
3. **网络容错**: 离线模式支持和断线重连
4. **错误处理**: 全面的异常处理和日志记录

这个完整的项目结构已经具备了生产环境部署的所有必要组件，包括开发、测试、部署、监控和运维的完整支持。