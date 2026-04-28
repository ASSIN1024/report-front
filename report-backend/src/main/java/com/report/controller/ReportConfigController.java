package com.report.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.dto.ReportConfigDTO;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.service.ReportConfigService;
import com.report.service.FtpConfigService;
import com.report.service.LogService;
import com.report.util.ColumnMappingValidator;
import com.report.util.FileNameDateExtractor;
import com.report.service.TaskService;
import com.report.entity.FtpConfig;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report/config")
public class ReportConfigController {

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FtpConfigService ftpConfigService;

    @Autowired
    private MiddlewareEngine middlewareEngine;

    @Autowired
    private LogService logService;

    @GetMapping("/page")
    public Result<Page<ReportConfigDTO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String reportName,
            @RequestParam(required = false) Integer status) {
        return Result.success(reportConfigService.pageList(pageNum, pageSize, reportName, status));
    }

    @GetMapping("/list/enabled")
    public Result<List<ReportConfig>> listEnabled() {
        return Result.success(reportConfigService.listEnabled());
    }

    @GetMapping("/{id}")
    public Result<ReportConfigDTO> getById(@PathVariable Long id) {
        return Result.success(reportConfigService.getDetailById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody ReportConfigDTO dto) {
        ReportConfig config = new ReportConfig();
        config.setReportCode(dto.getReportCode());
        config.setReportName(dto.getReportName());
        config.setFtpConfigId(dto.getFtpConfigId());
        config.setFilePattern(dto.getFilePattern());
        config.setSheetIndex(dto.getSheetIndex());
        config.setHeaderRow(dto.getHeaderRow());
        config.setDataStartRow(dto.getDataStartRow());
        config.setColumnMapping(JSONUtil.toJsonStr(dto.getColumnMappings()));
        config.setOutputTable(dto.getOutputTable());
        config.setOutputMode(dto.getOutputMode());
        config.setStartRow(dto.getStartRow());
        config.setStartCol(dto.getStartCol());
        config.setMappingMode(dto.getMappingMode());
        config.setDuplicateColStrategy(dto.getDuplicateColStrategy());
        config.setOdsBackupEnabled(dto.getOdsBackupEnabled());
        config.setOdsTableName(dto.getOdsTableName());
        config.setStatus(dto.getStatus());
        config.setRemark(dto.getRemark());
        reportConfigService.save(config);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody ReportConfigDTO dto) {
        ReportConfig config = new ReportConfig();
        config.setId(dto.getId());
        config.setReportCode(dto.getReportCode());
        config.setReportName(dto.getReportName());
        config.setFtpConfigId(dto.getFtpConfigId());
        config.setFilePattern(dto.getFilePattern());
        config.setSheetIndex(dto.getSheetIndex());
        config.setHeaderRow(dto.getHeaderRow());
        config.setDataStartRow(dto.getDataStartRow());
        config.setColumnMapping(JSONUtil.toJsonStr(dto.getColumnMappings()));
        config.setOutputTable(dto.getOutputTable());
        config.setOutputMode(dto.getOutputMode());
        config.setStartRow(dto.getStartRow());
        config.setStartCol(dto.getStartCol());
        config.setMappingMode(dto.getMappingMode());
        config.setDuplicateColStrategy(dto.getDuplicateColStrategy());
        config.setOdsBackupEnabled(dto.getOdsBackupEnabled());
        config.setOdsTableName(dto.getOdsTableName());
        config.setStatus(dto.getStatus());
        config.setRemark(dto.getRemark());
        reportConfigService.updateById(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ReportConfig config = reportConfigService.getById(id);
        if (config == null) {
            return Result.fail("报表配置不存在");
        }
        reportConfigService.removeById(id);
        return Result.success();
    }

    @PostMapping("/upload")
    public Result<Long> uploadFile(@RequestParam("file") MultipartFile file,
                                   @RequestParam("reportConfigId") Long reportConfigId) {
        try {
            ReportConfig reportConfig = reportConfigService.getById(reportConfigId);
            if (reportConfig == null) {
                return Result.fail("报表配置不存在");
            }

            File tempFile = File.createTempFile("upload_", file.getOriginalFilename());
            file.transferTo(tempFile);

            MatchedFile matchedFile = new MatchedFile();
            matchedFile.setFileName(file.getOriginalFilename());
            matchedFile.setFilePath(tempFile.getAbsolutePath());
            matchedFile.setReportConfigId(reportConfigId);
            matchedFile.setLocalFile(tempFile);
            LocalDate date = FileNameDateExtractor.extractDate(file.getOriginalFilename());
            matchedFile.setPtDt(date != null ? date.toString() : null);

            middlewareEngine.processFile(matchedFile, reportConfig);

            return Result.success(reportConfigId);
        } catch (Exception e) {
            log.error("文件上传处理失败", e);
            return Result.fail("文件上传处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/column-mapping/validate")
    public Result<Map<String, Object>> validateColumnMapping(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {

        String json = params.get("json");
        List<ColumnMappingValidator.ValidationError> errors =
                ColumnMappingValidator.validate(json);

        Map<String, Object> result = new HashMap<>();
        result.put("valid", errors.isEmpty());
        result.put("errors", errors);
        result.put("count", ColumnMappingValidator.countMappings(json));

        return Result.success(result);
    }

    @PostMapping("/{id}/column-mapping/import")
    public Result<Map<String, Object>> importColumnMapping(
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {

        ReportConfig config = reportConfigService.getById(id);
        if (config == null) {
            return Result.fail("报表配置不存在");
        }

        String json = params.get("json");
        List<ColumnMappingValidator.ValidationError> errors =
                ColumnMappingValidator.validate(json);

        if (!errors.isEmpty()) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("errors", errors);
            result.put("message", "校验失败");
            return Result.success(result);
        }

        config.setColumnMapping(json);
        reportConfigService.updateById(config);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("imported", ColumnMappingValidator.countMappings(json));

        return Result.success(result);
    }

    @PostMapping("/{id}/scan")
    public Result<Map<String, Object>> scan(@PathVariable Long id) {
        ReportConfig config = reportConfigService.getById(id);
        if (config == null) {
            return Result.fail("报表配置不存在");
        }

        FtpConfig ftpConfig = ftpConfigService.getById(config.getFtpConfigId());
        if (ftpConfig == null) {
            return Result.fail("FTP配置不存在");
        }

        TaskExecution task = taskService.createTask("SCAN", "立即扫描-" + config.getReportName(), id, null, null);
        Long taskId = task.getId();
        logService.logInfo(taskId, "开始扫描FTP目录...");

        FTPClient ftpClient = null;
        File tempFile = null;
        try {
            logService.logInfo(taskId, "连接FTP服务器: " + ftpConfig.getHost() + ":" + ftpConfig.getPort());
            ftpClient = FtpUtil.connect(ftpConfig);
            if (ftpClient == null || !ftpClient.isConnected()) {
                taskService.finishTask(taskId, "FAILED", "FTP连接失败");
                logService.logError(taskId, "FTP连接失败");
                return Result.fail("FTP连接失败");
            }

            String scanPath = ftpConfig.getScanPath();
            logService.logInfo(taskId, "扫描目录: " + scanPath);
            List<String> files = FtpUtil.listFiles(ftpClient, scanPath, "*.xlsx");
            if (files == null || files.isEmpty()) {
                taskService.finishTask(taskId, "FAILED", "FTP目录中没有找到Excel文件");
                logService.logError(taskId, "FTP目录中没有找到Excel文件");
                return Result.fail("FTP目录中没有找到Excel文件");
            }

            String pattern = config.getFilePattern();
            logService.logInfo(taskId, "匹配模式: " + pattern);
            String matchedFileName = null;
            for (String f : files) {
                String fileName = new java.io.File(f).getName();
                if (fileName.matches(pattern.replace("*", ".*").replace("?", "."))) {
                    matchedFileName = fileName;
                    break;
                }
            }

            if (matchedFileName == null) {
                taskService.finishTask(taskId, "FAILED", "没有找到匹配 " + pattern + " 的文件");
                logService.logError(taskId, "没有找到匹配 " + pattern + " 的文件");
                return Result.fail("没有找到匹配 " + pattern + " 的文件");
            }

            logService.logInfo(taskId, "找到文件: " + matchedFileName);
            String remotePath = scanPath.endsWith("/") ? scanPath + matchedFileName : scanPath + "/" + matchedFileName;

            byte[] fileData = FtpUtil.downloadFile(ftpClient, remotePath);
            if (fileData == null || fileData.length == 0) {
                taskService.finishTask(taskId, "FAILED", "文件下载失败: " + matchedFileName);
                logService.logError(taskId, "文件下载失败: " + matchedFileName);
                return Result.fail("文件下载失败: " + matchedFileName);
            }

            tempFile = File.createTempFile("scan_", "_" + matchedFileName);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(fileData);
            }

            logService.logInfo(taskId, "开始处理文件...");

            MatchedFile matchedFile = new MatchedFile();
            matchedFile.setFileName(matchedFileName);
            matchedFile.setFilePath(remotePath);
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
            FtpUtil.disconnect(ftpClient);
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
