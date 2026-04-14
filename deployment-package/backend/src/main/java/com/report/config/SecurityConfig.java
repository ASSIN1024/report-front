package com.report.config;

import com.report.filter.AuthenticationFilter;
import com.report.filter.CsrfFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全配置类
 * 注册安全相关的Filter到Spring容器
 *
 * @author Report System
 * @since 2026-04-09
 */
@Configuration
public class SecurityConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Autowired
    private CsrfFilter csrfFilter;

    /**
     * 注册AuthenticationFilter
     * Order=1，优先级最高
     *
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authenticationFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * 注册CsrfFilter
     * Order=2，在AuthenticationFilter之后执行
     *
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration() {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(csrfFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2);
        return registration;
    }
}
