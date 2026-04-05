#!/bin/bash
# 文档同步脚本
# 用途：自动检测代码变更并同步到相关文档

set -e

PROJECT_DIR="/home/nova/projects/report-front"
cd "$PROJECT_DIR"

echo "=== 文档同步脚本 ==="
echo "项目目录: $PROJECT_DIR"
echo ""

# 检查是否有未提交的变更
if [ -n "$(git status --porcelain)" ]; then
    echo "[INFO] 检测到未提交的变更"

    # 获取变更的文件列表
    CHANGED_FILES=$(git diff --name-only HEAD)

    echo "[INFO] 变更文件:"
    echo "$CHANGED_FILES"
    echo ""

    # 检测API变更
    if echo "$CHANGED_FILES" | grep -q "controller.*Controller.java"; then
        echo "[WARN] 检测到 Controller 变更，请手动更新 API.md"
        echo "       新增接口需要添加到 API.md 的对应章节"
    fi

    # 检测前端页面变更
    if echo "$CHANGED_FILES" | grep -q "src/views/"; then
        echo "[WARN] 检测到前端页面变更，请确认是否需要更新文档"
    fi

    # 检测后端实体变更
    if echo "$CHANGED_FILES" | grep -q "entity/"; then
        echo "[WARN] 检测到实体变更，请确认是否需要更新 schema.sql"
    fi

    # 检测测试文件变更
    if echo "$CHANGED_FILES" | grep -q "src/test/"; then
        echo "[INFO] 检测到测试文件变更"
    fi

    echo ""
    echo "[INFO] 请确保已完成以下同步:"
    echo "       - tasks.json 任务状态更新"
    echo "       - progress-notes.md 会话记录"
    echo "       - API.md 接口文档（如有新增API）"
    echo "       - docs/superpowers/specs/ 设计文档"
    echo "       - docs/superpowers/plans/ 实施计划"

else
    echo "[INFO] 没有检测到未提交的变更"
fi

echo ""
echo "=== 文档同步检查完成 ==="
