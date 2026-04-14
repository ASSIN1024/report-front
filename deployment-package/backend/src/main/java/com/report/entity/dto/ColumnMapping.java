package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class ColumnMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private String excelColumn;

    private String fieldName;

    private String fieldType;

    private String dateFormat;

    private Integer scale;

    private List<CleanRule> cleanRules;

    private Validators validators;
}
