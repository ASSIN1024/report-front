# TestFlowPipeline 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 TestFlowPipeline，包含清洗和聚合两个步骤：test_flow → dwd_tets_fdkow → dwd_tets_fdkow_agg

**Architecture:** 使用 Pipeline + PipelineStep 架构，每个 Step 继承 AbstractStep，通过 DELETE+INSERT 保证幂等性

**Tech Stack:** Spring Boot, JdbcTemplate, MySQL

---

## 文件结构

```
report-backend/src/main/java/com/report/pipeline/example/
├── TestFlowPipeline.java         # 重写: 组装清洗+聚合两步流程
├── TestFlowCleanseStep.java      # 新增: 清洗Step (test_flow → dwd_tets_fdkow)
└── TestFlowAggregateStep.java    # 新增: 聚合Step (dwd_tets_fdkow → dwd_tets_fdkow_agg)
```

---

## Task 1: 创建 TestFlowCleanseStep 清洗步骤

**Files:**
- Create: `report-backend/src/main/java/com/report/pipeline/example/TestFlowCleanseStep.java`
- Reference: `report-backend/src/main/java/com/report/pipeline/step/DataCleanseStep.java`

- [ ] **Step 1: 创建 TestFlowCleanseStep.java**

```java
package com.report.pipeline.example;

import com.report.entity.dto.StepContext;
import com.report.pipeline.AbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TestFlowCleanseStep extends AbstractStep {

    @Override
    public String getStepName() {
        return "TestFlow清洗";
    }

    @Override
    protected String getTableName() {
        return "dwd_tets_fdkow";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();
        log.info("[TestFlow清洗] 从 test_flow 读取数据，分区: {}", partitionDate);

        List<Map<String, Object>> rawData = jdbcTemplate.queryForList(
            "SELECT name, amount, pt_dt FROM test_flow WHERE pt_dt = ?",
            java.sql.Date.valueOf(partitionDate)
        );

        log.info("[TestFlow清洗] 读取到 {} 行数据", rawData.size());

        for (Map<String, Object> row : rawData) {
            row.put("pt_dt", partitionDate);
        }

        insertData(getTargetTable(), rawData, partitionDate);
        log.info("[TestFlow清洗] 完成，写入 {} 行到 {}", rawData.size(), getTargetTable());
    }
}
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la report-backend/src/main/java/com/report/pipeline/example/TestFlowCleanseStep.java`

---

## Task 2: 创建 TestFlowAggregateStep 聚合步骤

**Files:**
- Create: `report-backend/src/main/java/com/report/pipeline/example/TestFlowAggregateStep.java`
- Reference: `report-backend/src/main/java/com/report/pipeline/step/DataAggregateStep.java`

- [ ] **Step 1: 创建 TestFlowAggregateStep.java**

```java
package com.report.pipeline.example;

import com.report.entity.dto.StepContext;
import com.report.pipeline.AbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class TestFlowAggregateStep extends AbstractStep {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getStepName() {
        return "TestFlow聚合";
    }

    @Override
    protected String getTableName() {
        return "dwd_tets_fdkow_agg";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();
        log.info("[TestFlow聚合] 从 dwd_tets_fdkow 读取数据，分区: {}", partitionDate);

        String sql = String.format(
            "SELECT name, SUM(amount) as total_amount " +
            "FROM dwd_tets_fdkow WHERE pt_dt = '%s' GROUP BY name",
            partitionDate
        );

        List<Map<String, Object>> aggregatedData = jdbcTemplate.queryForList(sql);

        for (Map<String, Object> row : aggregatedData) {
            row.put("pt_dt", partitionDate);
            if (row.get("total_amount") == null) {
                row.put("total_amount", BigDecimal.ZERO);
            }
        }

        insertData(getTargetTable(), aggregatedData, partitionDate);
        log.info("[TestFlow聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la report-backend/src/main/java/com/report/pipeline/example/TestFlowAggregateStep.java`

---

## Task 3: 重写 TestFlowPipeline 组装两步流程

**Files:**
- Modify: `report-backend/src/main/java/com/report/pipeline/example/TestFlowPipeline.java`

- [ ] **Step 1: 重写 TestFlowPipeline.java**

```java
package com.report.pipeline.example;

import com.report.entity.dto.StepContext;
import com.report.pipeline.Pipeline;
import com.report.pipeline.PipelineStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class TestFlowPipeline implements Pipeline {

    @Autowired
    private TestFlowCleanseStep testFlowCleanseStep;

    @Autowired
    private TestFlowAggregateStep testFlowAggregateStep;

    @Override
    public String getCode() {
        return "test_flow_pipeline";
    }

    @Override
    public String getName() {
        return "测试流水线";
    }

    @Override
    public List<PipelineStep> getSteps() {
        return Arrays.asList(
            testFlowCleanseStep,
            testFlowAggregateStep
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

- [ ] **Step 2: 验证代码编译**

Run: `cd report-backend && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 4: 创建目标表（如果不存在）

