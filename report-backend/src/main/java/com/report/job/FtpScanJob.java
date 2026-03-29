package com.report.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.FtpConfig;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.service.FtpConfigService;
import com.report.service.LogService;
import com.report.service.ReportConfigService;
import com.report.service.TaskService;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Slf4j
@Component
public class FtpScanJob implements Job {

    @Autowired
    private FtpConfigService ftpConfigService;

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

    @Autowired
    private DataProcessJob dataProcessJob;

    private static final String PROCESSED_FILES_TABLE = "processed_files";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("FTP扫描任务开始执行");

        List<FtpConfig> enabledConfigs = ftpConfigService.list(
            new LambdaQueryWrapper<FtpConfig>()
                .eq(FtpConfig::getStatus, 1)
                .eq(FtpConfig::getDeleted, 0)
        );

        if (enabledConfigs.isEmpty()) {
            log.info("没有启用的FTP配置，跳过扫描");
            return;
        }

        for (FtpConfig ftpConfig : enabledConfigs) {
            scanFtpDirectory(ftpConfig);
        }

        log.info("FTP扫描任务执行完成");
    }

    private void scanFtpDirectory(FtpConfig ftpConfig) {
        log.info("开始扫描FTP配置: {}, 主机: {}, 路径: {}",
                ftpConfig.getConfigName(), ftpConfig.getHost(), ftpConfig.getScanPath());

        boolean connected = FtpUtil.testConnection(ftpConfig);
        if (!connected) {
            log.error("FTP连接失败: {}", ftpConfig.getConfigName());
            return;
        }

        try {
            List<String> files = FtpUtil.listFiles(ftpConfig);
            if (files == null || files.isEmpty()) {
                log.info("FTP目录为空: {}", ftpConfig.getScanPath());
                return;
            }

            List<ReportConfig> reportConfigs = reportConfigService.list(
                new LambdaQueryWrapper<ReportConfig>()
                    .eq(ReportConfig::getFtpConfigId, ftpConfig.getId())
                    .eq(ReportConfig::getStatus, 1)
                    .eq(ReportConfig::getDeleted, 0)
            );

            for (ReportConfig reportConfig : reportConfigs) {
                matchAndProcessFiles(ftpConfig, reportConfig, files);
            }

        } finally {
            FtpUtil.disconnect(ftpConfig);
        }
    }

    private void matchAndProcessFiles(FtpConfig ftpConfig, ReportConfig reportConfig, List<String> files) {
        String pattern = reportConfig.getFilePattern();
        String filePattern = pattern.replace("*", ".*").replace("?", ".");

        for (String fileName : files) {
            if (!fileName.matches(filePattern)) {
                continue;
            }

            if (isFileProcessed(reportConfig.getId(), fileName)) {
                log.debug("文件已处理过，跳过: {}", fileName);
                continue;
            }

            String filePath = ftpConfig.getScanPath() + "/" + fileName;
            log.info("检测到新文件: {}, 报表配置: {}", fileName, reportConfig.getReportName());

            TaskExecution task = taskService.createTask(
                "FTP_SCAN",
                "FTP扫描-" + reportConfig.getReportName(),
                reportConfig.getId(),
                fileName,
                filePath
            );

            try {
                File localFile = FtpUtil.downloadFile(ftpConfig, fileName);
                if (localFile != null && isFileComplete(localFile)) {
                    dataProcessJob.processFile(task.getId(), reportConfig, localFile);
                    markFileAsProcessed(reportConfig.getId(), fileName);
                }
            } catch (Exception e) {
                log.error("文件处理失败: {}", fileName, e);
                taskService.finishTask(task.getId(), "FAILED", e.getMessage());
            }
        }
    }

    private boolean isFileComplete(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        long size = file.length();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long newSize = file.length();
        return size == newSize && size > 0;
    }

    private boolean isFileProcessed(Long reportConfigId, String fileName) {
        return false;
    }

    private void markFileAsProcessed(Long reportConfigId, String fileName) {
    }
}
