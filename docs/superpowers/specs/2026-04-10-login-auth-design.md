# 登录认证系统设计文档

**版本**: V1.0
**日期**: 2026-04-10
**状态**: 待实现

---

## 1. 设计目标

为报表数据处理平台提供安全、简洁的用户认证功能，限制非授权用户访问系统配置接口，防止黑客攻击。

### 1.1 核心需求

| 需求 | 说明 |
|------|------|
| 用户认证 | 用户名/密码登录，密码加密存储 |
| 会话管理 | 登录状态跟踪，30分钟超时 |
| 权限控制 | 已登录用户才能修改系统配置 |
| 安全防护 | 防SQL注入、XSS、CSRF攻击 |
| 简化设计 | 2-3人使用，无需复杂角色管理 |

### 1.2 非需求

- 不实现多角色、多级菜单
- 不实现复杂的权限矩阵
- 不实现登录失败锁定机制

---

## 2. 技术方案

**方案B：自定义Filter + Session认证**

| 组件 | 技术实现 |
|------|----------|
| 认证Filter | javax.servlet.Filter + HttpSession |
| 密码加密 | BCrypt（Spring Security Crypto） |
| CSRF防护 | 自定义Token头 `X-CSRF-Token` |
| 会话超时 | 30分钟无操作超时 |

---

## 3. 数据库设计

### 3.1 用户表

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密密码',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '系统用户表';
```

### 3.2 默认用户初始化

系统启动时自动创建默认用户，无需手动执行SQL：

```java
@PostConstruct
public void initDefaultUser() {
    if (userMapper.selectCount(null) == 0) {
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        userMapper.insert(admin);
    }
}
```

**默认账户**：
- 用户名：`admin`
- 密码：`admin123`

如需修改密码，直接修改代码后重新部署即可。

---

## 4. 后端组件设计

### 4.1 组件列表

| 组件 | 路径 | 职责 |
|------|------|------|
| SysUser | entity/SysUser.java | 用户实体 |
| SysUserMapper | mapper/SysUserMapper.java | 用户数据访问 |
| AuthService | service/AuthService.java | 认证业务逻辑 |
| AuthController | controller/AuthController.java | 登录/登出接口 |
| LoginUser | entity/LoginUser.java | 当前登录用户信息 |
| AuthenticationFilter | filter/AuthenticationFilter.java | 认证拦截器 |
| CsrfFilter | filter/CsrfFilter.java | CSRF防护 |
| PasswordEncoder | util/PasswordEncoder.java | 密码加密工具 |
| SecurityConfig | config/SecurityConfig.java | 安全配置 |
| AuthInterceptor | interceptor/AuthInterceptor.java | 路由权限拦截（前端） |

### 4.2 API接口

| 接口 | 方法 | 说明 | 认证要求 |
|------|------|------|----------|
| `/api/auth/login` | POST | 用户登录 | 否 |
| `/api/auth/logout` | POST | 用户登出 | 是 |
| `/api/auth/current-user` | GET | 获取当前用户 | 是 |
| `/api/auth/csrf-token` | GET | 获取CSRF Token | 否 |

### 4.3 登录流程

```
1. 前端调用 /api/auth/login，提交 username, password
2. 后端验证用户名密码
3. 验证成功：创建Session，存储LoginUser，返回用户信息
4. 验证失败：返回错误信息
5. 前端存储SessionId到Cookie
```

### 4.4 请求拦截流程

```
请求进入
    ↓
CsrfFilter: 检查 X-CSRF-Token 头（POST/PUT/DELETE）
    ↓
AuthenticationFilter: 检查Session中是否有LoginUser
    ↓
