package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.entity.FtpConfig;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.dto.TaskQueryDTO;
import com.report.service.FtpConfigService;
import com.report.service.ProcessedFileService;
import com.report.service.ReportConfigService;
import com.report.service.TaskService;
import com.report.service.PackagingService;
import com.report.util.FileNameDateExtractor;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
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
    private FtpConfigService ftpConfigService;

    @Autowired
    private ProcessedFileService processedFileService;

    @Autowired
    private MiddlewareEngine middlewareEngine;

    @Autowired
    private PackagingService packagingService;

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

        FtpConfig ftpConfig = ftpConfigService.getById(reportConfig.getFtpConfigId());
        if (ftpConfig == null) {
            return Result.fail("FTP配置不存在");
        }

        FTPClient ftpClient = null;
        File tempFile = null;
        try {
            ftpClient = FtpUtil.connect(ftpConfig);
            if (ftpClient == null || !ftpClient.isConnected()) {
                return Result.fail("FTP连接失败");
            }

            if (fileName == null || fileName.isEmpty()) {
                List<String> files = FtpUtil.listFiles(ftpClient, ftpConfig.getScanPath(), "*.xlsx");
                if (files == null || files.isEmpty()) {
                    return Result.fail("FTP目录中没有找到文件");
                }
                String pattern = reportConfig.getFilePattern();
                for (String f : files) {
                    if (f.matches(pattern.replace("*", ".*").replace("?", "."))) {
                        fileName = f;
                        break;
                    }
                }
                if (fileName == null) {
                    return Result.fail("没有找到匹配 " + reportConfig.getFilePattern() + " 的文件");
                }
            }

            if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
                return Result.fail("文件已处理过: " + fileName);
            }

            String remotePath = ftpConfig.getScanPath() + "/" + fileName;
            if (remotePath.startsWith("//")) {
                remotePath = remotePath.substring(1);
            }
            byte[] fileData = FtpUtil.downloadFile(ftpClient, remotePath);
            if (fileData == null || fileData.length == 0) {
                return Result.fail("文件下载失败: " + fileName);
            }

            tempFile = File.createTempFile("trigger_", "_" + fileName);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileData);
            }

            MatchedFile matchedFile = new MatchedFile();
            matchedFile.setFileName(fileName);
            matchedFile.setFilePath(remotePath);
            matchedFile.setReportConfigId(reportConfigId);
            matchedFile.setLocalFile(tempFile);
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
        } finally {
            FtpUtil.disconnect(ftpClient);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
