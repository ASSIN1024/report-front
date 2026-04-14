#!/bin/bash

# 报表数据处理平台构建脚本
# 用法: ./build.sh [backend|frontend|all]

set -e

PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT/report-backend"
FRONTEND_DIR="$PROJECT_ROOT/report-front"
OUTPUT_DIR="$PROJECT_ROOT/dist"

echo "=========================================="
echo "  报表数据处理平台 - 构建脚本"
echo "=========================================="

# 创建输出目录
mkdir -p "$OUTPUT_DIR"

build_backend() {
    echo ""
    echo "[1/3] 构建后端 (Spring Boot)..."
    echo "----------------------------------------"

    cd "$BACKEND_DIR"

    if [ ! -f "pom.xml" ]; then
        echo "错误: 未找到 pom.xml"
        exit 1
    fi

    echo "清理并打包..."
    mvn clean package -DskipTests -q

    if [ -f "target/report-backend-1.0.0.jar" ]; then
        cp "target/report-backend-1.0.0.jar" "$OUTPUT_DIR/"
        echo "后端构建完成: $OUTPUT_DIR/report-backend-1.0.0.jar"
    else
        echo "错误: 打包失败"
        exit 1
    fi
}

build_frontend() {
    echo ""
    echo "[2/3] 构建前端 (Vue.js)..."
    echo "----------------------------------------"

    cd "$FRONTEND_DIR"

    if [ ! -f "package.json" ]; then
        echo "错误: 未找到 package.json"
        exit 1
    fi

    echo "安装依赖..."
    npm install --silent

    echo "打包..."
    npm run build --silent

    if [ -d "dist" ]; then
        cp -r dist/* "$OUTPUT_DIR/"
        echo "前端构建完成: $OUTPUT_DIR/index.html"
    else
        echo "错误: 打包失败"
        exit 1
    fi
}

copy_sql() {
    echo ""
    echo "[3/3] 复制数据库脚本..."
    echo "----------------------------------------"

    cp "$BACKEND_DIR/src/main/resources/schema.sql" "$OUTPUT_DIR/"
    echo "数据库脚本已复制: $OUTPUT_DIR/schema.sql"
}

show_info() {
    echo ""
    echo "=========================================="
    echo "  构建输出目录: $OUTPUT_DIR"
    echo "=========================================="
    echo ""
    echo "输出内容:"
    ls -la "$OUTPUT_DIR"
    echo ""
    echo "启动命令:"
    echo "  后端: java -jar $OUTPUT_DIR/report-backend-1.0.0.jar"
    echo "  前端: 请使用 nginx 或 http-server 托管 $OUTPUT_DIR"
    echo ""
}

case "$1" in
    backend)
        build_backend
        copy_sql
        ;;
    frontend)
        build_frontend
        ;;
    all|"")
        build_backend
        build_frontend
        copy_sql
        ;;
    *)
        echo "用法: $0 [backend|frontend|all]"
        echo "  backend  - 仅构建后端"
        echo "  frontend - 仅构建前端"
        echo "  all      - 构建全部 (默认)"
        exit 1
        ;;
esac

show_info
