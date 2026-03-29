# 报表数据处理平台实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个轻量化的报表数据处理平台，实现FTP文件监听、Excel解析、数据处理和BI报表数据输出

**Architecture:** 采用Spring Boot 2.1.2单体架构，Vue 2.6.12前端，MyBatis-Plus ORM，Quartz定时调度，配置驱动设计

**Tech Stack:** Spring Boot 2.1.2, MyBatis-Plus 3.4.x, Vue 2.6.12, Element UI 2.15.x, Apache POI 5.2.x, Apache Commons Net 3.9.x, Quartz 2.3.x

---

## 文件结构规划

### 后端文件结构

```
report-backend/
├── pom.xml
├── src/main/java/com/report/
│   ├── ReportApplication.java
│   ├── common/
│   │   ├── config/
│   │   │   ├── MybatisPlusConfig.java
│   │   │   ├── QuartzConfig.java
│   │   │   └── WebMvcConfig.java
│   │   ├── constant/
│   │   │   ├── ErrorCode.java
│   │   │   └── FieldTypeEnum.java
│   │   ├── exception/
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── result/
│   │       └── Result.java
│   ├── controller/
│   │   ├── FtpConfigController.java
│   │   ├── ReportConfigController.java
│   │   ├── TaskController.java
│   │   ├── LogController.java
│   │   └── DataController.java
│   ├── service/
│   │   ├── FtpConfigService.java
│   │   ├── ReportConfigService.java
│   │   ├── TaskService.java
│   │   ├── LogService.java
│   │   ├── DataService.java
│   │   └── impl/
│   │       ├── FtpConfigServiceImpl.java
│   │       ├── ReportConfigServiceImpl.java
│   │       ├── TaskServiceImpl.java
│   │       ├── LogServiceImpl.java
│   │       └── DataServiceImpl.java
│   ├── mapper/
│   │   ├── FtpConfigMapper.java
│   │   ├── ReportConfigMapper.java
│   │   ├── TaskExecutionMapper.java
│   │   └── TaskExecutionLogMapper.java
│   ├── entity/
│   │   ├── FtpConfig.java
│   │   ├── ReportConfig.java
│   │   ├── TaskExecution.java
│   │   ├── TaskExecutionLog.java
│   │   └── dto/
│   │       ├── ColumnMapping.java
│   │       ├── FieldMapping.java
│   │       ├── ReportConfigDTO.java
│   │       └── TaskQueryDTO.java
│   ├── job/
│   │   └── FtpScanJob.java
│   ├── handler/
│   │   ├── FtpHandler.java
│   │   ├── ExcelHandler.java
│   │   └── DataHandler.java
│   └── util/
│       ├── FtpUtil.java
│       ├── ExcelUtil.java
│       └── DateUtil.java
└── src/main/resources/
    ├── application.yml
    ├── application-dev.yml
    ├── application-prod.yml
    └── mapper/
        ├── FtpConfigMapper.xml
        ├── ReportConfigMapper.xml
        ├── TaskExecutionMapper.xml
        └── TaskExecutionLogMapper.xml
```

### 前端文件结构

```
report-front/
├── package.json
├── vue.config.js
├── public/
│   └── index.html
└── src/
    ├── main.js
    ├── App.vue
    ├── api/
    │   ├── ftpConfig.js
    │   ├── reportConfig.js
    │   ├── task.js
    │   ├── log.js
    │   └── data.js
    ├── components/
    │   ├── Pagination.vue
    │   └── StatusTag.vue
    ├── views/
    │   ├── ftp/
    │   │   └── FtpConfig.vue
    │   ├── report/
    │   │   ├── ReportList.vue
    │   │   └── components/
    │   │       ├── BasicInfo.vue
    │   │       ├── ScanConfig.vue
    │   │       ├── ColumnMapping.vue
    │   │       └── OutputConfig.vue
    │   ├── task/
    │   │   └── TaskMonitor.vue
    │   ├── log/
    │   │   └── LogList.vue
    │   └── data/
    │       └── DataQuery.vue
    ├── router/
    │   └── index.js
    ├── store/
    │   └── index.js
    ├── utils/
    │   ├── request.js
    │   └── auth.js
    └── styles/
        ├── index.scss
        └── variables.scss
```

---

## Phase 1: 后端基础框架

### Task 1.1: 创建后端项目结构

**Files:**
- Create: `report-backend/pom.xml`
- Create: `report-backend/src/main/java/com/report/ReportApplication.java`
- Create: `report-backend/src/main/resources/application.yml`
- Create: `report-backend/src/main/resources/application-dev.yml`

- [ ] **Step 1: 创建后端项目目录结构**

```bash
mkdir -p report-backend/src/main/java/com/report/{common/{config,constant,exception,result},controller,service/impl,mapper,entity/dto,job,handler,util}
mkdir -p report-backend/src/main/resources/mapper
mkdir -p report-backend/src/test/java/com/report
```

- [ ] **Step 2: 创建pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.report</groupId>
    <artifactId>report-backend</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>report-backend</name>
    <description>报表数据处理平台</description>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.1.2.RELEASE</version>
        <relativePath/>
    </parent>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <java.version>1.8</java.version>
        <mybatis-plus.version>3.4.3</mybatis-plus.version>
        <hutool.version>5.8.25</hutool.version>
        <poi.version>5.2.3</poi.version>
        <commons-net.version>3.9.0</commons-net.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-quartz</artifactId>
        </dependency>

        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <dependency>
            <groupId>commons-net</groupId>
            <artifactId>commons-net</artifactId>
            <version>${commons-net.version}</version>
        </dependency>

        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>${poi.version}</version>
        </dependency>

        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 创建ReportApplication.java**

```java
package com.report;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.report.mapper")
public class ReportApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}
```

- [ ] **Step 4: 创建application.yml**

```yaml
server:
  port: 8080

spring:
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

logging:
  level:
    com.report: debug
```

- [ ] **Step 5: 创建application-dev.yml**

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/report_db?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root

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
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 6: 验证项目结构**

```bash
ls -la report-backend/
ls -la report-backend/src/main/java/com/report/
```

Expected: 目录结构正确创建

---

### Task 1.2: 创建公共模块 - 统一返回和异常处理

**Files:**
- Create: `report-backend/src/main/java/com/report/common/result/Result.java`
- Create: `report-backend/src/main/java/com/report/common/constant/ErrorCode.java`
- Create: `report-backend/src/main/java/com/report/common/exception/BusinessException.java`
- Create: `report-backend/src/main/java/com/report/common/exception/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建Result.java统一返回类**

```java
package com.report.common.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("操作成功");
        result.setData(data);
        return result;
    }

    public static <T> Result<T> success(String message, T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> Result<T> error(Integer code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }
}
```

- [ ] **Step 2: 创建ErrorCode.java错误码常量**

```java
package com.report.common.constant;

public class ErrorCode {

    private ErrorCode() {}

    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;

    public static final int FTP_CONNECT_ERROR = 1001;
    public static final int FILE_PARSE_ERROR = 1002;
    public static final int DATA_VALIDATE_ERROR = 1003;
    public static final int DB_OPERATE_ERROR = 1004;
}
```

- [ ] **Step 3: 创建BusinessException.java业务异常**

```java
package com.report.common.exception;

import com.report.common.constant.ErrorCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.INTERNAL_ERROR;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
```

- [ ] **Step 4: 创建GlobalExceptionHandler.java全局异常处理**

```java
package com.report.common.exception;

import com.report.common.constant.ErrorCode;
import com.report.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数校验异常: {}", message);
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.error("参数绑定异常: {}", message);
        return Result.error(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR, "系统异常，请联系管理员");
    }
}
```

- [ ] **Step 5: 验证公共模块**

```bash
ls -la report-backend/src/main/java/com/report/common/
```

Expected: result、constant、exception目录存在且文件正确

---

### Task 1.3: 创建公共模块 - 配置类和枚举

**Files:**
- Create: `report-backend/src/main/java/com/report/common/config/MybatisPlusConfig.java`
- Create: `report-backend/src/main/java/com/report/common/config/WebMvcConfig.java`
- Create: `report-backend/src/main/java/com/report/common/constant/FieldTypeEnum.java`

- [ ] **Step 1: 创建MybatisPlusConfig.java**

```java
package com.report.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

@Configuration
public class MybatisPlusConfig implements MetaObjectHandler {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
```

- [ ] **Step 2: 创建WebMvcConfig.java**

```java
package com.report.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
```

- [ ] **Step 3: 创建FieldTypeEnum.java字段类型枚举**

```java
package com.report.common.constant;

import lombok.Getter;

@Getter
public enum FieldTypeEnum {

    STRING("STRING", "字符串"),
    INTEGER("INTEGER", "整数"),
    DECIMAL("DECIMAL", "小数"),
    DATE("DATE", "日期");

    private final String code;
    private final String desc;

    FieldTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static FieldTypeEnum getByCode(String code) {
        for (FieldTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
```

- [ ] **Step 4: 验证配置类**

```bash
ls -la report-backend/src/main/java/com/report/common/config/
ls -la report-backend/src/main/java/com/report/common/constant/
```

Expected: 配置类和枚举文件存在

---

### Task 1.4: 创建数据库初始化脚本

**Files:**
- Create: `report-backend/src/main/resources/schema.sql`
- Create: `report-backend/src/main/resources/data.sql`

- [ ] **Step 1: 创建schema.sql数据库表结构**

```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS report_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE report_db;

-- FTP配置表
DROP TABLE IF EXISTS ftp_config;
CREATE TABLE ftp_config (
    id BIGINT NOT NULL COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '配置名称',
    host VARCHAR(255) NOT NULL COMMENT 'FTP主机地址',
    port INT NOT NULL DEFAULT 21 COMMENT '端口号',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    encoding VARCHAR(20) DEFAULT 'UTF-8' COMMENT '编码格式',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='FTP连接配置表';

-- 报表配置表
DROP TABLE IF EXISTS report_config;
CREATE TABLE report_config (
    id BIGINT NOT NULL COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '报表名称',
    code VARCHAR(50) NOT NULL COMMENT '报表编码',
    ftp_config_id BIGINT NOT NULL COMMENT '关联FTP配置',
    scan_path VARCHAR(500) NOT NULL COMMENT '扫描路径',
    file_pattern VARCHAR(200) NOT NULL COMMENT '文件名正则匹配规则',
    scan_interval INT NOT NULL DEFAULT 10 COMMENT '扫描间隔（分钟）',
    sheet_index INT DEFAULT 0 COMMENT 'Sheet索引',
    header_row INT DEFAULT 1 COMMENT '表头行号',
    data_start_row INT DEFAULT 2 COMMENT '数据起始行号',
    column_mapping TEXT NOT NULL COMMENT '列映射配置（JSON格式）',
    output_type TINYINT NOT NULL COMMENT '输出类型：1数据库 2文件 3混合',
    output_table VARCHAR(100) DEFAULT NULL COMMENT '目标表名',
    output_path VARCHAR(500) DEFAULT NULL COMMENT '文件输出路径',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除 1已删除',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用 1启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_ftp_config_id (ftp_config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表配置表';

-- 任务执行记录表
DROP TABLE IF EXISTS task_execution;
CREATE TABLE task_execution (
    id BIGINT NOT NULL COMMENT '主键',
    report_config_id BIGINT NOT NULL COMMENT '关联报表配置',
    file_name VARCHAR(200) NOT NULL COMMENT '处理的文件名',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0待处理 1处理中 2成功 3失败',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    total_rows INT DEFAULT 0 COMMENT '总行数',
    success_rows INT DEFAULT 0 COMMENT '成功行数',
    error_rows INT DEFAULT 0 COMMENT '失败行数',
    error_message TEXT DEFAULT NULL COMMENT '错误信息',
    pt_dt DATE NOT NULL COMMENT '数据日期',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_report_config_id (report_config_id),
    KEY idx_status (status),
    KEY idx_pt_dt (pt_dt)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行记录表';

-- 任务执行日志表
DROP TABLE IF EXISTS task_execution_log;
CREATE TABLE task_execution_log (
    id BIGINT NOT NULL COMMENT '主键',
    task_execution_id BIGINT NOT NULL COMMENT '关联执行记录',
    log_level VARCHAR(10) NOT NULL COMMENT '日志级别：INFO/WARN/ERROR',
    log_message TEXT NOT NULL COMMENT '日志内容',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_task_execution_id (task_execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务执行日志表';
```

- [ ] **Step 2: 创建data.sql初始化数据**

```sql
USE report_db;

-- 插入示例FTP配置
INSERT INTO ftp_config (id, name, host, port, username, password, encoding, deleted, status, create_time, update_time)
VALUES (1, '测试FTP服务器', '192.168.1.100', 21, 'admin', 'admin123', 'UTF-8', 0, 1, NOW(), NOW());

-- 插入示例报表配置
INSERT INTO report_config (id, name, code, ftp_config_id, scan_path, file_pattern, scan_interval, sheet_index, header_row, data_start_row, column_mapping, output_type, output_table, output_path, deleted, status, remark, create_time, update_time)
VALUES (1, '销售日报', 'SALE_001', 1, '/data/sales', '^sales_\\d{8}\\.xlsx$', 10, 0, 1, 2, '{"mappings":[{"excelColumnName":"产品名称","fieldName":"product_name","fieldType":"STRING","defaultValue":null,"required":true},{"excelColumnName":"销售数量","fieldName":"sales_quantity","fieldType":"INTEGER","defaultValue":"0","required":true},{"excelColumnName":"销售金额","fieldName":"sales_amount","fieldType":"DECIMAL","defaultValue":"0.00","required":false}]}', 1, 'ods_sales_daily', NULL, 0, 1, '每日销售数据报表', NOW(), NOW());
```

- [ ] **Step 3: 验证SQL文件**

```bash
ls -la report-backend/src/main/resources/
```

Expected: schema.sql和data.sql文件存在

---

### Task 1.5: 创建实体类

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/FtpConfig.java`
- Create: `report-backend/src/main/java/com/report/entity/ReportConfig.java`
- Create: `report-backend/src/main/java/com/report/entity/TaskExecution.java`
- Create: `report-backend/src/main/java/com/report/entity/TaskExecutionLog.java`

- [ ] **Step 1: 创建FtpConfig.java实体类**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ftp_config")
public class FtpConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String host;

    private Integer port;

    private String username;

    private String password;

    private String encoding;

    @TableLogic
    private Integer deleted;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- [ ] **Step 2: 创建ReportConfig.java实体类**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.report.entity.dto.ColumnMapping;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "report_config", autoResultMap = true)
public class ReportConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String name;

    private String code;

    private Long ftpConfigId;

    private String scanPath;

    private String filePattern;

    private Integer scanInterval;

    private Integer sheetIndex;

    private Integer headerRow;

    private Integer dataStartRow;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private ColumnMapping columnMapping;

    private Integer outputType;

    private String outputTable;

    private String outputPath;

    @TableLogic
    private Integer deleted;

    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- [ ] **Step 3: 创建TaskExecution.java实体类**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("task_execution")
public class TaskExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long reportConfigId;

    private String fileName;

    private Integer status;

    private Date startTime;

    private Date endTime;

    private Integer totalRows;

    private Integer successRows;

    private Integer errorRows;

    private String errorMessage;

    private Date ptDt;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- [ ] **Step 4: 创建TaskExecutionLog.java实体类**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("task_execution_log")
public class TaskExecutionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long taskExecutionId;

    private String logLevel;

    private String logMessage;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
```

- [ ] **Step 5: 验证实体类**

```bash
ls -la report-backend/src/main/java/com/report/entity/
```

Expected: 四个实体类文件存在

---

### Task 1.6: 创建DTO类

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/dto/FieldMapping.java`
- Create: `report-backend/src/main/java/com/report/entity/dto/ColumnMapping.java`
- Create: `report-backend/src/main/java/com/report/entity/dto/ReportConfigDTO.java`

- [ ] **Step 1: 创建FieldMapping.java字段映射DTO**

```java
package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FieldMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private String excelColumnName;

    private String fieldName;

    private String fieldType;

    private String defaultValue;

    private Boolean required;
}
```

- [ ] **Step 2: 创建ColumnMapping.java列映射DTO**

```java
package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ColumnMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<FieldMapping> mappings;
}
```

- [ ] **Step 3: 创建ReportConfigDTO.java报表配置DTO**

```java
package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReportConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String code;

    private Long ftpConfigId;

    private String ftpConfigName;

    private String scanPath;

    private String filePattern;

    private Integer scanInterval;

    private Integer sheetIndex;

    private Integer headerRow;

    private Integer dataStartRow;

    private ColumnMapping columnMapping;

    private Integer outputType;

    private String outputTable;

    private String outputPath;

    private Integer status;

    private String remark;
}
```

- [ ] **Step 4: 验证DTO类**

```bash
ls -la report-backend/src/main/java/com/report/entity/dto/
```

Expected: 三个DTO文件存在

---

### Task 1.7: 创建Mapper接口和XML

**Files:**
- Create: `report-backend/src/main/java/com/report/mapper/FtpConfigMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/ReportConfigMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/TaskExecutionMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/TaskExecutionLogMapper.java`
- Create: `report-backend/src/main/resources/mapper/ReportConfigMapper.xml`

- [ ] **Step 1: 创建FtpConfigMapper.java**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.FtpConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FtpConfigMapper extends BaseMapper<FtpConfig> {
}
```

- [ ] **Step 2: 创建ReportConfigMapper.java**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.entity.ReportConfig;
import com.report.entity.dto.ReportConfigDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportConfigMapper extends BaseMapper<ReportConfig> {

    IPage<ReportConfigDTO> selectPageWithFtp(Page<ReportConfigDTO> page, @Param("name") String name, @Param("status") Integer status);
}
```

- [ ] **Step 3: 创建TaskExecutionMapper.java**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.TaskExecution;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskExecutionMapper extends BaseMapper<TaskExecution> {
}
```

- [ ] **Step 4: 创建TaskExecutionLogMapper.java**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.TaskExecutionLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskExecutionLogMapper extends BaseMapper<TaskExecutionLog> {
}
```

- [ ] **Step 5: 创建ReportConfigMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.report.mapper.ReportConfigMapper">

    <resultMap id="ReportConfigDTOResultMap" type="com.report.entity.dto.ReportConfigDTO">
        <id column="id" property="id"/>
        <result column="name" property="name"/>
        <result column="code" property="code"/>
        <result column="ftp_config_id" property="ftpConfigId"/>
        <result column="ftp_config_name" property="ftpConfigName"/>
        <result column="scan_path" property="scanPath"/>
        <result column="file_pattern" property="filePattern"/>
        <result column="scan_interval" property="scanInterval"/>
        <result column="sheet_index" property="sheetIndex"/>
        <result column="header_row" property="headerRow"/>
        <result column="data_start_row" property="dataStartRow"/>
        <result column="column_mapping" property="columnMapping" typeHandler="com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler"/>
        <result column="output_type" property="outputType"/>
        <result column="output_table" property="outputTable"/>
        <result column="output_path" property="outputPath"/>
        <result column="status" property="status"/>
        <result column="remark" property="remark"/>
    </resultMap>

    <select id="selectPageWithFtp" resultMap="ReportConfigDTOResultMap">
        SELECT
            rc.id,
            rc.name,
            rc.code,
            rc.ftp_config_id,
            fc.name as ftp_config_name,
            rc.scan_path,
            rc.file_pattern,
            rc.scan_interval,
            rc.sheet_index,
            rc.header_row,
            rc.data_start_row,
            rc.column_mapping,
            rc.output_type,
            rc.output_table,
            rc.output_path,
            rc.status,
            rc.remark
        FROM report_config rc
        LEFT JOIN ftp_config fc ON rc.ftp_config_id = fc.id
        WHERE rc.deleted = 0
        <if test="name != null and name != ''">
            AND rc.name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="status != null">
            AND rc.status = #{status}
        </if>
        ORDER BY rc.create_time DESC
    </select>

</mapper>
```

- [ ] **Step 6: 验证Mapper文件**

```bash
ls -la report-backend/src/main/java/com/report/mapper/
ls -la report-backend/src/main/resources/mapper/
```

Expected: Mapper接口和XML文件存在

---

## Phase 2: 后端核心业务模块

### Task 2.1: 创建FTP配置管理模块

**Files:**
- Create: `report-backend/src/main/java/com/report/service/FtpConfigService.java`
- Create: `report-backend/src/main/java/com/report/service/impl/FtpConfigServiceImpl.java`
- Create: `report-backend/src/main/java/com/report/controller/FtpConfigController.java`

- [ ] **Step 1: 创建FtpConfigService.java接口**

```java
package com.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.entity.FtpConfig;

public interface FtpConfigService {

    IPage<FtpConfig> page(Integer pageNum, Integer pageSize, String name, Integer status);

    FtpConfig getById(Long id);

    void save(FtpConfig ftpConfig);

    void updateById(FtpConfig ftpConfig);

    void removeById(Long id);

    boolean testConnection(Long id);

    boolean testConnection(FtpConfig ftpConfig);
}
```

- [ ] **Step 2: 创建FtpConfigServiceImpl.java实现类**

```java
package com.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.common.constant.ErrorCode;
import com.report.common.exception.BusinessException;
import com.report.entity.FtpConfig;
import com.report.mapper.FtpConfigMapper;
import com.report.service.FtpConfigService;
import com.report.util.FtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FtpConfigServiceImpl extends ServiceImpl<FtpConfigMapper, FtpConfig> implements FtpConfigService {

    @Override
    public IPage<FtpConfig> page(Integer pageNum, Integer pageSize, String name, Integer status) {
        LambdaQueryWrapper<FtpConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), FtpConfig::getName, name);
        wrapper.eq(status != null, FtpConfig::getStatus, status);
        wrapper.orderByDesc(FtpConfig::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public FtpConfig getById(Long id) {
        FtpConfig ftpConfig = super.getById(id);
        if (ftpConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "FTP配置不存在");
        }
        return ftpConfig;
    }

    @Override
    public void save(FtpConfig ftpConfig) {
        LambdaQueryWrapper<FtpConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FtpConfig::getName, ftpConfig.getName());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配置名称已存在");
        }
        save(ftpConfig);
    }

    @Override
    public void updateById(FtpConfig ftpConfig) {
        FtpConfig existing = super.getById(ftpConfig.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "FTP配置不存在");
        }
        LambdaQueryWrapper<FtpConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FtpConfig::getName, ftpConfig.getName());
        wrapper.ne(FtpConfig::getId, ftpConfig.getId());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "配置名称已存在");
        }
        updateById(ftpConfig);
    }

