# FTP 报表数据转换中间件 - 技术设计文档

**Status**: Approved
**Author**: Alex (PM) + AI Assistant
**Last Updated**: 2026-04-26
**Version**: 1.0
**Stakeholders**: Adam (Sponsor), 开发团队

---

## 1. 系统定位

**FTP 报表数据转换中间件** — 位于上游 RPA（数据采集）和下游 RPA（数据入库）之间的自动化管道。

核心职责：
- FTP 新文件检测
- 中国风表格解析
- 字段映射 + 数据清洗
- 标准化 Excel 生成
- 批量打包（ZIP + 配置表）
- 投递到待上传目录
- 源文件归档

**技术栈**：Spring Boot 2.1.2 + Vue 2.6 + MySQL + Apache POI/EasyExcel

**技术决策**：保留 Vue 2 + Element UI（内部工具，升级收益低）；保留内置 FTP（开发测试用）

---

## 2. 核心数据流

```
上游 RPA ──FTP上传──> 监控目录
                              │
                         FtpScanJob（定时扫描）
                              │
                    ┌─────────┴─────────┐
                    │                   │
              检查 processed_       新文件加入
              file 防重             待处理队列
                    │                   │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼─────────┐
                    │ ExcelTransform    │
                    │ Service           │
                    │ ──────────────── │
                    │ 1. 解析 Excel     │
                    │ 2. 字段映射       │
                    │ 3. 数据清洗       │
                    │ 4. 生成标准 Excel │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼─────────┐
                    │  ODS 备份写入      │ ← 可选
                    │ （可选功能）       │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼─────────┐
                    │ PackagingService │
                    │ 1. 打包 ZIP       │
                    │ 2. 生成配置表     │
                    └─────────┬─────────┘
                              │
                    ┌─────────▼─────────┐
                    │   /staging/       │ ← 批次 ZIP 队列
                    │   目录             │
                    └─────────┬─────────┘
                              │
              检查 /for-upload/ 目录状态
                    │                   │
              ┌─────┴─────┐             │
              │           │             │
           为空        不为空            │
         从 staging   等待 RPA          │
         取最早批次    消费完             │
         移入                         │
         /for-upload/                  │
              │                        │
    ┌─────────▼─────────┐              │
    │  /for-upload/     │              │
    │  目录（单文件）     │              │
    └─────────┬─────────┘              │
              │                        │
              ▼                        │
         RPA 下载 ZIP                   │
              │                        │
              ▼                        │
         RPA 上传到目标系统              │
              │                        │
              ▼                        │
         RPA 删除 ZIP                   │
         （/for-upload/ 变空）           │
              │                        │
         回到步骤「检查 for-upload」      │
```

---

## 3. FTP 目录结构

```
FTP 根目录（由 FTP 配置的 output_dir 指定）
├── {监控目录}/              ← 上游 RPA 上传原始文件（scan_path）
├── staging/                ← 中间件打包暂存（FIFO 队列）
│   ├── batch_001.zip
│   ├── batch_002.zip
│   └── ...
└── for-upload/             ← 供下游 RPA 获取（单文件）
    └── output.zip           ← 固定文件名
```

**目录派生规则**：
- 假设 FTP 配置的 `output_dir` = `/upload`
- `staging_dir` = `{output_dir}/staging`（可配置覆盖）
- `for_upload_dir` = `{output_dir}/for-upload`（可配置覆盖）

**默认行为**：
- 若未配置 `staging_dir`/`for_upload_dir`，自动派生为 `{output_dir}/staging` 和 `{output_dir}/for-upload`
- 若未配置 `archive_dir`，默认归档到 `{output_dir}/archive`
- 若未配置 `error_dir`，默认错误文件到 `{output_dir}/error`

---

## 4. 批次与幂等性

### 4.1 批次定义

- **不预设批次号**：扫描周期内所有新文件视为同一批次
- **ZIP 打包时机**：有新文件处理时立即打包（不累积）
- **staging/ 作为队列**：每个处理完毕的文件打包成一个 ZIP，追加到 staging/

### 4.2 幂等性保证

| 层级 | 保证机制 |
|------|----------|
| 文件级别 | `processed_file` 表 (report_config_id, file_name) 唯一索引，处理前检查，存在则跳过 |
| ZIP 投递 | 检查 for-upload/ 为空后才从 staging/ 取一个 ZIP 移入 |

### 4.3 消费流程

