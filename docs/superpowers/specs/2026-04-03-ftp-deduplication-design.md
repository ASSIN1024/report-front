# FTP文件去重处理机制设计文档

> **文档版本**: V1.0
> **创建日期**: 2026-04-03
> **作者**: AI Assistant
> **状态**: 待评审

---

## 1. 概述

### 1.1 背景问题

当前FTP扫描机制存在严重缺陷：

| 方法 | 当前实现 | 问题 |
|------|----------|------|
| `isFileProcessed()` | `return false;` | 永远返回false，文件永远被当作新文件 |
| `markFileAsProcessed()` | 空实现 | 没有任何标记逻辑 |

**实际表现**：同一文件被重复处理多次，导致数据重复插入。

### 1.2 解决方案

采用**数据库记录已处理文件**方案，通过 `(report_config_id, file_name)` 联合判断唯一性。

### 1.3 业务场景

- 同一报表每天上传新文件（文件名包含日期版本）
- 示例：`osd_nanping_20260401.xlsx` vs `osd_nanping_20260402.xlsx`
- 文件名相同 → 视为重复文件，跳过处理

---

## 2. 数据库设计

### 2.1 表结构

```sql
CREATE TABLE processed_file (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    report_config_id    BIGINT NOT NULL COMMENT '报表配置ID',
    file_name           VARCHAR(200) NOT NULL COMMENT '文件名（不含路径）',
    file_size           BIGINT COMMENT '文件大小（字节）',
    pt_dt               DATE COMMENT '数据分区日期',
    status              VARCHAR(20) DEFAULT 'PROCESSED' COMMENT '处理状态',
    task_id             BIGINT COMMENT '关联任务ID',
    error_message       TEXT COMMENT '错误信息',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_report_file (report_config_id, file_name),
    INDEX idx_pt_dt (pt_dt),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已处理文件记录表';
```

### 2.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| report_config_id | BIGINT | 关联的报表配置ID |
| file_name | VARCHAR(200) | 文件名（不含路径） |
| file_size | BIGINT | 文件大小，用于辅助校验 |
| pt_dt | DATE | 数据分区日期 |
| status | VARCHAR(20) | PROCESSED=成功处理, FAILED=处理失败 |
| task_id | BIGINT | 关联的任务ID，便于追溯 |
| error_message | TEXT | 错误信息 |

### 2.3 索引设计

- `idx_report_file`: 联合索引，用于快速查询 `(report_config_id, file_name)`
- `idx_pt_dt`: 分区日期索引
- `idx_status`: 状态索引，用于查询失败记录

---

## 3. 去重判断逻辑

### 3.1 唯一性判断

```sql
-- 判断文件是否已处理
SELECT COUNT(*) FROM processed_file
WHERE report_config_id = ?
  AND file_name = ?
  AND status = 'PROCESSED'
```

### 3.2 判断流程

```
1. 文件名 + 报表配置ID 组合查询
         │
         ▼
   查询结果 > 0?
         │
    YES / \ NO
       /   \
   已处理   未处理
   (跳过)   (执行处理)
```

---

## 4. 组件设计

### 4.1 实体类

**ProcessedFile.java**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("processed_file")
public class ProcessedFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long reportConfigId;

    private String fileName;

    private Long fileSize;

    private Date ptDt;

    private String status;

    private Long taskId;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
```

### 4.2 枚举类

**ProcessedFileStatus.java**

```java
package com.report.common.constant;

public enum ProcessedFileStatus {
    PROCESSED("PROCESSED", "已处理"),
    FAILED("FAILED", "处理失败"),
    SKIPPED("SKIPPED", "已跳过");

    private final String code;
    private final String desc;

    ProcessedFileStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
```

### 4.3 Service接口

**ProcessedFileService.java**

```java
package com.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.ProcessedFile;

public interface ProcessedFileService extends IService<ProcessedFile> {

    boolean isFileProcessed(Long reportConfigId, String fileName);

    void markAsProcessed(Long reportConfigId, String fileName, Long fileSize, Long taskId);

