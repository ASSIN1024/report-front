# 登录认证系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为报表数据处理平台实现安全的用户登录认证功能，包含Session管理、CSRF防护和简单的访问控制。

**Architecture:** 使用自定义Filter实现Session认证，BCrypt密码加密，CSRF Token防护。前端使用Vue Router导航守卫实现页面访问控制。

**Tech Stack:** Spring Boot 2.1.2, Vue 2.6, MyBatis-Plus, Spring Security Crypto

---

## 文件结构

### 后端新增文件

```
report-backend/src/main/java/com/report/
├── entity/
│   ├── SysUser.java              # 用户实体
│   └── LoginUser.java            # 登录用户信息（存Session用）
├── mapper/
│   └── SysUserMapper.java        # 用户Mapper
├── service/
│   ├── AuthService.java          # 认证服务接口
│   └── impl/AuthServiceImpl.java # 认证服务实现（含默认用户初始化）
├── controller/
│   └── AuthController.java       # 登录/登出/当前用户接口
├── filter/
│   ├── AuthenticationFilter.java # 认证Filter
│   └── CsrfFilter.java            # CSRF防护Filter
├── config/
│   └── SecurityConfig.java       # 安全配置（注册Filter）
└── util/
    └── PasswordEncoderUtil.java  # 密码加密工具
```

### 前端新增/修改文件

```
src/
├── views/login/
│   └── Login.vue                 # 登录页面（新增）
├── api/
│   └── auth.js                   # 认证API封装（新增）
├── utils/
│   └── request.js                # HTTP请求封装（改造）
├── router/
│   └── index.js                  # 路由配置（改造，添加导航守卫）
└── store/
    └── index.js                  # Vuex store（改造，添加user state）
```

---

## Task 1: 后端 - 用户实体和Mapper

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/SysUser.java`
- Create: `report-backend/src/main/java/com/report/entity/LoginUser.java`
- Create: `report-backend/src/main/java/com/report/mapper/SysUserMapper.java`

- [ ] **Step 1: 创建SysUser实体**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private Date lastLoginTime;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- [ ] **Step 2: 创建LoginUser实体（存Session用）**

```java
package com.report.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String csrfToken;
    private Long loginTime;
}
```

- [ ] **Step 3: 创建SysUserMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.SysUser;

public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

- [ ] **Step 4: 提交**

```bash
git add report-backend/src/main/java/com/report/entity/SysUser.java
git add report-backend/src/main/java/com/report/entity/LoginUser.java
git add report-backend/src/main/java/com/report/mapper/SysUserMapper.java
git commit -m "feat(auth): add SysUser entity and mapper"
```

---

## Task 2: 后端 - 密码加密工具

**Files:**
- Create: `report-backend/src/main/java/com/report/util/PasswordEncoderUtil.java`

- [ ] **Step 1: 创建PasswordEncoderUtil**

使用Spring Security的BCryptPasswordEncoder：

```java
package com.report.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
```

需要在pom.xml添加依赖：

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-crypto</artifactId>
    <version>5.2.1.RELEASE</version>
</dependency>
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/util/PasswordEncoderUtil.java
git add report-backend/pom.xml
git commit -m "feat(auth): add BCrypt password encoder utility"
```

---

## Task 3: 后端 - 认证服务

**Files:**
- Create: `report-backend/src/main/java/com/report/service/AuthService.java`
- Create: `report-backend/src/main/java/com/report/service/impl/AuthServiceImpl.java`

- [ ] **Step 1: 创建AuthService接口**

```java
package com.report.service;

import com.report.entity.LoginUser;
import com.report.entity.SysUser;

public interface AuthService {

    LoginUser login(String username, String password);

    void logout();

    LoginUser getCurrentUser();

    String generateCsrfToken();
}
```

- [ ] **Step 2: 创建AuthServiceImpl**

