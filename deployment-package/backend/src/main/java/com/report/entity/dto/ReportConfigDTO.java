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

    private String filePattern;

    private Integer sheetIndex;

    private Integer headerRow;

    private Integer dataStartRow;

    private List<ColumnMapping> columnMappings;

    private String outputTable;

    private String outputMode;

    private Integer status;

    private String remark;
}
