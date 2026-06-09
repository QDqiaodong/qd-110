#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

if [ ! -f .env ]; then
    echo "❌ 错误: .env 文件不存在，请先创建 .env 配置文件"
    exit 1
fi

source .env

echo "============================================"
echo "  个人作息与习惯养成助手 - 启动检查"
echo "============================================"
echo ""

check_port_occupied() {
    local port=$1
    local service_name=$2
    
    if lsof -nP -iTCP:$port -sTCP:LISTEN >/dev/null 2>&1; then
        echo ""
        echo "❌ 端口冲突检测失败!"
        echo "   端口 $port 已被占用:"
        lsof -nP -iTCP:$port -sTCP:LISTEN
        echo ""
        echo "   请先停止占用该端口的进程，或修改 .env 中的 $service_name"
        exit 1
    fi
}

echo "🔍 正在检查端口可用性..."
check_port_occupied $FRONTEND_PORT "FRONTEND_PORT"
check_port_occupied $BACKEND_PORT "BACKEND_PORT"
check_port_occupied $MYSQL_PORT "MYSQL_PORT"
check_port_occupied $REDIS_PORT "REDIS_PORT"
echo "✅ 所有端口可用"
echo ""

echo "🚀 开始构建并启动服务..."
echo ""

docker compose up -d --build

echo ""
echo "⏳ 等待服务启动..."
sleep 5

MAX_RETRIES=30
RETRY_COUNT=0
FRONTEND_READY=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -sSf http://127.0.0.1:${FRONTEND_PORT} >/dev/null 2>&1; then
        FRONTEND_READY=1
        break
    fi
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
done

echo ""
echo ""

if [ $FRONTEND_READY -eq 1 ]; then
    echo "============================================"
    echo "  ✅ 项目启动成功!"
    echo "============================================"
    echo ""
    echo "🌐 前端访问地址:"
    echo "   http://localhost:${FRONTEND_PORT}"
    echo "   http://127.0.0.1:${FRONTEND_PORT}"
    echo ""
    echo "🔧 后端API地址:"
    echo "   http://127.0.0.1:${BACKEND_PORT}/api"
    echo ""
    echo "💾 MySQL 数据库:"
    echo "   Host: 127.0.0.1"
    echo "   Port: ${MYSQL_PORT}"
    echo "   User: root"
    echo "   Pass: ${MYSQL_ROOT_PASSWORD}"
    echo "   DB:   ${MYSQL_DATABASE}"
    echo ""
    echo "⚡ Redis 缓存:"
    echo "   Host: 127.0.0.1"
    echo "   Port: ${REDIS_PORT}"
    echo ""
    echo "📦 运行中的容器:"
    docker compose ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" | head -5
    echo ""
    echo "📝 查看日志: docker compose logs -f"
    echo "⏹️  停止服务: docker compose down"
    echo "============================================"
else
    echo "⚠️  警告: 前端服务启动超时，请检查日志:"
    echo "   docker compose logs -f frontend"
    exit 1
fi
