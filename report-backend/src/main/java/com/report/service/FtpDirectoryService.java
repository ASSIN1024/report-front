package com.report.service;

public interface FtpDirectoryService {

    boolean createDirectoriesIfNotExist(String scanPath);

    boolean createDirectory(String directory);

    boolean directoryExists(String path);

    Long getOrCreateMonitorId(String reportCode, String scanPath);
}