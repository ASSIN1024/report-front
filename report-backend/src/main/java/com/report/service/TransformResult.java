package com.report.service;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class TransformResult {
    private boolean success;
    private String sourceFile;
    private String standardExcelPath;
    private String ptDt;
    private String dbName;
    private String tableName;
    private String fieldMappingJson;
    private String sourceToFieldMapping;
    private String loadMode;
    private String errorMessage;
    private Long fileSize;
    private List<String> headers;
    private List<Map<String, Object>> rows;
}
