package com.report.service;

import com.report.entity.ReportConfig;
import java.io.File;

public interface ArchiveService {
    void archiveToSuccess(File localFile, ReportConfig config);
    void archiveToError(File localFile, ReportConfig config);
}
