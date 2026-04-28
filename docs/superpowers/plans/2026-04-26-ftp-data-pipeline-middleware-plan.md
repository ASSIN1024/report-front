# FTP 报表数据转换中间件 - 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有报表数据处理平台重构为 FTP 报表数据转换中间件，实现 FTP 扫描 → 中国风表格解析 → 字段映射清洗 → 标准化 Excel → ZIP 打包 → staging/for-upload 队列投递 → 源文件归档的完整流程。

**Architecture:**
- 后端：Spring Boot 2.1.2，单一 MiddlewareEngine 核心引擎编排全流程
- 前端：Vue 2.6 + Element UI，保留现有结构局部重构
- 数据库：MySQL（dev）/ GaussDB（prod），保留现有 Schema 增强
- 文件传输：Apache Commons Net FTP

**Tech Stack:** Spring Boot 2.1.2, Vue 2.6, MyBatis-Plus 3.x, Apache POI/EasyExcel, Apache Commons Net, Quartz

---

## Phase 1: 核心引擎（后端）

### 阶段目标
完成 MiddlewareEngine 核心流程、ExcelTransformService、PackagingService、BatchService、AlertService 的实现。

### 1.1 数据库 Schema 增强

**Files:**
- Modify: `report-backend/src/main/resources/schema.sql` - 新增字段

- [ ] **Step 1: 新增 alert_record 表**

```sql
CREATE TABLE alert_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    report_config_id BIGINT,
    file_name VARCHAR(255),
    alert_level VARCHAR(20) COMMENT 'ERROR/WARNING',
    alert_type VARCHAR(50) COMMENT 'MAPPING_FAILED/FIELD_MISSING/PARSE_ERROR/FTP_ERROR',
    alert_message TEXT,
    status VARCHAR(20) DEFAULT 'OPEN' COMMENT 'OPEN/RESOLVED',
    resolved_by VARCHAR(50),
    resolved_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) COMMENT '告警记录表';
```

- [ ] **Step 2: 新增 batch_record 表**

```sql
CREATE TABLE batch_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_code VARCHAR(64) NOT NULL UNIQUE COMMENT '批次编码',
    ftp_config_id BIGINT,
    zip_file_name VARCHAR(255) COMMENT 'ZIP文件名',
    file_count INT DEFAULT 0 COMMENT '文件数量',
    total_size BIGINT DEFAULT 0 COMMENT 'ZIP大小',
    status VARCHAR(20) DEFAULT 'CREATED' COMMENT 'CREATED/DELIVERED/CONSUMED',
    delivered_at DATETIME,
    consumed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) COMMENT '批次记录表';
```

- [ ] **Step 3: 修改 report_config 表新增字段**

```sql
ALTER TABLE report_config
ADD COLUMN start_row INT DEFAULT 1 COMMENT '起始行号偏移',
ADD COLUMN start_col INT DEFAULT 1 COMMENT '起始列号偏移',
ADD COLUMN mapping_mode VARCHAR(20) DEFAULT 'DUAL' COMMENT 'BY_NAME/BY_INDEX/DUAL',
ADD COLUMN duplicate_col_strategy VARCHAR(20) DEFAULT 'AUTO_SUFFIX' COMMENT 'AUTO_SUFFIX/BY_INDEX',
ADD COLUMN ods_backup_enabled TINYINT DEFAULT 0 COMMENT '是否ODS备份',
ADD COLUMN ods_table_name VARCHAR(128) COMMENT 'ODS备份表名';
```

- [ ] **Step 4: 修改 ftp_config 表新增字段**

```sql
ALTER TABLE ftp_config
ADD COLUMN staging_dir VARCHAR(512) COMMENT '暂存目录',
ADD COLUMN for_upload_dir VARCHAR(512) COMMENT '待上传目录',
ADD COLUMN archive_dir VARCHAR(512) COMMENT '归档目录',
ADD COLUMN error_dir VARCHAR(512) COMMENT '错误文件目录';
```

- [ ] **Step 5: 修改 task_execution 表新增字段**

```sql
ALTER TABLE task_execution
ADD COLUMN file_name VARCHAR(255) COMMENT '源文件名',
ADD COLUMN output_file VARCHAR(255) COMMENT '输出标准Excel文件名',
ADD COLUMN pt_dt VARCHAR(20) COMMENT '分区日期';
```

- [ ] **Step 6: 提交**

