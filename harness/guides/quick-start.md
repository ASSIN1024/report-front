# 快速开始指南

> **文档版本**: V1.0
> **创建日期**: 2026-04-05
> **最后更新**: 2026-04-05
> **维护人**: AI Assistant

---

## 1. 环境要求

| 环境 | 版本要求 | 备注 |
|------|----------|------|
| **JDK** | **1.8 (JDK 8)** | **生产环境标准版本** |
| Node.js | 12+ | 前端开发 |
| npm | 6+ | 前端包管理 |
| MySQL | 5.7+ | 数据库 |
| Maven | 3.6+ | 项目构建 |

> **注意**: 后端代码专为 JDK 1.8 适配，生产环境统一使用 JDK 1.8 运行

---

## 2. 启动步骤

### 2.1 方式一：使用启动脚本（推荐）

```bash
# 检查服务状态
./scripts/start.sh status

# 启动所有服务（MySQL + Redis + FTP + Backend + Frontend）
./scripts/start.sh all

# 单独启动后端
./scripts/start.sh backend

# 单独启动前端
./scripts/start.sh frontend

# 重启所有服务
./scripts/start.sh restart

# 停止所有服务
./scripts/start.sh stop
```

### 2.2 方式二：手动启动

#### 步骤1: 初始化数据库

```bash
mysql -u root -p < report-backend/src/main/resources/schema.sql
```

#### 步骤2: 启动后端 (端口8082)

```bash
cd report-backend
mvn spring-boot:run -Dmaven.test.skip=true
```

#### 步骤3: 启动前端 (端口8083)

```bash
cd report-front
npm install
npm run serve
```

---

## 3. 访问地址

| 服务 | 地址 | 凭据 |
|------|------|------|
| 前端 | http://localhost:8083 | - |
| 后端API | http://localhost:8082/api | - |
| Druid监控 | http://localhost:8082/druid | admin/admin |

---

## 4. 服务状态检查

### 4.1 验证后端服务

```bash
curl http://localhost:8082/api/ftp-config/list
```

### 4.2 验证前端服务

访问 http://localhost:8083 ，确认页面正常加载。

### 4.3 Docker 服务状态

```bash
# 查看运行中的容器
docker ps

# 检查 MySQL
docker ps | grep mysql

# 检查 FTP
docker ps | grep vsftpd
```

---

## 5. 常见问题排查

### 5.1 系统时间检查

如果遇到服务启动异常，首先检查系统时间：

```bash
date
timedatectl
```

确保 NTP 同步启用且时间正确。

### 5.2 端口占用检查

如果端口被占用：

```bash
# 检查端口占用
ss -tuln | grep 8082
ss -tuln | grep 8083

# 终止占用进程
kill -9 <PID>
```

### 5.3 日志查看

```bash
# 后端日志
tail -f report-backend/logs/application.log

# 前端日志
npm run serve 的终端输出
```

---

## 6. 下一步

- 查看 [项目上下文](harness/docs/project-context.md) 了解项目详情
- 查看 [任务清单](harness/tasks.json) 了解当前任务
- 查看 [操作日志](../src/views/OperationLog.vue) 监控系统操作

---

## 7. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 | 关联任务ID |
|------|------|----------|--------|------------|
| 2026-04-05 | V1.0 | [新增] 从CLAUDE.md迁移快速开始指南 | AI Assistant | H-009 |
