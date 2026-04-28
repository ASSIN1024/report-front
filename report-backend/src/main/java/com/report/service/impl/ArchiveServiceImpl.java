package com.report.service.impl;

import com.report.entity.FtpConfig;
import com.report.entity.ReportConfig;
import com.report.mapper.FtpConfigMapper;
import com.report.service.ArchiveService;
import com.report.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
@Service
public class ArchiveServiceImpl implements ArchiveService {

    @Autowired
    private FtpConfigMapper ftpConfigMapper;

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

        FtpConfig ftpConfig = ftpConfigMapper.selectById(config.getFtpConfigId());
        if (ftpConfig == null) {
            log.warn("FTP config not found for archiving, keeping local file: {}", localFile.getName());
            return;
        }

        String targetDir = resolveDir(ftpConfig, dirType);
        String remotePath = targetDir + "/" + localFile.getName();

        FTPClient ftpClient = null;
        FileInputStream fis = null;
        try {
            ftpClient = FtpUtil.connect(ftpConfig);
            ftpClient.makeDirectory(targetDir);

            if (fileExistsOnFtp(ftpClient, remotePath)) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                remotePath = targetDir + "/" + localFile.getName().replace(".", "_" + sdf.format(new Date()) + ".");
            }

            fis = new FileInputStream(localFile);
            boolean stored = ftpClient.storeFile(remotePath, fis);
            if (stored) {
                localFile.delete();
                log.info("Archived file to {}: {}", dirType, remotePath);
            } else {
                log.warn("Failed to archive file, keeping local: {}", localFile.getName());
            }
        } catch (Exception e) {
            log.warn("Archive failed, keeping local file: {}", localFile.getName(), e);
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (IOException ignored) {}
            }
            FtpUtil.disconnect(ftpClient);
        }
    }

    private boolean fileExistsOnFtp(FTPClient ftpClient, String remotePath) throws IOException {
        String[] names = ftpClient.listNames(remotePath);
        return names != null && names.length > 0;
    }

    private String resolveDir(FtpConfig ftpConfig, String dirType) {
        switch (dirType) {
            case "archive":
                if (StringUtils.hasText(ftpConfig.getArchiveDir())) {
                    return ftpConfig.getArchiveDir();
                }
                return ftpConfig.getScanPath() + "/archive";
            case "error":
                if (StringUtils.hasText(ftpConfig.getErrorDir())) {
                    return ftpConfig.getErrorDir();
                }
                return ftpConfig.getScanPath() + "/error";
            default:
                return ftpConfig.getScanPath() + "/" + dirType;
        }
    }
}
