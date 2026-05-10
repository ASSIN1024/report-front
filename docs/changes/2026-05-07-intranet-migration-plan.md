# 内网迁移实施方案

> **文档版本**: V1.0
> **创建日期**: 2026-05-07
> **维护人**: AI Assistant
> **关联文档**: [数据库结构一致性验证报告](2026-05-07-database-schema-consistency-report.md)

---

## 1. 迁移概述

### 1.1 背景

报表数据处理平台当前运行在开发环境中（MySQL 8.0），需要迁移至内网生产环境（GaussDB 5.x）。本次迁移涉及：

- 数据库从MySQL 8.0迁移到GaussDB 5.x
- 应用部署到内网服务器
- 配置参数调整为生产环境要求

### 1.2 迁移目标

| 目标 | 说明 |
|------|------|
| 数据完整迁移 | 所有业务数据、配置数据完整迁移 |
| 零停机迁移 | 尽可能减少业务中断时间 |
| 回滚能力 | 迁移失败时可快速回滚 |
| 兼容性验证 | 迁移后功能正常，无异常 |

### 1.3 迁移范围

| 组件 | 当前环境 | 目标环境 |
|------|----------|----------|
| 数据库 | MySQL 8.0 (Docker) | GaussDB 5.x |
| 后端服务 | Spring Boot 2.1.2 | Spring Boot 2.1.2 |
| 前端服务 | Vue 2.6 (Node 14+) | Vue 2.6 |
| JDK | OpenJDK 1.8 | OpenJDK 1.8 |
| FTP服务 | 内置Apache FtpServer | 内置Apache FtpServer |
| 调度器 | Quartz JDBC集群 | Quartz JDBC集群 |

---

## 2. 环境配置要求

### 2.1 服务器配置

#### 最小配置 (单实例)

| 资源 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 4核 | 8核 |
| 内存 | 8GB | 16GB |
| 磁盘 | 100GB | 200GB SSD |
| 网络 | 100Mbps | 1Gbps |

#### 推荐配置 (集群模式)

| 资源 | 最低要求 | 推荐配置 |
|------|----------|----------|
| CPU | 8核 x 2台 | 16核 x 2台 |
| 内存 | 16GB x 2台 | 32GB x 2台 |
| 磁盘 | 200GB SSD x 2台 | 500GB SSD x 2台 |
| 网络 | 1Gbps | 10Gbps |

### 2.2 软件要求

| 软件 | 版本要求 | 说明 |
|------|----------|------|
| 操作系统 | CentOS 7.x / 8.x 或 麒麟V10 | 国产化支持 |
| JDK | OpenJDK 1.8.0_xxx | 建议1.8.0_272+ |
| Node.js | 14.x | 前端构建需要 |
| Maven | 3.6.x | 后端构建需要 |
| GaussDB | 5.0.0+ | 数据库 |
| Nginx | 1.16+ | 前端反向代理 |

### 2.3 网络要求

| 项目 | 要求 |
|------|------|
| 内网访问 | 应用服务器、数据库服务器之间内网互通 |
| 端口开放 | 8082(后端API)、8086(前端)、3306(GaussDB)、5432(GaussDB) |
| FTP端口 | 2021(内置FTP) |
| Druid监控 | 8082/druid (仅内网访问) |

---

## 3. 数据库迁移

### 3.1 GaussDB Schema初始化

#### 步骤1: 创建数据库

```sql
-- 使用高权限账号连接GaussDB
-- 创建数据库
CREATE DATABASE report_db WITH ENCODING='UTF8' TEMPLATE = template0;

-- 连接到数据库
\c report_db
```

#### 步骤2: 执行Schema创建

```bash
# 将schema-gaussdb.sql上传到服务器后执行
psql -h <hostname> -p 5432 -U <username> -d report_db -f schema-gaussdb.sql
```

#### 步骤3: 补充缺失表 (必须执行)

GaussDB Schema相比MySQL Schema缺少以下表，需要手动补充：

**文件**: `migration/V1.4__add_missing_tables_for_gaussdb.sql`

