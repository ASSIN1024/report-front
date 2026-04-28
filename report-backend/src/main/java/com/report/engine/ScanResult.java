package com.report.engine;

import lombok.Data;
import java.util.List;

@Data
public class ScanResult {
    private List<MatchedFile> matchedFiles;
    private int totalScanned;
}
