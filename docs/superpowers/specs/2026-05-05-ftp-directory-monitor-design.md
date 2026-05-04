# FTP目录监听与监控模块设计

**日期**: 2026-05-05
**状态**: 已批准

---

## 1. 概述

本文档描述FTP目录自动监听与创建、端到端测试、以及监控模块的实现设计。

## 2. 需求

1. **FTP目录自动监听与创建功能**
   - 监控指定FTP目录下的文件上传事件
   - 报表配置驱动的FTP目录自动创建功能
   - 支持多级目录结构，处理权限问题和重复创建场景

2. **端到端测试实现**
   - 开发上游文件上传模拟功能
   - 构建完整的端到端测试流程
   - 每一步操作都有明确的日志输出

3. **监控模块完善**
   - 报表处理全流程监控
   - 覆盖：目录创建状态、文件上传状态、解析进度、数据处理结果、异常记录
   - 提供REST API接口展示监控数据

---

## 3. 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue)                                │
│   http://localhost:8086  ←→  监控数据展示                         │
└─────────────────────┬───────────────────────────────────────────┘
                      │ REST API
┌─────────────────────▼───────────────────────────────────────────┐
│                      后端 (Spring Boot)                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ ReportConfig │  │  FtpScanJob  │  │ MonitorAPI   │          │
│  │  Controller  │  │              │  │  Controller  │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                 │                   │
│  ┌──────▼─────────────────▼─────────────────▼───────┐          │
│  │              MonitorService (NEW)                │          │
│  │  - 记录各环节状态                                  │          │
│  │  - 提供REST API查询                               │          │
│  └──────────────────────────────────────────────────┘          │
│  ┌──────────────────────────────────────────────────┐          │
│  │          FtpDirectoryService (NEW)                │          │
│  │  - 配置保存时创建目录                               │          │
│  │  - 多级目录支持                                    │          │
│  └──────────────────────────────────────────────────┘          │
└─────────────────────┬───────────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────────┐
│              内置FTP服务器 (Apache FtpServer)                     │
│  端口: 9021  ←  FtpScanJob 监听扫描                              │
│            ←  测试用例上传测试文件                                │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. 核心组件设计

### 4.1 ProcessStep 枚举

定义处理步骤枚举类 `com.report.common.enums.ProcessStep`:

| 步骤 | 说明 |
|------|------|
| `DIR_CREATING` | 目录创建中 |
| `DIR_CREATED` | 目录创建完成 |
| `FILE_UPLOADING` | 文件上传中 |
| `FILE_UPLOADED` | 文件上传完成 |
| `FILE_DETECTED` | 文件被检测到 |
| `PARSING` | 解析中 |
| `PARSED` | 解析完成 |
| `PROCESSING` | 处理中 |
| `PROCESSED` | 处理完成 |
| `EXCEPTION` | 异常 |

### 4.2 ProcessMonitorLog 实体

```java
package com.report.entity;

@TableName("process_monitor_log")
public class ProcessMonitorLog {
    Long id;
    String reportCode;           // 报表编码
    String fileName;             // 文件名
    String step;                 // 当前步骤 (ProcessStep)
    String status;               // RUNNING/SUCCESS/FAILED
    String message;              // 详细信息
    Long durationMs;             // 耗时
    Date startTime;
    Date endTime;
}
```

### 4.3 MonitorService

**职责**:
- 记录各环节监控事件
- 提供查询接口

**方法**:
- `startMonitor(reportCode, fileName, step)` - 开始记录
- `updateMonitor(monitorId, step, status, message)` - 更新状态
- `endMonitor(monitorId, status, message)` - 结束记录
- `getMonitorHistory(reportCode)` - 获取报表处理历史
- `getFileMonitorHistory(fileName)` - 获取文件处理历史
- `getRealtimeProgress(reportCode)` - 获取实时进度

### 4.4 MonitorController

