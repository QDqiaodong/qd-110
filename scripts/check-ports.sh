#!/bin/bash

check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 1
    else
        return 0
    fi
}

find_available_port() {
    local base_port=$1
    local port=$base_port
    while ! check_port $port; do
        echo "端口 $port 已被占用，尝试 $((port + 1))..."
        port=$((port + 1))
    done
    echo $port
}

echo "检测端口可用性..."

FRONTEND_PORT=$(find_available_port 8033)
BACKEND_PORT=$(find_available_port 9033)
MYSQL_PORT=$(find_available_port 3333)
REDIS_PORT=$(find_available_port 6433)

echo "可用端口配置："
echo "  前端: $FRONTEND_PORT"
echo "  后端: $BACKEND_PORT"
echo "  MySQL: $MYSQL_PORT"
echo "  Redis: $REDIS_PORT"

export FRONTEND_PORT
export BACKEND_PORT
export MYSQL_PORT
export REDIS_PORT
