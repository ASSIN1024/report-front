# 数据处理流水线实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现数据处理流水线架构，支持多步Pipeline、监听器触发、分区覆盖幂等

**Architecture:**
- TriggerJob 定时轮询检查目标表分区，数据就绪后触发 PipelineExecutor
- PipelineExecutor 按顺序执行 Pipeline 中的各个 Step
- 每个 Step 执行前通过 DELETE + INSERT 保证幂等性
- Pipeline 之间通过 Trigger 解耦

**Tech Stack:** Spring Boot 2.1.2, Quartz, MyBatis-Plus, JdbcTemplate

---

## 文件结构

```
report-backend/src/main/java/com/report/
├── pipeline/                          # 新增目录
│   ├── Pipeline.java                  # Pipeline接口
│   ├── PipelineStep.java              # Step接口
│   ├── AbstractStep.java              # Step抽象基类
│   ├── PipelineExecutor.java          # Pipeline执行器
│   ├── step/                          # 内置Step实现
│   │   ├── DataCleanseStep.java       # 数据清洗Step
│   │   ├── DataAggregateStep.java     # 数据聚合Step
│   │   └── ReportGenerateStep.java     # 报表生成Step
│   └── example/                       # 示例Pipeline
│       └── SalesDataPipeline.java     # 销售数据流水线示例
├── trigger/                           # 新增目录
│   ├── TriggerConfig.java             # 触发器配置实体
│   ├── TriggerConfigMapper.java       # MyBatis Mapper
│   ├── TriggerService.java            # 触发器服务
│   ├── TriggerServiceImpl.java         # 触发器服务实现
│   ├── TriggerController.java         # 触发器API
│   └── TriggerJob.java                # 触发器轮询Job
├── config/
│   └── PipelineConfig.java             # 流水线配置
└── entity/dto/
    └── StepContext.java               # Step执行上下文

report-backend/src/main/resources/
├── mapper/trigger/                    # MyBatis XML
│   └── TriggerConfigMapper.xml
└── schema.sql                         # 数据库变更
```

---

## Task 1: 数据库变更

**Files:**
- Modify: `report-backend/src/main/resources/schema.sql`

- [ ] **Step 1: 添加 trigger_config 表**

```sql
-- 触发器配置表
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

- [ ] **Step 2: 添加 pipeline_config 表**

```sql
-- 流水线配置表
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

- [ ] **Step 3: 扩展 task_execution 表**

```sql
-- 任务执行表新增字段
ALTER TABLE task_execution
ADD COLUMN pipeline_code VARCHAR(100) COMMENT '流水线编码',
ADD COLUMN partition_value VARCHAR(50) COMMENT '分区值',
ADD COLUMN step_name VARCHAR(100) COMMENT '当前步骤名称';
```

---

## Task 2: 核心接口定义

**Files:**
- Create: `report-backend/src/main/java/com/report/pipeline/Pipeline.java`
- Create: `report-backend/src/main/java/com/report/pipeline/PipelineStep.java`
- Create: `report-backend/src/main/java/com/report/entity/dto/StepContext.java`

- [ ] **Step 1: 创建 Pipeline 接口**

```java
package com.report.pipeline;

import com.report.entity.dto.StepContext;
import java.time.LocalDate;
import java.util.List;

public interface Pipeline {
    String getCode();
    String getName();
    List<PipelineStep> getSteps();
    boolean isIdempotent();
    void execute(StepContext context) throws Exception;
}
```

- [ ] **Step 2: 创建 PipelineStep 接口**

```java
package com.report.pipeline;

import com.report.entity.dto.StepContext;

public interface PipelineStep {
    String getStepName();
    void execute(StepContext context) throws StepExecutionException;
    boolean isOverwrite();
    String getTargetTable();
}
```

- [ ] **Step 3: 创建 StepExecutionException**

```java
package com.report.pipeline;

public class StepExecutionException extends Exception {
    private final String stepName;

    public StepExecutionException(String stepName, String message) {
        super(message);
        this.stepName = stepName;
    }

    public StepExecutionException(String stepName, String message, Throwable cause) {
        super(message, cause);
        this.stepName = stepName;
    }

    public String getStepName() {
        return stepName;
    }
}
```

- [ ] **Step 4: 创建 StepContext**

```java
package com.report.entity.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.Map;

@Data
public class StepContext {
    private Long taskId;
    private LocalDate partitionDate;
    private Map<String, Object> params;

    public StepContext(Long taskId, LocalDate partitionDate) {
        this.taskId = taskId;
        this.partitionDate = partitionDate;
    }
}
```

---

## Task 3: AbstractStep 抽象基类

