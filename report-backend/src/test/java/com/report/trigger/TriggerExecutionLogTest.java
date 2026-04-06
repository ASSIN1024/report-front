package com.report.trigger;

import org.junit.Test;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.Assert.*;

public class TriggerExecutionLogTest {

    @Test
    public void testTriggerExecutionLogCreation() {
        TriggerExecutionLog log = new TriggerExecutionLog();
        log.setId(1L);
        log.setTriggerCode("sales_trigger");
        log.setTriggerName("销售数据触发器");
        log.setPartitionDate(LocalDate.of(2026, 4, 7));
        log.setDataCount(100);
        log.setTriggerStatus("TRIGGERED");
        log.setPipelineTaskId(123L);
        log.setRetryCount(0);
        log.setExecutionTime(new Date());

        assertEquals(Long.valueOf(1L), log.getId());
        assertEquals("sales_trigger", log.getTriggerCode());
        assertEquals("销售数据触发器", log.getTriggerName());
        assertEquals(LocalDate.of(2026, 4, 7), log.getPartitionDate());
        assertEquals(Integer.valueOf(100), log.getDataCount());
        assertEquals("TRIGGERED", log.getTriggerStatus());
        assertEquals(Long.valueOf(123L), log.getPipelineTaskId());
        assertEquals(Integer.valueOf(0), log.getRetryCount());
        assertNotNull(log.getExecutionTime());
    }

    @Test
    public void testTriggerExecutionLogStatus() {
        TriggerExecutionLog log = new TriggerExecutionLog();

        log.setTriggerStatus("WAITING");
        assertEquals("WAITING", log.getTriggerStatus());

        log.setTriggerStatus("TRIGGERED");
        assertEquals("TRIGGERED", log.getTriggerStatus());

        log.setTriggerStatus("SKIPPED");
        assertEquals("SKIPPED", log.getTriggerStatus());

        log.setTriggerStatus("FAILED");
        assertEquals("FAILED", log.getTriggerStatus());
    }

    @Test
    public void testTriggerExecutionLogSettersAndGetters() {
        TriggerExecutionLog log = new TriggerExecutionLog();

        log.setId(100L);
        assertEquals(Long.valueOf(100L), log.getId());

        log.setErrorMessage("Connection timeout");
        assertEquals("Connection timeout", log.getErrorMessage());

        Date now = new Date();
        log.setExecutionTime(now);
        assertEquals(now, log.getExecutionTime());
    }
}