```java
package com.report.service.impl;

import com.report.entity.LoginUser;
import com.report.entity.SysUser;
import com.report.mapper.SysUserMapper;
import com.report.service.AuthService;
import com.report.util.PasswordEncoderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_USER_KEY = "LOGIN_USER";
    private static final String CSRF_TOKEN_KEY = "CSRF_TOKEN";

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private HttpSession httpSession;

    @PostConstruct
    public void initDefaultUser() {
        if (sysUserMapper.selectCount(null) == 0) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(PasswordEncoderUtil.encode("admin123"));
            admin.setCreateTime(new Date());
            sysUserMapper.insert(admin);
        }
    }

    @Override
    public LoginUser login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
        );

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        if (!PasswordEncoderUtil.matches(password, user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setLastLoginTime(new Date());
        sysUserMapper.updateById(user);

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setCsrfToken(UUID.randomUUID().toString());

        httpSession.setAttribute(LOGIN_USER_KEY, loginUser);
        httpSession.setAttribute(CSRF_TOKEN_KEY, loginUser.getCsrfToken());

        return loginUser;
    }

    @Override
    public void logout() {
        httpSession.invalidate();
    }

    @Override
    public LoginUser getCurrentUser() {
        return (LoginUser) httpSession.getAttribute(LOGIN_USER_KEY);
    }

    @Override
    public String generateCsrfToken() {
        String token = UUID.randomUUID().toString();
        httpSession.setAttribute(CSRF_TOKEN_KEY, token);
        LoginUser loginUser = getCurrentUser();
        if (loginUser != null) {
            loginUser.setCsrfToken(token);
        }
        return token;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/service/AuthService.java
git add report-backend/src/main/java/com/report/service/impl/AuthServiceImpl.java
git commit -m "feat(auth): add AuthService with login/logout and default user init"
```

---

## Task 4: 后端 - 认证Controller

**Files:**
- Create: `report-backend/src/main/java/com/report/controller/AuthController.java`

- [ ] **Step 1: 创建AuthController**

```java
package com.report.controller;

import com.report.common.result.Result;
import com.report.entity.LoginUser;
import com.report.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public Result<LoginUser> login(@RequestParam String username,
                                   @RequestParam String password,
                                   HttpSession session) {
        try {
            LoginUser loginUser = authService.login(username, password);
            return Result.success("登录成功", loginUser);
        } catch (RuntimeException e) {
            return Result.fail(400, e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        authService.logout();
        return Result.success("已退出登录", null);
    }

    @GetMapping("/current-user")
    public Result<LoginUser> getCurrentUser() {
        LoginUser loginUser = authService.getCurrentUser();
        if (loginUser == null) {
            return Result.fail(401, "请先登录");
        }
        return Result.success(loginUser);
    }

    @GetMapping("/csrf-token")
    public Result<String> getCsrfToken() {
        return Result.success(authService.generateCsrfToken());
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/controller/AuthController.java
git commit -m "feat(auth): add AuthController for login/logout/csrf-token endpoints"
```

---

## Task 5: 后端 - Filter认证和CSRF防护

**Files:**
- Create: `report-backend/src/main/java/com/report/filter/AuthenticationFilter.java`
- Create: `report-backend/src/main/java/com/report/filter/CsrfFilter.java`

- [ ] **Step 1: 创建AuthenticationFilter**

```java
package com.report.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.common.result.Result;
import com.report.entity.LoginUser;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Component
public class AuthenticationFilter implements Filter {

    private static final String LOGIN_USER_KEY = "LOGIN_USER";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        if (path.equals("/api/auth/login") || path.equals("/api/auth/csrf-token")) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            sendUnauthorized(httpResponse, "请先登录");
            return;
        }

        LoginUser loginUser = (LoginUser) session.getAttribute(LOGIN_USER_KEY);
        if (loginUser == null) {
            sendUnauthorized(httpResponse, "请先登录");
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.fail(401, message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
```

- [ ] **Step 2: 创建CsrfFilter**

```java
package com.report.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.common.result.Result;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class CsrfFilter implements Filter {

    private static final String CSRF_TOKEN_KEY = "CSRF_TOKEN";
    private static final List<String> SAFE_METHODS = Arrays.asList("GET", "HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String method = httpRequest.getMethod();

        if (SAFE_METHODS.contains(method)) {
            chain.doFilter(request, response);
            return;
        }

        String requestToken = httpRequest.getHeader("X-CSRF-Token");
        if (requestToken == null || requestToken.isEmpty()) {
            sendForbidden(httpResponse, "CSRF验证失败");
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            sendForbidden(httpResponse, "CSRF验证失败");
            return;
        }

        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_KEY);
        if (sessionToken == null || !sessionToken.equals(requestToken)) {
            sendForbidden(httpResponse, "CSRF验证失败");
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        Result<Object> result = Result.fail(403, message);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/filter/AuthenticationFilter.java
git add report-backend/src/main/java/com/report/filter/CsrfFilter.java
git commit -m "feat(auth): add AuthenticationFilter and CsrfFilter"
```

---

