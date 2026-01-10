#!/bin/bash

echo "停止所有工作流平台服务..."

# 停止前端服务
if [ -f frontend.pid ]; then
    FRONTEND_PID=$(cat frontend.pid)
    if kill -0 $FRONTEND_PID 2>/dev/null; then
        kill $FRONTEND_PID
        echo "停止前端服务 (PID: $FRONTEND_PID)"
    fi
    rm -f frontend.pid
fi

if [ -f frontend-offline.pid ]; then
    FRONTEND_OFFLINE_PID=$(cat frontend-offline.pid)
    if kill -0 $FRONTEND_OFFLINE_PID 2>/dev/null; then
        kill $FRONTEND_OFFLINE_PID
        echo "停止离线前端服务 (PID: $FRONTEND_OFFLINE_PID)"
    fi
    rm -f frontend-offline.pid
fi

# 停止后端服务
if [ -f backend.pid ]; then
    BACKEND_PID=$(cat backend.pid)
    if kill -0 $BACKEND_PID 2>/dev/null; then
        kill $BACKEND_PID
        echo "停止后端服务 (PID: $BACKEND_PID)"
    fi
    rm -f backend.pid
fi

if [ -f backend-offline.pid ]; then
    BACKEND_OFFLINE_PID=$(cat backend-offline.pid)
    if kill -0 $BACKEND_OFFLINE_PID 2>/dev/null; then
        kill $BACKEND_OFFLINE_PID
        echo "停止离线后端服务 (PID: $BACKEND_OFFLINE_PID)"
    fi
    rm -f backend-offline.pid
fi

# 清理临时文件
rm -f *.pid

echo "所有服务已停止"