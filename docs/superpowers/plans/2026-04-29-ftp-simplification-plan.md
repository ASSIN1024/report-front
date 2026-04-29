# FTP配置简化重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 废弃外部FTP配置体系，统一使用内置FTP，扫描路径迁移至报表配置

**Architecture:**
- 移除 `FtpConfig` 实体及相关后端代码
- 移除前端 FTP 配置页面及相关路由
- `report_config` 表新增 `scan_path` 字段
- 所有扫描功能改为读取 `report_config.scan_path`

**Tech Stack:** Spring Boot, MyBatis-Plus, Vue 2, MySQL

---

## 文件变更清单

### 待删除文件
- `report-backend/src/main/java/com/report/controller/FtpConfigController.java`
- `report-backend/src/main/java/com/report/service/FtpConfigService.java`
- `report-backend/src/main/java/com/report/service/impl/FtpConfigServiceImpl.java`
- `report-backend/src/main/java/com/report/mapper/FtpConfigMapper.java`
- `report-backend/src/main/java/com/report/entity/FtpConfig.java`
- `report-backend/src/main/resources/mapper/FtpConfigMapper.xml`
- `src/views/ftp/FtpConfig.vue`
- `src/api/ftpConfig.js`

### 待修改文件
- `report-backend/src/main/java/com/report/entity/ReportConfig.java` - 新增scanPath字段
- `report-backend/src/main/java/com/report/job/FtpScanJob.java` - 移除外部FTP扫描
- `report-backend/src/main/java/com/report/controller/TaskController.java` - 使用内置FTP
- `report-backend/src/main/java/com/report/controller/ReportConfigController.java` - 使用内置FTP
- `report-backend/src/main/resources/schema.sql` - 移除ftp_config表，report_config新增字段
- `report-backend/src/main/resources/schema-gaussdb.sql` - 同上
- `src/router/index.js` - 移除FTP路由
- `src/views/report/components/ReportConfig.vue` - 新增scanPath配置

---

## Task 1: 删除FtpConfig后端代码

**Files:**
- Delete: `report-backend/src/main/java/com/report/controller/FtpConfigController.java`
- Delete: `report-backend/src/main/java/com/report/service/FtpConfigService.java`
- Delete: `report-backend/src/main/java/com/report/service/impl/FtpConfigServiceImpl.java`
- Delete: `report-backend/src/main/java/com/report/mapper/FtpConfigMapper.java`
- Delete: `report-backend/src/main/resources/mapper/FtpConfigMapper.xml`
- Delete: `report-backend/src/main/java/com/report/entity/FtpConfig.java`

- [ ] **Step 1: 删除FtpConfigController.java**
```bash
rm /home/nova/projects/report-front/report-backend/src/main/java/com/report/controller/FtpConfigController.java
```

- [ ] **Step 2: 删除FtpConfigService接口和实现**
```bash
rm /home/nova/projects/report-front/report-backend/src/main/java/com/report/service/FtpConfigService.java
rm /home/nova/projects/report-front/report-backend/src/main/java/com/report/service/impl/FtpConfigServiceImpl.java
```

- [ ] **Step 3: 删除FtpConfigMapper**
```bash
rm /home/nova/projects/report-front/report-backend/src/main/java/com/report/mapper/FtpConfigMapper.java
rm /home/nova/projects/report-front/report-backend/src/main/resources/mapper/FtpConfigMapper.xml
```

- [ ] **Step 4: 删除FtpConfig实体**
```bash
rm /home/nova/projects/report-front/report-backend/src/main/java/com/report/entity/FtpConfig.java
```

- [ ] **Step 5: 提交变更**
```bash
git add -A && git commit -m "refactor: remove FtpConfig backend code

- Remove FtpConfigController, FtpConfigService, FtpConfigMapper
- Remove FtpConfig entity and MyBatis XML
- Related to H-FTP-REFACTOR"
```

---

## Task 2: 修改FtpScanJob - 移除外部FTP扫描

**Files:**
- Modify: `report-backend/src/main/java/com/report/job/FtpScanJob.java`

- [ ] **Step 1: 修改FtpScanJob.java**

