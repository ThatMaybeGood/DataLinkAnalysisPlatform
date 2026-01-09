四、部署和运行
1. 不同模式的启动方式
   在线模式启动：
   bash
# 方式1：通过环境变量
export APP_MODE=online
npm start  # 前端
mvn spring-boot:run -Dspring.profiles.active=online  # 后端

# 方式2：通过配置文件修改
# 修改 frontend/.env 中的 REACT_APP_MODE=online
# 修改 backend/src/main/resources/application.yml 中的 app.mode=online

# 方式3：通过命令行参数
java -jar workflow-platform.jar --app.mode=online
离线模式启动：
bash
# 方式1：通过环境变量
export APP_MODE=offline
npm start  # 前端
mvn spring-boot:run -Dspring.profiles.active=offline  # 后端

# 方式2：通过配置文件修改
# 修改 frontend/.env 中的 REACT_APP_MODE=offline
# 修改 backend/src/main/resources/application.yml 中的 app.mode=offline

# 方式3：通过命令行参数
java -jar workflow-platform.jar --app.mode=offline