    @Override
    public void removeById(Long id) {
        FtpConfig ftpConfig = super.getById(id);
        if (ftpConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "FTP配置不存在");
        }
        removeById(id);
    }

    @Override
    public boolean testConnection(Long id) {
        FtpConfig ftpConfig = getById(id);
        return testConnection(ftpConfig);
    }

    @Override
    public boolean testConnection(FtpConfig ftpConfig) {
        try {
            return FtpUtil.testConnection(ftpConfig);
        } catch (Exception e) {
            log.error("FTP连接测试失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.FTP_CONNECT_ERROR, "FTP连接失败: " + e.getMessage());
        }
    }
}
```

- [ ] **Step 3: 创建FtpConfigController.java控制器**

```java
package com.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.common.result.Result;
import com.report.entity.FtpConfig;
import com.report.service.FtpConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ftp-config")
@RequiredArgsConstructor
public class FtpConfigController {

    private final FtpConfigService ftpConfigService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        IPage<FtpConfig> page = ftpConfigService.page(pageNum, pageSize, name, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<FtpConfig> getById(@PathVariable Long id) {
        return Result.success(ftpConfigService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody FtpConfig ftpConfig) {
        ftpConfigService.save(ftpConfig);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody FtpConfig ftpConfig) {
        ftpConfig.setId(id);
        ftpConfigService.updateById(ftpConfig);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ftpConfigService.removeById(id);
        return Result.success();
    }

    @PostMapping("/test")
    public Result<Boolean> testConnection(@RequestBody FtpConfig ftpConfig) {
        return Result.success(ftpConfigService.testConnection(ftpConfig));
    }

    @PostMapping("/test/{id}")
    public Result<Boolean> testConnectionById(@PathVariable Long id) {
        return Result.success(ftpConfigService.testConnection(id));
    }
}
```

- [ ] **Step 4: 验证FTP配置模块**

```bash
ls -la report-backend/src/main/java/com/report/service/
ls -la report-backend/src/main/java/com/report/service/impl/
ls -la report-backend/src/main/java/com/report/controller/
```

Expected: FTP配置相关文件存在

---

### Task 2.2: 创建FTP工具类

**Files:**
- Create: `report-backend/src/main/java/com/report/util/FtpUtil.java`

- [ ] **Step 1: 创建FtpUtil.java工具类**

```java
package com.report.util;

import cn.hutool.core.io.FileUtil;
import com.report.entity.FtpConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FtpUtil {

    private FtpUtil() {}

    public static FTPClient connect(FtpConfig config) throws IOException {
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(config.getHost(), config.getPort());
        boolean loginSuccess = ftpClient.login(config.getUsername(), config.getPassword());
        if (!loginSuccess) {
            throw new IOException("FTP登录失败");
        }
        ftpClient.setControlEncoding(config.getEncoding());
        ftpClient.enterLocalPassiveMode();
        return ftpClient;
    }

    public static void disconnect(FTPClient ftpClient) {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (IOException e) {
                log.error("FTP断开连接失败: {}", e.getMessage());
            }
        }
    }

    public static boolean testConnection(FtpConfig config) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = connect(config);
            return ftpClient.isConnected();
        } finally {
            disconnect(ftpClient);
        }
    }

