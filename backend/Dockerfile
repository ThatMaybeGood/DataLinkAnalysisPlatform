# ============================================================
# DataLink 平台 · 后端镜像
# 构建：docker build -t datalink-backend ./backend
# 运行（部署模式 MySQL，生产用）：
#   docker run -p 8080:8080 \
#     -e SPRING_PROFILES_ACTIVE=mysql \
#     -e DB_URL=jdbc:mysql://host:3306/datalink?... \
#     -e DB_USER=datalink -e DB_PASSWORD=xxx \
#     -e DATALINK_JWT_SECRET=xxx \
#     datalink-backend
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/datalink-backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