替换原有文件内容，移除外部FTP扫描相关代码，仅保留内置FTP扫描：

```java
package com.report.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.entity.ReportConfig;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import com.report.service.ProcessedFileService;
import com.report.service.ReportConfigService;
import com.report.util.FileNameDateExtractor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;

@Slf4j
@Component
@DisallowConcurrentExecution
public class FtpScanJob implements Job {

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private ProcessedFileService processedFileService;

    @Autowired
    private MiddlewareEngine middlewareEngine;

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired(required = false)
    private EmbeddedFtpServer embeddedFtpServer;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("FTP扫描任务开始执行");

        if (embeddedFtpServer == null || !embeddedFtpServer.isRunning()) {
            log.info("内置FTP服务未运行，跳过扫描");
            return;
        }

        scanBuiltInFtp();

        log.info("FTP扫描任务执行完成");
    }

    private void scanBuiltInFtp() {
        if (builtInFtpConfigService == null) {
            log.warn("内置FTP服务未配置");
            return;
        }

        BuiltInFtpConfig config = builtInFtpConfigService.getConfig();
        if (config == null || !config.getEnabled()) {
            log.info("内置FTP未启用");
            return;
        }

        log.info("开始扫描内置FTP目录: {}", config.getRootDirectory());

        File ftpRoot = new File(config.getRootDirectory());

        List<ReportConfig> reportConfigs = reportConfigService.list(
            new LambdaQueryWrapper<ReportConfig>()
                .eq(ReportConfig::getStatus, 1)
                .eq(ReportConfig::getDeleted, 0)
        );

        if (reportConfigs.isEmpty()) {
            log.info("没有启用的报表配置");
            return;
        }

        for (ReportConfig reportConfig : reportConfigs) {
            scanReportDirectory(reportConfig, ftpRoot);
        }
    }

    private void scanReportDirectory(ReportConfig reportConfig, File ftpRoot) {
        String scanPath = reportConfig.getScanPath();
        if (scanPath == null || scanPath.isEmpty()) {
            scanPath = "/upload";
        }

        File scanDir = new File(ftpRoot, scanPath);
        if (!scanDir.exists() || !scanDir.isDirectory()) {
            log.info("扫描目录不存在: {}", scanDir.getAbsolutePath());
            return;
        }

        String pattern = reportConfig.getFilePattern();
        String fileRegex = pattern.replace("*", ".*").replace("?", ".");

        File[] files = scanDir.listFiles((dir, name) -> name.matches(fileRegex));
        if (files == null || files.length == 0) {
            log.info("扫描目录没有匹配的文件: {}", reportConfig.getFilePattern());
            return;
        }

        log.info("扫描到 {} 个匹配文件", files.length);

        for (File file : files) {
            String fileName = file.getName();
            if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
                log.info("文件已处理过，跳过: {}", fileName);
                continue;
            }

            log.info("检测到新文件: {}, 报表配置: {}", fileName, reportConfig.getReportName());

            MatchedFile matchedFile = new MatchedFile();
            matchedFile.setFileName(fileName);
            matchedFile.setFilePath(file.getAbsolutePath());
            matchedFile.setReportConfigId(reportConfig.getId());
            matchedFile.setLocalFile(file);
            LocalDate date = FileNameDateExtractor.extractDate(fileName);
            matchedFile.setPtDt(date != null ? date.toString() : null);

            try {
                middlewareEngine.processFile(matchedFile, reportConfig);
            } catch (Exception e) {
                log.error("文件处理失败: {}", fileName, e);
            }
        }
    }
}
```

- [ ] **Step 2: 提交变更**
```bash
git add -A && git commit -m "refactor: simplify FtpScanJob to use built-in FTP only

- Remove external FTP scan logic
- Scan path now comes from report_config.scan_path
- Related to H-FTP-REFACTOR"
```

---

## Task 3: 修改ReportConfig实体 - 新增scanPath字段

**Files:**
- Modify: `report-backend/src/main/java/com/report/entity/ReportConfig.java`

- [ ] **Step 1: 修改ReportConfig.java**

