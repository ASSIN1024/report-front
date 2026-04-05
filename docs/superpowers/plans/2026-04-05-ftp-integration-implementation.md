# 内置FTP服务集成实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在Spring Boot应用中集成Apache FtpServer，实现RPA机器人直接上传Excel文件到本系统，内置FTP服务与FtpScanJob无缝对接。

**Architecture:** 采用Apache FtpServer嵌入式方案，FTP服务作为Spring Boot应用的子模块运行，统一配置管理，统一日志输出。内置FTP与现有外部FTP配置**共存**，分别服务不同场景。

**Tech Stack:** Apache FtpServer 2.2.0, Spring Boot 2.1.2, MyBatis-Plus 3.4.3

---

## 文件结构

```
report-backend/src/main/java/com/report/
├── ftp/                                    # 新增FTP模块
│   ├── EmbeddedFtpServer.java             # FTP服务生命周期管理
│   ├── BuiltInFtpConfig.java              # 内置FTP配置实体
│   ├── BuiltInFtpConfigMapper.java        # 数据库映射
│   ├── BuiltInFtpConfigService.java       # 配置服务接口
│   ├── impl/BuiltInFtpConfigServiceImpl.java
│   └── controller/BuiltInFtpConfigController.java
├── config/FtpConfigProperties.java        # 配置属性类
└── job/FtpScanJob.java                    # 扩展支持内置FTP

report-backend/src/main/resources/
├── application.yml                         # 新增内置FTP配置
└── schema.sql                             # 新增built_in_ftp_config表
```

---

## Task 1: 添加Apache FtpServer依赖

**Files:**
- Modify: `report-backend/pom.xml`

- [ ] **Step 1: 添加Apache FtpServer依赖**

在`<dependencies>`节点内添加：

```xml
<dependency>
    <groupId>org.apache.ftpserver</groupId>
    <artifactId>ftpserver-core</artifactId>
    <version>1.2.0</version>
</dependency>

<dependency>
    <groupId>org.apache.ftpserver</groupId>
    <artifactId>ftplet-api</artifactId>
    <version>1.2.0</version>
</dependency>

<dependency>
    <groupId>org.apache.mina</groupId>
    <artifactId>mina-core</artifactId>
    <version>2.2.0</version>
</dependency>
```

- [ ] **Step 2: 验证依赖**

Run: `cd report-backend && mvn dependency:tree | grep ftpserver`
Expected: 显示ftpserver-core和ftplet-api依赖

- [ ] **Step 3: 提交**

```bash
git add report-backend/pom.xml
git commit -m "chore: Add Apache FtpServer dependencies

关联任务: H-XXX"
```

---

## Task 2: 创建配置属性类

**Files:**
- Create: `report-backend/src/main/java/com/report/config/FtpConfigProperties.java`

- [ ] **Step 1: 创建FtpConfigProperties配置属性类**

```java
package com.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ftp.built-in")
public class FtpConfigProperties {

    private boolean enabled = false;

    private int port = 2021;

    private String username = "rpa_user";

    private String password = "rpa_password";

    private String rootDirectory = "/data/ftp-root";

    private int maxConnections = 10;

    private int idleTimeout = 300;

    private boolean passiveMode = true;

    private int passivePortStart = 50000;

    private int passivePortEnd = 50100;

    private int maxThreads = 5;
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/config/FtpConfigProperties.java
git commit -m "feat(ftp): Add FtpConfigProperties for built-in FTP configuration

关联任务: H-XXX"
```

---

## Task 3: 创建内置FTP配置实体

**Files:**
- Create: `report-backend/src/main/java/com/report/ftp/BuiltInFtpConfig.java`

- [ ] **Step 1: 创建BuiltInFtpConfig实体类**

```java
package com.report.ftp;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("built_in_ftp_config")
public class BuiltInFtpConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Boolean enabled;

    private Integer port;

    private String username;

    private String password;

    private String rootDirectory;

    private Integer maxConnections;

    private Integer idleTimeout;

    private Boolean passiveMode;

    private Integer passivePortStart;

    private Integer passivePortEnd;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/ftp/BuiltInFtpConfig.java
git commit -m "feat(ftp): Add BuiltInFtpConfig entity

关联任务: H-XXX"
```