    public static List<String> listFiles(FtpConfig config, String path, String pattern) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = connect(config);
            ftpClient.changeWorkingDirectory(path);
            FTPFile[] files = ftpClient.listFiles();
            List<String> fileNames = new ArrayList<>();
            for (FTPFile file : files) {
                if (file.isFile() && file.getName().matches(pattern)) {
                    fileNames.add(file.getName());
                }
            }
            return fileNames;
        } finally {
            disconnect(ftpClient);
        }
    }

    public static void downloadFile(FtpConfig config, String remotePath, String localPath) throws IOException {
        FTPClient ftpClient = null;
        try {
            ftpClient = connect(config);
            FileUtil.mkParentDir(localPath);
            try (OutputStream os = new FileOutputStream(localPath)) {
                ftpClient.retrieveFile(remotePath, os);
            }
        } finally {
            disconnect(ftpClient);
        }
    }

    public static InputStream downloadStream(FtpConfig config, String remotePath) throws IOException {
        FTPClient ftpClient = connect(config);
        InputStream is = ftpClient.retrieveFileStream(remotePath);
        return new FTPInputStream(ftpClient, is);
    }

    public static class FTPInputStream extends InputStream {
        private final FTPClient ftpClient;
        private final InputStream inputStream;

        public FTPInputStream(FTPClient ftpClient, InputStream inputStream) {
            this.ftpClient = ftpClient;
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            return inputStream.read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return inputStream.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            try {
                inputStream.close();
                ftpClient.completePendingCommand();
            } finally {
                disconnect(ftpClient);
            }
        }
    }
}
```

- [ ] **Step 2: 验证工具类**

```bash
ls -la report-backend/src/main/java/com/report/util/
```

Expected: FtpUtil.java文件存在

---

### Task 2.3: 创建报表配置管理模块

**Files:**
- Create: `report-backend/src/main/java/com/report/service/ReportConfigService.java`
- Create: `report-backend/src/main/java/com/report/service/impl/ReportConfigServiceImpl.java`
- Create: `report-backend/src/main/java/com/report/controller/ReportConfigController.java`

- [ ] **Step 1: 创建ReportConfigService.java接口**

```java
package com.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.entity.ReportConfig;
import com.report.entity.dto.ReportConfigDTO;

public interface ReportConfigService {

    IPage<ReportConfigDTO> page(Integer pageNum, Integer pageSize, String name, Integer status);

    ReportConfig getById(Long id);

    void save(ReportConfig reportConfig);

    void updateById(ReportConfig reportConfig);

    void removeById(Long id);

    void updateStatus(Long id, Integer status);

    void copy(Long id);
}
```

- [ ] **Step 2: 创建ReportConfigServiceImpl.java实现类**

```java
package com.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.common.constant.ErrorCode;
import com.report.common.exception.BusinessException;
import com.report.entity.ReportConfig;
import com.report.entity.dto.ReportConfigDTO;
import com.report.mapper.ReportConfigMapper;
import com.report.service.ReportConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportConfigServiceImpl extends ServiceImpl<ReportConfigMapper, ReportConfig> implements ReportConfigService {

    private final ReportConfigMapper reportConfigMapper;

    @Override
    public IPage<ReportConfigDTO> page(Integer pageNum, Integer pageSize, String name, Integer status) {
        return reportConfigMapper.selectPageWithFtp(new Page<>(pageNum, pageSize), name, status);
    }

    @Override
    public ReportConfig getById(Long id) {
        ReportConfig reportConfig = super.getById(id);
        if (reportConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表配置不存在");
        }
        return reportConfig;
    }

    @Override
    public void save(ReportConfig reportConfig) {
        LambdaQueryWrapper<ReportConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportConfig::getCode, reportConfig.getCode());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报表编码已存在");
        }
        save(reportConfig);
    }

    @Override
    public void updateById(ReportConfig reportConfig) {
        ReportConfig existing = super.getById(reportConfig.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表配置不存在");
        }
        LambdaQueryWrapper<ReportConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportConfig::getCode, reportConfig.getCode());
        wrapper.ne(ReportConfig::getId, reportConfig.getId());
        if (count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "报表编码已存在");
        }
        updateById(reportConfig);
    }

    @Override
    public void removeById(Long id) {
        ReportConfig reportConfig = super.getById(id);
        if (reportConfig == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表配置不存在");
        }
        removeById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        ReportConfig reportConfig = getById(id);
        reportConfig.setStatus(status);
        updateById(reportConfig);
    }

    @Override
    public void copy(Long id) {
        ReportConfig original = getById(id);
        ReportConfig copy = new ReportConfig();
        copy.setName(original.getName() + "_副本");
        copy.setCode(original.getCode() + "_COPY");
        copy.setFtpConfigId(original.getFtpConfigId());
        copy.setScanPath(original.getScanPath());
        copy.setFilePattern(original.getFilePattern());
        copy.setScanInterval(original.getScanInterval());
        copy.setSheetIndex(original.getSheetIndex());
        copy.setHeaderRow(original.getHeaderRow());
        copy.setDataStartRow(original.getDataStartRow());
        copy.setColumnMapping(original.getColumnMapping());
        copy.setOutputType(original.getOutputType());
        copy.setOutputTable(original.getOutputTable());
        copy.setOutputPath(original.getOutputPath());
        copy.setStatus(0);
        copy.setRemark(original.getRemark());
        save(copy);
    }
}
```

- [ ] **Step 3: 创建ReportConfigController.java控制器**

```java
package com.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.common.result.Result;
import com.report.entity.ReportConfig;
import com.report.entity.dto.ReportConfigDTO;
import com.report.service.ReportConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/report-config")
@RequiredArgsConstructor
public class ReportConfigController {

