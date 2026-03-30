# 技术栈文档

> **文档版本**: V1.2
> **创建日期**: 2026-03-30
> **最后更新**: 2026-03-31

---

## 1. 技术栈概览

### 1.1 核心技术选型

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **后端框架** | Spring Boot | 2.1.2 | 核心框架 |
| **ORM框架** | MyBatis-Plus | 3.4.3 | 数据库访问 |
| **数据库连接池** | Druid | 1.1.20 | 数据库连接管理 |
| **定时调度** | Quartz | 2.3.x | 任务调度 |
| **Excel处理** | Apache POI | 5.2.3 | Excel解析 |
| **FTP客户端** | Apache Commons Net | 3.9.0 | FTP操作 |
| **工具库** | Hutool | 5.8.25 | 工具类集合 |
| **前端框架** | Vue.js | 2.6.14 | 前端框架 |
| **UI组件库** | Element UI | 2.15.14 | UI组件库 |
| **HTTP客户端** | Axios | 0.21.4 | API请求 |
| **状态管理** | Vuex | 3.6.2 | 状态管理 |
| **路由** | Vue Router | 3.5.4 | 页面路由 |

---

## 2. 后端技术详解

### 2.1 Spring Boot 2.1.2

**为什么选择**: LTS版本，稳定可靠，社区资源丰富

| 组件 | 版本 | 说明 |
|------|------|------|
| spring-boot-starter-web | 2.1.2 | Web MVC |
| spring-boot-starter-quartz | 2.1.2 | 定时任务 |
| spring-boot-starter-test | 2.1.2 | 单元测试 |

### 2.2 MyBatis-Plus 3.4.3

**为什么选择**: 内置分页、逻辑删除、代码生成，减少样板代码

**核心特性**:
- CRUD接口自动注入
- 分页插件内置
- 逻辑删除支持
- 自动填充字段
- 条件构造器

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.4.3</version>
</dependency>
```

### 2.3 Apache POI 5.2.3

**为什么选择**: Java操作Office文档的标准库，支持.xls和.xlsx

**支持格式**:
- Excel 97-2003 (.xls)
- Excel 2007+ (.xlsx)

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.3</version>
</dependency>
```

### 2.4 Apache Commons Net 3.9.0

**为什么选择**: FTP协议的标准实现，支持被动/主动模式

**功能**:
- FTP连接管理
- 文件列表获取
- 文件下载
- 文件上传
- 文件删除/重命名

```xml
<dependency>
    <groupId>commons-net</groupId>
    <artifactId>commons-net</artifactId>
    <version>3.9.0</version>
</dependency>
```

### 2.5 Quartz 2.3.x

**为什么选择**: 企业级任务调度，支持cron表达式

**配置**:
- 内置于Spring Boot
- 支持持久化任务
- 支持集群

### 2.6 Hutool 5.8.25

**为什么选择**: 国产工具库，涵盖所有常用工具类