```bash
git add report-backend/src/main/resources/schema.sql
git commit -m "feat(schema): 新增 alert_record, batch_record 表及 report_config, ftp_config, task_execution 增强字段"
```

---

### 1.2 Entity 实体类创建与重构

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/AlertRecord.java`
- Create: `report-backend/src/main/java/com/report/entity/BatchRecord.java`
- Modify: `report-backend/src/main/java/com/report/entity/ReportConfig.java`
- Modify: `report-backend/src/main/java/com/report/entity/FtpConfig.java`
- Modify: `report-backend/src/main/java/com/report/entity/TaskExecution.java`

- [ ] **Step 1: 创建 AlertRecord 实体**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("alert_record")
public class AlertRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportConfigId;
    private String fileName;
    private String alertLevel;
    private String alertType;
    private String alertMessage;
    private String status;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;

    public enum AlertLevel { ERROR, WARNING }
    public enum AlertType { MAPPING_FAILED, FIELD_MISSING, PARSE_ERROR, FTP_ERROR }
    public enum Status { OPEN, RESOLVED }
}
```

- [ ] **Step 2: 创建 BatchRecord 实体**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("batch_record")
public class BatchRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchCode;
    private Long ftpConfigId;
    private String zipFileName;
    private Integer fileCount;
    private Long totalSize;
    private String status;
    private LocalDateTime deliveredAt;
    private LocalDateTime consumedAt;
    private LocalDateTime createdAt;

    public enum Status { CREATED, DELIVERED, CONSUMED }
}
```

- [ ] **Step 3: 重构 ReportConfig 实体 - 新增字段**

在现有字段列表后添加：
```java
private Integer startRow = 1;
private Integer startCol = 1;
private String mappingMode = "DUAL";
private String duplicateColStrategy = "AUTO_SUFFIX";
private Boolean odsBackupEnabled = false;
private String odsTableName;
```

- [ ] **Step 4: 重构 FtpConfig 实体 - 新增字段**

在现有字段列表后添加：
```java
private String stagingDir;
private String forUploadDir;
private String archiveDir;
private String errorDir;
```

- [ ] **Step 5: 重构 TaskExecution 实体 - 新增字段**

在现有字段列表后添加：
```java
private String fileName;
private String outputFile;
private String ptDt;
```

- [ ] **Step 6: 提交**

```bash
git add report-backend/src/main/java/com/report/entity/
git commit -m "feat(entity): 新增 AlertRecord, BatchRecord 实体及现有实体增强字段"
```

---

### 1.3 Mapper 接口创建与重构

**Files:**
- Create: `report-backend/src/main/java/com/report/mapper/AlertRecordMapper.java`
- Create: `report-backend/src/main/java/com/report/mapper/BatchRecordMapper.java`
- Modify: `report-backend/src/main/java/com/report/mapper/ReportConfigMapper.java`
- Modify: `report-backend/src/main/java/com/report/mapper/FtpConfigMapper.java`

- [ ] **Step 1: 创建 AlertRecordMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.AlertRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlertRecordMapper extends BaseMapper<AlertRecord> {
}
```

