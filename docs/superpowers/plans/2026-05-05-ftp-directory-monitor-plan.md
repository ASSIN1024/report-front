# FTP目录监听与监控模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现FTP目录自动创建、文件监听和完整处理流程监控功能

**Architecture:** 基于Apache FtpServer内置FTP服务器，在报表配置保存时自动创建目录，FtpScanJob监听扫描文件，MonitorService记录全流程监控数据，通过REST API对外提供监控查询接口。

**Tech Stack:** Spring Boot 2.1.2, Apache FtpServer, MyBatis-Plus, JUnit

---

## 文件结构

```
report-backend/src/main/java/com/report/
├── common/enums/
│   └── ProcessStep.java                    # 处理步骤枚举
├── entity/
│   └── ProcessMonitorLog.java              # 监控日志实体
├── mapper/
│   └── ProcessMonitorLogMapper.java         # 监控日志Mapper
├── service/
│   ├── MonitorService.java                  # 监控服务接口
│   ├── impl/
│   │   └── MonitorServiceImpl.java          # 监控服务实现
│   ├── FtpDirectoryService.java             # FTP目录服务接口
│   └── impl/
│       └── FtpDirectoryServiceImpl.java    # FTP目录服务实现
└── controller/
    └── MonitorController.java               # 监控REST API

report-backend/src/test/java/com/report/
└── test/
    └── FtpUploadE2eTest.java               # 端到端测试
```

---

## Task 1: 创建ProcessStep枚举

**Files:**
- Create: `report-backend/src/main/java/com/report/common/enums/ProcessStep.java`

- [ ] **Step 1: 创建ProcessStep枚举类**

```java
package com.report.common.enums;

public enum ProcessStep {
    DIR_CREATING("目录创建中"),
    DIR_CREATED("目录创建完成"),
    FILE_UPLOADING("文件上传中"),
    FILE_UPLOADED("文件上传完成"),
    FILE_DETECTED("文件被检测到"),
    PARSING("解析中"),
    PARSED("解析完成"),
    PROCESSING("处理中"),
    PROCESSED("处理完成"),
    EXCEPTION("异常");

    private final String description;

    ProcessStep(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/common/enums/ProcessStep.java
git commit -m "feat: 添加ProcessStep处理步骤枚举"
```

---

## Task 2: 创建ProcessMonitorLog实体

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/ProcessMonitorLog.java`

- [ ] **Step 1: 创建ProcessMonitorLog实体类**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("process_monitor_log")
public class ProcessMonitorLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportCode;

    private String fileName;

    private String step;

    private String status;

    private String message;

    private Long durationMs;

    private Date startTime;

    private Date endTime;

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}
```

- [ ] **Step 2: 创建建表SQL**

Create: `report-backend/src/main/resources/sql/process_monitor_log.sql`

```sql
CREATE TABLE `process_monitor_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `report_code` varchar(64) DEFAULT NULL COMMENT '报表编码',
  `file_name` varchar(256) DEFAULT NULL COMMENT '文件名',
  `step` varchar(32) DEFAULT NULL COMMENT '处理步骤',
  `status` varchar(16) DEFAULT NULL COMMENT '状态',
  `message` text DEFAULT NULL COMMENT '详细信息',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '耗时',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_code` (`report_code`),
  KEY `idx_file_name` (`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/entity/ProcessMonitorLog.java
git add report-backend/src/main/resources/sql/process_monitor_log.sql
git commit -m "feat: 添加ProcessMonitorLog监控日志实体和建表SQL"
```

---

## Task 3: 创建ProcessMonitorLogMapper

**Files:**
- Create: `report-backend/src/main/java/com/report/mapper/ProcessMonitorLogMapper.java`

- [ ] **Step 1: 创建Mapper接口**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ProcessMonitorLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProcessMonitorLogMapper extends BaseMapper<ProcessMonitorLog> {
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/mapper/ProcessMonitorLogMapper.java
git commit -m "feat: 添加ProcessMonitorLogMapper"
```

---

## Task 4: 创建MonitorService服务接口

**Files:**
- Create: `report-backend/src/main/java/com/report/service/MonitorService.java`

- [ ] **Step 1: 创建MonitorService接口**

