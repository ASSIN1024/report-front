package com.report.service;

import java.util.List;

public interface PackagingService {

    void moveToStagingDir(String standardExcelPath, String sourceFileName, TransformResult result);

    void collectAndPackageAll();

    int collectAndPackageBySizeLimit(long maxSizeBytes);

    String packageToStaging(Long ftpConfigId, String standardExcelPath, String sourceFileName, TransformResult result);
}