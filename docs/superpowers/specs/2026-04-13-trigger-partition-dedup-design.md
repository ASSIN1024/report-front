# Trigger 分区防重复触发设计文档

> **版本**: V1.0
> **创建日期**: 2026-04-13
> **问题**: 同一分区被重复触发执行

---

## 1. 问题描述

### 1.1 问题现象

TriggerJob 每 60 秒执行一次，每次检查 `osd_sales_trigger` 时，发现同一分区 `2026-04-07` 被重复触发：

```
partition_date  execution_time        pipeline_task_id        retry_count
2026-04-07     23:59:14            17755775540320000       0
2026-04-07     23:58:14            17755774940300000       0   ← 重复！
2026-04-07     23:57:14            17755774340240000       0   ← 重复！
...
```

### 1.2 问题根因

TriggerJob 代码逻辑：

```java
private void processTrigger(TriggerConfig trigger) {
    TriggerState state = stateManager.getOrCreate(trigger.getTriggerCode());
    Date partitionDate = new Date();  // 每次都是当前日期

    if (数据存在) {
        if (!state.isTriggered()) {   // ← 全局状态，不区分分区
            triggerPipeline(trigger, ...);
            stateManager.reset(trigger.getTriggerCode());  // ← 立即重置
        }
    }
}
```

**时序问题**：
1. T=0s: 实例A执行，检测到数据，触发pipeline，调用 `reset()`
2. T=60s: 实例B执行，`isTriggered()=false`（被A重置），再次触发同一分区

---

## 2. 解决方案

### 2.1 方案设计

**新建 `trigger_partition_record` 表**，以 `trigger_code + partition_date` 作为唯一键，记录每个分区是否已触发。

### 2.2 表结构设计

```sql
CREATE TABLE trigger_partition_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    trigger_code    VARCHAR(100) NOT NULL COMMENT '触发器编码',
    partition_date  DATE NOT NULL COMMENT '分区日期',
    triggered       TINYINT(1) DEFAULT 0 COMMENT '是否已触发',
    pipeline_task_id BIGINT COMMENT 'Pipeline任务ID',
    trigger_time    DATETIME COMMENT '触发时间',
    status          VARCHAR(20) DEFAULT 'TRIGGERED' COMMENT '状态: TRIGGERED/SKIPPED',
    instance_id     VARCHAR(100) COMMENT '执行的实例ID',
    create_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version         INT DEFAULT 0 COMMENT '乐观锁版本',

    UNIQUE KEY uk_trigger_partition (trigger_code, partition_date),
    INDEX idx_status (status),
    INDEX idx_trigger_time (trigger_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT '触发器分区记录表';
```

### 2.3 核心逻辑修改

**修改 `TriggerJob.processTrigger()`**：

```java
private void processTrigger(TriggerConfig trigger) {
    Date partitionDate = new Date();

    // 1. 检查该分区是否已触发
    if (partitionRecordService.isPartitionTriggered(trigger.getTriggerCode(), partitionDate)) {
        log.debug("[{}] 分区 {} 已触发，跳过", trigger.getTriggerName(), partitionDate);
        return;
    }

    // 2. 检查数据是否存在
    int dataCount = triggerService.checkDataExists(trigger, partitionDate);
    if (dataCount > 0) {
        // 3. 尝试标记为触发中（防止并发）
        boolean marked = partitionRecordService.markPartitionTriggering(
            trigger.getTriggerCode(), partitionDate);
        if (!marked) {
            log.debug("[{}] 分区 {} 已被其他实例标记，跳过", trigger.getTriggerName(), partitionDate);
            return;
        }

        // 4. 执行 pipeline
        triggerPipeline(trigger, partitionDate, dataCount);

        // 5. 标记为已触发
        partitionRecordService.markPartitionTriggered(
            trigger.getTriggerCode(), partitionDate, pipelineTaskId);
    }
}
```

---

## 3. 涉及文件

### 3.1 新增文件

| 文件路径 | 说明 |
|---------|------|
| `entity/TriggerPartitionRecord.java` | 分区记录实体 |
| `mapper/TriggerPartitionRecordMapper.java` | MyBatis Mapper |
| `service/PartitionRecordService.java` | 分区记录服务接口 |
| `service/impl/PartitionRecordServiceImpl.java` | 分区记录服务实现 |

