package com.report.trigger;

import lombok.Data;
import java.util.Date;

@Data
public class TriggerState {
    private String triggerCode;
    private int retryCount;
    private Date lastCheckTime;
    private boolean triggered;
}
