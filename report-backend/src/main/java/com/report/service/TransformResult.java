package com.report.service;

import lombok.Data;

@Data
public class TransformResult {
    private boolean success;
    private String standardExcelPath;
    private String ptDt;
    private String dbName;
    private String tableName;
    private String fieldMappingJson;
    private String loadMode;
    private String errorMessage;
}
