package com.report.engine;

import com.report.entity.FtpConfig;
import com.report.entity.ProcessedFile;
import com.report.entity.ReportConfig;
import com.report.service.*;
import com.report.util.FileNameDateExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

    public void processFile(MatchedFile file, ReportConfig config) {
        String fileName = file.getFileName();
        Long taskId = file.getTaskId();

        if (taskId != null) {
            logService.logInfo(taskId, "开始处理文件: " + fileName);
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
        TransformResult result = excelTransformService.transform(file.getLocalFile().getAbsolutePath(), config.getId());
        result.setLoadMode(config.getLoadMode() != null ? config.getLoadMode() : "partitioned-append");

        if (result.isSuccess()) {
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
                result
            );

            processedFileService.markAsProcessed(config.getId(), fileName, file.getLocalFile().length(), null);
            archiveService.archiveToSuccess(file.getLocalFile(), config);

            log.info("File processed successfully: {}", fileName);
            if (taskId != null) {
                logService.logInfo(taskId, "文件处理成功: " + fileName);
            }
        } else {
            alertService.createAlert(config.getId(), fileName, "ERROR", "MAPPING_FAILED", result.getErrorMessage());
            archiveService.archiveToError(file.getLocalFile(), config);
            log.error("File processing failed: {}, reason: {}", fileName, result.getErrorMessage());
            if (taskId != null) {
                logService.logError(taskId, "文件处理失败: " + result.getErrorMessage());
            }
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
