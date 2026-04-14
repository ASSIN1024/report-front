package com.report.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.FtpConfig;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.dto.TaskQueryDTO;
import com.report.job.DataProcessJob;
import com.report.service.FtpConfigService;
import com.report.service.ProcessedFileService;
import com.report.service.ReportConfigService;
import com.report.service.TaskService;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
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
    private DataProcessJob dataProcessJob;

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
        TaskExecution task = null;
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

            task = taskService.createTask(
                "MANUAL_TRIGGER",
                "手动触发-" + reportConfig.getReportName(),
                reportConfig.getId(),
                fileName,
                tempFile.getAbsolutePath()
            );

            dataProcessJob.processFile(task.getId(), reportConfig, tempFile);
            processedFileService.markAsProcessed(reportConfig.getId(), fileName, tempFile.length(), task.getId());
            tempFile.delete();

            TaskExecution completedTask = taskService.getById(task.getId());
            Map<String, Object> result = new HashMap<>();
            result.put("taskId", task.getId());
            result.put("status", completedTask.getStatus());
            result.put("totalRows", completedTask.getTotalRows());
            result.put("successRows", completedTask.getSuccessRows());
            result.put("failedRows", completedTask.getFailedRows());
            return Result.success(result);

        } catch (Exception e) {
            log.error("手动触发任务失败", e);
            if (task != null) {
                processedFileService.markAsFailed(reportConfig.getId(), fileName, task.getId(), e.getMessage());
            }
            return Result.fail("任务执行失败: " + e.getMessage());
        } finally {
            FtpUtil.disconnect(ftpClient);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
