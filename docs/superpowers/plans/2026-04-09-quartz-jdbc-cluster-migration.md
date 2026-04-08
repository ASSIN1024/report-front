# Quartz JDBC 集群模式迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 Quartz 调度器从内存模式迁移到 JDBC 集群模式，支持多实例运行且不重复执行任务，同时实现开发环境(MySQL)与生产环境(GaussDB)的配置隔离。

**Architecture:** 使用 Spring Profile 机制隔离不同环境的数据库配置；Quartz 使用 JDBC JobStore 实现集群调度；TriggerStateManager 从内存实现改为数据库持久化；通过数据库行锁和唯一索引保证任务执行的幂等性。

**Tech Stack:** Spring Boot 2.1.2, Quartz Scheduler (JDBC Mode), MyBatis-Plus 3.4.3, MySQL 5.7+ / GaussDB, Druid Connection Pool

---

## 文件结构映射

### 新建文件
| 文件路径 | 职责 |
|---------|------|
| `report-backend/src/main/resources/application-prod.yml` | 生产环境配置（GaussDB连接、Quartz集群参数） |
| `report-backend/src/main/java/com/report/trigger/DatabaseTriggerStateManager.java` | 数据库持久化的触发器状态管理器 |
| `report-backend/src/main/java/com/report/entity/TriggerStateRecord.java` | 触发器状态实体类 |
| `report-backend/src/main/java/com/report/mapper/TriggerStateRecordMapper.java` | 触发器状态Mapper接口 |
| `report-backend/src/main/resources/mapper/TriggerStateRecordMapper.xml` | 触发器状态Mapper XML |

### 修改文件
| 文件路径 | 修改内容 |
|---------|---------|
| `report-backend/src/main/resources/application.yml` | 添加 Quartz JDBC 集群配置 |
| `report-backend/src/main/resources/application-dev.yml` | 扩容连接池、添加 Quartz 配置 |
| `report-backend/src/main/resources/schema.sql` | 添加 trigger_state_record 表、processed_file 唯一索引 |
| `report-backend/src/main/java/com/report/trigger/TriggerStateManager.java` | 改为接口定义 |
| `report-backend/src/main/java/com/report/trigger/TriggerJob.java` | 注入 DatabaseTriggerStateManager |
| `report-backend/src/main/java/com/report/common/config/QuartzConfig.java` | 添加集群相关配置 |

---

## Task 1: 创建触发器状态实体类和Mapper

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/TriggerStateRecord.java`
- Create: `report-backend/src/main/java/com/report/mapper/TriggerStateRecordMapper.java`
- Create: `report-backend/src/main/resources/mapper/TriggerStateRecordMapper.xml`

- [ ] **Step 1: 创建 TriggerStateRecord 实体类**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("trigger_state_record")
public class TriggerStateRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String triggerCode;

    private Integer retryCount;

    private Date lastCheckTime;

    private Boolean triggered;

    private String instanceId;

    private Date createTime;

    private Date updateTime;

    @Version
    private Integer version;
}
```

- [ ] **Step 2: 创建 TriggerStateRecordMapper 接口**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.TriggerStateRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TriggerStateRecordMapper extends BaseMapper<TriggerStateRecord> {

    int resetTriggered(@Param("triggerCode") String triggerCode);

    int updateTriggeredWithVersion(
        @Param("triggerCode") String triggerCode,
        @Param("triggered") Boolean triggered,
        @Param("instanceId") String instanceId,
        @Param("version") Integer version
    );
}
```

- [ ] **Step 3: 创建 TriggerStateRecordMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.report.mapper.TriggerStateRecordMapper">

    <update id="resetTriggered">
        UPDATE trigger_state_record
        SET triggered = 0,
            retry_count = 0,
            update_time = NOW()
        WHERE trigger_code = #{triggerCode}
    </update>

    <update id="updateTriggeredWithVersion">
        UPDATE trigger_state_record
        SET triggered = #{triggered},
            instance_id = #{instanceId},
            retry_count = retry_count + 1,
            update_time = NOW(),
            version = version + 1
        WHERE trigger_code = #{triggerCode}
          AND version = #{version}
    </update>

</mapper>
```

