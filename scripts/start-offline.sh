#!/bin/bash

# 工作流平台启动脚本
# 使用方式: ./start-offline.sh [online|offline]

set -e

# 默认模式
MODE=${1:-"online"}

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}  工作流平台启动脚本${NC}"
echo -e "${GREEN}  模式: ${MODE}${NC}"
echo -e "${GREEN}=======================================${NC}"

# 检查模式
if [[ "$MODE" != "online" && "$MODE" != "offline" ]]; then
    echo -e "${RED}错误: 模式必须是 'online' 或 'offline'${NC}"
    exit 1
fi

# 创建数据目录
echo "创建数据目录..."
mkdir -p ./data/workflows ./data/nodes ./data/rules ./data/exports ./data/backups ./logs

# 设置环境变量
export APP_MODE=$MODE
export REACT_APP_MODE=$MODE

# 前端构建
echo -e "${YELLOW}构建前端应用...${NC}"
cd frontend
if [[ "$MODE" == "offline" ]]; then
    npm run build:offline
else
    npm run build
fi
cd ..

# 后端构建
echo -e "${YELLOW}构建后端应用...${NC}"
cd backend
if [[ "$MODE" == "offline" ]]; then
    mvn clean package -Poffline
else
    mvn clean package -Ponline
fi
cd ..

# 启动服务
echo -e "${YELLOW}启动服务...${NC}"
if [[ "$MODE" == "offline" ]]; then
    # 离线模式：不需要数据库
    docker-compose -f docker-compose-offline.yml up -d
else
    # 在线模式：需要数据库
    docker-compose up -d
fi

# 等待服务启动
echo -e "${YELLOW}等待服务启动...${NC}"
sleep 10

# 检查服务状态
echo -e "${YELLOW}检查服务状态...${NC}"

# 检查后端服务
if curl -s http://localhost:8080/api/health > /dev/null; then
    echo -e "${GREEN}✅ 后端服务运行正常${NC}"
else
    echo -e "${RED}❌ 后端服务异常${NC}"
    exit 1
fi

# 检查前端服务
if curl -s http://localhost > /dev/null; then
    echo -e "${GREEN}✅ 前端服务运行正常${NC}"
else
    echo -e "${RED}❌ 前端服务异常${NC}"
    exit 1
fi

# 显示访问信息
echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}  工作流平台启动完成!${NC}"
echo -e "${GREEN}  模式: ${MODE}${NC}"
echo -e "${GREEN}  前端地址: http://localhost${NC}"
echo -e "${GREEN}  后端地址: http://localhost:8080/api${NC}"
echo -e "${GREEN}=======================================${NC}"

# 显示模式特定信息
if [[ "$MODE" == "offline" ]]; then
    echo -e "${YELLOW}离线模式说明:${NC}"
    echo "- 数据存储在本地文件中"
    echo "- 位置: ./data/ 目录"
    echo "- 支持导入/导出功能"
    echo "- 支持离线工作流设计"
else
    echo -e "${YELLOW}在线模式说明:${NC}"
    echo "- 数据存储在MySQL数据库"
    echo "- 支持实时协作"
    echo "- 支持用户管理和权限控制"
fi

echo -e "${GREEN}=======================================${NC}"