```sql
-- =============================================================================
-- 补充GaussDB Schema缺失的表 (MySQL版迁移需要)
-- 执行时间: 2026-05-07
-- =============================================================================

-- 1. 系统用户表 (sys_user)
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    last_login_time TIMESTAMP DEFAULT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_username UNIQUE (username)
);
CREATE TRIGGER trigger_sys_user_update
    BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
COMMENT ON TABLE sys_user IS '系统用户表';

-- 2. 打包配置表 (packing_config)
DROP TABLE IF EXISTS packing_config CASCADE;
CREATE TABLE packing_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL,
    config_value VARCHAR(500),
    config_type VARCHAR(50),
    description VARCHAR(200),
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_packing_config_key UNIQUE (config_key)
);
CREATE TRIGGER trigger_packing_config_update
    BEFORE UPDATE ON packing_config
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
COMMENT ON TABLE packing_config IS '打包配置表';

-- 3. 打包批次表 (packing_batch)
DROP TABLE IF EXISTS packing_batch CASCADE;
CREATE TABLE packing_batch (
    id BIGSERIAL PRIMARY KEY,
    batch_no VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_size BIGINT DEFAULT 0,
    file_count INT DEFAULT 0,
    for_upload_path VARCHAR(500),
    done_dir_path VARCHAR(500),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_packing_batch_no UNIQUE (batch_no)
);
CREATE INDEX idx_packing_batch_status ON packing_batch(status);
CREATE INDEX idx_packing_batch_create_time ON packing_batch(create_time);
CREATE TRIGGER trigger_packing_batch_update
    BEFORE UPDATE ON packing_batch
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
COMMENT ON TABLE packing_batch IS '打包批次表';

-- 4. 告警记录表 (alert_record)
DROP TABLE IF EXISTS alert_record CASCADE;
CREATE TABLE alert_record (
    id BIGSERIAL PRIMARY KEY,
    alert_type VARCHAR(20) NOT NULL,
    file_name VARCHAR(200),
    report_config_id BIGINT,
    alert_level VARCHAR(20),
    alert_message VARCHAR(500),
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolve_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT NULL,
    CONSTRAINT uk_alert_record_id UNIQUE (id)
);
CREATE INDEX idx_alert_record_type ON alert_record(alert_type);
CREATE INDEX idx_alert_record_status ON alert_record(status);
CREATE INDEX idx_alert_record_create_time ON alert_record(create_time);
COMMENT ON TABLE alert_record IS '告警记录表';

-- 5. ODS备份记录表 (ods_backup)
DROP TABLE IF EXISTS ods_backup CASCADE;
CREATE TABLE ods_backup (
    id BIGSERIAL PRIMARY KEY,
    source_file VARCHAR(200) NOT NULL,
    pt_dt VARCHAR(20),
    db_name VARCHAR(128),
    table_name VARCHAR(128),
    report_config_id BIGINT,
    file_size BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ods_backup_pt_dt ON ods_backup(pt_dt);
CREATE INDEX idx_ods_backup_source_file ON ods_backup(source_file);
CREATE INDEX idx_ods_backup_report_config_id ON ods_backup(report_config_id);
COMMENT ON TABLE ods_backup IS 'ODS备份记录表';

-- 6. 表分层映射表 (table_layer_mapping)
DROP TABLE IF EXISTS table_layer_mapping CASCADE;
CREATE TABLE table_layer_mapping (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    table_layer VARCHAR(20),
    source_type VARCHAR(50),
    source_id BIGINT,
    source_name VARCHAR(200),
    business_domain VARCHAR(200),
    description VARCHAR(500),
    tags JSONB,
    marked SMALLINT NOT NULL DEFAULT 0,
    deleted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_table_layer_mapping_name UNIQUE (table_name)
);
CREATE TRIGGER trigger_table_layer_mapping_update
    BEFORE UPDATE ON table_layer_mapping
    FOR EACH ROW EXECUTE FUNCTION update_timestamp();
COMMENT ON TABLE table_layer_mapping IS '表分层映射表';

-- 7. 触发器执行日志表 (trigger_execution_log)
DROP TABLE IF EXISTS trigger_execution_log CASCADE;
CREATE TABLE trigger_execution_log (
    id BIGSERIAL PRIMARY KEY,
    trigger_code VARCHAR(100) NOT NULL,
    trigger_name VARCHAR(200) NOT NULL,
    partition_date DATE NOT NULL,
    data_count INT DEFAULT 0,
    trigger_status VARCHAR(20) NOT NULL,
    pipeline_task_id BIGINT,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    execution_time TIMESTAMP NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_trigger_exec_log_code ON trigger_execution_log(trigger_code);
CREATE INDEX idx_trigger_exec_log_date ON trigger_execution_log(partition_date);
CREATE INDEX idx_trigger_exec_log_time ON trigger_execution_log(execution_time);
COMMENT ON TABLE trigger_execution_log IS '触发器执行日志表';

-- =============================================================================
-- 初始化数据
-- =============================================================================

-- 插入默认管理员用户 (密码: admin123, BCrypt加密)
INSERT INTO sys_user (username, password) VALUES
('admin', '$2a$10$zg.SpcNkqcH2g65SLjwZeO1KBObp6waE2bjm2pCrF5bsgED6Mwd.2');

-- 插入内置FTP默认配置
INSERT INTO built_in_ftp_config (enabled, port, username, password, root_directory, max_connections, idle_timeout, passive_mode, passive_port_start, passive_port_end)
VALUES (0, 2021, 'rpa_user', 'rpa_password', '/data/ftp-root', 10, 300, 1, 50000, 50100);

-- 插入打包默认配置
INSERT INTO packing_config (config_key, config_value, config_type, description) VALUES
('max_package_size', '209715200', 'SIZE', '单个包最大大小(200MB)'),
('upload_dir', '/data/ftp-root/for-upload', 'PATH', '上传目录'),
('done_dir', '/data/ftp-root/done', 'PATH', '完成目录'),
('fixed_filename', 'outputs.zip', 'STRING', '固定文件名'),
('polling_interval', '30', 'INT', '消费轮询间隔(秒)'),
('scan_interval', '300', 'INT', '扫描间隔(秒)');

-- =============================================================================
-- 完成提示
-- =============================================================================
-- 数据库初始化完成！
-- 默认管理员账号: admin / admin123
-- =============================================================================
```

