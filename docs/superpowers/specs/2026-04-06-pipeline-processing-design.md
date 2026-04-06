# 数据处理流水线架构设计

**日期**: 2026-04-06
**版本**: V1.0
**状态**: 待确认

***

## 1. 概述

### 1.1 背景

当前系统已完成 RPA 上传报表的基础处理流程：FTP 扫描 → OSD 表。随着业务需求发展，需要支持更复杂的数据处理场景，包括多步流水线处理、分层数据输出、以及流水线间的自动触发机制。

### 1.2 设计目标

1. 支持多步处理流水线（清洗→验证→转换→聚合→报表）
2. 支持分层输出到多张目标表（OSD、layer\_1、layer\_2...）
3. 支持基于分区数据可用性的流程控制（等待重试机制）
4. 支持流水线间的监听触发机制
5. 保证幂等性，防止重跑导致的数据重复

### 1.3 设计原则

- **代码驱动**：Pipeline 和 Step 通过 Java 代码定义，灵活度高
- **配置简单**：监听规则通过配置文件或数据库管理
- **幂等安全**：每个 Step 执行前先 DELETE 再 INSERT
- **松耦合**：Pipeline 之间通过监听器解耦

***

## 2. 整体架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         数据源                                   │
│  ┌──────────┐      ┌──────────┐      ┌──────────────────────┐  │
│  │ RPA上传   │ ───→ │ FTP扫描  │ ───→ │ OSD表 (pt_dt=?)     │  │
│  └──────────┘      └──────────┘      └──────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│                      监听器层 (Listener)                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │  TriggerJob (定时轮询)                                    │   │
│  │  - 监听表: osd_sales                                      │   │
│  │  - 监听分区: pt_dt = '2026-04-06'                         │   │
│  │  - 轮询间隔: 1分钟                                         │   │
│  │  - 最大重试: 60次 (共60分钟)                               │   │
│  │  - 超时动作: 标记为 SKIPPED                                │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                           ↓
┌─────────────────────────────────────────────────────────────────┐
│                      流水线层 (Pipeline)                          │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Pipeline: sales_data_pipeline                            │   │
│  │  Step 1: DataCleanse → layer_1_sales (DELETE+INSERT)    │   │
│  │  Step 2: DataAggregate → layer_2_summary (DELETE+INSERT)│   │
│  │  Step 3: ReportGenerate → osd_sales_report (OVERWRITE)  │   │
│  └──────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                           ↓
                    ┌──────────────┐
                    │ layer_1 表   │
                    │ layer_2 表   │
                    │ osd_report   │
                    └──────────────┘
                           ↓
                    监听器（触发下游Pipeline）
```

### 2.2 核心组件

| 组件               | 职责           | 实现方式             |
| ---------------- | ------------ | ---------------- |
| TriggerJob       | 监听目标表分区是否有数据 | Quartz定时Job，轮询检查 |
| Pipeline         | 多步骤编排执行器     | Java类，定义步骤链      |
| Step             | 单个处理步骤       | Java接口/抽象类       |
| PartitionCleaner | 执行前清空目标分区    | 通用逻辑             |
| TaskExecution    | 任务状态记录       | 复用现有表            |

***

## 3. 核心组件设计

### 3.1 监听器（Trigger）

#### 3.1.1 功能描述

监听器负责检查目标表的指定分区是否有数据。如果数据不存在，则等待后重试；如果超时仍未有数据，则标记任务为 SKIPPED。

#### 3.1.2 配置结构

```java
public class TriggerConfig {
    private String triggerCode;           // 触发器编码，如 "osd_sales_trigger"
    private String sourceTable;            // 监听目标表，如 "osd_sales"
    private String partitionColumn;        // 分区字段，如 "pt_dt"
    private String partitionValue;        // 分区值，如 "2026-04-06"
    private Integer pollIntervalSeconds;  // 轮询间隔，默认60秒
    private Integer maxRetries;           // 最大重试次数，默认60次
    private String pipelineCode;           // 触发执行的Pipeline编码
    private String status;                // 触发器状态：ENABLED/DISABLED
}
```

#### 3.1.3 执行流程

```
TriggerJob 执行逻辑：

