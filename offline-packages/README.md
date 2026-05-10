# 内网离线部署方案

> **文档版本**: V1.0
> **创建日期**: 2026-05-07
> **维护人**: AI Assistant

---

## 1. 方案概述

### 1.1 背景

内网环境无法访问Maven中央仓库和npm官方仓库，需要预先收集所有依赖并配置本地仓库。

### 1.2 目标

- 将Maven依赖和npm依赖完整打包
- 提供本地仓库配置方案
- 简化配置文件修改，实现"零命令行"部署
- 确保依赖版本完全匹配

### 1.3 资源清单

| 资源类型 | 大小估算 | 说明 |
|----------|----------|------|
| Maven离线包 | 200-300MB | 包含所有Spring Boot及项目依赖 |
| npm离线包 | 150-200MB | 包含Vue及所有前端依赖 |
| Maven嵌入版 | 8-10MB | 可携带的Maven命令行工具 |
| npm嵌入版 | 40-50MB | 可携带的npm命令行工具 |

---

## 2. 目录结构

```
offline-packages/
├── maven-offline-bin/           # Maven命令行工具 (可移植版)
│   ├── bin/
│   │   └── mvn                  # Linux/macOS启动脚本
│   ├── boot/
│   │   └── maven-boot.jar      # 嵌入式Maven引导程序
│   └── conf/
│       └── settings.xml         # 本地仓库配置
│
├── maven-repository/           # Maven本地仓库
│   └── (通过脚本从联网环境下载)
│
├── npm-offline-bin/            # npm命令行工具 (可移植版)
│   ├── bin/
│   │   ├── npm
│   │   └── node
│   └── lib/
│       └── (npm核心模块)
│
├── npm-offline/                # npm本地仓库
│   └── (通过脚本从联网环境下载)
│
├── repositories/               # 依赖配置文件
│   ├── maven-settings.xml     # Maven settings.xml配置
│   └── .npmrc                 # npm配置
│
├── scripts/                    # 离线安装脚本
│   ├── download-maven-deps.sh  # 下载Maven依赖脚本
│   ├── download-npm-deps.sh    # 下载npm依赖脚本
│   ├── install-maven-offline.sh # Maven离线安装脚本
│   └── install-npm-offline.sh  # npm离线安装脚本
│
└── README.md                   # 本文件
```

---

## 3. Maven依赖配置

### 3.1 离线settings.xml配置

**文件**: `repositories/maven-settings.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0
                              http://maven.apache.org/xsd/settings-1.2.0.xsd">

    <!-- 本地仓库路径 -->
    <localRepository>${user.home}/.m2/repository</localRepository>

    <mirrors>
        <!-- 本地仓库镜像 -->
        <mirror>
            <id>local-maven-repo</id>
            <name>Local Maven Repository</name>
            <url>file://${user.home}/offline-packages/maven-repository</url>
            <mirrorOf>*</mirrorOf>
        </mirror>
    </mirrors>

    <profiles>
        <profile>
            <id>offline</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
            <repositories>
                <repository>
                    <id>central</id>
                    <url>file://${user.home}/offline-packages/maven-repository</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <id>central</id>
                    <url>file://${user.home}/offline-packages/maven-repository</url>
                    <releases>
                        <enabled>true</enabled>
                    </releases>
                    <snapshots>
                        <enabled>false</enabled>
                    </snapshots>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>offline</activeProfile>
    </activeProfiles>
</settings>
```

### 3.2 项目pom.xml配置 (无需修改)

项目已有的pom.xml配置无需任何修改，Maven会自动使用本地仓库。

### 3.3 Maven依赖清单

#### 核心依赖 (已验证)

