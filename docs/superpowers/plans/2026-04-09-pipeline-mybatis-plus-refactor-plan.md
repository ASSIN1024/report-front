# Pipeline Step MyBatis-Plus 重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Pipeline Step 模块从 JdbcTemplate + String SQL 重构为基于 MyBatis-Plus 的完整 ORM 层，消除 SQL 注入风险，提升性能和可维护性。

**Architecture:** 引入 MyBatisPlusAbstractStep 泛型抽象基类，统一管理事务边界和批量操作；每个表对应一个 Entity 和 Mapper；复杂查询使用 XML 映射；简单查询使用 QueryWrapper。

**Tech Stack:** Spring Boot 2.1.2, MyBatis-Plus 3.x, MySQL 5.7+

**设计文档:** [2026-04-09-pipeline-mybatis-plus-refactor-design.md](../specs/2026-04-09-pipeline-mybatis-plus-refactor-design.md)

---

## 文件结构映射

### 新增文件

```
report-backend/src/main/java/com/report/
├── entity/
│   ├── ods/
│   │   ├── OsdSales.java
│   │   └── TestFlow.java
│   ├── dwd/
│   │   ├── Layer1Sales.java
│   │   └── DwdCleanTestFlow.java
│   └── dws/
│       ├── Layer2Summary.java
│       └── DwdTestFlowAgg.java
├── mapper/
│   ├── OsdSalesMapper.java
│   ├── Layer1SalesMapper.java
│   ├── Layer2SummaryMapper.java
│   ├── TestFlowMapper.java
│   ├── DwdCleanTestFlowMapper.java
│   └── DwdTestFlowAggMapper.java
└── pipeline/
    └── MyBatisPlusAbstractStep.java

report-backend/src/main/resources/mapper/
├── Layer1SalesMapper.xml
└── DwdCleanTestFlowMapper.xml
```

### 修改文件

```
report-backend/src/main/java/com/report/pipeline/step/
├── DataCleanseStep.java
├── DataAggregateStep.java
└── testFlow/
    ├── TestFlowCleanseStep.java
    └── TestFlowAggregateStep.java

report-backend/src/main/resources/
└── application.yml
```

### 移除文件

```
report-backend/src/main/java/com/report/pipeline/util/DataInsertHelper.java
report-backend/src/main/java/com/report/pipeline/AbstractStep.java (可选)
```

---

## Phase 1: 基础设施搭建

### Task 1: 创建 ODS 层实体类

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/ods/OsdSales.java`
- Create: `report-backend/src/main/java/com/report/entity/ods/TestFlow.java`

- [ ] **Step 1: 创建 OsdSales 实体类**

```java
package com.report.entity.ods;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("osd_sales")
public class OsdSales {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private BigDecimal amount;

    private LocalDate ptDt;
}
```

- [ ] **Step 2: 创建 TestFlow 实体类**

```java
package com.report.entity.ods;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("test_flow")
public class TestFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal amount;

    private LocalDate ptDt;
}
```

- [ ] **Step 3: 提交 ODS 实体类**

```bash
git add report-backend/src/main/java/com/report/entity/ods/
git commit -m "feat(entity): Add ODS layer entities (OsdSales, TestFlow)"
```

---

### Task 2: 创建 DWD 层实体类

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/dwd/Layer1Sales.java`
- Create: `report-backend/src/main/java/com/report/entity/dwd/DwdCleanTestFlow.java`

- [ ] **Step 1: 创建 Layer1Sales 实体类**

```java
package com.report.entity.dwd;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("layer_1_sales")
public class Layer1Sales {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private BigDecimal amount;

    private LocalDate ptDt;
}
```

- [ ] **Step 2: 创建 DwdCleanTestFlow 实体类**

```java
package com.report.entity.dwd;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("dwd_clean_tets_flow")
public class DwdCleanTestFlow {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal amount;

    private LocalDate ptDt;
}
```

- [ ] **Step 3: 提交 DWD 实体类**

