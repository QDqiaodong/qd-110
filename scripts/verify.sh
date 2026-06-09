#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

if [ -f .env ]; then
    source .env
else
    FRONTEND_PORT=3010
    BACKEND_PORT=8090
    MYSQL_PORT=3313
    REDIS_PORT=6382
fi

echo "============================================"
echo "  项目端口自检脚本"
echo "============================================"
echo ""

check_port() {
    local port=$1
    local name=$2
    
    echo "🔍 检查 $name (端口 $port)..."
    
    if lsof -nP -iTCP:$port -sTCP:LISTEN >/dev/null 2>&1; then
        echo "   ✅ 端口 $port 已被监听"
        echo "   占用进程:"
        lsof -nP -iTCP:$port -sTCP:LISTEN | tail -n +2 | while read line; do
            echo "   - $line"
        done
        return 0
    else
        echo "   ❌ 端口 $port 未被监听"
        return 1
    fi
}

check_http() {
    local url=$1
    local name=$2
    
    echo "🔍 检查 $name ($url)..."
    
    if curl -sSf "$url" >/dev/null 2>&1; then
        local title=$(curl -sS "$url" | grep -o '<title>[^<]*</title>' | head -1 | sed 's/<[^>]*>//g')
        echo "   ✅ HTTP 响应正常"
        echo "   页面标题: $title"
        return 0
    else
        echo "   ❌ HTTP 请求失败"
        return 1
    fi
}

echo ""
echo "📋 端口占用检查:"
echo "--------------------------------------------"
check_port $FRONTEND_PORT "前端服务"
check_port $BACKEND_PORT "后端服务"
check_port $MYSQL_PORT "MySQL"
check_port $REDIS_PORT "Redis"

echo ""
echo "🌐 HTTP 服务检查:"
echo "--------------------------------------------"

echo ""
echo "前端 127.0.0.1 访问:"
HTTP1=$(check_http "http://127.0.0.1:${FRONTEND_PORT}" "前端-127.0.0.1") || true

echo ""
echo "前端 localhost 访问:"
HTTP2=$(check_http "http://localhost:${FRONTEND_PORT}" "前端-localhost") || true

echo ""
echo "--------------------------------------------"
echo "🔍 一致性验证:"

TITLE1=$(curl -sS "http://127.0.0.1:${FRONTEND_PORT}" 2>/dev/null | grep -o '<title>[^<]*</title>' | head -1 | sed 's/<[^>]*>//g')
TITLE2=$(curl -sS "http://localhost:${FRONTEND_PORT}" 2>/dev/null | grep -o '<title>[^<]*</title>' | head -1 | sed 's/<[^>]*>//g')

if [ "$TITLE1" = "$TITLE2" ] && [ -n "$TITLE1" ]; then
    echo "   ✅ localhost 和 127.0.0.1 访问一致"
    echo "   页面标题: $TITLE1"
else
    echo "   ❌ 访问不一致!"
    echo "   127.0.0.1 标题: $TITLE1"
    echo "   localhost 标题: $TITLE2"
fi

echo ""
echo "============================================"
