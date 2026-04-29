# FTP配置简化重构设计文档

> **文档版本**: V1.0
> **创建日期**: 2026-04-29
> **状态**: 待审核

---

## 1. 背景与目标

### 1.1 背景

当前系统存在两套FTP连接体系：
1. **外部FTP配置 (`ftp_config` 表)**：支持多FTP服务器配置，包含扫描路径、文件匹配模式等
2. **内置FTP服务 (`built_in_ftp_config` 表)**：系统自带的FTP服务器

这种双轨制设计增加了系统复杂度，且外部FTP配置功能使用率低。

### 1.2 目标

| 目标 | 描述 |
|------|------|
| 简化架构 | 废弃多FTP设计方案，统一采用内置FTP作为唯一连接方式 |
| 移除扫描路径配置 | FTP配置仅保留连接参数，扫描路径迁移至报表配置 |
| 配置化管理 | FTP用户名密码通过配置文件管理，不再存储在数据库 |
| 删除冗余页面 | 移除FTP管理页面及相关API |

---

## 2. 架构变更

### 2.1 变更前后对比

```
变更前:
┌─────────────┐     ┌─────────────┐
│  report_config  │     │  ftp_config   │
│  (ftp_config_id)│────▶│  (scan_path)  │
└─────────────┘     └─────────────┘
                           │
                    ┌──────┴──────┐
                    │  内置FTP    │
                    │  (独立配置) │
                    └─────────────┘

变更后:
┌─────────────┐
│  report_config  │
│  (scan_path)    │──────┐
└─────────────┘       │
                      ▼
               ┌─────────────┐
               │   内置FTP    │
               │ (配置驱动)   │
               └─────────────┘
```

### 2.2 数据库变更

#### 2.2.1 `report_config` 表变更

```sql
-- 新增字段
ALTER TABLE report_config ADD COLUMN scan_path VARCHAR(200) COMMENT '扫描路径' AFTER ftp_config_id;

-- 保留 ftp_config_id 字段但用途变更：
-- - -1 或 特殊值 表示使用内置FTP
-- - 该字段将在后续版本逐步废弃
```

#### 2.2.2 删除 `ftp_config` 表

```sql
DROP TABLE IF EXISTS ftp_config;
```

#### 2.2.3 `built_in_ftp_config` 表变更

保持不变，但 username/password 将从配置文件读取，不再使用数据库中的值。

### 2.3 配置文件变更

#### `application.yml` 新增配置项

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
    max-threads: 5
