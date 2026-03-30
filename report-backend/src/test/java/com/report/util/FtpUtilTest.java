package com.report.util;

import com.report.entity.FtpConfig;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class FtpUtilTest {

    private static final String FTP_HOST = "127.0.0.1";
    private static final int FTP_PORT = 21;
    private static final String FTP_USER = "ftpuser";
    private static final String FTP_PASS = "ftppass";

    private FtpConfig ftpConfig;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(30);

    @Before
    public void setUp() {
        ftpConfig = new FtpConfig();
        ftpConfig.setHost(FTP_HOST);
        ftpConfig.setPort(FTP_PORT);
        ftpConfig.setUsername(FTP_USER);
        ftpConfig.setPassword(FTP_PASS);
        ftpConfig.setScanPath("/");
    }

    @Test
    public void testDisconnect_NullClient() {
        FtpUtil.disconnect(null);
    }

    @Test
    public void testTestConnection_ExceptionReturnsFalse() {
        FtpConfig wrongConfig = new FtpConfig();
        wrongConfig.setHost("10.255.255.1");
        wrongConfig.setPort(21);
        wrongConfig.setUsername("test");
        wrongConfig.setPassword("test");
        
        boolean result = FtpUtil.testConnection(wrongConfig);
        assertFalse("连接失败应该返回false", result);
    }

    @Test(expected = NullPointerException.class)
    public void testConnect_NullConfig() throws IOException {
        FtpUtil.connect(null);
    }

    @Test
    public void testFtpConfigGettersSetters() {
        FtpConfig config = new FtpConfig();
        config.setHost("192.168.1.100");
        config.setPort(2121);
        config.setUsername("testuser");
        config.setPassword("testpass");
        config.setScanPath("/data");
        config.setFilePattern("*.xlsx");
        config.setScanInterval(300);
        config.setStatus(1);
        config.setRemark("测试配置");
        
        assertEquals("192.168.1.100", config.getHost());
        assertEquals(Integer.valueOf(2121), config.getPort());
        assertEquals("testuser", config.getUsername());
        assertEquals("testpass", config.getPassword());
        assertEquals("/data", config.getScanPath());
        assertEquals("*.xlsx", config.getFilePattern());
        assertEquals(Integer.valueOf(300), config.getScanInterval());
        assertEquals(Integer.valueOf(1), config.getStatus());
        assertEquals("测试配置", config.getRemark());
    }
}
