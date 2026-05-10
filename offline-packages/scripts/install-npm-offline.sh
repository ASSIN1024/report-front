#!/bin/bash
# npm离线安装脚本
# 使用方法: ./install-npm-offline.sh
# 无需联网，自动配置本地仓库

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OFFLINE_DIR="$(dirname "$SCRIPT_DIR")"
NPM_OFFLINE_DIR="$OFFLINE_DIR/npm-offline"

echo "=========================================="
echo "npm离线安装配置工具"
echo "=========================================="

# 1. 检查Node.js和npm
echo "[1/4] 检查Node.js和npm..."
if ! command -v node &> /dev/null; then
    echo "  错误: 未找到Node.js，请先安装"
    exit 1
fi

if ! command -v npm &> /dev/null; then
    echo "  错误: 未找到npm，请先安装"
    exit 1
fi

echo "  Node.js: $(node -version)"
echo "  npm: $(npm -version)"

# 2. 配置npm使用本地仓库
echo "[2/4] 配置npm本地仓库..."
if [ -d "$NPM_OFFLINE_DIR" ]; then
    npm config set registry "file://$NPM_OFFLINE_DIR"
    npm config set prefix "$NPM_OFFLINE_DIR"
    npm config set cache "$NPM_OFFLINE_DIR/.npm-cache"
    npm config set legacy-peer-deps true
    echo "  npm已配置使用本地仓库"
else
    echo "  警告: 未找到离线npm目录: $NPM_OFFLINE_DIR"
fi

# 3. 复制.npmrc配置文件
echo "[3/4] 配置项目级.npmrc..."
if [ -f "$OFFLINE_DIR/repositories/.npmrc" ]; then
    cp "$OFFLINE_DIR/repositories/.npmrc" "$HOME/.npmrc"

    # 替换占位符
    sed -i "s|/opt/offline-packages|$OFFLINE_DIR|g" "$HOME/.npmrc"

    echo "  已创建 ~/.npmrc 配置文件"
else
    echo "  警告: 未找到.npmrc模板"
fi

# 4. 验证配置
echo "[4/4] 验证配置..."
echo "  npm仓库: $(npm config get registry)"
echo "  npm前缀: $(npm config get prefix)"
echo "  npm缓存: $(npm config get cache)"

echo ""
echo "=========================================="
echo "npm离线配置完成!"
echo "=========================================="
echo ""
echo "验证命令:"
echo "  npm -v"
echo "  npm list --depth=0"
echo ""
echo "构建命令:"
echo "  npm install --legacy-peer-deps"
echo "  npm run build"
echo "=========================================="
