# FTP 报表数据转换中间件 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的FTP报表数据转换中间件，包括FTP扫描、Excel解析、字段映射、批量打包、消费监控和轮转机制

**Architecture:**
- 后端采用 Spring Boot 架构，复用现有 FtpConfig、ReportConfig 实体
- 新增 PackingConfig、PackingBatch、AlertRecord 实体管理打包流程
- ConsumptionWatcher 通过轮询检测 outputs.zip 是否存在来判断消费完成
- 打包服务按配置大小限制自动分批，支持排队等待轮转

**Tech Stack:** Spring Boot 2.1.2, MyBatis-Plus 3.x, Apache POI, Apache Commons Net

---

## 文件结构

```
report-backend/src/main/java/com/report/
├── packing/
│   ├── entity/
│   │   ├── PackingConfig.java        # 打包配置实体
│   │   ├── PackingBatch.java        # 批次实体
│   │   └── AlertRecord.java         # 告警实体
│   ├── mapper/
│   │   ├── PackingConfigMapper.java
│   │   ├── PackingBatchMapper.java
│   │   └── AlertRecordMapper.java
│   ├── service/
│   │   ├── PackingConfigService.java
│   │   ├── PackingBatchService.java
│   │   ├── PackingService.java      # 核心打包服务
│   │   └── ConsumptionWatcher.java  # 消费监控器
│   ├── manager/
│   │   └── PackingManager.java      # 打包管理器
│   ├── generator/
│   │   └── ConfigTableGenerator.java # 配置表生成器
│   └── job/
│       └── PackingJob.java          # 定时打包任务
```

---

## Task 1: 数据库表创建

**Files:**
- Modify: `report-backend/src/main/resources/schema.sql`

- [ ] **Step 1: 添加 packing_config 表**

在 schema.sql 末尾添加：

```sql
-- ----------------------------
-- Table structure for packing_config
-- ----------------------------
DROP TABLE IF EXISTS `packing_config`;
CREATE TABLE `packing_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key` varchar(100) NOT NULL COMMENT '配置键',
    `config_value` varchar(500) DEFAULT NULL COMMENT '配置值',
    `config_type` varchar(50) DEFAULT NULL COMMENT '配置类型',
    `description` varchar(200) DEFAULT NULL COMMENT '描述',
    `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '删除标记',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包配置表';

-- 初始化默认配置
INSERT INTO `packing_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('max_package_size', '209715200', 'NUMBER', '最大包大小(字节)，默认200MB'),
('upload_dir', '/data/ftp-root/for-upload', 'STRING', '上传目录'),
('done_dir', '/data/ftp-root/done', 'STRING', '完成目录'),
('fixed_filename', 'outputs.zip', 'STRING', '固定文件名'),
('polling_interval', '30', 'NUMBER', '消费轮询间隔(秒)'),
('scan_interval', '300', 'NUMBER', '扫描间隔(秒)');
```

- [ ] **Step 2: 添加 packing_batch 表**