```
中间件扫描周期
  │
  ├── 检查 /for-upload/ 是否为空
  │     ├── 不为空 → 跳过（等待 RPA 消费）
  │     └── 为空
  │           ├── 检查 /staging/ 是否有 ZIP
  │           │     ├── 有 → 取最早的 ZIP → 移入 /for-upload/
  │           │     └── 没有 → 跳过（暂无批次）
  │
  └── RPA 消费流程
        ├── 从 /for-upload/ 下载 output.zip
        ├── 上传到目标系统
        └── 删除 /for-upload/output.zip
```

### 4.4 ZIP 文件命名

| 阶段 | 文件名 | 说明 |
|------|--------|------|
| 中间件打包 | `batch_{timestamp}_{uuid8}.zip` | 唯一命名，避免冲突 |
| 移入 for-upload | 统一命名为 `output.zip` | 固定文件名，RPA 方便获取 |

---

## 5. 后端架构

### 5.1 包结构

```
com.report/
├── controller/
│   ├── AuthController               # [保留]
│   ├── FtpConfigController         # [重构] 增加 output/staging/for-upload 目录配置
│   ├── ReportConfigController      # [重构] 增加起始行列、映射模式等
│   ├── TaskController              # [重构] 增加 batchId, fileName, outputFile, ptDt
│   ├── AlertController             # [新增] 告警管理
│   ├── BatchController             # [新增] 批次查询
│   ├── LogController               # [保留]
│   ├── LogFileController           # [保留]
│   ├── OperationLogController      # [保留]
│   └── SystemConfigController      # [保留]
│
├── service/
│   ├── FtpConfigService            # [保留]
│   ├── ReportConfigService         # [重构] 报表配置
│   ├── TaskService                 # [重构] 处理记录
│   ├── AlertService                # [新增] 告警服务
│   ├── BatchService                # [新增] 批次服务（staging/for-upload 队列管理）
│   ├── ExcelTransformService       # [重构] 核心转换（解析+映射+清洗+标准Excel）
│   ├── OdsBackupService            # [新增] ODS 备份写入
│   ├── PackagingService            # [新增] ZIP 打包 + 配置表生成
│   ├── FtpUploadService            # [新增] staging → for-upload 移动
│   ├── ArchiveService              # [新增] 源文件归档
│   ├── ProcessedFileService        # [保留]
│   └── ...
│
├── engine/
│   └── MiddlewareEngine            # [重构] 核心引擎编排
│
├── job/
│   └── FtpScanJob                  # [重构] 定时扫描 → 调用 MiddlewareEngine
│
├── entity/
│   ├── FtpConfig                   # [重构] 增加字段
│   ├── ReportConfig                # [重构] 增加字段
│   ├── TaskExecution              # [重构] 增加字段
│   ├── ProcessedFile              # [保留]
│   ├── AlertRecord                 # [新增]
│   ├── BatchRecord                # [新增] 批次记录
│   ├── SysUser                    # [保留]
│   ├── OperationLog               # [保留]
│   └── SystemConfig               # [保留]
│   # 移除：Pipeline/Trigger/DWD/DWS/ADS 相关实体
│
├── mapper/
│   ├── FtpConfigMapper             # [保留]
│   ├── ReportConfigMapper         # [保留]
│   ├── TaskExecutionMapper        # [保留]
│   ├── AlertRecordMapper          # [新增]
│   ├── BatchRecordMapper          # [新增]
│   └── ...                        # 移除 DWD/DWS/ADS/Trigger Mapper
│
├── ftp/
│   ├── EmbeddedFtpServer           # [保留] 开发测试用
│   └── FtpAutoStartRunner         # [保留]
│
├── pipeline/                       # [移除] 旧架构
│   # 所有内容移除
│
├── trigger/                        # [移除] 旧架构
│   # 所有内容移除
│
└── util/
    ├── ExcelUtil                   # [重构] 增强解析
    ├── FileNameDateExtractor       # [保留]
    ├── StandardExcelWriter         # [新增] 标准化 Excel 输出
    ├── ConfigExcelWriter           # [新增] 配置表生成
    └── ZipPackager                 # [新增] ZIP 打包工具
```

### 5.2 MiddlewareEngine 核心流程

