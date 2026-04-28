package com.report.service.impl;

import com.report.entity.LoginUser;
import com.report.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String SESSION_USER_KEY = "loginUser";
    private static final String SESSION_CSRF_KEY = "csrfToken";

    @Value("${auth.admin.username:admin}")
    private String adminUsername;

    @Value("${auth.admin.password:admin123}")
    private String adminPassword;

    private final HttpSession session;

    public AuthServiceImpl(HttpSession session) {
        this.session = session;
    }

    @Override
    public LoginUser login(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        if (!adminUsername.equals(username.trim()) || !adminPassword.equals(password)) {
            logger.warn("登录失败，用户名或密码错误: {}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(1L);
        loginUser.setUsername(username.trim());
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setCsrfToken(generateCsrfToken());

        session.setAttribute(SESSION_USER_KEY, loginUser);

        logger.info("用户登录成功: {}", username);
        return loginUser;
    }

    @Override
    public void logout() {
        LoginUser currentUser = getCurrentUser();
        if (currentUser != null) {
            logger.info("用户登出: {}", currentUser.getUsername());
        }
        session.removeAttribute(SESSION_USER_KEY);
        session.removeAttribute(SESSION_CSRF_KEY);
        session.invalidate();
    }

    @Override
    public LoginUser getCurrentUser() {
        Object userObj = session.getAttribute(SESSION_USER_KEY);
        if (userObj instanceof LoginUser) {
            return (LoginUser) userObj;
        }
        return null;
    }

    @Override
    public String generateCsrfToken() {
        String csrfToken = UUID.randomUUID().toString();
        session.setAttribute(SESSION_CSRF_KEY, csrfToken);
        return csrfToken;
    }
}