```bash
git add report-backend/src/main/java/com/report/entity/dwd/
git commit -m "feat(entity): Add DWD layer entities (Layer1Sales, DwdCleanTestFlow)"
```

---

### Task 3: 创建 DWS 层实体类

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/dws/Layer2Summary.java`
- Create: `report-backend/src/main/java/com/report/entity/dws/DwdTestFlowAgg.java`

- [ ] **Step 1: 创建 Layer2Summary 实体类**

```java
package com.report.entity.dws;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("layer_2_summary")
public class Layer2Summary {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String productName;

    private BigDecimal totalAmount;

    private Integer orderCount;

    private LocalDate ptDt;
}
```

- [ ] **Step 2: 创建 DwdTestFlowAgg 实体类**

```java
package com.report.entity.dws;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("dwd_tets_flow_agg")
public class DwdTestFlowAgg {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private BigDecimal totalAmount;

    private LocalDate ptDt;
}
```

- [ ] **Step 3: 提交 DWS 实体类**

```bash
git add report-backend/src/main/java/com/report/entity/dws/
git commit -m "feat(entity): Add DWS layer entities (Layer2Summary, DwdTestFlowAgg)"
```

---

### Task 4: 创建 Mapper 接口

**Files:**
- Create: `report-backend/src/main/java/com/report/mapper/OsdSalesMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/Layer1SalesMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/Layer2SummaryMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/TestFlowMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/DwdCleanTestFlowMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/DwdTestFlowAggMapper.java`

- [ ] **Step 1: 创建 OsdSalesMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ods.OsdSales;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OsdSalesMapper extends BaseMapper<OsdSales> {
}
```

- [ ] **Step 2: 创建 Layer1SalesMapper（含聚合方法声明）**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dwd.Layer1Sales;
import com.report.entity.dws.Layer2Summary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface Layer1SalesMapper extends BaseMapper<Layer1Sales> {

    List<Layer2Summary> aggregateByProduct(@Param("partitionDate") LocalDate partitionDate);
}
```

- [ ] **Step 3: 创建 Layer2SummaryMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dws.Layer2Summary;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface Layer2SummaryMapper extends BaseMapper<Layer2Summary> {
}
```

- [ ] **Step 4: 创建 TestFlowMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ods.TestFlow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestFlowMapper extends BaseMapper<TestFlow> {
}
```

- [ ] **Step 5: 创建 DwdCleanTestFlowMapper（含聚合方法声明）**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dwd.DwdCleanTestFlow;
import com.report.entity.dws.DwdTestFlowAgg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DwdCleanTestFlowMapper extends BaseMapper<DwdCleanTestFlow> {

    List<DwdTestFlowAgg> aggregateByName(@Param("partitionDate") LocalDate partitionDate);
}
```

- [ ] **Step 6: 创建 DwdTestFlowAggMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dws.DwdTestFlowAgg;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DwdTestFlowAggMapper extends BaseMapper<DwdTestFlowAgg> {
}
```

- [ ] **Step 7: 提交 Mapper 接口**

```bash
git add report-backend/src/main/java/com/report/mapper/
git commit -m "feat(mapper): Add all Mapper interfaces for pipeline entities"
```

---

### Task 5: 创建 XML 映射文件

**Files:**
- Create: `report-backend/src/main/resources/mapper/Layer1SalesMapper.xml`
- Create: `report-backend/src/main/resources/mapper/DwdCleanTestFlowMapper.xml`

- [ ] **Step 1: 创建 Layer1SalesMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.report.mapper.Layer1SalesMapper">

    <select id="aggregateByProduct" resultType="com.report.entity.dws.Layer2Summary">
        SELECT
            product_name AS productName,
            SUM(amount) AS totalAmount,
            COUNT(*) AS orderCount,
            #{partitionDate} AS ptDt
        FROM layer_1_sales
        WHERE pt_dt = #{partitionDate}
        GROUP BY product_name
    </select>

</mapper>
```