1. 获取所有启用的触发器配置
2. 对每个触发器：
   a. 检查目标表指定分区是否有数据
      SELECT COUNT(*) FROM {sourceTable} WHERE {partitionColumn} = {partitionValue}
   b. 如果有数据 (count > 0)：
      - 创建 Pipeline 任务
      - 调用 PipelineExecutor 执行
      - 更新触发器状态为 TRIGGERED
   c. 如果无数据：
      - 增加重试计数
      - 如果超过 maxRetries：
        - 标记任务为 SKIPPED
        - 记录跳过原因
```

### 3.2 流水线（Pipeline）

#### 3.2.1 功能描述

Pipeline 是多步骤处理流程的编排器，按顺序执行各个 Step，支持条件跳过、错误处理等。

#### 3.2.2 接口设计

```java
public interface Pipeline {
    String getCode();
    String getName();
    List<PipelineStep> getSteps();
    boolean isIdempotent();
}

public interface PipelineStep {
    String getStepName();
    void execute(StepContext context) throws StepExecutionException;
    boolean isOverwrite();  // 是否覆盖模式
    String getTargetTable(); // 目标表名
}
```

#### 3.2.3 Pipeline 实现示例

```java
@Component
public class SalesDataPipeline implements Pipeline {

    @Override
    public String getCode() {
        return "sales_data_pipeline";
    }

    @Override
    public String getName() {
        return "销售数据处理流水线";
    }

    @Override
    public List<PipelineStep> getSteps() {
        return Arrays.asList(
            new DataCleanseStep(),      // 清洗
            new DataAggregateStep(),     // 聚合
            new ReportGenerateStep()     // 报表生成
        );
    }

    @Override
    public boolean isIdempotent() {
        return true;  // 通过覆盖模式保证幂等
    }
}
```

### 3.3 步骤（Step）

#### 3.3.1 Step 抽象基类

```java
public abstract class AbstractStep implements PipelineStep {

    @Override
    public void execute(StepContext context) throws StepExecutionException {
        String targetTable = getTargetTable();
        LocalDate partitionDate = context.getPartitionDate();

        // 幂等保证：执行前清空目标分区
        if (isOverwrite()) {
            jdbcTemplate.execute(
                String.format("DELETE FROM %s WHERE pt_dt = '%s'", targetTable, partitionDate)
            );
        }

        // 执行具体处理逻辑
        doExecute(context);
    }

    protected abstract void doExecute(StepContext context) throws StepExecutionException;

    protected abstract String getTargetTable();

    protected boolean isOverwrite() {
        return true;  // 默认覆盖模式
    }
}
```

#### 3.3.2 Step 实现示例

```java
public class DataCleanseStep extends AbstractStep {

    @Override
    public String getStepName() {
        return "数据清洗";
    }

    @Override
    protected String getTargetTable() {
        return "layer_1_sales";
    }

    @Override
    protected void doExecute(StepContext context) {
        // 1. 从 OSD 表读取数据
        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(
            "SELECT * FROM osd_sales WHERE pt_dt = ?",
            context.getPartitionDate()
        );

        // 2. 执行清洗逻辑
        List<Map<String, Object>> cleansedData = rawData.stream()
            .map(this::cleanseRow)
            .collect(Collectors.toList());

        // 3. 写入目标表
        insertData(getTargetTable(), cleansedData, context.getPartitionDate());
    }

    private Map<String, Object> cleanseRow(Map<String, Object> row) {
        // 清洗规则：替换空值、修正格式等
        if (row.get("amount") == null || "".equals(row.get("amount"))) {
            row.put("amount", BigDecimal.ZERO);
        }
        return row;
    }
}
```

### 3.4 流水线执行器（PipelineExecutor）

```java
@Service
@Slf4j
public class PipelineExecutor {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TaskService taskService;