### 3.2 数据迁移策略

#### MySQL到GaussDB数据迁移方法

| 方法 | 适用场景 | 工具 |
|------|----------|------|
| 全量导出导入 | 首次全量迁移 | mysqldump + psql |
| 增量同步 | 业务不中断迁移 | DataX / Canal |
| 应用双写 | 切换期间一致性保证 | 应用层控制 |

#### 推荐方案: 全量导出+增量同步

**步骤1: 数据导出 (MySQL)**

```bash
# 导出所有业务表数据 (排除Quartz调度表)
mysqldump -h localhost -P 33060 -uroot -proot123456 report_db \
    --no-create-info \
    --skip-quote-names \
    --compatible=postgresql \
    --tables \
    sys_config \
    sys_user \
    built_in_ftp_config \
    report_config \
    processed_file \
    task_execution \
    task_execution_log \
    trigger_config \
    trigger_state_record \
    trigger_partition_record \
    pipeline_config \
    table_layer_mapping \
    packing_config \
    packing_batch \
    alert_record \
    ods_backup \
    operation_log \
    > /tmp/report_db_data.sql
```

**步骤2: 数据清洗与转换**

由于MySQL和GaussDB存在语法差异，需要对导出的SQL进行转换：

```bash
# 使用sed进行基本转换
sed -i 's/`//g' /tmp/report_db_data.sql                    # 移除反引号
sed -i 's/ENGINE=InnoDB.*//g' /tmp/report_db_data.sql      # 移除Engine声明
sed -i 's/CHARSET=utf8mb4.*//g' /tmp/report_db_data.sql    # 移除字符集声明
sed -i 's/COLLATE=utf8mb4_.*//g' /tmp/report_db_data.sql   # 移除Collate
sed -i 's/CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP//g' /tmp/report_db_data.sql
sed -i 's/DEFAULT CURRENT_TIMESTAMP//g' /tmp/report_db_data.sql
sed -i "s/'0'::bit/0/g" /tmp/report_db_data.sql             # 转换bit类型
sed -i "s/::text//g" /tmp/report_db_data.sql                # 移除类型转换
sed -i 's/NOT NULL AUTO_INCREMENT/NOT NULL/g' /tmp/report_db_data.sql  # GaussDB用SERIAL
sed -i 's/tinyint(1)/SMALLINT/g' /tmp/report_db_data.sql    # 转换布尔类型
sed -i 's/bigint NOT NULL AUTO_INCREMENT/BIGSERIAL/g' /tmp/report_db_data.sql
```

**步骤3: 数据导入 (GaussDB)**

```bash
# 导入转换后的数据
psql -h <gaussdb_host> -p 5432 -U <username> -d report_db -f /tmp/report_db_data.sql
```

**步骤4: 验证数据**

```sql
-- 验证各表数据量
SELECT 'sys_user' as table_name, COUNT(*) as cnt FROM sys_user
UNION ALL
SELECT 'sys_config', COUNT(*) FROM sys_config
UNION ALL
SELECT 'report_config', COUNT(*) FROM report_config
UNION ALL
SELECT 'processed_file', COUNT(*) FROM processed_file
UNION ALL
SELECT 'task_execution', COUNT(*) FROM task_execution
UNION ALL
SELECT 'trigger_config', COUNT(*) FROM trigger_config
UNION ALL
SELECT 'pipeline_config', COUNT(*) FROM pipeline_config
UNION ALL
SELECT 'packing_config', COUNT(*) FROM packing_config
UNION ALL
SELECT 'packing_batch', COUNT(*) FROM packing_batch
UNION ALL
SELECT 'alert_record', COUNT(*) FROM alert_record
UNION ALL
SELECT 'ods_backup', COUNT(*) FROM ods_backup;
```

---

## 4. 应用部署

### 4.1 后端部署

#### 构建

```bash
# 1. 进入后端目录
cd /path/to/report-front/report-backend

