# Pipeline Step 模块 MyBatis-Plus 重构设计文档

> **文档版本**: V1.0
> **创建日期**: 2026-04-09
> **状态**: 待评审
> **关联任务**: H-PIPELINE-MYBATIS

---

## 1. 背景与目标

### 1.1 问题背景

当前 Pipeline Step 模块使用 `JdbcTemplate` + 字符串拼接 SQL 的方式操作数据库，存在以下问题：

| 问题类型 | 具体表现 | 影响范围 |
|---------|---------|---------|
| 安全性 | `String.format()` 拼接SQL，存在注入风险 | AbstractStep.clearPartition(), DataAggregateStep |
| 性能 | DataInsertHelper 逐行 INSERT，无批量操作 | 所有 Step 的数据写入 |
| 可维护性 | SQL 散落在 Java 代码中，复杂查询难以维护 | 聚合/多表关联场景 |
| 类型安全 | 使用 Map<String, Object> 存储结果，编译期无法检查 | 所有查询 |

### 1.2 重构目标

1. 消除 SQL 注入风险，统一使用参数绑定
2. 引入批量插入能力，提升十万级数据量性能
3. 将复杂 SQL 集中管理到 XML 映射文件
4. 实现强类型 Entity，支持 IDE 补全和编译期检查
5. 保证 Step 级别事务 + 幂等性

---

## 2. 架构设计

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        PipelineExecutor                              │
│                     (流水线执行器 - 不变)                              │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                           Pipeline                                   │
│              (流水线定义 - 不变)                                      │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │ 调用
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MyBatisPlusAbstractStep                           │
│                  (新增: ORM抽象基类)                                   │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ - 注入 BaseMapper<T> 子类泛型                                │   │
│  │ - 提供 insertBatch() 批量插入方法                            │   │
│  │ - 提供 selectByPartition() 分区查询方法                      │   │
│  │ - 保留原有 execute() 幂等逻辑 + 事务边界                     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────┬───────────────────────────────────┘
                                  │
          ┌───────────────────────┼───────────────────────┐
          ▼                       ▼                       ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ DataCleanseStep  │  │DataAggregateStep │  │ TestFlowCleanse  │
