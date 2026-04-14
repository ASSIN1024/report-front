package com.report.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class DataQueryDTO {
    private String tableName;
    private List<String> columns;
    private String condition;
    private Integer pageNum;
    private Integer pageSize;
}