### 3.2 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `trigger/TriggerJob.java` | 使用 PartitionRecordService 防重 |
| `schema.sql` | 添加 trigger_partition_record 表 |
| `schema-gaussdb.sql` | 添加 trigger_partition_record 表 |

---

## 4. 测试验证

### 4.1 单元测试

1. 单实例多次执行同一分区，只触发一次
2. 多实例并发执行同一分区，只有一个成功

### 4.2 集成测试

1. 启动两个实例
2. 等待 TriggerJob 执行
3. 查询 `trigger_partition_record` 表，验证只有一个分区记录
4. 查询 `trigger_execution_log` 表，验证只有一个 TRIGGERED 记录

---

## 5. 回滚方案

如出现问题，可通过以下方式回滚：

1. 删除 `trigger_partition_record` 表（业务由 `trigger_execution_log` 记录）
2. 恢复 `TriggerJob.java` 到修改前版本
3. 保持 `trigger_state_record` 表原有逻辑

---

## 6. 其他潜在问题（待后续处理）

| 问题 | 说明 | 优先级 | 状态 |
|------|------|--------|------|
| ProcessedFileService 竞态条件 | `isFileProcessed()` 和 `markAsProcessed()` 之间存在时间窗口 | 中 | 唯一索引兜底 |
| PipelineExecutor 幂等性 | 依赖 Pipeline 内部实现 | 中 | ✅ 已实现 UPSERT |
| TriggerStateRecord 重构 | 现有 `triggered` 和 `retry_count` 可考虑移除 | 低 | 待处理 |

---

## 7. Pipeline Step 幂等机制（已实现）

### 7.1 问题描述

原来的 Step 使用 `DELETE + INSERT` 模式（非原子），并发时可能互相覆盖数据。

### 7.2 解决方案

实现真正的幂等写入：`INSERT ... ON DUPLICATE KEY UPDATE`

### 7.3 实现方式

**PipelineStep 接口新增方法**：

```java
public interface PipelineStep {
    boolean isIdempotent();  // 新增
}
```

**MyBatisPlusAbstractStep 修改**：

```java
@Override
public void execute(StepContext context) throws StepExecutionException {
    try {
        if (isIdempotent()) {
            // 幂等模式：INSERT ... ON DUPLICATE KEY UPDATE
            doExecute(context);
        } else {
            // 覆盖模式：DELETE + INSERT
            if (isOverwrite()) {
                clearPartition(...);
            }
            doExecute(context);
        }
    } catch (Exception e) {
        throw e;
    }
}
```

**幂等 SQL 生成**：

```java
private String buildUpsertSql(String tableName, String[] columns, String[] values) {
    return "INSERT INTO " + tableName + " (" + String.join(", ", columns) + ") " +
           "VALUES (" + String.join(", ", values) + ") " +
           "ON DUPLICATE KEY UPDATE " +
           Arrays.stream(columns)
               .map(c -> c + " = VALUES(" + c + ")")
               .collect(Collectors.joining(", "));
}
```

### 7.4 幂等模式 vs 覆盖模式对比

| 特性 | 覆盖模式 (isOverwrite) | 幂等模式 (isIdempotent) |
|------|-------------------------|--------------------------|
| SQL 操作 | DELETE + INSERT | INSERT ... ON DUPLICATE KEY UPDATE |
| 原子性 | ❌ 非原子 | ✅ 原子 |
| 并发安全 | ❌ 可能互相覆盖 | ✅ 安全 |
| 多次执行效果 | 结果一致 | 结果一致 |

### 7.5 Step 实现示例

```java
@Component
public class DataCleanseStep extends MyBatisPlusAbstractStep {

    @Override
    public boolean isIdempotent() {
        return true;  // 启用幂等模式
    }

    // ... 其他方法保持不变
}
```

### 7.6 支持的数据库

| 数据库 | 语法 |
|--------|------|
| MySQL | `INSERT ... ON DUPLICATE KEY UPDATE` |
| GaussDB/PostgreSQL | 需修改为 `ON CONFLICT DO UPDATE` |
| Oracle | 需修改为 `MERGE INTO` |
