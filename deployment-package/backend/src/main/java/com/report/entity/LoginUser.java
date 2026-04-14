package com.report.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录用户信息（存Session用）
 */
@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * CSRF Token
     */
    private String csrfToken;

    /**
     * 登录时间戳
     */
    private Long loginTime;
}