| 依赖 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 2.1.2.RELEASE | 核心框架 |
| MyBatis-Plus | 3.4.3 | ORM框架 |
| Druid | 1.1.20 | 数据库连接池 |
| Hutool | 5.8.25 | 工具库 |
| Apache POI | 4.1.2 | Excel处理 |
| Commons-Net | 3.9.0 | FTP客户端 |
| Apache FtpServer | 1.2.0 | 内置FTP服务 |
| Mina Core | 2.2.0 | NIO框架 |
| MySQL Connector | 8.0.28 | MySQL驱动 |
| OpenGauss Driver | - | GaussDB驱动(需单独获取) |

#### 依赖组说明

```
spring-boot-starter-web        # Web服务
spring-boot-starter-quartz     # 任务调度
spring-boot-starter-aop        # AOP支持
mybatis-plus-boot-starter     # MyBatis增强
druid-spring-boot-starter      # 连接池
ftpserver-core               # FTP服务器
mina-core                    # NIO框架
poi-ooxml                   # Office文档
commons-net                  # 网络工具
hutool-all                   # 工具集
```

---

## 4. npm依赖配置

### 4.1 离线.npmrc配置

**文件**: `repositories/.npmrc`

```
registry=file:///path/to/offline-packages/npm-offline
cache=/path/to/offline-packages/npm-cache
prefix=/path/to/offline-packages/npm-offline-bin
```

### 4.2 npm依赖清单

#### 生产依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| vue | 2.6.14 | 核心框架 |
| vue-router | 3.5.4 | 路由 |
| vuex | 3.6.2 | 状态管理 |
| element-ui | 2.15.14 | UI组件库 |
| axios | 0.21.4 | HTTP客户端 |

#### 开发依赖

| 依赖 | 版本 | 说明 |
|------|------|------|
| @vue/cli-service | ~4.5.0 | 构建服务 |
| @vue/cli-plugin-babel | ~4.5.0 | Babel插件 |
| @vue/cli-plugin-router | ~4.5.0 | 路由插件 |
| @vue/cli-plugin-vuex | ~4.5.0 | Vuex插件 |
| sass | 1.26.5+ | SCSS编译 |
| sass-loader | 8.0.2+ | Sass加载器 |
| vue-template-compiler | 2.6.14 | 模板编译 |

---

## 5. 下载脚本

### 5.1 Maven依赖下载脚本

**文件**: `scripts/download-maven-deps.sh`

```bash
#!/bin/bash
# Maven离线依赖下载脚本
# 用法: ./download-maven-deps.sh

set -e

OFFLINE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPO_DIR="$OFFLINE_DIR/maven-repository"
MAVEN_VERSION="3.8.6"

echo "=========================================="
echo "Maven离线依赖下载工具"
echo "=========================================="

# 创建仓库目录
mkdir -p "$REPO_DIR"

# 检查Maven是否可用
if command -v mvn &> /dev/null; then
    MAVEN_CMD="mvn"
else
    echo "错误: 当前环境未安装Maven"
    echo "请在联网环境下运行此脚本"
    exit 1
fi

# 定义需要下载的依赖
ARTIFACTS=(
    "org.springframework.boot:spring-boot-starter-web:2.1.2.RELEASE"
    "org.springframework.boot:spring-boot-starter-quartz:2.1.2.RELEASE"
    "org.springframework.boot:spring-boot-starter-aop:2.1.2.RELEASE"
    "com.baomidou:mybatis-plus-boot-starter:3.4.3"
    "com.alibaba:druid-spring-boot-starter:1.1.20"
    "cn.hutool:hutool-all:5.8.25"
    "org.apache.poi:poi-ooxml:4.1.2"
    "commons-net:commons-net:3.9.0"
    "org.apache.ftpserver:ftpserver-core:1.2.0"
    "org.apache.ftpserver:ftplet-api:1.2.0"
    "org.apache.mina:mina-core:2.2.0"
    "mysql:mysql-connector-java:8.0.28"
    "org.springframework.security:spring-security-crypto:5.2.1.RELEASE"
    "org.projectlombok:lombok:1.18.24"
)

echo "开始下载依赖到: $REPO_DIR"
echo "预计需要下载 ${#ARTIFACTS[@]} 个依赖组"
echo ""

# 下载依赖
for artifact in "${ARTIFACTS[@]}"; do
    echo "下载: $artifact"
    mvn dependency:get \
        -Dartifact="$artifact" \
        -DremoteRepositories=central \
        -Dtransitive=true \
        -DoutputDirectory="$REPO_DIR" \
        -q
done

# 复制传递依赖
echo ""
echo "下载传递依赖..."
mvn dependency:copy-dependencies \
    -DoutputDirectory="$REPO_DIR" \
    -DincludeScope=runtime \
    -q

echo ""
echo "=========================================="
echo "Maven依赖下载完成!"
echo "仓库位置: $REPO_DIR"
echo "=========================================="
```

