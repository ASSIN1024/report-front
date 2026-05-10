# 项目学习报告

> **文档版本**: V1.0
> **创建日期**: 2026-05-07
> **维护人**: AI Assistant

---

## 1. 项目概述

### 1.1 项目简介

报表数据处理平台是一个轻量化的数据处理中间件，作为RPA抓取数据上传到FTP服务器和BI报表之间的中间处理环节。

### 1.2 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 2.1.2 |
| ORM | MyBatis-Plus | 3.x |
| 数据库 | MySQL 8.0 / GaussDB | 5.7+ / 5.x |
| 连接池 | Druid | - |
| 定时任务 | Quartz (JDBC集群模式) | - |
| JDK | OpenJDK | 1.8 |
| 前端框架 | Vue | 2.6 |
| UI库 | Element UI | 2.x |
| 构建工具 | Maven | 3.x |

### 1.3 项目架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 2.6)                        │
│  FTP配置 │ 报表配置 │ 任务监控 │ 数据中心 │ 打包管理 │ 系统日志  │
└─────────────────────────────────────────────────────────────┘
                              │ REST API
┌─────────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot 2.1.2)                │
│  Controller │ Service │ Mapper │ Entity │ Job │ Pipeline     │
└─────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  MySQL 8.0    │   │ 内置FTP服务   │   │   Quartz调度   │
│  (开发环境)    │   │ (Apache FtpServer)│   │  (JDBC集群)   │
└───────────────┘   └───────────────┘   └───────────────┘
        │
        │ 生产环境
        ▼