---

## Task 4: 创建内置FTP数据访问层

**Files:**
- Create: `report-backend/src/main/java/com/report/ftp/BuiltInFtpConfigMapper.java`
- Create: `report-backend/src/main/resources/mapper/BuiltInFtpConfigMapper.xml`

- [ ] **Step 1: 创建BuiltInFtpConfigMapper接口**

```java
package com.report.ftp;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BuiltInFtpConfigMapper extends BaseMapper<BuiltInFtpConfig> {
}
```

- [ ] **Step 2: 创建MyBatis XML映射文件**

在`report-backend/src/main/resources/mapper/`目录下创建`BuiltInFtpConfigMapper.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.report.ftp.BuiltInFtpConfigMapper">
</mapper>
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/ftp/BuiltInFtpConfigMapper.java
git add report-backend/src/main/resources/mapper/BuiltInFtpConfigMapper.xml
git commit -m "feat(ftp): Add BuiltInFtpConfigMapper

关联任务: H-XXX"
```

---

## Task 5: 创建内置FTP服务层

**Files:**
- Create: `report-backend/src/main/java/com/report/ftp/BuiltInFtpConfigService.java`
- Create: `report-backend/src/main/java/com/report/ftp/impl/BuiltInFtpConfigServiceImpl.java`

- [ ] **Step 1: 创建BuiltInFtpConfigService接口**

```java
package com.report.ftp;

import com.baomidou.mybatisplus.extension.service.IService;

public interface BuiltInFtpConfigService extends IService<BuiltInFtpConfig> {

    BuiltInFtpConfig getConfig();

    void updateConfig(BuiltInFtpConfig config);

    boolean isServerRunning();
}
```

- [ ] **Step 2: 创建BuiltInFtpConfigServiceImpl实现**

```java
package com.report.ftp.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.ftp.BuiltInFtpConfigService;
import org.springframework.stereotype.Service;

@Service
public class BuiltInFtpConfigServiceImpl extends ServiceImpl<BuiltInFtpConfigMapper, BuiltInFtpConfig>
        implements BuiltInFtpConfigService {

    @Override
    public BuiltInFtpConfig getConfig() {
        BuiltInFtpConfig config = this.getById(1L);
        if (config == null) {
            config = new BuiltInFtpConfig();
            config.setId(1L);
            config.setEnabled(false);
            config.setPort(2021);
            config.setUsername("rpa_user");
            config.setPassword("rpa_password");
            config.setRootDirectory("/data/ftp-root");
            config.setMaxConnections(10);
            config.setIdleTimeout(300);
            config.setPassiveMode(true);
            config.setPassivePortStart(50000);
            config.setPassivePortEnd(50100);
            this.save(config);
        }
        return config;
    }

    @Override
    public void updateConfig(BuiltInFtpConfig config) {
        config.setId(1L);
        this.updateById(config);
    }

    @Override
    public boolean isServerRunning() {
        return false;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/ftp/BuiltInFtpConfigService.java
git add report-backend/src/main/java/com/report/ftp/impl/BuiltInFtpConfigServiceImpl.java
git commit -m "feat(ftp): Add BuiltInFtpConfigService

关联任务: H-XXX"
```

---

## Task 6: 创建EmbeddedFtpServer核心服务

**Files:**
- Create: `report-backend/src/main/java/com/report/ftp/EmbeddedFtpServer.java`

- [ ] **Step 1: 创建EmbeddedFtpServer FTP服务管理类**

