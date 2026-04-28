# FTP 报表数据转换中间件 - 技术设计文档

**状态**: 已确认
**版本**: 1.0
**创建日期**: 2026-04-28
**基于PRD**: prd-data-pipeline-middleware.md

---

## 1. 系统概述

### 1.1 背景

公司内部存在两条 RPA 数据链路：上游 RPA 从各个业务系统爬取报表数据并上传至 FTP，下游 RPA 定期从 FTP 获取压缩包并通过 WebUI 逐条上传入库。两段 RPA 之间缺乏一个自动化的数据转换与打包中间层。

### 1.2 核心痛点

1. **非标表格解析困难**：上游报表多为"中国风"表格——合并单元格、多行表头、中文列名
2. **缺失自动化转换**：无标准化的字段映射、数据清洗和打包能力
3. **数据传输低效**：下游 RPA 通过 WebUI 逐文件上传，散乱文件浪费大量等待时间
4. **无监控与追溯**：字段变更、解析失败难以感知

### 1.3 系统定位

**FTP 报表数据转换中间件**——位于上游 RPA（数据采集）和下游 RPA（数据入库）之间的自动化管道。

---

## 2. 核心流程设计

### 2.1 整体流程

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           打包业务流程                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                │
│  │  FTP扫描器   │ → │  Excel解析器  │ → │  字段映射器  │                │
│  │ (全量FTP)   │    │ (中国风表格) │    │ (双模式)     │                │
│  └─────────────┘    └─────────────┘    └─────────────┘                │
│                                              │                          │
│                                              ▼                          │
│                          ┌─────────────────────────────────┐           │
│                          │  打包管理器 (PackingManager)     │           │
│                          │  - 检查 outputs.zip 是否存在     │           │
│                          │  - 存在则新文件排队等待          │           │
│                          │  - 按大小限制分批                │           │
│                          └─────────────────────────────────┘           │
│                                              │                          │
│                                              ▼                          │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐                │
│  │  Done目录    │ ← │  消费监控器   │ ← │  批量打包    │                │
│  │ outputs_*_  │    │ (轮询检测)   │    │ outputs.zip │                │
│  │ done.zip    │    │             │    │             │                │
│  └─────────────┘    └─────────────┘    └─────────────┘                │
│                                              │                          │
│                                              ▼                          │
│                          ┌─────────────────────────────────┐           │
│                          │  消费监控器 (ConsumptionWatcher) │           │
│                          │  - 每隔X秒检测 outputs.zip      │           │
│                          │  - 不存在则消费完成              │           │
│                          │  - 触发下一批打包                │           │
│                          └─────────────────────────────────┘           │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 打包触发机制

**触发方式**：
1. **定时触发**：可配置扫描间隔（如每5分钟）
2. **手动触发**：用于调试，可立即执行扫描

**触发条件**：
- 扫描周期内发现新文件
- 新文件达到打包条件（已解析、已处理）

### 2.3 消费监控与轮转

**轮转机制**：
1. 系统检测 `outputs.zip` 是否存在
2. 若存在，等待下游消费（文件消失 = 消费完成）
3. 消费完成后，将备份移动到 Done 目录（`outputs_timestamp_done.zip`）
4. 若不存在，立即上传新打包的 `outputs.zip`
5. 新文件在等待期间排队

---

## 3. 压缩包设计

### 3.1 压缩包命名

| 目录 | 文件名 | 说明 |
|------|--------|------|
| 上传目录 | `outputs.zip` | 固定文件名，始终只有一个 |
| Done目录 | `outputs_20260428_143000_done.zip` | 消费完成后的备份 |

### 3.2 压缩包内容结构

```
outputs.zip
├── report1_processed.xlsx    # 处理后的Excel文件
├── report2_processed.xlsx
├── ...
├── config.xlsx              # 配置表
└── metadata.json             # 批次元数据
```

### 3.3 配置表结构（config.xlsx）

根据 `informationTemplate.xlsx` 模板：