在 `ftpConfigId` 字段后添加 `scanPath` 字段：

```java
// 在 ftpConfigId 字段后添加
private String scanPath;
```

完整实体类变更（仅展示新增字段部分）：
```java
private Long ftpConfigId;

private String scanPath;  // 新增字段

private String filePattern;
```

- [ ] **Step 2: 提交变更**
```bash
git add -A && git commit -m "feat: add scanPath field to ReportConfig

- Add scanPath field for specifying FTP scan directory
- Related to H-FTP-REFACTOR"
```

---

## Task 4: 修改ReportConfigController - 使用内置FTP

**Files:**
- Modify: `report-backend/src/main/java/com/report/controller/ReportConfigController.java`

- [ ] **Step 1: 修改ReportConfigController.java**

移除 `FtpConfig` 相关引用，修改 `/scan` 接口使用内置FTP：

需要修改的imports:
```java
// 删除这些import
import com.report.entity.FtpConfig;
import com.report.service.FtpConfigService;
import com.report.util.FtpUtil;
import org.apache.commons.net.ftp.FTPClient;

// 保留需要的imports
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
```

需要修改的autowired字段：
```java
// 删除
@Autowired
private FtpConfigService ftpConfigService;

// 添加
@Autowired(required = false)
private BuiltInFtpConfigService builtInFtpConfigService;

@Autowired(required = false)
private EmbeddedFtpServer embeddedFtpServer;
```

需要修改的save和update方法：
```java
// 在save和update方法中添加scanPath字段的处理
config.setScanPath(dto.getScanPath());
```

需要修改的scan方法（完全重写）：
```java
@PostMapping("/{id}/scan")
public Result<Map<String, Object>> scan(@PathVariable Long id) {
    ReportConfig config = reportConfigService.getById(id);
    if (config == null) {
        return Result.fail("报表配置不存在");
    }

    if (embeddedFtpServer == null || !embeddedFtpServer.isRunning()) {
        return Result.fail("内置FTP服务未运行");
    }

    BuiltInFtpConfig ftpConfig = builtInFtpConfigService.getConfig();
    if (ftpConfig == null) {
        return Result.fail("FTP配置不存在");
    }

    TaskExecution task = taskService.createTask("SCAN", "立即扫描-" + config.getReportName(), id, null, null);
    Long taskId = task.getId();
    logService.logInfo(taskId, "开始扫描内置FTP目录...");

    File tempFile = null;
    try {
        String scanPath = config.getScanPath();
        if (scanPath == null || scanPath.isEmpty()) {
            scanPath = "/upload";
        }

        File ftpRoot = new File(ftpConfig.getRootDirectory());
        File scanDir = new File(ftpRoot, scanPath);

        if (!scanDir.exists() || !scanDir.isDirectory()) {
            taskService.finishTask(taskId, "FAILED", "扫描目录不存在: " + scanDir.getAbsolutePath());
            logService.logError(taskId, "扫描目录不存在");
            return Result.fail("扫描目录不存在");
        }

        logService.logInfo(taskId, "扫描目录: " + scanDir.getAbsolutePath());

        String pattern = config.getFilePattern();
        String fileRegex = pattern.replace("*", ".*").replace("?", ".");

        File[] files = scanDir.listFiles((dir, name) -> name.matches(fileRegex));
        if (files == null || files.length == 0) {
            taskService.finishTask(taskId, "FAILED", "目录中没有找到匹配的文件");
            logService.logError(taskId, "目录中没有找到匹配的文件");
            return Result.fail("目录中没有找到匹配的文件");
        }

        File targetFile = files[0];
        String matchedFileName = targetFile.getName();
        logService.logInfo(taskId, "找到文件: " + matchedFileName);

        tempFile = File.createTempFile("scan_", "_" + matchedFileName);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            java.nio.file.Files.copy(targetFile.toPath(), fos);
        }

        MatchedFile matchedFile = new MatchedFile();
        matchedFile.setFileName(matchedFileName);
        matchedFile.setFilePath(targetFile.getAbsolutePath());
        matchedFile.setReportConfigId(id);
        matchedFile.setLocalFile(tempFile);
        LocalDate date = FileNameDateExtractor.extractDate(matchedFileName);
        matchedFile.setPtDt(date != null ? date.toString() : null);
        matchedFile.setTaskId(taskId);

        middlewareEngine.processFile(matchedFile, config);

        taskService.finishTask(taskId, "SUCCESS", null);

        Map<String, Object> result = new HashMap<>();
        result.put("reportConfigId", id);
        result.put("fileName", matchedFileName);
        result.put("status", "PROCESSED");
        result.put("taskId", taskId);
        return Result.success(result);

    } catch (Exception e) {
        log.error("扫描任务失败", e);
        taskService.finishTask(taskId, "FAILED", e.getMessage());
        logService.logError(taskId, "扫描异常: " + e.getMessage());
        return Result.fail("扫描失败: " + e.getMessage());
    } finally {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }
}
```

