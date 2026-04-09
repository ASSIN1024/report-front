package com.report.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.common.result.Result;
import com.report.entity.LoginUser;
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
import java.util.List;

/**
 * 认证过滤器
 * 拦截所有/api/*请求，检查Session中是否有LoginUser
 *
 * @author Report System
 * @since 2026-04-09
 */
@Component
@Order(1)
public class AuthenticationFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private static final String SESSION_USER_KEY = "loginUser";

    /**
     * 白名单路径列表（不需要认证的路径）
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/csrf-token"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("AuthenticationFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        logger.debug("AuthenticationFilter processing: {} {}", method, requestURI);

        // 检查是否是白名单路径
        if (isWhiteListPath(requestURI)) {
            logger.debug("White list path, allowing: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        // 检查是否是/api路径
        if (!requestURI.startsWith("/api/")) {
            logger.debug("Non-API path, allowing: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        // 检查Session中是否有登录用户
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            logger.warn("No session found for protected path: {}", requestURI);
            sendUnauthorizedResponse(httpResponse);
            return;
        }

        Object userObj = session.getAttribute(SESSION_USER_KEY);
        if (!(userObj instanceof LoginUser)) {
            logger.warn("No login user in session for protected path: {}", requestURI);
            sendUnauthorizedResponse(httpResponse);
            return;
        }

        LoginUser loginUser = (LoginUser) userObj;
        logger.debug("User authenticated: {}", loginUser.getUsername());

        // 已登录，放行
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        logger.info("AuthenticationFilter destroyed");
    }

    /**
     * 检查是否是白名单路径
     *
     * @param requestURI 请求URI
     * @return 是否是白名单路径
     */
    private boolean isWhiteListPath(String requestURI) {
        return WHITE_LIST.contains(requestURI);
    }

    /**
     * 发送未授权响应
     *
     * @param response HTTP响应
     * @throws IOException IO异常
     */
    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Result<Void> result = Result.fail(401, "请先登录");
        String jsonResponse = objectMapper.writeValueAsString(result);

        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
}
