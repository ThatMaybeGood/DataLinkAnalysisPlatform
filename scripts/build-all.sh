#!/bin/bash

# 工作流平台完整构建脚本
# 使用方式: ./build-all.sh [online|offline|all]

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认构建所有
MODE=${1:-"all"}

echo -e "${BLUE}=======================================${NC}"
echo -e "${BLUE}  工作流平台构建脚本${NC}"
echo -e "${BLUE}  构建模式: ${MODE}${NC}"
echo -e "${BLUE}=======================================${NC}"

# 检查参数
if [[ "$MODE" != "all" && "$MODE" != "online" && "$MODE" != "offline" ]]; then
    echo -e "${RED}错误: 模式必须是 'all', 'online' 或 'offline'${NC}"
    exit 1
fi

# 构建前端
echo -e "${YELLOW}构建前端应用...${NC}"
cd frontend

if [[ "$MODE" == "all" || "$MODE" == "online" ]]; then
    echo -e "${BLUE}构建在线版本...${NC}"
    npm run build:online
    echo -e "${GREEN}在线版本构建完成${NC}"
fi

if [[ "$MODE" == "all" || "$MODE" == "offline" ]]; then
    echo -e "${BLUE}构建离线版本...${NC}"
    npm run build:offline
    echo -e "${GREEN}离线版本构建完成${NC}"
fi

cd ..

# 构建后端
echo -e "${YELLOW}构建后端应用...${NC}"
cd backend

if [[ "$MODE" == "all" || "$MODE" == "online" ]]; then
    echo -e "${BLUE}构建在线版本...${NC}"
    mvn clean package -Ponline -DskipTests
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}在线版本构建成功${NC}"
        cp target/workflow-platform-online.jar ../dist/workflow-platform-online.jar
    else
        echo -e "${RED}在线版本构建失败${NC}"
        exit 1
    fi
fi

if [[ "$MODE" == "all" || "$MODE" == "offline" ]]; then
    echo -e "${BLUE}构建离线版本...${NC}"
    mvn clean package -Poffline -DskipTests
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}离线版本构建成功${NC}"
        cp target/workflow-platform-offline.jar ../dist/workflow-platform-offline.jar
    else
        echo -e "${RED}离线版本构建失败${NC}"
        exit 1
    fi
fi

cd ..

# 创建Docker镜像
echo -e "${YELLOW}创建Docker镜像...${NC}"

if [[ "$MODE" == "all" || "$MODE" == "online" ]]; then
    echo -e "${BLUE}构建在线Docker镜像...${NC}"
    docker build -t workflow-platform-online:latest -f backend/Dockerfile .
    echo -e "${GREEN}在线Docker镜像构建完成${NC}"
fi

if [[ "$MODE" == "all" || "$MODE" == "offline" ]]; then
    echo -e "${BLUE}构建离线Docker镜像...${NC}"
    docker build -t workflow-platform-offline:latest -f backend/Dockerfile.offline .
    echo -e "${GREEN}离线Docker镜像构建完成${NC}"
fi

# 创建发布包
echo -e "${YELLOW}创建发布包...${NC}"
mkdir -p dist
cp -r frontend/build dist/frontend
cp -r backend/target/*.jar dist/
cp scripts/*.sh dist/
cp README.md dist/
cp LICENSE dist/

# 创建压缩包
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
if [[ "$MODE" == "all" ]]; then
    tar -czf workflow-platform-full-$TIMESTAMP.tar.gz dist/
    echo -e "${GREEN}完整发布包: workflow-platform-full-$TIMESTAMP.tar.gz${NC}"
elif [[ "$MODE" == "online" ]]; then
    tar -czf workflow-platform-online-$TIMESTAMP.tar.gz dist/workflow-platform-online.jar dist/frontend/ scripts/start-online.sh
    echo -e "${GREEN}在线发布包: workflow-platform-online-$TIMESTAMP.tar.gz${NC}"
elif [[ "$MODE" == "offline" ]]; then
    tar -czf workflow-platform-offline-$TIMESTAMP.tar.gz dist/workflow-platform-offline.jar dist/frontend/ scripts/start-offline.sh
    echo -e "${GREEN}离线发布包: workflow-platform-offline-$TIMESTAMP.tar.gz${NC}"
fi

echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}  构建完成!${NC}"
echo -e "${GREEN}=======================================${NC}"

# 显示构建结果
echo -e "${YELLOW}构建结果:${NC}"
ls -la *.tar.gz 2>/dev/null || echo "没有生成压缩包"
echo ""

# Docker镜像列表
echo -e "${YELLOW}Docker镜像:${NC}"
docker images | grep workflow-platform