**Files:**
- 需要在 MySQL 数据库中创建目标表

- [ ] **Step 1: 创建 dwd_tets_fdkow 表（清洗后的中间表）**

```sql
CREATE TABLE IF NOT EXISTS dwd_tets_fdkow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '名称',
    amount DECIMAL(15,2) DEFAULT 0 COMMENT '金额',
    pt_dt DATE NOT NULL COMMENT '分区日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pt_dt (pt_dt),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TestFlow清洗表';
```

- [ ] **Step 2: 创建 dwd_tets_fdkow_agg 表（聚合结果表）**

```sql
CREATE TABLE IF NOT EXISTS dwd_tets_fdkow_agg (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '名称',
    total_amount DECIMAL(15,2) DEFAULT 0 COMMENT '总金额',
    pt_dt DATE NOT NULL COMMENT '分区日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pt_dt (pt_dt),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TestFlow聚合表';
```

- [ ] **Step 3: 执行建表SQL**

Run: `docker exec -i mysql_container mysql -uroot -proot123 report_db < /dev/stdin <<EOF
CREATE TABLE IF NOT EXISTS dwd_tets_fdkow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '名称',
    amount DECIMAL(15,2) DEFAULT 0 COMMENT '金额',
    pt_dt DATE NOT NULL COMMENT '分区日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pt_dt (pt_dt),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TestFlow清洗表';

CREATE TABLE IF NOT EXISTS dwd_tets_fdkow_agg (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '名称',
    total_amount DECIMAL(15,2) DEFAULT 0 COMMENT '总金额',
    pt_dt DATE NOT NULL COMMENT '分区日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_pt_dt (pt_dt),
    KEY idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='TestFlow聚合表';
EOF`

---

## Task 5: 执行端到端测试

**Files:**
- 验证 Pipeline 执行

- [ ] **Step 1: 调用 Pipeline 执行接口**

Run: `curl -X POST "http://localhost:8082/api/pipeline/test_flow_pipeline/execute?partitionDate=2026-04-06"`
Expected: `{"success":true,"data":{"taskId":123}}`

- [ ] **Step 2: 检查 dwd_tets_fdkow 表数据**

Run: `docker exec -i mysql_container mysql -uroot -proot123 report_db -e "SELECT * FROM dwd_tets_fdkow WHERE pt_dt = '2026-04-06';"`
Expected: 清洗后的数据（无 id 列）

- [ ] **Step 3: 检查 dwd_tets_fdkow_agg 表数据**

Run: `docker exec -i mysql_container mysql -uroot -proot123 report_db -e "SELECT * FROM dwd_tets_fdkow_agg WHERE pt_dt = '2026-04-06';"`
Expected: 按 name 聚合后的数据

- [ ] **Step 4: 验证幂等性（重新执行）**

Run: `curl -X POST "http://localhost:8082/api/pipeline/test_flow_pipeline/execute?partitionDate=2026-04-06"`
然后检查两表数据行数与之前一致

---

## 验证清单

- [ ] TestFlowCleanseStep.java 已创建
- [ ] TestFlowAggregateStep.java 已创建
- [ ] TestFlowPipeline.java 已重写
- [ ] dwd_tets_fdkow 表已创建
- [ ] dwd_tets_fdkow_agg 表已创建
- [ ] Pipeline 执行成功
- [ ] dwd_tets_fdkow 有数据（清洗结果）
- [ ] dwd_tets_fdkow_agg 有数据（聚合结果）
- [ ] 幂等性验证通过

---

## 数据流图

```
test_flow (源表)
    │
    │ SELECT name, amount, pt_dt
    │ WHERE pt_dt = '2026-04-06'
    ▼
┌─────────────────────────────────┐
│  TestFlowCleanseStep           │
│  (清洗: 去掉id列)               │
└─────────────────────────────────┘
    │
    │ INSERT INTO dwd_tets_fdkow
    │ (pt_dt, name, amount)
    ▼
dwd_tets_fdkow (中间表)
    │
    │ SELECT name, SUM(amount)
    │ FROM dwd_tets_fdkow
    │ WHERE pt_dt = '2026-04-06'
    │ GROUP BY name
    ▼
┌─────────────────────────────────┐
│  TestFlowAggregateStep         │
│  (聚合: 按name求和)             │
└─────────────────────────────────┘
    │
    │ INSERT INTO dwd_tets_fdkow_agg
    │ (pt_dt, name, total_amount)
    ▼
dwd_tets_fdkow_agg (结果表)
```
