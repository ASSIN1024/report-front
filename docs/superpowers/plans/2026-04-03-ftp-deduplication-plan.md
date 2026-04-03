# FTP文件去重处理机制实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现FTP文件去重处理机制，防止同一文件被重复扫描和数据重复插入

**Architecture:** 通过数据库记录已处理文件，使用 (report_config_id, file_name) 联合唯一判断，确保同一报表配置下的同一文件名只被处理一次

**Tech Stack:** Spring Boot 2.1.2, MyBatis-Plus, MySQL, Apache POI

---

## 文件结构

```
报告后端/
├── src/main/java/com/report/
│   ├── entity/
│   │   └── ProcessedFile.java          # 新增：已处理文件实体
│   ├── common/constant/
│   │   └── ProcessedFileStatus.java   # 新增：处理状态枚举
│   ├── mapper/
│   │   └── ProcessedFileMapper.java   # 新增：Mapper接口
│   ├── service/
│   │   ├── ProcessedFileService.java  # 新增：服务接口
│   │   └── impl/
│   │       └── ProcessedFileServiceImpl.java  # 新增：服务实现
│   └── job/
│       └── FtpScanJob.java            # 修改：注入去重逻辑
└── src/main/resources/
    └── migration/
        └── V1.0__create_processed_file.sql  # 新增：数据库迁移脚本
```

---

## 实施任务

### Task 1: 创建数据库迁移脚本

**Files:**
- Create: `report-backend/src/main/resources/migration/V1.0__create_processed_file.sql`

- [ ] **Step 1: 创建迁移脚本目录**

```bash
mkdir -p /home/nova/projects/report-front/report-backend/src/main/resources/migration
```

- [ ] **Step 2: 编写建表SQL**

```sql
-- V1.0__create_processed_file.sql
-- 描述: 创建已处理文件记录表，用于FTP文件去重

CREATE TABLE IF NOT EXISTS processed_file (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    report_config_id    BIGINT NOT NULL COMMENT '报表配置ID',
    file_name           VARCHAR(200) NOT NULL COMMENT '文件名（不含路径）',
    file_size           BIGINT COMMENT '文件大小（字节）',
    pt_dt               DATE COMMENT '数据分区日期',
    status              VARCHAR(20) DEFAULT 'PROCESSED' COMMENT '处理状态：PROCESSED-已处理，FAILED-处理失败',
    task_id             BIGINT COMMENT '关联任务ID',
    error_message       TEXT COMMENT '错误信息',
    create_time         DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_report_file (report_config_id, file_name),
    INDEX idx_pt_dt (pt_dt),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='已处理文件记录表';
```

- [ ] **Step 3: 执行SQL创建表**

```bash
mysql -h localhost -u root -proot report_db < /home/nova/projects/report-front/report-backend/src/main/resources/migration/V1.0__create_processed_file.sql
```

- [ ] **Step 4: 验证表创建成功**

```sql
mysql -h localhost -u root -proot report_db -e "DESC processed_file;"
```
预期输出：显示 processed_file 表结构

- [ ] **Step 5: Commit**

```bash
git add report-backend/src/main/resources/migration/V1.0__create_processed_file.sql
git commit -m "feat: add processed_file table for FTP deduplication"
```

---

### Task 2: 创建 ProcessedFileStatus 枚举类

**Files:**
- Create: `report-backend/src/main/java/com/report/common/constant/ProcessedFileStatus.java`

- [ ] **Step 1: 创建枚举类**

```java
package com.report.common.constant;

/**
 * 已处理文件状态枚举
 */
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

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add report-backend/src/main/java/com/report/common/constant/ProcessedFileStatus.java
git commit -m "feat: add ProcessedFileStatus enum"
```

---

