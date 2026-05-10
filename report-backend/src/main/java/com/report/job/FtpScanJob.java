package com.report.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.engine.MatchedFile;
import com.report.engine.MiddlewareEngine;
import com.report.entity.ReportConfig;
import com.report.ftp.EmbeddedFtpServer;
import com.report.ftp.FtpBuiltInProperties;
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
import java.util.regex.Pattern;

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
    private EmbeddedFtpServer embeddedFtpServer;

    @Autowired(required = false)
    private FtpBuiltInProperties ftpProperties;

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
        if (ftpProperties == null || !ftpProperties.isEnabled()) {
            log.info("内置FTP未启用，跳过扫描");
            return;
        }

        String rootDir = ftpProperties.getRootDirectory();
        log.info("开始扫描内置FTP目录: {}", rootDir);

        File ftpRoot = new File(rootDir);

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
            try {
                scanReportDirectory(reportConfig, ftpRoot);
            } catch (Exception e) {
                log.error("扫描报表配置失败: id={}, name={}", reportConfig.getId(), reportConfig.getReportName(), e);
            }
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
        if (pattern == null || pattern.trim().isEmpty()) {
            pattern = "*";
        }

        Pattern fileRegex = globToRegex(pattern);

        File[] files = scanDir.listFiles((dir, name) -> {
            if (name.startsWith(".") || name.equalsIgnoreCase("archive") || name.equalsIgnoreCase("error")) {
                return false;
            }
            return fileRegex.matcher(name).matches();
        });

        if (files == null || files.length == 0) {
            log.info("扫描目录没有匹配的文件: pattern={}, dir={}", pattern, scanDir.getAbsolutePath());
            return;
        }

        log.info("扫描到 {} 个匹配文件 (配置: {})", files.length, reportConfig.getReportName());

        for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }

            String fileName = file.getName();
            if (processedFileService.isFileProcessed(reportConfig.getId(), fileName)) {
                log.info("文件已处理过，跳过: {}", fileName);
                continue;
            }

            log.info("检测到新文件: {}, 报表配置: {}", fileName, reportConfig.getReportName());

            File tempFile = null;
            try {
                tempFile = File.createTempFile("scan_", "_" + fileName);
                java.nio.file.Files.copy(file.toPath(), tempFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                MatchedFile matchedFile = new MatchedFile();
                matchedFile.setFileName(fileName);
                matchedFile.setFilePath(file.getAbsolutePath());
                matchedFile.setReportConfigId(reportConfig.getId());
                matchedFile.setLocalFile(tempFile);
                LocalDate date = FileNameDateExtractor.extractDate(fileName);
                matchedFile.setPtDt(date != null ? date.toString() : null);

                middlewareEngine.processFile(matchedFile, reportConfig);
            } catch (Exception e) {
                log.error("文件处理失败: {}", fileName, e);
            } finally {
                if (tempFile != null && tempFile.exists()) {
                    try {
                        tempFile.delete();
                    } catch (Exception e) {
                        log.warn("删除临时文件失败: {}", tempFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private Pattern globToRegex(String glob) {
        StringBuilder sb = new StringBuilder("^");
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append(".");
                    break;
                case '.':
                    sb.append("\\.");
                    break;
                case '(':
                case ')':
                case '[':
                case ']':
                case '{':
                case '}':
                case '+':
                case '^':
                case '$':
                case '|':
                case '\\':
                    sb.append("\\").append(c);
                    break;
                default:
                    sb.append(c);
            }
        }
        sb.append("$");
        return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
    }
}
