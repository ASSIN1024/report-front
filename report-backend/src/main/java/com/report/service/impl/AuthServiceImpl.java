package com.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.LoginUser;
import com.report.entity.SysUser;
import com.report.mapper.SysUserMapper;
import com.report.service.AuthService;
import com.report.util.PasswordEncoderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.UUID;

/**
 * 认证服务实现类
 *
 * @author Report System
 * @since 2026-04-09
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String SESSION_USER_KEY = "loginUser";
    private static final String SESSION_CSRF_KEY = "csrfToken";

    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "admin123";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private HttpSession session;

    /**
     * 设置Session（用于测试注入）
     *
     * @param session HttpSession对象
     */
    public void setSession(HttpSession session) {
        this.session = session;
    }

    /**
     * 初始化默认用户
     * 如果数据库中不存在admin用户，则创建默认管理员账号
     */
    @PostConstruct
    public void initDefaultUser() {
        try {
            // 查询是否存在admin用户
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUser::getUsername, DEFAULT_USERNAME);
            SysUser existUser = sysUserMapper.selectOne(wrapper);

            if (existUser == null) {
                // 创建默认用户
                SysUser defaultUser = new SysUser();
                defaultUser.setUsername(DEFAULT_USERNAME);
                defaultUser.setPassword(PasswordEncoderUtil.encode(DEFAULT_PASSWORD));
                defaultUser.setCreateTime(new Date());

                sysUserMapper.insert(defaultUser);
                logger.info("默认管理员账号创建成功: {}", DEFAULT_USERNAME);
            } else {
                logger.info("默认管理员账号已存在: {}", DEFAULT_USERNAME);
            }
        } catch (Exception e) {
            logger.error("初始化默认用户失败", e);
        }
    }

    @Override
    public LoginUser login(String username, String password) {
        // 参数校验
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username.trim());
        SysUser user = sysUserMapper.selectOne(wrapper);

        // 验证用户是否存在
        if (user == null) {
            logger.warn("登录失败，用户不存在: {}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 验证密码
        if (!PasswordEncoderUtil.matches(password, user.getPassword())) {
            logger.warn("登录失败，密码错误: {}", username);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        // 创建登录用户信息
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setCsrfToken(generateCsrfToken());

        // 存入Session
        session.setAttribute(SESSION_USER_KEY, loginUser);

        // 更新最后登录时间
        user.setLastLoginTime(new Date());
        sysUserMapper.updateById(user);

        logger.info("用户登录成功: {}", username);
        return loginUser;
    }

    @Override
    public void logout() {
        LoginUser currentUser = getCurrentUser();
        if (currentUser != null) {
            logger.info("用户登出: {}", currentUser.getUsername());
        }

        // 清除Session
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
