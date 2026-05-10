package com.report.service.impl;

import com.report.common.enums.ProcessStep;
import com.report.entity.ProcessMonitorLog;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.FtpConfigProvider;
import com.report.service.FtpDirectoryService;
import com.report.service.MonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
public class FtpDirectoryServiceImpl implements FtpDirectoryService {

    @Autowired
    private FtpConfigProvider ftpConfigProvider;

    @Autowired
    private MonitorService monitorService;

    @Override
    public boolean createDirectoriesIfNotExist(String scanPath) {
        BuiltInFtpConfig config = ftpConfigProvider.getConfig();
        if (config == null) {
            log.error("[FtpDirectory] FTP配置不存在");
            return false;
        }

        String rootDir = config.getRootDirectory();
        File ftpRoot = new File(rootDir);
        if (!ftpRoot.exists()) {
            ftpRoot.mkdirs();
        }

        File targetDir = new File(ftpRoot, scanPath);
        if (targetDir.exists()) {
            log.info("[FtpDirectory] 目录已存在，无需创建: {}", targetDir.getAbsolutePath());
            return true;
        }

        boolean created = targetDir.mkdirs();
        if (created) {
            log.info("[FtpDirectory] 目录创建成功: {}", targetDir.getAbsolutePath());
        } else {
            log.error("[FtpDirectory] 目录创建失败: {}", targetDir.getAbsolutePath());
        }
        return created;
    }

    @Override
    public boolean createDirectory(String directory) {
        File dir = new File(directory);
        if (dir.exists()) {
            return true;
        }
        boolean created = dir.mkdirs();
        if (created) {
            log.info("[FtpDirectory] 创建目录: {}", directory);
        }
        return created;
    }

    @Override
    public boolean directoryExists(String path) {
        BuiltInFtpConfig config = ftpConfigProvider.getConfig();
        if (config == null) {
            return false;
        }
        File dir = new File(config.getRootDirectory(), path);
        return dir.exists() && dir.isDirectory();
    }

    @Override
    public Long getOrCreateMonitorId(String reportCode, String scanPath) {
        Long monitorId = monitorService.startMonitor(reportCode, null, ProcessStep.DIR_CREATING.name());
        boolean success = createDirectoriesIfNotExist(scanPath);
        if (success) {
            monitorService.updateMonitor(monitorId, ProcessStep.DIR_CREATED.name(),
                    ProcessMonitorLog.STATUS_SUCCESS, "目录创建成功: " + scanPath);
        } else {
            monitorService.updateMonitor(monitorId, ProcessStep.DIR_CREATED.name(),
                    ProcessMonitorLog.STATUS_FAILED, "目录创建失败: " + scanPath);
        }
        return monitorId;
    }
}
