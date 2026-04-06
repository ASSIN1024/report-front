package com.report.trigger;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class TriggerStateManager {
    private final ConcurrentHashMap<String, TriggerState> states = new ConcurrentHashMap<>();

    public TriggerState getOrCreate(String triggerCode) {
        return states.computeIfAbsent(triggerCode, code -> {
            TriggerState state = new TriggerState();
            state.setTriggerCode(code);
            state.setRetryCount(0);
            state.setTriggered(false);
            return state;
        });
    }

    public void reset(String triggerCode) {
        TriggerState state = states.get(triggerCode);
        if (state != null) {
            state.setRetryCount(0);
            state.setTriggered(false);
        }
    }
}