    public void execute(String pipelineCode, LocalDate partitionDate) {
        Pipeline pipeline = getPipeline(pipelineCode);
        Long taskId = createTask(pipeline, partitionDate);

        TaskContext context = new TaskContext(taskId, partitionDate);

        for (PipelineStep step : pipeline.getSteps()) {
            try {
                log.info("执行步骤: {} (Pipeline: {})", step.getStepName(), pipelineCode);
                step.execute(context);
                log.info("步骤完成: {}", step.getStepName());
            } catch (Exception e) {
                log.error("步骤执行失败: {}", step.getStepName(), e);
                taskService.finishTask(taskId, "FAILED", e.getMessage());
                throw new StepExecutionException("步骤执行失败: " + step.getStepName(), e);
            }
        }

        taskService.finishTask(taskId, "SUCCESS", null);
        log.info("Pipeline执行完成: {}", pipelineCode);
    }
}
```

***

## 4. 数据库设计

### 4.1 新增表：trigger\_config（触发器配置）

```sql
CREATE TABLE trigger_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code VARCHAR(100) NOT NULL COMMENT '触发器编码',
    trigger_name VARCHAR(200) NOT NULL COMMENT '触发器名称',
    source_table VARCHAR(100) NOT NULL COMMENT '监听目标表',
    partition_column VARCHAR(50) NOT NULL DEFAULT 'pt_dt' COMMENT '分区字段',
    partition_pattern VARCHAR(50) COMMENT '分区值模式，支持日期格式如 yyyy-MM-dd',
    poll_interval_seconds INT NOT NULL DEFAULT 60 COMMENT '轮询间隔(秒)',
    max_retries INT NOT NULL DEFAULT 60 COMMENT '最大重试次数',
    pipeline_code VARCHAR(100) NOT NULL COMMENT '触发执行的Pipeline编码',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    last_trigger_time DATETIME COMMENT '最后触发时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_trigger_code (trigger_code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='触发器配置表';