### 5.2 npm依赖下载脚本

**文件**: `scripts/download-npm-deps.sh`

```bash
#!/bin/bash
# npm离线依赖下载脚本
# 用法: ./download-npm-deps.sh

set -e

OFFLINE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
NPM_OFFLINE_DIR="$OFFLINE_DIR/npm-offline"

echo "=========================================="
echo "npm离线依赖下载工具"
echo "=========================================="

# 创建目录
mkdir -p "$NPM_OFFLINE_DIR"
cd "$NPM_OFFLINE_DIR"

# 检查npm是否可用
if ! command -v npm &> /dev/null; then
    echo "错误: 当前环境未安装npm"
    echo "请在联网环境下运行此脚本"
    exit 1
fi

# 初始化package.json (如果不存在)
if [ ! -f package.json ]; then
    cat > package.json << 'EOF'
{
  "name": "offline-dependencies",
  "version": "1.0.0",
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
    "sass": "^1.26.5",
    "sass-loader": "^8.0.2",
    "vue-template-compiler": "^2.6.14"
  }
}
EOF
fi

echo "开始下载npm依赖..."
echo "目标位置: $NPM_OFFLINE_DIR"
echo ""

# 下载所有依赖
npm install \
    --registry=https://registry.npmmirror.com \
    --prefix="$NPM_OFFLINE_DIR" \
    --cache "$NPM_OFFLINE_DIR/.npm-cache" \
    --legacy-peer-deps

# 打包为离线压缩包
echo ""
echo "打包npm离线资源..."
cd "$NPM_OFFLINE_DIR"
tar -czvf "../npm-offline.tar.gz" .

echo ""
echo "=========================================="
echo "npm依赖下载完成!"
echo "离线包位置: $OFFLINE_DIR/npm-offline.tar.gz"
echo "=========================================="
```

---

## 6. 内网部署指南

### 6.1 第一步：复制离线包

将整个 `offline-packages` 目录复制到内网服务器的任意位置，例如：

```bash
# 在内网服务器上
cd /opt
scp -r user@外网服务器:/path/to/offline-packages ./
```

### 6.2 第二步：配置环境变量

**编辑用户环境变量** (`~/.bashrc` 或 `~/.profile`)：

```bash
# Maven配置
export M2_HOME=/opt/offline-packages/maven-repository
export PATH=/opt/offline-packages/maven-offline-bin:$PATH

# npm配置
export PATH=/opt/offline-packages/npm-offline-bin:$PATH
```

**使配置生效**：
```bash
source ~/.bashrc
```

### 6.3 第三步：配置Maven

**复制settings.xml**：
```bash
mkdir -p ~/.m2
cp /opt/offline-packages/repositories/maven-settings.xml ~/.m2/settings.xml
```

**验证Maven配置**：
```bash
mvn -v
# 应该显示本地仓库路径
```

### 6.4 第四步：配置npm

**设置npm使用本地仓库**：
```bash
# 方法1: 复制.npmrc到用户目录
cp /opt/offline-packages/repositories/.npmrc ~/

# 方法2: 直接设置
npm config set registry file:///opt/offline-packages/npm-offline
```

