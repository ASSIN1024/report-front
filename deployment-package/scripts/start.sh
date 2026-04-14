#!/bin/bash

# =============================================================================
# 项目启动脚本 - 报表数据处理平台
# =============================================================================
# 功能: 前后端独立启动、停止、重启
# 使用: ./start.sh [backend|frontend|all|restart|stop|status]
# =============================================================================

set -e

# 配置
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BACKEND_DIR="$PROJECT_ROOT/report-backend"
FRONTEND_DIR="$PROJECT_ROOT/src"
PID_DIR="$PROJECT_ROOT/.pid"

BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"

BACKEND_PORT=8082
FRONTEND_PORT=8083

BACKEND_LOG="$PROJECT_ROOT/logs/backend.log"
FRONTEND_LOG="$PROJECT_ROOT/logs/frontend.log"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查端口是否被占用
check_port() {
    local port=$1
    if command -v lsof >/dev/null 2>&1; then
        lsof -i:$port >/dev/null 2>&1
    elif command -v netstat >/dev/null 2>&1; then
        netstat -tuln 2>/dev/null | grep -q ":$port "
    else
        ss -tuln 2>/dev/null | grep -q ":$port "
    fi
    return $?
}

# 获取端口对应的进程ID
get_pid_by_port() {
    local port=$1
    if command -v lsof >/dev/null 2>&1; then
        lsof -ti:$port 2>/dev/null | head -1
    else
        ss -tlnp 2>/dev/null | grep ":$port " | sed -n 's/.*pid=\([0-9]*\).*/\1/p' | head -1
    fi
}

# 等待端口释放
wait_for_port_release() {
    local port=$1
    local max_wait=10
    local count=0
    while check_port $port; do
        sleep 1
        count=$((count + 1))
        if [ $count -ge $max_wait ]; then
            log_error "端口 $port 等待释放超时"
            return 1
        fi
    done
    return 0
}

# 创建日志目录
ensure_log_dir() {
    if [ ! -d "$PROJECT_ROOT/logs" ]; then
        mkdir -p "$PROJECT_ROOT/logs"
    fi
}

# =============================================================================
# 后端操作
# =============================================================================

start_backend() {
    log_info "启动后端服务..."

    local existing_pid=$(get_pid_by_port $BACKEND_PORT)
    if [ -n "$existing_pid" ]; then
        log_warn "后端已在端口 $BACKEND_PORT 运行 (PID: $existing_pid)"
        return 0
    fi

    ensure_log_dir

    cd "$BACKEND_DIR"

    nohup mvn spring-boot:run -Dmaven.test.skip=true > "$BACKEND_LOG" 2>&1 &
    local new_pid=$!

    echo $new_pid > "$BACKEND_PID_FILE"

    sleep 3

    if check_port $BACKEND_PORT; then
        log_success "后端启动成功 (PID: $new_pid, 端口: $BACKEND_PORT)"
        log_info "日志文件: $BACKEND_LOG"
    else
        log_error "后端启动失败，请检查日志: $BACKEND_LOG"
        rm -f "$BACKEND_PID_FILE"
        return 1
    fi
}

stop_backend() {
    log_info "停止后端服务..."

    local pid=$(get_pid_by_port $BACKEND_PORT)
    if [ -z "$pid" ]; then
        pid=$(cat "$BACKEND_PID_FILE" 2>/dev/null || echo "")
    fi

    if [ -n "$pid" ] && kill -0 $pid 2>/dev/null; then
        kill $pid 2>/dev/null
        wait_for_port_release $BACKEND_PORT
        log_success "后端已停止 (PID: $pid)"
    else
        log_warn "后端未运行"
    fi

    rm -f "$BACKEND_PID_FILE"
}

restart_backend() {
    log_info "重启后端服务..."
    stop_backend
    sleep 2
    start_backend
}

status_backend() {
    if check_port $BACKEND_PORT; then
        local pid=$(get_pid_by_port $BACKEND_PORT)
        echo -e "${GREEN}● 后端运行中${NC} (PID: $pid, 端口: $BACKEND_PORT)"
        return 0
    else
        echo -e "${RED}○ 后端未运行${NC}"
        return 1
    fi
}

