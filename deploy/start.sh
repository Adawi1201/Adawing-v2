#!/usr/bin/env bash
# =============================================================================
# AdaWing v2 — 一键启动
# 用法: bash deploy/start.sh           # 启动所有服务
#       bash deploy/start.sh backend   # 仅启动后端
#       bash deploy/start.sh frontend  # 仅启动前端
#       bash deploy/start.sh stop      # 停止所有服务
#       bash deploy/start.sh status    # 查看服务状态
# =============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DIST="$ROOT/deploy/dist"
PID_DIR="$ROOT/deploy/.pids"
mkdir -p "$PID_DIR"

# 加载 .env
if [ -f "$ROOT/deploy/.env" ]; then
    source "$ROOT/deploy/.env"
fi

SERVER_PORT="${SERVER_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-80}"

# ---- 停止服务 ----
stop_all() {
    echo "停止所有服务..."
    for pidfile in "$PID_DIR"/*.pid; do
        if [ -f "$pidfile" ]; then
            PID=$(cat "$pidfile")
            NAME=$(basename "$pidfile" .pid)
            if kill -0 "$PID" 2>/dev/null; then
                kill "$PID" 2>/dev/null && echo "  ✓ 已停止 $NAME (pid=$PID)"
            fi
            rm -f "$pidfile"
        fi
    done
    echo "完成"
}

# ---- 查看状态 ----
show_status() {
    RUNNING=0
    for pidfile in "$PID_DIR"/*.pid; do
        if [ -f "$pidfile" ]; then
            PID=$(cat "$pidfile")
            NAME=$(basename "$pidfile" .pid)
            if kill -0 "$PID" 2>/dev/null; then
                echo "  ✓ $NAME 运行中 (pid=$PID)"
                RUNNING=$((RUNNING + 1))
            else
                echo "  ✗ $NAME 已停止 (stale pid)"
                rm -f "$pidfile"
            fi
        fi
    done
    if [ "$RUNNING" -eq 0 ]; then
        echo "  没有运行中的服务"
    fi
}

# ---- 后端 ----
start_backend() {
    JAR="$DIST/adawing-backend.jar"
    if [ ! -f "$JAR" ]; then
        echo "错误: $JAR 不存在，请先运行 bash deploy/package.sh"
        exit 1
    fi

    # 检查端口
    if ss -tlnp 2>/dev/null | grep -q ":$SERVER_PORT " || netstat -tlnp 2>/dev/null | grep -q ":$SERVER_PORT "; then
        echo "  ! 端口 $SERVER_PORT 已被占用，跳过"
        return 1
    fi

    echo "启动后端 (port=$SERVER_PORT)..."
    nohup java -jar "$JAR" \
        --server.port="$SERVER_PORT" \
        --spring.profiles.active=local \
        > "$ROOT/deploy/.logs/backend.log" 2>&1 &
    echo $! > "$PID_DIR/backend.pid"

    # 等待启动
    echo -n "  等待启动"
    for i in $(seq 1 30); do
        if curl -s "http://localhost:$SERVER_PORT/api/v2/system/config/dashboard" > /dev/null 2>&1; then
            echo " ✓ (pid=$(cat $PID_DIR/backend.pid))"
            return 0
        fi
        echo -n "."
        sleep 1
    done
    echo ""
    echo "  ! 启动超时，查看日志: tail -f deploy/.logs/backend.log"
}

# ---- 前端 ----
start_frontend() {
    if [ ! -d "$DIST/frontend" ]; then
        echo "错误: $DIST/frontend 不存在，请先运行 bash deploy/package.sh"
        exit 1
    fi

    # 使用 Python 简单 HTTP Server 或 npx serve
    if command -v npx &>/dev/null; then
        echo "启动前端 (port=$FRONTEND_PORT)..."
        nohup npx --yes serve "$DIST/frontend" \
            -l "$FRONTEND_PORT" \
            --no-clipboard \
            > "$ROOT/deploy/.logs/frontend.log" 2>&1 &
        echo $! > "$PID_DIR/frontend.pid"
        echo "  ✓ 前端已启动 (pid=$(cat $PID_DIR/frontend.pid))"
        echo "  → http://localhost:$FRONTEND_PORT"
    else
        echo "  ! 未找到 npx，跳过前端启动"
        echo "  你可以手动用 nginx 指向: $DIST/frontend"
        return 1
    fi
}

# ---- Main ----
mkdir -p "$ROOT/deploy/.logs"

case "${1:-all}" in
    stop)
        stop_all
        exit 0
        ;;
    status)
        show_status
        exit 0
        ;;
    backend)
        start_backend
        exit 0
        ;;
    frontend)
        start_frontend
        exit 0
        ;;
    all)
        echo "========================================="
        echo " AdaWing v2 启动"
        echo "========================================="
        echo ""

        # 检查是否已在运行
        if [ -f "$PID_DIR/backend.pid" ] && kill -0 "$(cat "$PID_DIR/backend.pid")" 2>/dev/null; then
            echo "后端已在运行中 (pid=$(cat $PID_DIR/backend.pid))"
        else
            start_backend
        fi

        echo ""

        if [ -f "$PID_DIR/frontend.pid" ] && kill -0 "$(cat "$PID_DIR/frontend.pid")" 2>/dev/null; then
            echo "前端已在运行中 (pid=$(cat $PID_DIR/frontend.pid))"
        else
            start_frontend
        fi

        echo ""
        echo "========================================="
        echo " 服务已启动 ✓"
        echo "========================================="
        echo ""
        echo "  博客首页:    http://localhost:$FRONTEND_PORT"
        echo "  管理后台:    http://localhost:$FRONTEND_PORT/v2/ren/admin/login"
        echo "  MCP 端点:    http://localhost:$SERVER_PORT/mcp"
        echo ""
        echo "  管理命令:"
        echo "    bash deploy/start.sh status   # 查看状态"
        echo "    bash deploy/start.sh stop     # 停止服务"
        echo "    tail -f deploy/.logs/backend.log   # 查看后端日志"
        ;;
    *)
        echo "用法: bash deploy/start.sh [all|backend|frontend|stop|status]"
        exit 1
        ;;
esac