### Task 3: 创建 ProcessedFile 实体类

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/ProcessedFile.java`

- [ ] **Step 1: 创建实体类**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

/**
 * 已处理文件记录实体
 */
@Data
@TableName("processed_file")
public class ProcessedFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报表配置ID
     */
    private Long reportConfigId;

    /**
     * 文件名（不含路径）
     */
    private String fileName;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 数据分区日期
     */
    private Date ptDt;

    /**
     * 处理状态：PROCESSED-已处理，FAILED-处理失败
     */
    private String status;

    /**
     * 关联任务ID
     */
    private Long taskId;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
```

- [ ] **Step 2: Commit**

```bash
git add report-backend/src/main/java/com/report/entity/ProcessedFile.java
git commit -m "feat: add ProcessedFile entity"
```

---

### Task 4: 创建 ProcessedFileMapper 接口

**Files:**
- Create: `report-backend/src/main/java/com/report/mapper/ProcessedFileMapper.java`

- [ ] **Step 1: 创建Mapper接口**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.ProcessedFile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 已处理文件Mapper接口
 */
@Mapper
public interface ProcessedFileMapper extends BaseMapper<ProcessedFile> {
}
```

- [ ] **Step 2: Commit**

```bash
git add report-backend/src/main/java/com/report/mapper/ProcessedFileMapper.java
git commit -m "feat: add ProcessedFileMapper interface"
```

---

### Task 5: 创建 ProcessedFileService 接口

**Files:**
- Create: `report-backend/src/main/java/com/report/service/ProcessedFileService.java`

- [ ] **Step 1: 创建Service接口**

```java
package com.report.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.report.entity.ProcessedFile;

/**
 * 已处理文件服务接口
 */
public interface ProcessedFileService extends IService<ProcessedFile> {

    /**
     * 检查文件是否已处理
     * @param reportConfigId 报表配置ID
     * @param fileName 文件名
     * @return true-已处理，false-未处理
     */
    boolean isFileProcessed(Long reportConfigId, String fileName);

    /**
     * 标记文件为已处理
     * @param reportConfigId 报表配置ID
     * @param fileName 文件名
     * @param fileSize 文件大小
     * @param taskId 任务ID
     */
    void markAsProcessed(Long reportConfigId, String fileName, Long fileSize, Long taskId);

