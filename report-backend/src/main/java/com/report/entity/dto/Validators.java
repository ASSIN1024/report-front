package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Validators implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean required;
    private Boolean positiveOnly;
    private String minValue;
    private String maxValue;
}
