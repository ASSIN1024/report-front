package com.report.service;

import com.report.entity.ReportConfig;

public interface PackagingService {

    void moveToStagingDir(String standardExcelPath, String sourceFileName, TransformResult result, ReportConfig config);

    void collectAndPackageAll();

    int collectAndPackageBySizeLimit(long maxSizeBytes);

    String packageToStaging(Long ftpConfigId, String standardExcelPath, String sourceFileName, TransformResult result);
}