- [ ] **Step 2: 创建 DwdCleanTestFlowMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
    "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.report.mapper.DwdCleanTestFlowMapper">

    <select id="aggregateByName" resultType="com.report.entity.dws.DwdTestFlowAgg">
        SELECT
            name,
            SUM(amount) AS totalAmount,
            #{partitionDate} AS ptDt
        FROM dwd_clean_tets_flow
        WHERE pt_dt = #{partitionDate}
        GROUP BY name
    </select>

</mapper>
```

- [ ] **Step 3: 提交 XML 映射文件**

```bash
git add report-backend/src/main/resources/mapper/
git commit -m "feat(mapper): Add XML mapping files for aggregate queries"
```

---

### Task 6: 创建 MyBatisPlusAbstractStep 抽象基类

**Files:**
- Create: `report-backend/src/main/java/com/report/pipeline/MyBatisPlusAbstractStep.java`

- [ ] **Step 1: 创建 MyBatisPlusAbstractStep**

```java
package com.report.pipeline;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.dto.StepContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDate;

@Slf4j
public abstract class MyBatisPlusAbstractStep implements PipelineStep {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public void execute(StepContext context) throws StepExecutionException {
        TransactionStatus status = transactionManager.getTransaction(
            new DefaultTransactionDefinition()
        );

        try {
            if (isOverwrite()) {
                clearPartition(getTargetTable(), context.getPartitionDate());
            }
            doExecute(context);
            transactionManager.commit(status);
            log.info("[{}] 执行成功", getStepName());
        } catch (Exception e) {
            transactionManager.rollback(status);
            log.error("[{}] 执行失败，已回滚: {}", getStepName(), e.getMessage());
            throw new StepExecutionException("Step执行失败: " + getStepName(), e);
        }
    }

    protected void clearPartition(String tableName, LocalDate partitionDate) {
        String safeTableName = tableName.replaceAll("[^a-zA-Z0-9_]", "");
        String sql = String.format("DELETE FROM %s WHERE pt_dt = ?", safeTableName);
        jdbcTemplate.update(sql, partitionDate.toString());
        log.info("[{}] 清空分区: {} pt_dt={}", getStepName(), safeTableName, partitionDate);
    }

    protected <T> void insertBatch(BaseMapper<T> mapper, java.util.List<T> dataList) {
        insertBatch(mapper, dataList, 500);
    }

    protected <T> void insertBatch(BaseMapper<T> mapper, java.util.List<T> dataList, int batchSize) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            java.util.List<T> batch = dataList.subList(i, end);

            for (T entity : batch) {
                mapper.insert(entity);
            }
        }
        log.info("[{}] 批量插入完成，共{}条", getStepName(), dataList.size());
    }

    protected abstract String getTableName();
    protected abstract void doExecute(StepContext context) throws StepExecutionException;

    @Override
    public String getTargetTable() {
        return getTableName();
    }

    @Override
    public boolean isOverwrite() {
        return true;
    }
}
```

- [ ] **Step 2: 提交抽象基类**

```bash
git add report-backend/src/main/java/com/report/pipeline/MyBatisPlusAbstractStep.java
git commit -m "feat(pipeline): Add MyBatisPlusAbstractStep with transaction and batch insert support"
```

---

### Task 7: 配置 MyBatis-Plus Mapper 扫描

**Files:**
- Modify: `report-backend/src/main/resources/application.yml`

- [ ] **Step 1: 添加 MyBatis-Plus 配置**

在 `application.yml` 中添加或确认以下配置：

```yaml
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.report.entity
  configuration:
    map-underscore-to-camel-case: true