┌───────────────┐
│  GaussDB 5.x  │
└───────────────┘
```

### 1.4 核心功能模块

| 模块 | 功能 | 状态 |
|------|------|------|
| FTP配置管理 | 内置FTP服务配置 | ✅ 已完成 |
| 报表配置管理 | Excel文件解析、字段映射、数据清洗 | ✅ 已完成 |
| 任务执行管理 | 定时扫描、手动触发、状态追踪 | ✅ 已完成 |
| 数据处理Pipeline | 多步数据处理流水线 | ✅ 已完成 |
| 触发器管理 | 数据库分区监听、Pipeline触发 | ✅ 已完成 |
| 打包管理 | 文件打包、消费监控、告警管理 | ✅ 已完成 |
| 数据中心 | 表分层管理、浏览、标记 | ✅ 已完成 |
| 日志管理 | 操作日志、系统日志 | ✅ 已完成 |

---

## 2. 数据库表结构总览

### 2.1 业务表清单

| 序号 | 表名 | 说明 | 核心字段 |
|------|------|------|----------|
| 1 | sys_config | 系统配置表 | config_key, config_value |
| 2 | sys_user | 系统用户表 | username, password |
| 3 | built_in_ftp_config | 内置FTP配置表 | port, username, password, root_directory |
| 4 | report_config | 报表配置表 | report_code, scan_path, column_mapping, output_table |
| 5 | processed_file | 已处理文件记录表 | report_config_id, file_name, status, pt_dt |
| 6 | task_execution | 任务执行记录表 | task_type, status, pipeline_code |
| 7 | task_execution_log | 任务执行日志表 | task_execution_id, log_level, log_message |
| 8 | trigger_config | 触发器配置表 | trigger_code, source_table, pipeline_code |
| 9 | trigger_state_record | 触发器状态持久化表 | trigger_code, triggered, instance_id |
| 10 | trigger_partition_record | 触发器分区记录表 | trigger_code, partition_date, triggered |
| 11 | pipeline_config | 流水线配置表 | pipeline_code, idempotent_mode, status |
| 12 | table_layer_mapping | 表分层映射表 | table_name, table_layer, business_domain |
| 13 | packing_config | 打包配置表 | config_key, config_value |
| 14 | packing_batch | 打包批次表 | batch_no, status, total_size, file_count |
| 15 | alert_record | 告警记录表 | alert_type, alert_level, status |
| 16 | ods_backup | ODS备份记录表 | source_file, pt_dt, table_name |
| 17 | operation_log | 操作日志表 | module, operation_type, operation_desc |
| 18 | process_monitor_log | 进程监控日志表 | report_code, step, status, duration_ms |

### 2.2 Quartz集群表 (11张)

QRTZ_JOB_DETAILS, QRTZ_TRIGGERS, QRTZ_SIMPLE_TRIGGERS, QRTZ_CRON_TRIGGERS, QRTZ_SIMPROP_TRIGGERS, QRTZ_BLOB_TRIGGERS, QRTZ_CALENDARS, QRTZ_PAUSED_TRIGGER_GRPS, QRTZ_FIRED_TRIGGERS, QRTZ_SCHEDULER_STATE, QRTZ_LOCKS

---

## 3. 核心代码模块分析

### 3.1 后端项目结构

```
report-backend/
├── src/main/java/com/report/
│   ├── controller/          # REST API控制器
│   │   ├── FtpConfigController.java (已废弃)
│   │   ├── ReportConfigController.java
│   │   ├── TaskController.java
│   │   ├── TriggerController.java
│   │   ├── PipelineController.java
│   │   ├── PackingController.java
│   │   ├── DataCenterController.java
│   │   └── MonitorController.java
│   │
│   ├── service/            # 业务服务层
│   │   ├── ReportConfigService.java
│   │   ├── TaskService.java
│   │   ├── TriggerService.java
│   │   ├── PackingService.java
│   │   ├── DataCenterService.java
│   │   └── MonitorService.java
│   │
│   ├── job/                # Quartz定时任务
│   │   ├── FtpScanJob.java       # FTP扫描任务
│   │   ├── DataProcessJob.java   # 数据处理任务
│   │   ├── PackingJob.java       # 打包任务
│   │   └── TriggerJob.java       # 触发器轮询任务
│   │
│   ├── pipeline/           # 数据处理流水线
│   │   ├── Pipeline.java
│   │   ├── PipelineStep.java
│   │   ├── AbstractStep.java
│   │   ├── PipelineExecutor.java
│   │   └── step/
│   │       ├── DataCleanseStep.java
│   │       └── DataAggregateStep.java
│   │
│   ├── trigger/            # 触发器模块
│   │   ├── TriggerConfig.java
│   │   ├── TriggerStateManager.java
│   │   ├── DatabaseTriggerStateManager.java
│   │   └── TriggerJob.java
│   │
│   ├── packing/           # 打包模块
│   │   ├── entity/
│   │   ├── service/
│   │   ├── manager/
│   │   └── controller/
│   │
│   ├── ftp/               # 内置FTP模块
│   │   ├── BuiltInFtpConfig.java
│   │   ├── BuiltInFtpConfigMapper.java
│   │   ├── EmbeddedFtpServer.java
│   │   └── FtpAutoStartRunner.java
│   │
│   ├── entity/            # 数据实体
│   │   ├── ReportConfig.java
│   │   ├── TaskExecution.java
│   │   ├── ProcessedFile.java
│   │   ├── OperationLog.java
│   │   ├── SystemConfig.java
│   │   ├── ProcessMonitorLog.java
│   │   └── AlertRecord.java
│   │
│   └── util/              # 工具类
│       ├── FtpUtil.java
│       ├── ExcelUtil.java
│       └── LogUtil.java
│
├── src/main/resources/
│   ├── schema.sql              # MySQL数据库Schema
│   ├── schema-gaussdb.sql      # GaussDB数据库Schema
│   ├── application.yml         # 主配置文件
│   ├── application-dev.yml     # 开发环境配置
│   ├── application-prod.yml    # 生产环境配置
│   └── mapper/                 # MyBatis XML映射
│
└── pom.xml                 # Maven依赖配置
```

### 3.2 前端项目结构

```
src/
├── api/                    # API接口封装
│   ├── request.js         # Axios封装
│   ├── ftpConfig.js       # FTP配置API
│   ├── reportConfig.js    # 报表配置API
│   ├── task.js            # 任务API
│   ├── data.js            # 数据API
│   └── dataCenter.js      # 数据中心API
│
├── views/                 # 页面组件
│   ├── Login.vue          # 登录页
│   ├── FtpConfig.vue      # FTP配置页 (已废弃)
│   ├── ReportList.vue     # 报表列表页
│   ├── ReportConfig.vue   # 报表配置页
│   ├── TaskMonitor.vue    # 任务监控页
│   ├── LogList.vue        # 日志列表页
│   ├── data-center/       # 数据中心模块
│   ├── packing/           # 打包管理模块
│   └── operation-logs/    # 操作日志模块
│
├── router/index.js        # 路由配置
├── store/index.js         # Vuex状态管理
└── App.vue                # 根组件
```

---

## 4. 关键业务流程

### 4.1 数据处理主流程

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ RPA上传 │───▶│ FTP扫描 │───▶│ 数据解析 │───▶│ 字段映射 │───▶│ 数据清洗 │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
                                                                   │
                   ┌───────────────────────────────────────────────┘
                   ▼
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ ODS备份 │◀───│ OSD表   │◀───│ 打包生成 │◀───│ 标准Excel│◀───│ 数据转换 │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └─────────┘
```

