package com.report.trigger;

import java.time.LocalDate;
import java.util.List;

public interface TriggerService {
    List<TriggerConfig> getAllEnabled();
    TriggerConfig getByCode(String triggerCode);
    int checkDataExists(TriggerConfig config, LocalDate partitionDate);
    void updateLastTriggerTime(String triggerCode);
}