**使用模块**:
- JSON处理 (JSONUtil)
- 日期处理 (DateUtil)
- 反射工具 (ReflectUtil)
- 字符串工具 (StrUtil)

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>
```

### 2.7 Logback 1.2.3

**为什么选择**: Spring Boot内置日志框架，性能优异

**功能**:
- 分级日志输出 (DEBUG/INFO/WARN/ERROR)
- 日志文件轮转
- 按模块分离日志文件
- 异步日志写入

**配置文件**: `logback-spring.xml`

**日志文件**:
| 文件 | 说明 | 轮转策略 |
|------|------|----------|
| report-platform.log | 全部日志 | 50MB/文件, 保留30天 |
| report-platform-error.log | 错误日志 | 50MB/文件, 保留60天 |
| report-platform-operation.log | 操作日志 | 100MB/文件, 保留90天 |
| report-platform-access.log | 访问日志 | 100MB/文件, 保留30天 |

### 2.8 Spring AOP

**为什么选择**: 实现操作日志切面记录

**功能**:
- 方法拦截
- 自定义注解处理
- 操作前后数据对比
- 异步日志记录

---

## 3. 前端技术详解

### 3.1 Vue.js 2.6.14

**为什么选择**: 生态成熟，学习曲线平缓，适合企业项目

**核心特性**:
- 响应式数据绑定
- 组件化开发
- 指令系统
- 虚拟DOM

### 3.2 Element UI 2.15.14

**为什么选择**: Vue 2生态中最成熟的UI组件库

**使用组件**:
- el-form / el-form-item (表单)
- el-table (表格)
- el-pagination (分页)
- el-dialog (弹窗)
- el-button (按钮)
- el-select / el-option (选择器)
- el-input (输入框)
- el-radio / el-checkbox (单选/多选)
- el-tag (标签)

### 3.3 Axios 0.21.4

**为什么选择**: Promise-based HTTP客户端，易于配置拦截器

**配置**:
- 请求拦截器 (添加token)
- 响应拦截器 (统一处理错误)
- 基础URL配置
- 超时设置

### 3.4 Vue Router 3.5.4

**为什么选择**: Vue官方路由管理器

**模式**:
- Hash模式 (前端路由)
- History模式 (需要服务器配置)

### 3.5 Vuex 3.6.2

**为什么选择**: Vue官方状态管理库

**模块**:
- state (状态存储)
- mutations (同步修改)
- actions (异步操作)
- getters (计算属性)

---

## 4. 数据库技术

### 4.1 MySQL 5.7+

**为什么选择**: 开源免费，社区活跃，开发环境首选

**配置参数**:
```yaml
driver-class-name: com.mysql.cj.jdbc.Driver
url: jdbc:mysql://localhost:3306/report_db
username: root
password: root
```

### 4.2 GaussDB (可选)

**为什么选择**: 华为国产数据库，支持MySQL协议，兼容生产环境

**兼容配置**:
- 驱动: com.mysql.cj.jdbc.Driver
- 方言: MySQL

---

## 5. 开发工具

### 5.1 后端开发工具

| 工具 | 版本 | 用途 |
|------|------|------|
| **JDK** | **1.8 (JDK 8)** | Java运行时 / **生产环境标准版本** |
| Maven | 3.6+ | 项目构建 |
| IDEA / VS Code | - | 代码编辑 |
| MySQL Workbench | - | 数据库客户端 |

> **JDK 版本说明**:
> - **当前代码适配版本**: JDK 1.8 (JDK 8)
> - **生产环境标准版本**: JDK 1.8
> - **兼容性**: 代码中使用 `java.time` API (JDK 8+)，确保使用 JDK 1.8 或更高版本

### 5.2 前端开发工具

| 工具 | 版本 | 用途 |
|------|------|------|
| Node.js | 12+ | JavaScript运行时 |
| npm | 6+ | 包管理器 |
| Vue CLI | 4.5+ | 项目脚手架 |
| VS Code | - | 代码编辑 |
| Vue DevTools | - | 浏览器调试 |

---

## 6. 项目依赖

### 6.1 后端 pom.xml 关键依赖

```xml
<properties>
    <java.version>1.8</java.version>
    <mybatis-plus.version>3.4.3</mybatis-plus.version>
    <hutool.version>5.8.25</hutool.version>
    <poi.version>5.2.3</poi.version>
    <commons-net.version>3.9.0</commons-net.version>
</properties>
```

### 6.2 前端 package.json 关键依赖

```json
{
    "vue": "^2.6.14",
    "element-ui": "^2.15.14",
    "vue-router": "^3.5.4",
    "vuex": "^3.6.2",
    "axios": "^0.21.4"
}
```

---

## 7. 技术选型理由总结

| 原则 | 实现 |
|------|------|
| **稳定性** | 选择LTS版本，避免使用最新但未稳定版本 |
| **简洁性** | 单体架构，避免过度设计 |
| **团队适配** | 技术栈团队熟悉，学习成本低 |
| **社区支持** | 主流技术，社区活跃，文档完善 |
| **国产化** | 考虑GaussDB等国产数据库兼容性 |

---

## 8. 第三方服务

| 服务 | 用途 | 必要性 |
|------|------|--------|
| FTP服务器 | 文件来源 | 必须 |
| MySQL/GaussDB | 数据存储 | 必须 |
| BI报表系统 | 数据消费 | 外部系统 |

---

## 9. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 | 关联任务ID |
|------|------|----------|--------|------------|
| 2026-03-30 | V1.0 | [新增] 初始版本创建 | - | - |
| 2026-03-30 | V1.1 | [新增] 添加Druid数据库连接池技术栈 | - | TASK-006 |
| 2026-03-31 | V1.2 | [新增] 添加Logback日志框架技术栈 | AI Assistant | LOG-001 |
| 2026-03-31 | V1.2 | [新增] 添加Spring AOP技术栈 | AI Assistant | LOG-002 |
