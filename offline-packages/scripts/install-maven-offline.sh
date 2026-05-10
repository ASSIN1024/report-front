#!/bin/bash
# Maven离线安装脚本
# 使用方法: ./install-maven-offline.sh
# 无需联网，自动配置本地仓库

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OFFLINE_DIR="$(dirname "$SCRIPT_DIR")"
REPO_DIR="$OFFLINE_DIR/maven-repository"
M2_HOME="${HOME}/.m2"

echo "=========================================="
echo "Maven离线安装配置工具"
echo "=========================================="

# 1. 创建Maven配置目录
echo "[1/4] 创建Maven配置目录..."
mkdir -p "$M2_HOME"

# 2. 复制settings.xml
echo "[2/4] 配置Maven settings.xml..."
if [ -f "$OFFLINE_DIR/repositories/maven-settings.xml" ]; then
    cp "$OFFLINE_DIR/repositories/maven-settings.xml" "$M2_HOME/settings.xml"

    # 替换占位符
    sed -i "s|\${user.home}|$HOME|g" "$M2_HOME/settings.xml"
    sed -i "s|{user.home}|$HOME|g" "$M2_HOME/settings.xml"

    echo "  settings.xml 已配置"
else
    echo "  警告: 未找到settings.xml模板"
fi

# 3. 创建本地仓库软链接或复制
echo "[3/4] 配置本地仓库..."
if [ -d "$REPO_DIR" ]; then
    # 方案A: 创建软链接 (节省空间)
    if [ ! -L "$M2_HOME/repository" ] && [ ! -d "$M2_HOME/repository" ]; then
        ln -s "$REPO_DIR" "$M2_HOME/repository"
        echo "  已创建软链接: $M2_HOME/repository -> $REPO_DIR"
    else
        echo "  仓库已存在，跳过"
    fi
else
    echo "  警告: 未找到离线仓库目录: $REPO_DIR"
fi

# 4. 验证配置
echo "[4/4] 验证配置..."
if command -v mvn &> /dev/null; then
    echo "  Maven版本: $(mvn -version | head -1)"
    echo "  本地仓库: $(mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout 2>/dev/null || echo '无法获取')"
else
    echo "  警告: 系统未安装Maven，请手动安装"
fi

echo ""
echo "=========================================="
echo "Maven离线配置完成!"
echo "=========================================="
echo "本地仓库: $M2_HOME/repository"
echo "配置文件: $M2_HOME/settings.xml"
echo ""
echo "验证命令:"
echo "  mvn -v"
echo "  mvn dependency:resolve"
echo "=========================================="
