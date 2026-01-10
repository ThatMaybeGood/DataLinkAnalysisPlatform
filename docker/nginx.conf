version: '3.8'

services:
  # 在线模式数据库
  mysql:
    image: mysql:8.0
    environment:
      - MYSQL_ROOT_PASSWORD=root123
      - MYSQL_DATABASE=workflow_db
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - workflow-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 20s
      retries: 10

  # Redis缓存
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    networks:
      - workflow-network

  # 后端API服务
  backend:
    build:
      context: ../backend
      dockerfile: Dockerfile
    environment:
      - APP_MODE=${APP_MODE:-online}  # 从环境变量读取模式
      - SPRING_PROFILES_ACTIVE=${APP_MODE:-online}
      - DATABASE_URL=jdbc:mysql://mysql:3306/workflow_db
      - REDIS_HOST=redis
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    networks:
      - workflow-network
    volumes:
      - ./data:/app/data  # 挂载数据目录

  # 前端Web服务
  frontend:
    build:
      context: ../frontend
      dockerfile: Dockerfile
      args:
        - REACT_APP_MODE=${APP_MODE:-online}  # 构建时注入模式
    environment:
      - REACT_APP_API_BASE_URL=http://localhost:8080/api
    ports:
      - "80:80"
    depends_on:
      - backend
    networks:
      - workflow-network

networks:
  workflow-network:
    driver: bridge

volumes:
  mysql_data:
  data: