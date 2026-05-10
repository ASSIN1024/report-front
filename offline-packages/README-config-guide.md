# 内网部署 - 配置文件修改指南

> **文档版本**: V1.0
> **创建日期**: 2026-05-07

---

## 1. 概述

本指南针对内网环境无法连接Maven和npm源的情况，通过简单的配置文件修改即可完成依赖的本地化部署。

## 2. 修改文件清单

| 序号 | 文件 | 修改内容 | 重要性 |
|------|------|----------|--------|
| 1 | `~/.m2/settings.xml` | Maven本地仓库配置 | 必须 |
| 2 | `~/.npmrc` | npm本地仓库配置 | 必须 |
| 3 | 项目pom.xml | 依赖版本锁定 | 可选 |
| 4 | 项目package.json | 依赖版本锁定 | 可选 |

---

## 3. Maven配置修改 (必须)

### 3.1 复制配置文件

```bash
# 在内网服务器上执行
mkdir -p ~/.m2
cp /path/to/offline-packages/repositories/maven-settings.xml ~/.m2/settings.xml
```

### 3.2 修改本地仓库路径

编辑 `~/.m2/settings.xml`，找到以下内容：

```xml
<!-- 找到这行 -->
<url>file://{user.home}/offline-packages/maven-repository</url>

<!-- 修改为实际路径，例如 -->
<url>file:///opt/offline-packages/maven-repository</url>
```

或者使用sed命令直接替换：

```bash
# 根据实际路径修改
sed -i 's|{user.home}/offline-packages|/opt/offline-packages|g' ~/.m2/settings.xml
sed -i 's|\${user.home}/offline-packages|/opt/offline-packages|g' ~/.m2/settings.xml
```

### 3.3 验证Maven配置

```bash
mvn -v
# 应该显示本地仓库路径
```

---

## 4. npm配置修改 (必须)

### 4.1 复制配置文件

```bash
# 在内网服务器上执行
cp /path/to/offline-packages/repositories/.npmrc ~/
```

### 4.2 修改本地仓库路径

编辑 `~/.npmrc`，找到以下内容：

```ini
# 修改这行
registry=file:///opt/offline-packages/npm-offline

# 改为实际路径
registry=file:///你的实际路径/offline-packages/npm-offline
```

或者使用sed命令直接替换：

```bash
sed -i 's|/opt/offline-packages|/你的实际路径/offline-packages|g' ~/.npmrc
```

### 4.3 验证npm配置

```bash
npm config get registry
# 应该显示本地文件路径
```

---

## 5. 项目pom.xml配置 (推荐)

### 5.1 锁定依赖版本

为确保离线环境下依赖能够正确解析，建议在pom.xml中添加以下配置：

```xml
<properties>
    <!-- Spring Boot版本 -->
    <spring-boot.version>2.1.2.RELEASE</spring-boot.version>

    <!-- MyBatis-Plus版本 -->
    <mybatis-plus.version>3.4.3</mybatis-plus.version>

    <!-- Druid版本 -->
    <druid.version>1.1.20</druid.version>

    <!-- Hutool版本 -->
    <hutool.version>5.8.25</hutool.version>

    <!-- POI版本 -->
    <poi.version>4.1.2</poi.version>
</properties>
```

### 5.2 依赖管理配置

```xml
<dependencyManagement>
    <dependencies>
        <!-- Spring Boot依赖管理 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- MyBatis-Plus依赖管理 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 6. 项目package.json配置 (推荐)

### 6.1 锁定依赖版本

```json
{
  "dependencies": {
    "vue": "2.6.14",
    "vue-router": "3.5.4",
    "vuex": "3.6.2",
    "element-ui": "2.15.14",
    "axios": "0.21.4"
  },
  "devDependencies": {
    "@vue/cli-service": "4.5.19",
    "sass": "1.26.5",
    "sass-loader": "8.0.2",
    "vue-template-compiler": "2.6.14"
  }
}
```

### 6.2 npm离线安装

```bash
# 进入前端项目目录
cd /path/to/project/src

# 清理缓存
npm cache clean --force

# 离线安装
npm install --legacy-peer-deps --offline
```

---

## 7. 一键配置脚本

创建 `setup-offline.sh` 脚本：

```bash
#!/bin/bash
# 一键配置离线环境

# 设置离线包路径
OFFLINE_PATH="/opt/offline-packages"

echo "开始配置离线环境..."

# 1. 配置Maven
echo "1. 配置Maven..."
mkdir -p ~/.m2
cp "$OFFLINE_PATH/repositories/maven-settings.xml" ~/.m2/settings.xml
sed -i "s|/opt/offline-packages|$OFFLINE_PATH|g" ~/.m2/settings.xml
echo "   Maven配置完成"

# 2. 配置npm
echo "2. 配置npm..."
cp "$OFFLINE_PATH/repositories/.npmrc" ~/.npmrc
sed -i "s|/opt/offline-packages|$OFFLINE_PATH|g" ~/.npmrc
echo "   npm配置完成"

# 3. 验证
echo ""
echo "验证配置:"
echo "Maven本地仓库:"
mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout 2>/dev/null || echo "配置中"
echo ""
echo "npm仓库:"
npm config get registry

echo ""
echo "离线环境配置完成!"
```

---

## 8. 快速验证

### 8.1 Maven验证命令

```bash
# 验证本地仓库
mvn help:evaluate -Dexpression=settings.localRepository -q -DforceStdout

# 验证依赖解析
cd /path/to/project/report-backend
mvn dependency:resolve -o

# 验证编译
mvn compile -o
```

### 8.2 npm验证命令

```bash
# 验证仓库
npm config get registry

# 验证依赖列表
npm list --depth=0
```

---

## 9. 常见问题

### Q1: Maven提示 "Could not resolve dependencies"

**原因**: 本地仓库路径不正确

**解决**:
1. 检查 `~/.m2/settings.xml` 中的路径
2. 确认 `maven-repository` 目录存在
3. 执行 `mvn -X` 查看详细日志

### Q2: npm提示 "enoent ENOENT: no such file or directory"

**原因**: 本地仓库目录不存在或路径错误

**解决**:
1. 检查 `~/.npmrc` 中的路径
2. 确认 `npm-offline` 目录存在
3. 确认 `node_modules` 子目录存在

### Q3: 依赖版本冲突

**原因**: 传递依赖有不同的版本要求

**解决**:
在pom.xml中显式声明冲突的依赖版本

---

## 10. 配置完成检查清单

- [ ] `~/.m2/settings.xml` 已创建并配置正确
- [ ] Maven本地仓库路径已修改
- [ ] `~/.npmrc` 已创建并配置正确
- [ ] npm本地仓库路径已修改
- [ ] `mvn -v` 显示正确的本地仓库
- [ ] `npm config get registry` 显示本地路径
- [ ] 后端项目 `mvn compile -o` 成功
- [ ] 前端项目 `npm install --offline` 成功