```

- [ ] **Step 2: 提交配置变更**

```bash
git add report-backend/src/main/resources/application.yml
git commit -m "chore(config): Add MyBatis-Plus mapper locations configuration"
```

---

## Phase 2: Step 迁移

### Task 8: 重构 DataCleanseStep

**Files:**
- Modify: `report-backend/src/main/java/com/report/pipeline/step/DataCleanseStep.java`

- [ ] **Step 1: 重写 DataCleanseStep**

```java
package com.report.pipeline.step;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.dwd.Layer1Sales;
import com.report.entity.ods.OsdSales;
import com.report.entity.dto.StepContext;
import com.report.mapper.Layer1SalesMapper;
import com.report.mapper.OsdSalesMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DataCleanseStep extends MyBatisPlusAbstractStep {

    @Autowired
    private OsdSalesMapper osdSalesMapper;

    @Autowired
    private Layer1SalesMapper layer1SalesMapper;

    @Override
    public String getStepName() {
        return "数据清洗";
    }

    @Override
    protected String getTableName() {
        return "layer_1_sales";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        log.info("[数据清洗] 从 OSD 表读取数据，分区: {}", partitionDate);

        List<OsdSales> rawData = osdSalesMapper.selectList(
            new LambdaQueryWrapper<OsdSales>()
                .eq(OsdSales::getPtDt, partitionDate)
        );

        log.info("[数据清洗] 读取到 {} 行数据", rawData.size());

        List<Layer1Sales> cleansedData = rawData.stream()
            .map(this::cleanseRow)
            .collect(Collectors.toList());

        insertBatch(layer1SalesMapper, cleansedData);
        log.info("[数据清洗] 完成，写入 {} 行到 {}", cleansedData.size(), getTargetTable());
    }

    private Layer1Sales cleanseRow(OsdSales rawRow) {
        Layer1Sales cleansed = new Layer1Sales();
        cleansed.setProductName(rawRow.getProductName());

        if (rawRow.getAmount() == null || rawRow.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            cleansed.setAmount(BigDecimal.ZERO);
        } else {
            cleansed.setAmount(rawRow.getAmount());
        }

        cleansed.setPtDt(rawRow.getPtDt());
        return cleansed;
    }
}
```

- [ ] **Step 2: 提交 DataCleanseStep 重构**

```bash
git add report-backend/src/main/java/com/report/pipeline/step/DataCleanseStep.java
git commit -m "refactor(step): Migrate DataCleanseStep to MyBatis-Plus"
```

---

### Task 9: 重构 DataAggregateStep

**Files:**
- Modify: `report-backend/src/main/java/com/report/pipeline/step/DataAggregateStep.java`

- [ ] **Step 1: 重写 DataAggregateStep**

```java
package com.report.pipeline.step;

