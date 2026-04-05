package com.report.ftp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.ftp.BuiltInFtpConfig;
import com.report.ftp.BuiltInFtpConfigService;
import com.report.ftp.EmbeddedFtpServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(MockitoJUnitRunner.class)
public class BuiltInFtpConfigControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BuiltInFtpConfigService builtInFtpConfigService;

    @Mock
    private EmbeddedFtpServer embeddedFtpServer;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        BuiltInFtpConfigController controller = new BuiltInFtpConfigController();

        Field serviceField = BuiltInFtpConfigController.class.getDeclaredField("builtInFtpConfigService");
        serviceField.setAccessible(true);
        serviceField.set(controller, builtInFtpConfigService);

        Field serverField = BuiltInFtpConfigController.class.getDeclaredField("embeddedFtpServer");
        serverField.setAccessible(true);
        serverField.set(controller, embeddedFtpServer);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testGetConfig_ReturnsConfig() throws Exception {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setId(1L);
        config.setEnabled(true);
        config.setPort(2021);
        config.setUsername("test_user");

        when(builtInFtpConfigService.getConfig()).thenReturn(config);

        mockMvc.perform(get("/api/built-in-ftp/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.port").value(2021))
                .andExpect(jsonPath("$.data.username").value("test_user"));

        verify(builtInFtpConfigService).getConfig();
    }

    @Test
    public void testStart_WhenNotRunning_ReturnsSuccess() throws Exception {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setId(1L);
        config.setEnabled(true);
        config.setPort(2021);

        when(embeddedFtpServer.start()).thenReturn(true);
        when(embeddedFtpServer.getConnectedClients()).thenReturn(0);
        when(builtInFtpConfigService.getConfig()).thenReturn(config);

        mockMvc.perform(post("/api/built-in-ftp/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(true))
                .andExpect(jsonPath("$.data.port").value(2021))
                .andExpect(jsonPath("$.data.connectedClients").value(0));

        verify(embeddedFtpServer).start();
    }

    @Test
    public void testStart_WhenFails_ReturnsFail() throws Exception {
        when(embeddedFtpServer.start()).thenReturn(false);

        mockMvc.perform(post("/api/built-in-ftp/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    public void testStop_ReturnsSuccess() throws Exception {
        mockMvc.perform(post("/api/built-in-ftp/stop"))
                .andExpect(status().isOk());

        verify(embeddedFtpServer).stop();
    }

    @Test
    public void testStatus_WhenRunning_ReturnsRunningStatus() throws Exception {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setPort(2021);

        when(embeddedFtpServer.isRunning()).thenReturn(true);
        when(embeddedFtpServer.getConnectedClients()).thenReturn(5);
        when(builtInFtpConfigService.getConfig()).thenReturn(config);

        mockMvc.perform(get("/api/built-in-ftp/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(true))
                .andExpect(jsonPath("$.data.port").value(2021))
                .andExpect(jsonPath("$.data.connectedClients").value(5));
    }

    @Test
    public void testStatus_WhenNotRunning_ReturnsNotRunningStatus() throws Exception {
        when(embeddedFtpServer.isRunning()).thenReturn(false);

        mockMvc.perform(get("/api/built-in-ftp/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.running").value(false));
    }

    @Test
    public void testUpdateConfig_WhenServerNotRunning_ReturnsSuccess() throws Exception {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setUsername("new_user");
        config.setPassword("new_pass");

        when(embeddedFtpServer.isRunning()).thenReturn(false);

        mockMvc.perform(put("/api/built-in-ftp/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk());

        verify(builtInFtpConfigService).updateConfig(any(BuiltInFtpConfig.class));
    }

    @Test
    public void testUpdateConfig_WhenServerRunning_ReturnsFail() throws Exception {
        BuiltInFtpConfig config = new BuiltInFtpConfig();
        config.setUsername("new_user");

        when(embeddedFtpServer.isRunning()).thenReturn(true);

        mockMvc.perform(put("/api/built-in-ftp/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(config)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(builtInFtpConfigService, never()).updateConfig(any(BuiltInFtpConfig.class));
    }
}