│   (改造)         │  │    (改造)         │  │   (改造)          │
│                  │  │                  │  │                  │
│ - OsdSalesMapper │  │- Layer1SalesMapper│  │ - TestFlowMapper │
│ - Layer1SalesMapper│ │- Layer2SummaryMapper│ │ - DwdCleanMapper │
└──────────────────┘  └──────────────────┘  └──────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    MyBatis-Plus Mapper 层                           │
│                                                                     │
│  Entity (com.report.entity.*)                                      │
│  ├── ods/OsdSales.java                                             │
│  ├── ods/TestFlow.java                                             │
│  ├── dwd/Layer1Sales.java                                          │
│  ├── dwd/DwdCleanTestFlow.java                                     │
│  ├── dws/Layer2Summary.java                                        │
│  └── dws/DwdTestFlowAgg.java                                       │
│                                                                     │
│  Mapper (com.report.mapper.*)                                      │
│  ├── OsdSalesMapper.java                                           │
│  ├── Layer1SalesMapper.java                                        │
│  ├── Layer2SummaryMapper.java                                      │
│  ├── TestFlowMapper.java                                           │
│  ├── DwdCleanTestFlowMapper.java                                   │
│  └── DwdTestFlowAggMapper.java                                     │
│                                                                     │
│  XML Mapping                                                       │
│  └── mapper/*.xml (仅复杂查询需要)                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### 2.2 分层说明

| 层级 | 组件 | 职责 |
|------|------|------|
| **Pipeline层** | Pipeline, PipelineExecutor | 流水线编排与执行调度（不变） |
| **Step层** | MyBatisPlusAbstractStep + 各Step实现 | 业务逻辑封装 |
| **Entity层** | 各表实体类 | 数据模型定义，类型安全保证 |
| **Mapper层** | Mapper接口 + XML映射 | 数据访问抽象，SQL集中管理 |

---

## 3. 核心组件详细设计

### 3.1 MyBatisPlusAbstractStep 抽象基类

**文件位置**: `com/report/pipeline/MyBatisPlusAbstractStep.java`

```java
package com.report.pipeline;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.dto.StepContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.time.LocalDate;
import java.util.List;

@Slf4j
public abstract class MyBatisPlusAbstractStep<T> implements PipelineStep {

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

    /**
     * 批量插入，默认每批500行
     */
    protected void insertBatch(BaseMapper<T> mapper, List<T> dataList) {
        insertBatch(mapper, dataList, 500);
    }

    /**
     * 批量插入，支持自定义批次大小
     */
    protected void insertBatch(BaseMapper<T> mapper, List<T> dataList, int batchSize) {
        if (dataList == null || dataList.isEmpty()) {
            return;
        }

        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<T> batch = dataList.subList(i, end);

            for (T entity : batch) {
                mapper.insert(entity);
            }
        }
        log.info("[{}] 批量插入完成，共{}条", getStepName(), dataList.size());
    }

    /**
     * 根据分区日期查询
     */
    protected List<T> selectByPartition(BaseMapper<T> mapper, LocalDate partitionDate) {
        LambdaQueryWrapper<T> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(T::getPtDt, partitionDate);
        return mapper.selectList(wrapper);
    }

    protected abstract String getTableName();
    protected abstract void doExecute(StepContext context) throws StepExecutionException;
}
```

### 3.2 实体类设计

#### ODS 层实体

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

#### DWD 层实体

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

### 3.3 Mapper 接口设计

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ods.OsdSales;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface OsdSalesMapper extends BaseMapper<OsdSales> {

    default List<OsdSales> selectByPartition(LocalDate partitionDate) {
        LambdaQueryWrapper<OsdSales> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OsdSales::getPtDt, partitionDate);
        return selectList(wrapper);
    }
}
```

对于复杂查询（如聚合），在 XML 中定义：

```xml
<!-- resources/mapper/Layer1SalesMapper.xml -->
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

对应的 Mapper 接口：

```java
@Mapper
public interface Layer1SalesMapper extends BaseMapper<Layer1Sales> {

    List<Layer2Summary> aggregateByProduct(@Param("partitionDate") LocalDate partitionDate);
}
```

---

## 4. Step 重构示例

### 4.1 DataCleanseStep 重构后

```java
package com.report.pipeline.step;

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
public class DataCleanseStep extends MyBatisPlusAbstractStep<Layer1Sales> {

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

        // 使用 QueryWrapper 查询，类型安全
        List<OsdSales> rawData = osdSalesMapper.selectList(
            new LambdaQueryWrapper<OsdSales>()
                .eq(OsdSales::getPtDt, partitionDate)
        );

        log.info("[数据清洗] 读取到 {} 行数据", rawData.size());

        // 对象转换，编译期检查
        List<Layer1Sales> cleansedData = rawData.stream()
            .map(this::cleanseRow)
            .collect(Collectors.toList());

        // 批量插入
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

### 4.2 DataAggregateStep 重构后

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
public class DataAggregateStep extends MyBatisPlusAbstractStep<Layer2Summary> {

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

        // 使用 XML 映射的复杂查询
        List<Layer2Summary> aggregatedData =
            layer1SalesMapper.aggregateByProduct(partitionDate);

        // 后处理：处理 NULL 值
        aggregatedData.forEach(row -> {
            if (row.getTotalAmount() == null) {
                row.setTotalAmount(BigDecimal.ZERO);
            }
            if (row.getOrderCount() == null) {
                row.setOrderCount(0);
            }
        });

        log.info("[数据聚合] 聚合得到 {} 行数据", aggregatedData.size());

