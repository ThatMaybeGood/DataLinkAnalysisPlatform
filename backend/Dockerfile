# 基础镜像
FROM openjdk:11-jre-slim

# 维护者信息
LABEL maintainer="workflow-platform@example.com"
LABEL version="1.0.0"
LABEL description="Workflow Platform - Business Process Visualization"

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建应用目录
RUN mkdir -p /app && mkdir -p /app/logs && mkdir -p /app/data
WORKDIR /app

# 添加非root用户
RUN groupadd -r workflow && useradd -r -g workflow workflow
RUN chown -R workflow:workflow /app
USER workflow

# 复制应用文件
COPY --chown=workflow:workflow target/workflow-platform-*.jar app.jar
COPY --chown=workflow:workflow src/main/resources/application*.yml ./

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM参数
ENV JAVA_OPTS="-server -Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dapp.mode=${APP_MODE:-online} -jar app.jar"]