package com.report.trigger;

import org.junit.Test;
import java.util.Date;

import static org.junit.Assert.*;

public class TriggerRealtimeStateTest {

    @Test
    public void testTriggerRealtimeStateCreation() {
        TriggerRealtimeState state = new TriggerRealtimeState();
        state.setTriggerCode("sales_trigger");
        state.setTriggerName("销售数据触发器");
        state.setStatus("WAITING");
        state.setRetryCount(5);
        state.setMaxRetries(60);
        state.setLastCheckTime(new Date());
        state.setLastTriggerTime(null);
        state.setPipelineCode("sales_pipeline");
        state.setPollIntervalSeconds(60);

        assertEquals("sales_trigger", state.getTriggerCode());
        assertEquals("销售数据触发器", state.getTriggerName());
        assertEquals("WAITING", state.getStatus());
        assertEquals(5, state.getRetryCount());
        assertEquals(60, state.getMaxRetries());
        assertNotNull(state.getLastCheckTime());
        assertNull(state.getLastTriggerTime());
        assertEquals("sales_pipeline", state.getPipelineCode());
        assertEquals(Integer.valueOf(60), state.getPollIntervalSeconds());
    }

    @Test
    public void testTriggerRealtimeStateStatus() {
        TriggerRealtimeState state = new TriggerRealtimeState();

        state.setStatus("WAITING");
        assertEquals("WAITING", state.getStatus());

        state.setStatus("CHECKING");
        assertEquals("CHECKING", state.getStatus());

        state.setStatus("TRIGGERED");
        assertEquals("TRIGGERED", state.getStatus());

        state.setStatus("SKIPPED");
        assertEquals("SKIPPED", state.getStatus());
    }

    @Test
    public void testTriggerRealtimeStateRetryProgress() {
        TriggerRealtimeState state = new TriggerRealtimeState();
        state.setRetryCount(12);
        state.setMaxRetries(60);

        assertEquals("12/60", state.getRetryCount() + "/" + state.getMaxRetries());
        assertTrue(state.getRetryCount() < state.getMaxRetries());
    }

    @Test
    public void testTriggerRealtimeStatePollInterval() {
        TriggerRealtimeState state = new TriggerRealtimeState();

        state.setPollIntervalSeconds(30);
        assertEquals(Integer.valueOf(30), state.getPollIntervalSeconds());

        state.setPollIntervalSeconds(300);
        assertEquals(Integer.valueOf(300), state.getPollIntervalSeconds());
    }
}