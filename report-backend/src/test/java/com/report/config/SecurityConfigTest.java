package com.report.config;

import com.report.filter.AuthenticationFilter;
import com.report.filter.CsrfFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * SecurityConfig 测试类
 * 测试FilterRegistrationBean的配置是否正确
 *
 * @author Report System
 * @since 2026-04-09
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {SecurityConfig.class, AuthenticationFilter.class, CsrfFilter.class})
public class SecurityConfigTest {

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration;

    @Autowired
    private FilterRegistrationBean<CsrfFilter> csrfFilterRegistration;

    /**
     * 测试AuthenticationFilter的FilterRegistrationBean是否正确创建
     */
    @Test
    public void testAuthenticationFilterRegistrationNotNull() {
        assertNotNull("AuthenticationFilterRegistration should not be null",
                authenticationFilterRegistration);
    }

    /**
     * 测试CsrfFilter的FilterRegistrationBean是否正确创建
     */
    @Test
    public void testCsrfFilterRegistrationNotNull() {
        assertNotNull("CsrfFilterRegistration should not be null",
                csrfFilterRegistration);
    }

    /**
     * 测试AuthenticationFilter的URL Pattern是否正确
     */
    @Test
    public void testAuthenticationFilterUrlPatterns() {
        assertNotNull("AuthenticationFilterRegistration URL patterns should not be null",
                authenticationFilterRegistration.getUrlPatterns());
        assertTrue("AuthenticationFilterRegistration should have /api/* pattern",
                authenticationFilterRegistration.getUrlPatterns().contains("/api/*"));
    }

    /**
     * 测试CsrfFilter的URL Pattern是否正确
     */
    @Test
    public void testCsrfFilterUrlPatterns() {
        assertNotNull("CsrfFilterRegistration URL patterns should not be null",
                csrfFilterRegistration.getUrlPatterns());
        assertTrue("CsrfFilterRegistration should have /api/* pattern",
                csrfFilterRegistration.getUrlPatterns().contains("/api/*"));
    }

    /**
     * 测试AuthenticationFilter的Order是否正确
     */
    @Test
    public void testAuthenticationFilterOrder() {
        assertEquals("AuthenticationFilterRegistration order should be 1",
                1, authenticationFilterRegistration.getOrder());
    }

    /**
     * 测试CsrfFilter的Order是否正确
     */
    @Test
    public void testCsrfFilterOrder() {
        assertEquals("CsrfFilterRegistration order should be 2",
                2, csrfFilterRegistration.getOrder());
    }

    /**
     * 测试AuthenticationFilter是否正确注入到FilterRegistrationBean
     */
    @Test
    public void testAuthenticationFilterInjected() {
        assertNotNull("AuthenticationFilter should be injected",
                authenticationFilterRegistration.getFilter());
        assertTrue("Filter should be instance of AuthenticationFilter",
                authenticationFilterRegistration.getFilter() instanceof AuthenticationFilter);
    }

    /**
     * 测试CsrfFilter是否正确注入到FilterRegistrationBean
     */
    @Test
    public void testCsrfFilterInjected() {
        assertNotNull("CsrfFilter should be injected",
                csrfFilterRegistration.getFilter());
        assertTrue("Filter should be instance of CsrfFilter",
                csrfFilterRegistration.getFilter() instanceof CsrfFilter);
    }
}
