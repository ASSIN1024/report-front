package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("report_config")
public class ReportConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String reportCode;

    private String reportName;

    private Long ftpConfigId;

    private String scanPath;

    private String filePattern;

    private Integer sheetIndex;

    private Integer headerRow;

    private Integer dataStartRow;

    private Integer skipColumns;

    private String dateExtractPattern;

    private String columnMapping;

    private String outputTable;

    private String outputMode;

    private Integer startRow;

    private Integer startCol;

    private String mappingMode;

    private String duplicateColStrategy;

    private Integer odsBackupEnabled;

    private String odsTableName;

    private String loadMode;

    private String targetTableType;
    private String targetDbName;
    private Integer isOverseas;
    private String fieldTypeJson;
    private Integer sparkExecutorNum;
    private Integer sparkExecutorCores;
    private String sparkExecutorMemory;
    private Integer sparkDriverNum;
    private String sparkDriverMemory;

    private Integer status;

    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
