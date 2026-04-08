package com.report.trigger;

public interface TriggerStateManager {

    TriggerState getOrCreate(String triggerCode);

    void reset(String triggerCode);

    void incrementRetryCount(String triggerCode);

    void setTriggered(String triggerCode, boolean triggered);
}
