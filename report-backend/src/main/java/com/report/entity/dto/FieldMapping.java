package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class FieldMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private String excelColumn;

    private String fieldName;

    private String fieldType;

    private String dateFormat;

    private Integer scale;

    private String sourceField;

    private String targetField;

    private String transformType;

    private String transformRule;

    private List<CleanRule> cleanRules;

    private Validators validators;
}
