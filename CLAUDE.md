# 报表数据处理平台 - 项目概述

> **文档版本**: V1.0
> **创建日期**: 2026-03-29
> **最后更新**: 2026-03-30
> **文档状态**: 进行中

---

## 1. 项目概述

### 1.1 项目简介

报表数据处理平台是一个轻量化的数据处理中间件，作为RPA抓取数据上传到FTP服务器和BI报表之间的中间处理环节，负责监听FTP服务器指定文件夹、读取并解析Excel文件、进行数据处理和转换、生成BI报表可用的数据表。

### 1.2 项目目标

| 目标 | 描述 |
|------|------|
| 自动化 | 自动监听FTP、解析文件、处理数据，减少人工操作 |
| 可配置 | 通过配置管理报表，无需硬编码即可新增/修改报表 |
| 可追溯 | 历史数据版本化管理(pt_dt字段)，支持数据回溯 |
| 轻量化 | 单体架构，部署简单，维护成本低 |

### 1.3 项目范围

| 范围 | 说明 |
|------|------|
| 目标用户 | 企业内部数据分析团队、RPA开发团队 |
| 报表数量 | 支持几十个报表配置 |
| 开发团队 | 1-2人小团队 |
| 部署环境 | 支持Windows/Linux服务器 |

---

## 2. 项目背景

### 2.1 业务背景

在企业数据流程中，存在以下痛点：

1. **RPA上传环节**：RPA机器人抓取数据后上传到FTP服务器
2. **BI报表环节**：BI系统需要从数据库读取标准化数据生成报表
3. **中间缺失**：FTP中的Excel文件无法直接被BI系统使用，需要中间处理

本项目填补了这个中间环节，实现自动化数据转换。

### 2.2 项目定位

- **定位**：轻量化数据处理中间件
- **角色**：RPA数据上传 → **本平台** → BI报表数据
- **核心价值**：配置驱动、自动化处理、历史可追溯

---

## 3. 项目里程碑

| 阶段 | 里程碑 | 目标日期 | 状态 |
|------|--------|----------|------|
| Phase 1 | 后端基础框架搭建 | 2026-03-29 | ✅ 已完成 |
| Phase 2 | 后端核心业务模块开发 | 2026-03-29 | ✅ 已完成 |
| Phase 3 | 前端基础框架搭建 | 2026-03-30 | ✅ 已完成 |
| Phase 4 | 前端页面开发 | 2026-03-30 | ✅ 已完成 |
| Phase 5 | 核心处理器开发 | 待定 | ⏳ 待开始 |
| Phase 6 | 系统集成测试 | 待定 | ⏳ 待开始 |
| Phase 7 | 部署上线 | 待定 | ⏳ 待开始 |

---

## 4. 项目进度

### 4.1 整体进度

| 模块 | 任务数 | 已完成 | 进行中 | 待开始 |
|------|--------|--------|--------|--------|
| 后端基础框架 | 7 | 7 | 0 | 0 |
| 后端核心业务 | 6 | 6 | 0 | 0 |
| 前端基础框架 | 3 | 3 | 0 | 0 |
| 前端页面开发 | 3 | 3 | 0 | 0 |
| **总计** | **19** | **19** | **0** | **0** |

### 4.2 已完成功能

#### 后端模块
- [x] 项目结构创建 (pom.xml, application.yml, 主类)
- [x] 统一返回结果 (Result.java)
- [x] 异常处理 (BusinessException, GlobalExceptionHandler)
- [x] 配置类 (MybatisPlusConfig, QuartzConfig, WebMvcConfig)
- [x] 枚举类 (ErrorCode, FieldTypeEnum)
- [x] 数据库初始化脚本 (schema.sql)
- [x] 实体类 (FtpConfig, ReportConfig, TaskExecution, TaskExecutionLog)
- [x] DTO类 (ColumnMapping, FieldMapping, ReportConfigDTO, TaskQueryDTO)
- [x] Mapper接口和XML
- [x] FTP配置管理模块 (Service, Controller)
- [x] FTP工具类 (FtpUtil)
- [x] 报表配置管理模块 (Service, Controller)
- [x] Excel工具类 (ExcelUtil)
- [x] 任务管理模块 (Service, Controller)
- [x] 日志管理模块 (Service, Controller)

#### 前端模块
- [x] 项目结构 (package.json, vue.config.js, main.js, App.vue)
- [x] 路由配置 (router/index.js)
- [x] 状态管理 (store/index.js)
- [x] 工具类 (request.js, auth.js)
- [x] 公共组件 (Pagination.vue, StatusTag.vue)
- [x] 样式文件 (variables.scss, index.scss)
- [x] API接口 (ftpConfig.js, reportConfig.js, task.js, log.js, data.js)
- [x] FTP配置页面 (FtpConfig.vue)
- [x] 报表列表页面 (ReportList.vue)
- [x] 报表配置页面 (ReportConfig.vue)
- [x] 任务监控页面 (TaskMonitor.vue)
- [x] 日志列表页面 (LogList.vue)

### 4.3 待完成功能

| 编号 | 功能 | 优先级 | 说明 |
|------|------|--------|------|
| T01 | FTP扫描任务 (FtpScanJob) | 高 | Quartz定时扫描FTP |
| T02 | 数据处理器 (DataHandler) | 高 | 核心数据转换处理 |
| T03 | 数据查询接口 (DataController) | 中 | 前端数据预览 |
| T04 | 文件上传功能 | 中 | 本地文件上传测试 |
| T05 | 系统配置接口 | 低 | 系统参数配置 |

---

## 5. 团队成员

| 角色 | 姓名 | 职责 |
|------|------|------|
| 项目负责人 | - | 项目规划、架构设计 |
| 后端开发 | - | 后端模块开发 |
| 前端开发 | - | 前端页面开发 |

---

## 6. 项目文档

| 文档 | 位置 | 说明 |
|------|------|------|
| PRD | /docs/prd.md | 产品需求文档 |
| 实施计划 | /docs/superpowers/plans/2026-03-29-report-platform.md | 开发执行计划 |
| 技术栈 | /docs/TECH_STACK.md | 技术选型说明 |
| 设计规范 | /docs/DESIGN.md | UI/UX设计规范 |
| API文档 | /docs/API.md | 接口文档 |
| 任务清单 | /docs/TODO.md | 任务跟踪 |

---

## 7. 快速开始

### 7.1 环境要求

| 环境 | 版本要求 | 备注 |
|------|----------|------|
| **JDK** | **1.8 (JDK 8)** | **生产环境标准版本** |
| Node.js | 12+ | 前端开发 |
| npm | 6+ | 前端包管理 |
| MySQL | 5.7+ | 数据库 |
| Maven | 3.6+ | 项目构建 |

> **注意**: 后端代码专为 JDK 1.8 适配，生产环境统一使用 JDK 1.8 运行

### 7.2 启动步骤

```bash
# 1. 初始化数据库
mysql -u root -p < report-backend/src/main/resources/schema.sql

# 2. 启动后端
cd report-backend
mvn spring-boot:run

# 3. 启动前端
cd report-front
npm install
npm run serve
```

### 7.3 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:8081 |
| 后端API | http://localhost:8080/api |

---

## 8. 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|----------|------|
| 2026-03-29 | V1.0 | 初始版本创建 | - |
| 2026-03-30 | V1.0 | 完成19个开发任务 | - |
