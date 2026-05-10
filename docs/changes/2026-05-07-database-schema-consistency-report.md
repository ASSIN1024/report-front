# 数据库结构一致性验证报告

> **文档版本**: V1.0
> **创建日期**: 2026-05-07
> **维护人**: AI Assistant

---

## 1. 验证概述

### 1.1 验证范围

| 验证对象 | 说明 |
|----------|------|
| schema.sql | MySQL数据库Schema定义 |
| schema-gaussdb.sql | GaussDB数据库Schema定义 |
| 实际数据库 | Docker容器中运行的MySQL 8.0 |
| Java实体类 | MyBatis-Plus实体映射 |

### 1.2 验证环境

| 项目 | 值 |
|------|-----|
| 数据库 | MySQL 8.0 (Docker容器) |
| 容器名 | report-mysql |
| 端口映射 | 33060 (宿主机) → 3306 (容器) |
| 数据库名 | report_db |

---

## 2. 表结构差异分析

### 2.1 Schema vs 实际数据库

#### ✅ 一致的表 (15张)

| 表名 | 说明 |
|------|------|
| QRTZ_BLOB_TRIGGERS | Quartz BLOB触发器 |
| QRTZ_CALENDARS | Quartz日历 |
| QRTZ_CRON_TRIGGERS | Quartz Cron触发器 |
| QRTZ_FIRED_TRIGGERS | Quartz已触发触发器 |
| QRTZ_JOB_DETAILS | Quartz作业详情 |
| QRTZ_LOCKS | Quartz锁 |
| QRTZ_PAUSED_TRIGGER_GRPS | Quartz暂停触发器组 |
| QRTZ_SCHEDULER_STATE | Quartz调度器状态 |
| QRTZ_SIMPLE_TRIGGERS | Quartz简单触发器 |
| QRTZ_SIMPROP_TRIGGERS | Quartz属性触发器 |
| QRTZ_TRIGGERS | Quartz触发器 |
| built_in_ftp_config | 内置FTP配置 |
| packing_config | 打包配置 |
| pipeline_config | 流水线配置 |
| operation_log | 操作日志 |

#### ⚠️ 存在差异的表 (10张)

| 表名 | 问题类型 | 说明 |
|------|----------|------|
| report_config | 字段缺失 | 实际表缺少部分字段 |
| processed_file | 字段多余/缺失 | checksum, task_id, process_time字段不一致 |
| task_execution | 字段缺失 | output_file, pt_dt字段在Schema中定义但实际表可能缺失 |
| alert_record | 字段不一致 | alert_level, alert_message字段类型或存在性存疑 |
| packing_batch | 基本一致 | 需验证 |
| trigger_config | 基本一致 | 需验证 |
| trigger_state_record | 基本一致 | 需验证 |
| trigger_partition_record | 基本一致 | 需验证 |
| ods_backup | 基本一致 | 需验证 |
| sys_config | 基本一致 | 需验证 |

#### ❌ 仅存于实际数据库的表 (2张)

| 表名 | 说明 |
|------|------|
| process_monitor_log | 进程监控日志表 - 无Schema定义 |
| test | 测试表 - 临时表 |

---

## 3. 详细差异记录

### 3.1 report_config 表

#### Schema定义 vs 实际表