- [ ] **Step 4: 验证文件创建**

Run: `ls -la report-backend/src/main/java/com/report/entity/TriggerStateRecord.java report-backend/src/main/java/com/report/mapper/TriggerStateRecordMapper.java report-backend/src/main/resources/mapper/TriggerStateRecordMapper.xml`

Expected: 三个文件都存在

---

## Task 2: 重构 TriggerStateManager 为接口并实现数据库版本

**Files:**
- Modify: `report-backend/src/main/java/com/report/trigger/TriggerStateManager.java`
- Create: `report-backend/src/main/java/com/report/trigger/DatabaseTriggerStateManager.java`

- [ ] **Step 1: 将 TriggerStateManager 改为接口**

```java
package com.report.trigger;

public interface TriggerStateManager {

    TriggerState getOrCreate(String triggerCode);

    void reset(String triggerCode);

    void incrementRetryCount(String triggerCode);

    void setTriggered(String triggerCode, boolean triggered);
}
```

- [ ] **Step 2: 创建 DatabaseTriggerStateManager 实现类**

```java
package com.report.trigger;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.TriggerStateRecord;
import com.report.mapper.TriggerStateRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

@Slf4j
@Service
public class DatabaseTriggerStateManager implements TriggerStateManager {

    @Autowired
    private TriggerStateRecordMapper triggerStateRecordMapper;

    @Value("${spring.application.name:report-backend}")
    private String applicationName;

    private String instanceId;

    public DatabaseTriggerStateManager() {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            String pid = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            this.instanceId = hostname + "-" + pid;
        } catch (UnknownHostException e) {
            this.instanceId = "unknown-" + System.currentTimeMillis();
        }
    }

    @Override
    @Transactional
    public TriggerState getOrCreate(String triggerCode) {
        LambdaQueryWrapper<TriggerStateRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TriggerStateRecord::getTriggerCode, triggerCode);

        TriggerStateRecord record = triggerStateRecordMapper.selectOne(wrapper);

        if (record == null) {
            record = new TriggerStateRecord();
            record.setTriggerCode(triggerCode);
            record.setRetryCount(0);
            record.setTriggered(false);
            record.setInstanceId(instanceId);
            record.setCreateTime(new Date());
            record.setUpdateTime(new Date());
            record.setVersion(0);
            try {
                triggerStateRecordMapper.insert(record);
            } catch (Exception e) {
                log.warn("并发插入触发器状态记录，尝试重新查询: {}", triggerCode);
                record = triggerStateRecordMapper.selectOne(wrapper);
                if (record == null) {
                    throw new RuntimeException("无法创建触发器状态记录: " + triggerCode);
                }
            }
        }

        return convertToState(record);
    }

    @Override
    @Transactional
    public void reset(String triggerCode) {
        int updated = triggerStateRecordMapper.resetTriggered(triggerCode);
        log.debug("重置触发器状态: triggerCode={}, updated={}", triggerCode, updated);
    }

    @Override
    @Transactional
    public void incrementRetryCount(String triggerCode) {
        TriggerStateRecord record = getRecordByCode(triggerCode);
        if (record != null) {
            record.setRetryCount(record.getRetryCount() + 1);
            record.setLastCheckTime(new Date());
            record.setUpdateTime(new Date());
            triggerStateRecordMapper.updateById(record);
        }
    }

    @Override
    @Transactional
    public void setTriggered(String triggerCode, boolean triggered) {
        TriggerStateRecord record = getRecordByCode(triggerCode);
        if (record != null) {
            int updated = triggerStateRecordMapper.updateTriggeredWithVersion(
                triggerCode, triggered, instanceId, record.getVersion()
            );
            if (updated == 0) {
                log.warn("触发器状态更新失败(版本冲突)，可能被其他实例修改: triggerCode={}", triggerCode);
            }
        }
    }

    private TriggerStateRecord getRecordByCode(String triggerCode) {
        LambdaQueryWrapper<TriggerStateRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TriggerStateRecord::getTriggerCode, triggerCode);
        return triggerStateRecordMapper.selectOne(wrapper);
    }

    private TriggerState convertToState(TriggerStateRecord record) {
        TriggerState state = new TriggerState();
        state.setTriggerCode(record.getTriggerCode());
        state.setRetryCount(record.getRetryCount());
        state.setLastCheckTime(record.getLastCheckTime());
        state.setTriggered(record.getTriggered());
        return state;
    }
}
```

