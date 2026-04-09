package com.report.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.entity.LoginUser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpSession;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * AuthenticationFilter 单元测试
 *
 * @author Report System
 * @since 2026-04-09
 */
@RunWith(MockitoJUnitRunner.class)
public class AuthenticationFilterTest {

    private AuthenticationFilter authenticationFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        authenticationFilter = new AuthenticationFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void testWhiteListPath_Login_ShouldPass() throws Exception {
        // 测试登录接口白名单
        request.setRequestURI("/api/auth/login");
        request.setMethod("POST");

        authenticationFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testWhiteListPath_CsrfToken_ShouldPass() throws Exception {
        // 测试CSRF Token接口白名单
        request.setRequestURI("/api/auth/csrf-token");
        request.setMethod("GET");

        authenticationFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testLoggedInUser_ShouldPass() throws Exception {
        // 测试已登录用户访问受保护资源
        request.setRequestURI("/api/data/list");
        request.setMethod("GET");

        // 创建Session并设置登录用户
        MockHttpSession session = new MockHttpSession();
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername("testuser");
        loginUser.setLoginTime(System.currentTimeMillis());
        session.setAttribute("loginUser", loginUser);
        request.setSession(session);

        authenticationFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testNotLoggedInUser_ShouldReturn401() throws Exception {
        // 测试未登录用户访问受保护资源
        request.setRequestURI("/api/data/list");
        request.setMethod("GET");

        // 不设置Session，模拟未登录状态

        authenticationFilter.doFilter(request, response, filterChain);

        // 验证返回401
        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        // 验证响应内容
        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("\"code\":401"));
        assertTrue(responseBody.contains("\"message\":\"请先登录\""));

        // 验证过滤器链未被调用（拦截）
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testNotLoggedInUser_SessionWithoutUser_ShouldReturn401() throws Exception {
        // 测试Session存在但没有用户信息的情况
        request.setRequestURI("/api/data/list");
        request.setMethod("GET");

        // 创建Session但不设置登录用户
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        authenticationFilter.doFilter(request, response, filterChain);

        // 验证返回401
        assertEquals(401, response.getStatus());

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("\"code\":401"));
        assertTrue(responseBody.contains("\"message\":\"请先登录\""));

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testNonApiPath_ShouldPass() throws Exception {
        // 测试非/api路径应该放行
        request.setRequestURI("/static/index.html");
        request.setMethod("GET");

        authenticationFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }
}