| 列名 | 说明 | 数据类型 | 示例 |
|------|------|----------|------|
| 序号 | 报表序号 | INT | 1, 2, 3... |
| 文件名 | 处理后的文件名 | VARCHAR | report1.xlsx |
| 目标表类型 | hive/mpp | VARCHAR | hive |
| 目标库名 | 目标数据库 | VARCHAR | bdsp_mpvs_t1 |
| 目标表名 | bi_前缀自动拼接 | VARCHAR | test1_hive |
| 是否境外 | 0-否, 1-是 | INT | 0 |
| 字段类型列表 | JSON格式 | JSON | {"id":"int","name":"string"} |
| 数据载入模式 | partitioned-append等 | VARCHAR | partitioned-append |
| 分区信息 | pt_dt='2022-01-01' | VARCHAR | pt_dt='2022-01-01' |
| Spark运行资源 | executor数量 | INT | 4 |
| Spark运行资源 | executor核数 | INT | 4 |
| Spark运行资源 | executor内存 | VARCHAR | 8G |
| Spark运行资源 | driver数量 | INT | 2 |
| Spark运行资源 | driver内存 | VARCHAR | 2G |

---

## 4. 数据库设计

### 4.1 新增表

#### 4.1.1 packing_config（打包配置表）

```sql
CREATE TABLE packing_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) DEFAULT NULL COMMENT '配置值',
    config_type VARCHAR(50) DEFAULT NULL COMMENT '配置类型',
    description VARCHAR(200) DEFAULT NULL COMMENT '描述',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包配置表';
```

**配置项**：

| 配置键 | 说明 | 默认值 |
|--------|------|--------|
| max_package_size | 最大包大小(字节) | 209715200 (200MB) |
| upload_dir | 上传目录 | /data/ftp-root/for-upload |
| done_dir | 完成目录 | /data/ftp-root/done |
| fixed_filename | 固定文件名 | outputs.zip |
| polling_interval | 消费轮询间隔(秒) | 30 |
| scan_interval | 扫描间隔(秒) | 300 |

#### 4.1.2 packing_batch（打包批次表）

```sql
CREATE TABLE packing_batch (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    batch_no VARCHAR(50) NOT NULL COMMENT '批次号',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待打包, UPLOADING-上传中, CONSUMING-消费中, DONE-已完成',
    total_size BIGINT DEFAULT 0 COMMENT '总大小(字节)',
    file_count INT DEFAULT 0 COMMENT '文件数量',
    for_upload_path VARCHAR(500) DEFAULT NULL COMMENT '上传路径',
    done_dir_path VARCHAR(500) DEFAULT NULL COMMENT 'Done目录路径',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_no (batch_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包批次表';
```

#### 4.1.3 alert_record（告警记录表）

```sql
CREATE TABLE alert_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    alert_type VARCHAR(20) NOT NULL COMMENT '告警类型: PARSE_ERROR-解析错误, MAPPING_ERROR-映射错误, PACKING_ERROR-打包错误, CONSUMPTION_TIMEOUT-消费超时',
    file_name VARCHAR(200) DEFAULT NULL COMMENT '相关文件名',
    report_config_id BIGINT DEFAULT NULL COMMENT '关联报表配置ID',
    reason VARCHAR(500) DEFAULT NULL COMMENT '告警原因',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待处理, RESOLVED-已解决, IGNORED-已忽略',
    resolve_time DATETIME DEFAULT NULL COMMENT '解决时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_alert_type (alert_type),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';
```

### 4.2 扩展现有表

#### 4.2.1 report_config 扩展

新增字段用于配置表的完整信息：