```java
public class MiddlewareEngine {

    @Autowired private ExcelTransformService excelTransformService;
    @Autowired private OdsBackupService odsBackupService;
    @Autowired private PackagingService packagingService;
    @Autowired private FtpUploadService ftpUploadService;
    @Autowired private ArchiveService archiveService;
    @Autowired private AlertService alertService;
    @Autowired private BatchService batchService;

    public void processCycle() {
        // 1. 扫描 FTP 监控目录，获取新文件
        List<MatchedFile> newFiles = scanNewFiles();

        // 2. 处理每个新文件
        for (MatchedFile file : newFiles) {
            try {
                ProcessResult result = excelTransformService.transform(file);

                if (result.isSuccess()) {
                    // 3. ODS 备份（可选）
                    if (config.isOdsBackupEnabled()) {
                        odsBackupService.backup(result);
                    }

                    // 4. 打包到 staging
                    packagingService.packageToStaging(result);

                    // 5. 记录已处理
                    processedFileService.markAsProcessed(file);
                } else {
                    // 6. 告警
                    alertService.createAlert(file, result.getErrorMessage(), AlertLevel.ERROR);
                }
            } catch (Exception e) {
                alertService.createAlert(file, e.getMessage(), AlertLevel.ERROR);
                archiveService.archiveToError(file);
            }
        }

        // 7. 检查 for-upload 目录，投递 ZIP
        batchService.deliverZipIfReady();
    }
}
```

### 5.3 BatchService 投递逻辑

```java
public class BatchService {

    public void deliverZipIfReady() {
        String stagingPath = ftpConfig.getStagingDir();
        String forUploadPath = ftpConfig.getForUploadDir();

        // 检查 for-upload 是否为空
        if (!ftpService.isDirectoryEmpty(forUploadPath)) {
            return; // 等待 RPA 消费
        }

        // 获取 staging 中最早的 ZIP
        String earliestZip = ftpService.getEarliestFile(stagingPath);
        if (earliestZip == null) {
            return; // 暂无批次
        }

        // 移动到 for-upload（重命名为 output.zip）
        ftpService.rename(earliestZip, forUploadPath + "/output.zip");
    }
}
```

---

## 6. 数据模型

### 6.1 数据库表变更

#### 新增表

**alert_record（告警记录）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| report_config_id | BIGINT | 关联报表配置 |
| file_name | VARCHAR(255) | 文件名 |
| alert_level | VARCHAR(20) | ERROR / WARNING |
| alert_type | VARCHAR(50) | MAPPING_FAILED / FIELD_MISSING / PARSE_ERROR / FTP_ERROR |
| alert_message | TEXT | 告警详情 |
| status | VARCHAR(20) | OPEN / RESOLVED |
| resolved_by | VARCHAR(50) | 解决人 |
| resolved_at | DATETIME | 解决时间 |
| created_at | DATETIME | 创建时间 |

**batch_record（批次记录）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 主键 |
| batch_code | VARCHAR(64) | 批次编码 |
| ftp_config_id | BIGINT | 关联 FTP 配置 |
| zip_file_name | VARCHAR(255) | ZIP 文件名 |
| file_count | INT | 文件数量 |
| total_size | BIGINT | ZIP 大小 |
| status | VARCHAR(20) | CREATED / DELIVERED / CONSUMED |
| delivered_at | DATETIME | 投递时间 |
| consumed_at | DATETIME | 消费时间 |
| created_at | DATETIME | 创建时间 |

#### 修改表

**report_config（报表配置）**

| 新增字段 | 类型 | 说明 |
|----------|------|------|
| start_row | INT | 起始行号偏移（默认 1） |
| start_col | INT | 起始列号偏移（默认 1） |
| mapping_mode | VARCHAR(20) | BY_NAME / BY_INDEX / DUAL（默认 DUAL） |
| duplicate_col_strategy | VARCHAR(20) | AUTO_SUFFIX / BY_INDEX（默认 AUTO_SUFFIX） |
| ods_backup_enabled | TINYINT | 是否 ODS 备份（默认 0） |
| ods_table_name | VARCHAR(128) | ODS 备份表名 |

**task_execution（任务执行记录）**

| 新增字段 | 类型 | 说明 |
|----------|------|------|
| file_name | VARCHAR(255) | 源文件名 |
| output_file | VARCHAR(255) | 输出标准 Excel 文件名 |
| pt_dt | VARCHAR(20) | 分区日期 |

**ftp_config（FTP 配置）**

| 新增字段 | 类型 | 说明 |
|----------|------|------|
| staging_dir | VARCHAR(512) | 暂存目录（默认 {root}/staging） |
| for_upload_dir | VARCHAR(512) | 待上传目录（默认 {root}/for-upload） |
| archive_dir | VARCHAR(512) | 归档目录 |
| error_dir | VARCHAR(512) | 错误文件目录 |

#### 移除表

- table_layer_mapping
- trigger_config, trigger_state_record, trigger_partition_record, trigger_execution_log
- 所有 DWD/DWS/ADS 数据仓库表（dwd_*, dws_*, ads_*, layer_*）
- Pipeline, PipelineStep, MyBatisPlusAbstractStep 相关表

#### 保留表