    /**
     * 标记文件处理失败
     * @param reportConfigId 报表配置ID
     * @param fileName 文件名
     * @param taskId 任务ID
     * @param errorMessage 错误信息
     */
    void markAsFailed(Long reportConfigId, String fileName, Long taskId, String errorMessage);
}
```

- [ ] **Step 2: Commit**

```bash
git add report-backend/src/main/java/com/report/service/ProcessedFileService.java
git commit -m "feat: add ProcessedFileService interface"
```

---

### Task 6: 创建 ProcessedFileServiceImpl 实现类

**Files:**
- Create: `report-backend/src/main/java/com/report/service/impl/ProcessedFileServiceImpl.java`

- [ ] **Step 1: 创建Service实现类**

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

/**
 * 已处理文件服务实现类
 */
@Slf4j
@Service
public class ProcessedFileServiceImpl
        extends ServiceImpl<ProcessedFileMapper, ProcessedFile>
        implements ProcessedFileService {

    @Override
    public boolean isFileProcessed(Long reportConfigId, String fileName) {
        try {
            LambdaQueryWrapper<ProcessedFile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ProcessedFile::getReportConfigId, reportConfigId)
                   .eq(ProcessedFile::getFileName, fileName)
                   .eq(ProcessedFile::getStatus, ProcessedFileStatus.PROCESSED.getCode());

            long count = this.count(wrapper);
            log.debug("检查文件是否已处理: reportConfigId={}, fileName={}, count={}",
                      reportConfigId, fileName, count);
            return count > 0;
        } catch (Exception e) {
            log.error("检查文件处理状态异常，降级处理: reportConfigId={}, fileName={}, error={}",
                      reportConfigId, fileName, e.getMessage());
            return false;
        }
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

- [ ] **Step 2: Commit**

```bash
git add report-backend/src/main/java/com/report/service/impl/ProcessedFileServiceImpl.java
git commit -m "feat: add ProcessedFileService implementation"
```

---

### Task 7: 修改 FtpScanJob 实现去重逻辑

**Files:**
- Modify: `report-backend/src/main/java/com/report/job/FtpScanJob.java`

- [ ] **Step 1: 添加Autowired注入**

在类顶部添加:
```java
@Autowired
private ProcessedFileService processedFileService;
```

- [ ] **Step 2: 修改matchAndProcessFiles方法**

将原来的方法体替换为:

```java
private void matchAndProcessFiles(FTPClient ftpClient, FtpConfig ftpConfig,
                                  ReportConfig reportConfig, List<String> files) {
    String pattern = reportConfig.getFilePattern();
    String fileRegex = pattern.replace("*", ".*").replace("?", ".");

    for (String filePath : files) {
        String fileName = new File(filePath).getName();

        if (!fileName.matches(fileRegex)) {
            continue;
        }

        if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
            log.info("文件已处理过，跳过: {}", fileName);
            continue;
        }

        log.info("检测到新文件: {}, 报表配置: {}",
                 fileName, reportConfig.getReportName());

        TaskExecution task = taskService.createTask(
            "FTP_SCAN",
            "FTP扫描-" + reportConfig.getReportName(),
            reportConfig.getId(),
            fileName,
            filePath
        );

        try {
            File localFile = downloadToLocalFile(ftpClient, filePath, fileName);
            if (localFile != null && localFile.exists()) {
                dataProcessJob.processFile(task.getId(), reportConfig, localFile);

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

- [ ] **Step 3: 删除空实现的isFileProcessed和markFileAsProcessed方法**

删除以下两个方法:
```java
private boolean isFileProcessed(Long reportConfigId, String fileName) {
    return false;
}

private void markFileAsProcessed(Long reportConfigId, String fileName) {
}
```

- [ ] **Step 4: 重启后端服务验证编译通过**

```bash
cd /home/nova/projects/report-front/report-backend
mvn compile -q
```
预期：无编译错误

- [ ] **Step 5: Commit**

```bash
git add report-backend/src/main/java/com/report/job/FtpScanJob.java
git commit -m "feat: implement FTP file deduplication in FtpScanJob"
```

---

### Task 8: 修改 TaskController.trigger 实现去重逻辑

**Files:**
- Modify: `report-backend/src/main/java/com/report/controller/TaskController.java`

- [ ] **Step 1: 添加Autowired注入**

在类顶部添加:
```java
@Autowired
private ProcessedFileService processedFileService;
```

- [ ] **Step 2: 在trigger方法中添加去重检查**

在 `TaskExecution task = taskService.createTask(...)` 之前添加:

```java
if (processedFileService.isFileProcessed(reportConfigId, fileName)) {
    return Result.fail("文件已处理过，请勿重复提交");
}
```

- [ ] **Step 3: 在成功处理后标记**

在 `dataProcessJob.processFile(task.getId(), reportConfig, localFile);` 之后，`return Result.success(result);` 之前添加:

```java
processedFileService.markAsProcessed(
    reportConfigId, fileName, localFile.length(), task.getId()
);
```

- [ ] **Step 4: 在失败时标记**

在 `taskService.finishTask(task.getId(), "FAILED", e.getMessage());` 之后添加:

```java
processedFileService.markAsFailed(
    reportConfigId, fileName, task.getId(), e.getMessage()
);
```

- [ ] **Step 5: 验证编译**

```bash
cd /home/nova/projects/report-front/report-backend
mvn compile -q
```
预期：无编译错误

- [ ] **Step 6: Commit**

```bash
git add report-backend/src/main/java/com/report/controller/TaskController.java
git commit -m "feat: implement deduplication in manual trigger"
```

---

### Task 9: 集成测试验证

**Files:**
- 无新增文件

- [ ] **Step 1: 重启后端服务**

```bash
pkill -f "mvn.*spring-boot" 2>/dev/null; sleep 2
cd /home/nova/projects/report-front/report-backend
mvn spring-boot:run -Dmaven.test.skip=true 2>&1 &
```

- [ ] **Step 2: 等待服务启动**

```bash
sleep 60
curl -s -o /dev/null -w "%{http_code}" http://localhost:8082/api/ftp/config/page?pageNum=1\&pageSize=1
```
预期：返回200

- [ ] **Step 3: 上传测试文件到FTP**

```bash
curl -T /home/nova/projects/report-front/test-files/e2e_test_demo.xlsx \
  ftp://ftpuser:ftppass@127.0.0.1/e2e_test_verify.xlsx
```
预期：226 Transfer complete

- [ ] **Step 4: 手动触发第一次处理**

```bash
curl -X POST http://localhost:8082/api/task/trigger \
  -H "Content-Type: application/json" \
  -d '{"reportConfigId":"17751435157050000","fileName":"e2e_test_verify.xlsx"}'
```
预期：返回成功，status=SUCCESS

- [ ] **Step 5: 再次触发同一文件（应被拒绝）**

```bash
curl -X POST http://localhost:8082/api/task/trigger \
  -H "Content-Type: application/json" \
  -d '{"reportConfigId":"17751435157050000","fileName":"e2e_test_verify.xlsx"}'
```
预期：返回失败，message包含"已处理过"

- [ ] **Step 6: 检查processed_file表**

```sql
SELECT * FROM processed_file ORDER BY id DESC LIMIT 5;
```
预期：看到e2e_test_verify.xlsx的记录

- [ ] **Step 7: 检查数据库记录数**

```sql
SELECT COUNT(*) FROM t_e2e_test_report_v2;
```
预期：记录数未增加（因为第二次被拒绝）

---

### Task 10: 更新CLAUDE.md文档

**Files:**
- Modify: `/home/nova/projects/report-front/CLAUDE.md`

- [ ] **Step 1: 更新版本信息**

将文档版本从V1.5更新为V1.6，最后更新日期改为2026-04-03

- [ ] **Step 2: 添加新功能到已完成的4.2节**

在"核心处理器"部分添加:
```
- [x] ProcessedFile实体/Mapper/Service - 已处理文件去重服务
- [x] FTP文件去重机制 - 基于数据库记录防止重复处理
```

- [ ] **Step 3: 添加变更记录**

在变更记录表格末尾添加:
```
| 2026-04-03 | V1.6 | [新增] FTP文件去重处理机制 | AI Assistant | FEAT-DEDUP |
| 2026-04-03 | V1.6 | [新增] ProcessedFile实体/Service | AI Assistant | FEAT-DEDUP |
```

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: update CLAUDE.md for FTP deduplication feature"
```

---

## 验证检查清单

- [ ] 数据库表 processed_file 创建成功
- [ ] ProcessedFileStatus 枚举定义正确
- [ ] ProcessedFile 实体类编译通过
- [ ] ProcessedFileMapper 接口编译通过
- [ ] ProcessedFileService 接口定义正确
- [ ] ProcessedFileServiceImpl 实现去重逻辑
- [ ] FtpScanJob 使用去重服务
- [ ] TaskController.trigger 使用去重服务
- [ ] 第一次处理成功
- [ ] 第二次处理被拒绝（返回"已处理过"）
- [ ] processed_file 表有正确记录
- [ ] CLAUDE.md 已更新

---

## 实施顺序

1. Task 1: 创建数据库迁移脚本
2. Task 2: 创建枚举类
3. Task 3: 创建实体类
4. Task 4: 创建Mapper
5. Task 5: 创建Service接口
6. Task 6: 创建Service实现
7. Task 7: 修改FtpScanJob
8. Task 8: 修改TaskController
9. Task 9: 集成测试验证
10. Task 10: 更新文档

---

## 回滚方案

如果实施失败需要回滚：

```sql
-- 删除processed_file表
DROP TABLE IF EXISTS processed_file;

-- 撤销FtpScanJob和TaskController的代码修改
git checkout HEAD~1 -- report-backend/src/main/java/com/report/job/FtpScanJob.java
git checkout HEAD~1 -- report-backend/src/main/java/com/report/controller/TaskController.java
```
