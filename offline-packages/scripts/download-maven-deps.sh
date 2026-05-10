#!/bin/bash
# Maven离线依赖下载脚本
# 使用方法: ./download-maven-deps.sh
# 运行前请确保已联网且安装了Maven

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OFFLINE_DIR="$(dirname "$SCRIPT_DIR")"
REPO_DIR="$OFFLINE_DIR/maven-repository"

echo "=========================================="
echo "Maven离线依赖下载工具"
echo "=========================================="
echo "目标目录: $REPO_DIR"
echo ""

# 创建仓库目录
mkdir -p "$REPO_DIR"

# 检查Maven是否可用
if ! command -v mvn &> /dev/null; then
    echo "错误: 当前环境未安装Maven"
    echo "请在联网环境下先安装Maven"
    exit 1
fi

# Maven版本检查
MAVEN_VERSION=$(mvn -version | head -1 | awk '{print $3}')
echo "检测到Maven版本: $MAVEN_VERSION"

# 定义项目核心依赖 (组ID:artifactID:version)
declare -a ARTIFACTS=(
    # Spring Boot核心
    "org.springframework.boot:spring-boot-starter-web:2.1.2.RELEASE"
    "org.springframework.boot:spring-boot-starter-quartz:2.1.2.RELEASE"
    "org.springframework.boot:spring-boot-starter-aop:2.1.2.RELEASE"
    "org.springframework.boot:spring-boot-starter-jdbc:2.1.2.RELEASE"
    "org.springframework.boot:spring-boot-starter-validation:2.1.2.RELEASE"

    # MyBatis-Plus
    "com.baomidou:mybatis-plus-boot-starter:3.4.3"

    # 数据库连接池
    "com.alibaba:druid-spring-boot-starter:1.1.20"

    # 工具库
    "cn.hutool:hutool-all:5.8.25"

    # Office文档处理
    "org.apache.poi:poi-ooxml:4.1.2"
    "org.apache.poi:poi:4.1.2"

    # FTP服务
    "org.apache.ftpserver:ftpserver-core:1.2.0"
    "org.apache.ftpserver:ftplet-api:1.2.0"
    "org.apache.ftpserver:mina-ftpserver:1.2.0"

    # NIO框架
    "org.apache.mina:mina-core:2.2.0"

    # 网络工具
    "commons-net:commons-net:3.9.0"

    # 数据库驱动
    "mysql:mysql-connector-java:8.0.28"

    # 安全框架
    "org.springframework.security:spring-security-crypto:5.2.1.RELEASE"

    # 工具类
    "org.projectlombok:lombok:1.18.24"
    "com.fasterxml.jackson.core:jackson-databind:2.9.10.8"
    "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.9.10"

    # SLF4J日志
    "org.slf4j:slf4j-api:1.7.29"
    "ch.qos.logback:logback-classic:1.2.3"

    # 测试依赖
    "org.springframework.boot:spring-boot-starter-test:2.1.2.RELEASE"
    "org.junit.jupiter:junit-jupiter:5.4.2"
)

echo "开始下载 ${#ARTIFACTS[@]} 个核心依赖..."
echo "传递依赖将自动下载"
echo ""

# 设置进度显示
TOTAL=${#ARTIFACTS[@]}
CURRENT=0

# 下载每个依赖
for artifact in "${ARTIFACTS[@]}"; do
    CURRENT=$((CURRENT + 1))
    echo "[$CURRENT/$TOTAL] 下载: $artifact"

    mvn dependency:get \
        -Dartifact="$artifact" \
        -DremoteRepositories=central \
        -Dtransitive=true \
        -DoutputDirectory="$REPO_DIR" \
        -q 2>/dev/null || echo "  警告: $artifact 下载可能失败"
done

# 复制pom文件以便Maven识别
echo ""
echo "整理Maven仓库结构..."
find "$REPO_DIR" -name "*.pom" -type f 2>/dev/null | head -50

# 生成依赖树文件
echo ""
echo "生成依赖清单..."
cd "$REPO_DIR"
if command -v find &> /dev/null && command -v awk &> /dev/null; then
    echo "# Maven依赖清单 - 生成时间: $(date)" > "$OFFLINE_DIR/maven-dependencies.txt"
    echo "" >> "$OFFLINE_DIR/maven-dependencies.txt"
    find . -type d -name "*.jar" 2>/dev/null | while read dir; do
        jar=$(basename "$dir")
        group=$(echo "$dir" | sed 's|^\./||' | sed 's|/.*||' | tr '.' '/')
        artifact=$(echo "$jar" | sed 's/-[0-9].*//')
        version=$(echo "$jar" | sed 's/.*-\([0-9].*\)\.jar/\1/')
        echo "$group:$artifact:$version" >> "$OFFLINE_DIR/maven-dependencies.txt"
    done
    sort -u "$OFFLINE_DIR/maven-dependencies.txt" -o "$OFFLINE_DIR/maven-dependencies.txt"
fi

# 计算仓库大小
REPO_SIZE=$(du -sh "$REPO_DIR" 2>/dev/null | cut -f1)

echo ""
echo "=========================================="
echo "Maven依赖下载完成!"
echo "=========================================="
echo "仓库位置: $REPO_DIR"
echo "仓库大小: $REPO_SIZE"
echo "依赖清单: $OFFLINE_DIR/maven-dependencies.txt"
echo ""
echo "下一步:"
echo "1. 将此目录复制到内网服务器"
echo "2. 配置Maven使用本地仓库"
echo "3. 参考 README.md 进行部署"
echo "=========================================="
