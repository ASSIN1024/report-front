package com.report.service.impl;

import com.report.entity.ReportConfig;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigMapper;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.service.ArchiveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
public class ArchiveServiceImpl implements ArchiveService {

    @Autowired(required = false)
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Autowired(required = false)
    private BuiltInFtpConfigMapper builtInFtpConfigMapper;

    @Override
    public void archiveToSuccess(File localFile, ReportConfig config) {
        archive(localFile, config, "archive");
    }

    @Override
    public void archiveToError(File localFile, ReportConfig config) {
        archive(localFile, config, "error");
    }

    private void archive(File localFile, ReportConfig config, String dirType) {
        if (localFile == null || !localFile.exists()) {
            return;
        }

        BuiltInFtpConfig ftpConfig = builtInFtpConfigMapper != null ? builtInFtpConfigMapper.getConfig() : null;
        if (ftpConfig == null) {
            log.warn("Built-in FTP config not found for archiving, keeping local file: {}", localFile.getName());
            return;
        }

        String targetDir = resolveDir(ftpConfig, config.getScanPath(), dirType);
        Path targetPath = new File(ftpConfig.getRootDirectory(), targetDir).toPath();
        Path targetFile = targetPath.resolve(localFile.getName());

        FileInputStream fis = null;
        try {
            Files.createDirectories(targetPath);

            if (Files.exists(targetFile)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                String newFileName = localFile.getName().replace(".", "_" + sdf.format(new Date()) + ".");
                targetFile = targetPath.resolve(newFileName);
            }

            Files.copy(localFile.toPath(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            localFile.delete();
            log.info("Archived file to {}: {}", dirType, targetFile);
        } catch (Exception e) {
            log.warn("Archive failed, keeping local file: {}", localFile.getName(), e);
        }
    }

    private String resolveDir(BuiltInFtpConfig ftpConfig, String scanPath, String dirType) {
        if (scanPath == null || scanPath.isEmpty()) {
            scanPath = "/upload";
        }
        switch (dirType) {
            case "archive":
                return scanPath + "/archive";
            case "error":
                return scanPath + "/error";
            default:
                return scanPath + "/" + dirType;
        }
    }
}