```

---

## 3. 模块变更清单

### 3.1 后端删除模块

| 模块路径 | 说明 |
|----------|------|
| `controller/FtpConfigController.java` | FTP配置REST API |
| `service/FtpConfigService.java` | FTP配置服务接口 |
| `service/impl/FtpConfigServiceImpl.java` | FTP配置服务实现 |
| `mapper/FtpConfigMapper.java` | FTP配置Mapper |
| `resources/mapper/FtpConfigMapper.xml` | FTP配置MyBatis XML |
| `entity/FtpConfig.java` | FTP配置实体 |
| `job/FtpScanJob.java` 中的外部FTP扫描逻辑 | 移除外部FTP扫描 |

### 3.2 后端修改模块

| 模块路径 | 变更内容 |
|----------|----------|
| `entity/ReportConfig.java` | 新增 scanPath 字段 |
| `job/FtpScanJob.java` | 移除外部FTP扫描，仅保留内置FTP扫描 |
| `controller/ReportConfigController.java` | 扫描逻辑适配 |
| `controller/TaskController.java` | 扫描路径获取方式变更 |
| `util/FtpUtil.java` | 移除外部FTP连接相关方法 |
| `config/FtpConfigProperties.java` | 配置属性增强 |

### 3.3 前端删除模块

| 模块路径 | 说明 |
|----------|------|
| `views/ftp/FtpConfig.vue` | FTP配置管理页面 |
| `api/ftpConfig.js` | FTP配置API调用 |
| `router/index.js` 中的 `/ftp` 路由 | 移除FTP页面路由 |

### 3.4 前端修改模块

| 模块路径 | 变更内容 |
|----------|----------|
| `views/report/components/ReportConfig.vue` | 新增扫描路径配置项，隐藏FTP选择框 |

### 3.5 数据库脚本

| 脚本 | 说明 |
|------|------|
| `schema.sql` | 移除 ftp_config 表定义，report_config 新增 scan_path |
| `schema-gaussdb.sql` | 同上，适配GaussDB |
| `migration/V1.0__ftp_simplification.sql` | 数据迁移脚本 |

---

## 4. 内置FTP单用户权限配置

### 4.1 权限要求

| 权限 | 说明 |
|------|------|
| READ | 读取文件/目录 |
| WRITE | 写入/上传文件 |
| DELETE | 删除文件/目录 |
| RENAME | 重命名文件/目录 |
| MAKE | 创建目录 |

### 4.2 实现方案

`EmbeddedFtpServer.java` 中配置单用户权限：

```java
List<Authority> authorities = new ArrayList<>();
authorities.add(new ReadPermission());      // 读权限
authorities.add(new WritePermission());     // 写权限
authorities.add(new DeletePermission());    // 删除权限
authorities.add(new RenamePermission());    // 重命名权限
authorities.add(new MakeDirectoryPermission()); // 创建目录权限
```

---

## 5. 目录结构规划

内置FTP根目录下按报表编码创建子目录：

```
/data/ftp-root/
├── upload/                      # 默认上传目录
│   ├── SALES_REPORT/           # 销售报表目录
│   │   ├── 2026-04-29/
│   │   │   └── sales_20260429.xlsx
│   │   └── archive/
│   ├── INVENTORY_REPORT/       # 库存报表目录
│   └── ...
├── staging/                     # 暂存目录
├── for-upload/                  # 待上传目录
├── archive/                     # 归档目录
└── error/                       # 错误文件目录
```

---

## 6. 功能影响分析

### 6.1 受影响的现有功能

| 功能 | 影响 | 处理方式 |
|------|------|----------|
| FTP配置管理页面 | 页面删除 | 用户需迁移到报表配置中设置扫描路径 |
| 外部FTP扫描 | 移除 | 使用内置FTP替代 |
| 立即扫描功能 | 路径获取方式变更 | 适配新的scan_path获取逻辑 |
| 定时FTP扫描任务 | 改为仅扫描内置FTP | 修改FtpScanJob |

### 6.2 向后兼容处理

- `ftp_config_id = -1` 或 `NULL` 视为使用内置FTP
- 保留字段以便后续数据清理

---

## 7. 数据迁移策略

### 7.1 迁移步骤

1. **备份数据**：备份现有 `ftp_config` 和 `report_config` 表
2. **新增字段**：`report_config` 表新增 `scan_path` 字段
3. **数据迁移**：将 `ftp_config.scan_path` 合并到 `report_config.scan_path`
4. **验证数据**：确认所有报表配置的扫描路径已正确迁移
5. **删除旧表**：确认无误后删除 `ftp_config` 表

### 7.2 迁移SQL

```sql
-- 1. 新增 scan_path 字段
ALTER TABLE report_config ADD COLUMN scan_path VARCHAR(200) COMMENT '扫描路径' AFTER ftp_config_id;

-- 2. 迁移数据：将 ftp_config 的 scan_path 合并到 report_config
UPDATE report_config rc
INNER JOIN ftp_config fc ON rc.ftp_config_id = fc.id
SET rc.scan_path = fc.scan_path;

-- 3. 设置默认值：使用内置FTP的upload目录
UPDATE report_config SET scan_path = '/upload' WHERE scan_path IS NULL OR scan_path = '';

-- 4. 确认迁移完成后再删除 ftp_config 表
-- DROP TABLE ftp_config;
```

---

## 8. 测试计划

| 测试项 | 测试内容 |
|--------|----------|
| 内置FTP连接 | 验证单用户可连接并完成读写操作 |
| 目录权限 | 验证可创建、删除、修改文件 |
| 报表扫描 | 验证报表按配置的scan_path扫描 |
| 数据迁移 | 验证迁移脚本正确执行 |
| API兼容性 | 验证相关API正常响应 |

---

## 9. 风险评估

| 风险 | 等级 | 缓解措施 |
|------|------|----------|
| 数据迁移丢失 | 中 | 执行前完整备份 |
| 现有报表配置失效 | 高 | 迁移后逐一验证 |
| 内置FTP性能瓶颈 | 低 | 单用户场景下无性能问题 |

---

## 10. 实施里程碑

| 阶段 | 任务 |
|------|------|
| Phase 1 | 后端删除FtpConfig相关代码 |
| Phase 2 | 前端删除FTP页面 |
| Phase 3 | ReportConfig新增scan_path |
| Phase 4 | FtpScanJob重构 |
| Phase 5 | 数据迁移与验证 |
| Phase 6 | 文档同步与Git提交 |
