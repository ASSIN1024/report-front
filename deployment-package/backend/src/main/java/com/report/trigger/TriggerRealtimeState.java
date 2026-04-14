package com.report.trigger;

import lombok.Data;
import java.util.Date;

@Data
public class TriggerRealtimeState {
    private String triggerCode;
    private String triggerName;
    private String status;
    private int retryCount;
    private int maxRetries;
    private Date lastCheckTime;
    private Date lastTriggerTime;
    private String pipelineCode;
    private Integer pollIntervalSeconds;
}