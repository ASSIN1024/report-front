package com.report.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.report.common.result.Result;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.entity.dto.ReportConfigDTO;
import com.report.job.DataProcessJob;
import com.report.job.FtpScanJob;
import com.report.service.ReportConfigService;
import com.report.util.ColumnMappingValidator;
import com.report.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
    private DataProcessJob dataProcessJob;

    @Autowired
    private FtpScanJob ftpScanJob;

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

            TaskExecution task = taskService.createTask(
                    "FILE_PROCESS",
                    "文件上传-" + reportConfig.getReportName(),
                    reportConfigId,
                    file.getOriginalFilename(),
                    tempFile.getAbsolutePath()
            );

            dataProcessJob.processFile(task.getId(), reportConfig, tempFile);

            return Result.success(task.getId());
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.fail("文件上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/scan")
    public Result<Long> triggerScan(@PathVariable Long id) {
        ReportConfig config = reportConfigService.getById(id);
        if (config == null) {
            return Result.fail("报表配置不存在");
        }
        if (config.getStatus() != 1) {
            return Result.fail("报表配置未启用");
        }

        TaskExecution task = taskService.createTask(
            "FTP_SCAN",
            "FTP扫描-" + config.getReportName(),
            id,
            null,
            null
        );

        ftpScanJob.scanReportConfig(id, task.getId());

        return Result.success(task.getId());
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
}