```java
package com.report.ftp;

import lombok.extern.slf4j.Slf4j;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class EmbeddedFtpServer {

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Autowired
    private BuiltInFtpConfigService builtInFtpConfigService;

    private FtpServer ftpServer;

    public boolean start() {
        if (running.get()) {
            log.warn("FTP服务已在运行");
            return true;
        }

        BuiltInFtpConfig config = builtInFtpConfigService.getConfig();
        if (!config.getEnabled()) {
            log.warn("内置FTP未启用");
            return false;
        }

        try {
            FtpServerFactory serverFactory = new FtpServerFactory();
            ListenerFactory listenerFactory = new ListenerFactory();

            listenerFactory.setPort(config.getPort());
            listenerFactory.setIdleTimeout(config.getIdleTimeout());

            PropertiesUserManagerFactory userManagerFactory = new PropertiesUserManagerFactory();
            userManagerFactory.setFile(
                File.createTempFile("ftp-users", ".properties")
            );

            BaseUser user = new BaseUser();
            user.setName(config.getUsername());
            user.setPassword(config.getPassword());
            user.setHomeDirectory(config.getRootDirectory());

            List<Authority> authorities = new ArrayList<>();
            authorities.add(new WritePermission());
            user.setAuthorities(authorities);

            userManagerFactory.getUserManager().save(user);

            Map<String, Ftplet> ftplets = new HashMap<>();
            ftplets.put("ftpLogging", new Ftplet() {
                @Override
                public void init(FtpletContext ftpletContext) {
                }

                @Override
                public void destroy() {
                }

                @Override
                public FtpletResult beforeCommand(FtpSession session, FtpRequest request) {
                    log.info("[FTP-ACCESS] {} - {} - {} - {}",
                        session.getClientAddress(),
                        session.getUser().getName(),
                        request.getCommand(),
                        request.getArgument());
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpletResult result) {
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult onConnect(FtpSession session) {
                    log.info("[FTP-CONNECT] {}", session.getClientAddress());
                    return FtpletResult.DEFAULT;
                }

                @Override
                public FtpletResult onDisconnect(FtpSession session) {
                    log.info("[FTP-DISCONNECT] {}", session.getClientAddress());
                    return FtpletResult.DEFAULT;
                }
            });

            serverFactory.setUserManager(userManagerFactory.getUserManager());
            serverFactory.setFtplets(ftplets);
            serverFactory.addListener("default", listenerFactory.createListener());

            ftpServer = serverFactory.createServer();
            ftpServer.start();

            running.set(true);
            log.info("内置FTP服务已启动，端口: {}", config.getPort());
            return true;

        } catch (Exception e) {
            log.error("启动内置FTP服务失败", e);
            return false;
        }
    }

    public void stop() {
        if (ftpServer != null && !ftpServer.isStopped()) {
            ftpServer.stop();
            running.set(false);
            log.info("内置FTP服务已停止");
        }
    }

    public boolean isRunning() {
        return running.get() && ftpServer != null && !ftpServer.isStopped();
    }

    public int getConnectedClients() {
        return 0;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/ftp/EmbeddedFtpServer.java
git commit -m "feat(ftp): Add EmbeddedFtpServer core FTP service

关联任务: H-XXX"
```

---

## Task 7: 创建内置FTP控制器

**Files:**
- Create: `report-backend/src/main/java/com/report/ftp/controller/BuiltInFtpConfigController.java`

- [ ] **Step 1: 创建BuiltInFtpConfigController**

```java
package com.report.ftp.controller;

import com.report.annotation.OperationLogAnnotation;
import com.report.common.result.Result;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/built-in-ftp")
public class BuiltInFtpConfigController {

    @Autowired
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired
    private EmbeddedFtpServer embeddedFtpServer;

    @GetMapping("/config")
    public Result<BuiltInFtpConfig> getConfig() {
        return Result.success(builtInFtpConfigService.getConfig());
    }

    @PutMapping("/config")
    @OperationLogAnnotation(module = "内置FTP", operationType = "UPDATE", operationDesc = "修改内置FTP配置")
    public Result<Void> updateConfig(@RequestBody BuiltInFtpConfig config) {
        if (embeddedFtpServer.isRunning()) {
            return Result.fail("FTP服务正在运行，请先停止服务");
        }
        builtInFtpConfigService.updateConfig(config);
        return Result.success();
    }

    @PostMapping("/start")
    @OperationLogAnnotation(module = "内置FTP", operationType = "START", operationDesc = "启动内置FTP服务")
    public Result<Map<String, Object>> start() {
        Map<String, Object> result = new HashMap<>();
        boolean success = embeddedFtpServer.start();
        result.put("running", success);
        if (success) {
            BuiltInFtpConfig config = builtInFtpConfigService.getConfig();
            result.put("port", config.getPort());
            result.put("connectedClients", embeddedFtpServer.getConnectedClients());
            return Result.success(result);
        } else {
            return Result.fail("启动FTP服务失败");
        }
    }

    @PostMapping("/stop")
    @OperationLogAnnotation(module = "内置FTP", operationType = "STOP", operationDesc = "停止内置FTP服务")
    public Result<Void> stop() {
        embeddedFtpServer.stop();
        return Result.success();
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        Map<String, Object> result = new HashMap<>();
        result.put("running", embeddedFtpServer.isRunning());
        if (embeddedFtpServer.isRunning()) {
            BuiltInFtpConfig config = builtInFtpConfigService.getConfig();
            result.put("port", config.getPort());
            result.put("connectedClients", embeddedFtpServer.getConnectedClients());
        }
        return Result.success(result);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/ftp/controller/BuiltInFtpConfigController.java
git commit -m "feat(ftp): Add BuiltInFtpConfigController API endpoints

关联任务: H-XXX"
```

