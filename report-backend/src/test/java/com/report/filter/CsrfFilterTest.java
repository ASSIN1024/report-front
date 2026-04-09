package com.report.filter;

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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * CsrfFilter 单元测试
 *
 * @author Report System
 * @since 2026-04-09
 */
@RunWith(MockitoJUnitRunner.class)
public class CsrfFilterTest {

    private CsrfFilter csrfFilter;

    @Mock
    private FilterChain filterChain;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @Before
    public void setUp() {
        csrfFilter = new CsrfFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void testGetRequest_ShouldPass() throws Exception {
        // 测试GET请求应该放行
        request.setRequestURI("/api/data/list");
        request.setMethod("GET");

        csrfFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testHeadRequest_ShouldPass() throws Exception {
        // 测试HEAD请求应该放行
        request.setRequestURI("/api/data/list");
        request.setMethod("HEAD");

        csrfFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testOptionsRequest_ShouldPass() throws Exception {
        // 测试OPTIONS请求应该放行
        request.setRequestURI("/api/data/list");
        request.setMethod("OPTIONS");

        csrfFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPostRequest_ValidToken_ShouldPass() throws Exception {
        // 测试POST请求Token验证成功
        request.setRequestURI("/api/data/save");
        request.setMethod("POST");

        // 创建Session并设置CSRF Token
        MockHttpSession session = new MockHttpSession();
        String csrfToken = "test-csrf-token-12345";
        session.setAttribute("csrfToken", csrfToken);
        request.setSession(session);

        // 设置请求头中的CSRF Token
        request.addHeader("X-CSRF-Token", csrfToken);

        csrfFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPutRequest_ValidToken_ShouldPass() throws Exception {
        // 测试PUT请求Token验证成功
        request.setRequestURI("/api/data/update");
        request.setMethod("PUT");

        // 创建Session并设置CSRF Token
        MockHttpSession session = new MockHttpSession();
        String csrfToken = "test-csrf-token-12345";
        session.setAttribute("csrfToken", csrfToken);
        request.setSession(session);

        // 设置请求头中的CSRF Token
        request.addHeader("X-CSRF-Token", csrfToken);

        csrfFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteRequest_ValidToken_ShouldPass() throws Exception {
        // 测试DELETE请求Token验证成功
        request.setRequestURI("/api/data/delete");
        request.setMethod("DELETE");

        // 创建Session并设置CSRF Token
        MockHttpSession session = new MockHttpSession();
        String csrfToken = "test-csrf-token-12345";
        session.setAttribute("csrfToken", csrfToken);
        request.setSession(session);

        // 设置请求头中的CSRF Token
        request.addHeader("X-CSRF-Token", csrfToken);

        csrfFilter.doFilter(request, response, filterChain);

        // 验证过滤器链被调用（放行）
        verify(filterChain).doFilter(request, response);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPostRequest_InvalidToken_ShouldReturn403() throws Exception {
        // 测试POST请求Token验证失败
        request.setRequestURI("/api/data/save");
        request.setMethod("POST");

        // 创建Session并设置CSRF Token
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("csrfToken", "correct-token");
        request.setSession(session);

        // 设置错误的CSRF Token
        request.addHeader("X-CSRF-Token", "wrong-token");

        csrfFilter.doFilter(request, response, filterChain);

        // 验证返回403
        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());

        // 验证响应内容
        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("\"code\":403"));
        assertTrue(responseBody.contains("\"message\":\"CSRF验证失败\""));

        // 验证过滤器链未被调用（拦截）
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testPostRequest_MissingToken_ShouldReturn403() throws Exception {
        // 测试POST请求缺少Token
        request.setRequestURI("/api/data/save");
        request.setMethod("POST");

        // 创建Session并设置CSRF Token
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("csrfToken", "correct-token");
        request.setSession(session);

        // 不设置请求头中的CSRF Token

        csrfFilter.doFilter(request, response, filterChain);

        // 验证返回403
        assertEquals(403, response.getStatus());

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("\"code\":403"));
        assertTrue(responseBody.contains("\"message\":\"CSRF验证失败\""));

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testPostRequest_NoSession_ShouldReturn403() throws Exception {
        // 测试POST请求没有Session
        request.setRequestURI("/api/data/save");
        request.setMethod("POST");

        // 不创建Session
        request.addHeader("X-CSRF-Token", "some-token");

        csrfFilter.doFilter(request, response, filterChain);

        // 验证返回403
        assertEquals(403, response.getStatus());

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("\"code\":403"));
        assertTrue(responseBody.contains("\"message\":\"CSRF验证失败\""));

        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    public void testPostRequest_SessionWithoutToken_ShouldReturn403() throws Exception {
        // 测试POST请求Session中没有Token
        request.setRequestURI("/api/data/save");
        request.setMethod("POST");

        // 创建Session但不设置CSRF Token
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        request.addHeader("X-CSRF-Token", "some-token");

        csrfFilter.doFilter(request, response, filterChain);

        // 验证返回403
        assertEquals(403, response.getStatus());

        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("\"code\":403"));
        assertTrue(responseBody.contains("\"message\":\"CSRF验证失败\""));

        verify(filterChain, never()).doFilter(request, response);
    }
}
