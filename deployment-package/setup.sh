#!/bin/bash

# =============================================================================
# 报表数据处理平台 - 快速部署脚本
# =============================================================================
# 功能: 初始化环境并启动服务
# 使用: ./setup.sh [mysql|gaussdb]
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PACKAGE_ROOT="$(dirname "$SCRIPT_DIR")"
DB_TYPE="${1:-mysql}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        log_error "未找到命令: $1，请先安装"
        exit 1
    fi
}

check_java() {
    check_command java
    JAVA_VER=$(java -version 2>&1 | head -1)
    log_info "Java 版本: $JAVA_VER"
}

check_maven() {
    check_command mvn
    MVN_VER=$(mvn -version 2>&1 | head -1)
    log_info "Maven 版本: $MVN_VER"
}

check_node() {
    check_command node
    check_command npm
    NODE_VER=$(node -v)
    NPM_VER=$(npm -v)
    log_info "Node 版本: $NODE_VER, NPM 版本: $NPM_VER"
}

init_mysql() {
    log_info "初始化 MySQL 数据库..."

    MYSQL_HOST="${DB_HOST:-localhost}"
    MYSQL_PORT="${DB_PORT:-3306}"
    MYSQL_USER="${DB_USER:-root}"
    MYSQL_PASS="${DB_PASSWORD:-root123}"
    DB_NAME="${DB_NAME:-report_db}"

    log_info "连接信息: $MYSQL_USER@$MYSQL_HOST:$MYSQL_PORT"

    mysql -h"$MYSQL_HOST" -P"$MYSQL_PORT" -u"$MYSQL_USER" -p"$MYSQL_PASS" <<EOF
CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE \`$DB_NAME\`;
SOURCE $PACKAGE_ROOT/database/schema.sql;
SOURCE $PACKAGE_ROOT/database/quartz_tables_mysql.sql;
SOURCE $PACKAGE_ROOT/database/init-builtin-ftp.sql;
EOF

    log_success "MySQL 数据库初始化完成"
}

init_gaussdb() {
    log_info "初始化 GaussDB 数据库..."

    GAUSS_HOST="${GAUSSDB_HOST:-localhost}"
    GAUSS_PORT="${GAUSSDB_PORT:-5432}"
    GAUSS_USER="${GAUSSDB_USER:-report_user}"
    GAUSS_PASS="${GAUSSDB_PASSWORD:-password}"
    DB_NAME="${GAUSSDB_DB:-report_db}"

    log_info "连接信息: $GAUSS_USER@$GAUSS_HOST:$GAUSS_PORT"

    psql -h "$GAUSS_HOST" -p "$GAUSS_PORT" -U "$GAUSS_USER" -d postgres -c "CREATE DATABASE $DB_NAME;" 2>/dev/null || true

    PGPASSWORD="$GAUSS_PASS" psql -h "$GAUSS_HOST" -p "$GAUSS_PORT" -U "$GAUSS_USER" -d "$DB_NAME" -f "$PACKAGE_ROOT/database/schema-gaussdb.sql"
    PGPASSWORD="$GAUSS_PASS" psql -h "$GAUSS_HOST" -p "$GAUSS_PORT" -U "$GAUSS_USER" -d "$DB_NAME" -f "$PACKAGE_ROOT/database/quartz_tables_gaussdb.sql"

    log_success "GaussDB 数据库初始化完成"
}

setup_backend() {
    log_info "构建后端..."

    cd "$PACKAGE_ROOT/backend"
    mvn clean package -DskipTests -q

    if [ -f "target/report-backend-1.0.0.jar" ]; then
        log_success "后端构建完成"
    else
        log_error "后端构建失败"
        exit 1
    fi
}

setup_frontend() {
    log_info "构建前端..."

    cd "$PACKAGE_ROOT/frontend"
    npm install --silent
    npm run build --silent

    if [ -d "dist" ]; then
        log_success "前端构建完成"
    else
        log_error "前端构建失败"
        exit 1
    fi
}

main() {
    echo "=========================================="
    echo "  报表数据处理平台 - 快速部署"
    echo "=========================================="
    echo ""

    log_info "检查环境依赖..."

    if [ "$DB_TYPE" = "mysql" ]; then
        check_command mysql
        init_mysql
    elif [ "$DB_TYPE" = "gaussdb" ]; then
        check_command psql
        init_gaussdb
    else
        log_error "不支持的数据库类型: $DB_TYPE"
        echo "支持的类型: mysql, gaussdb"
        exit 1
    fi

    check_java
    check_maven
    check_node

    echo ""
    log_info "构建项目..."
    setup_backend
    setup_frontend

    echo ""
    echo "=========================================="
    echo "  部署完成!"
    echo "=========================================="
    echo ""
    echo "启动命令:"
    echo "  后端: cd $PACKAGE_ROOT/backend && java -jar target/report-backend-1.0.0.jar --spring.profiles.active=dev"
    echo "  前端: cd $PACKAGE_ROOT/frontend && npm run serve"
    echo ""
    echo "或使用: $PACKAGE_ROOT/scripts/start.sh"
    echo ""
}

main "$@"