```java
package com.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.entity.ProcessMonitorLog;
import java.util.List;

public interface MonitorService {

    Long startMonitor(String reportCode, String fileName, String step);

    void updateMonitor(Long monitorId, String step, String status, String message);

    void endMonitor(Long monitorId, String status, String message);

    List<ProcessMonitorLog> getMonitorHistory(String reportCode);

    List<ProcessMonitorLog> getFileMonitorHistory(String fileName);

    Page<ProcessMonitorLog> pageListByReportCode(String reportCode, Integer pageNum, Integer pageSize);

    ProcessMonitorLog getLatestByReportCode(String reportCode);
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/service/MonitorService.java
git commit -m "feat: 添加MonitorService监控服务接口"
```

---

## Task 5: 创建MonitorServiceImpl服务实现

**Files:**
- Create: `report-backend/src/main/java/com/report/service/impl/MonitorServiceImpl.java`

- [ ] **Step 1: 创建MonitorServiceImpl实现类**

```java
package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.ProcessMonitorLog;
import com.report.mapper.ProcessMonitorLogMapper;
import com.report.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class MonitorServiceImpl extends ServiceImpl<ProcessMonitorLogMapper, ProcessMonitorLog>
        implements MonitorService {

    @Override
    public Long startMonitor(String reportCode, String fileName, String step) {
        ProcessMonitorLog monitorLog = new ProcessMonitorLog();
        monitorLog.setReportCode(reportCode);
        monitorLog.setFileName(fileName);
        monitorLog.setStep(step);
        monitorLog.setStatus(ProcessMonitorLog.STATUS_RUNNING);
        monitorLog.setStartTime(new Date());
        save(monitorLog);

        log.info("[Monitor] started - reportCode={}, fileName={}, step={}, monitorId={}",
                reportCode, fileName, step, monitorLog.getId());
        return monitorLog.getId();
    }

    @Override
    public void updateMonitor(Long monitorId, String step, String status, String message) {
        ProcessMonitorLog monitorLog = getById(monitorId);
        if (monitorLog == null) {
            log.warn("[Monitor] monitorId={} not found", monitorId);
            return;
        }
        monitorLog.setStep(step);
        monitorLog.setStatus(status);
        monitorLog.setMessage(message);
        updateById(monitorLog);

        log.info("[Monitor] updated - monitorId={}, step={}, status={}, message={}",
                monitorId, step, status, message);
    }

    @Override
    public void endMonitor(Long monitorId, String status, String message) {
        ProcessMonitorLog monitorLog = getById(monitorId);
        if (monitorLog == null) {
            log.warn("[Monitor] monitorId={} not found", monitorId);
            return;
        }
        monitorLog.setStatus(status);
        monitorLog.setMessage(message);
        monitorLog.setEndTime(new Date());
        if (monitorLog.getStartTime() != null) {
            monitorLog.setDurationMs(monitorLog.getEndTime().getTime() - monitorLog.getStartTime().getTime());
        }
        updateById(monitorLog);

        log.info("[Monitor] ended - monitorId={}, status={}, message={}, durationMs={}",
                monitorId, status, message, monitorLog.getDurationMs());
    }

    @Override
    public List<ProcessMonitorLog> getMonitorHistory(String reportCode) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getReportCode, reportCode);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        return list(wrapper);
    }

    @Override
    public List<ProcessMonitorLog> getFileMonitorHistory(String fileName) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getFileName, fileName);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        return list(wrapper);
    }

    @Override
    public Page<ProcessMonitorLog> pageListByReportCode(String reportCode, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getReportCode, reportCode);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ProcessMonitorLog getLatestByReportCode(String reportCode) {
        LambdaQueryWrapper<ProcessMonitorLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessMonitorLog::getReportCode, reportCode);
        wrapper.orderByDesc(ProcessMonitorLog::getStartTime);
        wrapper.last("LIMIT 1");
        return getOne(wrapper);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/service/impl/MonitorServiceImpl.java
git commit -m "feat: 添加MonitorServiceImpl监控服务实现"
```

---

## Task 6: 创建MonitorController REST API

**Files:**
- Create: `report-backend/src/main/java/com/report/controller/MonitorController.java`

- [ ] **Step 1: 创建MonitorController**

