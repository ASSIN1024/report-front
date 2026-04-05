# 内置FTP服务集成设计

> **文档版本**: V1.0
> **创建日期**: 2026-04-05
> **维护人**: AI Assistant

---

## 1. 背景与目标

### 1.1 业务背景

当前系统作为RPA与BI之间的数据处理中间件，需要从外部FTP服务器监听并读取Excel文件。当外部FTP服务不稳定时，整个数据流程将受到影响。

### 1.2 目标

在内置Spring Boot应用中集成FTP服务器，让RPA机器人直接上传Excel到本系统，消除对外部FTP服务的依赖，提高系统稳定性。

---

## 2. 需求分析

| 项目 | 需求 |
|------|------|
| 场景 | RPA上传Excel到内置FTP，系统自动处理 |
| 并发 | 低（1-3个RPA），文件可能200MB+ |
| 用户模型 | 共享账号，共用目录 |
| 安全性 | 明文传输，内网使用 |
| 存储 | 永久归档，不清理 |
| 部署 | 内置嵌入Spring Boot |
| 端口 | 自定义端口2021 |
| 与现有系统 | 共存，保留外部FTP功能 |

---

## 3. 技术选型

### 3.1 方案选择

**Apache FtpServer** - 纯Java实现的FTP服务器，适合嵌入式集成。

### 3.2 选型理由

| 考量 | 评估 |
|------|------|
| 技术成熟度 | Apache顶级项目，稳定可靠 |
| Spring Boot集成 | 官方提供嵌入式支持 |
| 协议兼容性 | 标准FTP协议，RPA无需改造 |
| 大文件支持 | 支持断点续传，处理200MB+文件 |
| 跨平台 | Windows/Linux兼容 |
| 维护成本 | 纯Java，无额外系统依赖 |

---

## 4. 架构设计

### 4.1 整体架构

```
┌─────────────────────────────────────────────────────────┐
│                   Spring Boot Application                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ FtpServer   │  │ FtpScanJob   │  │ DataProcess │  │
│  │ (Apache      │  │ (监听内置FTP) │  │ Job         │  │
│  │ FtpServer)  │  │              │  │             │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                  │                  │          │
│         ▼                  ▼                  ▼          │
│  ┌──────────────────────────────────────────────────┐   │
│  │              共享存储目录 (/data/ftp-root)         │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
         ▲
         │ FTP (端口2021)
         │
    ┌────┴────┐
    │   RPA   │
    │ Robots  │
    └─────────┘
```

### 4.2 组件职责

| 组件 | 包路径 | 职责 |
|------|--------|------|
| `EmbeddedFtpServer` | `com.report.ftp` | FTP服务生命周期管理 |
| `FtpConfigProperties` | `com.report.config` | FTP配置属性类 |
| `BuiltInFtpConfig` | `com.report.entity` | 内置FTP配置实体 |
| `BuiltInFtpConfigMapper` | `com.report.mapper` | 数据库映射 |
| `BuiltInFtpConfigService` | `com.report.service` | 配置管理服务 |
| `BuiltInFtpConfigController` | `com.report.controller` | 配置管理API |
| `FtpScanJob` | `com.report.job` | 扫描内置FTP目录 |

### 4.3 数据流

```
1. RPA机器人连接FTP服务器 (IP:2021)
2. RPA上传Excel文件到 /data/ftp-root/upload/
3. FtpScanJob定时扫描 upload/ 目录
4. 发现新文件后，读取并解析Excel
5. 执行数据处理Job
6. 处理完成后记录到 processed_file 表
```

---

## 5. 数据库设计

### 5.1 新增表：built_in_ftp_config

```sql
CREATE TABLE `built_in_ftp_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `port` INT NOT NULL DEFAULT 2021 COMMENT 'FTP端口',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码',
  `root_directory` VARCHAR(256) NOT NULL DEFAULT '/data/ftp-root' COMMENT '根目录',
  `max_connections` INT NOT NULL DEFAULT 10 COMMENT '最大连接数',
  `idle_timeout` INT NOT NULL DEFAULT 300 COMMENT '空闲超时(秒)',
  `被动模式` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否被动模式',
  `被动端口起始` INT DEFAULT 50000 COMMENT '被动端口起始',
  `被动端口结束` INT DEFAULT 50100 COMMENT '被动端口结束',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='内置FTP配置';
