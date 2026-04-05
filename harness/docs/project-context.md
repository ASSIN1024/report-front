# 项目上下文

> **文档版本**: V1.0
> **创建日期**: 2026-04-05
> **最后更新**: 2026-04-05
> **维护人**: AI Assistant

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

## 2. 项目定位

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

### 2.3 技术栈

#### 后端
- **框架**: Spring Boot 2.1.2
- **ORM**: MyBatis-Plus 3.x
- **数据库**: MySQL 5.7+
- **连接池**: Druid
- **定时任务**: Quartz
- **JDK版本**: 1.8

#### 前端
- **框架**: Vue 2.6
- **路由**: Vue Router 3
- **状态管理**: Vuex 3
- **UI库**: Element UI 2
- **HTTP客户端**: Axios 0.21

---

## 3. 项目里程碑

| 阶段 | 里程碑 | 目标日期 | 状态 |
|------|--------|----------|------|
| Phase 1 | 后端基础框架搭建 | 2026-03-29 | ✅ 已完成 |
| Phase 2 | 后端核心业务模块开发 | 2026-03-29 | ✅ 已完成 |
| Phase 3 | 前端基础框架搭建 | 2026-03-30 | ✅ 已完成 |
| Phase 4 | 前端页面开发 | 2026-03-30 | ✅ 已完成 |
| Phase 5 | 核心处理器开发 | 2026-03-30 | ✅ 已完成 |
| Phase 6 | 系统集成测试 | 2026-04-01 | ✅ 已完成 |
| Phase 7 | 部署上线 | 待定 | ⏳ 待开始 |

---

## 4. 已完成功能清单

### 4.1 后端模块

- [x] 项目结构创建 (pom.xml, application.yml, 主类)
- [x] 统一返回结果 (Result.java)
- [x] 异常处理 (BusinessException, GlobalExceptionHandler)
- [x] 配置类 (MybatisPlusConfig, QuartzConfig, WebMvcConfig, DruidConfig)
- [x] 枚举类 (ErrorCode, FieldTypeEnum)
- [x] 数据库初始化脚本 (schema.sql)
- [x] 实体类 (FtpConfig, ReportConfig, TaskExecution, TaskExecutionLog)
- [x] DTO类 (ColumnMapping, FieldMapping, ReportConfigDTO, TaskQueryDTO)
- [x] Mapper接口和XML
- [x] FTP配置管理模块 (Service, Controller)
- [x] FTP工具类 (FtpUtil) - 含FTPClient参数方法
- [x] 报表配置管理模块 (Service, Controller)
- [x] Excel工具类 (ExcelUtil) - 含readExcel和parseColumnMapping方法
- [x] 任务管理模块 (Service, Controller)
- [x] 日志管理模块 (Service, Controller)
- [x] Druid数据库连接池配置
- [x] Quartz定时任务配置

### 4.2 前端模块

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

### 4.3 核心处理器

- [x] FtpScanJob - FTP扫描定时任务
- [x] DataProcessJob - 数据处理任务
- [x] FtpUtil扩展方法 - listFiles/downloadFile(FTPClient参数)
- [x] ExcelUtil扩展方法 - readExcel/parseColumnMapping
- [x] 项目启动验证 - 前后端通信正常

### 4.4 日志系统

- [x] 操作日志实体类 (OperationLog.java)
- [x] 操作日志服务 (OperationLogService)
- [x] 操作日志切面 (OperationLogAspect)
- [x] 操作日志注解 (OperationLogAnnotation)
- [x] 操作日志控制器 (OperationLogController)
- [x] 文件日志配置 (logback-spring.xml)
- [x] 日志工具类 (LogUtil.java)
- [x] 日志文件服务 (LogFileService)
- [x] 日志文件控制器 (LogFileController)
- [x] 前端操作日志页面 (OperationLog.vue)
- [x] 前端系统日志页面 (SystemLog.vue)

### 4.5 新增模块 (2026-04-02)

- [x] **TableCreatorService** - 自动建表服务接口
- [x] **TableCreatorServiceImpl** - 自动建表实现（基于column_mapping动态建表）
- [x] **CustomIdGenerator** - 自定义ID生成器（解决Docker时钟回拨问题）
- [x] **TaskController.trigger()** - 手动触发任务执行端点

### 4.6 去重机制 (2026-04-03)

- [x] **processed_file表** - 已处理文件追踪表 (report_config_id, file_name)
- [x] **ProcessedFileStatus枚举** - PROCESSED/FAILED/SKIPPED状态
- [x] **ProcessedFile实体** - 已处理文件记录实体
- [x] **ProcessedFileMapper** - MyBatis-Plus Mapper接口
- [x] **ProcessedFileService** - 去重服务接口
- [x] **ProcessedFileServiceImpl** - 去重服务实现（优雅降级）
- [x] **FtpScanJob去重集成** - 自动扫描时跳过已处理文件
- [x] **TaskController去重集成** - 手动触发时检查文件状态

---

## 5. 团队成员

| 角色 | 姓名 | 职责 |
|------|------|------|
| 项目负责人 | - | 项目规划、架构设计 |
| 后端开发 | - | 后端模块开发 |
| 前端开发 | - | 前端页面开发 |

---

## 6. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 | 关联任务ID |
|------|------|----------|--------|------------|
| 2026-04-05 | V1.0 | [新增] 从CLAUDE.md迁移项目上下文 | AI Assistant | H-009 |
