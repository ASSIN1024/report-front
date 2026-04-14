package com.report.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogQueryDTO {
    
    private String logType;
    
    private String startDate;
    
    private String endDate;
    
    private String level;
    
    private String keyword;
    
    private Integer pageNum = 1;
    
    private Integer pageSize = 100;
    
    private Integer lines;
}