---

## Task 8: 创建数据库Schema

**Files:**
- Modify: `report-backend/src/main/resources/schema.sql`

- [ ] **Step 1: 在schema.sql末尾添加内置FTP配置表**

```sql
-- 内置FTP配置表
CREATE TABLE IF NOT EXISTS `built_in_ftp_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enabled` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用',
  `port` INT NOT NULL DEFAULT 2021 COMMENT 'FTP端口',
  `username` VARCHAR(64) NOT NULL DEFAULT 'rpa_user' COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL DEFAULT 'rpa_password' COMMENT '密码',
  `root_directory` VARCHAR(256) NOT NULL DEFAULT '/data/ftp-root' COMMENT '根目录',
  `max_connections` INT NOT NULL DEFAULT 10 COMMENT '最大连接数',
  `idle_timeout` INT NOT NULL DEFAULT 300 COMMENT '空闲超时(秒)',
  `passive_mode` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否被动模式',
  `passive_port_start` INT DEFAULT 50000 COMMENT '被动端口起始',
  `passive_port_end` INT DEFAULT 50100 COMMENT '被动端口结束',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内置FTP配置';
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/resources/schema.sql
git commit -m "feat(ftp): Add built_in_ftp_config table schema

关联任务: H-XXX"
```

---

## Task 9: 添加application.yml配置

**Files:**
- Modify: `report-backend/src/main/resources/application.yml`

- [ ] **Step 1: 在application.yml末尾添加内置FTP默认配置**

```yaml
ftp:
  built-in:
    enabled: false
    port: 2021
    username: rpa_user
    password: rpa_password
    root-directory: /data/ftp-root
    max-connections: 10
    idle-timeout: 300
    passive-mode: true
    passive-port-start: 50000
    passive-port-end: 50100
    max-threads: 5
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/resources/application.yml
git commit -m "feat(ftp): Add built-in FTP default configuration

关联任务: H-XXX"
```

---

## Task 10: 扩展FtpScanJob支持内置FTP

**Files:**
- Modify: `report-backend/src/main/java/com/report/job/FtpScanJob.java`

- [ ] **Step 1: 扩展FtpScanJob添加内置FTP扫描方法**

在FtpScanJob类中添加：

