package com.report.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.dto.ReportConfigDTO;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import com.report.service.LogService;
import com.report.service.ReportConfigService;
import com.report.service.TaskService;
import com.report.util.ColumnMappingValidator;
import com.report.util.FileNameDateExtractor;
import lombok.extern.slf4j.Slf4j;
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

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired(required = false)
    private EmbeddedFtpServer embeddedFtpServer;

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
        config.setScanPath(dto.getScanPath());
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
        config.setScanPath(dto.getScanPath());
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

        if (embeddedFtpServer == null || !embeddedFtpServer.isRunning()) {
            return Result.fail("内置FTP服务未运行");
        }

        BuiltInFtpConfig ftpConfig = builtInFtpConfigService.getConfig();
        if (ftpConfig == null) {
            return Result.fail("FTP配置不存在");
        }

        TaskExecution task = taskService.createTask("SCAN", "立即扫描-" + config.getReportName(), id, null, null);
        Long taskId = task.getId();
        logService.logInfo(taskId, "开始扫描内置FTP目录...");

        File tempFile = null;
        try {
            String scanPath = config.getScanPath();
            if (scanPath == null || scanPath.isEmpty()) {
                scanPath = "/upload";
            }

            File ftpRoot = new File(ftpConfig.getRootDirectory());
            File scanDir = new File(ftpRoot, scanPath);

            if (!scanDir.exists() || !scanDir.isDirectory()) {
                taskService.finishTask(taskId, "FAILED", "扫描目录不存在: " + scanDir.getAbsolutePath());
                logService.logError(taskId, "扫描目录不存在");
                return Result.fail("扫描目录不存在");
            }

            logService.logInfo(taskId, "扫描目录: " + scanDir.getAbsolutePath());

            String pattern = config.getFilePattern();
            String fileRegex = pattern.replace("*", ".*").replace("?", ".");

            File[] files = scanDir.listFiles((dir, name) -> name.matches(fileRegex));
            if (files == null || files.length == 0) {
                taskService.finishTask(taskId, "FAILED", "目录中没有找到匹配的文件");
                logService.logError(taskId, "目录中没有找到匹配的文件");
                return Result.fail("目录中没有找到匹配的文件");
            }

            File targetFile = files[0];
            String matchedFileName = targetFile.getName();
            logService.logInfo(taskId, "找到文件: " + matchedFileName);

            tempFile = File.createTempFile("scan_", "_" + matchedFileName);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                java.nio.file.Files.copy(targetFile.toPath(), fos);
            }

            MatchedFile matchedFile = new MatchedFile();
            matchedFile.setFileName(matchedFileName);
            matchedFile.setFilePath(targetFile.getAbsolutePath());
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
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
