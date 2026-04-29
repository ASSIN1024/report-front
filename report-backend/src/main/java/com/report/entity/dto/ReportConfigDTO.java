package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ReportConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String reportCode;

    private String reportName;

    private Long ftpConfigId;

    private String ftpConfigName;

    private String scanPath;

    private String filePattern;

    private Integer sheetIndex;

    private Integer headerRow;

    private Integer dataStartRow;

    private Integer skipColumns;

    private String dateExtractPattern;

    private List<ColumnMapping> columnMappings;

    private String outputTable;

    private Integer startRow;

    private Integer startCol;

    private String mappingMode;

    private String duplicateColStrategy;

    private Integer odsBackupEnabled;

    private String odsTableName;

    private String targetTableType;

    private String targetDbName;

    private Integer isOverseas;

    private String loadMode;

    private String fieldTypeJson;

    private Integer sparkExecutorNum;

    private Integer sparkExecutorCores;

    private String sparkExecutorMemory;

    private Integer sparkDriverNum;

    private String sparkDriverMemory;

    private String partitionInfo;

    private Integer status;

    private String remark;
}