- [ ] **Step 2: 提交变更**
```bash
git add -A && git commit -m "refactor: ReportConfigController uses built-in FTP

- Remove FtpConfigService dependency
- Scan method now uses built-in FTP and reportConfig.scanPath
- Related to H-FTP-REFACTOR"
```

---

## Task 5: 修改TaskController - 使用内置FTP

**Files:**
- Modify: `report-backend/src/main/java/com/report/controller/TaskController.java`

- [ ] **Step 1: 修改TaskController.java**

移除 `FtpConfig` 相关引用，修改 `/trigger` 接口使用内置FTP：

需要修改的imports：
```java
// 删除
import com.report.entity.FtpConfig;
import com.report.service.FtpConfigService;
import com.report.util.FtpUtil;
import org.apache.commons.net.ftp.FTPClient;

// 添加
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
```

需要修改的autowired字段：
```java
// 删除
@Autowired
private FtpConfigService ftpConfigService;

// 添加
@Autowired(required = false)
private BuiltInFtpConfigService builtInFtpConfigService;

@Autowired(required = false)
private EmbeddedFtpServer embeddedFtpServer;
```

需要修改的trigger方法（完全重写）：
```java
@PostMapping("/trigger")
public Result<Map<String, Object>> trigger(@RequestBody Map<String, Object> params) {
    Long reportConfigId = Long.valueOf(params.get("reportConfigId").toString());
    String fileName = params.containsKey("fileName") ? params.get("fileName").toString() : null;

    ReportConfig reportConfig = reportConfigService.getById(reportConfigId);
    if (reportConfig == null) {
        return Result.fail("报表配置不存在");
    }

    if (embeddedFtpServer == null || !embeddedFtpServer.isRunning()) {
        return Result.fail("内置FTP服务未运行");
    }

    BuiltInFtpConfig ftpConfig = builtInFtpConfigService.getConfig();
    if (ftpConfig == null) {
        return Result.fail("FTP配置不存在");
    }

    File tempFile = null;
    try {
        String scanPath = reportConfig.getScanPath();
        if (scanPath == null || scanPath.isEmpty()) {
            scanPath = "/upload";
        }

        File ftpRoot = new File(ftpConfig.getRootDirectory());
        File scanDir = new File(ftpRoot, scanPath);

        if (!scanDir.exists() || !scanDir.isDirectory()) {
            return Result.fail("扫描目录不存在: " + scanDir.getAbsolutePath());
        }

        if (fileName == null || fileName.isEmpty()) {
            String pattern = reportConfig.getFilePattern();
            String fileRegex = pattern.replace("*", ".*").replace("?", ".");
            File[] files = scanDir.listFiles((dir, name) -> name.matches(fileRegex));
            if (files == null || files.length == 0) {
                return Result.fail("目录中没有找到匹配的文件");
            }
            tempFile = files[0];
            fileName = tempFile.getName();
        } else {
            tempFile = new File(scanDir, fileName);
            if (!tempFile.exists()) {
                return Result.fail("文件不存在: " + fileName);
            }
        }

        if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
            return Result.fail("文件已处理过: " + fileName);
        }

        File finalTempFile = File.createTempFile("trigger_", "_" + fileName);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(finalTempFile)) {
            java.nio.file.Files.copy(tempFile.toPath(), fos);
        }

        MatchedFile matchedFile = new MatchedFile();
        matchedFile.setFileName(fileName);
        matchedFile.setFilePath(tempFile.getAbsolutePath());
        matchedFile.setReportConfigId(reportConfigId);
        matchedFile.setLocalFile(finalTempFile);
        LocalDate date = FileNameDateExtractor.extractDate(fileName);
        matchedFile.setPtDt(date != null ? date.toString() : null);

        middlewareEngine.processFile(matchedFile, reportConfig);

        Map<String, Object> result = new HashMap<>();
        result.put("reportConfigId", reportConfigId);
        result.put("fileName", fileName);
        result.put("status", "PROCESSED");
        return Result.success(result);

    } catch (Exception e) {
        log.error("手动触发任务失败", e);
        return Result.fail("任务执行失败: " + e.getMessage());
    }
}
```