# 2. 清理并打包 (跳过测试)
mvn clean package -DskipTests

# 3. 检查生成的jar文件
ls -la target/*.jar
```

#### 部署配置

**文件**: `application-prod.yml` (已存在，需确认)

```yaml
spring:
  datasource:
    driver-class-name: org.opengauss.Driver
    url: jdbc:opengauss://${GAUSSDB_HOST}:${GAUSSDB_PORT:5432}/${GAUSSDB_DB:report_db}
    username: ${GAUSSDB_USER}
    password: ${GAUSSDB_PASSWORD}
    druid:
      max-active: 50

  quartz:
    properties:
      org:
        quartz:
          jobStore:
            driverDelegateClass: org.quartz.impl.jdbcjobstore.PostgreSQLDelegate

ftp:
  built-in:
    enabled: true
    port: 2021
    username: rpa_user
    password: rpa_password
    root-directory: /data/ftp-root
```

#### 启动脚本

**文件**: `start-backend.sh`

```bash
#!/bin/bash

# 环境变量
export GAUSSDB_HOST=192.168.1.100
export GAUSSDB_PORT=5432
export GAUSSDB_DB=report_db
export GAUSSDB_USER=report_user
export GAUSSDB_PASSWORD=your_password_here

# JVM参数
JAVA_OPTS="-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 应用端口
SERVER_PORT=8082

# 启动应用
java $JAVA_OPTS -jar report-backend-1.0.0.jar --spring.profiles.active=prod
```

### 4.2 前端部署

#### 构建

```bash
# 1. 进入前端目录
cd /path/to/report-front/src

# 2. 安装依赖
npm install --registry=https://registry.npmmirror.com

# 3. 构建生产版本
npm run build:prod
```

#### Nginx配置

**文件**: `/etc/nginx/conf.d/report-front.conf`

```nginx
server {
    listen 8086;
    server_name _;

    # 前端静态文件
    location / {
        root /path/to/report-front/src/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端API代理
    location /api {
        proxy_pass http://127.0.0.1:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Druid监控代理
    location /druid {
        proxy_pass http://127.0.0.1:8082/druid;
        proxy_set_header Host $host;
    }
}
```

---

## 5. 兼容性验证

### 5.1 数据库兼容性验证

| 测试项 | 验证方法 | 预期结果 |
|--------|----------|----------|
| 连接测试 | psql连接GaussDB | 连接成功 |
| Schema验证 | 检查所有表是否存在 | 29张表全部存在 |
| 数据完整性 | 对比MySQL和GaussDB数据量 | 数据量一致 |
| 序列验证 | 检查自增ID是否正常 | 自增正常 |
| 触发器验证 | 验证update_time触发器 | 自动更新时间正常 |

### 5.2 应用兼容性验证

| 测试项 | 验证方法 | 预期结果 |
|--------|----------|----------|
| 启动测试 | 启动后端应用 | 启动成功，无异常 |
| 健康检查 | GET /api/health | 返回200 |
| 数据库连接池 | Druid监控页面 | 连接池正常 |
| Quartz调度 | 查看调度器状态 | 集群模式正常 |
| FTP服务 | 连接内置FTP | 连接成功 |
| API接口 | 调用各API接口 | 返回正常 |

### 5.3 功能回归测试

| 模块 | 测试项 | 验证方法 |
|------|--------|----------|
| 登录 | 管理员登录 | admin/admin123登录成功 |
| 报表配置 | 新增/编辑/删除 | CRUD操作正常 |
| 任务执行 | 手动触发任务 | 任务正常执行 |
| 打包功能 | 触发打包 | 打包正常完成 |
| 数据中心 | 查看表列表 | 列表展示正常 |
| 告警功能 | 触发告警 | 告警记录正常 |

### 5.4 验证检查清单

```markdown
## 迁移验证检查清单

### 数据库验证
- [ ] GaussDB连接成功
- [ ] 所有Schema表存在
- [ ] 数据量与MySQL一致
- [ ] 序列自增正常
- [ ] 触发器工作正常

### 应用验证
- [ ] 后端启动成功
- [ ] 健康检查通过
- [ ] Druid监控可访问
- [ ] Quartz调度正常
- [ ] 内置FTP正常

### 功能验证
- [ ] 登录功能正常
- [ ] 报表配置正常
- [ ] 任务执行正常
- [ ] 打包功能正常
- [ ] 数据中心正常

### 性能验证
- [ ] API响应时间<500ms
- [ ] 数据库连接池正常
- [ ] 内存使用正常
```

---

## 6. 回滚机制

### 6.1 回滚触发条件

| 条件 | 描述 |
|------|------|
| 启动失败 | 后端应用无法启动 |
| 功能异常 | 核心功能无法正常使用 |
| 数据丢失 | 迁移后数据不完整 |
| 性能严重下降 | 响应时间超过预期10倍 |

### 6.2 回滚步骤

**步骤1: 停止应用**

```bash
# 停止后端
pkill -f report-backend

# 停止前端Nginx
nginx -s stop
```

**步骤2: 恢复MySQL连接

修改 `application-prod.yml`:

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://<mysql_host>:3306/report_db
    username: root
    password: root123456
```

**步骤3: 启动开发环境**

```bash
# 重新启动后端连接MySQL
./start-backend.sh dev
```

**步骤4: 验证回滚**

确认开发环境功能正常。

---

## 7. 迁移时间线

### 7.1 推荐迁移窗口

| 阶段 | 时间 | 持续时间 | 备注 |
|------|------|----------|------|
| 准备阶段 | Day 1 09:00-12:00 | 3小时 | 环境检查、备份 |
| 数据库迁移 | Day 1 14:00-18:00 | 4小时 | Schema创建、数据迁移 |
| 应用部署 | Day 2 09:00-12:00 | 3小时 | 构建、部署 |
| 验证测试 | Day 2 14:00-18:00 | 4小时 | 功能回归测试 |
| 割接上线 | Day 3 09:00-12:00 | 3小时 | 切换、验证 |

### 7.2 风险预案

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| 数据迁移失败 | 高 | 保留MySQL备份，延长迁移窗口 |
| 应用启动失败 | 高 | 回滚到MySQL环境 |
| 功能异常 | 中 | 使用旧版本jar包 |
| 性能下降 | 中 | 优化SQL和JVM参数 |

---

## 8. 附录

### 8.1 环境变量清单

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| GAUSSDB_HOST | GaussDB主机地址 | 192.168.1.100 |
| GAUSSDB_PORT | GaussDB端口 | 5432 |
| GAUSSDB_DB | 数据库名 | report_db |
| GAUSSDB_USER | 数据库用户名 | report_user |
| GAUSSDB_PASSWORD | 数据库密码 | ********** |
| DRUID_USER | Druid监控用户 | admin |
| DRUID_PASSWORD | Druid监控密码 | admin123 |
| FTP_PORT | 内置FTP端口 | 2021 |
| FTP_USER | FTP用户名 | rpa_user |
| FTP_PASSWORD | FTP密码 | rpa_password |

### 8.2 关键文件路径

| 用途 | 路径 |
|------|------|
| 应用jar | /data/app/report-backend.jar |
| 前端dist | /data/app/front-dist |
| FTP根目录 | /data/ftp-root |
| 日志目录 | /data/logs |
| 备份目录 | /data/backup |

### 8.3 联系方式

| 角色 | 职责 | 联系方式 |
|------|------|----------|
| 项目负责人 | 总体协调 | - |
| DBA | 数据库迁移 | - |
| 后端开发 | 应用部署 | - |
| 前端开发 | 前端部署 | - |

---

## 9. 总结

本迁移方案提供了从MySQL到GaussDB的完整迁移路径，包括：

1. **环境准备**: 明确服务器、软件、网络要求
2. **数据库迁移**: Schema初始化 + 数据迁移策略
3. **应用部署**: 后端和前端的部署配置
4. **兼容性验证**: 完整的测试检查清单
5. **回滚机制**: 快速回滚到MySQL环境的能力

执行本方案时，请严格按照检查清单进行验证，确保迁移顺利完成。
