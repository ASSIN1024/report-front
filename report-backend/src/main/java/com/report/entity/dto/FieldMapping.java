package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FieldMapping implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sourceField;

    private String targetField;

    private String transformType;

    private String transformRule;
}
