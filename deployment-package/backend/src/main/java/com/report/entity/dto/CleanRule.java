package com.report.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CleanRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pattern;
    private String replace;
}