```java
package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.ProcessMonitorLog;
import com.report.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Autowired
    private MonitorService monitorService;

    @GetMapping("/report/{reportCode}")
    public Result<Page<ProcessMonitorLog>> getReportMonitorHistory(
            @PathVariable String reportCode,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<ProcessMonitorLog> page = monitorService.pageListByReportCode(reportCode, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/file/{fileName}")
    public Result<List<ProcessMonitorLog>> getFileMonitorHistory(@PathVariable String fileName) {
        List<ProcessMonitorLog> logs = monitorService.getFileMonitorHistory(fileName);
        return Result.success(logs);
    }

    @GetMapping("/realtime/{reportCode}")
    public Result<Map<String, Object>> getRealtimeProgress(@PathVariable String reportCode) {
        ProcessMonitorLog latest = monitorService.getLatestByReportCode(reportCode);
        Map<String, Object> result = new HashMap<>();
        result.put("latest", latest);
        result.put("history", monitorService.getMonitorHistory(reportCode));
        return Result.success(result);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/controller/MonitorController.java
git commit -m "feat: 添加MonitorController监控REST API"
```

---

## Task 7: 创建FtpDirectoryService服务接口

**Files:**
- Create: `report-backend/src/main/java/com/report/service/FtpDirectoryService.java`

- [ ] **Step 1: 创建FtpDirectoryService接口**

```java
package com.report.service;

import com.report.entity.ProcessMonitorLog;

public interface FtpDirectoryService {

    boolean createDirectoriesIfNotExist(String scanPath);

    boolean createDirectory(String directory);

    boolean directoryExists(String path);

    Long getOrCreateMonitorId(String reportCode, String scanPath);
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/service/FtpDirectoryService.java
git commit -m "feat: 添加FtpDirectoryService FTP目录服务接口"
```

---

## Task 8: 创建FtpDirectoryServiceImpl服务实现

**Files:**
- Create: `report-backend/src/main/java/com/report/service/impl/FtpDirectoryServiceImpl.java`

- [ ] **Step 1: 创建FtpDirectoryServiceImpl实现类**

```java
package com.report.service.impl;

import com.report.common.enums.ProcessStep;
import com.report.entity.ProcessMonitorLog;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.service.FtpDirectoryService;
import com.report.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class FtpDirectoryServiceImpl implements FtpDirectoryService {

    @Autowired
    private BuiltInFtpConfigMapper builtInFtpConfigMapper;

    @Autowired
    private MonitorService monitorService;

    @Override
    public boolean createDirectoriesIfNotExist(String scanPath) {
        BuiltInFtpConfig config = builtInFtpConfigMapper.getConfig();
        if (config == null) {
            log.error("[FtpDirectory] FTP配置不存在");
            return false;
        }

        String rootDir = config.getRootDirectory();
        File ftpRoot = new File(rootDir);
        if (!ftpRoot.exists()) {
            ftpRoot.mkdirs();
        }

        File targetDir = new File(ftpRoot, scanPath);
        if (targetDir.exists()) {
            log.info("[FtpDirectory] 目录已存在，无需创建: {}", targetDir.getAbsolutePath());
            return true;
        }

        boolean created = targetDir.mkdirs();
        if (created) {
            log.info("[FtpDirectory] 目录创建成功: {}", targetDir.getAbsolutePath());
        } else {
            log.error("[FtpDirectory] 目录创建失败: {}", targetDir.getAbsolutePath());
        }
        return created;
    }

    @Override
    public boolean createDirectory(String directory) {
        File dir = new File(directory);
        if (dir.exists()) {
            return true;
        }
        boolean created = dir.mkdirs();
        if (created) {
            log.info("[FtpDirectory] 创建目录: {}", directory);
        }
        return created;
    }

    @Override
    public boolean directoryExists(String path) {
        BuiltInFtpConfig config = builtInFtpConfigMapper.getConfig();
        if (config == null) {
            return false;
        }
        File dir = new File(config.getRootDirectory(), path);
        return dir.exists() && dir.isDirectory();
    }

    @Override
    public Long getOrCreateMonitorId(String reportCode, String scanPath) {
        Long monitorId = monitorService.startMonitor(reportCode, null, ProcessStep.DIR_CREATING.name());
        boolean success = createDirectoriesIfNotExist(scanPath);
        if (success) {
            monitorService.updateMonitor(monitorId, ProcessStep.DIR_CREATED.name(),
                    ProcessMonitorLog.STATUS_SUCCESS, "目录创建成功: " + scanPath);
        } else {
            monitorService.updateMonitor(monitorId, ProcessStep.DIR_CREATED.name(),
                    ProcessMonitorLog.STATUS_FAILED, "目录创建失败: " + scanPath);
        }
        return monitorId;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/service/impl/FtpDirectoryServiceImpl.java
git commit -m "feat: 添加FtpDirectoryServiceImpl FTP目录服务实现"
```

---

## Task 9: 修改ReportConfigController集成目录创建