- [ ] **Step 3: 更新 TriggerState 类添加 setter 方法**

检查 `TriggerState.java` 是否已有 setter 方法（Lombok @Data 应该已提供），确认无需修改。

- [ ] **Step 4: 验证编译**

Run: `cd report-backend && mvn compile -q`

Expected: BUILD SUCCESS

---

## Task 3: 更新 TriggerJob 使用新的状态管理器

**Files:**
- Modify: `report-backend/src/main/java/com/report/trigger/TriggerJob.java`

- [ ] **Step 1: 修改 TriggerJob 注入方式**

找到 TriggerJob.java 中的 `@Autowired private TriggerStateManager stateManager;` 确认注入的是接口类型。如果已经是接口则无需修改。

检查文件内容：
```java
@Autowired
private TriggerStateManager stateManager;
```

如果类型是具体的实现类，改为接口类型。

- [ ] **Step 2: 添加 setTriggered 调用**

在 `triggerPipeline` 方法中，成功触发后调用 `stateManager.setTriggered(trigger.getTriggerCode(), true);`

修改后的 `triggerPipeline` 方法：

```java
private void triggerPipeline(TriggerConfig trigger, Date partitionDate, int dataCount, int retryCount) {
    Long pipelineTaskId = null;
    String status = "FAILED";
    String errorMessage = null;

    try {
        log.info("[{}] 触发Pipeline: {}", trigger.getTriggerName(), trigger.getPipelineCode());
        java.time.LocalDate localDate = new java.sql.Date(partitionDate.getTime()).toLocalDate();
        pipelineTaskId = pipelineExecutor.execute(trigger.getPipelineCode(), localDate);
        triggerService.updateLastTriggerTime(trigger.getTriggerCode());
        stateManager.setTriggered(trigger.getTriggerCode(), true);
        status = "TRIGGERED";
    } catch (Exception e) {
        log.error("[{}] Pipeline触发失败: {}", trigger.getTriggerName(), e.getMessage());
        errorMessage = e.getMessage();
    }

    logTriggerExecution(trigger, partitionDate, dataCount, status, pipelineTaskId, retryCount);
}
```

- [ ] **Step 3: 修改 processTrigger 方法使用新的 incrementRetryCount**

在 `processTrigger` 方法中，将 `state.setRetryCount(state.getRetryCount() + 1);` 改为调用 `stateManager.incrementRetryCount(trigger.getTriggerCode());`

修改后的 `processTrigger` 方法：

```java
private void processTrigger(TriggerConfig trigger) {
    TriggerState state = stateManager.getOrCreate(trigger.getTriggerCode());
    Date partitionDate = new Date();

    log.debug("[{}] 开始检查数据，轮询间隔: {}秒，重试: {}/{}",
        trigger.getTriggerName(),
        trigger.getPollIntervalSeconds(),
        state.getRetryCount(),
        trigger.getMaxRetries());

    int dataCount = triggerService.checkDataExists(trigger, partitionDate);

    if (dataCount > 0) {
        log.info("[{}] 检测到数据: {} 行，分区: {}", trigger.getTriggerName(), dataCount, partitionDate);

        if (!state.isTriggered()) {
            triggerPipeline(trigger, partitionDate, dataCount, state.getRetryCount());
            stateManager.reset(trigger.getTriggerCode());
        }
    } else {
        stateManager.incrementRetryCount(trigger.getTriggerCode());
        state.setLastCheckTime(new Date());

        if (state.getRetryCount() > trigger.getMaxRetries()) {
            log.warn("[{}] 等待数据超时，分区: {}，重试次数: {}",
                trigger.getTriggerName(), partitionDate, state.getRetryCount());

            logTriggerExecution(trigger, partitionDate, 0, "SKIPPED", null, state.getRetryCount());
            markTaskSkipped(trigger, partitionDate, "数据就绪超时");
            stateManager.reset(trigger.getTriggerCode());
        } else {
            log.debug("[{}] 等待数据中，分区: {}，重试: {}/{}",
                trigger.getTriggerName(), partitionDate,
                state.getRetryCount(), trigger.getMaxRetries());

            logTriggerExecution(trigger, partitionDate, 0, "WAITING", null, state.getRetryCount());
        }
    }
}
```