```

### 4.2 新增表：pipeline\_config（流水线配置）

```sql
CREATE TABLE pipeline_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    pipeline_code VARCHAR(100) NOT NULL COMMENT '流水线编码',
    pipeline_name VARCHAR(200) NOT NULL COMMENT '流水线名称',
    description VARCHAR(500) COMMENT '流水线描述',
    idempotent_mode VARCHAR(20) NOT NULL DEFAULT 'OVERWRITE' COMMENT '幂等模式: OVERWRITE/APPEND',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '状态: ENABLED/DISABLED',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pipeline_code (pipeline_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='流水线配置表';
```

### 4.3 扩展现有表：task\_execution

新增字段：

```sql
ALTER TABLE task_execution
ADD COLUMN pipeline_code VARCHAR(100) COMMENT '流水线编码',
ADD COLUMN partition_value VARCHAR(50) COMMENT '分区值',
ADD COLUMN step_name VARCHAR(100) COMMENT '当前步骤名称';
```

***

## 5. 触发流程详解

### 5.1 场景1：RPA上传触发

```
时间线：
[08:00] RPA上传 sales_20260406.xlsx 到 FTP
[08:01] FtpScanJob 扫描到文件
[08:01] DataProcessJob 处理文件，写入 osd_sales (pt_dt='2026-04-06')
[08:02] TriggerJob 检查 osd_sales (pt_dt='2026-04-06') → 150行
[08:02] 触发 sales_data_pipeline
[08:02] Step 1: 数据清洗 → layer_1_sales
[08:05] Step 2: 数据聚合 → layer_2_summary
[08:08] Step 3: 报表生成 → osd_sales_report
[08:10] Pipeline 完成
```

### 5.2 场景2：跨流水线触发

```
时间线：
[08:10] sales_data_pipeline 完成，layer_2_summary 有新数据
[08:11] TriggerJob 检查 layer_2_summary (pt_dt='2026-04-06') → 有数据
[08:11] 触发 financial_analysis_pipeline
[08:11] Step 1: 财务指标计算
[08:15] Step 2: 同比环比分析
[08:20] Pipeline 完成
```

### 5.3 场景3：等待重试

```
时间线：
[08:00] TriggerJob 检查 osd_sales (pt_dt='2026-04-06') → 0行
[08:01] 重试第1次 → 0行
[08:02] 重试第2次 → 0行
...（每分钟重试一次）
[09:00] 重试第60次 → 0行
[09:00] 标记任务为 SKIPPED，跳过原因：数据就绪超时
```

***

## 6. 幂等性保证

### 6.1 分区覆盖写入

每个 Step 执行前，先删除目标表的对应分区数据，再插入新数据：

```java
// Step 执行前
String sql = String.format("DELETE FROM %s WHERE pt_dt = '%s'", targetTable, partitionDate);
jdbcTemplate.execute(sql);

// Step 执行中
String insertSql = String.format("INSERT INTO %s (...) VALUES (...)", targetTable);
jdbcTemplate.batchUpdate(insertSql, batchArgs);
```

### 6.2 重跑场景

```
原始执行：
  Step 1 DELETE + INSERT → layer_1_sales (100行)
  Step 2 DELETE + INSERT → layer_2_summary (10行)

重跑执行：
  Step 1 DELETE + INSERT → layer_1_sales (重新计算100行)
  Step 2 DELETE + INSERT → layer_2_summary (重新计算10行)

结果：最终数据是正确的，没有重复
```

### 6.3 失败恢复

```
Step 3 执行失败时：
  - layer_1_sales: 已插入（正确）
  - layer_2_summary: 已插入（正确）
  - osd_report: 未执行

重跑时：
  - Step 1: DELETE + INSERT → 重新清洗
  - Step 2: DELETE + INSERT → 重新聚合
  - Step 3: DELETE + INSERT → 重新生成报表

结果：最终数据一致，无重复
```

***

## 7. API 设计

### 7.1 触发器管理

```
GET /api/trigger
  - 获取所有触发器配置
  - Response: { data: [TriggerConfig...] }

POST /api/trigger
  - 创建触发器
  - Request: TriggerConfig
  - Response: { success: true, id: 1 }

PUT /api/trigger/{code}
  - 更新触发器
  - Request: TriggerConfig
  - Response: { success: true }

DELETE /api/trigger/{code}
  - 删除触发器
  - Response: { success: true }

POST /api/trigger/{code}/test
  - 测试触发器（立即执行一次检查）
  - Response: { triggered: true/false, message: "..." }
```

### 7.2 流水线管理

```
GET /api/pipeline
  - 获取所有流水线配置
  - Response: { data: [PipelineConfig...] }

POST /api/pipeline/{code}/execute
  - 手动触发流水线执行
  - Request: { partitionDate: "2026-04-06" }
  - Response: { taskId: 123, status: "RUNNING" }

GET /api/pipeline/{code}/tasks
  - 获取流水线执行历史
  - Response: { data: [TaskExecution...] }
```

### 7.3 任务监控

```
GET /api/task/{taskId}
  - 获取任务详情
  - Response: { taskId, pipelineCode, status, steps: [...] }

POST /api/task/{taskId}/retry
  - 重试失败任务
  - Response: { taskId: 123, status: "RUNNING" }
```

***

## 8. 实现步骤

### Phase 1: 核心框架

1. 实现 Pipeline 接口和抽象类
2. 实现 PipelineExecutor
3. 实现 TriggerJob 基础轮询逻辑

### Phase 2: 步骤实现

1. 实现数据清洗 Step（DataCleanseStep）
2. 实现数据聚合 Step（DataAggregateStep）
3. 实现报表生成 Step（ReportGenerateStep）

### Phase 3: 配置管理

1. 创建 trigger\_config 表和实体
2. 创建 pipeline\_config 表和实体
3. 实现 TriggerController 和 PipelineController

### Phase 4: 完善功能

1. 实现等待重试机制
2. 实现跨流水线触发
3. 实现任务监控界面

***

## 9. 与现有系统的整合

### 9.1 复用现有组件

| 现有组件           | 复用方式                       |
| -------------- | -------------------------- |
| TaskService    | 复用 task\_execution 表记录任务状态 |
| LogService     | 复用日志记录功能                   |
| FtpScanJob     | 保持独立，OSD 数据由其产生            |
| DataProcessJob | 可作为 Pipeline 的一个 Step 整合   |

### 9.2 共存策略

新架构与现有架构共存：

- 现有 FtpScanJob + DataProcessJob 继续产生 OSD 数据
- 新 TriggerJob 监听 OSD 表，触发 Pipeline 执行
- Pipeline 的第一步可以是"数据验证/清洗"

***

## 10. 待确认事项

1. TriggerJob 的轮询间隔和最大重试次数默认值是否合适？
2. 是否需要支持一个 Pipeline 被多个 Trigger 触发？
3. Pipeline 执行失败时的告警机制？
4. 是否有其他需要内置的 Step 类型？

