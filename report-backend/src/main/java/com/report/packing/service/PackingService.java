package com.report.packing.service;

import java.util.List;

public interface PackingService {
    String pack(List<Long> processedFileIds);
    void upload(String batchNo);
    boolean canUpload();
    boolean isBeingConsumed();
    String getUploadDir();
    String getDoneDir();
    String getFixedFilename();
    Long getMaxPackageSize();
}