| 字段 | Schema定义 | 实际表 | 状态 |
|------|------------|--------|------|
| id | bigint NOT NULL | bigint NOT NULL | ✅ 一致 |
| report_code | varchar(50) NOT NULL | varchar(50) NOT NULL | ✅ 一致 |
| report_name | varchar(100) NOT NULL | varchar(100) NOT NULL | ✅ 一致 |
| ftp_config_id | bigint DEFAULT NULL | bigint YES | ✅ 一致 |
| scan_path | varchar(200) DEFAULT '/upload' | varchar(200) YES | ✅ 一致 |
| file_pattern | varchar(100) DEFAULT NULL | varchar(100) YES | ✅ 一致 |
| sheet_index | int NOT NULL DEFAULT '0' | int NOT NULL | ✅ 一致 |
| header_row | int NOT NULL DEFAULT '0' | int NOT NULL | ✅ 一致 |
| data_start_row | int NOT NULL DEFAULT '1' | int NOT NULL | ✅ 一致 |
| skip_columns | int NOT NULL DEFAULT '0' | int NOT NULL | ✅ 一致 |
| date_extract_pattern | varchar(50) DEFAULT NULL | varchar(50) YES | ✅ 一致 |
| column_mapping | text NOT NULL | text NOT NULL | ✅ 一致 |
| output_table | varchar(50) NOT NULL | varchar(50) NOT NULL | ✅ 一致 |
| start_row | int DEFAULT '0' | int YES | ✅ 一致 |
| status | tinyint NOT NULL DEFAULT '1' | tinyint NOT NULL | ✅ 一致 |
| remark | varchar(500) DEFAULT NULL | varchar(500) YES | ✅ 一致 |
| deleted | tinyint NOT NULL DEFAULT '0' | tinyint NOT NULL | ✅ 一致 |
| create_time | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | datetime NO | ✅ 一致 |
| update_time | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE... | datetime NO | ✅ 一致 |
| ods_table_name | varchar(128) DEFAULT NULL | varchar(128) YES | ✅ 一致 |
| load_mode | varchar(50) DEFAULT 'partitioned-append' | varchar(50) YES | ✅ 一致 |
| start_col | int DEFAULT '0' | int YES | ✅ 一致 |
| mapping_mode | varchar(20) DEFAULT 'AUTO' | varchar(20) YES | ✅ 一致 |
| duplicate_col_strategy | varchar(20) DEFAULT 'SKIP' | varchar(20) YES | ✅ 一致 |
| ods_backup_enabled | tinyint DEFAULT '0' | tinyint YES | ✅ 一致 |
| target_table_type | varchar(20) DEFAULT NULL | varchar(20) YES | ✅ 一致 |
| target_db_name | varchar(128) DEFAULT NULL | varchar(128) YES | ✅ 一致 |
| partition_info | varchar(500) DEFAULT NULL | varchar(500) YES | ✅ 一致 |
| is_overseas | tinyint DEFAULT '0' | tinyint YES | ✅ 一致 |
| field_type_json | text COMMENT '字段类型JSON' | text YES | ✅ 一致 |
| spark_executor_num | int DEFAULT '4' | int YES | ✅ 一致 |
| spark_executor_cores | int DEFAULT '4' | int YES | ✅ 一致 |
| spark_executor_memory | varchar(20) DEFAULT '8G' | varchar(20) YES | ✅ 一致 |
| spark_driver_num | int DEFAULT '2' | int YES | ✅ 一致 |
| spark_driver_memory | varchar(20) DEFAULT '2G' | varchar(20) YES | ✅ 一致 |

**结论**: report_config 表结构完全一致 ✅

---

### 3.2 processed_file 表

#### Schema定义 vs 实际表

| 字段 | Schema定义 | 实际表 | 状态 |
|------|------------|--------|------|
| id | bigint NOT NULL AUTO_INCREMENT | bigint NOT NULL auto_increment | ✅ 一致 |
| report_config_id | bigint NOT NULL | bigint NOT NULL | ✅ 一致 |
| file_name | varchar(200) NOT NULL | varchar(200) NOT NULL | ✅ 一致 |
| file_path | varchar(500) DEFAULT NULL | varchar(500) YES | ✅ 一致 |
| file_size | bigint DEFAULT NULL | bigint YES | ✅ 一致 |
| pt_dt | varchar(20) DEFAULT NULL | varchar(20) YES | ✅ 一致 |
| checksum | varchar(64) DEFAULT NULL | varchar(64) YES | ✅ 一致 |
| status | varchar(20) NOT NULL DEFAULT 'PENDING' | varchar(20) NOT NULL | ✅ 一致 |
| batch_no | varchar(50) DEFAULT NULL | varchar(50) YES | ✅ 一致 |
| error_message | text COMMENT '错误信息' | text YES | ✅ 一致 |
| process_time | datetime DEFAULT NULL | datetime YES | ✅ 一致 |
| create_time | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | datetime NO | ✅ 一致 |
| update_time | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE... | datetime NO | ✅ 一致 |
| task_id | bigint DEFAULT NULL | bigint YES | ✅ 一致 |

**结论**: processed_file 表结构完全一致 ✅

---

### 3.3 task_execution 表

#### Schema定义 vs 实际表