## Task 6: 后端 - 注册Filter

**Files:**
- Create: `report-backend/src/main/java/com/report/config/SecurityConfig.java`
- Modify: `report-backend/src/main/java/com/report/common/config/WebMvcConfig.java`

- [ ] **Step 1: 创建SecurityConfig**

```java
package com.report.config;

import com.report.filter.AuthenticationFilter;
import com.report.filter.CsrfFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Autowired
    private CsrfFilter csrfFilter;

    @Bean
    public FilterRegistrationBean<AuthenticationFilter> authenticationFilterRegistration() {
        FilterRegistrationBean<AuthenticationFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(authenticationFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CsrfFilter> csrfFilterRegistration() {
        FilterRegistrationBean<CsrfFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(csrfFilter);
        registration.addUrlPatterns("/api/*");
        registration.setOrder(2);
        return registration;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/config/SecurityConfig.java
git commit -m "feat(auth): add SecurityConfig to register auth filters"
```

---

## Task 7: 前端 - 登录页面

**Files:**
- Create: `src/views/login/Login.vue`

- [ ] **Step 1: 创建Login.vue**

```vue
<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="login-title">报表数据处理平台</h2>
      <el-form :model="loginForm" :rules="rules" ref="loginForm">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="el-icon-user"
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            prefix-icon="el-icon-lock"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { login, getCurrentUser } from '@/api/auth';

export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      loading: false
    };
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return;

        this.loading = true;
        login(this.loginForm.username, this.loginForm.password)
          .then(res => {
            if (res.code === 200) {
              this.$store.dispatch('setUser', res.data);
              localStorage.setItem('csrfToken', res.data.csrfToken);
              this.$message.success('登录成功');
              this.$router.push('/');
            } else {
              this.$message.error(res.message || '登录失败');
            }
          })
          .catch(err => {
            this.$message.error(err.message || '登录失败');
          })
          .finally(() => {
            this.loading = false;
          });
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 20px;
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #333;
}

.login-button {
  width: 100%;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add src/views/login/Login.vue
git commit -m "feat(auth): add Login.vue page"
```

---

## Task 8: 前端 - API封装

**Files:**
- Create: `src/api/auth.js`
- Create: `src/utils/request.js`

- [ ] **Step 1: 创建auth.js API**

```javascript
import request from '@/utils/request';

export function login(username, password) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data: { username, password }
  });
}

export function logout() {
  return request({
    url: '/api/auth/logout',
    method: 'post'
  });
}

export function getCurrentUser() {
  return request({
    url: '/api/auth/current-user',
    method: 'get'
  });
}

export function getCsrfToken() {
  return request({
    url: '/api/auth/csrf-token',
    method: 'get'
  });
}
```

- [ ] **Step 2: 创建request.js（改造现有axios封装）**

检查是否存在 `src/utils/request.js`，如果不存在则创建：

```javascript
import axios from 'axios';
import { Message } from 'element-ui';

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API || '',
  timeout: 30000
});

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('csrfToken');
    if (token) {
      config.headers['X-CSRF-Token'] = token;
    }
    return config;
  },
  error => {
    console.error('Request error:', error);
    return Promise.reject(error);
  }
);

service.interceptors.response.use(
  response => {
    const res = response.data;
    if (res.code !== 200) {
      if (res.code === 401) {
        Message.error('请先登录');
        localStorage.removeItem('user');
        localStorage.removeItem('csrfToken');
        window.location.href = '/#/login';
      } else {
        Message.error(res.message || '请求失败');
      }
      return Promise.reject(new Error(res.message || '请求失败'));
    }
    return res;
  },
  error => {
    if (error.response && error.response.status === 401) {
      Message.error('请先登录');
      localStorage.removeItem('user');
      localStorage.removeItem('csrfToken');
      window.location.href = '/#/login';
    } else {
      Message.error(error.message || '网络错误');
    }
    return Promise.reject(error);
  }
);

export default service;
```

如果已存在 `src/utils/request.js`，在现有基础上添加CSRF Token头即可。

- [ ] **Step 3: 提交**

```bash
git add src/api/auth.js
git add src/utils/request.js
git commit -m "feat(auth): add auth API and update request interceptor for CSRF"
```

---

## Task 9: 前端 - 路由守卫和Store改造

**Files:**
- Modify: `src/router/index.js`
- Modify: `src/store/index.js`

- [ ] **Step 1: 改造router/index.js**

在路由配置中添加导航守卫：