**Files:**
- Modify: `report-backend/src/main/java/com/report/controller/ReportConfigController.java`

- [ ] **Step 1: 在ReportConfigController中添加FtpDirectoryService依赖**

在类中添加字段:
```java
@Autowired
private FtpDirectoryService ftpDirectoryService;
```

- [ ] **Step 2: 在保存配置后调用目录创建方法**

在save或update方法中，配置保存成功后添加:
```java
// 自动创建FTP目录
if (reportConfig.getScanPath() != null && !reportConfig.getScanPath().isEmpty()) {
    try {
        ftpDirectoryService.getOrCreateMonitorId(
                reportConfig.getReportCode(),
                reportConfig.getScanPath()
        );
    } catch (Exception e) {
        log.warn("FTP目录自动创建失败，但不影响配置保存: {}", e.getMessage());
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/controller/ReportConfigController.java
git commit -m "feat: 集成FTP目录自动创建到报表配置保存"
```

---

## Task 10: 创建FtpUploadE2eTest端到端测试

**Files:**
- Create: `report-backend/src/test/java/com/report/test/FtpUploadE2eTest.java`

- [ ] **Step 1: 创建端到端测试类**

```java
package com.report.test;

import com.report.common.enums.ProcessStep;
import com.report.entity.ProcessMonitorLog;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.service.MonitorService;
import com.report.service.FtpDirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class FtpUploadE2eTest {

    private static final String TEST_REPORT_CODE = "TEST_E2E_001";
    private static final String TEST_SCAN_PATH = "/upload/test";
    private static final String TEST_FILE_NAME = "test120260429.xlsx";

    @Autowired
    private BuiltInFtpConfigMapper builtInFtpConfigMapper;

    @Autowired
    private FtpDirectoryService ftpDirectoryService;

    @Autowired
    private MonitorService monitorService;

    private BuiltInFtpConfig ftpConfig;

    @Before
    public void setUp() {
        ftpConfig = builtInFtpConfigMapper.getConfig();
        assertNotNull("FTP配置不应为空", ftpConfig);
        assertTrue("FTP服务应已启用", ftpConfig.getEnabled());
    }

    @Test
    public void testFtpDirectoryCreation() {
        log.info("[E2E] ========== 测试FTP目录创建 ==========");

        Long monitorId = ftpDirectoryService.getOrCreateMonitorId(TEST_REPORT_CODE, TEST_SCAN_PATH);
        assertNotNull("监控ID不应为空", monitorId);

        ProcessMonitorLog monitorLog = monitorService.getMonitorHistory(TEST_REPORT_CODE).stream()
                .filter(log -> monitorId.equals(log.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull("监控日志不应为空", monitorLog);
        assertEquals("步骤应为DIR_CREATED", ProcessStep.DIR_CREATED.name(), monitorLog.getStep());
        assertEquals("状态应为SUCCESS", ProcessMonitorLog.STATUS_SUCCESS, monitorLog.getStatus());

        assertTrue("目录应该存在", ftpDirectoryService.directoryExists(TEST_SCAN_PATH));

        log.info("[E2E] FTP目录创建测试通过");
    }

    @Test
    public void testFtpFileUpload() throws IOException {
        log.info("[E2E] ========== 测试FTP文件上传 ==========");

        File testFile = new File("src/main/resources/test120260429.xlsx");
        if (!testFile.exists()) {
            log.warn("[E2E] 测试文件不存在，跳过上传测试: {}", testFile.getAbsolutePath());
            return;
        }

        Long monitorId = monitorService.startMonitor(TEST_REPORT_CODE, TEST_FILE_NAME, ProcessStep.FILE_UPLOADING.name());

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect("localhost", ftpConfig.getPort());
            ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            String targetPath = TEST_SCAN_PATH + "/" + TEST_FILE_NAME;
            try (FileInputStream fis = new FileInputStream(testFile)) {
                boolean uploaded = ftpClient.storeFile(targetPath, fis);
                assertTrue("文件上传应成功", uploaded);
                log.info("[E2E] 文件上传成功: {}", targetPath);
            }

            monitorService.updateMonitor(monitorId, ProcessStep.FILE_UPLOADED.name(),
                    ProcessMonitorLog.STATUS_SUCCESS, "文件上传成功: " + targetPath);

            boolean fileExists = ftpDirectoryService.directoryExists(TEST_SCAN_PATH + "/" + TEST_FILE_NAME);
            log.info("[E2E] 文件在FTP目录中是否存在: {}", fileExists);

        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            monitorService.endMonitor(monitorId, ProcessMonitorLog.STATUS_SUCCESS, "FTP操作完成");
        }

        log.info("[E2E] FTP文件上传测试通过");
    }

    @Test
    public void testEndToEndFlow() throws IOException {
        log.info("[E2E] ========== 端到端流程测试 ==========");

        String scanPath = "/upload/e2e_test";
        String fileName = "e2e_test_file.txt";

        Long dirMonitorId = ftpDirectoryService.getOrCreateMonitorId(TEST_REPORT_CODE, scanPath);
        log.info("[E2E] 目录创建监控ID: {}", dirMonitorId);
        assertTrue("目录应创建成功", ftpDirectoryService.directoryExists(scanPath));

        Long uploadMonitorId = monitorService.startMonitor(TEST_REPORT_CODE, fileName, ProcessStep.FILE_UPLOADING.name());

        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient.connect("localhost", ftpConfig.getPort());
            ftpClient.login(ftpConfig.getUsername(), ftpConfig.getPassword());
            ftpClient.enterLocalPassiveMode();
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);

            File tempFile = new File("/tmp/" + fileName);
            tempFile.createNewFile();

            try (FileInputStream fis = new FileInputStream(tempFile)) {
                String targetPath = scanPath + "/" + fileName;
                boolean uploaded = ftpClient.storeFile(targetPath, fis);
                assertTrue("文件上传应成功", uploaded);
                log.info("[E2E] 文件上传成功: {}", targetPath);
            }

            monitorService.updateMonitor(uploadMonitorId, ProcessStep.FILE_UPLOADED.name(),
                    ProcessMonitorLog.STATUS_SUCCESS, "文件上传成功");

        } finally {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
            monitorService.endMonitor(uploadMonitorId, ProcessMonitorLog.STATUS_SUCCESS, "上传流程完成");
        }

        List<ProcessMonitorLog> history = monitorService.getMonitorHistory(TEST_REPORT_CODE);
        log.info("[E2E] 监控历史记录数: {}", history.size());
        history.forEach(log ->
                log.info("[E2E] 监控记录 - step: {}, status: {}, message: {}",
                        log.getStep(), log.getStatus(), log.getMessage())
        );

        assertFalse("应有监控记录", history.isEmpty());

        log.info("[E2E] 端到端流程测试通过");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/test/java/com/report/test/FtpUploadE2eTest.java
git commit -m "test: 添加FTP上传端到端测试"
```