- ftp_config（增强）
- report_config（增强）
- task_execution（增强）
- task_execution_log
- processed_file
- sys_config
- operation_log
- sys_user
- built_in_ftp_config
- ODS 表（osd_sales, osd_company_deposit, test_flow）

### 6.2 ColumnMapping JSON 结构

```json
{
  "mappings": [
    {
      "mode": "BY_NAME",
      "sourceName": "销售额",
      "targetName": "sale_amt",
      "sourceIndex": null
    },
    {
      "mode": "BY_INDEX",
      "sourceName": null,
      "sourceIndex": 5,
      "targetName": "remark"
    }
  ],
  "cleanRules": [
    {
      "targetColumn": "sale_amt",
      "pattern": "-",
      "replacement": "0"
    }
  ]
}
```

### 6.3 配置 Excel 表字段（ZIP 内）

| 字段 | 说明 | 类型 | 示例 |
|------|------|------|------|
| source_file | 源文件名 | VARCHAR | `sales_report.xlsx` |
| db_name | 目标数据库名 | VARCHAR | `ods_layer` |
| table_name | 目标表名 | VARCHAR | `ods_sales_daily` |
| field_mapping | 字段映射 JSON | JSON/TEXT | `{"销售额":"sale_amt"}` |
| is_partitioned | 是否分区 | BOOLEAN | `true` |
| partition_field | 分区字段名 | VARCHAR | `pt_dt` |
| partition_value | 分区字段值 | VARCHAR | `2026-04-26` |
| processed_at | 处理时间戳 | DATETIME | `2026-04-26 14:30:00` |

---

## 7. 前端变更

### 7.1 页面变更

| 页面 | 操作 | 说明 |
|------|------|------|
| FTP 配置 | **重构** | 增加 staging_dir, for_upload_dir, archive_dir, error_dir |
| 报表配置 | **重构** | 增加起始行列偏移、映射模式、ODS 备份开关 |
| 处理记录 | **重构** | 增加批次信息、文件名、输出文件、pt_dt 字段 |
| 告警管理 | **新增** | 告警列表、解决操作 |
| 数据中心 | **移除** | 表分层管理页面 |
| Trigger 监控 | **移除** | Trigger 监控页面 |
| 数据管理 | **移除** | 通用数据查询页面 |

### 7.2 侧边栏菜单

```
重构后：
- FTP 配置
- 报表配置
- 处理记录
- 告警管理    ← 新增
- 执行日志
- 操作日志
- 系统日志
```

### 7.3 报表配置页面新增字段

| 字段 | 类型 | 默认值 |
|------|------|--------|
| 起始行号 | 数字 | 1 |
| 起始列号 | 数字 | 1 |
| 映射模式 | 下拉 | 双模式 |
| 重复列名策略 | 下拉 | 自动加后缀 |
| ODS 备份开关 | 开关 | 关闭 |
| ODS 表名 | 文本 | 空（开关打开时显示） |

**ODS 备份说明**：
- 备份写入到与中间件相同的数据库（MySQL dev / GaussDB prod）
- ODS 表结构预先创建（中间件不自动建表）
- ODS 写入作为审计/备份用途，不影响主流程

### 7.4 告警管理页面

**功能**：
- 告警列表（分页、筛选：级别/类型/状态/时间范围）
- 告警详情（文件名、失败原因、关联报表、关联批次）
- 解决告警操作

---

## 8. API 接口

### 8.1 新增接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/alert | 分页查询告警列表 |
| GET | /api/alert/{id} | 查询告警详情 |
| PUT | /api/alert/{id}/resolve | 标记告警已解决 |
| GET | /api/alert/stats | 告警统计 |
| GET | /api/batch | 分页查询批次列表 |
| GET | /api/batch/{id} | 查询批次详情 |

### 8.2 重构接口

| 方法 | 路径 | 变更 |
|------|------|------|
| POST/PUT | /api/ftp/config | 增加 staging/for-upload/archive/error 目录字段 |
| POST/PUT | /api/report/config | 增加 startRow, startCol, mappingMode, duplicateStrategy, odsBackupEnabled, odsTableName |
| GET | /api/task | 返回 batchId, fileName, outputFile, ptDt |

### 8.3 移除接口

- `/api/pipeline/*`
- `/api/trigger/*`
- `/api/data-center/*`

---

## 9. 错误处理

