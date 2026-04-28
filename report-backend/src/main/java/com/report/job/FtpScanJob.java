package com.report.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.entity.FtpConfig;
import com.report.entity.ReportConfig;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import com.report.service.FtpConfigService;
import com.report.service.ProcessedFileService;
import com.report.service.ReportConfigService;
import com.report.util.FileNameDateExtractor;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@DisallowConcurrentExecution
public class FtpScanJob implements Job {

    @Autowired
    private FtpConfigService ftpConfigService;

    @Autowired
    private ReportConfigService reportConfigService;

    @Autowired
    private ProcessedFileService processedFileService;

    @Autowired
    private MiddlewareEngine middlewareEngine;

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired(required = false)
    private EmbeddedFtpServer embeddedFtpServer;

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
            middlewareEngine.deliverPendingZips(ftpConfig.getId());
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

            MatchedFile matchedFile = new MatchedFile();
            matchedFile.setFileName(fileName);
            matchedFile.setFilePath(file.getAbsolutePath());
            matchedFile.setReportConfigId(reportConfig.getId());
            matchedFile.setLocalFile(file);
            LocalDate date = FileNameDateExtractor.extractDate(fileName);
            matchedFile.setPtDt(date != null ? date.toString() : null);

            try {
                middlewareEngine.processFile(matchedFile, reportConfig);
            } catch (Exception e) {
                log.error("文件处理失败: {}", fileName, e);
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

            FtpUtil.ensureDirectories(ftpConfig,
                ftpConfig.getScanPath(),
                ftpConfig.getStagingDir(),
                ftpConfig.getArchiveDir(),
                ftpConfig.getForUploadDir(),
                ftpConfig.getErrorDir());

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

            try {
                File localFile = downloadToLocalFile(ftpClient, filePath, fileName);
                if (localFile != null && localFile.exists()) {
                    MatchedFile matchedFile = new MatchedFile();
                    matchedFile.setFileName(fileName);
                    matchedFile.setFilePath(filePath);
                    matchedFile.setReportConfigId(reportConfig.getId());
                    matchedFile.setLocalFile(localFile);
                    LocalDate date = FileNameDateExtractor.extractDate(fileName);
                    matchedFile.setPtDt(date != null ? date.toString() : null);

                    middlewareEngine.processFile(matchedFile, reportConfig);
                }
            } catch (Exception e) {
                log.error("文件处理失败: {}", fileName, e);
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
