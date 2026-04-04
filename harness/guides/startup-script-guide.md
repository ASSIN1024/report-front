# 启动脚本操作指南

> **文档版本**: V1.0
> **创建日期**: 2026-04-04
> **最后更新**: 2026-04-04
> **维护人**: AI Assistant

---

## 1. 概述

### 1.1 功能简介

`scripts/start.sh` 是报表数据处理平台的统一启动脚本，实现前后端项目的**独立启动、停止、重启**功能。

### 1.2 核心特性

| 特性 | 说明 |
|------|------|
| 独立启动 | 前后端可分别启动，互不影响 |
| 无交互 | 自动执行，无需确认 |
| PID管理 | 通过端口检测进程，不依赖PID文件 |
| 日志记录 | 所有操作记录到日志文件 |
| 颜色输出 | 彩色终端输出，易于识别 |

### 1.3 端口配置

| 服务 | 端口 | 进程检测方式 |
|------|------|--------------|
| 后端 | 8082 | Maven Spring Boot |
| 前端 | 8083 | Vue CLI |

---

## 2. 文件结构

```
report-front/
├── scripts/
│   └── start.sh              # 统一启动脚本
├── .pid/                     # PID目录（自动创建）
│   ├── backend.pid          # 后端进程ID
│   └── frontend.pid          # 前端进程ID
└── logs/                     # 日志目录（自动创建）
    ├── backend.log           # 后端日志
    └── frontend.log          # 前端日志
```

---

## 3. 使用方法

### 3.1 基本语法

```bash
./start.sh [命令]
```

### 3.2 可用命令

| 命令 | 说明 | 示例 |
|------|------|------|
| `backend` | 启动后端服务 | `./start.sh backend` |
| `frontend` | 启动前端服务 | `./start.sh frontend` |
| `all` | 启动全部服务 | `./start.sh all` |
| `restart` | 重启全部服务 | `./start.sh restart` |
| `stop` | 停止全部服务 | `./start.sh stop` |
| `status` | 查看服务状态 | `./start.sh status` |
| `help` | 显示帮助信息 | `./start.sh help` |

### 3.3 详细使用场景

#### 场景1: 首次启动（全新环境）

```bash
cd /home/nova/projects/report-front
./start.sh all
```

**预期输出:**
```
[INFO] 启动全部服务...
[INFO] 启动后端服务...
[SUCCESS] 后端启动成功 (PID: 12345, 端口: 8082)
[INFO] 日志文件: /home/nova/projects/report-front/logs/backend.log
[INFO] 启动前端服务...
[SUCCESS] 前端启动成功 (PID: 12346, 端口: 8083)
[INFO] 日志文件: /home/nova/projects/report-front/logs/frontend.log
```

#### 场景2: 只启动后端（前端已运行）

```bash
./start.sh backend
```

#### 场景3: 重启前端（代码热更新后）

```bash
./start.sh restart
```

> **注意**: `restart` 命令会重启全部服务（前后端）

#### 场景4: 查看服务状态

```bash
./start.sh status
```

**预期输出:**
```
==========================================
          服务状态
==========================================
● 后端运行中 (PID: 12345, 端口: 8082)
● 前端运行中 (PID: 12346, 端口: 8083)
==========================================
```

#### 场景5: 停止所有服务

```bash
./start.sh stop
```

---

## 4. AI Agent 使用规范

### 4.1 启动服务

当需要启动服务进行测试或开发时，使用以下命令：

```bash
./start.sh all
```

或启动单个服务：

```bash
./start.sh backend   # 后端
./start.sh frontend  # 前端
```

### 4.2 重启服务

修改代码后需要重启服务时：

```bash
./start.sh restart
```

### 4.3 检查服务状态

在执行测试或验证功能前，先确认服务状态：

```bash
./start.sh status
```

### 4.4 日志查看

发现问题需要查看日志时：

```bash
# 后端日志
tail -f logs/backend.log

# 前端日志
tail -f logs/frontend.log
```

---

## 5. 实现原理

### 5.1 进程检测机制

脚本通过**端口检测**判断进程是否运行，而非依赖PID文件：

```bash
# 使用 lsof 检测端口
lsof -i:8082

# 或使用 ss 命令
ss -tuln | grep ":8082 "
```

### 5.2 无交互重启流程

```
1. 检测现有进程（通过端口）
2. kill 现有进程
3. 等待端口释放（最多10秒）
4. 启动新进程
5. 验证端口监听
```

### 5.3 日志重定向

所有输出重定向到日志文件：

```bash
nohup mvn spring-boot:run > logs/backend.log 2>&1 &
```

---

## 6. 注意事项

### 6.1 前置条件

| 条件 | 检查命令 |
|------|----------|
| Maven 已安装 | `mvn -v` |
| Node.js 已安装 | `node -v` |
| npm 已安装 | `npm -v` |

### 6.2 端口占用

如果启动失败，提示端口被占用：

1. 检查是否有其他进程占用端口：
   ```bash
   lsof -i:8082
   lsof -i:8083
   ```
2. 停止占用进程或更改配置

### 6.3 权限要求

脚本需要执行权限：

```bash
chmod +x scripts/start.sh
```

### 6.4 日志清理

日志文件可能随时间增长，定期清理：

```bash
# 清空日志（保留文件）
> logs/backend.log
> logs/frontend.log

# 或删除旧日志
rm logs/*.log
```

---

## 7. 故障排查

### 7.1 后端启动失败

**检查步骤:**
1. 查看后端日志: `tail -50 logs/backend.log`
2. 检查Maven依赖: `cd report-backend && mvn clean`
3. 检查数据库连接配置

### 7.2 前端启动失败

**检查步骤:**
1. 查看前端日志: `tail -50 logs/frontend.log`
2. 检查node_modules: `cd src && rm -rf node_modules && npm install`
3. 检查端口占用: `lsof -i:8083`

### 7.3 进程残留

如果重启后旧进程未停止：

```bash
# 手动停止所有Java进程
pkill -f "spring-boot"

# 手动停止所有Node进程
pkill -f "vue-cli-service"

# 或按端口停止
kill $(lsof -ti:8082)
kill $(lsof -ti:8083)
```

---

## 8. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-04-04 | V1.0 | 初始创建 | AI Assistant |