---

## Task 11: 执行数据库迁移

- [ ] **Step 1: 执行建表SQL**

使用MySQL执行:
```sql
CREATE TABLE `process_monitor_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `report_code` varchar(64) DEFAULT NULL COMMENT '报表编码',
  `file_name` varchar(256) DEFAULT NULL COMMENT '文件名',
  `step` varchar(32) DEFAULT NULL COMMENT '处理步骤',
  `status` varchar(16) DEFAULT NULL COMMENT '状态',
  `message` text DEFAULT NULL COMMENT '详细信息',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '耗时',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  PRIMARY KEY (`id`),
  KEY `idx_report_code` (`report_code`),
  KEY `idx_file_name` (`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## Task 12: 整体验证

- [ ] **Step 1: 启动后端服务**

```bash
./scripts/start.sh backend
```

- [ ] **Step 2: 检查服务是否正常启动**

```bash
curl http://localhost:8082/api/monitor/report/TEST
```

预期返回: `{"code":0,"data":{"records":[],"total":0,"size":10,"current":1}}`

- [ ] **Step 3: 运行单元测试**

```bash
cd report-backend && mvn test -Dtest=FtpUploadE2eTest
```

---

## 验证检查清单

- [ ] ProcessStep枚举创建完成
- [ ] ProcessMonitorLog实体创建完成
- [ ] process_monitor_log表创建完成
- [ ] MonitorService接口和实现创建完成
- [ ] MonitorController REST API创建完成
- [ ] FtpDirectoryService接口和实现创建完成
- [ ] ReportConfigController集成目录创建
- [ ] FtpUploadE2eTest端到端测试创建完成
- [ ] 所有测试通过
- [ ] API接口验证通过

---

## 备选：使用subagent-driven-development执行

推荐使用subagent模式，将任务分配给专门的subagent去执行，每个任务完成后进行review，然后再执行下一个任务。

**Plan complete and saved to `docs/superpowers/plans/2026-05-05-ftp-directory-monitor-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
