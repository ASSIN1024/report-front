package com.report.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.entity.ReportConfig;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import com.report.service.ProcessedFileService;
import com.report.service.ReportConfigService;
import com.report.util.FileNameDateExtractor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@DisallowConcurrentExecution
public class FtpScanJob implements Job {

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

        if (embeddedFtpServer == null || !embeddedFtpServer.isRunning()) {
            log.info("内置FTP服务未运行，跳过扫描");
            return;
        }

        scanBuiltInFtp();

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

        List<ReportConfig> reportConfigs = reportConfigService.list(
            new LambdaQueryWrapper<ReportConfig>()
                .eq(ReportConfig::getStatus, 1)
                .eq(ReportConfig::getDeleted, 0)
        );

        if (reportConfigs.isEmpty()) {
            log.info("没有启用的报表配置");
            return;
        }

        for (ReportConfig reportConfig : reportConfigs) {
            scanReportDirectory(reportConfig, ftpRoot);
        }
    }

    private void scanReportDirectory(ReportConfig reportConfig, File ftpRoot) {
        String scanPath = reportConfig.getScanPath();
        if (scanPath == null || scanPath.isEmpty()) {
            scanPath = "/upload";
        }

        File scanDir = new File(ftpRoot, scanPath);
        if (!scanDir.exists() || !scanDir.isDirectory()) {
            log.info("扫描目录不存在: {}", scanDir.getAbsolutePath());
            return;
        }

        String pattern = reportConfig.getFilePattern();
        String fileRegex = pattern.replace("*", ".*").replace("?", ".");

        File[] files = scanDir.listFiles((dir, name) -> name.matches(fileRegex));
        if (files == null || files.length == 0) {
            log.info("扫描目录没有匹配的文件: {}", reportConfig.getFilePattern());
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
}