- [ ] **Step 2: 提交变更**
```bash
git add -A && git commit -m "refactor: TaskController uses built-in FTP

- Remove FtpConfigService dependency
- Trigger method now uses built-in FTP and reportConfig.scanPath
- Related to H-FTP-REFACTOR"
```

---

## Task 6: 修改数据库Schema

**Files:**
- Modify: `report-backend/src/main/resources/schema.sql`
- Modify: `report-backend/src/main/resources/schema-gaussdb.sql`

- [ ] **Step 1: 修改schema.sql**

1. 在 `report_config` 表定义中添加 `scan_path` 字段（在 `ftp_config_id` 之后）
2. 删除 `ftp_config` 表定义

修改后的 report_config 表部分：
```sql
CREATE TABLE `report_config` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `report_code` varchar(50) NOT NULL COMMENT '报表编码',
  `report_name` varchar(100) NOT NULL COMMENT '报表名称',
  `ftp_config_id` bigint DEFAULT NULL COMMENT '关联FTP配置ID(已废弃,仅保留兼容)',
  `scan_path` varchar(200) DEFAULT '/upload' COMMENT '扫描路径',
  `file_pattern` varchar(100) DEFAULT NULL COMMENT '文件匹配模式',
  ...
```

删除 ftp_config 表定义（整个表定义块）

- [ ] **Step 2: 修改schema-gaussdb.sql**

同样的修改

- [ ] **Step 3: 提交变更**
```bash
git add -A && git commit -m "refactor: update database schema

- Add scan_path column to report_config
- Remove ftp_config table definition
- Related to H-FTP-REFACTOR"
```

---

## Task 7: 删除前端FTP配置代码

**Files:**
- Delete: `src/views/ftp/FtpConfig.vue`
- Delete: `src/api/ftpConfig.js`
- Modify: `src/router/index.js`

- [ ] **Step 1: 删除FtpConfig.vue**
```bash
rm /home/nova/projects/report-front/src/views/ftp/FtpConfig.vue
```

- [ ] **Step 2: 删除ftpConfig.js API文件**
```bash
rm /home/nova/projects/report-front/src/api/ftpConfig.js
```

- [ ] **Step 3: 修改router/index.js**

移除FTP相关路由：
```javascript
// 删除以下路由定义
{
  path: '/ftp',
  name: 'FtpConfig',
  component: () => import('@/views/ftp/FtpConfig.vue')
},
// 修改根路径重定向
{
  path: '/',
  redirect: '/report'  // 改为重定向到 /report
}
```

- [ ] **Step 4: 提交变更**
```bash
git add -A && git commit -m "refactor: remove FTP config frontend

- Remove FtpConfig.vue page
- Remove ftpConfig.js API
- Remove /ftp route
- Related to H-FTP-REFACTOR"
```

---

## Task 8: 修改ReportConfig.vue - 新增扫描路径配置

**Files:**
- Modify: `src/views/report/components/ReportConfig.vue`

- [ ] **Step 1: 修改ReportConfig.vue**

1. 在"关联FTP"表单项后添加"扫描路径"配置项
2. 保留"关联FTP"下拉框但设为隐藏或禁用（为兼容旧数据）

