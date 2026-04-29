package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.dto.TaskQueryDTO;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import com.report.service.ProcessedFileService;
import com.report.service.ReportConfigService;
import com.report.service.TaskService;
import com.report.service.PackagingService;
import com.report.util.FileNameDateExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private ProcessedFileService processedFileService;

    @Autowired
    private MiddlewareEngine middlewareEngine;

    @Autowired
    private PackagingService packagingService;

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired(required = false)
    private EmbeddedFtpServer embeddedFtpServer;

    @GetMapping("/page")
    public Result<Page<TaskExecution>> page(TaskQueryDTO queryDTO) {
        return Result.success(taskService.pageList(queryDTO));
    }

    @GetMapping("/{id}")
    public Result<TaskExecution> getById(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @PostMapping("/retry/{id}")
    public Result<Void> retry(@PathVariable Long id) {
        TaskExecution task = taskService.getById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        taskService.updateTaskStatus(id, "PENDING");
        return Result.success();
    }

    @PostMapping("/cancel/{id}")
    public Result<Void> cancel(@PathVariable Long id) {
        TaskExecution task = taskService.getById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        taskService.updateTaskStatus(id, "CANCELLED");
        return Result.success();
    }

    @PostMapping("/package")
    public Result<Map<String, Object>> triggerPackage() {
        try {
            log.info("手动触发批量打包...");
            int count = packagingService.collectAndPackageBySizeLimit(200 * 1024 * 1024);
            Map<String, Object> result = new HashMap<>();
            result.put("packageCount", count);
            result.put("message", count > 0 ? "打包完成，共生成 " + count + " 个包" : "没有待打包的文件");
            return Result.success(result);
        } catch (Exception e) {
            log.error("批量打包失败", e);
            return Result.fail("批量打包失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        TaskExecution task = taskService.getById(id);
        if (task == null) {
            return Result.fail("任务记录不存在");
        }
        taskService.removeById(id);
        return Result.success();
    }

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
}
