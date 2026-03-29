#!/bin/bash

# 报表数据处理平台启动脚本

JAR_FILE="$(dirname "$0")/report-backend-1.0.0.jar"

if [ ! -f "$JAR_FILE" ]; then
    echo "错误: 未找到 $JAR_FILE"
    echo "请先运行 build.sh 构建项目"
    exit 1
fi

echo "=========================================="
echo "  报表数据处理平台 - 启动中"
echo "=========================================="
echo ""
echo "后端API: http://localhost:8080"
echo "前端页面: 请确保前端服务已启动"
echo ""

java -jar "$JAR_FILE" --spring.profiles.active=dev