    void markAsFailed(Long reportConfigId, String fileName, Long taskId, String errorMessage);
}
```

### 4.4 Service实现

**ProcessedFileServiceImpl.java**

```java
package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.report.common.constant.ProcessedFileStatus;
import com.report.entity.ProcessedFile;
import com.report.mapper.ProcessedFileMapper;
import com.report.service.ProcessedFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class ProcessedFileServiceImpl
    extends ServiceImpl<ProcessedFileMapper, ProcessedFile>
    implements ProcessedFileService {

    @Override
    public boolean isFileProcessed(Long reportConfigId, String fileName) {
        LambdaQueryWrapper<ProcessedFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProcessedFile::getReportConfigId, reportConfigId)
               .eq(ProcessedFile::getFileName, fileName)
               .eq(ProcessedFile::getStatus, ProcessedFileStatus.PROCESSED.getCode());

        long count = this.count(wrapper);
        log.debug("检查文件是否已处理: reportConfigId={}, fileName={}, count={}",
                  reportConfigId, fileName, count);
        return count > 0;
    }

    @Override
    public void markAsProcessed(Long reportConfigId, String fileName,
                               Long fileSize, Long taskId) {
        ProcessedFile record = new ProcessedFile();
        record.setReportConfigId(reportConfigId);
        record.setFileName(fileName);
        record.setFileSize(fileSize);
        record.setStatus(ProcessedFileStatus.PROCESSED.getCode());
        record.setTaskId(taskId);
        record.setPtDt(new Date());

        this.save(record);
        log.info("文件已标记为已处理: reportConfigId={}, fileName={}",
                 reportConfigId, fileName);
    }

    @Override
    public void markAsFailed(Long reportConfigId, String fileName,
                            Long taskId, String errorMessage) {
        ProcessedFile record = new ProcessedFile();
        record.setReportConfigId(reportConfigId);
        record.setFileName(fileName);
        record.setStatus(ProcessedFileStatus.FAILED.getCode());
        record.setTaskId(taskId);
        record.setErrorMessage(errorMessage);
        record.setPtDt(new Date());

        this.save(record);
        log.warn("文件已标记为处理失败: reportConfigId={}, fileName={}, error={}",
                 reportConfigId, fileName, errorMessage);
    }
}
```

---

## 5. FtpScanJob改造

### 5.1 注入依赖

```java
@Autowired
private ProcessedFileService processedFileService;
```

### 5.2 改造后的matchAndProcessFiles方法

```java
private void matchAndProcessFiles(FTPClient ftpClient, FtpConfig ftpConfig,
                                  ReportConfig reportConfig, List<String> files) {
    String pattern = reportConfig.getFilePattern();
    String fileRegex = pattern.replace("*", ".*").replace("?", ".");

    for (String filePath : files) {
        String fileName = new File(filePath).getName();

        // 1. 匹配文件模式
        if (!fileName.matches(fileRegex)) {
            continue;
        }

        // 2. 【核心去重逻辑】检查是否已处理
        if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
            log.info("文件已处理过，跳过: {}", fileName);
            continue;
        }

        log.info("检测到新文件: {}, 报表配置: {}",
                 fileName, reportConfig.getReportName());

        // 3. 创建任务
        TaskExecution task = taskService.createTask(
            "FTP_SCAN",
            "FTP扫描-" + reportConfig.getReportName(),
            reportConfig.getId(),
            fileName,
            filePath
        );

        // 4. 处理文件
        try {
            File localFile = downloadToLocalFile(ftpClient, filePath, fileName);
            if (localFile != null && localFile.exists()) {
                dataProcessJob.processFile(task.getId(), reportConfig, localFile);

                // 5. 【核心】处理成功，标记为已处理
                processedFileService.markAsProcessed(
                    reportConfig.getId(),
                    fileName,
                    localFile.length(),
                    task.getId()
                );
                localFile.delete();
            }
        } catch (Exception e) {
            log.error("文件处理失败: {}", fileName, e);
            taskService.finishTask(task.getId(), "FAILED", e.getMessage());

            // 6. 记录失败状态（可选，防止无限重试）
            processedFileService.markAsFailed(
                reportConfig.getId(),
                fileName,
                task.getId(),
                e.getMessage()
            );
        }
    }
}
```

### 5.3 删除空实现方法

```java
// 删除以下空实现方法：
// private boolean isFileProcessed(Long reportConfigId, String fileName)
// private void markFileAsProcessed(Long reportConfigId, String fileName)
```

---

## 6. 手动触发场景

### 6.1 TaskController.trigger改造

在手动触发时，同样需要调用去重逻辑：

```java
@PostMapping("/trigger")
public Result<Map<String, Object>> trigger(@RequestBody Map<String, Object> params) {
    // ... 验证代码 ...

    // 检查文件是否已处理
    if (processedFileService.isFileProcessed(reportConfigId, fileName)) {
        return Result.fail("文件已处理过，请勿重复提交");
    }

    // 创建任务并处理
    TaskExecution task = taskService.createTask(...);

    try {
        dataProcessJob.processFile(task.getId(), reportConfig, localFile);

        // 标记为已处理
        processedFileService.markAsProcessed(
            reportConfigId, fileName, localFile.length(), task.getId()
        );

        return Result.success(result);
    } catch (Exception e) {
        taskService.finishTask(task.getId(), "FAILED", e.getMessage());

        // 记录失败
        processedFileService.markAsFailed(
            reportConfigId, fileName, task.getId(), e.getMessage()
        );

        return Result.fail("处理失败: " + e.getMessage());
    }
}
```

---

## 7. 异常处理机制

| 异常场景 | 处理策略 | 后果 |
|----------|----------|------|
| **数据库查询失败** | 降级处理，跳过去重检查 | 可能重复处理，但不影响主流程 |
| **数据库写入失败** | 打印错误日志 | 文件不会被标记，下次可能重复处理 |
| **文件处理失败** | 标记状态为FAILED | 下次扫描仍会处理（可选设计） |
| **网络超时** | 重试3次后失败 | 标记失败状态 |

### 7.1 降级策略

```java
public boolean isFileProcessed(Long reportConfigId, String fileName) {
    try {
        // 正常逻辑
        return count > 0;
    } catch (Exception e) {
        log.error("检查文件处理状态失败，降级处理: {}", e.getMessage());
        return false; // 降级：视为未处理，允许继续
    }
}
```

---

## 8. 前端功能扩展

### 8.1 已处理文件查询

新增API：

```java
@GetMapping("/processed-file/page")
public Result<Page<ProcessedFile>> pageProcessedFiles(
    @RequestParam Long reportConfigId,
    @RequestParam(defaultValue = "1") Integer pageNum,
    @RequestParam(defaultValue = "10") Integer pageSize)
