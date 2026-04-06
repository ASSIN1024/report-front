package com.report.trigger;

import java.util.Date;
import java.util.List;

public interface ITriggerService {
    List<TriggerConfig> getAllEnabled();
    TriggerConfig getByCode(String triggerCode);
    int checkDataExists(TriggerConfig config, Date partitionDate);
    void updateLastTriggerTime(String triggerCode);
}
