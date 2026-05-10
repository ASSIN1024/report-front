# 报表数据处理平台 - 极简部署指南

> **版本**: V4.0
> **适用数据库**: MariaDB 10.3.39 / MySQL 5.7+
> **发布日期**: 2026-05-08

---

## 1. 部署包内容

```
simple-deploy/
├── bin/
│   ├── report-backend.jar    # 后端应用
│   ├── application.yml       # 配置文件（修改这里）
│   ├── start.sh              # Linux启动脚本
│   └── start.bat             # Windows启动脚本
│
├── sql/
│   ├── init-mariadb.sql      # 业务表初始化
│   └── quartz_tables.sql     # Quartz调度表
│
└── docs/
    └── README.md             # 本文件
```

---

## 2. 核心特性

**配置文件与JAR同目录，Spring Boot 自动加载覆盖内置配置。**

部署时只需修改 `bin/application.yml` 中的数据库密码等配置即可。

---

## 3. 环境要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 1.8+ | 建议1.8.0_272或更高 |
| MariaDB | 10.3.39+ | 数据库 |
| 系统 | CentOS 7.x / 8.x / Windows Server | 均可 |

---

## 4. 部署步骤

### 第一步：上传并解压

```bash
unzip simple-deploy.zip -d /opt/report-platform
cd /opt/report-platform/bin
```

### 第二步：初始化数据库

```bash
mysql -u root -p
CREATE DATABASE report_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SOURCE /opt/report-platform/sql/init-mariadb.sql;
SOURCE /opt/report-platform/sql/quartz_tables.sql;
EXIT;
```

### 第三步：修改配置

编辑 `bin/application.yml`，修改数据库密码：

```yaml
spring:
  datasource:
    password: your_password_here   # ← 修改为你的密码

ftp:
  built-in:
    root-directory: /data/ftp-root  # ← FTP根目录
    password: your_ftp_password    # ← FTP密码
```

### 第四步：启动服务

**Linux:**
```bash
chmod +x start.sh
./start.sh start
```

**Windows:**
```bat
start.bat
```

---

## 5. 服务管理

```bash
./start.sh start    # 启动
./start.sh stop     # 停止
./start.sh restart  # 重启
./start.sh status   # 状态
./start.sh logs     # 日志
```

---

## 6. 配置项说明

| 配置项 | 路径 | 说明 |
|--------|------|------|
| 端口 | `server.port` | 默认8082 |
| 数据库密码 | `spring.datasource.password` | 必须修改 |
| FTP根目录 | `ftp.built-in.root-directory` | 根据环境修改 |
| FTP扫描间隔 | `job.ftp-scan.interval-minutes` | 默认5分钟 |
| 打包间隔 | `job.batch-packaging.interval-minutes` | 默认1分钟 |

---

## 7. 完整配置示例

```yaml
server:
  port: 8082

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/report_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root
    password: your_password_here

ftp:
  built-in:
    enabled: true
    port: 2021
    username: rpa_user
    password: your_ftp_password
    root-directory: C:/data/ftp-root

job:
  ftp-scan:
    enabled: true
    interval-minutes: 5
  batch-packaging:
    enabled: true
    interval-minutes: 1
```

---

## 8. 版本信息

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2026-05-07 | V3.0 | 初始版本，支持MariaDB |
| 2026-05-08 | V3.1-V3.2 | 优化配置和启动脚本 |
| 2026-05-09 | V4.0 | 配置文件移至bin目录，简化部署 |