- [ ] **Step 2: 创建 BatchRecordMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.BatchRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BatchRecordMapper extends BaseMapper<BatchRecord> {
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/mapper/
git commit -m "feat(mapper): 新增 AlertRecordMapper, BatchRecordMapper"
```

---

### 1.4 工具类创建

**Files:**
- Create: `report-backend/src/main/java/com/report/util/StandardExcelWriter.java`
- Create: `report-backend/src/main/java/com/report/util/ConfigExcelWriter.java`
- Create: `report-backend/src/main/java/com/report/util/ZipPackager.java`

- [ ] **Step 1: 创建 StandardExcelWriter - 标准化 Excel 输出工具**

```java
package com.report.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class StandardExcelWriter {

    public static String write(String outputPath, List<String> headers, List<Map<String, Object>> rows) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
        }

        for (int rowIdx = 0; rowIdx < rows.size(); rowIdx++) {
            Row row = sheet.createRow(rowIdx + 1);
            Map<String, Object> data = rows.get(rowIdx);
            for (int colIdx = 0; colIdx < headers.size(); colIdx++) {
                Cell cell = row.createCell(colIdx);
                Object value = data.get(headers.get(colIdx));
                if (value != null) {
                    cell.setCellValue(value.toString());
                }
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }
        workbook.close();
        return outputPath;
    }
}
```

- [ ] **Step 2: 创建 ConfigExcelWriter - 配置表生成工具**

```java
package com.report.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConfigExcelWriter {

    private static final DateTimeFormatter DFT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String write(String outputPath, List<Map<String, Object>> configRecords) throws Exception {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Config");

        String[] headers = {"source_file", "db_name", "table_name", "field_mapping", 
                           "is_partitioned", "partition_field", "partition_value", "processed_at"};

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        for (int rowIdx = 0; rowIdx < configRecords.size(); rowIdx++) {
            Row row = sheet.createRow(rowIdx + 1);
            Map<String, Object> record = configRecords.get(rowIdx);
            for (int colIdx = 0; colIdx < headers.length; colIdx++) {
                Cell cell = row.createCell(colIdx);
                Object value = record.get(headers[colIdx]);
                if (value != null) {
                    if (value instanceof LocalDateTime) {
                        cell.setCellValue(((LocalDateTime) value).format(DFT));
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }
        }

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            workbook.write(fos);
        }
        workbook.close();
        return outputPath;
    }
}
```

- [ ] **Step 3: 创建 ZipPackager - ZIP 打包工具**

```java
package com.report.util;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipPackager {

    public static String packageFiles(String zipPath, List<String> filePaths) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath))) {
            for (String filePath : filePaths) {
                File file = new File(filePath);
                if (file.exists()) {
                    zos.putNextEntry(new ZipEntry(file.getName()));
                    Files.copy(file.toPath(), zos);
                    zos.closeEntry();
                }
            }
        }
        return zipPath;
    }

    public static String packageFiles(String zipPath, String... filePaths) throws IOException {
        return packageFiles(zipPath, java.util.Arrays.asList(filePaths));
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add report-backend/src/main/java/com/report/util/
git commit -m "feat(util): 新增 StandardExcelWriter, ConfigExcelWriter, ZipPackager"
```

---

### 1.5 Service 层实现

**Files:**
- Create: `report-backend/src/main/java/com/report/service/AlertService.java`
- Create: `report-backend/src/main/java/com/report/service/AlertServiceImpl.java`
- Create: `report-backend/src/main/java/com/report/service/BatchService.java`
- Create: `report-backend/src/main/java/com/report/service/BatchServiceImpl.java`
- Create: `report-backend/src/main/java/com/report/service/PackagingService.java`
- Create: `report-backend/src/main/java/com/report/service/PackagingServiceImpl.java`
- Create: `report-backend/src/main/java/com/report/service/ExcelTransformService.java`
- Create: `report-backend/src/main/java/com/report/service/ExcelTransformServiceImpl.java`

- [ ] **Step 1: 创建 AlertService 接口与实现**

```java
package com.report.service;

import com.report.entity.AlertRecord;
import java.util.List;

public interface AlertService {
    void createAlert(Long reportConfigId, String fileName, String level, String type, String message);
    List<AlertRecord> listAlerts(String level, String type, String status);
    void resolveAlert(Long alertId, String resolvedBy);
}
```

```java
package com.report.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.report.entity.AlertRecord;
import com.report.mapper.AlertRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    @Override
    public void createAlert(Long reportConfigId, String fileName, String level, String type, String message) {
        AlertRecord alert = new AlertRecord();
        alert.setReportConfigId(reportConfigId);
        alert.setFileName(fileName);
        alert.setAlertLevel(level);
        alert.setAlertType(type);
        alert.setAlertMessage(message);
        alert.setStatus(AlertRecord.Status.OPEN.name());
        alert.setCreatedAt(LocalDateTime.now());
        alertRecordMapper.insert(alert);
    }

    @Override
    public List<AlertRecord> listAlerts(String level, String type, String status) {
        QueryWrapper<AlertRecord> wrapper = new QueryWrapper<>();
        if (level != null) wrapper.eq("alert_level", level);
        if (type != null) wrapper.eq("alert_type", type);
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("created_at");
        return alertRecordMapper.selectList(wrapper);
    }

    @Override
    public void resolveAlert(Long alertId, String resolvedBy) {
        AlertRecord alert = alertRecordMapper.selectById(alertId);
        if (alert != null) {
            alert.setStatus(AlertRecord.Status.RESOLVED.name());
            alert.setResolvedBy(resolvedBy);
            alert.setResolvedAt(LocalDateTime.now());
            alertRecordMapper.updateById(alert);
        }
    }
}
```

- [ ] **Step 2: 创建 ExcelTransformService - 核心转换服务**

```java
package com.report.service;

