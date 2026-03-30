package com.report.dto;

import lombok.Data;

import java.util.List;

@Data
public class LogResultDTO {
    
    private String logType;
    
    private String fileName;
    
    private Long fileSize;
    
    private Integer totalLines;
    
    private List<LogLineDTO> lines;
    
    private Integer pageNum;
    
    private Integer pageSize;
    
    private Integer total;
}