        // 批量插入
        insertBatch(layer2SummaryMapper, aggregatedData);
        log.info("[数据聚合] 完成，写入 {} 行到 {}", aggregatedData.size(), getTargetTable());
    }
}
```

---

## 5. 数据访问方式规范

### 5.1 查询方式选择指南

| 场景 | 推荐方式 | 示例代码 |
|------|---------|---------|
| **简单条件查询** | QueryWrapper | `mapper.selectList(new LambdaQueryWrapper<X>().eq(X::getField, value))` |
| **复杂 SQL**（多表关联/聚合/子查询） | XML 映射 | `<select>` 在 XML 中定义 |
| **动态条件拼接** | XML + `<if>` 标签 | 多个可选参数的灵活组合 |
| **批量插入** | 循环调用 insert() | `insertBatch(mapper, list)` |
| **单条 CRUD** | MyBatis-Plus 内置方法 | `mapper.insert(entity)` / `mapper.selectById(id)` |

### 5.2 动态 SQL 示例

```xml
<select id="queryOrderDetails" resultType="OrderDetailVO">
    SELECT
        o.id,
        o.orderNo,
        o.amount,
        p.name AS productName,
        c.name AS customerName
    FROM orders o
    LEFT JOIN products p ON o.productId = p.id
    LEFT JOIN customers c ON o.customerId = c.id
    <where>
        <if test="startDate != null">
            AND o.createTime >= #{startDate}
        </if>
        <if test="endDate != null">
            AND o.createTime &lt;= #{endDate}
        </if>
        <if test="status != null and status != ''">
            AND o.status = #{status}
        </if>
    </where>
    ORDER BY o.createTime DESC
</select>
```

---

## 6. 文件变更清单

### 6.1 新增文件

| 文件路径 | 说明 |
|---------|------|
| `pipeline/MyBatisPlusAbstractStep.java` | 泛型抽象基类 |
| `entity/ods/OsdSales.java` | OSD销售原始数据实体 |
| `entity/ods/TestFlow.java` | 测试流原始数据实体 |
| `entity/dwd/Layer1Sales.java` | DWD明细数据实体 |
| `entity/dwd/DwdCleanTestFlow.java` | 清洗后测试流实体 |
| `entity/dws/Layer2Summary.java` | DWS汇总数据实体 |
| `entity/dws/DwdTestFlowAgg.java` | 聚合后测试流实体 |
| `mapper/OsdSalesMapper.java` | OSD销售 Mapper 接口 |
| `mapper/Layer1SalesMapper.java` | DWD明细 Mapper 接口 |
| `mapper/Layer2SummaryMapper.java` | DWS汇总 Mapper 接口 |
| `mapper/TestFlowMapper.java` | 测试流 Mapper 接口 |
| `mapper/DwdCleanTestFlowMapper.java` | 清洗后 Mapper 接口 |
| `mapper/DwdTestFlowAggMapper.java` | 聚合后 Mapper 接口 |
| `resources/mapper/Layer1SalesMapper.xml` | 聚合查询 XML 映射 |
| `resources/mapper/DwdCleanTestFlowMapper.xml` | 测试流聚合 XML 映射 |

### 6.2 修改文件

| 文件路径 | 变更内容 |
|---------|---------|
| `pipeline/step/DataCleanseStep.java` | 改为继承 MyBatisPlusAbstractStep，使用 Mapper |
| `pipeline/step/DataAggregateStep.java` | 改为继承 MyBatisPlusAbstractStep，使用 Mapper |
| `pipeline/step/testFlow/TestFlowCleanseStep.java` | 同上改造 |
| `pipeline/step/testFlow/TestFlowAggregateStep.java` | 同上改造 |
| `pipeline/util/DataInsertHelper.java` | 移除（功能由 MyBatisPlusAbstractStep 替代） |
| `application.yml` | 配置 mybatis-plus.mapper-locations |

### 6.3 可选移除文件

| 文件路径 | 处理方式 |
|---------|---------|
| `pipeline/AbstractStep.java` | 保留但标记为 @Deprecated，或直接移除 |

---

## 7. 事务与幂等性设计

### 7.1 幂等性保证机制

每个 Step 的执行流程：

```
开始执行
    ↓
[幂等检查] isOverwrite() == true ?
    ↓ Yes