**Files:**
- Create: `report-backend/src/main/java/com/report/pipeline/AbstractStep.java`

- [ ] **Step 1: 创建 AbstractStep 抽象类**

```java
package com.report.pipeline;

import com.report.entity.dto.StepContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
public abstract class AbstractStep implements PipelineStep {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Override
    public void execute(StepContext context) throws StepExecutionException {
        if (isOverwrite()) {
            clearPartition(getTargetTable(), context.getPartitionDate());
        }
        doExecute(context);
    }

    protected void clearPartition(String tableName, java.time.LocalDate partitionDate) {
        String sql = String.format("DELETE FROM %s WHERE pt_dt = '%s'", tableName, partitionDate);
        log.info("[{}] 清空分区: {}", getStepName(), sql);
        jdbcTemplate.execute(sql);
    }

    protected abstract void doExecute(StepContext context) throws StepExecutionException;

    protected abstract String getTargetTable();

    @Override
    public boolean isOverwrite() {
        return true;
    }
}
```

---

## Task 4: PipelineExecutor 执行器

**Files:**
- Create: `report-backend/src/main/java/com/report/pipeline/PipelineExecutor.java`

- [ ] **Step 1: 创建 PipelineExecutor**

```java
package com.report.pipeline;

import com.report.entity.TaskExecution;
import com.report.entity.dto.StepContext;
import com.report.service.LogService;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class PipelineExecutor {

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

    @Autowired
    private Map<String, Pipeline> pipelineMap;

    public Long execute(String pipelineCode, LocalDate partitionDate) {
        Pipeline pipeline = pipelineMap.get(pipelineCode);
        if (pipeline == null) {
            throw new RuntimeException("Pipeline不存在: " + pipelineCode);
        }

        TaskExecution task = taskService.createTask(
            "PIPELINE",
            pipeline.getName() + " - " + partitionDate,
            null,
            pipelineCode,
            null
        );

        StepContext context = new StepContext(task.getId(), partitionDate);
        context.setParams(new HashMap<>());

        try {
            taskService.updateTaskStatus(task.getId(), "RUNNING");
            logService.saveLog(task.getId(), "INFO", "Pipeline开始执行: " + pipelineCode);

            pipeline.execute(context);

            taskService.finishTask(task.getId(), "SUCCESS", null);
            logService.saveLog(task.getId(), "INFO", "Pipeline执行完成: " + pipelineCode);
        } catch (Exception e) {
            log.error("Pipeline执行失败: {}", pipelineCode, e);
            taskService.finishTask(task.getId(), "FAILED", e.getMessage());
            logService.saveLog(task.getId(), "ERROR", "Pipeline执行失败: " + e.getMessage());
        }

        return task.getId();
    }
}
```

---

## Task 5: Trigger 配置层

**Files:**
- Create: `report-backend/src/main/java/com/report/trigger/TriggerConfig.java`
- Create: `report-backend/src/main/java/com/report/trigger/TriggerConfigMapper.java`
- Create: `report-backend/src/main/java/com/report/trigger/TriggerService.java`
- Create: `report-backend/src/main/java/com/report/trigger/TriggerServiceImpl.java`
- Create: `report-backend/src/main/resources/mapper/trigger/TriggerConfigMapper.xml`

- [ ] **Step 1: 创建 TriggerConfig 实体**

```java
package com.report.trigger;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TriggerConfig {
    private Long id;
    private String triggerCode;
    private String triggerName;
    private String sourceTable;
    private String partitionColumn;
    private String partitionPattern;
    private Integer pollIntervalSeconds;
    private Integer maxRetries;
    private String pipelineCode;
    private String status;
    private LocalDateTime lastTriggerTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 TriggerConfigMapper**

```java
package com.report.trigger;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TriggerConfigMapper extends BaseMapper<TriggerConfig> {
}
```

- [ ] **Step 3: 创建 TriggerService 接口**

```java
package com.report.trigger;

import java.time.LocalDate;
import java.util.List;

public interface TriggerService {
    List<TriggerConfig> getAllEnabled();
    TriggerConfig getByCode(String triggerCode);
    int checkDataExists(TriggerConfig config, LocalDate partitionDate);
    void updateLastTriggerTime(String triggerCode);
}
```

- [ ] **Step 4: 创建 TriggerServiceImpl**

```java
package com.report.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
public class TriggerServiceImpl implements TriggerService {

    @Autowired
    private TriggerConfigMapper triggerConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TaskService taskService;

    @Autowired
    private PipelineExecutor pipelineExecutor;