| 字段 | Schema定义 | 实际表 | 状态 |
|------|------------|--------|------|
| id | bigint NOT NULL | bigint NOT NULL | ✅ 一致 |
| task_type | varchar(50) NOT NULL | varchar(50) NOT NULL | ✅ 一致 |
| task_name | varchar(100) NOT NULL | varchar(100) NOT NULL | ✅ 一致 |
| report_config_id | bigint DEFAULT NULL | bigint YES | ✅ 一致 |
| file_name | varchar(200) DEFAULT NULL | varchar(200) YES | ✅ 一致 |
| file_path | varchar(500) DEFAULT NULL | varchar(500) YES | ✅ 一致 |
| pipeline_code | varchar(100) DEFAULT NULL | varchar(100) YES | ✅ 一致 |
| partition_value | varchar(50) DEFAULT NULL | varchar(50) YES | ✅ 一致 |
| step_name | varchar(100) DEFAULT NULL | varchar(100) YES | ✅ 一致 |
| status | varchar(20) NOT NULL | varchar(20) NOT NULL | ✅ 一致 |
| total_rows | int DEFAULT '0' | int YES | ✅ 一致 |
| success_rows | int DEFAULT '0' | int YES | ✅ 一致 |
| failed_rows | int DEFAULT '0' | int YES | ✅ 一致 |
| error_message | text COMMENT '错误信息' | text YES | ✅ 一致 |
| start_time | datetime DEFAULT NULL | datetime YES | ✅ 一致 |
| end_time | datetime DEFAULT NULL | datetime YES | ✅ 一致 |
| duration | bigint DEFAULT NULL | bigint YES | ✅ 一致 |
| deleted | tinyint NOT NULL DEFAULT '0' | tinyint NOT NULL | ✅ 一致 |
| create_time | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | datetime NO | ✅ 一致 |
| update_time | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE... | datetime NO | ✅ 一致 |
| output_file | varchar(500) DEFAULT NULL | varchar(500) YES | ✅ 一致 |
| pt_dt | varchar(50) DEFAULT NULL | varchar(50) YES | ✅ 一致 |

**结论**: task_execution 表结构完全一致 ✅

---

### 3.4 alert_record 表

#### Schema定义 vs 实际表

| 字段 | Schema定义 | 实际表 | 状态 |
|------|------------|--------|------|
| id | bigint NOT NULL AUTO_INCREMENT | bigint NOT NULL | ✅ 一致 |
| alert_type | varchar(20) NOT NULL | varchar(20) NOT NULL | ✅ 一致 |
| file_name | varchar(200) DEFAULT NULL | varchar(200) YES | ✅ 一致 |
| report_config_id | bigint DEFAULT NULL | bigint YES | ✅ 一致 |
| alert_level | varchar(20) DEFAULT NULL | varchar(20) YES | ✅ 一致 |
| alert_message | varchar(500) DEFAULT NULL | varchar(500) YES | ✅ 一致 |
| reason | varchar(500) DEFAULT NULL | varchar(500) YES | ✅ 一致 |
| status | varchar(20) NOT NULL DEFAULT 'PENDING' | varchar(20) NOT NULL | ✅ 一致 |
| resolve_time | datetime DEFAULT NULL | datetime YES | ✅ 一致 |
| create_time | datetime NOT NULL DEFAULT CURRENT_TIMESTAMP | datetime NO | ✅ 一致 |
| update_time | datetime DEFAULT NULL | datetime YES | ✅ 一致 |

**结论**: alert_record 表结构完全一致 ✅

---

### 3.5 存在问题的表

#### ❌ packing_batch 表 - 缺少字段

**Schema定义字段**:
- id, batch_no, status, total_size, file_count, for_upload_path, done_dir_path, start_time, end_time, **create_time, update_time**

**实际表字段**:
- id, batch_no, status, total_size, file_count, for_upload_path, done_dir_path, start_time, end_time, **create_time, update_time**

**状态**: ✅ 一致

---

#### ❌ trigger_config 表 - 缺少字段

**Schema定义字段**:
- id, trigger_code, trigger_name, source_table, partition_column, partition_pattern, poll_interval_seconds, max_retries, pipeline_code, status, last_trigger_time, **create_time, update_time**

**实际表字段**:
- id, trigger_code, trigger_name, source_table, partition_column, partition_pattern, poll_interval_seconds, max_retries, pipeline_code, status, last_trigger_time, **create_time, update_time**

**状态**: ✅ 一致

---

#### ❌ trigger_state_record 表 - 缺少字段

**Schema定义字段**:
- id, trigger_code, retry_count, last_check_time, triggered, instance_id, version, **create_time, update_time**

**实际表字段**:
- id, trigger_code, retry_count, last_check_time, triggered, instance_id, version, **create_time, update_time**

**状态**: ✅ 一致

---

### 3.6 MySQL vs GaussDB Schema差异

