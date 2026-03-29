package com.report.util;

import com.report.entity.FtpConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("需要真实的FTP服务器")
public class FtpUtilTest {

    private FtpConfig createTestConfig() {
        FtpConfig config = new FtpConfig();
        config.setHost("192.168.1.100");
        config.setPort(21);
        config.setUsername("ftpuser");
        config.setPassword("ftppass");
        config.setScanPath("/data/reports");
        return config;
    }

    @Test
    public void testConnection() {
        FtpConfig config = createTestConfig();
        boolean result = FtpUtil.testConnection(config);
        assertTrue(result, "FTP连接测试应该成功");
    }

    @Test
    public void testListFiles() {
        FtpConfig config = createTestConfig();
        boolean connected = FtpUtil.testConnection(config);
        if (connected) {
            var files = FtpUtil.listFiles(config);
            assertNotNull(files);
            FtpUtil.disconnect(config);
        }
    }
}
