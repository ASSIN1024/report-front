package com.report.dto;

import lombok.Data;

@Data
public class LogLineDTO {
    
    private Integer lineNumber;
    
    private String timestamp;
    
    private String level;
    
    private String logger;
    
    private String message;
    
    private String rawLine;
}
