#!/bin/bash
# ============================================================
# AdaWing v2 — 服务管理脚本
# 用法: bash run.sh {start|stop|restart|status|logs}
# ============================================================

APP_HOME="$(cd "$(dirname "$0")" && pwd)"
JAR="$APP_HOME/adawing-backend.jar"
CFG="$APP_HOME/application-prod.yaml"
ENV_FILE="$APP_HOME/.env"
PID_FILE="$APP_HOME/.pid"
LOG_DIR="$APP_HOME/logs"

# Load optional environment overrides
if [[ -f "$ENV_FILE" ]]; then
    set -a
    source "$ENV_FILE"
    set +a
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

mkdir -p "$LOG_DIR"

get_pid() {
    if [[ -f "$PID_FILE" ]]; then
        local pid; pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then echo "$pid"; return 0; fi
    fi
    pgrep -f "adawing-backend.jar" 2>/dev/null | head -1
}

do_start() {
    if pid=$(get_pid) && [[ -n "$pid" ]]; then
        echo -e "${YELLOW}Already running (pid=$pid)${NC}"
        do_status
        return 0
    fi

    if [[ ! -f "$JAR" ]]; then
        echo -e "${RED}[FAIL]${NC} jar not found: $JAR"
        exit 1
    fi
    if [[ ! -f "$CFG" ]]; then
        echo -e "${RED}[FAIL]${NC} config not found: $CFG"
        exit 1
    fi

    echo -n "Starting AdaWing..."
    nohup java -jar "$JAR" \
        --spring.config.name=application-prod \
        --spring.config.location=/opt/adawing/ \
        >> "$LOG_DIR/app.log" 2>&1 &
    local pid=$!
    echo "$pid" > "$PID_FILE"
    echo " (pid=$pid)"

    local elapsed=0
    while [[ $elapsed -lt 60 ]]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            echo ""
            echo -e "${RED}[FAIL]${NC} Process died. Startup failed."
            echo ""
            echo "  Last 20 lines of log:"
            tail -20 "$LOG_DIR/app.log" 2>/dev/null
            echo ""
            rm -f "$PID_FILE"
            exit 1
        fi

        if curl -sf "http://localhost:8080/api/v2/system/config/dashboard" >/dev/null 2>&1; then
            echo ""
            echo -e "${GREEN}[ OK ]${NC} AdaWing started (pid=$pid)"
            echo ""
            echo "  Public:   http://$(hostname -I 2>/dev/null | awk '{print $1}' || echo 'localhost')"
            echo "  Admin:    http://localhost:8080/yusal/admin/login"
            echo "  MCP:      http://localhost:8080/mcp"
            echo "  Logs:     tail -f $LOG_DIR/app.log"
            return 0
        fi

        sleep 1
        elapsed=$((elapsed + 1))
        echo -n "."
    done

    echo ""
    echo -e "${YELLOW}[WARN]${NC} Timeout (60s). Process alive but health check failed."
    echo "  Check: tail -f $LOG_DIR/app.log"
    return 1
}

do_stop() {
    local pid; pid=$(get_pid)
    if [[ -z "$pid" ]]; then
        echo "AdaWing is not running"
        rm -f "$PID_FILE"
        return 0
    fi

    echo -n "Stopping AdaWing (pid=$pid)..."
    kill "$pid" 2>/dev/null

    local waited=0
    while [[ $waited -lt 15 ]]; do
        if ! kill -0 "$pid" 2>/dev/null; then
            echo -e " ${GREEN}stopped${NC}"
            rm -f "$PID_FILE"
            return 0
        fi
        sleep 1; waited=$((waited + 1))
    done

    echo -e " ${RED}timeout, force kill${NC}"
    kill -9 "$pid" 2>/dev/null || true
    rm -f "$PID_FILE"
}

do_restart() {
    do_stop
    sleep 1
    do_start
}

do_status() {
    local pid; pid=$(get_pid)
    if [[ -z "$pid" ]]; then
        echo "AdaWing: NOT RUNNING"
        return 1
    fi

    local uptime; uptime=$(ps -o etime= -p "$pid" 2>/dev/null | tr -d ' ')
    local mem; mem=$(ps -o rss= -p "$pid" 2>/dev/null | awk '{printf "%.0f MB", $1/1024}')

    echo "AdaWing: RUNNING"
    echo "  PID:    $pid"
    [[ -n "$uptime" ]] && echo "  Uptime: $uptime"
    [[ -n "$mem" ]]    && echo "  Memory: $mem"

    if curl -sf "http://localhost:8080/api/v2/system/config/dashboard" >/dev/null 2>&1; then
        echo -e "  Health: ${GREEN}OK${NC}"
    else
        echo -e "  Health: ${RED}FAIL${NC}"
    fi
}

do_logs() {
    if [[ -f "$LOG_DIR/app.log" ]]; then
        echo "Tailing $LOG_DIR/app.log (Ctrl+C to exit)..."
        tail -f "$LOG_DIR/app.log"
    else
        echo "Log not found: $LOG_DIR/app.log"
    fi
}

case "${1:-start}" in
    start)   do_start ;;
    stop)    do_stop ;;
    restart) do_restart ;;
    status)  do_status ;;
    logs)    do_logs ;;
    *)
        echo "Usage: bash run.sh {start|stop|restart|status|logs}"
        exit 1
        ;;
esac