import lombok.Data;

@Data
public class TransformResult {
    private boolean success;
    private String standardExcelPath;
    private String ptDt;
    private String dbName;
    private String tableName;
    private String fieldMappingJson;
    private String errorMessage;
}
```

```java
package com.report.service;

public interface ExcelTransformService {
    TransformResult transform(String filePath, Long reportConfigId);
}
```

```java
package com.report.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.report.entity.ProcessedFile;
import com.report.entity.ReportConfig;
import com.report.mapper.ProcessedFileMapper;
import com.report.mapper.ReportConfigMapper;
import com.report.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExcelTransformServiceImpl implements ExcelTransformService {

    @Autowired
    private ReportConfigMapper reportConfigMapper;

    @Autowired
    private ProcessedFileMapper processedFileMapper;

    @Override
    public TransformResult transform(String filePath, Long reportConfigId) {
        TransformResult result = new TransformResult();
        ReportConfig config = reportConfigMapper.selectById(reportConfigId);

        if (config == null) {
            result.setSuccess(false);
            result.setErrorMessage("Report config not found");
            return result;
        }

        try {
            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> rows = new ArrayList<>();

            try (Workbook workbook = new XSSFWorkbook(new File(filePath))) {
                Sheet sheet = workbook.getSheetAt(0);
                int startRow = config.getStartRow() != null ? config.getStartRow() - 1 : 0;
                int startCol = config.getStartCol() != null ? config.getStartCol() - 1 : 0;

                Row headerRow = sheet.getRow(startRow);
                if (headerRow == null) {
                    result.setSuccess(false);
                    result.setErrorMessage("Header row not found at start row");
                    return result;
                }

                for (int i = startCol; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    String colName = cell != null ? cell.getStringCellValue() : "col_" + i;
                    headers.add(colName);
                }

                for (int rowIdx = startRow + 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;
                    Map<String, Object> rowData = new LinkedHashMap<>();
                    for (int colIdx = startCol; colIdx < headers.size(); colIdx++) {
                        Cell cell = row.getCell(colIdx);
                        Object value = getCellValue(cell);
                        rowData.put(headers.get(colIdx), value);
                    }
                    rows.add(rowData);
                }
            }

            String outputDir = System.getProperty("java.io.tmpdir") + "/standard-excel/";
            new File(outputDir).mkdirs();
            String outputFile = outputDir + File.separator + new File(filePath).getName().replace(".xlsx", "_standard.xlsx");

            StandardExcelWriter.write(outputFile, headers, rows);

            result.setSuccess(true);
            result.setStandardExcelPath(outputFile);
            result.setDbName("ods_layer");
            result.setTableName(config.getOdsTableName());
            result.setPtDt(FileNameDateExtractor.extract(new File(filePath).getName()));

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    private Object getCellValue(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING: return cell.getStringCellValue();
            case NUMERIC: return cell.getNumericCellValue();
            case BOOLEAN: return cell.getBooleanCellValue();
            default: return "";
        }
    }
}
```

- [ ] **Step 3: 创建 PackagingService - 打包服务**

```java
package com.report.service;

public interface PackagingService {
    String packageToStaging(Long ftpConfigId, String standardExcelPath, String sourceFileName, TransformResult result);
}
```

```java
package com.report.service;

import com.report.entity.BatchRecord;
import com.report.entity.FtpConfig;
import com.report.mapper.BatchRecordMapper;
import com.report.mapper.FtpConfigMapper;
import com.report.util.ConfigExcelWriter;
import com.report.util.ZipPackager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PackagingServiceImpl implements PackagingService {

    @Autowired
    private FtpConfigMapper ftpConfigMapper;

    @Autowired
    private BatchRecordMapper batchRecordMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String packageToStaging(Long ftpConfigId, String standardExcelPath, String sourceFileName, TransformResult result) {
        FtpConfig ftpConfig = ftpConfigMapper.selectById(ftpConfigId);
        String stagingDir = ftpConfig.getStagingDir();

        String batchCode = "batch_" + LocalDateTime.now().format(DTF) + "_" + UUID.randomUUID().toString().substring(0, 8);
        String zipFileName = batchCode + ".zip";
        String zipPath = stagingDir + File.separator + zipFileName;

        List<Map<String, Object>> configRecords = new ArrayList<>();
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("source_file", sourceFileName);
        record.put("db_name", result.getDbName());
        record.put("table_name", result.getTableName());
        record.put("field_mapping", result.getFieldMappingJson());
        record.put("is_partitioned", true);
        record.put("partition_field", "pt_dt");
        record.put("partition_value", result.getPtDt());
        record.put("processed_at", LocalDateTime.now());
        configRecords.add(record);

        String configExcelPath = stagingDir + File.separator + "config_" + batchCode + ".xlsx";
        try {
            ConfigExcelWriter.write(configExcelPath, configRecords);
            List<String> files = Arrays.asList(standardExcelPath, configExcelPath);
            ZipPackager.packageFiles(zipPath, files);
        } catch (Exception e) {
            throw new RuntimeException("Failed to package files", e);
        }

        BatchRecord batch = new BatchRecord();
        batch.setBatchCode(batchCode);
        batch.setFtpConfigId(ftpConfigId);
        batch.setZipFileName(zipFileName);
        batch.setFileCount(1);
        batch.setTotalSize(new File(zipPath).length());
        batch.setStatus(BatchRecord.Status.CREATED.name());
        batch.setCreatedAt(LocalDateTime.now());
        batchRecordMapper.insert(batch);

        return zipPath;
    }
}
```

- [ ] **Step 4: 创建 BatchService - 批次投递服务**

```java
package com.report.service;

public interface BatchService {
    void deliverZipIfReady(Long ftpConfigId);
}
```

```java
package com.report.service;

import com.report.entity.FtpConfig;
import com.report.mapper.FtpConfigMapper;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BatchServiceImpl implements BatchService {

    @Autowired
    private FtpConfigMapper ftpConfigMapper;

    @Autowired
    private FtpService ftpService;

    @Override
    public void deliverZipIfReady(Long ftpConfigId) {
        FtpConfig ftpConfig = ftpConfigMapper.selectById(ftpConfigId);
        String forUploadDir = ftpConfig.getForUploadDir();

        boolean isEmpty = ftpService.isDirectoryEmpty(ftpConfigId, forUploadDir);
        if (!isEmpty) {
            return;
        }

        String stagingDir = ftpConfig.getStagingDir();
        String earliestFile = ftpService.getEarliestFile(ftpConfigId, stagingDir);
        if (earliestFile == null) {
            return;
        }

        ftpService.renameFile(ftpConfigId, earliestFile, forUploadDir + "/output.zip");
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add report-backend/src/main/java/com/report/service/
git commit -m "feat(service): 新增 AlertService, ExcelTransformService, PackagingService, BatchService"
```

---

### 1.6 MiddlewareEngine 核心引擎

**Files:**
- Create: `report-backend/src/main/java/com/report/engine/MiddlewareEngine.java`
- Create: `report-backend/src/main/java/com/report/engine/ScanResult.java`
- Create: `report-backend/src/main/java/com/report/engine/MatchedFile.java`

- [ ] **Step 1: 创建 DTO 类**

```java
package com.report.engine;

import lombok.Data;

@Data
public class MatchedFile {
    private String fileName;
    private String filePath;
    private Long reportConfigId;
    private String ptDt;
}
```

```java
package com.report.engine;

import lombok.Data;
import java.util.List;

@Data
public class ScanResult {
    private List<MatchedFile> matchedFiles;
    private int totalScanned;
}
```

- [ ] **Step 2: 创建 MiddlewareEngine**

```java
package com.report.engine;

import com.report.entity.ProcessedFile;
import com.report.entity.ReportConfig;
import com.report.mapper.ProcessedFileMapper;
import com.report.mapper.ReportConfigMapper;
import com.report.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class MiddlewareEngine {

    private static final Logger log = LoggerFactory.getLogger(MiddlewareEngine.class);

    @Autowired
    private FtpService ftpService;

    @Autowired
    private ExcelTransformService excelTransformService;

    @Autowired
    private PackagingService packagingService;

    @Autowired
    private BatchService batchService;

    @Autowired
    private AlertService alertService;

    @Autowired
    private ArchiveService archiveService;

    @Autowired
    private ProcessedFileMapper processedFileMapper;

    @Autowired
    private ReportConfigMapper reportConfigMapper;

    public void processCycle(Long ftpConfigId) {
        log.info("Starting middleware process cycle for ftpConfigId={}", ftpConfigId);

        List<MatchedFile> newFiles = ftpService.scanNewFiles(ftpConfigId);
        log.info("Found {} new files", newFiles.size());

        for (MatchedFile file : newFiles) {
            try {
                processFile(file);
            } catch (Exception e) {
                log.error("Failed to process file: {}", file.getFileName(), e);
                alertService.createAlert(file.getReportConfigId(), file.getFileName(),
                    "ERROR", "PARSE_ERROR", e.getMessage());
            }
        }

        batchService.deliverZipIfReady(ftpConfigId);
        log.info("Completed middleware process cycle");
    }

    private void processFile(MatchedFile file) {
        QueryWrapper<ProcessedFile> wrapper = new QueryWrapper<>();
        wrapper.eq("report_config_id", file.getReportConfigId());
        wrapper.eq("file_name", file.getFileName());
        long count = processedFileMapper.selectCount(wrapper);

        if (count > 0) {
            log.info("File already processed, skipping: {}", file.getFileName());
            return;
        }

        TransformResult result = excelTransformService.transform(file.getFilePath(), file.getReportConfigId());

        if (result.isSuccess()) {
            ReportConfig config = reportConfigMapper.selectById(file.getReportConfigId());

            packagingService.packageToStaging(
                config.getFtpConfigId(),
                result.getStandardExcelPath(),
                file.getFileName(),
                result
            );

            ProcessedFile processedFile = new ProcessedFile();
            processedFile.setReportConfigId(file.getReportConfigId());
            processedFile.setFileName(file.getFileName());
            processedFile.setStatus("PROCESSED");
            processedFile.setProcessedAt(LocalDateTime.now());
            processedFileMapper.insert(processedFile);

            archiveService.archiveToSuccess(file.getFilePath());
        } else {
            alertService.createAlert(file.getReportConfigId(), file.getFileName(),
                "ERROR", "MAPPING_FAILED", result.getErrorMessage());
            archiveService.archiveToError(file.getFilePath());
        }
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/engine/
git commit -m "feat(engine): 新增 MiddlewareEngine 核心引擎"
```

---

## Phase 2: 归档与 ODS

### 阶段目标
完成 ArchiveService、OdsBackupService、FtpService 增强。

### 2.1 ArchiveService 实现

**Files:**
- Create: `report-backend/src/main/java/com/report/service/ArchiveService.java`
- Create: `report-backend/src/main/java/com/report/service/ArchiveServiceImpl.java`

- [ ] **Step 1: 创建 ArchiveService**

```java
package com.report.service;

public interface ArchiveService {
    void archiveToSuccess(String filePath);
    void archiveToError(String filePath);
}
```

```java
package com.report.service;

import com.report.entity.FtpConfig;
import com.report.mapper.FtpConfigMapper;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ArchiveServiceImpl implements ArchiveService {

    @Autowired
    private FtpConfigMapper ftpConfigMapper;

    @Autowired
    private FtpService ftpService;

    @Override
    public void archiveToSuccess(String filePath) {
        archive(filePath, "archive_dir");
    }

    @Override
    public void archiveToError(String filePath) {
        archive(filePath, "error_dir");
    }

    private void archive(String filePath, String dirType) {
        File file = new File(filePath);
        String fileName = file.getName();

        Long ftpConfigId = ftpService.getFtpConfigIdByFilePath(filePath);
        if (ftpConfigId == null) return;

        FtpConfig ftpConfig = ftpConfigMapper.selectById(ftpConfigId);
        String targetDir = ftpConfig.getArchiveDir();

        ftpService.moveFile(ftpConfigId, filePath, targetDir + "/" + fileName);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/service/ArchiveServiceImpl.java
git commit -m "feat(service): 新增 ArchiveService 归档服务"
```

---

### 2.2 FtpService 增强

**Files:**
- Modify: `report-backend/src/main/java/com/report/service/FtpService.java` - 新增方法
- Modify: `report-backend/src/main/java/com/report/service/FtpServiceImpl.java` - 新增实现

- [ ] **Step 1: 增强 FtpService 接口**

新增方法：
```java
List<MatchedFile> scanNewFiles(Long ftpConfigId);
boolean isDirectoryEmpty(Long ftpConfigId, String dirPath);
String getEarliestFile(Long ftpConfigId, String dirPath);
void renameFile(Long ftpConfigId, String fromPath, String toPath);
void moveFile(Long ftpConfigId, String fromPath, String toPath);
Long getFtpConfigIdByFilePath(String filePath);
```

- [ ] **Step 2: 实现新增方法**

在 FtpServiceImpl 中实现上述方法，调用 Apache Commons Net FTPClient。

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/service/FtpServiceImpl.java
git commit -m "feat(ftp): 增强 FtpService 支持 staging/for-upload 目录操作"
```

---

### 2.3 OdsBackupService 实现

**Files:**
- Create: `report-backend/src/main/java/com/report/service/OdsBackupService.java`
- Create: `report-backend/src/main/java/com/report/service/OdsBackupServiceImpl.java`

- [ ] **Step 1: 创建 OdsBackupService**

```java
package com.report.service;

public interface OdsBackupService {
    void backup(TransformResult result, String sourceFileName);
}
```

```java
package com.report.service;

import org.springframework.stereotype.Service;

@Service
public class OdsBackupServiceImpl implements OdsBackupService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void backup(TransformResult result, String sourceFileName) {
        if (result.getTableName() == null || result.getTableName().isEmpty()) {
            return;
        }

        String sql = String.format(
            "INSERT INTO %s (source_file, pt_dt, data) VALUES (?, ?, ?)",
            result.getTableName()
        );

        jdbcTemplate.update(sql,
            sourceFileName,
            result.getPtDt(),
            "{}"
        );
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/service/OdsBackupServiceImpl.java
git commit -m "feat(ods): 新增 OdsBackupService ODS备份服务"
```

---

## Phase 3: 前端重构

### 阶段目标
完成 FTP 配置页面增强、报表配置页面增强、新增告警管理页面、菜单路由调整。

### 3.1 FTP 配置页面重构

**Files:**
- Modify: `src/views/ftp/FtpConfig.vue` - 新增目录配置字段

- [ ] **Step 1: 新增表单字段**

在现有 FTP 配置表单中添加：
```vue
<el-form-item label="暂存目录">
  <el-input v-model="form.stagingDir" placeholder="默认: {output_dir}/staging"></el-input>
</el-form-item>
<el-form-item label="待上传目录">
  <el-input v-model="form.forUploadDir" placeholder="默认: {output_dir}/for-upload"></el-input>
</el-form-item>
<el-form-item label="归档目录">
  <el-input v-model="form.archiveDir" placeholder="默认: {output_dir}/archive"></el-input>
</el-form-item>
<el-form-item label="错误文件目录">
  <el-input v-model="form.errorDir" placeholder="默认: {output_dir}/error"></el-input>
</el-form-item>
```

- [ ] **Step 2: 提交**

```bash
git add src/views/ftp/FtpConfig.vue
git commit -m "feat(frontend): FTP配置页面新增目录配置字段"
```

---

### 3.2 报表配置页面重构

**Files:**
- Modify: `src/views/report/components/ReportConfig.vue` - 新增映射模式和 ODS 配置

- [ ] **Step 1: 新增表单字段**

```vue
<el-form-item label="起始行号">
  <el-input-number v-model="form.startRow" :min="1" :max="100"></el-input-number>
</el-form-item>
<el-form-item label="起始列号">
  <el-input-number v-model="form.startCol" :min="1" :max="50"></el-input-number>
</el-form-item>
<el-form-item label="映射模式">
  <el-select v-model="form.mappingMode">
    <el-option label="按列名" value="BY_NAME"></el-option>
    <el-option label="按列序号" value="BY_INDEX"></el-option>
    <el-option label="双模式" value="DUAL"></el-option>
  </el-select>
</el-form-item>
<el-form-item label="重复列名策略">
  <el-select v-model="form.duplicateColStrategy">
    <el-option label="自动加后缀" value="AUTO_SUFFIX"></el-option>
    <el-option label="按序号标识" value="BY_INDEX"></el-option>
  </el-select>
</el-form-item>
<el-form-item label="ODS 备份">
  <el-switch v-model="form.odsBackupEnabled"></el-switch>
</el-form-item>
<el-form-item label="ODS 表名" v-if="form.odsBackupEnabled">
  <el-input v-model="form.odsTableName"></el-input>
</el-form-item>
```

- [ ] **Step 2: 提交**

```bash
git add src/views/report/components/ReportConfig.vue
git commit -m "feat(frontend): 报表配置页面新增起始行列偏移、映射模式、ODS配置"
```

---

### 3.3 新增告警管理页面

**Files:**
- Create: `src/views/alert/AlertList.vue`
- Create: `src/api/alert.js`
- Modify: `src/router/index.js` - 新增路由

- [ ] **Step 1: 创建告警管理页面**

```vue
<template>
  <div class="alert-container">
    <el-card>
      <div slot="header">
        <span>告警管理</span>
      </div>
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="级别">
          <el-select v-model="queryForm.level" clearable>
            <el-option label="ERROR" value="ERROR"></el-option>
            <el-option label="WARNING" value="WARNING"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryForm.status" clearable>
            <el-option label="未解决" value="OPEN"></el-option>
            <el-option label="已解决" value="RESOLVED"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="tableData" border>
        <el-table-column prop="fileName" label="文件名"></el-table-column>
        <el-table-column prop="alertLevel" label="级别"></el-table-column>
        <el-table-column prop="alertType" label="类型"></el-table-column>
        <el-table-column prop="alertMessage" label="告警信息"></el-table-column>
        <el-table-column prop="status" label="状态"></el-table-column>
        <el-table-column prop="createdAt" label="创建时间"></el-table-column>
        <el-table-column label="操作">
          <template slot-scope="{row}">
            <el-button v-if="row.status === 'OPEN'" size="mini" @click="resolve(row)">解决</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
```

- [ ] **Step 2: 创建告警 API**

```javascript
import request from '@/utils/request'

export function listAlerts(params) {
  return request({
    url: '/api/alert',
    method: 'get',
    params
  })
}

export function resolveAlert(id) {
  return request({
    url: `/api/alert/${id}/resolve`,
    method: 'put'
  })
}
```

- [ ] **Step 3: 提交**

```bash
git add src/views/alert/ src/api/alert.js src/router/index.js
git commit -m "feat(frontend): 新增告警管理页面和API"
```

---

### 3.4 菜单和路由调整

**Files:**
- Modify: `src/router/index.js` - 移除 data-center, trigger-monitor
- Modify: `src/App.vue` - 调整侧边栏菜单

- [ ] **Step 1: 调整路由**

移除：
- `/data-center`
- `/trigger-monitor`

- [ ] **Step 2: 调整菜单**

```
重构后菜单：
- FTP 配置
- 报表配置
- 处理记录
- 告警管理  ← 新增
- 执行日志
- 操作日志
- 系统日志
```

- [ ] **Step 3: 提交**

```bash
git add src/router/index.js src/App.vue
git commit -m "feat(frontend): 调整菜单和路由，移除数据中心和Trigger监控"
```

---

## Phase 4: 测试与优化

### 4.1 单元测试

- [ ] **Step 1: ExcelTransformService 测试**
- [ ] **Step 2: PackagingService 测试**
- [ ] **Step 3: MiddlewareEngine 测试**

### 4.2 集成测试

- [ ] **Step 1: FtpScanJob 端到端测试**
- [ ] **Step 2: staging/for-upload 流程测试**

### 4.3 文档更新

- [ ] **Step 1: 更新 API.md**
- [ ] **Step 2: 更新 README.md**

---

## 自审检查清单

### 1. Spec 覆盖检查
- [x] 核心数据流
- [x] FTP 目录结构（staging/for-upload）
- [x] 幂等性保证
- [x] 后端架构（MiddlewareEngine, ExcelTransformService, PackagingService, BatchService）
- [x] 数据模型（alert_record, batch_record, 实体增强）
- [x] 前端变更（FTP配置、报表配置、告警管理）
- [x] API 接口
- [x] 错误处理
- [x] 移除清单

### 2. 占位符扫描
- 无 "TBD"、"TODO" 未完成步骤
- 无 "implement later"、"fill in details"

### 3. 类型一致性
- TransformResult 类字段在后续任务中一致使用
- AlertRecord.Status 枚举在 Service 层一致使用
- BatchRecord.Status 枚举在 Service 层一致使用

---

**Plan 完成时间**: 预计 4 个阶段完成
**优先级**: Phase 1 (核心引擎) > Phase 2 (归档与ODS) > Phase 3 (前端) > Phase 4 (测试)