# =============================================================================
# 前端操作
# =============================================================================

start_frontend() {
    log_info "启动前端服务..."

    local existing_pid=$(get_pid_by_port $FRONTEND_PORT)
    if [ -n "$existing_pid" ]; then
        log_warn "前端已在端口 $FRONTEND_PORT 运行 (PID: $existing_pid)"
        return 0
    fi

    ensure_log_dir

    cd "$FRONTEND_DIR"

    nohup npm run serve > "$FRONTEND_LOG" 2>&1 &
    local new_pid=$!

    echo $new_pid > "$FRONTEND_PID_FILE"

    sleep 5

    if check_port $FRONTEND_PORT; then
        log_success "前端启动成功 (PID: $new_pid, 端口: $FRONTEND_PORT)"
        log_info "日志文件: $FRONTEND_LOG"
    else
        log_error "前端启动失败，请检查日志: $FRONTEND_LOG"
        rm -f "$FRONTEND_PID_FILE"
        return 1
    fi
}

stop_frontend() {
    log_info "停止前端服务..."

    local pid=$(get_pid_by_port $FRONTEND_PORT)
    if [ -z "$pid" ]; then
        pid=$(cat "$FRONTEND_PID_FILE" 2>/dev/null || echo "")
    fi

    if [ -n "$pid" ] && kill -0 $pid 2>/dev/null; then
        kill $pid 2>/dev/null
        wait_for_port_release $FRONTEND_PORT
        log_success "前端已停止 (PID: $pid)"
    else
        log_warn "前端未运行"
    fi

    rm -f "$FRONTEND_PID_FILE"
}

restart_frontend() {
    log_info "重启前端服务..."
    stop_frontend
    sleep 2
    start_frontend
}

status_frontend() {
    if check_port $FRONTEND_PORT; then
        local pid=$(get_pid_by_port $FRONTEND_PORT)
        echo -e "${GREEN}● 前端运行中${NC} (PID: $pid, 端口: $FRONTEND_PORT)"
        return 0
    else
        echo -e "${RED}○ 前端未运行${NC}"
        return 1
    fi
}

# =============================================================================
# 组合操作
# =============================================================================

start_all() {
    log_info "启动全部服务..."
    start_backend
    sleep 3
    start_frontend
}

stop_all() {
    log_info "停止全部服务..."
    stop_frontend
    stop_backend
}

restart_all() {
    log_info "重启全部服务..."
    stop_all
    sleep 3
    start_all
}

status_all() {
    echo "=========================================="
    echo "          服务状态"
    echo "=========================================="
    status_backend || true
    status_frontend || true
    echo "=========================================="
}

# =============================================================================
# 主入口
# =============================================================================

show_usage() {
    echo "报表数据处理平台 - 启动脚本"
    echo ""
    echo "用法: $0 [命令]"
    echo ""
    echo "命令:"
    echo "  backend    启动后端服务 (Spring Boot, 端口 $BACKEND_PORT)"
    echo "  frontend   启动前端服务 (Vue, 端口 $FRONTEND_PORT)"
    echo "  all        启动全部服务"
    echo "  restart    重启全部服务"
    echo "  stop       停止全部服务"
    echo "  status     查看服务状态"
    echo "  help       显示帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 backend      # 只启动后端"
    echo "  $0 frontend     # 只启动前端"
    echo "  $0 all          # 启动前后端"
    echo "  $0 restart      # 重启前后端"
    echo "  $0 status       # 查看状态"
}

main() {
    case "${1:-help}" in
        backend)
            start_backend
            ;;
        frontend)
            start_frontend
            ;;
        all)
            start_all
            ;;
        restart)
            restart_all
            ;;
        stop)
            stop_all
            ;;
        status)
            status_all
            ;;
        help|--help|-h)
            show_usage
            ;;
        *)
            log_error "未知命令: $1"
            echo ""
            show_usage
            exit 1
            ;;
    esac
}

main "$@"
