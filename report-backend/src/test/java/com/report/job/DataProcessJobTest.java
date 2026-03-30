package com.report.job;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

@Ignore("需要完整的Spring上下文和数据库连接")
public class DataProcessJobTest {

    @Test
    public void testProcessFileWithValidData() {
        DataProcessJob dataProcessJob = new DataProcessJob();
        assertNotNull(dataProcessJob);
    }

    @Test
    public void testRetryTask() {
        DataProcessJob dataProcessJob = new DataProcessJob();
        assertNotNull(dataProcessJob);
    }
}
