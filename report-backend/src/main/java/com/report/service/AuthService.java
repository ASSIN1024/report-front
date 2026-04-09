package com.report.service;

import com.report.entity.LoginUser;

/**
 * 认证服务接口
 * 提供用户登录、登出、获取当前用户等认证相关功能
 *
 * @author Report System
 * @since 2026-04-09
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录用户信息
     * @throws IllegalArgumentException 当用户名或密码为空、用户不存在或密码错误时抛出
     */
    LoginUser login(String username, String password);

    /**
     * 用户登出
     * 清除Session中的用户信息
     */
    void logout();

    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户信息，未登录时返回null
     */
    LoginUser getCurrentUser();

    /**
     * 生成CSRF Token
     * 生成UUID格式的Token并存入Session
     *
     * @return CSRF Token
     */
    String generateCsrfToken();
}