| 字段 | 类型 | 说明 |
|------|------|------|
| target_table_type | VARCHAR(20) | 目标表类型: hive/mpp |
| target_db_name | VARCHAR(128) | 目标库名 |
| is_overseas | TINYINT | 是否境外: 0-否, 1-是 |
| field_type_json | TEXT | 字段类型JSON |
| spark_executor_num | INT | Spark executor数量 |
| spark_executor_cores | INT | Spark executor核数 |
| spark_executor_memory | VARCHAR(20) | Spark executor内存 |
| spark_driver_num | INT | Spark driver数量 |
| spark_driver_memory | VARCHAR(20) | Spark driver内存 |

#### 4.2.2 ftp_config 确认

现有字段已满足需求：
- `scan_path`: 扫描路径
- `staging_dir`: 暂存目录
- `for_upload_dir`: 上传目录
- `archive_dir`: 归档目录
- `error_dir`: 错误目录

---

## 5. 核心组件设计

### 5.1 组件架构

```
com.report.packing
├── PackingConfig.java           # 打包配置实体
├── PackingBatch.java           # 批次实体
├── AlertRecord.java            # 告警实体
├── PackingConfigMapper.java    # Mapper
├── PackingBatchMapper.java     # Mapper
├── AlertRecordMapper.java      # Mapper
├── PackingConfigService.java   # 服务接口
├── PackingBatchService.java    # 服务接口
├── PackingService.java         # 核心打包服务
├── PackingManager.java         # 打包管理器（协调）
├── ConsumptionWatcher.java     # 消费监控器
├── ConfigTableGenerator.java    # 配置表生成器
└── PackingJob.java             # 定时打包任务
```

### 5.2 核心服务接口

#### PackingService

```java
public interface PackingService {
    /**
     * 执行打包
     * @param files 待打包文件列表
     * @return 打包后的批次号
     */
    String pack(List<ProcessedFile> files);

    /**
     * 上传打包结果到FTP
     * @param batchNo 批次号
     */
    void upload(String batchNo);

    /**
     * 检查是否可以上传
     * @return true 可以上传，false 需等待
     */
    boolean canUpload();

    /**
     * 获取当前上传状态
     * @return true 正在被消费，false 可上传
     */
    boolean isBeingConsumed();
}
```

#### ConsumptionWatcher

```java
public interface ConsumptionWatcher {
    /**
     * 启动消费监控
     */
    void start();

    /**
     * 停止消费监控
     */
    void stop();

    /**
     * 检查是否消费完成
     * @return true 消费完成
     */
    boolean isConsumed();
}
```

---

## 6. Excel解析器设计

### 6.1 中国风表格解析策略

**挑战**：
- 合并单元格
- 多行表头
- 中文列名
- 非A1起始

**解析策略**：

1. **起始行列偏移**：从配置的 startRow, startCol 开始解析
2. **表头行识别**：headerRow 指定表头位置
3. **列名映射**：
   - 模式A（按列名）：匹配源列名到目标列名
   - 模式B（按列序号）：按列顺序映射
4. **重复列处理**：配置策略（SKIP/APPEND_NUMBER）
5. **合并单元格**：使用 POI 的 cellRangeAddress 处理

### 6.2 字段映射器

```java
public interface FieldMapper {
    /**
     * 执行字段映射
     * @param sourceData 源数据
     * @param mappingConfig 映射配置
     * @return 映射后的数据
     */
    List<Map<String, Object>> map(List<Map<String, Object>> sourceData, ColumnMappingConfig mappingConfig);

    /**
     * 校验映射完整性
     * @param sourceHeaders 源表头
     * @param mappingConfig 映射配置
     * @return 缺失字段列表
     */
    List<String> validateMapping(List<String> sourceHeaders, ColumnMappingConfig mappingConfig);
}
```

---

## 7. 打包流程时序

### 7.1 正常打包流程

