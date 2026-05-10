#!/bin/bash
# npm离线依赖下载脚本
# 使用方法: ./download-npm-deps.sh
# 运行前请确保已联网且安装了npm

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OFFLINE_DIR="$(dirname "$SCRIPT_DIR")"
NPM_OFFLINE_DIR="$OFFLINE_DIR/npm-offline"
NPM_CACHE_DIR="$OFFLINE_DIR/npm-cache"

echo "=========================================="
echo "npm离线依赖下载工具"
echo "=========================================="
echo "目标目录: $NPM_OFFLINE_DIR"
echo ""

# 创建目录
mkdir -p "$NPM_OFFLINE_DIR"
mkdir -p "$NPM_CACHE_DIR"

# 检查npm是否可用
if ! command -v npm &> /dev/null; then
    echo "错误: 当前环境未安装npm"
    echo "请在联网环境下先安装Node.js和npm"
    exit 1
fi

# npm版本检查
NPM_VERSION=$(npm -version)
NODE_VERSION=$(node -version)
echo "检测到Node.js版本: $NODE_VERSION"
echo "检测到npm版本: $NPM_VERSION"

# 切换到目标目录
cd "$NPM_OFFLINE_DIR"

# 创建package.json
cat > package.json << 'EOF'
{
  "name": "report-platform-offline",
  "version": "1.0.0",
  "description": "报表平台离线依赖包",
  "private": true,
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint"
  },
  "dependencies": {
    "vue": "^2.6.14",
    "vue-router": "^3.5.4",
    "vuex": "^3.6.2",
    "element-ui": "^2.15.14",
    "axios": "^0.21.4"
  },
  "devDependencies": {
    "@vue/cli-service": "~4.5.0",
    "@vue/cli-plugin-babel": "~4.5.0",
    "@vue/cli-plugin-router": "~4.5.0",
    "@vue/cli-plugin-vuex": "~4.5.0",
    "@vue/cli-plugin-eslint": "~4.5.0",
    "babel-eslint": "^10.1.0",
    "sass": "^1.26.5",
    "sass-loader": "^8.0.2",
    "vue-template-compiler": "^2.6.14",
    "webpack": "^4.46.0"
  },
  "engines": {
    "node": ">= 12.0.0",
    "npm": ">= 6.0.0"
  }
}
EOF

echo "开始下载npm依赖..."
echo ""

# 使用镜像源加速下载
NPM_REGISTRY="https://registry.npmmirror.com"

# 设置npm配置
npm config set registry "$NPM_REGISTRY"
npm config set cache "$NPM_CACHE_DIR"
npm config set prefix "$NPM_OFFLINE_DIR"
npm config set legacy-peer-deps true

# 下载依赖
npm install \
    --registry="$NPM_REGISTRY" \
    --cache "$NPM_CACHE_DIR" \
    --prefix "$NPM_OFFLINE_DIR" \
    --legacy-peer-deps \
    --no-audit \
    --no-fund \
    2>&1 | tail -20

# 检查node_modules是否存在
if [ -d "$NPM_OFFLINE_DIR/node_modules" ]; then
    MODULE_COUNT=$(ls -1 "$NPM_OFFLINE_DIR/node_modules" | wc -l)
    echo ""
    echo "成功下载 $MODULE_COUNT 个依赖包"
else
    echo ""
    echo "警告: node_modules目录未找到，下载可能失败"
fi

# 打包为tar.gz
echo ""
echo "打包npm离线资源..."
cd "$OFFLINE_DIR"
tar -czvf "npm-offline.tar.gz" -C "$NPM_OFFLINE_DIR" .

# 计算大小
NPM_SIZE=$(du -sh "$NPM_OFFLINE_DIR" 2>/dev/null | cut -f1)
TAR_SIZE=$(du -sh "npm-offline.tar.gz" 2>/dev/null | cut -f1)

# 生成依赖清单
echo ""
echo "生成依赖清单..."
cd "$NPM_OFFLINE_DIR"
npm list --depth=0 --json > "$OFFLINE_DIR/npm-dependencies.json" 2>/dev/null || true
npm list --depth=0 > "$OFFLINE_DIR/npm-dependencies.txt" 2>/dev/null || true

echo ""
echo "=========================================="
echo "npm依赖下载完成!"
echo "=========================================="
echo "离线目录: $NPM_OFFLINE_DIR"
echo "目录大小: $NPM_SIZE"
echo "打包文件: $OFFLINE_DIR/npm-offline.tar.gz"
echo "打包大小: $TAR_SIZE"
echo "依赖清单: $OFFLINE_DIR/npm-dependencies.txt"
echo ""
echo "下一步:"
echo "1. 将此目录复制到内网服务器"
echo "2. 配置npm使用本地仓库"
echo "3. 参考 README.md 进行部署"
echo "=========================================="
