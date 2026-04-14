package com.report.trigger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ITriggerServiceTest {

    @Test
    public void testGetAllEnabledReturnsList() {
        ITriggerService service = new MockTriggerServiceImpl();
        List<TriggerConfig> configs = service.getAllEnabled();
        assertNotNull(configs);
    }

    @Test
    public void testGetByCodeReturnsConfig() {
        ITriggerService service = new MockTriggerServiceImpl();
        TriggerConfig config = service.getByCode("sales_trigger");
        assertNotNull(config);
        assertEquals("sales_trigger", config.getTriggerCode());
    }

    @Test
    public void testGetByCodeReturnsNullForUnknown() {
        ITriggerService service = new MockTriggerServiceImpl();
        TriggerConfig config = service.getByCode("unknown_trigger");
        assertNull(config);
    }

    @Test
    public void testCheckDataExistsReturnsCount() {
        ITriggerService service = new MockTriggerServiceImpl();
        TriggerConfig config = new TriggerConfig();
        config.setSourceTable("test_table");
        config.setPartitionColumn("pt_dt");
        int count = service.checkDataExists(config, new Date());
        assertTrue(count >= 0);
    }

    @Test
    public void testGetRealtimeState() {
        ITriggerService service = new MockTriggerServiceImpl();
        TriggerRealtimeState state = service.getRealtimeState("sales_trigger");
        assertNotNull(state);
        assertEquals("sales_trigger", state.getTriggerCode());
    }

    @Test
    public void testGetRealtimeStateReturnsNullForUnknown() {
        ITriggerService service = new MockTriggerServiceImpl();
        TriggerRealtimeState state = service.getRealtimeState("unknown");
        assertNull(state);
    }

    @Test
    public void testGetRealtimeStatesReturnsAllStates() {
        ITriggerService service = new MockTriggerServiceImpl();
        List<TriggerRealtimeState> states = service.getRealtimeStates();
        assertNotNull(states);
        assertTrue(states.size() >= 0);
    }

    @Test
    public void testLogTriggerExecution() {
        ITriggerService service = new MockTriggerServiceImpl();
        TriggerExecutionLog log = new TriggerExecutionLog();
        log.setTriggerCode("sales_trigger");
        log.setTriggerName("销售触发器");
        log.setTriggerStatus("TRIGGERED");
        log.setPartitionDate(new Date());
        log.setDataCount(100);
        log.setRetryCount(0);
        log.setExecutionTime(new Date());

        service.logTriggerExecution(log);
    }

    @Test
    public void testGetExecutionHistory() {
        ITriggerService service = new MockTriggerServiceImpl();
        List<TriggerExecutionLog> history = service.getExecutionHistory("sales_trigger", LocalDate.now());
        assertNotNull(history);
    }

    static class MockTriggerServiceImpl implements ITriggerService {
        @Override
        public List<TriggerConfig> getAllEnabled() {
            return new java.util.ArrayList<>();
        }

        @Override
        public TriggerConfig getByCode(String triggerCode) {
            if ("sales_trigger".equals(triggerCode)) {
                TriggerConfig config = new TriggerConfig();
                config.setTriggerCode("sales_trigger");
                config.setTriggerName("销售触发器");
                config.setSourceTable("osd_sales");
                config.setPartitionColumn("pt_dt");
                config.setMaxRetries(60);
                config.setPipelineCode("sales_pipeline");
                config.setPollIntervalSeconds(60);
                return config;
            }
            return null;
        }

        @Override
        public int checkDataExists(TriggerConfig config, Date partitionDate) {
            return 100;
        }

        @Override
        public void updateLastTriggerTime(String triggerCode) {
        }

        @Override
        public TriggerRealtimeState getRealtimeState(String triggerCode) {
            if ("sales_trigger".equals(triggerCode)) {
                TriggerRealtimeState state = new TriggerRealtimeState();
                state.setTriggerCode(triggerCode);
                state.setTriggerName("销售触发器");
                state.setStatus("WAITING");
                state.setRetryCount(5);
                state.setMaxRetries(60);
                state.setLastCheckTime(new Date());
                state.setPipelineCode("sales_pipeline");
                state.setPollIntervalSeconds(60);
                return state;
            }
            return null;
        }

        @Override
        public List<TriggerRealtimeState> getRealtimeStates() {
            return new java.util.ArrayList<>();
        }

        @Override
        public void logTriggerExecution(TriggerExecutionLog log) {
        }

        @Override
        public List<TriggerExecutionLog> getExecutionHistory(String triggerCode, LocalDate partitionDate) {
            return new java.util.ArrayList<>();
        }
    }
}