通过 → 执行业务逻辑
拒绝 → 返回 401 未登录 / 403 CSRF验证失败
```

---

## 5. 安全防护设计

### 5.1 SQL注入防护

- 使用 MyBatis-Plus 参数化查询
- 不拼接SQL字符串

### 5.2 XSS防护

- Response设置Content-Security-Policy头
- 前端输入框特殊字符转义
- 后端统一错误响应，不泄露内部信息

### 5.3 CSRF防护

- 登录成功后返回CSRF Token
- 前端存储Token，每次POST/PUT/DELETE请求携带到 `X-CSRF-Token` 头
- Token与Session绑定

### 5.4 会话管理

- Session超时时间：30分钟
- 登出时销毁Session
- 登录接口返回SessionId（Cookie自动携带）

---

## 6. 前端改造

### 6.1 新增页面

| 页面 | 路径 | 说明 |
|------|------|------|
| 登录页 | views/login/Login.vue | 独立登录页面 |

### 6.2 路由改造

所有业务路由添加路由守卫，未登录重定向到 `/login`：

```javascript
router.beforeEach((to, from, next) => {
  if (to.path === '/login') {
    next()
  } else if (!store.state.user) {
    next('/login')
  } else {
    next()
  }
})
```

### 6.3 API封装

封装HTTP请求，自动携带Session和CSRF Token：

```javascript
// request.js
axios.interceptors.request.use(config => {
  // 自动携带Cookie（Session）
  // 自动携带CSRF Token
  return config
})
```

---

## 7. 文件清单

### 7.1 后端文件

```
report-backend/src/main/java/com/report/
├── entity/
│   ├── SysUser.java
│   └── LoginUser.java
├── mapper/
│   └── SysUserMapper.java
├── service/
│   ├── AuthService.java
│   └── impl/AuthServiceImpl.java
├── controller/
│   └── AuthController.java
├── filter/
│   ├── AuthenticationFilter.java
│   └── CsrfFilter.java
├── config/
│   └── SecurityConfig.java
└── util/
    └── PasswordEncoder.java
```

### 7.2 前端文件

```
src/
├── views/
│   └── login/
│       └── Login.vue
├── store/
│   └── index.js (改造)
├── router/
│   └── index.js (改造)
├── api/
│   └── auth.js
└── utils/
    └── request.js
```

### 7.3 数据库文件

```
docs/superpowers/specs/
└── 2026-04-10-login-auth-design.md (本文档)
```

---

## 8. 测试方式

### 8.1 后端测试

测试时带上SessionId即可：

```java
@Test
public void testApi() {
    // 1. 先登录获取Session
    MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .param("username", "admin")
            .param("password", "admin123"))
            .andReturn();

    String sessionId = loginResult.getRequest().getSessionId();

    // 2. 后续请求带上Session
    mockMvc.perform(get("/api/ftp/config")
            .sessionId(sessionId))
            .andExpect(status().isOk());
}
```

### 8.2 前端测试

使用浏览器登录后，Cookie自动携带Session。

### 8.3 API工具测试

先调用登录接口，工具会自动保存Cookie，后续请求无需手动处理。

---

## 9. 配置项

### 9.1 application.yml

```yaml
auth:
  session-timeout: 1800  # 30分钟，单位秒
  csrf-enabled: true     # 是否启用CSRF防护
```

---

## 10. 错误处理

| 错误场景 | HTTP状态码 | 返回信息 |
|----------|------------|----------|
| 未登录访问 | 401 | {"code": 401, "message": "请先登录"} |
| CSRF验证失败 | 403 | {"code": 403, "message": "CSRF验证失败"} |
| 用户名密码错误 | 200 | {"code": 400, "message": "用户名或密码错误"} |
| Session过期 | 401 | {"code": 401, "message": "会话已过期，请重新登录"} |

---

## 11. 实现顺序

1. 数据库表创建和初始化数据
2. 后端实体类和Mapper
3. AuthService认证服务
4. AuthController登录接口
5. Filter认证和CSRF拦截器
6. SecurityConfig安全配置
7. 前端登录页面
8. 路由守卫和API封装
9. 现有Controller权限验证（可选）