- [ ] **Step 4: 验证编译**

Run: `cd report-backend && mvn compile -q`

Expected: BUILD SUCCESS

---

## Task 4: 更新数据库 Schema

**Files:**
- Modify: `report-backend/src/main/resources/schema.sql`

- [ ] **Step 1: 添加 trigger_state_record 表 DDL**

在 schema.sql 文件末尾添加：

```sql
-- 触发器状态持久化表（支持集群模式）
CREATE TABLE IF NOT EXISTS trigger_state_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    trigger_code VARCHAR(100) NOT NULL COMMENT '触发器编码',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    last_check_time DATETIME COMMENT '最后检查时间',
    triggered TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已触发: 0-未触发, 1-已触发',
    instance_id VARCHAR(200) COMMENT '实例标识',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trigger_code (trigger_code),
    KEY idx_triggered (triggered),
    KEY idx_instance_id (instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='触发器状态持久化表';

-- 为 processed_file 表添加唯一索引（防止重复处理）
ALTER TABLE processed_file
ADD UNIQUE INDEX uk_report_file (report_config_id, file_name);
```

- [ ] **Step 2: 验证 SQL 语法**

Run: `mysql -u root -proot123 report_db -e "SELECT 1" 2>/dev/null && echo "MySQL连接正常" || echo "MySQL连接失败，请手动验证SQL"`

Expected: MySQL连接正常 或 手动验证提示

---

## Task 5: 修改主配置文件启用 Quartz JDBC 集群

**Files:**
- Modify: `report-backend/src/main/resources/application.yml`

- [ ] **Step 1: 修改 Quartz 配置为 JDBC 集群模式**

将现有的 Quartz 配置：

```yaml
spring:
  quartz:
    job-store-type: memory
    wait-for-jobs-to-complete-on-shutdown: true
    overwrite-existing-jobs: true
    startup-delay: 10s
```

替换为：

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always
    wait-for-jobs-to-complete-on-shutdown: true
    overwrite-existing-jobs: true
    startup-delay: 10s
    properties:
      org:
        quartz:
          scheduler:
            instanceName: ReportScheduler
            instanceId: AUTO
          jobStore:
            class: org.quartz.impl.jdbcjobstore.JobStoreTX
            driverDelegateClass: org.quartz.impl.jdbcjobstore.StdJDBCDelegate
            tablePrefix: QRTZ_
            isClustered: true
            clusterCheckinInterval: 20000
            misfireThreshold: 60000
          threadPool:
            class: org.quartz.simpl.SimpleThreadPool
            threadCount: 5
            threadPriority: 5
```

- [ ] **Step 2: 验证配置文件格式**

Run: `cd report-backend && python3 -c "import yaml; yaml.safe_load(open('src/main/resources/application.yml'))" && echo "YAML格式正确"`

Expected: YAML格式正确

---

## Task 6: 创建生产环境配置文件 (GaussDB)

**Files:**
- Create: `report-backend/src/main/resources/application-prod.yml`

- [ ] **Step 1: 创建 application-prod.yml**

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://${GAUSSDB_HOST:localhost}:${GAUSSDB_PORT:5432}/${GAUSSDB_DB:report_db}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${GAUSSDB_USER:report_user}
    password: ${GAUSSDB_PASSWORD:password}
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      filters: stat,wall
      web-stat-filter:
        enabled: true
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: ${DRUID_USER:admin}
        login-password: ${DRUID_PASSWORD:admin}

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.report.entity
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.report: info
    org.quartz: info

ftp:
  built-in:
    enabled: false
```