clearPartition() → DELETE FROM target_table WHERE pt_dt = ?
    ↓
doExecute() → 业务逻辑处理
    ↓
insertBatch() → 批量插入目标表
    ↓
事务提交 COMMIT
```

**关键点**：
- DELETE 在事务内执行，如果后续 INSERT 失败则整体回滚
- 重做时先清空再写入，保证最终状态一致
- 幂等性由 Step 层控制，不依赖外部机制

### 7.2 异常重做场景分析

| 场景 | 第1次执行 | 第2次执行 | 最终状态 |
|------|----------|----------|---------|
| 正常完成 | DELETE→INSERT 成功 | DELETE(空)→INSERT(覆盖) | 数据一致 |
| INSERT 异常 | DELETE→INSERT(异常) 回滚 | DELETE(空)→INSERT(成功) | 数据正确 |
| 清洗逻辑异常 | DELETE→异常 回滚 | DELETE(空)→成功 | 原始数据不变 |

### 7.3 事务配置

```yaml
# application.yml
spring:
  datasource:
    # 保持现有配置不变

mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.report.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

## 8. 性能优化要点

### 8.1 批量插入优化

| 方式 | 单次插入 | 批量插入 (500条/批) |
|------|---------|-------------------|
| 10万条数据 | ~100秒 | ~10秒 |
| 数据库往返次数 | 100,000 次 | 200 次 |

### 8.2 查询性能建议

1. 确保 `pt_dt` 字段有索引
2. 大表查询使用分页或游标
3. 聚合查询考虑物化视图或预计算

---

## 9. 测试策略

### 9.1 单元测试覆盖

| 测试目标 | 测试内容 |
|---------|---------|
| MyBatisPlusAbstractStep | clearPartition、insertBatch、selectByPartition |
| DataCleanseStep | 数据读取、清洗转换、批量写入 |
| DataAggregateStep | 聚合查询、NULL值处理、批量写入 |
| Entity | 字段映射正确性 |
| Mapper | 自定义方法正确性 |

### 9.2 集成测试

- 完整 Pipeline 执行流程测试
- 事务回滚验证
- 幂等性验证（重复执行）

---

## 10. 迁移计划概要

### Phase 1: 基础设施搭建
- 创建 Entity 类
- 创建 Mapper 接口
- 实现 MyBatisPlusAbstractStep
- 配置 MyBatis-Plus

### Phase 2: Step 逐步迁移
- DataCleanseStep 迁移
- DataAggregateStep 迁移
- TestFlow 系列 Step 迁移

### Phase 3: 清理与验证
- 移除 DataInsertHelper
- 移除旧 AbstractStep 或标记废弃
- 全量测试验证
- 性能基准测试

---

## 附录 A: 数据库表结构参考

```sql
-- OSD 原始数据层
CREATE TABLE osd_sales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100),
    amount DECIMAL(15,2),
    pt_dt DATE,
    INDEX idx_pt_dt (pt_dt)
);

CREATE TABLE test_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    amount DECIMAL(15,2),
    pt_dt DATE,
    INDEX idx_pt_dt (pt_dt)
);

-- DWD 明细数据层
CREATE TABLE layer_1_sales (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100),
    amount DECIMAL(15,2),
    pt_dt DATE,
    INDEX idx_pt_dt (pt_dt)
);

CREATE TABLE dwd_clean_tets_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    amount DECIMAL(15,2),
    pt_dt DATE,
    INDEX idx_pt_dt (pt_dt)
);

-- DWS 汇总数据层
CREATE TABLE layer_2_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_name VARCHAR(100),
    total_amount DECIMAL(15,2),
    order_count INT,
    pt_dt DATE,
    INDEX idx_pt_dt (pt_dt),
    UNIQUE KEY uk_product_date (product_name, pt_dt)
);

CREATE TABLE dwd_tets_flow_agg (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100),
    total_amount DECIMAL(15,2),
    pt_dt DATE,
    INDEX idx_pt_dt (pt_dt),
    UNIQUE KEY uk_name_date (name, pt_dt)
);
```