```sql
-- ----------------------------
-- Table structure for packing_batch
-- ----------------------------
DROP TABLE IF EXISTS `packing_batch`;
CREATE TABLE `packing_batch` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `batch_no` varchar(50) NOT NULL COMMENT '批次号',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待打包, UPLOADING-上传中, CONSUMING-消费中, DONE-已完成',
    `total_size` bigint DEFAULT 0 COMMENT '总大小(字节)',
    `file_count` int DEFAULT 0 COMMENT '文件数量',
    `for_upload_path` varchar(500) DEFAULT NULL COMMENT '上传路径',
    `done_dir_path` varchar(500) DEFAULT NULL COMMENT 'Done目录路径',
    `start_time` datetime DEFAULT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '结束时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_batch_no` (`batch_no`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打包批次表';
```

- [ ] **Step 3: 添加 alert_record 表**

```sql
-- ----------------------------
-- Table structure for alert_record
-- ----------------------------
DROP TABLE IF EXISTS `alert_record`;
CREATE TABLE `alert_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `alert_type` varchar(20) NOT NULL COMMENT '告警类型',
    `file_name` varchar(200) DEFAULT NULL COMMENT '相关文件名',
    `report_config_id` bigint DEFAULT NULL COMMENT '关联报表配置ID',
    `reason` varchar(500) DEFAULT NULL COMMENT '告警原因',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待处理, RESOLVED-已解决, IGNORED-已忽略',
    `resolve_time` datetime DEFAULT NULL COMMENT '解决时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_alert_type` (`alert_type`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';
```

- [ ] **Step 4: 添加 report_config 扩展字段**

```sql
-- ----------------------------
-- Table structure for report_config extension
-- ----------------------------
ALTER TABLE `report_config`
ADD COLUMN `target_table_type` varchar(20) DEFAULT NULL COMMENT '目标表类型: hive/mpp' AFTER `load_mode`,
ADD COLUMN `target_db_name` varchar(128) DEFAULT NULL COMMENT '目标库名' AFTER `target_table_type`,
ADD COLUMN `is_overseas` tinyint DEFAULT 0 COMMENT '是否境外: 0-否, 1-是' AFTER `target_db_name`,
ADD COLUMN `field_type_json` text COMMENT '字段类型JSON' AFTER `is_overseas`,
ADD COLUMN `spark_executor_num` int DEFAULT 4 COMMENT 'Spark executor数量' AFTER `field_type_json`,
ADD COLUMN `spark_executor_cores` int DEFAULT 4 COMMENT 'Spark executor核数' AFTER `spark_executor_num`,
ADD COLUMN `spark_executor_memory` varchar(20) DEFAULT '8G' COMMENT 'Spark executor内存' AFTER `spark_executor_cores`,
ADD COLUMN `spark_driver_num` int DEFAULT 2 COMMENT 'Spark driver数量' AFTER `spark_executor_memory`,
ADD COLUMN `spark_driver_memory` varchar(20) DEFAULT '2G' COMMENT 'Spark driver内存' AFTER `spark_driver_num`;
```

- [ ] **Step 5: 执行数据库更新**

Run: 连接到MySQL执行以上SQL语句

---

## Task 2: 实体类创建

**Files:**
- Create: `report-backend/src/main/java/com/report/packing/entity/PackingConfig.java`
- Create: `report-backend/src/main/java/com/report/packing/entity/PackingBatch.java`
- Create: `report-backend/src/main/java/com/report/packing/entity/AlertRecord.java`

- [ ] **Step 1: 创建 PackingConfig 实体**

```java
package com.report.packing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("packing_config")
public class PackingConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String configKey;
    private String configValue;
    private String configType;
    private String description;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- [ ] **Step 2: 创建 PackingBatch 实体**

```java
package com.report.packing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("packing_batch")
public class PackingBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String batchNo;
    private String status;
    private Long totalSize;
    private Integer fileCount;
    private String forUploadPath;
    private String doneDirPath;
    private Date startTime;
    private Date endTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_UPLOADING = "UPLOADING";
    public static final String STATUS_CONSUMING = "CONSUMING";
    public static final String STATUS_DONE = "DONE";
}
```

- [ ] **Step 3: 创建 AlertRecord 实体**

```java
package com.report.packing.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("alert_record")
public class AlertRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String alertType;
    private String fileName;
    private Long reportConfigId;
    private String reason;
    private String status;
    private Date resolveTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    public static final String TYPE_PARSE_ERROR = "PARSE_ERROR";
    public static final String TYPE_MAPPING_ERROR = "MAPPING_ERROR";
    public static final String TYPE_PACKING_ERROR = "PACKING_ERROR";
    public static final String TYPE_CONSUMPTION_TIMEOUT = "CONSUMPTION_TIMEOUT";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_RESOLVED = "RESOLVED";
    public static final String STATUS_IGNORED = "IGNORED";
}
```

- [ ] **Step 4: 编译验证**

Run: `cd report-backend && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 3: Mapper接口创建

**Files:**
- Create: `report-backend/src/main/java/com/report/packing/mapper/PackingConfigMapper.java`
- Create: `report-backend/src/main/java/com/report/packing/mapper/PackingBatchMapper.java`
- Create: `report-backend/src/main/java/com/report/packing/mapper/AlertRecordMapper.java`

- [ ] **Step 1: 创建 PackingConfigMapper**

```java
package com.report.packing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.packing.entity.PackingConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PackingConfigMapper extends BaseMapper<PackingConfig> {
}
```

- [ ] **Step 2: 创建 PackingBatchMapper**

```java
package com.report.packing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.packing.entity.PackingBatch;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PackingBatchMapper extends BaseMapper<PackingBatch> {
}
```

- [ ] **Step 3: 创建 AlertRecordMapper**

```java
package com.report.packing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.packing.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {
}
```

- [ ] **Step 4: 编译验证**

Run: `cd report-backend && mvn compile -q`
Expected: BUILD SUCCESS

---

## Task 4: 核心服务创建

**Files:**
- Create: `report-backend/src/main/java/com/report/packing/service/PackingConfigService.java`
- Create: `report-backend/src/main/java/com/report/packing/service/PackingService.java`
- Create: `report-backend/src/main/java/com/report/packing/service/ConsumptionWatcher.java`
- Create: `report-backend/src/main/java/com/report/packing/manager/PackingManager.java`
- Create: `report-backend/src/main/java/com/report/packing/generator/ConfigTableGenerator.java`

- [ ] **Step 1: 创建 PackingConfigService**

```java
package com.report.packing.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.report.packing.entity.PackingConfig;

import java.util.Map;

public interface PackingConfigService extends IService<PackingConfig> {
    String getStringValue(String key, String defaultValue);
    Long getLongValue(String key, Long defaultValue);
    Integer getIntValue(String key, Integer defaultValue);
    Map<String, String> getAllConfigs();
    void updateConfig(String key, String value);
}
```

- [ ] **Step 2: 创建 PackingService 接口**

```java
package com.report.packing.service;

import com.report.entity.ProcessedFile;

import java.util.List;

public interface PackingService {
    String pack(List<Long> processedFileIds);
    void upload(String batchNo);
    boolean canUpload();
    boolean isBeingConsumed();
    String getUploadDir();
    String getDoneDir();
    String getFixedFilename();
    Long getMaxPackageSize();
}
```

- [ ] **Step 3: 创建 ConsumptionWatcher**

```java
package com.report.packing.service;

public interface ConsumptionWatcher {
    void start(String batchNo);
    void stop();
    boolean isConsumed();
    boolean isRunning();
    String getCurrentBatchNo();
}
```

- [ ] **Step 4: 创建 ConfigTableGenerator**

```java
package com.report.packing.generator;

import com.report.entity.ProcessedFile;

import java.io.File;
import java.util.List;

public interface ConfigTableGenerator {
    File generate(List<Long> processedFileIds, String batchNo);
}
```

- [ ] **Step 5: 创建 PackingManager**

```java
package com.report.packing.manager;

import com.report.entity.ProcessedFile;

import java.util.List;

public interface PackingManager {
    void executePacking();
    boolean checkAndWaitForConsumption();
    void triggerNextPacking();
}
```

---

## Task 5: 定时任务创建

**Files:**
- Create: `report-backend/src/main/java/com/report/packing/job/PackingJob.java`

- [ ] **Step 1: 创建 PackingJob**

```java
package com.report.packing.job;

import com.report.packing.manager.PackingManager;
import org.quartz.DisallowConcurrentExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@DisallowConcurrentExecution
public class PackingJob {
    private static final Logger log = LoggerFactory.getLogger(PackingJob.class);

    @Autowired
    private PackingManager packingManager;

    public void execute() {
        log.info("PackingJob triggered");
        try {
            packingManager.executePacking();
        } catch (Exception e) {
            log.error("PackingJob execution failed", e);
        }
    }
}
```

- [ ] **Step 2: 注册定时任务**

在现有的 Job 注册逻辑中添加 PackingJob 的注册（约在 FtpScanJob 注册附近）

---

## Task 6: 管理接口创建

**Files:**
- Create: `report-backend/src/main/java/com/report/packing/controller/PackingController.java`
- Create: `report-backend/src/main/java/com/report/packing/controller/PackingAlertController.java`

- [ ] **Step 1: 创建 PackingController**

```java
package com.report.packing.controller;

import com.report.packing.manager.PackingManager;
import com.report.packing.service.PackingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/packing")
public class PackingController {

    @Autowired
    private PackingConfigService packingConfigService;

    @Autowired
    private PackingManager packingManager;

    @GetMapping("/config")
    public Map<String, String> getConfig() {
        return packingConfigService.getAllConfigs();
    }

    @PutMapping("/config")
    public void updateConfig(@RequestParam String key, @RequestParam String value) {
        packingConfigService.updateConfig(key, value);
    }

    @PostMapping("/trigger")
    public String triggerPacking() {
        packingManager.executePacking();
        return "Packing triggered";
    }
}
```

- [ ] **Step 2: 创建 PackingAlertController**

```java
package com.report.packing.controller;

import com.baomidou.mybatisplus.extension.service.IService;
import com.report.packing.entity.AlertRecord;
import com.report.packing.mapper.AlertRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/packing/alerts")
public class PackingAlertController {

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @GetMapping
    public List<AlertRecord> getAlerts(@RequestParam(required = false) String status) {
        if (status == null) {
            return alertRecordMapper.selectList(null);
        }
        return alertRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AlertRecord>()
                .eq("status", status)
        );
    }

    @PutMapping("/{id}/resolve")
    public void resolveAlert(@PathVariable Long id) {
        AlertRecord alert = alertRecordMapper.selectById(id);
        if (alert != null) {
            alert.setStatus(AlertRecord.STATUS_RESOLVED);
            alert.setResolveTime(new Date());
            alertRecordMapper.updateById(alert);
        }
    }
}
```

---

## Task 7: 数据库初始化数据

**Files:**
- Modify: `report-backend/src/main/resources/data.sql`（如存在）

- [ ] **Step 1: 确认初始化SQL**

如 data.sql 存在，添加打包配置的初始化数据

---

## Task 8: 前端界面创建

**Files:**
- Create: `src/views/packing/PackingConfig.vue`
- Create: `src/views/packing/PackingMonitor.vue`
- Create: `src/views/packing/AlertList.vue`
- Modify: `src/router/index.js`

- [ ] **Step 1: 创建 PackingConfig.vue**

打包配置管理界面，包含：
- max_package_size 输入框
- upload_dir 输入框
- done_dir 输入框
- fixed_filename 输入框
- polling_interval 输入框

- [ ] **Step 2: 创建 PackingMonitor.vue**

批次监控界面，包含：
- 批次列表（批次号、状态、文件数、大小、时间）
- 手动触发按钮

- [ ] **Step 3: 创建 AlertList.vue**

告警列表界面，包含：
- 告警类型筛选
- 状态筛选
- 告警列表
- 标记已解决按钮

- [ ] **Step 4: 更新路由**

```javascript
{
  path: '/packing',
  component: Layout,
  children: [
    { path: 'config', component: () => import('@/views/packing/PackingConfig') },
    { path: 'monitor', component: () => import('@/views/packing/PackingMonitor') },
    { path: 'alerts', component: () => import('@/views/packing/AlertList') }
  ]
}
```

---

## Task 9: 集成测试

**Files:**
- Create: `report-backend/src/test/java/com/report/packing/PackingServiceTest.java`
- Create: `report-backend/src/test/java/com/report/packing/ConsumptionWatcherTest.java`

- [ ] **Step 1: 创建 PackingServiceTest**

```java
@Test
public void testPack_Success() {
    // 测试打包成功
}

@Test
public void testCanUpload_WhenFileNotExists() {
    // 测试文件不存在时可以上传
}

@Test
public void testCanUpload_WhenFileExists() {
    // 测试文件存在时不能上传
}
```

- [ ] **Step 2: 创建 ConsumptionWatcherTest**

```java
@Test
public void testIsConsumed_WhenFileNotExists() {
    // 测试文件不存在时消费完成
}
```

---

## Task 10: 编译与验证

- [ ] **Step 1: 后端编译**

Run: `cd report-backend && mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端编译**

Run: `cd src && npm run build`
Expected: 无错误

- [ ] **Step 3: 启动验证**

Run: `./scripts/start.sh all`
Expected: 服务正常启动

---

## 实施检查清单

- [ ] Task 1: 数据库表创建
- [ ] Task 2: 实体类创建
- [ ] Task 3: Mapper接口创建
- [ ] Task 4: 核心服务创建
- [ ] Task 5: 定时任务创建
- [ ] Task 6: 管理接口创建
- [ ] Task 7: 数据库初始化
- [ ] Task 8: 前端界面创建
- [ ] Task 9: 集成测试
- [ ] Task 10: 编译验证

---

**Plan saved to:** `docs/superpowers/plans/2026-04-28-ftp-data-pipeline-plan.md`
