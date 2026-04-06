package com.report.trigger;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TriggerConfig {
    private Long id;
    private String triggerCode;
    private String triggerName;
    private String sourceTable;
    private String partitionColumn;
    private String partitionPattern;
    private Integer pollIntervalSeconds;
    private Integer maxRetries;
    private String pipelineCode;
    private String status;
    private LocalDateTime lastTriggerTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}