```javascript
import Vue from 'vue';
import VueRouter from 'vue-router';
import store from '@/store';

Vue.use(VueRouter);

const routes = [
  {
    path: '/',
    redirect: '/ftp'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue')
  },
  {
    path: '/data-center',
    name: 'DataCenter',
    component: () => import('@/views/data-center/Index.vue')
  },
  {
    path: '/ftp',
    name: 'FtpConfig',
    component: () => import('@/views/ftp/FtpConfig.vue')
  },
  {
    path: '/report',
    name: 'ReportList',
    component: () => import('@/views/report/ReportList.vue')
  },
  {
    path: '/report/config/:id?',
    name: 'ReportConfig',
    component: () => import('@/views/report/components/ReportConfig.vue'),
    props: true
  },
  {
    path: '/task',
    name: 'TaskMonitor',
    component: () => import('@/views/task/TaskMonitor.vue')
  },
  {
    path: '/trigger-monitor',
    name: 'TriggerMonitor',
    component: () => import('@/views/trigger/TriggerMonitor.vue')
  },
  {
    path: '/log',
    name: 'LogList',
    component: () => import('@/views/log/LogList.vue')
  },
  {
    path: '/operation-log',
    name: 'OperationLog',
    component: () => import('@/views/log/OperationLog.vue')
  },
  {
    path: '/system-log',
    name: 'SystemLog',
    component: () => import('@/views/log/SystemLog.vue')
  }
];

const router = new VueRouter({
  mode: 'hash',
  routes
});

router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next();
  } else if (!store.state.user) {
    next('/login');
  } else {
    next();
  }
});

export default router;
```

- [ ] **Step 2: 改造store/index.js**

```javascript
import Vue from 'vue';
import Vuex from 'vuex';

Vue.use(Vuex);

export default new Vuex.Store({
  state: {
    user: JSON.parse(localStorage.getItem('user') || 'null')
  },
  mutations: {
    setUser(state, user) {
      state.user = user;
      if (user) {
        localStorage.setItem('user', JSON.stringify(user));
      } else {
        localStorage.removeItem('user');
      }
    }
  },
  actions: {
    setUser({ commit }, user) {
      commit('setUser', user);
    }
  },
  modules: {}
});
```

- [ ] **Step 3: 提交**

```bash
git add src/router/index.js
git add src/store/index.js
git commit -m "feat(auth): add router guard and update Vuex store for auth"
```

---

## Task 10: 前端 - 登出功能

**Files:**
- Modify: `src/views/ftp/FtpConfig.vue` (或其他现有页面添加退出按钮)

选择一个现有页面添加退出登录按钮，示例：

- [ ] **Step 1: 在顶部导航或现有页面添加退出按钮**

在FtpConfig.vue的模板中添加退出按钮（如果需要独立的顶部导航，后续可以创建一个layout组件）。

- [ ] **Step 2: 提交**

```bash
git add src/views/ftp/FtpConfig.vue
git commit -m "feat(auth): add logout button to FtpConfig page"
```

---

## Task 11: 测试验证

**Files:**
- Create: `report-backend/src/test/java/com/report/controller/AuthControllerTest.java`

- [ ] **Step 1: 创建AuthControllerTest**

```java
package com.report.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLoginSuccess() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .param("username", "admin")
                .param("password", "admin123"))
                .andExpect(status().isOk())
                .andReturn();

        assertNotNull(result.getRequest().getSessionId());
    }

    @Test
    public void testLoginFailure() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .param("username", "admin")
                .param("password", "wrongpassword"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testCurrentUserWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/auth/current-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testCsrfToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .param("username", "admin")
                .param("password", "admin123"))
                .andReturn();

        String sessionId = loginResult.getRequest().getSessionId();

        mockMvc.perform(get("/api/auth/csrf-token")
                .sessionId(sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
cd report-backend && mvn test -Dtest=AuthControllerTest
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/test/java/com/report/controller/AuthControllerTest.java
git commit -m "test(auth): add AuthController tests"
```

---

## 实现顺序

1. Task 1: 用户实体和Mapper
2. Task 2: 密码加密工具
3. Task 3: 认证服务
4. Task 4: 认证Controller
5. Task 5: Filter认证和CSRF防护
6. Task 6: 注册Filter
7. Task 7: 前端登录页面
8. Task 8: 前端API封装
9. Task 9: 路由守卫和Store改造
10. Task 10: 登出功能
11. Task 11: 测试验证
