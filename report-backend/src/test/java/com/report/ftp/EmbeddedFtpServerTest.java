package com.report.ftp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EmbeddedFtpServerTest {

    @Mock
    private BuiltInFtpConfigService builtInFtpConfigService;

    private EmbeddedFtpServer embeddedFtpServer;

    @Before
    public void setUp() throws Exception {
        embeddedFtpServer = new EmbeddedFtpServer();
        java.lang.reflect.Field field = EmbeddedFtpServer.class.getDeclaredField("builtInFtpConfigService");
        field.setAccessible(true);
        field.set(embeddedFtpServer, builtInFtpConfigService);
    }

    @Test
    public void testIsRunning_WhenNotStarted_ReturnsFalse() {
        assertFalse(embeddedFtpServer.isRunning());
    }

    @Test
    public void testStop_WhenNotRunning_DoesNotThrow() {
        embeddedFtpServer.stop();
    }

    @Test
    public void testGetConnectedClients_ReturnsZero() {
        assertEquals(0, embeddedFtpServer.getConnectedClients());
    }

    @Test
    public void testStart_WhenDisabled_ReturnsFalse() {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setEnabled(false);
        config.setPort(2021);
        config.setUsername("user");
        config.setPassword("pass");
        config.setRootDirectory("/tmp/ftp");

        when(builtInFtpConfigService.getConfig()).thenReturn(config);

        boolean result = embeddedFtpServer.start();

        assertFalse(result);
    }
}