```java
@Autowired(required = false)
private EmbeddedFtpServer embeddedFtpServer;

public void scanBuiltInFtp(Long reportConfigId, Long taskId) {
    if (embeddedFtpServer == null || !embeddedFtpServer.isRunning()) {
        log.warn("内置FTP服务未运行");
        taskService.finishTask(taskId, "FAILED", "内置FTP服务未运行");
        return;
    }

    ReportConfig reportConfig = reportConfigService.getById(reportConfigId);
    if (reportConfig == null) {
        log.error("报表配置不存在: {}", reportConfigId);
        taskService.finishTask(taskId, "FAILED", "报表配置不存在");
        return;
    }

    BuiltInFtpConfig config = builtInFtpConfigService.getConfig();
    File ftpRoot = new File(config.getRootDirectory());
    File uploadDir = new File(ftpRoot, "upload");

    if (!uploadDir.exists() || !uploadDir.isDirectory()) {
        log.warn("内置FTP上传目录不存在: {}", uploadDir.getAbsolutePath());
        taskService.finishTask(taskId, "NO_FILE", "上传目录不存在");
        return;
    }

    String pattern = reportConfig.getFilePattern();
    String fileRegex = pattern.replace("*", ".*").replace("?", ".");

    File[] files = uploadDir.listFiles((dir, name) -> name.matches(fileRegex));
    if (files == null || files.length == 0) {
        log.info("内置FTP上传目录没有匹配的文件");
        taskService.finishTask(taskId, "NO_FILE", "未匹配到文件");
        return;
    }

    log.info("扫描到 {} 个匹配文件", files.length);
    int processedCount = 0;

    for (File file : files) {
        String fileName = file.getName();
        if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
            log.info("文件已处理过，跳过: {}", fileName);
            continue;
        }

        log.info("处理文件: {}", fileName);
        TaskExecution task = taskService.createTask(
            "FTP_SCAN",
            "内置FTP扫描-" + reportConfig.getReportName(),
            reportConfig.getId(),
            fileName,
            file.getAbsolutePath()
        );

        try {
            dataProcessJob.processFile(task.getId(), reportConfig, file);
            processedFileService.markAsProcessed(reportConfig.getId(), fileName, file.length(), task.getId());
            processedCount++;
        } catch (Exception e) {
            log.error("文件处理失败: {}", fileName, e);
            taskService.finishTask(task.getId(), "FAILED", e.getMessage());
            processedFileService.markAsFailed(reportConfig.getId(), fileName, task.getId(), e.getMessage());
        }
    }

    taskService.finishTask(taskId, "SUCCESS", null);
    logService.saveLog(taskId, "INFO", "扫描完成，共处理 " + processedCount + " 个文件");
}
```

同时在类顶部添加import：

```java
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
```

并添加Autowired字段：

```java
@Autowired
private BuiltInFtpConfigService builtInFtpConfigService;
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/job/FtpScanJob.java
git commit -m "feat(ftp): Extend FtpScanJob to support built-in FTP scanning

关联任务: H-XXX"
```

---

## Task 11: 编译验证

**Files:**
- (验证)

- [ ] **Step 1: 编译项目**

Run: `cd report-backend && mvn compile -q`
Expected: BUILD SUCCESS，无编译错误

- [ ] **Step 2: 如有错误，修复后重新提交**

---

## Task 12: 创建初始化脚本

**Files:**
- Create: `report-backend/src/main/resources/init-builtin-ftp.sql`

- [ ] **Step 1: 创建初始化SQL脚本**

```sql
-- 内置FTP配置初始化数据
INSERT INTO `built_in_ftp_config` (`id`, `enabled`, `port`, `username`, `password`, `root_directory`, `max_connections`, `idle_timeout`, `passive_mode`, `passive_port_start`, `passive_port_end`)
VALUES (1, 0, 2021, 'rpa_user', 'rpa_password', '/data/ftp-root', 10, 300, 1, 50000, 50100)
ON DUPLICATE KEY UPDATE `id` = `id`;
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/resources/init-builtin-ftp.sql
git commit -m "feat(ftp): Add init script for built-in FTP

关联任务: H-XXX"
```

---

## Task 13: 最终验证

**Files:**
- (验证)

- [ ] **Step 1: 完整编译**

Run: `cd report-backend && mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行测试**

Run: `cd report-backend && mvn test -q`
Expected: 所有测试通过

---

## 实施检查清单

- [ ] Task 1: pom.xml添加Apache FtpServer依赖
- [ ] Task 2: FtpConfigProperties配置属性类
- [ ] Task 3: BuiltInFtpConfig实体类
- [ ] Task 4: BuiltInFtpConfigMapper数据访问层
- [ ] Task 5: BuiltInFtpConfigService服务层
- [ ] Task 6: EmbeddedFtpServer核心FTP服务
- [ ] Task 7: BuiltInFtpConfigController API控制器
- [ ] Task 8: schema.sql数据库表
- [ ] Task 9: application.yml默认配置
- [ ] Task 10: FtpScanJob扩展支持内置FTP
- [ ] Task 11: 编译验证
- [ ] Task 12: 初始化脚本
- [ ] Task 13: 最终验证

---

## 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-04-05 | V1.0 | 初始实施计划 | AI Assistant |
