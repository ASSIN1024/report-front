package com.report.controller;

import com.report.common.result.Result;
import com.report.entity.LoginUser;
import com.report.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 * 提供用户登录、登出、获取当前用户等认证相关接口
 *
 * @author Report System
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return 登录用户信息
     */
    @PostMapping("/login")
    public Result<LoginUser> login(@RequestParam String username,
                                   @RequestParam String password) {
        try {
            LoginUser loginUser = authService.login(username, password);
            return Result.success("登录成功", loginUser);
        } catch (Exception e) {
            return Result.fail(400, e.getMessage());
        }
    }

    /**
     * 用户登出
     *
     * @return 操作结果
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("已退出登录", null);
    }

    /**
     * 获取当前登录用户
     *
     * @return 当前用户信息
     */
    @GetMapping("/current-user")
    public Result<LoginUser> getCurrentUser() {
        LoginUser loginUser = authService.getCurrentUser();
        if (loginUser == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(loginUser);
    }

    /**
     * 获取CSRF Token
     *
     * @return CSRF Token
     */
    @GetMapping("/csrf-token")
    public Result<String> getCsrfToken() {
        return Result.success(authService.generateCsrfToken());
    }
}