```
1. PackingJob 定时触发
       │
       ▼
2. FtpScanJob 扫描所有FTP（新文件检测）
       │
       ▼
3. ExcelParser 解析每个文件
       │
       ▼
4. FieldMapper 执行字段映射
       │
       ▼
5. DataCleaner 应用清洗规则
       │
       ▼
6. ProcessedFile 记录到数据库
       │
       ▼
7. PackingManager 检查 canUpload()
       │
       ├─→ false（正在被消费）→ 排队等待
       │
       ▼
8. PackingService.pack() 生成 outputs.zip
       │
       ▼
9. ConsumptionWatcher.start() 开始监控
       │
       ▼
10. FtpUtil.upload() 上传到上传目录
       │
       ▼
11. 等待下游消费（轮询检测 outputs.zip 是否存在）
       │
       ▼
12. 文件消失 → 消费完成
       │
       ▼
13. 移动到 Done 目录：outputs_timestamp_done.zip
       │
       ▼
14. 触发下一批打包（检查队列）
```

---

## 8. 配置管理

### 8.1 打包配置接口

| 接口 | 方法 | 说明 |
|------|------|------|
| GET /api/packing/config | GET | 获取打包配置 |
| PUT /api/packing/config | PUT | 更新打包配置 |

### 8.2 批次查询接口

| 接口 | 方法 | 说明 |
|------|------|------|
| GET /api/packing/batch | GET | 获取批次列表 |
| GET /api/packing/batch/{batchNo} | GET | 获取批次详情 |
| POST /api/packing/trigger | POST | 手动触发打包 |

### 8.3 告警接口

| 接口 | 方法 | 说明 |
|------|------|------|
| GET /api/packing/alerts | GET | 获取告警列表 |
| PUT /api/packing/alerts/{id}/resolve | PUT | 标记告警已解决 |

---

## 9. 技术实现要点

### 9.1 文件大小计算

```java
// 计算压缩包预计大小
long estimatedSize = files.stream()
    .mapToLong(File::length)
    .sum();

// 按配置大小限制拆分
List<List<ProcessedFile>> batches = partitionBySize(files, maxPackageSize);
```

### 9.2 消费监控

```java
// 轮询检测文件是否存在
while (watcher.isRunning()) {
    if (!ftpClient.fileExists(uploadDir + "/outputs.zip")) {
        // 消费完成
        handleConsumptionComplete();
        break;
    }
    Thread.sleep(pollingInterval * 1000);
}
```

### 9.3 Done目录移动

```java
// 消费完成后，移动文件并改名
String doneFileName = String.format("outputs_%s_done.zip",
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
String donePath = doneDir + "/" + doneFileName;
ftpClient.move(uploadPath, donePath);
```

---

## 10. 现有代码复用

### 10.1 复用组件

| 组件 | 复用方式 |
|------|----------|
| FtpConfig | 复用，已有scanPath, filePattern等 |
| ReportConfig | 扩展，新增字段 |
| BuiltInFtpConfig | 复用，内置FTP服务 |
| EmbeddedFtpServer | 复用，FTP操作 |
| ColumnMappingValidator | 复用，字段映射校验 |
| DataCleaning | 复用，清洗规则 |

### 10.2 扩展点

| 现有组件 | 扩展方式 |
|----------|----------|
| FtpScanJob | 扩展支持批量收集 |
| ReportConfig | 新增配置表相关字段 |
| FtpConfig | 确认目录配置完整 |

---

## 11. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 复杂合并单元格解析失败 | 高 | Sprint 0 验证POI解析能力 |
| 大文件OOM | 中 | EasyExcel流式读取 |
| FTP连接中断 | 高 | 异常时源文件不归档，保留原位 |
| 打包过程中文件被消费 | 中 | 先检测再上传，原子操作 |

---

## 12. 实施计划

| Phase | 内容 | 优先级 |
|-------|------|--------|
| Phase 1 | 核心打包服务（数据库、PackService、ConfigTableGenerator） | P0 |
| Phase 2 | FTP扫描与集成（扩展FtpScanJob、文件队列） | P0 |
| Phase 3 | 消费监控与轮转（ConsumptionWatcher、归档） | P0 |
| Phase 4 | 管理界面（打包配置、批次监控、告警） | P1 |

---

**文档状态**：已确认
**下次审查**：实施前
