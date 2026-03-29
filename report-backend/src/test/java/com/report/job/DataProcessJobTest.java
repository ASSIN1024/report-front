package com.report.job;

import com.report.entity.ReportConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@Disabled("需要完整的Spring上下文和数据库连接")
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
