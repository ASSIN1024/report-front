# FTP配置简化重构设计文档

> **文档版本**: V2.0
> **创建日期**: 2026-04-29
> **状态**: 待审核

---

## 1. 背景与目标

### 1.1 现状问题

当前系统存在两套FTP连接体系：
- **外部FTP配置 (`ftp_config` 表)**：支持多FTP服务器，配置复杂
- **内置FTP服务 (`built_in_ftp_config` 表)**：系统自带的FTP服务器

架构复杂，维护成本高。

### 1.2 重构目标

| 目标 | 说明 |
|------|------|
| 简化架构 | 废弃外部FTP，统一使用内置FTP |
| 独立模块 | 内置FTP作为独立模块，其他模块按需调用 |
| 配置驱动 | FTP参数通过 `application.yml` 配置 |
| 删除冗余 | 移除FTP管理页面和外部FTP相关代码 |

---

## 2. 架构设计

### 2.1 模块划分

```
┌─────────────────────────────────────────────────────────┐
│                      报表系统                            │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐ │
│  │ ReportConfig │    │ FtpScanJob  │    │ TaskController│ │
│  └──────┬──────┘    └──────┬──────┘    └──────┬──────┘ │
│         │                  │                  │        │
│         └──────────────────┼──────────────────┘        │
│                            │                            │
│                            ▼                            │
│              ┌─────────────────────────┐              │
│              │    内置FTP服务模块        │              │
│              │  ┌───────────────────┐  │              │
│              │  │ FtpServerManager  │  │              │
│              │  │ (启动/停止/状态)   │  │              │
│              │  └───────────────────┘  │              │
│              │  ┌───────────────────┐  │              │
│              │  │ FtpConfigProperties│  │              │
│              │  │ (配置管理)         │  │              │
│              │  └───────────────────┘  │              │
│              └───────────┬─────────────┘              │
│                          │                            │
│                          ▼                            │
│              ┌─────────────────────────┐              │
│              │   Apache FtpServer      │              │
│              │   (单用户, 全权限)       │              │
│              └─────────────────────────┘              │
└─────────────────────────────────────────────────────────┘
```

### 2.2 内置FTP配置

`application.yml` 配置项：

```yaml
ftp:
  built-in:
    enabled: true
    port: 2021
    username: ${FTP_USERNAME:rpa_user}
    password: ${FTP_PASSWORD:rpa_password}
    root-directory: /data/ftp-root
    max-connections: 10
    idle-timeout: 300
    passive-mode: true
    passive-port-start: 50000
    passive-port-end: 50100
```

**说明**：
- `enabled`: 是否启用内置FTP
- `port`: FTP服务端口
- `username/password`: FTP登录凭证（支持环境变量）
- `root-directory`: FTP根目录
- 其他参数: 连接数、超时、被动模式等

---

## 3. 数据库变更

### 3.1 `report_config` 表新增字段

```sql
ALTER TABLE report_config ADD COLUMN scan_path VARCHAR(200) COMMENT '扫描路径' AFTER ftp_config_id;
```

### 3.2 删除 `ftp_config` 表

```sql
DROP TABLE IF EXISTS ftp_config;
```

---

## 4. 代码变更清单

### 4.1 后端删除

| 文件 | 说明 |
|------|------|
| `controller/FtpConfigController.java` | FTP配置API |
| `service/FtpConfigService.java` | FTP配置服务接口 |
| `service/impl/FtpConfigServiceImpl.java` | FTP配置服务实现 |
| `mapper/FtpConfigMapper.java` | FTP配置Mapper |
| `resources/mapper/FtpConfigMapper.xml` | MyBatis XML |
| `entity/FtpConfig.java` | FTP配置实体 |

### 4.2 后端修改

| 文件 | 变更内容 |
|------|----------|
| `entity/ReportConfig.java` | 新增 `scanPath` 字段 |
| `job/FtpScanJob.java` | 移除外部FTP扫描，仅扫描内置FTP |
| `controller/ReportConfigController.java` | 扫描路径从reportConfig获取 |
| `controller/TaskController.java` | 同上 |
| `src/views/report/components/ReportConfig.vue` | 新增scanPath配置项 |

### 4.3 前端删除

| 文件 | 说明 |
|------|------|
| `views/ftp/FtpConfig.vue` | FTP配置页面 |
| `api/ftpConfig.js` | FTP API调用 |
| `router/index.js` 中的 `/ftp` 路由 | 移除FTP页面路由 |

---

## 5. 内置FTP单用户权限

内置FTP使用单用户模式，用户拥有FTP根目录的完整操作权限：

| 权限 | 说明 |
|------|------|
| 读取 | 下载文件、列出目录 |
| 写入 | 上传文件、创建目录 |
| 删除 | 删除文件、删除目录 |
| 重命名 | 重命名文件、目录 |

**实现方式**：Apache FtpServer 的 BaseUser + WritePermission + DeletePermission + MakeDirectoryPermission

```java
// EmbeddedFtpServer.java 权限配置
BaseUser user = new BaseUser();
user.setName(properties.getUsername());
user.setPassword(properties.getPassword());
user.setHomeDirectory(rootDir.getAbsolutePath());

List<Authority> authorities = new ArrayList<>();
authorities.add(new WritePermission());      // 写权限
authorities.add(new DeletePermission());     // 删除权限
authorities.add(new MakeDirectoryPermission()); // 创建目录权限
user.setAuthorities(authorities);
```

---

## 6. 目录结构

```
/data/ftp-root/
├── upload/                      # 上传目录
│   ├── SALES_REPORT/           # 销售报表目录
│   │   └── sales_20260429.xlsx
│   ├── archive/                # 归档子目录
│   └── error/                  # 错误文件子目录
└── ...
```

报表配置中的 `scan_path` 字段指定相对于FTP根目录的扫描路径，如 `/upload/SALES_REPORT`

---

## 7. 数据迁移

```sql
-- 1. 新增 scan_path 字段
ALTER TABLE report_config ADD COLUMN scan_path VARCHAR(200) COMMENT '扫描路径' AFTER ftp_config_id;

-- 2. 迁移现有数据
UPDATE report_config rc
INNER JOIN ftp_config fc ON rc.ftp_config_id = fc.id
SET rc.scan_path = fc.scan_path;

-- 3. 设置默认值
UPDATE report_config SET scan_path = '/upload' WHERE scan_path IS NULL OR scan_path = '';

-- 4. 删除外部FTP配置表（确认迁移完成后执行）
-- DROP TABLE ftp_config;
```

---

## 8. 实施计划

| 阶段 | 任务 |
|------|------|
| Phase 1 | 删除FtpConfig后端代码 |
| Phase 2 | 删除FtpConfig前端代码 |
| Phase 3 | ReportConfig新增scanPath字段 |
| Phase 4 | 修改FtpScanJob适配新架构 |
| Phase 5 | 数据迁移与验证 |
| Phase 6 | 文档同步与提交 |