```

### 5.2 目录结构

```
/data/ftp-root/
├── upload/          # RPA上传文件目录
├── processing/      # 正在处理的文件
├── completed/       # 处理完成的文件
└── failed/          # 处理失败的文件
```

---

## 6. 配置设计

### 6.1 application.yml

```yaml
ftp:
  built-in:
    enabled: true
    port: 2021
    username: rpa_user
    password: rpa_password
    root-directory: /data/ftp-root
    max-connections: 10
    idle-timeout: 300
    passive-mode: true
    passive-port-start: 50000
    passive-port-end: 50100
```

---

## 7. API接口设计

### 7.1 内置FTP配置管理

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/built-in-ftp/config` | 获取配置 |
| PUT | `/api/built-in-ftp/config` | 更新配置 |
| POST | `/api/built-in-ftp/start` | 启动FTP服务 |
| POST | `/api/built-in-ftp/stop` | 停止FTP服务 |
| GET | `/api/built-in-ftp/status` | 获取服务状态 |

### 7.2 响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "running": true,
    "port": 2021,
    "connectedClients": 2
  }
}
```

---

## 8. 日志设计

### 8.1 日志内容

| 日志类型 | 记录内容 |
|----------|----------|
| 访问日志 | 客户端IP、登录用户、访问时间、操作 |
| 操作日志 | 文件上传、下载、删除、命名的操作 |
| 错误日志 | 连接失败、认证失败、IO异常 |

### 8.2 日志格式

```
[2026-04-05 10:30:15] [FTP-ACCESS] 192.168.1.100 - rpa_user - Login - /
[2026-04-05 10:30:16] [FTP-ACCESS] 192.168.1.100 - rpa_user - Upload - /upload/report.xlsx
[2026-04-05 10:30:20] [FTP-ERROR] Authentication failed for user: invalid_user
```

---

## 9. 与现有系统集成

### 9.1 现有组件处理

| 现有组件 | 处理方式 |
|----------|----------|
| `FtpConfig` | 保留，新增`BuiltInFtpConfig`表 |
| `FtpScanJob` | 扩展支持扫描内置FTP目录 |
| `FtpUtil` | 保留，用于外部FTP（兼容现有逻辑） |
| `processed_file` | 复用，统一去重机制 |

### 9.2 前端页面

在FTP配置页面新增"内置FTP"Tab页，用于：
- 查看/修改内置FTP配置
- 启动/停止FTP服务
- 查看连接状态

---

## 10. 可行性评估

| 维度 | 评估 |
|------|------|
| 技术可行性 | ✅ Apache FtpServer成熟，Spring Boot集成简单 |
| 资源可行性 | ✅ 无需额外服务器，内置JVM进程 |
| 时间可行性 | ⏱ 预计1-2天开发周期 |
| 维护成本 | ✅ 低，配置驱动，无额外依赖 |

---

## 11. 优势与劣势

### 11.1 优势

1. **消除单点故障** - 不依赖外部FTP服务
2. **部署简化** - 单应用包含所有功能
3. **排查高效** - FTP服务日志与业务日志统一
4. **可控性强** - 可根据需求定制FTP行为
5. **数据安全** - 避免数据在外部FTP中流转

### 11.2 劣势

1. **增加系统复杂度** - FTP服务与业务进程绑定
2. **资源占用** - JVM进程需管理FTP线程池
3. **重启影响** - 应用重启时FTP暂时不可用

---

## 12. 风险与应对

| 风险 | 等级 | 应对措施 |
|------|------|----------|
| 应用重启FTP不可用 | 中 | 提供启动检查和自动重启机制 |
| 大文件上传超时 | 低 | 配置合理的idle-timeout和传输超时 |
| 磁盘空间不足 | 中 | 监控磁盘空间，日志告警 |
| 并发连接数超限 | 低 | 限制max-connections，前端提示 |

---

## 13. 实施建议

### 13.1 分阶段实施

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| Phase 1 | 基础FTP服务集成 | P0 |
| Phase 2 | 数据库配置管理 | P0 |
| Phase 3 | 前端配置界面 | P1 |
| Phase 4 | 日志与监控 | P2 |

### 13.2 测试建议

1. **功能测试** - 基本上传下载删除操作
2. **大文件测试** - 200MB+文件传输
3. **并发测试** - 3个客户端同时上传
4. **异常测试** - 网络中断、磁盘满等场景
5. **安全测试** - 暴力破解防护

---

## 14. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-04-05 | V1.0 | 初始版本 | AI Assistant |