**验证npm配置**：
```bash
npm config get registry
# 应该显示本地文件路径
```

### 6.5 第五步：构建项目

**构建后端**：
```bash
cd /path/to/project/report-backend
mvn clean package -DskipTests
```

**构建前端**：
```bash
cd /path/to/project/src
npm install --legacy-peer-deps
npm run build
```

---

## 7. 依赖打包清单

### 7.1 Maven依赖清单

使用以下命令生成完整依赖清单：

```bash
cd /path/to/project/report-backend
mvn dependency:list > dependency-list.txt
mvn dependency:tree > dependency-tree.txt
```

### 7.2 npm依赖清单

```bash
cd /path/to/project/src
npm list --all > dependency-list.txt
```

### 7.3 打包检查清单

```markdown
## 离线包检查清单

### Maven依赖
- [ ] spring-boot-starter-web
- [ ] spring-boot-starter-quartz
- [ ] spring-boot-starter-aop
- [ ] mybatis-plus-boot-starter
- [ ] druid-spring-boot-starter
- [ ] hutool-all
- [ ] poi-ooxml
- [ ] commons-net
- [ ] ftpserver-core
- [ ] ftplet-api
- [ ] mina-core
- [ ] mysql-connector-java
- [ ] spring-security-crypto
- [ ] lombok

### npm依赖
- [ ] vue
- [ ] vue-router
- [ ] vuex
- [ ] element-ui
- [ ] axios
- [ ] @vue/cli-service
- [ ] sass
- [ ] sass-loader
- [ ] vue-template-compiler
```

---

## 8. 验证步骤

### 8.1 Maven验证

```bash
# 1. 验证本地仓库
ls ~/.m2/repository | head -20

# 2. 验证依赖解析
cd /path/to/project/report-backend
mvn dependency:resolve -o  # offline模式

# 3. 验证编译
mvn compile -o
```

### 8.2 npm验证

```bash
# 1. 验证本地缓存
ls /opt/offline-packages/npm-offline/node_modules | head -20

# 2. 验证依赖安装
cd /path/to/project/src
npm ls --depth=0

# 3. 验证构建
npm run build --offline
```

### 8.3 完整构建验证

```bash
# 后端
cd /path/to/project/report-backend
mvn clean package -DskipTests -o

# 前端
cd /path/to/project/src
npm run build
```

---

## 9. 常见问题

### 9.1 Maven "Could not resolve dependencies"

**原因**: 本地仓库缺少依赖

**解决**:
1. 检查 `settings.xml` 是否正确配置
2. 确认依赖已下载到 `maven-repository` 目录
3. 尝试使用完整路径: `mvn -s /opt/offline-packages/repositories/maven-settings.xml ...`

### 9.2 npm "ENOENT: no such file or directory"

**原因**: 本地仓库路径不正确

**解决**:
```bash
# 重置npm配置
npm config delete registry
npm config set registry file:///opt/offline-packages/npm-offline
```

### 9.3 依赖版本不匹配

**原因**: 传递依赖有不同的版本要求

**解决**: 在pom.xml中显式声明正确的版本

---

## 10. 附录

### 10.1 快速下载命令 (联网环境)

```bash
# Maven依赖
cd /path/to/project/report-backend
mvn dependency:copy-dependencies -DoutputDirectory=/path/to/offline-packages/maven-repository

# npm依赖
cd /path/to/project/src
npm install --registry=https://registry.npmmirror.com --prefix=/path/to/offline-packages/npm-offline
```

### 10.2 仓库大小估算

| 组件 | 大小 |
|------|------|
| Spring Boot 2.1.2 | ~50MB |
| MyBatis-Plus | ~10MB |
| Vue 2.6 + Element UI | ~30MB |
| 其他依赖 | ~100MB |
| **总计** | ~200MB |

### 10.3 联系方式

如有问题，请联系项目维护人员。
