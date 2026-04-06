package com.report.trigger;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface ITriggerService {
    List<TriggerConfig> getAllEnabled();
    TriggerConfig getByCode(String triggerCode);
    int checkDataExists(TriggerConfig config, Date partitionDate);
    void updateLastTriggerTime(String triggerCode);

    TriggerRealtimeState getRealtimeState(String triggerCode);
    List<TriggerRealtimeState> getRealtimeStates();
    void logTriggerExecution(TriggerExecutionLog log);
    List<TriggerExecutionLog> getExecutionHistory(String triggerCode, LocalDate partitionDate);
}