- [ ] **Step 2: 验证文件创建**

Run: `ls -la report-backend/src/main/resources/application-prod.yml`

Expected: 文件存在

---

## Task 7: 优化开发环境配置 (MySQL + 连接池扩容)

**Files:**
- Modify: `report-backend/src/main/resources/application-dev.yml`

- [ ] **Step 1: 扩容 Druid 连接池**

将 `max-active: 20` 改为 `max-active: 50`，同时调整初始连接数：

```yaml
druid:
  initial-size: 10
  min-idle: 10
  max-active: 50
```

- [ ] **Step 2: 添加 Quartz 相关日志级别**

在 application-dev.yml 末尾添加：

```yaml
logging:
  level:
    com.report: debug
    org.quartz: debug
```

- [ ] **Step 3: 验证配置文件格式**

Run: `cd report-backend && python3 -c "import yaml; yaml.safe_load(open('src/main/resources/application-dev.yml'))" && echo "YAML格式正确"`

Expected: YAML格式正确

---

## Task 8: 添加 Quartz 集群配置 Bean

**Files:**
- Modify: `report-backend/src/main/java/com/report/common/config/QuartzConfig.java`

- [ ] **Step 1: 添加 @DisallowConcurrentExecution 注解到 Job 类**

在 FtpScanJob 和 TriggerJob 类上添加 `@DisallowConcurrentExecution` 注解：

```java
@Slf4j
@Component
@DisallowConcurrentExecution
public class FtpScanJob implements Job {
    // ...
}
```

```java
@Slf4j
@Component
@DisallowConcurrentExecution
public class TriggerJob implements Job {
    // ...
}
```

- [ ] **Step 2: 验证编译**

Run: `cd report-backend && mvn compile -q`

Expected: BUILD SUCCESS

---

## Task 9: 验证完整编译和测试

**Files:**
- 无新建文件

- [ ] **Step 1: 完整编译项目**

Run: `cd report-backend && mvn clean compile -q`

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行单元测试**

Run: `cd report-backend && mvn test -q`

Expected: Tests run: X, Failures: 0, Errors: 0

- [ ] **Step 3: 打包验证**

Run: `cd report-backend && mvn package -DskipTests -q`

Expected: BUILD SUCCESS，生成 target/report-backend-1.0.0.jar

---

## Task 10: 更新项目文档

**Files:**
- Modify: `AGENTS.md`

- [ ] **Step 1: 更新技术栈说明**

在 AGENTS.md 的技术栈部分添加 Quartz 集群配置说明：

```markdown
### 任务调度
- **框架**: Quartz Scheduler
- **模式**: JDBC 集群模式（支持多实例）
- **集群心跳**: 20秒
- **Misfire阈值**: 60秒
```

- [ ] **Step 2: 添加环境配置说明**

在快速链接部分添加：

```markdown
**环境配置**:
- 开发环境: application-dev.yml (MySQL)
- 生产环境: application-prod.yml (GaussDB)
```

---

## 自我审查清单

### 1. Spec 覆盖检查
- [x] MySQL/GaussDB 环境隔离 → Task 6
- [x] Quartz JDBC 集群模式 → Task 5
- [x] TriggerStateManager 数据库持久化 → Task 2
- [x] 连接池扩容 → Task 7
- [x] 唯一索引防重 → Task 4

### 2. 占位符扫描
- [x] 无 "TBD"、"TODO"、"implement later"
- [x] 无 "add appropriate error handling" 等模糊描述
- [x] 所有代码步骤包含完整代码块
- [x] 所有命令步骤包含完整命令

### 3. 类型一致性检查
- [x] TriggerStateManager 接口定义与实现类方法签名一致
- [x] TriggerStateRecord 实体类字段与 Mapper XML 一致
- [x] application.yml 配置项名称正确

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-04-09-quartz-jdbc-cluster-migration.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
