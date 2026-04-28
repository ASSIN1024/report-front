package com.report.engine;

import lombok.Data;
import java.io.File;

@Data
public class MatchedFile {
    private String fileName;
    private String filePath;
    private Long reportConfigId;
    private Long taskId;
    private String ptDt;
    private File localFile;
}
