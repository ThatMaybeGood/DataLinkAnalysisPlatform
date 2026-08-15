#!/usr/bin/env bash
# ============================================================
# 停止项目本地 MySQL
# 用法：./backend/scripts/stop-mysql.sh
# ============================================================
set -euo pipefail

if [ -f /tmp/datalink-mysql.pid ]; then
  kill "$(cat /tmp/datalink-mysql.pid)" 2>/dev/null || true
  rm -f /tmp/datalink-mysql.pid
  echo "MySQL 已停止。"
else
  if mysqladmin -uroot shutdown >/dev/null 2>&1; then
    echo "MySQL 已停止。"
  else
    echo "MySQL 未在运行。"
  fi
fi