**REST API**:

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/monitor/report/{reportCode}` | 获取报表处理监控历史 |
| GET | `/api/monitor/file/{fileName}` | 获取文件处理监控详情 |
| GET | `/api/monitor/realtime/{reportCode}` | 获取实时处理进度 |

### 4.5 FtpDirectoryService

**职责**:
- 报表配置保存时自动创建FTP目录
- 支持多级目录创建
- 处理目录已存在的幂等性

**方法**:
- `createDirectoriesIfNotExist(scanPath)` - 创建目录（如不存在）
- `createDirectory(directory)` - 创建单个目录
- `directoryExists(path)` - 检查目录是否存在

---

## 5. 处理流程

### 5.1 目录创建流程

```
报表配置保存
    │
    ▼
ReportConfigController.save()
    │
    ▼
FtpDirectoryService.createDirectoriesIfNotExist(scanPath)
    │
    ├──► 检查目录是否存在
    ├──► 不存在则逐级创建
    ├──► 记录监控日志 (DIR_CREATING → DIR_CREATED)
    └──► 返回创建结果
```

### 5.2 文件处理监控流程

```
文件上传完成
    │
    ▼
FtpScanJob 扫描检测
    │
    ▼
记录监控日志 (FILE_DETECTED)
    │
    ▼
MiddlewareEngine.processFile()
    │
    ├──► 记录监控日志 (PARSING)
    ├──► ExcelTransformService.transform()
    ├──► 记录监控日志 (PARSED/PROCESSED/EXCEPTION)
    └──► 记录监控日志 (PROCESSING → PROCESSED)
```

---

## 6. 端到端测试设计

### 6.1 FtpUploadE2eTest 测试类

**测试场景**:
1. 上传测试文件到FTP目录
2. 触发FtpScanJob扫描
3. 验证文件被正确处理
4. 验证监控日志记录完整

**测试数据**:
- 使用 `test120260429.xlsx` 作为测试文件
- 配置测试用报表编码: `TEST_E2E_001`

---

## 7. 数据库表

### process_monitor_log 表

```sql
CREATE TABLE `process_monitor_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `report_code` varchar(64) DEFAULT NULL COMMENT '报表编码',
  `file_name` varchar(256) DEFAULT NULL COMMENT '文件名',
  `step` varchar(32) DEFAULT NULL COMMENT '处理步骤',
  `status` varchar(16) DEFAULT NULL COMMENT '状态',
  `message` text DEFAULT NULL COMMENT '详细信息',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '耗时',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_code` (`report_code`),
  KEY `idx_file_name` (`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 8. 文件清单

| 文件 | 类型 | 说明 |
|------|------|------|
| `com.report.common.enums.ProcessStep` | 枚举 | 处理步骤枚举 |
| `com.report.entity.ProcessMonitorLog` | 实体 | 监控日志实体 |
| `com.report.mapper.ProcessMonitorLogMapper` | Mapper | 监控日志Mapper |
| `com.report.service.MonitorService` | 服务接口 | 监控服务接口 |
| `com.report.service.impl.MonitorServiceImpl` | 服务实现 | 监控服务实现 |
| `com.report.controller.MonitorController` | Controller | 监控REST API |
| `com.report.service.FtpDirectoryService` | 服务接口 | FTP目录服务接口 |
| `com.report.service.impl.FtpDirectoryServiceImpl` | 服务实现 | FTP目录服务实现 |
| `com.report.test.FtpUploadE2eTest` | 测试类 | 端到端测试 |
| `process_monitor_log.sql` | SQL | 建表脚本 |

---

## 9. 错误处理

| 场景 | 处理方式 |
|------|----------|
| FTP目录创建失败 | 记录ERROR日志，抛出业务异常 |
| 目录已存在 | 幂等处理，返回成功 |
| 权限不足 | 记录ERROR日志，提示检查FTP配置 |
| 文件上传失败 | 记录EXCEPTION日志，测试用例失败 |
| 文件处理异常 | 在MiddlewareEngine中捕获并记录 |

---

## 10. 日志输出

每个处理步骤都应输出结构化日志:

```java
log.info("[Monitor] reportCode={}, fileName={}, step={}, status={}, message={}, durationMs={}",
    reportCode, fileName, step, status, message, durationMs);
```
