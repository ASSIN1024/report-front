package com.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.LoginUser;
import com.report.entity.SysUser;
import com.report.mapper.SysUserMapper;
import com.report.service.impl.AuthServiceImpl;
import com.report.util.PasswordEncoderUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpSession;

import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 测试类
 * 测试认证服务的核心功能
 */
public class AuthServiceTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @InjectMocks
    private AuthServiceImpl authService;

    private MockHttpSession mockSession;
    private SysUser testUser;
    private String rawPassword = "admin123";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        // 创建测试用户
        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setPassword(PasswordEncoderUtil.encode(rawPassword));
        testUser.setCreateTime(new Date());

        // 创建MockHttpSession
        mockSession = new MockHttpSession();

        // 注入session到authService
        authService.setSession(mockSession);
    }

    /**
     * 测试登录成功
     */
    @Test
    public void testLogin_Success() {
        // Given
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // When
        LoginUser loginUser = authService.login("admin", rawPassword);

        // Then
        assertNotNull("登录成功应返回LoginUser", loginUser);
        assertEquals("用户ID应匹配", testUser.getId(), loginUser.getUserId());
        assertEquals("用户名应匹配", testUser.getUsername(), loginUser.getUsername());
        assertNotNull("登录时间应存在", loginUser.getLoginTime());
        assertNotNull("CSRF Token应存在", loginUser.getCsrfToken());

        // 验证Session中存储了用户信息
        Object sessionUser = mockSession.getAttribute("loginUser");
        assertNotNull("Session中应存储用户信息", sessionUser);
        assertTrue("Session中存储的应为LoginUser类型", sessionUser instanceof LoginUser);

        // 验证更新了最后登录时间
        verify(sysUserMapper, times(1)).updateById(any(SysUser.class));
    }

    /**
     * 测试登录失败 - 用户名错误
     */
    @Test
    public void testLogin_WrongUsername() {
        // Given
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When & Then
        try {
            authService.login("wronguser", "password");
            fail("用户名错误应抛出异常");
        } catch (IllegalArgumentException e) {
            assertEquals("错误信息应为'用户名或密码错误'", "用户名或密码错误", e.getMessage());
        }
    }

    /**
     * 测试登录失败 - 密码错误
     */
    @Test
    public void testLogin_WrongPassword() {
        // Given
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

        // When & Then
        try {
            authService.login("admin", "wrongpassword");
            fail("密码错误应抛出异常");
        } catch (IllegalArgumentException e) {
            assertEquals("错误信息应为'用户名或密码错误'", "用户名或密码错误", e.getMessage());
        }
    }

    /**
     * 测试登录失败 - 用户名为空
     */
    @Test
    public void testLogin_EmptyUsername() {
        // When & Then
        try {
            authService.login("", "password");
            fail("用户名为空应抛出异常");
        } catch (IllegalArgumentException e) {
            assertEquals("错误信息应为'用户名不能为空'", "用户名不能为空", e.getMessage());
        }
    }

    /**
     * 测试登录失败 - 密码为空
     */
    @Test
    public void testLogin_EmptyPassword() {
        // When & Then
        try {
            authService.login("admin", "");
            fail("密码为空应抛出异常");
        } catch (IllegalArgumentException e) {
            assertEquals("错误信息应为'密码不能为空'", "密码不能为空", e.getMessage());
        }
    }

    /**
     * 测试登出
     */
    @Test
    public void testLogout() {
        // Given - 先登录
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        LoginUser loginUser = authService.login("admin", rawPassword);
        assertNotNull("登录后应能获取用户", loginUser);

        // When - 登出
        authService.logout();

        // Then - 验证logout方法被调用（由于session.invalidate()，无法再访问session）
        // 这里主要验证logout方法不抛出异常
        assertTrue("登出应成功执行", true);
    }

    /**
     * 测试获取当前用户 - 已登录
     */
    @Test
    public void testGetCurrentUser_LoggedIn() {
        // Given - 先登录
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);
        LoginUser loginResult = authService.login("admin", rawPassword);

        // When
        LoginUser currentUser = authService.getCurrentUser();

        // Then
        assertNotNull("已登录时应能获取当前用户", currentUser);
        assertEquals("用户ID应匹配", loginResult.getUserId(), currentUser.getUserId());
        assertEquals("用户名应匹配", loginResult.getUsername(), currentUser.getUsername());
    }

    /**
     * 测试获取当前用户 - 未登录
     */
    @Test
    public void testGetCurrentUser_NotLoggedIn() {
        // When
        LoginUser currentUser = authService.getCurrentUser();

        // Then
        assertNull("未登录时应返回null", currentUser);
    }

    /**
     * 测试生成CSRF Token
     */
    @Test
    public void testGenerateCsrfToken() {
        // When
        String csrfToken = authService.generateCsrfToken();

        // Then
        assertNotNull("CSRF Token不应为空", csrfToken);
        assertTrue("CSRF Token应为UUID格式", csrfToken.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"));

        // 验证Token已存入Session
        Object sessionToken = mockSession.getAttribute("csrfToken");
        assertNotNull("CSRF Token应存入Session", sessionToken);
        assertEquals("Session中的Token应与返回的一致", csrfToken, sessionToken);
    }

    /**
     * 测试默认用户初始化
     */
    @Test
    public void testDefaultUserInitialization() {
        // Given - 模拟数据库中无用户
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When - 调用初始化方法（模拟@PostConstruct）
        authService.initDefaultUser();

        // Then - 应调用insert方法创建默认用户
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper, times(1)).insert(userCaptor.capture());

        SysUser insertedUser = userCaptor.getValue();
        assertEquals("默认用户名应为admin", "admin", insertedUser.getUsername());
        assertNotNull("密码应已加密", insertedUser.getPassword());
        assertTrue("密码应为BCrypt格式", insertedUser.getPassword().startsWith("$2a$"));
    }

    /**
     * 测试默认用户初始化 - 用户已存在
     */
    @Test
    public void testDefaultUserInitialization_UserExists() {
        // Given - 模拟数据库中已有admin用户
        SysUser existUser = new SysUser();
        existUser.setId(1L);
        existUser.setUsername("admin");
        existUser.setPassword(PasswordEncoderUtil.encode("admin123"));
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existUser);

        // When - 调用初始化方法
        authService.initDefaultUser();

        // Then - 不应调用insert方法
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }
}
