package com.report.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.common.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * CSRF过滤器
 * 拦截POST/PUT/DELETE请求，验证CSRF Token
 *
 * @author Report System
 * @since 2026-04-09
 */
@Component
@Order(2)
public class CsrfFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(CsrfFilter.class);

    private static final String SESSION_CSRF_KEY = "csrfToken";
    private static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    /**
     * 安全的HTTP方法（不需要CSRF验证）
     */
    private static final Set<String> SAFE_METHODS = new HashSet<>(Arrays.asList(
            "GET", "HEAD", "OPTIONS"
    ));

    private static final Set<String> CSRF_EXEMPT_PATHS = new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/register"
    ));

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("CsrfFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();

        logger.debug("CsrfFilter processing: {} {}", method, requestURI);

        // 检查是否是安全的HTTP方法
        if (SAFE_METHODS.contains(method.toUpperCase())) {
            logger.debug("Safe method {}, allowing: {}", method, requestURI);
            chain.doFilter(request, response);
            return;
        }

        // 检查是否是CSRF豁免路径（登录、注册等）
        if (CSRF_EXEMPT_PATHS.stream().anyMatch(requestURI::startsWith)) {
            logger.debug("CSRF exempt path, allowing: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        // 需要CSRF验证的方法（POST, PUT, DELETE等）
        if (!validateCsrfToken(httpRequest)) {
            logger.warn("CSRF validation failed for {} {}", method, requestURI);
            sendForbiddenResponse(httpResponse);
            return;
        }

        logger.debug("CSRF validation passed for {} {}", method, requestURI);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("CsrfFilter destroyed");
    }

    /**
     * 验证CSRF Token
     *
     * @param request HTTP请求
     * @return 验证是否通过
     */
    private boolean validateCsrfToken(HttpServletRequest request) {
        // 获取Session中的CSRF Token
        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.debug("No session found");
            return false;
        }

        Object sessionToken = session.getAttribute(SESSION_CSRF_KEY);
        if (sessionToken == null) {
            logger.debug("No CSRF token in session");
            return false;
        }

        // 获取请求头中的CSRF Token
        String requestToken = request.getHeader(CSRF_HEADER_NAME);
        if (requestToken == null || requestToken.isEmpty()) {
            logger.debug("No CSRF token in request header");
            return false;
        }

        // 比较Token
        boolean valid = sessionToken.toString().equals(requestToken);
        if (!valid) {
            logger.debug("CSRF token mismatch: session={}, request={}", sessionToken, requestToken);
        }

        return valid;
    }

    /**
     * 发送禁止访问响应
     *
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    private void sendForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.fail(403, "CSRF验证失败");
        String jsonResponse = objectMapper.writeValueAsString(result);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
