package com.report.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.entity.LoginUser;
import com.report.service.AuthService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 单元测试
 *
 * @author Report System
 * @since 2026-04-09
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        AuthController controller = new AuthController();

        // 使用反射注入 mock 对象
        Field serviceField = AuthController.class.getDeclaredField("authService");
        serviceField.setAccessible(true);
        serviceField.set(controller, authService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testLogin_Success() throws Exception {
        // 准备测试数据
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("testuser");
        loginUser.setLoginTime(System.currentTimeMillis());

        when(authService.login("testuser", "password123")).thenReturn(loginUser);

        // 执行测试
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "testuser")
                        .param("password", "password123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(authService).login("testuser", "password123");
    }

    @Test
    public void testLogin_Fail() throws Exception {
        // 模拟登录失败
        when(authService.login(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("用户名或密码错误"));

        // 执行测试
        mockMvc.perform(post("/api/auth/login")
                        .param("username", "wronguser")
                        .param("password", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));

        verify(authService).login("wronguser", "wrongpass");
    }

    @Test
    public void testLogout_Success() throws Exception {
        // 执行测试
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已退出登录"));

        verify(authService).logout();
    }

    @Test
    public void testGetCurrentUser_Success() throws Exception {
        // 准备测试数据
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("testuser");
        loginUser.setLoginTime(System.currentTimeMillis());

        when(authService.getCurrentUser()).thenReturn(loginUser);

        // 执行测试
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(authService).getCurrentUser();
    }

    @Test
    public void testGetCurrentUser_NotLoggedIn() throws Exception {
        // 模拟未登录
        when(authService.getCurrentUser()).thenReturn(null);

        // 执行测试
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("请先登录"));

        verify(authService).getCurrentUser();
    }

    @Test
    public void testGetCsrfToken_Success() throws Exception {
        // 准备测试数据
        String csrfToken = "test-csrf-token-12345";
        when(authService.generateCsrfToken()).thenReturn(csrfToken);

        // 执行测试
        mockMvc.perform(get("/api/auth/csrf-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(csrfToken));

        verify(authService).generateCsrfToken();
    }
}