    @Override
    public List<TriggerConfig> getAllEnabled() {
        return triggerConfigMapper.selectList(
            new LambdaQueryWrapper<TriggerConfig>()
                .eq(TriggerConfig::getStatus, "ENABLED")
        );
    }

    @Override
    public TriggerConfig getByCode(String triggerCode) {
        return triggerConfigMapper.selectOne(
            new LambdaQueryWrapper<TriggerConfig>()
                .eq(TriggerConfig::getTriggerCode, triggerCode)
        );
    }

    @Override
    public int checkDataExists(TriggerConfig config, LocalDate partitionDate) {
        String sql = String.format(
            "SELECT COUNT(*) FROM %s WHERE %s = '%s'",
            config.getSourceTable(),
            config.getPartitionColumn(),
            partitionDate
        );
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }

    @Override
    public void updateLastTriggerTime(String triggerCode) {
        TriggerConfig config = getByCode(triggerCode);
        if (config != null) {
            config.setLastTriggerTime(java.time.LocalDateTime.now());
            triggerConfigMapper.updateById(config);
        }
    }
}
```

- [ ] **Step 5: 创建 TriggerConfigMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.report.trigger.TriggerConfigMapper">
</mapper>
```

---

## Task 6: TriggerJob 轮询任务

**Files:**
- Create: `report-backend/src/main/java/com/report/trigger/TriggerJob.java`
- Create: `report-backend/src/main/java/com/report/trigger/TriggerStateManager.java`

- [ ] **Step 1: 创建 TriggerStateManager（重试状态管理）**

```java
package com.report.trigger;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TriggerState {
    private String triggerCode;
    private int retryCount;
    private LocalDateTime lastCheckTime;
    private boolean triggered;
}
```

```java
package com.report.trigger;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TriggerStateManager {
    private final ConcurrentHashMap<String, TriggerState> states = new ConcurrentHashMap<>();

    public TriggerState getOrCreate(String triggerCode) {
        return states.computeIfAbsent(triggerCode, code -> {
            TriggerState state = new TriggerState();
            state.setTriggerCode(code);
            state.setRetryCount(0);
            state.setTriggered(false);
            return state;
        });
    }

    public void reset(String triggerCode) {
        TriggerState state = states.get(triggerCode);
        if (state != null) {
            state.setRetryCount(0);
            state.setTriggered(false);
        }
    }
}
```

- [ ] **Step 2: 创建 TriggerJob**

```java
package com.report.trigger;

import com.report.pipeline.PipelineExecutor;
import com.report.service.LogService;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class TriggerJob implements Job {

    @Autowired
    private TriggerService triggerService;

    @Autowired
    private TriggerStateManager stateManager;

    @Autowired
    private PipelineExecutor pipelineExecutor;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("TriggerJob 开始执行");

        List<TriggerConfig> triggers = triggerService.getAllEnabled();
        for (TriggerConfig trigger : triggers) {
            processTrigger(trigger);
        }

        log.info("TriggerJob 执行完成");
    }

    private void processTrigger(TriggerConfig trigger) {
        TriggerState state = stateManager.getOrCreate(trigger.getTriggerCode());
        LocalDate partitionDate = resolvePartitionDate(trigger.getPartitionPattern());

        int dataCount = triggerService.checkDataExists(trigger, partitionDate);

        if (dataCount > 0) {
            log.info("[{}] 检测到数据: {} 行，分区: {}", trigger.getTriggerName(), dataCount, partitionDate);

            if (!state.isTriggered()) {
                triggerPipeline(trigger, partitionDate);
                stateManager.reset(trigger.getTriggerCode());
            }
        } else {
            state.setRetryCount(state.getRetryCount() + 1);
            state.setLastCheckTime(LocalDateTime.now());

            if (state.getRetryCount() > trigger.getMaxRetries()) {
                log.warn("[{}] 等待数据超时，分区: {}，重试次数: {}",
                    trigger.getTriggerName(), partitionDate, state.getRetryCount());

                markTaskSkipped(trigger, partitionDate, "数据就绪超时");
                stateManager.reset(trigger.getTriggerCode());
            } else {
                log.debug("[{}] 等待数据中，分区: {}，重试: {}/{}",
                    trigger.getTriggerName(), partitionDate,
                    state.getRetryCount(), trigger.getMaxRetries());
            }
        }
    }

    private void triggerPipeline(TriggerConfig trigger, LocalDate partitionDate) {
        try {
            log.info("[{}] 触发Pipeline: {}", trigger.getTriggerName(), trigger.getPipelineCode());
            pipelineExecutor.execute(trigger.getPipelineCode(), partitionDate);
            triggerService.updateLastTriggerTime(trigger.getTriggerCode());
        } catch (Exception e) {
            log.error("[{}] Pipeline触发失败: {}", trigger.getTriggerName(), e.getMessage());
        }
    }

    private void markTaskSkipped(TriggerConfig trigger, LocalDate partitionDate, String reason) {
        taskService.createTask(
            "TRIGGER",
            trigger.getTriggerName() + " - " + partitionDate,
            null,
            trigger.getTriggerCode(),
            "SKIPPED: " + reason
        );
    }

    private LocalDate resolvePartitionDate(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return LocalDate.now();
        }
        return LocalDate.now();
    }
}
```

