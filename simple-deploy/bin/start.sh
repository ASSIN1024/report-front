#!/bin/bash
# 报表数据处理平台 - Linux启动脚本
# 用法: ./start.sh [start|stop|restart|status|logs]

BIN_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_DIR="$BIN_DIR/../logs"
PID_FILE="$BIN_DIR/app.pid"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

check_java() {
    if ! command -v java &> /dev/null; then
        log_error "未找到Java，请先安装JDK 1.8+"
        exit 1
    fi
    log_info "Java版本: $(java -version 2>&1 | head -1)"
}

get_pid() {
    [ -f "$PID_FILE" ] && cat "$PID_FILE" 2>/dev/null
}

is_running() {
    local pid=$(get_pid)
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null
}

do_start() {
    log_info "启动报表数据处理平台..."

    if is_running; then
        log_warn "服务已在运行 (PID: $(get_pid))"
        return
    fi

    check_java
    mkdir -p "$LOG_DIR"

    cd "$BIN_DIR" || exit 1
    log_info "启动服务..."

    nohup java -Xms2g -Xmx4g -XX:+UseG1GC -Dfile.encoding=UTF-8 -jar "$BIN_DIR/report-backend.jar" > "$LOG_DIR/stdout.log" 2>&1 &
    local new_pid=$!

    sleep 3

    if kill -0 "$new_pid" 2>/dev/null; then
        echo "$new_pid" > "$PID_FILE"
        log_info "服务启动成功 (PID: $new_pid)"
        log_info "查看日志: ./start.sh logs"
    else
        log_error "启动失败，请查看日志"
    fi
}

do_stop() {
    local pid=$(get_pid)
    if [ -n "$pid" ]; then
        log_info "停止服务 (PID: $pid)..."
        kill "$pid" 2>/dev/null || true
        sleep 2
        rm -f "$PID_FILE"
    fi
    log_info "服务已停止"
}

check_status() {
    if [ -f "$LOG_DIR/stdout.log" ]; then
        local port=$(grep -oP 'Tomcat started on port \K\d+' "$LOG_DIR/stdout.log" 2>/dev/null || echo "")
        if [ -n "$port" ]; then
            log_info "访问地址: http://localhost:$port"
            log_info "Druid监控: http://localhost:$port/druid/"
        fi
    fi
}

do_status() {
    if is_running; then
        echo -e "${GREEN}服务运行中 (PID: $(get_pid))${NC}"
        check_status
    else
        echo -e "${YELLOW}服务未运行${NC}"
    fi
}

do_logs() {
    [ -f "$LOG_DIR/stdout.log" ] && tail -50 "$LOG_DIR/stdout.log" || log_warn "日志文件不存在"
}

case "${1:-start}" in
    start)   do_start ;;
    stop)    do_stop ;;
    restart) do_stop; sleep 1; do_start ;;
    status)  do_status ;;
    logs)    do_logs ;;
    *)       echo "用法: $0 {start|stop|restart|status|logs}" ;;
esac
