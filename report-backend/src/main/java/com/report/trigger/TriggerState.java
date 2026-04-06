package com.report.trigger;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TriggerState {
    private String triggerCode;
    private int retryCount;
    private LocalDateTime lastCheckTime;
    private boolean triggered;
}