在template中添加扫描路径表单项：
```html
<el-form-item label="关联FTP" prop="ftpConfigId">
  <el-select v-model="form.ftpConfigId" disabled placeholder="已统一使用内置FTP">
    <el-option label="内置FTP" :value="-1" />
  </el-select>
  <span style="margin-left: 10px; color: #909399; font-size: 12px;">
    (已统一使用内置FTP)
  </span>
</el-form-item>
<el-form-item label="扫描路径" prop="scanPath">
  <el-input v-model="form.scanPath" placeholder="如: /upload/SALES_REPORT" style="width: 300px;" />
  <span style="margin-left: 10px; color: #909399; font-size: 12px;">
    相对于FTP根目录的路径，默认 /upload
  </span>
</el-form-item>
```

在data中添加scanPath：
```javascript
form: {
  id: null,
  reportCode: '',
  reportName: '',
  ftpConfigId: -1,
  scanPath: '/upload',  // 新增
  filePattern: '*.xlsx',
  ...
}
```

- [ ] **Step 2: 提交变更**
```bash
git add -A && git commit -m "feat: add scanPath config to ReportConfig page

- Add scan path input field
- FTP selector now shows built-in FTP only
- Related to H-FTP-REFACTOR"
```

---

## Task 9: 数据迁移脚本

**Files:**
- Create: `report-backend/src/main/resources/migration/V1.0__ftp_simplification.sql`

- [ ] **Step 1: 创建数据迁移脚本**

```sql
-- FTP配置简化重构数据迁移脚本
-- 执行时间: 2026-04-29

-- 1. 新增 scan_path 字段
ALTER TABLE report_config ADD COLUMN IF NOT EXISTS scan_path VARCHAR(200) DEFAULT '/upload' COMMENT '扫描路径' AFTER ftp_config_id;

-- 2. 迁移数据：如果有外部FTP配置，将其scan_path迁移过来
-- 注意：这里假设外部FTP已经被废弃，scan_path统一使用默认值
UPDATE report_config SET scan_path = '/upload' WHERE scan_path IS NULL OR scan_path = '';

-- 3. 将所有报表的ftp_config_id设置为-1（内置FTP）
UPDATE report_config SET ftp_config_id = -1 WHERE ftp_config_id IS NOT NULL;

-- 4. 删除外部FTP配置表（确认迁移完成后执行）
-- DROP TABLE IF EXISTS ftp_config;

-- 5. 记录迁移完成
INSERT INTO operation_log (id, module, operation_type, operation_desc, result, create_time)
VALUES (NEXT VALUE FOR operation_log_id_seq, '数据迁移', 'MIGRATION', 'FTP配置简化重构数据迁移', 1, NOW());
```

- [ ] **Step 2: 提交迁移脚本**
```bash
git add -A && git commit -m "docs: add FTP simplification migration script

- Add scan_path column migration
- Related to H-FTP-REFACTOR"
```

---

## Task 10: 同步harness上下文

**Files:**
- Modify: `harness/tasks.json`
- Modify: `harness/progress-notes.md`

- [ ] **Step 1: 更新tasks.json**

添加新任务或更新现有任务状态

- [ ] **Step 2: 更新progress-notes.md**

记录本次重构的详细变更

- [ ] **Step 3: 提交harness变更**
```bash
git add -A && git commit -m "docs: sync harness context for FTP simplification

- Update tasks.json
- Update progress-notes.md with migration details
- Related to H-FTP-REFACTOR"
```

---

## 自检清单

完成所有任务后，确认以下内容：

- [ ] FtpConfig相关后端代码已全部删除
- [ ] FtpConfig相关前端代码已全部删除
- [ ] ReportConfig实体已新增scanPath字段
- [ ] FtpScanJob已重构为仅扫描内置FTP
- [ ] TaskController已重构为使用内置FTP
- [ ] ReportConfigController已重构为使用内置FTP
- [ ] 数据库schema已更新
- [ ] 前端ReportConfig页面已新增扫描路径配置
- [ ] 数据迁移脚本已创建
- [ ] harness上下文已同步
- [ ] 所有变更已提交Git