import com.report.entity.dws.Layer2Summary;
import com.report.entity.dto.StepContext;
import com.report.mapper.Layer1SalesMapper;
import com.report.mapper.Layer2SummaryMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class DataAggregateStep extends MyBatisPlusAbstractStep {

    @Autowired
    private Layer1SalesMapper layer1SalesMapper;

    @Autowired
    private Layer2SummaryMapper layer2SummaryMapper;

    @Override
    public String getStepName() {
        return "数据聚合";
    }

    @Override
    protected String getTableName() {
        return "layer_2_summary";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        log.info("[数据聚合] 从 DWD 表读取并聚合数据，分区: {}", partitionDate);

        List<Layer2Summary> aggregatedData = layer1SalesMapper.aggregateByProduct(partitionDate);

        aggregatedData.forEach(row -> {
            if (row.getTotalAmount() == null) {
                row.setTotalAmount(BigDecimal.ZERO);
            }
            if (row.getOrderCount() == null) {
                row.setOrderCount(0);
            }
        });

        log.info("[数据聚合] 聚合得到 {} 行数据", aggregatedData.size());

        insertBatch(layer2SummaryMapper, aggregatedData);
        log.info("[数据聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}
```

- [ ] **Step 2: 提交 DataAggregateStep 重构**

```bash
git add report-backend/src/main/java/com/report/pipeline/step/DataAggregateStep.java
git commit -m "refactor(step): Migrate DataAggregateStep to MyBatis-Plus with XML mapping"
```

---

### Task 10: 重构 TestFlowCleanseStep

**Files:**
- Modify: `report-backend/src/main/java/com/report/pipeline/step/testFlow/TestFlowCleanseStep.java`

- [ ] **Step 1: 重写 TestFlowCleanseStep**

```java
package com.report.pipeline.step.testFlow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.dwd.DwdCleanTestFlow;
import com.report.entity.ods.TestFlow;
import com.report.entity.dto.StepContext;
import com.report.mapper.DwdCleanTestFlowMapper;
import com.report.mapper.TestFlowMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class TestFlowCleanseStep extends MyBatisPlusAbstractStep {

    @Autowired
    private TestFlowMapper testFlowMapper;

    @Autowired
    private DwdCleanTestFlowMapper dwdCleanTestFlowMapper;

    @Override
    public String getStepName() {
        return "TestFlow清洗";
    }

    @Override
    protected String getTableName() {
        return "dwd_clean_tets_flow";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        log.info("[TestFlow清洗] 从 test_flow 读取数据，分区: {}", partitionDate);

        List<TestFlow> rawData = testFlowMapper.selectList(
            new LambdaQueryWrapper<TestFlow>()
                .eq(TestFlow::getPtDt, partitionDate)
        );

        log.info("[TestFlow清洗] 读取到 {} 行数据", rawData.size());

        List<DwdCleanTestFlow> cleansedData = rawData.stream()
            .map(this::transformRow)
            .collect(Collectors.toList());

        insertBatch(dwdCleanTestFlowMapper, cleansedData);
        log.info("[TestFlow清洗] 完成，写入 {} 行到 {}", cleansedData.size(), getTargetTable());
    }

    private DwdCleanTestFlow transformRow(TestFlow rawRow) {
        DwdCleanTestFlow cleansed = new DwdCleanTestFlow();
        cleansed.setName(rawRow.getName());
        cleansed.setAmount(rawRow.getAmount());
        cleansed.setPtDt(rawRow.getPtDt());
        return cleansed;
    }
}
```

- [ ] **Step 2: 提交 TestFlowCleanseStep 重构**

```bash
git add report-backend/src/main/java/com/report/pipeline/step/testFlow/TestFlowCleanseStep.java
git commit -m "refactor(step): Migrate TestFlowCleanseStep to MyBatis-Plus"
```

---

### Task 11: 重构 TestFlowAggregateStep

**Files:**
- Modify: `report-backend/src/main/java/com/report/pipeline/step/testFlow/TestFlowAggregateStep.java`

- [ ] **Step 1: 重写 TestFlowAggregateStep**

```java
package com.report.pipeline.step.testFlow;

import com.report.entity.dws.DwdTestFlowAgg;
import com.report.entity.dto.StepContext;
import com.report.mapper.DwdCleanTestFlowMapper;
import com.report.mapper.DwdTestFlowAggMapper;
import com.report.pipeline.MyBatisPlusAbstractStep;
import com.report.pipeline.StepExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class TestFlowAggregateStep extends MyBatisPlusAbstractStep {

    @Autowired
    private DwdCleanTestFlowMapper dwdCleanTestFlowMapper;

    @Autowired
    private DwdTestFlowAggMapper dwdTestFlowAggMapper;

    @Override
    public String getStepName() {
        return "TestFlow聚合";
    }

    @Override
    protected String getTableName() {
        return "dwd_tets_flow_agg";
    }

    @Override
    protected void doExecute(StepContext context) throws StepExecutionException {
        LocalDate partitionDate = context.getPartitionDate();

        log.info("[TestFlow聚合] 从 dwd_clean_tets_flow 读取并聚合数据，分区: {}", partitionDate);

        List<DwdTestFlowAgg> aggregatedData = dwdCleanTestFlowMapper.aggregateByName(partitionDate);

        aggregatedData.forEach(row -> {
            if (row.getTotalAmount() == null) {
                row.setTotalAmount(BigDecimal.ZERO);
            }
        });

        log.info("[TestFlow聚合] 聚合得到 {} 行数据", aggregatedData.size());

        insertBatch(dwdTestFlowAggMapper, aggregatedData);
        log.info("[TestFlow聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}
```

- [ ] **Step 2: 提交 TestFlowAggregateStep 重构**

```bash
git add report-backend/src/main/java/com/report/pipeline/step/testFlow/TestFlowAggregateStep.java
git commit -m "refactor(step): Migrate TestFlowAggregateStep to MyBatis-Plus with XML mapping"
```

---

## Phase 3: 清理与验证

### Task 12: 移除废弃代码

**Files:**
- Delete: `report-backend/src/main/java/com/report/pipeline/util/DataInsertHelper.java`
- Delete: `report-backend/src/main/java/com/report/pipeline/AbstractStep.java`

- [ ] **Step 1: 删除 DataInsertHelper**

```bash
git rm report-backend/src/main/java/com/report/pipeline/util/DataInsertHelper.java
```

- [ ] **Step 2: 删除 AbstractStep**

```bash
git rm report-backend/src/main/java/com/report/pipeline/AbstractStep.java
```

- [ ] **Step 3: 提交删除**

```bash
git commit -m "refactor(pipeline): Remove deprecated DataInsertHelper and AbstractStep"
```

---

### Task 13: 编译验证

**Files:**
- 无文件变更，仅验证

- [ ] **Step 1: 编译后端项目**

```bash
cd report-backend && mvn compile -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 检查编译错误**

如果有编译错误，检查：
1. Entity 类是否正确导入
2. Mapper 注解是否正确
3. XML 文件 namespace 是否匹配

---

### Task 14: 运行测试

**Files:**
- 无文件变更，仅验证

- [ ] **Step 1: 运行所有测试**

```bash
cd report-backend && mvn test
```

Expected: 所有测试通过

- [ ] **Step 2: 检查测试失败**

如果有测试失败：
1. 检查是否是 Pipeline 相关测试
2. 确认 Mock 对象是否需要更新
3. 确认测试数据是否符合新 Entity 结构

---

### Task 15: 启动服务验证

**Files:**
- 无文件变更，仅验证

- [ ] **Step 1: 启动后端服务**

```bash
./scripts/start.sh backend
```

- [ ] **Step 2: 检查启动日志**

确认以下内容：
1. MyBatis-Plus Mapper 扫描成功
2. XML 映射文件加载成功
3. 无 Bean 注入失败

- [ ] **Step 3: 测试 Pipeline 执行**

调用 Pipeline 执行 API：
```bash
curl -X POST http://localhost:8080/api/pipeline/sales_data_pipeline/execute?partitionDate=2026-04-09
```

Expected: 返回任务 ID，Pipeline 执行成功

---

### Task 16: 最终提交

**Files:**
- 无文件变更，整理提交

- [ ] **Step 1: 检查 Git 状态**

```bash
git status
```

Expected: 无未提交文件

- [ ] **Step 2: 推送到远程仓库**

```bash
git push origin master
```

---

## 验收标准

| 检查项 | 预期结果 |
|--------|---------|
| 编译通过 | `mvn compile` 成功 |
| 测试通过 | 所有单元测试通过 |
| 服务启动 | 后端服务正常启动 |
| Pipeline 执行 | Pipeline 可以正常执行并写入数据 |
| SQL 注入防护 | 无字符串拼接 SQL |
| 批量插入 | 使用 `insertBatch()` 方法 |
| 事务回滚 | Step 异常时数据回滚 |
| 幂等性 | 重复执行 Pipeline 结果一致 |

---

## 回滚方案

如果重构后出现问题，可通过以下方式回滚：

```bash
git revert HEAD~16  # 回滚最近16个提交
```

或使用 Git Worktree 创建独立分支进行重构，验证通过后再合并。
