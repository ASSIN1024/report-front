package com.report.engine;

import com.report.entity.ProcessedFile;
import com.report.entity.ReportConfig;
import com.report.service.*;
import com.report.util.FileNameDateExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class MiddlewareEngine {

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
    private OdsBackupService odsBackupService;

    @Autowired
    private ProcessedFileService processedFileService;

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private LogService logService;

    @Autowired
    private TaskService taskService;

    public void processFile(MatchedFile file, ReportConfig config) {
        Long taskId = file.getTaskId();
        processFile(file, config, taskId);
    }

    public void processFile(MatchedFile file, ReportConfig config, Long taskId) {
        String fileName = file.getFileName();

        if (taskId != null) {
            logService.logInfo(taskId, "开始处理文件: " + fileName);
            taskService.updateTaskStatus(taskId, "RUNNING");
            taskService.updateTaskProgress(taskId, 0, 0, 0);
        }

        log.info("MiddlewareEngine processing: {}", fileName);

        if (processedFileService.isFileProcessed(config.getId(), fileName)) {
            log.info("File already processed, skipping: {}", fileName);
            if (taskId != null) {
                logService.logWarn(taskId, "文件已处理过，跳过: " + fileName);
            }
            return;
        }

        if (taskId != null) {
            logService.logInfo(taskId, "转换Excel文件: " + fileName);
        }
        TransformResult result = excelTransformService.transform(file.getLocalFile().getAbsolutePath(), config.getId(), fileName);
        result.setLoadMode(config.getLoadMode() != null ? config.getLoadMode() : "partitioned-append");

        if (result.isSuccess()) {
            int totalRows = result.getRows() != null ? result.getRows().size() : 0;

            if (config.getOdsBackupEnabled() != null && config.getOdsBackupEnabled() == 1) {
                try {
                    if (taskId != null) {
                        logService.logInfo(taskId, "执行ODS备份...");
                    }
                    odsBackupService.backup(result, fileName);
                } catch (Exception e) {
                    log.warn("ODS backup failed for: {}", fileName, e);
                    if (taskId != null) {
                        logService.logWarn(taskId, "ODS备份失败: " + e.getMessage());
                    }
                    alertService.createAlert(config.getId(), fileName, "WARNING", "PARSE_ERROR", "ODS备份失败: " + e.getMessage());
                }
            }

            if (taskId != null) {
                logService.logInfo(taskId, "移动文件到暂存目录...");
            }
            packagingService.moveToStagingDir(
                result.getStandardExcelPath(),
                fileName,
                result,
                config
            );

            processedFileService.markAsProcessed(config.getId(), fileName, file.getLocalFile().length(), null, result.getStandardExcelPath());
            archiveService.archiveToSuccess(file.getLocalFile(), config);

            if (file.getFilePath() != null) {
                File originalFile = new File(file.getFilePath());
                if (originalFile.exists() && originalFile.delete()) {
                    log.info("Deleted original file from FTP: {}", originalFile.getAbsolutePath());
                }
            }

            if (taskId != null) {
                taskService.updateTaskProgress(taskId, totalRows, totalRows, 0);
                taskService.updateTaskStatus(taskId, "SUCCESS");
                logService.logInfo(taskId, "文件处理成功，共 " + totalRows + " 行");
            }

            log.info("File processed successfully: {}", fileName);
        } else {
            alertService.createAlert(config.getId(), fileName, "ERROR", "MAPPING_FAILED", result.getErrorMessage());
            archiveService.archiveToError(file.getLocalFile(), config);

            if (taskId != null) {
                taskService.updateTaskProgress(taskId, 0, 0, 0);
                taskService.updateTaskStatus(taskId, "FAILED");
                logService.logError(taskId, "文件处理失败: " + result.getErrorMessage());
            }

            log.error("File processing failed: {}, reason: {}", fileName, result.getErrorMessage());
        }
    }

    public void deliverPendingZips(Long ftpConfigId) {
        try {
            batchService.deliverZipIfReady(ftpConfigId);
        } catch (Exception e) {
            log.error("Failed to deliver ZIP for ftpConfigId={}", ftpConfigId, e);
        }
    }
}