### 4.2 触发器Pipeline流程

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 定时轮询TriggerJob  │───▶│ 检查分区数据 │───▶│ 触发Pipeline │
└──────────────┘    └──────────────┘    └──────────────┘
                                                │
                    ┌────────────────────────────┘
                    ▼
        ┌──────────────────────────────────────┐
        │         PipelineExecutor             │
        │  ┌────────────┐  ┌────────────┐     │
        │  │ Step1清洗  │─▶│ Step2聚合  │─────┼──▶ layer_1 / layer_2
        │  └────────────┘  └────────────┘     │
        └──────────────────────────────────────┘
```

---

## 5. 数据库配置

### 5.1 开发环境 (MySQL)

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:33060/report_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root123456
    druid:
      max-active: 50
```

### 5.2 生产环境 (GaussDB)

```yaml
spring:
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://hostname:5432/report_db
    username: root
    password: password
    druid:
      max-active: 50
```

### 5.3 Quartz JDBC集群配置

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always
    properties:
      org:
        quartz:
          scheduler:
            instanceName: ReportScheduler
            instanceId: AUTO
          jobStore:
            isClustered: true
            clusterCheckinInterval: 20000
            misfireThreshold: 60000
```

---

## 6. 项目成熟度评估

| 维度 | 评分 | 说明 |
|------|------|------|
| 架构设计 | ⭐⭐⭐⭐☆ | 分层清晰，模块化良好 |
| 代码质量 | ⭐⭐⭐⭐☆ | 遵循命名规范，注释完整 |
| 文档完善度 | ⭐⭐⭐⭐⭐ | Harness文档体系完整 |
| 测试覆盖 | ⭐⭐⭐☆☆ | 有单元测试和E2E测试 |
| 运维支撑 | ⭐⭐⭐⭐☆ | 启动脚本、日志配置完善 |
| 安全规范 | ⭐⭐⭐⭐☆ | 无硬编码，SQL注入防护 |

---

## 7. 项目当前状态

### 7.1 已完成功能

- ✅ 后端基础框架 (Spring Boot 2.1.2)
- ✅ MyBatis-Plus ORM集成
- ✅ 内置FTP服务 (Apache FtpServer 1.2.0)
- ✅ Quartz JDBC集群模式调度
- ✅ 数据处理Pipeline架构
- ✅ 触发器与分区监听机制
- ✅ 文件打包与消费监控
- ✅ 数据中心表管理
- ✅ 完整前端Vue 2.6应用
- ✅ 操作日志与系统日志
- ✅ Druid数据库连接池

### 7.2 已知问题

- ⚠️ 数据库Schema与代码存在不一致 (见差异报告)
- ⚠️ 部分表缺少update_time字段
- ⚠️ GaussDB版本Schema缺少部分表

### 7.3 下一步计划

- [ ] 修复数据库Schema不一致问题
- [ ] 完善生产环境GaussDB部署文档
- [ ] 补充更多单元测试
- [ ] 性能优化与压力测试

---

## 8. 总结

报表数据处理平台是一个设计良好的轻量化数据中间件，采用了现代化的微服务架构思想（虽然实际是单体部署），具有以下特点：

1. **配置驱动**: 通过数据库配置管理报表，无需硬编码
2. **可追溯**: 历史数据版本化管理(partition_info字段)
3. **高可用**: Quartz集群模式支持多实例部署
4. **可扩展**: Pipeline架构支持多步数据处理

项目整体成熟度较高，已具备生产部署条件，但需先解决数据库Schema一致性问题和完成内网环境适配。
