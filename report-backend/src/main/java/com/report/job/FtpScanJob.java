package com.report.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.FtpConfig;
import com.report.entity.ReportConfig;
import com.report.entity.TaskExecution;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import com.report.service.FtpConfigService;
import com.report.service.LogService;
import com.report.service.ProcessedFileService;
import com.report.service.ReportConfigService;
import com.report.service.TaskService;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private ProcessedFileService processedFileService;

    @Autowired
    private DataProcessJob dataProcessJob;

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired(required = false)
    private EmbeddedFtpServer embeddedFtpServer;

    public void scanReportConfig(Long reportConfigId, Long taskId) {
        ReportConfig reportConfig = reportConfigService.getById(reportConfigId);
        if (reportConfig == null) {
            log.error("报表配置不存在: {}", reportConfigId);
            taskService.finishTask(taskId, "FAILED", "报表配置不存在");
            return;
        }

        FtpConfig ftpConfig = ftpConfigService.getById(reportConfig.getFtpConfigId());
        if (ftpConfig == null) {
            log.error("FTP配置不存在: {}", reportConfig.getFtpConfigId());
            taskService.finishTask(taskId, "FAILED", "FTP配置不存在");
            return;
        }

        FTPClient ftpClient = null;
        try {
            ftpClient = FtpUtil.connect(ftpConfig);
            if (ftpClient == null || !ftpClient.isConnected()) {
                log.error("FTP连接失败: {}", ftpConfig.getConfigName());
                taskService.finishTask(taskId, "FAILED", "FTP连接失败");
                return;
            }

            String scanPath = ftpConfig.getScanPath() != null ? ftpConfig.getScanPath() : "/";
            List<String> files = FtpUtil.listFiles(ftpClient, scanPath, reportConfig.getFilePattern());
            if (files == null || files.isEmpty()) {
                log.info("FTP目录中没有匹配的文件: {}", scanPath);
                taskService.finishTask(taskId, "NO_FILE", "未匹配到文件");
                logService.saveLog(taskId, "WARN", "FTP目录 " + scanPath + " 中没有匹配 " + reportConfig.getFilePattern() + " 的文件");
                return;
            }

            log.info("扫描到 {} 个匹配文件", files.size());
            int processedCount = 0;

            for (String filePath : files) {
                String fileName = new File(filePath).getName();
                log.info("处理文件: {}", fileName);

                try {
                    File localFile = downloadToLocalFile(ftpClient, filePath, fileName);
                    if (localFile != null && localFile.exists()) {
                        dataProcessJob.processFile(taskId, reportConfig, localFile);
                        processedFileService.markAsProcessed(reportConfig.getId(), fileName, localFile.length(), taskId);
                        localFile.delete();
                        processedCount++;
                    }
                } catch (Exception e) {
                    log.error("文件处理失败: {}", fileName, e);
                    processedFileService.markAsFailed(reportConfig.getId(), fileName, taskId, e.getMessage());
                }
            }

            taskService.finishTask(taskId, "SUCCESS", null);
            logService.saveLog(taskId, "INFO", "扫描完成，共处理 " + processedCount + " 个文件");

        } catch (Exception e) {
            log.error("FTP扫描异常: {}", reportConfig.getReportName(), e);
            taskService.finishTask(taskId, "FAILED", e.getMessage());
        } finally {
            FtpUtil.disconnect(ftpClient);
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("FTP扫描任务开始执行");

        if (embeddedFtpServer != null && embeddedFtpServer.isRunning()) {
            scanBuiltInFtp();
        }

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

    private void scanBuiltInFtp() {
        if (builtInFtpConfigService == null) {
            log.warn("内置FTP服务未配置");
            return;
        }

        BuiltInFtpConfig config = builtInFtpConfigService.getConfig();
        if (config == null || !config.getEnabled()) {
            log.info("内置FTP未启用");
            return;
        }

        log.info("开始扫描内置FTP目录: {}", config.getRootDirectory());

        File ftpRoot = new File(config.getRootDirectory());
        File uploadDir = new File(ftpRoot, "upload");

        if (!uploadDir.exists() || !uploadDir.isDirectory()) {
            log.info("内置FTP上传目录不存在: {}", uploadDir.getAbsolutePath());
            return;
        }

        List<ReportConfig> reportConfigs = reportConfigService.list(
            new LambdaQueryWrapper<ReportConfig>()
                .eq(ReportConfig::getFtpConfigId, -1L)
                .eq(ReportConfig::getStatus, 1)
                .eq(ReportConfig::getDeleted, 0)
        );

        if (reportConfigs.isEmpty()) {
            log.info("没有配置使用内置FTP的报表");
            return;
        }

        for (ReportConfig reportConfig : reportConfigs) {
            scanBuiltInFtpDirectory(reportConfig, uploadDir);
        }
    }

    private void scanBuiltInFtpDirectory(ReportConfig reportConfig, File uploadDir) {
        String pattern = reportConfig.getFilePattern();
        String fileRegex = pattern.replace("*", ".*").replace("?", ".");

        File[] files = uploadDir.listFiles((dir, name) -> name.matches(fileRegex));
        if (files == null || files.length == 0) {
            log.info("内置FTP上传目录没有匹配的文件: {}", reportConfig.getFilePattern());
            return;
        }

        log.info("扫描到 {} 个匹配文件", files.length);

        for (File file : files) {
            String fileName = file.getName();
            if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
                log.info("文件已处理过，跳过: {}", fileName);
                continue;
            }

            log.info("检测到新文件: {}, 报表配置: {}", fileName, reportConfig.getReportName());

            TaskExecution task = taskService.createTask(
                "FTP_SCAN",
                "内置FTP扫描-" + reportConfig.getReportName(),
                reportConfig.getId(),
                fileName,
                file.getAbsolutePath()
            );

            try {
                dataProcessJob.processFile(task.getId(), reportConfig, file);
                processedFileService.markAsProcessed(reportConfig.getId(), fileName, file.length(), task.getId());
            } catch (Exception e) {
                log.error("文件处理失败: {}", fileName, e);
                taskService.finishTask(task.getId(), "FAILED", e.getMessage());
                processedFileService.markAsFailed(reportConfig.getId(), fileName, task.getId(), e.getMessage());
            }
        }
    }

    private void scanFtpDirectory(FtpConfig ftpConfig) {
        log.info("开始扫描FTP配置: {}, 主机: {}, 路径: {}",
                ftpConfig.getConfigName(), ftpConfig.getHost(), ftpConfig.getScanPath());

        FTPClient ftpClient = null;
        try {
            ftpClient = FtpUtil.connect(ftpConfig);
            if (ftpClient == null || !ftpClient.isConnected()) {
                log.error("FTP连接失败: {}", ftpConfig.getConfigName());
                return;
            }

            List<String> files = FtpUtil.listFiles(ftpClient, ftpConfig.getScanPath(), ftpConfig.getFilePattern());
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
                matchAndProcessFiles(ftpClient, ftpConfig, reportConfig, files);
            }

        } catch (Exception e) {
            log.error("FTP扫描异常: {}", ftpConfig.getConfigName(), e);
        } finally {
            FtpUtil.disconnect(ftpClient);
        }
    }

    private void matchAndProcessFiles(FTPClient ftpClient, FtpConfig ftpConfig, ReportConfig reportConfig, List<String> files) {
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

            log.info("检测到新文件: {}, 报表配置: {}", fileName, reportConfig.getReportName());

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
                    processedFileService.markAsProcessed(reportConfig.getId(), fileName, localFile.length(), task.getId());
                    localFile.delete();
                }
            } catch (Exception e) {
                log.error("文件处理失败: {}", fileName, e);
                taskService.finishTask(task.getId(), "FAILED", e.getMessage());
                processedFileService.markAsFailed(reportConfig.getId(), fileName, task.getId(), e.getMessage());
            }
        }
    }

    private File downloadToLocalFile(FTPClient ftpClient, String remotePath, String fileName) throws IOException {
        byte[] data = FtpUtil.downloadFile(ftpClient, remotePath);
        if (data == null || data.length == 0) {
            return null;
        }
        File tempFile = File.createTempFile("ftp_", "_" + fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(data);
        }
        return tempFile;
    }

}