```

### 8.2 手动清除处理记录（用于重新处理）

```java
@DeleteMapping("/processed-file/{reportConfigId}/{fileName}")
public Result<Void> deleteProcessedRecord(
    @PathVariable Long reportConfigId,
    @PathVariable String fileName)
```

---

## 9. 测试策略

### 9.1 单元测试

```java
@Test
public void testIsFileProcessed() {
    // 1. 文件未处理 → 返回false
    assertFalse(processedFileService.isFileProcessed(1L, "test.xlsx"));

    // 2. 标记为已处理
    processedFileService.markAsProcessed(1L, "test.xlsx", 1024L, 1L);

    // 3. 文件已处理 → 返回true
    assertTrue(processedFileService.isFileProcessed(1L, "test.xlsx"));
}

@Test
public void testDifferentReportConfig() {
    // 同一文件名，不同报表配置 → 都应返回false
    processedFileService.markAsProcessed(1L, "test.xlsx", 1024L, 1L);
    assertTrue(processedFileService.isFileProcessed(1L, "test.xlsx"));
    assertFalse(processedFileService.isFileProcessed(2L, "test.xlsx"));
}
```

### 9.2 集成测试

1. 上传新文件 → 验证处理成功
2. 再次上传同名文件 → 验证跳过
3. 手动清除记录 → 再次上传 → 验证处理

---

## 10. 部署注意事项

### 10.1 数据库迁移

```sql
-- 新建表
CREATE TABLE processed_file (...);

-- 初始化脚本路径
-- report-backend/src/main/resources/migration/V1.0__create_processed_file.sql
```

### 10.2 回滚方案

```sql
-- 如需回滚，删除表
DROP TABLE IF EXISTS processed_file;
```

---

## 11. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-04-03 | V1.0 | 初始版本创建 | AI Assistant |

---

## 12. 附录

### 12.1 术语表

| 术语 | 说明 |
|------|------|
| 去重 | 防止同一文件被重复处理 |
| 降级处理 | 异常时采用简化的后备处理逻辑 |
| pt_dt | 分区日期字段，用于数据版本管理 |

### 12.2 参考资料

- FtpScanJob.java - 原扫描任务
- DataProcessJob.java - 数据处理任务
- TaskExecution.java - 任务执行实体