---

## Task 7: 示例 Step 实现

**Files:**
- Create: `report-backend/src/main/java/com/report/pipeline/step/DataCleanseStep.java`
- Create: `report-backend/src/main/java/com/report/pipeline/step/DataAggregateStep.java`
- Create: `report-backend/src/main/java/com/report/pipeline/example/SalesDataPipeline.java`

- [ ] **Step 1: 创建 DataCleanseStep**

```java
package com.report.pipeline.step;

import com.report.entity.dto.StepContext;
import com.report.pipeline.AbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
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
    protected void doExecute(StepContext context) throws StepExecutionException {
        log.info("[数据清洗] 从 OSD 表读取数据，分区: {}", context.getPartitionDate());

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(
            "SELECT * FROM osd_sales WHERE pt_dt = ?",
            java.sql.Date.valueOf(context.getPartitionDate())
        );

        log.info("[数据清洗] 读取到 {} 行数据", rawData.size());

        for (Map<String, Object> row : rawData) {
            cleanseRow(row);
        }

        insertData(getTargetTable(), rawData, context.getPartitionDate());
        log.info("[数据清洗] 完成，写入 {} 行到 {}", rawData.size(), getTargetTable());
    }

    private void cleanseRow(Map<String, Object> row) {
        if (row.get("amount") == null || "".equals(row.get("amount"))) {
            row.put("amount", BigDecimal.ZERO);
        }
        row.put("pt_dt", context.getPartitionDate());
    }
}
```

- [ ] **Step 2: 创建 DataAggregateStep**

```java
package com.report.pipeline.step;

import com.report.entity.dto.StepContext;
import com.report.pipeline.AbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataAggregateStep extends AbstractStep {

    @Override
    public String getStepName() {
        return "数据聚合";
    }

    @Override
    protected String getTargetTable() {
        return "layer_2_summary";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        String sql = String.format(
            "SELECT product_name, SUM(amount) as total_amount, COUNT(*) as order_count " +
            "FROM layer_1_sales WHERE pt_dt = '%s' GROUP BY product_name",
            partitionDate
        );

        List<Map<String, Object>> aggregatedData = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : aggregatedData) {
            row.put("pt_dt", partitionDate);
        }

        insertData(getTargetTable(), aggregatedData, partitionDate);
        log.info("[数据聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}
```

- [ ] **Step 3: 创建 SalesDataPipeline 示例**

```java
package com.report.pipeline.example;

import com.report.entity.dto.StepContext;
import com.report.pipeline.Pipeline;
import com.report.pipeline.PipelineStep;
import com.report.pipeline.step.DataCleanseStep;
import com.report.pipeline.step.DataAggregateStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SalesDataPipeline implements Pipeline {

    @Autowired
    private DataCleanseStep dataCleanseStep;

    @Autowired
    private DataAggregateStep dataAggregateStep;

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
            dataCleanseStep,
            dataAggregateStep
        );
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }

    @Override
    public void execute(StepContext context) throws Exception {
        for (PipelineStep step : getSteps()) {
            step.execute(context);
        }
    }
}
```

---

## Task 8: TriggerController API

**Files:**
- Create: `report-backend/src/main/java/com/report/trigger/TriggerController.java`

- [ ] **Step 1: 创建 TriggerController**