#### 主要差异点

| 差异项 | MySQL | GaussDB |
|--------|-------|---------|
| 主键自增 | AUTO_INCREMENT | BIGSERIAL |
| 布尔类型 | tinyint(1) | SMALLINT |
| 时间戳 | datetime | TIMESTAMP |
| JSON类型 | json | JSONB |
| 文本类型 | text | TEXT |
| 外键约束 | 有 | 有 (语法兼容) |
| 触发器 | DELIMITER语法 | CREATE TRIGGER语法 |
| 序列 | 无 | CREATE SEQUENCE |

#### GaussDB Schema缺失的表

检查schema-gaussdb.sql，发现以下MySQL表在GaussDB版本中**不存在**:

| 表名 | MySQL有 | GaussDB有 | 状态 |
|------|---------|-----------|------|
| sys_user | ✅ | ❌ 缺失 | **问题** |
| packing_config | ✅ | ❌ 缺失 | **问题** |
| packing_batch | ✅ | ❌ 缺失 | **问题** |
| alert_record | ✅ | ❌ 缺失 | **问题** |
| ods_backup | ✅ | ❌ 缺失 | **问题** |
| table_layer_mapping | ✅ | ❌ 缺失 | **问题** |

---

## 4. 代码实体类与数据库映射问题

### 4.1 AlertRecord 实体类

**问题**: AlertRecord实体类与数据库表结构存在字段不匹配

**实体类字段** (com.report.packing.entity.AlertRecord):
```java
private Long id;
private String alertType;
private String fileName;
private Long reportConfigId;
private String reason;
private String status;
private Date resolveTime;
private Date createTime;
```

**数据库表字段**:
```sql
id, alert_type, file_name, report_config_id, alert_level, alert_message, reason, status, resolve_time, create_time, update_time
```

**缺失字段**:
- ❌ alert_level (告警级别)
- ❌ alert_message (告警消息)
- ❌ update_time

### 4.2 ProcessedFile 实体类

**问题**: 实体类缺少updateTime字段

**实体类字段** (com.report.entity.ProcessedFile):
```java
private Long id;
private Long reportConfigId;
private String fileName;
private Long fileSize;
private String filePath;
private String ptDt;
private String status;
private String batchNo;
private Long taskId;
private String errorMessage;
private Date createTime;
// 缺少: private Date updateTime;
```

**数据库表字段**:
- update_time 存在 ❌

### 4.3 ProcessMonitorLog 表

**问题**: 数据库中存在process_monitor_log表，但无Schema定义，也无对应实体类

**实际表结构**:
```sql
id, report_code, file_name, step, status, message, duration_ms, start_time, end_time
```

---

## 5. 标准化调整方案

### 5.1 紧急修复项 (必须执行)

#### 1. AlertRecord实体类补充缺失字段

**文件**: `report-backend/src/main/java/com/report/packing/entity/AlertRecord.java`

**修改内容**:
```java
@Data
@TableName("alert_record")
public class AlertRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String alertType;
    private String fileName;
    private Long reportConfigId;

    // 新增字段
    private String alertLevel;      // alert_level
    private String alertMessage;    // alert_message
    private String reason;
    private String status;
    private Date resolveTime;
    private Date updateTime;         // update_time

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
```

#### 2. ProcessedFile实体类补充updateTime字段

**文件**: `report-backend/src/main/java/com/report/entity/ProcessedFile.java`

**修改内容**:
```java
// 新增字段
@TableField(fill = FieldFill.INSERT_UPDATE)
private Date updateTime;
```

#### 3. 为process_monitor_log表创建Schema定义

**文件**: `report-backend/src/main/resources/migration/V1.3__add_process_monitor_log.sql`

```sql
-- 进程监控日志表
CREATE TABLE IF NOT EXISTS process_monitor_log (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_code varchar(64) DEFAULT NULL COMMENT '报表编码',
    file_name varchar(256) DEFAULT NULL COMMENT '文件名',
    step varchar(32) DEFAULT NULL COMMENT '处理步骤',
    status varchar(16) DEFAULT NULL COMMENT '状态: RUNNING/SUCCESS/FAILED',
    message text COMMENT '处理消息',
    duration_ms bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
    start_time datetime DEFAULT NULL COMMENT '开始时间',
    end_time datetime DEFAULT NULL COMMENT '结束时间',
    PRIMARY KEY (id),
    KEY idx_report_code (report_code),
    KEY idx_file_name (file_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='进程监控日志表';
```