| 场景 | 处理策略 | 归档动作 |
|------|----------|----------|
| 文件名匹配失败 | 跳过 | 不归档 |
| Excel 解析失败 | 告警 ERROR，跳过 | 移动到 error_dir |
| 字段映射失败 | 告警 ERROR，跳过 | 移动到 error_dir |
| 清洗规则未匹配 | 告警 WARNING，保留原值 | 继续处理 |
| ODS 备份失败 | 告警 WARNING | 不阻断主流程 |
| 归档移动失败 | 告警 WARNING | 保留原位 |

### 9.1 告警级别

| 级别 | 含义 | 是否阻断 |
|------|------|----------|
| ERROR | 映射失败、解析失败、FTP 错误 | 跳过该文件 |
| WARNING | 清洗规则未匹配、ODS 失败 | 保留原值继续 |

---

## 10. staging/ 队列管理

### 10.1 队列清理策略

| 策略 | 阈值 | 动作 |
|------|------|------|
| 文件数上限 | > 100 个 | 删除最早的批次 |
| 保留天数上限 | > 7 天 | 删除过期批次 |
| ZIP 大小上限 | 单个 > 200MB | 分割打包（待讨论） |

### 10.2 RPA 串行消费保证

- 中间件检查 for-upload/ 为空后才投递新 ZIP
- RPA 串行执行：下载 → 上传 → 删除
- 无并发边界问题

---

## 11. 移除内容清单

### 11.1 后端移除

```
pipeline/                          # 整个包移除
├── Pipeline.java
├── PipelineExecutor.java
├── PipelineController.java
├── step/                          # 整个包移除
│   ├── PipelineStep.java
│   ├── MyBatisPlusAbstractStep.java
│   ├── DataCleanseStep.java
│   ├── DataAggregateStep.java
│   └── ...
└── example/                        # 整个包移除

trigger/                           # 整个包移除
├── TriggerConfig.java
├── TriggerJob.java
├── ITriggerService.java
├── TriggerServiceImpl.java
├── TriggerStateManager.java
├── DatabaseTriggerStateManager.java
├── PartitionRecordService.java
├── TriggerController.java
├── TriggerMonitor.vue (前端)
└── ...

entity/ 移除
├── TableLayerMapping.java
├── TriggerStateRecord.java
├── TriggerPartitionRecord.java
├── TriggerExecutionLog.java
├── TriggerConfig.java
├── OsdCompanyDeposit.java
├── OsdSales.java
├── TestFlow.java
├── DwdCompanyDeposit.java
├── DwdCleanTestFlow.java
├── Layer1Sales.java
├── DwsCompanyDepositSummary.java
├── Layer2Summary.java
├── DwdTestFlowAgg.java
└── AdsCompanyDeposit.java

mapper/ 移除
├── TableLayerMappingMapper.java
├── TriggerConfigMapper.java
├── TriggerExecutionLogMapper.java
├── TriggerStateRecordMapper.java
├── TriggerPartitionRecordMapper.java
├── OsdCompanyDepositMapper.java
├── OsdSalesMapper.java
├── TestFlowMapper.java
├── DwdCompanyDepositMapper.java
├── DwdCleanTestFlowMapper.java
├── Layer1SalesMapper.java
├── DwsCompanyDepositSummaryMapper.java
├── Layer2SummaryMapper.java
├── DwdTestFlowAggMapper.java
└── AdsCompanyDepositMapper.java
```

### 11.2 前端移除

```
src/views/data-center/             # 整个目录移除
src/views/trigger/                  # 整个目录移除
src/views/data/DataManagement.vue   # 移除
src/api/trigger.js                  # 移除
src/api/dataCenter.js               # 移除
src/api/data.js                     # 重构（保留通用查询）
src/router/index.js                 # 移除 data-center, trigger-monitor 路由
```

---

## 12. 实施优先级

### Phase 1：核心引擎（后端）
1. 重构 entity/ReportConfig 增加字段
2. 重构 entity/FtpConfig 增加字段
3. 新增 entity/AlertRecord, BatchRecord
4. 实现 MiddlewareEngine 核心流程
5. 实现 ExcelTransformService（解析+映射+清洗）
6. 实现 PackagingService（ZIP + 配置表）
7. 实现 BatchService（staging/for-upload 队列）
8. 重构 FtpScanJob
9. 实现 AlertService

### Phase 2：归档与 ODS
1. 实现 ArchiveService
2. 实现 OdsBackupService
3. 数据库脚本更新

### Phase 3：前端
1. 重构 FTP 配置页面
2. 重构报表配置页面
3. 新增告警管理页面
4. 重构处理记录页面
5. 菜单和路由调整

### Phase 4：测试与优化
1. 单元测试
2. 集成测试
3. 性能测试（50MB 大文件）
4. 文档更新

---

**文档状态：Approved — 进入实施阶段**