    private final ReportConfigService reportConfigService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer status) {
        IPage<ReportConfigDTO> page = reportConfigService.page(pageNum, pageSize, name, status);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<ReportConfig> getById(@PathVariable Long id) {
        return Result.success(reportConfigService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody ReportConfig reportConfig) {
        reportConfigService.save(reportConfig);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ReportConfig reportConfig) {
        reportConfig.setId(id);
        reportConfigService.updateById(reportConfig);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportConfigService.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        reportConfigService.updateStatus(id, status);
        return Result.success();
    }

    @PostMapping("/{id}/copy")
    public Result<Void> copy(@PathVariable Long id) {
        reportConfigService.copy(id);
        return Result.success();
    }
}
```

- [ ] **Step 4: 验证报表配置模块**

```bash
ls -la report-backend/src/main/java/com/report/service/
ls -la report-backend/src/main/java/com/report/service/impl/
ls -la report-backend/src/main/java/com/report/controller/
```

Expected: 报表配置相关文件存在

---

### Task 2.4: 创建Excel工具类

**Files:**
- Create: `report-backend/src/main/java/com/report/util/ExcelUtil.java`

- [ ] **Step 1: 创建ExcelUtil.java工具类**

```java
package com.report.util;

import com.report.entity.dto.ColumnMapping;
import com.report.entity.dto.FieldMapping;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class ExcelUtil {

    private ExcelUtil() {}

    public static List<Map<String, Object>> parseExcel(InputStream is, int sheetIndex, int headerRow, int dataStartRow, ColumnMapping columnMapping) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Map<String, Integer> columnIndexMap = buildColumnIndexMap(sheet, headerRow);
            for (int i = dataStartRow - 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, Object> rowData = new HashMap<>();
                boolean hasData = false;
                for (FieldMapping mapping : columnMapping.getMappings()) {
                    String excelColumnName = mapping.getExcelColumnName();
                    Integer columnIndex = columnIndexMap.get(excelColumnName.toLowerCase().trim());
                    if (columnIndex == null) {
                        if (mapping.getDefaultValue() != null) {
                            rowData.put(mapping.getFieldName(), convertValue(mapping.getDefaultValue(), mapping.getFieldType()));
                        }
                        continue;
                    }
                    Cell cell = row.getCell(columnIndex);
                    Object value = getCellValue(cell, mapping.getFieldType());
                    if (value != null) {
                        hasData = true;
                    }
                    if (value == null && mapping.getDefaultValue() != null) {
                        value = convertValue(mapping.getDefaultValue(), mapping.getFieldType());
                    }
                    rowData.put(mapping.getFieldName(), value);
                }
                if (hasData) {
                    result.add(rowData);
                }
            }
        }
        return result;
    }

    private static Map<String, Integer> buildColumnIndexMap(Sheet sheet, int headerRow) {
        Map<String, Integer> map = new HashMap<>();
        Row row = sheet.getRow(headerRow - 1);
        if (row == null) {
            return map;
        }
        for (Cell cell : row) {
            String columnName = getCellValueAsString(cell);
            if (columnName != null) {
                map.put(columnName.toLowerCase().trim(), cell.getColumnIndex());
            }
        }
        return map;
    }

    private static Object getCellValue(Cell cell, String fieldType) {
        if (cell == null) {
            return null;
        }
        Object value = null;
        switch (cell.getCellType()) {
            case STRING:
                value = cell.getStringCellValue();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    value = cell.getDateCellValue();
                } else {
                    value = cell.getNumericCellValue();
                }
                break;
            case BOOLEAN:
                value = cell.getBooleanCellValue();
                break;
            case FORMULA:
                try {
                    value = cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        value = cell.getNumericCellValue();
                    } catch (Exception ex) {
                        value = null;
                    }
                }
                break;
            default:
                value = null;
        }
        return convertValue(value, fieldType);
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("yyyy-MM-dd").format(cell.getDateCellValue());
                } else {
                    return new DecimalFormat("#.##").format(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return null;
        }
    }

    private static Object convertValue(Object value, String fieldType) {
        if (value == null) {
            return null;
        }
        String strValue = String.valueOf(value).trim();
        if (strValue.isEmpty()) {
            return null;
        }
        try {
            switch (fieldType.toUpperCase()) {
                case "STRING":
                    return strValue;
                case "INTEGER":
                    return Integer.parseInt(strValue.replaceAll("[,，]", ""));
                case "DECIMAL":
                    return new DecimalFormat("#.##").parse(strValue.replaceAll("[,，]", "")).doubleValue();
                case "DATE":
                    if (value instanceof Date) {
                        return value;
                    }
                    return parseDate(strValue);
                default:
                    return strValue;
            }
        } catch (Exception e) {
            log.warn("类型转换失败: value={}, fieldType={}, error={}", value, fieldType, e.getMessage());
            return null;
        }
    }

    private static Date parseDate(String str) {
        String[] patterns = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyy年MM月dd日", "yyyyMMdd"};
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(str);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: 验证工具类**

```bash
ls -la report-backend/src/main/java/com/report/util/
```

Expected: ExcelUtil.java文件存在

---

### Task 2.5: 创建任务管理模块

**Files:**
- Create: `report-backend/src/main/java/com/report/service/TaskService.java`
- Create: `report-backend/src/main/java/com/report/service/impl/TaskServiceImpl.java`
- Create: `report-backend/src/main/java/com/report/controller/TaskController.java`

- [ ] **Step 1: 创建TaskService.java接口**

```java
package com.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.entity.TaskExecution;

import java.util.Map;

public interface TaskService {

    IPage<TaskExecution> page(Integer pageNum, Integer pageSize, Long reportConfigId, Integer status, String ptDt);

    TaskExecution getById(Long id);

    void trigger(Long reportConfigId, String fileName);

    void retry(Long id);

    Map<String, Long> statistics();
}
```

- [ ] **Step 2: 创建TaskServiceImpl.java实现类**

```java
package com.report.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.common.constant.ErrorCode;
import com.report.common.exception.BusinessException;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.TaskExecutionLog;
import com.report.mapper.TaskExecutionMapper;
import com.report.service.ReportConfigService;
import com.report.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskExecutionMapper, TaskExecution> implements TaskService {

    private final ReportConfigService reportConfigService;

    @Override
    public IPage<TaskExecution> page(Integer pageNum, Integer pageSize, Long reportConfigId, Integer status, String ptDt) {
        LambdaQueryWrapper<TaskExecution> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(reportConfigId != null, TaskExecution::getReportConfigId, reportConfigId);
        wrapper.eq(status != null, TaskExecution::getStatus, status);
        if (StrUtil.isNotBlank(ptDt)) {
            wrapper.eq(TaskExecution::getPtDt, DateUtil.parse(ptDt));
        }
        wrapper.orderByDesc(TaskExecution::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public TaskExecution getById(Long id) {
        TaskExecution task = super.getById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return task;
    }

    @Override
    public void trigger(Long reportConfigId, String fileName) {
        ReportConfig reportConfig = reportConfigService.getById(reportConfigId);
        TaskExecution task = new TaskExecution();
        task.setReportConfigId(reportConfigId);
        task.setFileName(fileName);
        task.setStatus(0);
        task.setPtDt(new Date());
        save(task);
    }

    @Override
    public void retry(Long id) {
        TaskExecution task = getById(id);
        if (task.getStatus() != 3) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "只有失败的任务可以重试");
        }
        task.setStatus(0);
        task.setErrorMessage(null);
        updateById(task);
    }

    @Override
    public Map<String, Long> statistics() {
        Map<String, Long> result = new HashMap<>();
        result.put("total", count());
        result.put("pending", count(new LambdaQueryWrapper<TaskExecution>().eq(TaskExecution::getStatus, 0)));
        result.put("processing", count(new LambdaQueryWrapper<TaskExecution>().eq(TaskExecution::getStatus, 1)));
        result.put("success", count(new LambdaQueryWrapper<TaskExecution>().eq(TaskExecution::getStatus, 2)));
        result.put("failed", count(new LambdaQueryWrapper<TaskExecution>().eq(TaskExecution::getStatus, 3)));
        return result;
    }
}
```

- [ ] **Step 3: 创建TaskController.java控制器**

```java
package com.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.common.result.Result;
import com.report.entity.TaskExecution;
import com.report.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long reportConfigId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String ptDt) {
        IPage<TaskExecution> page = taskService.page(pageNum, pageSize, reportConfigId, status, ptDt);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.success(result);
    }

    @GetMapping("/{id}")
    public Result<TaskExecution> getById(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @PostMapping("/trigger")
    public Result<Void> trigger(@RequestBody Map<String, Object> params) {
        Long reportConfigId = Long.valueOf(params.get("reportConfigId").toString());
        String fileName = params.get("fileName").toString();
        taskService.trigger(reportConfigId, fileName);
        return Result.success();
    }

    @PostMapping("/retry/{id}")
    public Result<Void> retry(@PathVariable Long id) {
        taskService.retry(id);
        return Result.success();
    }

    @GetMapping("/statistics")
    public Result<Map<String, Long>> statistics() {
        return Result.success(taskService.statistics());
    }
}
```

- [ ] **Step 4: 验证任务管理模块**

```bash
ls -la report-backend/src/main/java/com/report/service/
ls -la report-backend/src/main/java/com/report/service/impl/
ls -la report-backend/src/main/java/com/report/controller/
```

Expected: 任务管理相关文件存在

---

### Task 2.6: 创建日志管理模块

**Files:**
- Create: `report-backend/src/main/java/com/report/service/LogService.java`
- Create: `report-backend/src/main/java/com/report/service/impl/LogServiceImpl.java`
- Create: `report-backend/src/main/java/com/report/controller/LogController.java`

- [ ] **Step 1: 创建LogService.java接口**

```java
package com.report.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.entity.TaskExecutionLog;

public interface LogService {

    IPage<TaskExecutionLog> page(Integer pageNum, Integer pageSize, Long taskExecutionId, String logLevel);

    void log(Long taskExecutionId, String logLevel, String logMessage);

    void info(Long taskExecutionId, String logMessage);

    void warn(Long taskExecutionId, String logMessage);

    void error(Long taskExecutionId, String logMessage);
}
```

- [ ] **Step 2: 创建LogServiceImpl.java实现类**

```java
package com.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.entity.TaskExecutionLog;
import com.report.mapper.TaskExecutionLogMapper;
import com.report.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogServiceImpl extends ServiceImpl<TaskExecutionLogMapper, TaskExecutionLog> implements LogService {

    @Override
    public IPage<TaskExecutionLog> page(Integer pageNum, Integer pageSize, Long taskExecutionId, String logLevel) {
        LambdaQueryWrapper<TaskExecutionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(taskExecutionId != null, TaskExecutionLog::getTaskExecutionId, taskExecutionId);
        wrapper.eq(StrUtil.isNotBlank(logLevel), TaskExecutionLog::getLogLevel, logLevel);
        wrapper.orderByDesc(TaskExecutionLog::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void log(Long taskExecutionId, String logLevel, String logMessage) {
        TaskExecutionLog logEntity = new TaskExecutionLog();
        logEntity.setTaskExecutionId(taskExecutionId);
        logEntity.setLogLevel(logLevel);
        logEntity.setLogMessage(logMessage);
        save(logEntity);
    }

    @Override
    public void info(Long taskExecutionId, String logMessage) {
        log(taskExecutionId, "INFO", logMessage);
    }

    @Override
    public void warn(Long taskExecutionId, String logMessage) {
        log(taskExecutionId, "WARN", logMessage);
    }

    @Override
    public void error(Long taskExecutionId, String logMessage) {
        log(taskExecutionId, "ERROR", logMessage);
    }
}
```

- [ ] **Step 3: 创建LogController.java控制器**

```java
package com.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.report.common.result.Result;
import com.report.entity.TaskExecutionLog;
import com.report.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/log")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping("/list")
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long taskExecutionId,
            @RequestParam(required = false) String logLevel) {
        IPage<TaskExecutionLog> page = logService.page(pageNum, pageSize, taskExecutionId, logLevel);
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pageNum", page.getCurrent());
        result.put("pageSize", page.getSize());
        return Result.success(result);
    }

    @GetMapping("/task/{taskId}")
    public Result<Map<String, Object>> listByTask(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String logLevel) {
        return list(pageNum, pageSize, taskId, logLevel);
    }
}
```

- [ ] **Step 4: 验证日志管理模块**

```bash
ls -la report-backend/src/main/java/com/report/service/
ls -la report-backend/src/main/java/com/report/service/impl/
ls -la report-backend/src/main/java/com/report/controller/
```

Expected: 日志管理相关文件存在

---

## Phase 3: 前端基础框架

### Task 3.1: 创建前端项目结构

**Files:**
- Create: `report-front/package.json`
- Create: `report-front/vue.config.js`
- Create: `report-front/public/index.html`
- Create: `report-front/src/main.js`
- Create: `report-front/src/App.vue`

- [ ] **Step 1: 创建前端项目目录结构**

```bash
mkdir -p report-front/public
mkdir -p report-front/src/{api,components,views/{ftp,report/components,task,log,data},router,store,utils,styles}
```

- [ ] **Step 2: 创建package.json**

```json
{
  "name": "report-front",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "serve": "vue-cli-service serve",
    "build": "vue-cli-service build",
    "lint": "vue-cli-service lint"
  },
  "dependencies": {
    "axios": "^0.21.4",
    "core-js": "^3.8.3",
    "element-ui": "^2.15.14",
    "vue": "^2.6.12",
    "vue-router": "^3.5.1",
    "vuex": "^3.6.2"
  },
  "devDependencies": {
    "@vue/cli-plugin-babel": "~4.5.0",
    "@vue/cli-plugin-eslint": "~4.5.0",
    "@vue/cli-service": "~4.5.0",
    "babel-eslint": "^10.1.0",
    "eslint": "^6.7.2",
    "eslint-plugin-vue": "^6.2.2",
    "sass": "^1.26.5",
    "sass-loader": "^8.0.2",
    "vue-template-compiler": "^2.6.12"
  },
  "browserslist": [
    "> 1%",
    "last 2 versions",
    "not dead"
  ]
}
```

- [ ] **Step 3: 创建vue.config.js**

```javascript
module.exports = {
  lintOnSave: false,
  productionSourceMap: false,
  devServer: {
    port: 8081,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

- [ ] **Step 4: 创建public/index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width,initial-scale=1.0">
    <title>报表数据处理平台</title>
  </head>
  <body>
    <div id="app"></div>
  </body>
</html>
```

- [ ] **Step 5: 创建src/main.js**

```javascript
import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './styles/index.scss'

Vue.use(ElementUI)
Vue.config.productionTip = false

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
```

- [ ] **Step 6: 创建src/App.vue**

```vue
<template>
  <div id="app">
    <el-container v-if="$route.path !== '/login'">
      <el-aside width="200px">
        <el-menu
          :default-active="$route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF">
          <el-menu-item index="/ftp">
            <i class="el-icon-connection"></i>
            <span>FTP配置</span>
          </el-menu-item>
          <el-menu-item index="/report">
            <i class="el-icon-document"></i>
            <span>报表配置</span>
          </el-menu-item>
          <el-menu-item index="/task">
            <i class="el-icon-monitor"></i>
            <span>任务监控</span>
          </el-menu-item>
          <el-menu-item index="/log">
            <i class="el-icon-tickets"></i>
            <span>执行日志</span>
          </el-menu-item>
          <el-menu-item index="/data">
            <i class="el-icon-data-analysis"></i>
            <span>数据查询</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
    <router-view v-else />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style lang="scss">
#app {
  height: 100vh;
  .el-container {
    height: 100%;
  }
  .el-aside {
    background-color: #304156;
    .el-menu {
      border-right: none;
    }
  }
  .el-main {
    padding: 20px;
    background-color: #f0f2f5;
  }
}
</style>
```

- [ ] **Step 7: 验证前端项目结构**

```bash
ls -la report-front/
ls -la report-front/src/
```

Expected: 前端项目结构正确创建

---

### Task 3.2: 创建前端路由和工具类

**Files:**
- Create: `report-front/src/router/index.js`
- Create: `report-front/src/store/index.js`
- Create: `report-front/src/utils/request.js`
- Create: `report-front/src/styles/index.scss`

- [ ] **Step 1: 创建src/router/index.js**

```javascript
import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/ftp'
  },
  {
    path: '/ftp',
    name: 'FtpConfig',
    component: () => import('@/views/ftp/FtpConfig.vue'),
    meta: { title: 'FTP配置' }
  },
  {
    path: '/report',
    name: 'ReportList',
    component: () => import('@/views/report/ReportList.vue'),
    meta: { title: '报表配置' }
  },
  {
    path: '/task',
    name: 'TaskMonitor',
    component: () => import('@/views/task/TaskMonitor.vue'),
    meta: { title: '任务监控' }
  },
  {
    path: '/log',
    name: 'LogList',
    component: () => import('@/views/log/LogList.vue'),
    meta: { title: '执行日志' }
  },
  {
    path: '/data',
    name: 'DataQuery',
    component: () => import('@/views/data/DataQuery.vue'),
    meta: { title: '数据查询' }
  }
]

const router = new VueRouter({
  routes
})

export default router
```

- [ ] **Step 2: 创建src/store/index.js**

```javascript
import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
  },
  mutations: {
  },
  actions: {
  },
  modules: {
  }
})
```

- [ ] **Step 3: 创建src/utils/request.js**

```javascript
import axios from 'axios'
import { Message } from 'element-ui'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

service.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  error => {
    Message.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
```

- [ ] **Step 4: 创建src/styles/index.scss**

```scss
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

.page-container {
  background: #fff;
  padding: 20px;
  border-radius: 4px;
  
  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .page-title {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }
  
  .search-form {
    margin-bottom: 20px;
  }
  
  .pagination-container {
    margin-top: 20px;
    text-align: right;
  }
}
```

- [ ] **Step 5: 验证路由和工具类**

```bash
ls -la report-front/src/router/
ls -la report-front/src/store/
ls -la report-front/src/utils/
ls -la report-front/src/styles/
```

Expected: 路由、store、工具类、样式文件存在

---

### Task 3.3: 创建前端API接口

**Files:**
- Create: `report-front/src/api/ftpConfig.js`
- Create: `report-front/src/api/reportConfig.js`
- Create: `report-front/src/api/task.js`
- Create: `report-front/src/api/log.js`

- [ ] **Step 1: 创建src/api/ftpConfig.js**

```javascript
import request from '@/utils/request'

export function listFtpConfig(params) {
  return request({
    url: '/ftp-config/list',
    method: 'get',
    params
  })
}

export function getFtpConfig(id) {
  return request({
    url: `/ftp-config/${id}`,
    method: 'get'
  })
}

export function saveFtpConfig(data) {
  return request({
    url: '/ftp-config',
    method: 'post',
    data
  })
}

export function updateFtpConfig(id, data) {
  return request({
    url: `/ftp-config/${id}`,
    method: 'put',
    data
  })
}

export function deleteFtpConfig(id) {
  return request({
    url: `/ftp-config/${id}`,
    method: 'delete'
  })
}

export function testFtpConnection(data) {
  return request({
    url: '/ftp-config/test',
    method: 'post',
    data
  })
}
```

- [ ] **Step 2: 创建src/api/reportConfig.js**

```javascript
import request from '@/utils/request'

export function listReportConfig(params) {
  return request({
    url: '/report-config/list',
    method: 'get',
    params
  })
}

export function getReportConfig(id) {
  return request({
    url: `/report-config/${id}`,
    method: 'get'
  })
}

export function saveReportConfig(data) {
  return request({
    url: '/report-config',
    method: 'post',
    data
  })
}

export function updateReportConfig(id, data) {
  return request({
    url: `/report-config/${id}`,
    method: 'put',
    data
  })
}

export function deleteReportConfig(id) {
  return request({
    url: `/report-config/${id}`,
    method: 'delete'
  })
}

export function updateReportStatus(id, status) {
  return request({
    url: `/report-config/${id}/status`,
    method: 'put',
    params: { status }
  })
}

export function copyReportConfig(id) {
  return request({
    url: `/report-config/${id}/copy`,
    method: 'post'
  })
}
```

- [ ] **Step 3: 创建src/api/task.js**

```javascript
import request from '@/utils/request'

export function listTask(params) {
  return request({
    url: '/task/list',
    method: 'get',
    params
  })
}

export function getTask(id) {
  return request({
    url: `/task/${id}`,
    method: 'get'
  })
}

export function triggerTask(data) {
  return request({
    url: '/task/trigger',
    method: 'post',
    data
  })
}

export function retryTask(id) {
  return request({
    url: `/task/retry/${id}`,
    method: 'post'
  })
}

export function getTaskStatistics() {
  return request({
    url: '/task/statistics',
    method: 'get'
  })
}
```

- [ ] **Step 4: 创建src/api/log.js**

```javascript
import request from '@/utils/request'

export function listLog(params) {
  return request({
    url: '/log/list',
    method: 'get',
    params
  })
}

export function listLogByTask(taskId, params) {
  return request({
    url: `/log/task/${taskId}`,
    method: 'get',
    params
  })
}
```

- [ ] **Step 5: 验证API文件**

```bash
ls -la report-front/src/api/
```

Expected: API接口文件存在

---

## Phase 4: 前端页面开发

### Task 4.1: 创建FTP配置页面

**Files:**
- Create: `report-front/src/views/ftp/FtpConfig.vue`

- [ ] **Step 1: 创建src/views/ftp/FtpConfig.vue**

```vue
<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">FTP配置管理</span>
      <el-button type="primary" @click="handleAdd">新增配置</el-button>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="配置名称">
          <el-input v-model="queryParams.name" placeholder="请输入配置名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" border>
      <el-table-column prop="name" label="配置名称" />
      <el-table-column prop="host" label="主机地址" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="encoding" label="编码" width="100" />
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200">
        <template slot-scope="scope">
          <el-button type="text" @click="handleTest(scope.row)">测试连接</el-button>
          <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
    
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="500px">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="主机地址" prop="host">
          <el-input v-model="form.host" placeholder="请输入主机地址" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item label="编码" prop="encoding">
          <el-input v-model="form.encoding" placeholder="请输入编码" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listFtpConfig, getFtpConfig, saveFtpConfig, updateFtpConfig, deleteFtpConfig, testFtpConnection } from '@/api/ftpConfig'

export default {
  name: 'FtpConfig',
  data() {
    return {
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: '',
        status: null
      },
      tableData: [],
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        name: '',
        host: '',
        port: 21,
        username: '',
        password: '',
        encoding: 'UTF-8',
        status: 1
      },
      rules: {
        name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
        port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    async loadList() {
      const res = await listFtpConfig(this.queryParams)
      this.tableData = res.data.records
      this.total = res.data.total
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    handleReset() {
      this.queryParams = { pageNum: 1, pageSize: 10, name: '', status: null }
      this.loadList()
    },
    handlePageChange(page) {
      this.queryParams.pageNum = page
      this.loadList()
    },
    handleAdd() {
      this.dialogTitle = '新增FTP配置'
      this.form = { id: null, name: '', host: '', port: 21, username: '', password: '', encoding: 'UTF-8', status: 1 }
      this.dialogVisible = true
    },
    async handleEdit(row) {
      this.dialogTitle = '编辑FTP配置'
      const res = await getFtpConfig(row.id)
      this.form = res.data
      this.dialogVisible = true
    },
    async handleTest(row) {
      try {
        await testFtpConnection(row)
        this.$message.success('连接成功')
      } catch (e) {
        this.$message.error('连接失败')
      }
    },
    handleDelete(row) {
      this.$confirm('确定要删除该配置吗?', '提示', { type: 'warning' }).then(async () => {
        await deleteFtpConfig(row.id)
        this.$message.success('删除成功')
        this.loadList()
      })
    },
    handleSubmit() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          if (this.form.id) {
            await updateFtpConfig(this.form.id, this.form)
          } else {
            await saveFtpConfig(this.form)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadList()
        }
      })
    }
  }
}
</script>
```

- [ ] **Step 2: 验证FTP配置页面**

```bash
ls -la report-front/src/views/ftp/
```

Expected: FtpConfig.vue文件存在

---

### Task 4.2: 创建报表配置页面

**Files:**
- Create: `report-front/src/views/report/ReportList.vue`

- [ ] **Step 1: 创建src/views/report/ReportList.vue**

```vue
<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">报表配置管理</span>
      <el-button type="primary" @click="handleAdd">新增报表</el-button>
    </div>
    
    <div class="search-form">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="报表名称">
          <el-input v-model="queryParams.name" placeholder="请输入报表名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" border>
      <el-table-column prop="name" label="报表名称" />
      <el-table-column prop="code" label="报表编码" width="120" />
      <el-table-column prop="ftpConfigName" label="FTP配置" />
      <el-table-column prop="scanPath" label="扫描路径" />
      <el-table-column prop="scanInterval" label="扫描间隔(分钟)" width="120" />
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280">
        <template slot-scope="scope">
          <el-button type="text" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" @click="handleCopy(scope.row)">复制</el-button>
          <el-button type="text" @click="handleStatus(scope.row)">
            {{ scope.row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button type="text" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
    
    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="900px" :close-on-click-modal="false">
      <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 20px;">
        <el-step title="基本信息" />
        <el-step title="扫描配置" />
        <el-step title="列映射配置" />
        <el-step title="输出配置" />
      </el-steps>
      
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <div v-show="activeStep === 0">
          <el-form-item label="报表名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入报表名称" />
          </el-form-item>
          <el-form-item label="报表编码" prop="code">
            <el-input v-model="form.code" placeholder="请输入报表编码" />
          </el-form-item>
          <el-form-item label="FTP配置" prop="ftpConfigId">
            <el-select v-model="form.ftpConfigId" placeholder="请选择FTP配置" style="width: 100%;">
              <el-option v-for="item in ftpOptions" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
          </el-form-item>
        </div>
        
        <div v-show="activeStep === 1">
          <el-form-item label="扫描路径" prop="scanPath">
            <el-input v-model="form.scanPath" placeholder="请输入扫描路径，如 /data/sales" />
          </el-form-item>
          <el-form-item label="文件规则" prop="filePattern">
            <el-input v-model="form.filePattern" placeholder="请输入文件名正则，如 ^sales_\d{8}\.xlsx$" />
          </el-form-item>
          <el-form-item label="扫描间隔" prop="scanInterval">
            <el-input-number v-model="form.scanInterval" :min="1" :max="1440" /> 分钟
          </el-form-item>
          <el-form-item label="Sheet索引" prop="sheetIndex">
            <el-input-number v-model="form.sheetIndex" :min="0" />
          </el-form-item>
          <el-form-item label="表头行号" prop="headerRow">
            <el-input-number v-model="form.headerRow" :min="1" />
          </el-form-item>
          <el-form-item label="数据起始行" prop="dataStartRow">
            <el-input-number v-model="form.dataStartRow" :min="1" />
          </el-form-item>
        </div>
        
        <div v-show="activeStep === 2">
          <el-table :data="form.columnMapping.mappings" border style="margin-bottom: 10px;">
            <el-table-column prop="excelColumnName" label="Excel列名">
              <template slot-scope="scope">
                <el-input v-model="scope.row.excelColumnName" placeholder="Excel列名" />
              </template>
            </el-table-column>
            <el-table-column prop="fieldName" label="目标字段名">
              <template slot-scope="scope">
                <el-input v-model="scope.row.fieldName" placeholder="字段名" />
              </template>
            </el-table-column>
            <el-table-column prop="fieldType" label="字段类型" width="120">
              <template slot-scope="scope">
                <el-select v-model="scope.row.fieldType">
                  <el-option label="字符串" value="STRING" />
                  <el-option label="整数" value="INTEGER" />
                  <el-option label="小数" value="DECIMAL" />
                  <el-option label="日期" value="DATE" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column prop="defaultValue" label="默认值">
              <template slot-scope="scope">
                <el-input v-model="scope.row.defaultValue" placeholder="默认值" />
              </template>
            </el-table-column>
            <el-table-column prop="required" label="必填" width="80">
              <template slot-scope="scope">
                <el-checkbox v-model="scope.row.required" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template slot-scope="scope">
                <el-button type="text" @click="removeMapping(scope.$index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button type="primary" size="small" @click="addMapping">添加映射</el-button>
        </div>
        
        <div v-show="activeStep === 3">
          <el-form-item label="输出类型" prop="outputType">
            <el-radio-group v-model="form.outputType">
              <el-radio :label="1">数据库</el-radio>
              <el-radio :label="2">文件</el-radio>
              <el-radio :label="3">混合</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="form.outputType !== 2" label="目标表名" prop="outputTable">
            <el-input v-model="form.outputTable" placeholder="请输入目标表名" />
          </el-form-item>
          <el-form-item v-if="form.outputType !== 1" label="输出路径" prop="outputPath">
            <el-input v-model="form.outputPath" placeholder="请输入文件输出路径" />
          </el-form-item>
        </div>
      </el-form>
      
      <div slot="footer">
        <el-button v-if="activeStep > 0" @click="prevStep">上一步</el-button>
        <el-button v-if="activeStep < 3" type="primary" @click="nextStep">下一步</el-button>
        <el-button v-if="activeStep === 3" type="primary" @click="handleSubmit">提交</el-button>
        <el-button @click="dialogVisible = false">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listReportConfig, getReportConfig, saveReportConfig, updateReportConfig, deleteReportConfig, updateReportStatus, copyReportConfig } from '@/api/reportConfig'
import { listFtpConfig } from '@/api/ftpConfig'

export default {
  name: 'ReportList',
  data() {
    return {
      queryParams: { pageNum: 1, pageSize: 10, name: '', status: null },
      tableData: [],
      total: 0,
      dialogVisible: false,
      dialogTitle: '',
      activeStep: 0,
      ftpOptions: [],
      form: this.getEmptyForm(),
      rules: {
        name: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
        code: [{ required: true, message: '请输入报表编码', trigger: 'blur' }],
        ftpConfigId: [{ required: true, message: '请选择FTP配置', trigger: 'change' }],
        scanPath: [{ required: true, message: '请输入扫描路径', trigger: 'blur' }],
        filePattern: [{ required: true, message: '请输入文件规则', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadList()
    this.loadFtpOptions()
  },
  methods: {
    getEmptyForm() {
      return {
        id: null,
        name: '',
        code: '',
        ftpConfigId: null,
        scanPath: '',
        filePattern: '',
        scanInterval: 10,
        sheetIndex: 0,
        headerRow: 1,
        dataStartRow: 2,
        columnMapping: { mappings: [] },
        outputType: 1,
        outputTable: '',
        outputPath: '',
        status: 1,
        remark: ''
      }
    },
    async loadList() {
      const res = await listReportConfig(this.queryParams)
      this.tableData = res.data.records
      this.total = res.data.total
    },
    async loadFtpOptions() {
      const res = await listFtpConfig({ pageNum: 1, pageSize: 1000, status: 1 })
      this.ftpOptions = res.data.records
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    handleReset() {
      this.queryParams = { pageNum: 1, pageSize: 10, name: '', status: null }
      this.loadList()
    },
    handlePageChange(page) {
      this.queryParams.pageNum = page
      this.loadList()
    },
    handleAdd() {
      this.dialogTitle = '新增报表配置'
      this.activeStep = 0
      this.form = this.getEmptyForm()
      this.dialogVisible = true
    },
    async handleEdit(row) {
      this.dialogTitle = '编辑报表配置'
      this.activeStep = 0
      const res = await getReportConfig(row.id)
      this.form = res.data
      if (!this.form.columnMapping) {
        this.form.columnMapping = { mappings: [] }
      }
      this.dialogVisible = true
    },
    handleCopy(row) {
      this.$confirm('确定要复制该报表配置吗?', '提示', { type: 'warning' }).then(async () => {
        await copyReportConfig(row.id)
        this.$message.success('复制成功')
        this.loadList()
      })
    },
    handleStatus(row) {
      const newStatus = row.status === 1 ? 0 : 1
      const action = newStatus === 1 ? '启用' : '禁用'
      this.$confirm(`确定要${action}该报表吗?`, '提示', { type: 'warning' }).then(async () => {
        await updateReportStatus(row.id, newStatus)
        this.$message.success(`${action}成功`)
        this.loadList()
      })
    },
    handleDelete(row) {
      this.$confirm('确定要删除该报表配置吗?', '提示', { type: 'warning' }).then(async () => {
        await deleteReportConfig(row.id)
        this.$message.success('删除成功')
        this.loadList()
      })
    },
    prevStep() {
      this.activeStep--
    },
    nextStep() {
      this.$refs.form.validate((valid) => {
        if (valid) {
          this.activeStep++
        }
      })
    },
    addMapping() {
      this.form.columnMapping.mappings.push({
        excelColumnName: '',
        fieldName: '',
        fieldType: 'STRING',
        defaultValue: '',
        required: false
      })
    },
    removeMapping(index) {
      this.form.columnMapping.mappings.splice(index, 1)
    },
    handleSubmit() {
      this.$refs.form.validate(async (valid) => {
        if (valid) {
          if (this.form.id) {
            await updateReportConfig(this.form.id, this.form)
          } else {
            await saveReportConfig(this.form)
          }
          this.$message.success('保存成功')
          this.dialogVisible = false
          this.loadList()
        }
      })
    }
  }
}
</script>
```

- [ ] **Step 2: 验证报表配置页面**

```bash
ls -la report-front/src/views/report/
```

Expected: ReportList.vue文件存在

---

### Task 4.3: 创建任务监控页面

**Files:**
- Create: `report-front/src/views/task/TaskMonitor.vue`

- [ ] **Step 1: 创建src/views/task/TaskMonitor.vue**

```vue
<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">任务监控中心</span>
    </div>
    
    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="4">
        <el-card shadow="hover">
          <div>总任务</div>
          <div style="font-size: 24px; font-weight: bold;">{{ statistics.total || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div>待处理</div>
          <div style="font-size: 24px; font-weight: bold; color: #909399;">{{ statistics.pending || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div>处理中</div>
          <div style="font-size: 24px; font-weight: bold; color: #E6A23C;">{{ statistics.processing || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div>成功</div>
          <div style="font-size: 24px; font-weight: bold; color: #67C23A;">{{ statistics.success || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover">
          <div>失败</div>
          <div style="font-size: 24px; font-weight: bold; color: #F56C6C;">{{ statistics.failed || 0 }}</div>
        </el-card>
      </el-col>
    </el-row>
    
    <div class="search-form">
      <el-form :inline="true" :model="queryParams">
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
            <el-option label="待处理" :value="0" />
            <el-option label="处理中" :value="1" />
            <el-option label="成功" :value="2" />
            <el-option label="失败" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据日期">
          <el-date-picker v-model="queryParams.ptDt" type="date" value-format="yyyy-MM-dd" placeholder="选择日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <el-table :data="tableData" border>
      <el-table-column prop="fileName" label="文件名" />
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">{{ getStatusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="160" />
      <el-table-column prop="endTime" label="结束时间" width="160" />
      <el-table-column prop="totalRows" label="总行数" width="80" />
      <el-table-column prop="successRows" label="成功行数" width="90" />
      <el-table-column prop="errorRows" label="失败行数" width="90" />
      <el-table-column prop="ptDt" label="数据日期" width="120" />
      <el-table-column label="操作" width="150">
        <template slot-scope="scope">
          <el-button type="text" @click="handleViewLog(scope.row)">查看日志</el-button>
          <el-button v-if="scope.row.status === 3" type="text" @click="handleRetry(scope.row)">重试</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <div class="pagination-container">
      <el-pagination
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>
    
    <el-dialog title="执行日志" :visible.sync="logDialogVisible" width="800px">
      <el-table :data="logData" border max-height="400">
        <el-table-column prop="logLevel" label="级别" width="80">
          <template slot-scope="scope">
            <el-tag :type="getLogLevelType(scope.row.logLevel)">{{ scope.row.logLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="logMessage" label="日志内容" />
        <el-table-column prop="createTime" label="时间" width="160" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listTask, retryTask, getTaskStatistics } from '@/api/task'
import { listLogByTask } from '@/api/log'

export default {
  name: 'TaskMonitor',
  data() {
    return {
      queryParams: { pageNum: 1, pageSize: 10, status: null, ptDt: '' },
      tableData: [],
      total: 0,
      statistics: {},
      logDialogVisible: false,
      logData: []
    }
  },
  created() {
    this.loadList()
    this.loadStatistics()
  },
  methods: {
    async loadList() {
      const res = await listTask(this.queryParams)
      this.tableData = res.data.records
      this.total = res.data.total
    },
    async loadStatistics() {
      const res = await getTaskStatistics()
      this.statistics = res.data
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    handleReset() {
      this.queryParams = { pageNum: 1, pageSize: 10, status: null, ptDt: '' }
      this.loadList()
    },
    handlePageChange(page) {
      this.queryParams.pageNum = page
      this.loadList()
    },
    getStatusType(status) {
      const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
      return map[status]
    },
    getStatusText(status) {
      const map = { 0: '待处理', 1: '处理中', 2: '成功', 3: '失败' }
      return map[status]
    },
    getLogLevelType(level) {
      const map = { INFO: 'info', WARN: 'warning', ERROR: 'danger' }
      return map[level]
    },
    async handleViewLog(row) {
      const res = await listLogByTask(row.id, { pageNum: 1, pageSize: 1000 })
      this.logData = res.data.records
      this.logDialogVisible = true
    },
    handleRetry(row) {
      this.$confirm('确定要重试该任务吗?', '提示', { type: 'warning' }).then(async () => {
        await retryTask(row.id)
        this.$message.success('重试任务已提交')
        this.loadList()
        this.loadStatistics()
      })
    }
  }
}
</script>
```

- [ ] **Step 2: 验证任务监控页面**

```bash
ls -la report-front/src/views/task/
```

Expected: TaskMonitor.vue文件存在

---

## 执行总结

本实施计划分为4个主要阶段：

| 阶段 | 任务数 | 主要内容 |
|------|--------|----------|
| Phase 1 | 7个任务 | 后端基础框架（项目结构、公共模块、数据库、实体类、Mapper） |
| Phase 2 | 6个任务 | 后端核心业务模块（FTP配置、报表配置、任务管理、日志管理、工具类） |
| Phase 3 | 3个任务 | 前端基础框架（项目结构、路由、API接口） |
| Phase 4 | 3个任务 | 前端页面开发（FTP配置、报表配置、任务监控） |

**总计：19个任务**

每个任务都包含详细的步骤、完整的代码示例和验证命令，确保工程师可以独立完成开发工作。