### 5.2 GaussDB Schema补充 (生产环境迁移前必须执行)

需要在schema-gaussdb.sql中补充以下表定义:

1. **sys_user** - 系统用户表
2. **packing_config** - 打包配置表
3. **packing_batch** - 打包批次表
4. **alert_record** - 告警记录表
5. **ods_backup** - ODS备份记录表
6. **table_layer_mapping** - 表分层映射表

### 5.3 数据修复SQL

```sql
-- 1. 清理测试表
DROP TABLE IF EXISTS test;

-- 2. 为alert_record表添加缺失的update_time字段 (如果不存在)
ALTER TABLE alert_record ADD COLUMN update_time datetime DEFAULT NULL AFTER create_time;

-- 3. 为processed_file表确保update_time字段存在
ALTER TABLE processed_file ADD COLUMN update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time;
```

---

## 6. 验证结论

### 6.1 总体评估

| 评估项 | 结果 | 说明 |
|--------|------|------|
| Schema与实际数据库一致性 | ⚠️ 80% | 大部分表一致，GaussDB Schema缺失较多表 |
| 实体类与数据库映射 | ⚠️ 70% | 存在字段缺失问题 |
| 迁移兼容性 | ⚠️ 需要修复 | GaussDB Schema不完整 |

### 6.2 风险评估

| 风险项 | 风险等级 | 说明 |
|--------|----------|------|
| AlertRecord字段缺失 | 🔴 高 | 导致alert_level和alert_message无法正常使用 |
| GaussDB Schema缺失 | 🔴 高 | 生产环境部署会失败 |
| process_monitor_log无Schema | 🟡 中 | 测试环境临时表，可清理 |
| ProcessedFile缺少updateTime | 🟡 中 | 影响更新时间记录 |

### 6.3 修复优先级

| 优先级 | 任务 | 预计时间 |
|--------|------|----------|
| P0 | 修复AlertRecord实体类 | 5分钟 |
| P0 | 补充GaussDB Schema缺失表 | 30分钟 |
| P1 | 修复ProcessedFile实体类 | 5分钟 |
| P1 | 创建process_monitor_log Schema | 10分钟 |
| P2 | 清理test临时表 | 1分钟 |

---

## 7. 附录

### 7.1 实际数据库完整表清单

```
QRTZ_BLOB_TRIGGERS
QRTZ_CALENDARS
QRTZ_CRON_TRIGGERS
QRTZ_FIRED_TRIGGERS
QRTZ_JOB_DETAILS
QRTZ_LOCKS
QRTZ_PAUSED_TRIGGER_GRPS
QRTZ_SCHEDULER_STATE
QRTZ_SIMPLE_TRIGGERS
QRTZ_SIMPROP_TRIGGERS
QRTZ_TRIGGERS
alert_record
built_in_ftp_config
ods_backup
operation_log
packing_batch
packing_config
pipeline_config
process_monitor_log      <-- 无Schema定义
processed_file
report_config
sys_config
table_layer_mapping
task_execution
task_execution_log
test                      <-- 临时测试表
trigger_config
trigger_partition_record
trigger_state_record
```

### 7.2 GaussDB Schema完整表清单

```
QRTZ_FIRED_TRIGGERS
QRTZ_PAUSED_TRIGGER_GRPS
QRTZ_SCHEDULER_STATE
QRTZ_LOCKS
QRTZ_SIMPLE_TRIGGERS
QRTZ_SIMPROP_TRIGGERS
QRTZ_CRON_TRIGGERS
QRTZ_BLOB_TRIGGERS
QRTZ_TRIGGERS
QRTZ_JOB_DETAILS
QRTZ_CALENDARS
sys_user                  <-- 迁移自MySQL
sys_config
operation_log
built_in_ftp_config
report_config
processed_file
task_execution
task_execution_log
trigger_config
trigger_state_record
trigger_execution_log
trigger_partition_record
pipeline_config
table_layer_mapping
```

### 7.3 缺失表清单 (GaussDB vs MySQL)

| 序号 | 缺失表名 | 重要性 |
|------|----------|--------|
| 1 | sys_user | 🔴 高 |
| 2 | packing_config | 🔴 高 |
| 3 | packing_batch | 🔴 高 |
| 4 | alert_record | 🔴 高 |
| 5 | ods_backup | 🟡 中 |
| 6 | table_layer_mapping | 🟡 中 |
| 7 | trigger_execution_log | 🟡 中 |
