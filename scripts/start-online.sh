#!/bin/bash

echo "启动工作流平台（离线模式）"

# 设置环境变量
export APP_MODE=offline
export SPRING_PROFILES_ACTIVE=offline
export REACT_APP_MODE=offline

# 创建数据目录
mkdir -p ./data/offline/{workflows,nodes,rules,executions,exports,backups,logs}
mkdir -p ./logs

# 启动后端服务
echo "启动后端服务..."
cd backend
nohup java -jar target/workflow-platform.jar --app.mode=offline > ../logs/backend-offline.log 2>&1 &
BACKEND_PID=$!
cd ..

# 等待后端启动
echo "等待后端服务启动..."
sleep 10

# 检查后端是否启动
if curl -s http://localhost:8080/api/health > /dev/null; then
    echo "✅ 后端服务启动成功"
else
    echo "❌ 后端服务启动失败，请检查 logs/backend-offline.log"
    exit 1
fi

# 启动前端服务
echo "启动前端服务..."
cd frontend
npm run build:offline
nohup serve -s build -l 80 > ../logs/frontend-offline.log 2>&1 &
FRONTEND_PID=$!
cd ..

# 等待前端启动
sleep 5

# 检查前端是否启动
if curl -s http://localhost > /dev/null; then
    echo "✅ 前端服务启动成功"
else
    echo "❌ 前端服务启动失败，请检查 logs/frontend-offline.log"
    exit 1
fi

# 保存PID文件
echo $BACKEND_PID > ./backend-offline.pid
echo $FRONTEND_PID > ./frontend-offline.pid

echo ""
echo "========================================"
echo "工作流平台启动完成！"
echo "模式: 离线"
echo "前端地址: http://localhost"
echo "后端地址: http://localhost:8080/api"
echo "数据存储: 本地文件系统"
echo "数据目录: ./data/offline/"
echo ""
echo "使用以下命令停止服务:"
echo "./stop-offline.sh"
echo "========================================"

# 监控日志
tail -f logs/backend-offline.log logs/frontend-offline.log