```java
package com.report.trigger;

import com.report.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/trigger")
public class TriggerController {

    @Autowired
    private TriggerService triggerService;

    @GetMapping
    public Result<List<TriggerConfig>> getAllTriggers() {
        return Result.success(triggerService.getAllEnabled());
    }

    @GetMapping("/{code}")
    public Result<TriggerConfig> getTrigger(@PathVariable String code) {
        TriggerConfig config = triggerService.getByCode(code);
        if (config == null) {
            return Result.fail("触发器不存在");
        }
        return Result.success(config);
    }

    @PostMapping
    public Result<Void> createTrigger(@RequestBody TriggerConfig config) {
        return Result.success(null);
    }

    @PutMapping("/{code}")
    public Result<Void> updateTrigger(@PathVariable String code, @RequestBody TriggerConfig config) {
        return Result.success(null);
    }

    @DeleteMapping("/{code}")
    public Result<Void> deleteTrigger(@PathVariable String code) {
        return Result.success(null);
    }

    @PostMapping("/{code}/test")
    public Result<Map<String, Object>> testTrigger(@PathVariable String code) {
        TriggerConfig config = triggerService.getByCode(code);
        if (config == null) {
            return Result.fail("触发器不存在");
        }

        LocalDate partitionDate = LocalDate.now();
        int dataCount = triggerService.checkDataExists(config, partitionDate);

        Map<String, Object> result = new HashMap<>();
        result.put("triggerCode", code);
        result.put("partitionDate", partitionDate);
        result.put("dataCount", dataCount);
        result.put("hasData", dataCount > 0);

        return Result.success(result);
    }
}
```

---

## Task 9: Quartz 配置 TriggerJob

**Files:**
- Modify: `report-backend/src/main/resources/application.yml`

- [ ] **Step 1: 添加 TriggerJob 的 Quartz 配置**

```yaml
spring:
  quartz:
    job-store-type: memory
    wait-for-jobs-to-complete-on-shutdown: true
    overwrite-existing-jobs: true
    startup-delay: 10s
    properties:
      org:
        quartz:
          scheduler:
            instanceName: ReportScheduler
          jobStore:
            class: org.quartz.simpl.RAMJobStore
          threadPool:
            class: org.quartz.simpl.SimpleThreadPool
            threadCount: 5
            threadPriority: 5
```

---

## Task 10: 单元测试

**Files:**
- Create: `report-backend/src/test/java/com/report/pipeline/PipelineExecutorTest.java`
- Create: `report-backend/src/test/java/com/report/trigger/TriggerServiceTest.java`

- [ ] **Step 1: 创建 PipelineExecutorTest**

```java
package com.report.pipeline;

import com.report.entity.dto.StepContext;
import com.report.pipeline.example.SalesDataPipeline;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class PipelineExecutorTest {

    @Autowired
    private PipelineExecutor pipelineExecutor;

    @Test
    public void testExecutePipeline() {
        LocalDate partitionDate = LocalDate.now();
        Long taskId = pipelineExecutor.execute("sales_data_pipeline", partitionDate);
        assertNotNull(taskId);
    }
}
```

- [ ] **Step 2: 创建 TriggerServiceTest**

```java
package com.report.trigger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TriggerServiceTest {

    @Autowired
    private TriggerService triggerService;

    @Test
    public void testGetAllEnabled() {
        List<TriggerConfig> triggers = triggerService.getAllEnabled();
        assertNotNull(triggers);
    }

    @Test
    public void testCheckDataExists() {
        TriggerConfig config = new TriggerConfig();
        config.setSourceTable("osd_sales");
        config.setPartitionColumn("pt_dt");
        config.setPartitionPattern("yyyy-MM-dd");

        int count = triggerService.checkDataExists(config, LocalDate.now());
        assertTrue(count >= 0);
    }
}
```

---

## Task 11: Git 提交

- [ ] **Step 1: 提交代码**

```bash
git add .
git commit -m "feat(pipeline): 实现数据处理流水线架构

- 新增 Pipeline 和 PipelineStep 接口
- 新增 TriggerConfig 触发器配置
- 新增 TriggerJob 轮询任务
- 新增 PipelineExecutor 执行器
- 新增数据清洗和聚合 Step 示例
- 新增触发器 API 接口

关联设计文档: docs/superpowers/specs/2026-04-06-pipeline-processing-design.md"
```

---

## 实现顺序

1. **Task 1**: 数据库变更（基础，先建表）
2. **Task 2**: 核心接口定义（Pipeline, PipelineStep, StepContext）
3. **Task 3**: AbstractStep 抽象基类
4. **Task 4**: PipelineExecutor 执行器
5. **Task 5**: Trigger 配置层（实体、Mapper、Service）
6. **Task 6**: TriggerJob 轮询任务
7. **Task 7**: 示例 Step 实现
8. **Task 8**: TriggerController API
9. **Task 9**: Quartz 配置
10. **Task 10**: 单元测试
11. **Task 11**: Git 提交

---

## 依赖关系图

```
Task 1 (数据库)
    ↓
Task 2 (核心接口) ← Task 3 (AbstractStep)
    ↓                  ↓
Task 4 (Executor)  ← Task 7 (Step实现)
    ↑
Task 5 (Trigger配置)
    ↓
Task 6 (TriggerJob)
    ↓
Task 8 (API) → Task 9 (Quartz配置) → Task 10 (测试) → Task 11 (提交)
```
