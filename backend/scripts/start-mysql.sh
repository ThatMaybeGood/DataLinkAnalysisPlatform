#!/usr/bin/env bash
# ============================================================
# 启动项目本地 MySQL（离线本地模式，数据目录在 backend/.data/mysql）
# 用法：./backend/scripts/start-mysql.sh
# 如 mysqld 不在 PATH，可用环境变量指定：MYSQLD=/path/to/mysqld ./start-mysql.sh
# ============================================================
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATADIR="$ROOT/.data/mysql"
LOG="$DATADIR/error.log"
MYSQLD="${MYSQLD:-$(command -v mysqld || true)}"

if [ -z "$MYSQLD" ]; then
  echo "错误：未找到 mysqld。请安装 MySQL，或设置环境变量 MYSQLD 指向 mysqld 二进制。" >&2
  exit 1
fi

if mysqladmin -uroot ping >/dev/null 2>&1; then
  echo "MySQL 已在运行。"
  exit 0
fi

if [ ! -d "$DATADIR/mysql" ]; then
  echo "首次运行：初始化数据目录 $DATADIR ..."
  mkdir -p "$DATADIR"
  BASEDIR="$(cd "$(dirname "$MYSQLD")/.." && pwd)"
  "$MYSQLD" --initialize-insecure --basedir="$BASEDIR" --datadir="$DATADIR" || {
    echo "初始化失败，请查看 $LOG" >&2
    exit 1
  }
fi

echo "启动 MySQL（端口 3306，socket /tmp/mysql.sock）..."
nohup "$MYSQLD" --basedir="$(cd "$(dirname "$MYSQLD")/.." && pwd)" \
  --datadir="$DATADIR" --port=3306 --socket=/tmp/mysql.sock \
  --pid-file=/tmp/datalink-mysql.pid --log-error="$LOG" >/dev/null 2>&1 &

for _ in $(seq 1 15); do
  sleep 1
  if mysqladmin -uroot ping >/dev/null 2>&1; then
    mysql -uroot <<'SQL'
CREATE DATABASE IF NOT EXISTS datalink DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'datalink'@'localhost' IDENTIFIED BY 'datalink123';
CREATE USER IF NOT EXISTS 'datalink'@'127.0.0.1' IDENTIFIED BY 'datalink123';
GRANT ALL PRIVILEGES ON datalink.* TO 'datalink'@'localhost';
GRANT ALL PRIVILEGES ON datalink.* TO 'datalink'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
    echo "MySQL 已就绪，数据库 datalink 与账号 datalink/datalink123 已就绪。"
    exit 0
  fi
done

echo "MySQL 启动失败，请查看 $LOG" >&2
exit 1
