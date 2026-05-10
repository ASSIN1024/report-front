# 报表数据处理平台 - 部署指南

> **文档版本**: V2.0
> **创建日期**: 2026-05-07
> **维护人**: AI Assistant

---

## 1. 部署包概述

### 1.1 包内容

```
deployment-package/
├── backend/                      # 后端源代码 (Spring Boot 2.1.2)
│   ├── pom.xml                  # Maven配置文件
│   └── src/
│       └── main/
│           ├── java/com/report/ # Java源代码
│           └── resources/
│               ├── mapper/      # MyBatis映射文件
│               ├── migration/   # 数据库迁移脚本
│               ├── schema.sql           # MySQL Schema
│               ├── schema-gaussdb.sql   # GaussDB Schema
│               ├── application.yml       # 主配置
│               ├── application-dev.yml   # 开发环境配置
│               └── application-prod.yml  # 生产环境配置
│
├── frontend/                    # 前端源代码 (Vue 2.6)
│   ├── package.json
│   ├── vue.config.js
│   ├── public/
│   └── src/                     # Vue源代码
│
├── database/                    # 数据库脚本
│   ├── schema.sql               # MySQL完整Schema (28张表)
│   ├── schema-gaussdb.sql       # GaussDB完整Schema
│   ├── quartz_tables_mysql.sql   # Quartz MySQL表
│   ├── quartz_tables_gaussdb.sql # Quartz GaussDB表
│   ├── init-builtin-ftp.sql      # 内置FTP初始化
│   ├── application-template.yml  # MySQL配置模板
│   ├── application-prod-template.yml # GaussDB配置模板
│   └── migration/               # 数据库迁移脚本
│       ├── V1.0__create_processed_file.sql
│       ├── V1.0__ftp_simplification.sql
│       ├── V1.1__add_batch_no_and_file_path_to_processed_file.sql
│       ├── V1.1__add_skip_columns_and_date_pattern.sql
│       ├── V1.2__add_trigger_execution_log.sql
│       ├── V1.3__add_process_monitor_log.sql
│       └── V1.4__add_missing_tables_for_gaussdb.sql
│
├── scripts/                     # 构建和启动脚本
│   ├── start.sh                # 服务启动脚本
│   └── build.sh                # 项目构建脚本
│
├── docs/                        # 部署文档
│   ├── deployment-guide.md      # 详细部署指南
│   ├── database-schema.md       # 数据库设计文档
│   ├── api-reference.md         # API接口文档
│   └── troubleshooting.md       # 故障排查指南
│
├── env.template                 # 环境变量模板
├── setup.sh                     # 快速部署脚本
├── VERSION                      # 版本信息
└── README.md                    # 本文件
```

### 1.2 版本信息

| 组件 | 版本 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 2.1.2 | Java 1.8+ |
| ORM | MyBatis-Plus 3.x | - |
| 前端框架 | Vue 2.6 | - |
| UI组件 | Element UI 2.x | - |
| 数据库 | MySQL 8.0 / GaussDB 5.x | - |
| 定时任务 | Quartz 2.3.x | JDBC集群模式 |

---

## 2. 环境要求

### 2.1 软件要求

| 软件 | 最低版本 | 推荐版本 | 说明 |
|------|----------|----------|------|
| JDK | 1.8.0_100 | 1.8.0_272+ | OpenJDK或Oracle JDK |
| Maven | 3.6.0 | 3.8.x | 后端构建 |
| Node.js | 14.x | 16.x | 前端构建 |
| MySQL | 5.7 | 8.0 | 开发环境 |
| GaussDB | 5.0 | 5.0.0+ | 生产环境 |
| Nginx | 1.16 | 1.20+ | 前端反向代理 |

### 2.2 硬件要求

#### 开发环境 (单实例)

| 资源 | 最低 | 推荐 |
|------|------|------|
| CPU | 2核 | 4核 |
| 内存 | 4GB | 8GB |
| 磁盘 | 50GB | 100GB |

#### 生产环境 (集群)

| 资源 | 最低 x 2 | 推荐 x 2 |
|------|----------|----------|
| CPU | 4核 | 8核 |
| 内存 | 8GB | 16GB |
| 磁盘 | 100GB | 200GB SSD |

---

## 3. 快速部署

### 3.1 方式一：使用部署脚本（推荐）

```bash
# 1. 解压部署包
tar -xzf report-platform-deployment-v2.0.tar.gz
cd deployment-package

# 2. 配置环境变量
cp env.template .env
vim .env  # 编辑数据库等配置

# 3. 执行部署
source .env
./setup.sh mysql   # MySQL 环境
# 或
./setup.sh gaussdb # GaussDB 环境
```

### 3.2 方式二：手动部署

#### 步骤1: 初始化数据库

```bash
# MySQL环境
mysql -u root -p < database/schema.sql
mysql -u root -p < database/quartz_tables_mysql.sql
mysql -u root -p < database/init-builtin-ftp.sql

# GaussDB环境
psql -U postgres -d report_db -f database/schema-gaussdb.sql
psql -U postgres -d report_db -f database/quartz_tables_gaussdb.sql
psql -U postgres -d report_db -f database/migration/V1.4__add_missing_tables_for_gaussdb.sql
```

#### 步骤2: 构建后端

```bash
cd backend
mvn clean package -DskipTests
cd ..
```

#### 步骤3: 构建前端

```bash
cd frontend
npm install --registry=https://registry.npmmirror.com
npm run build
cd ..
```

#### 步骤4: 配置并启动

```bash
# 复制并编辑配置
cp database/application-dev.yml backend/src/main/resources/application-dev.yml
vim backend/src/main/resources/application-dev.yml

# 启动服务
./scripts/start.sh all
```

---

## 4. 详细配置说明

### 4.1 数据库配置

编辑 `backend/src/main/resources/application-dev.yml` (开发环境) 或 `application-prod.yml` (生产环境):

```yaml
spring:
  datasource:
    # MySQL配置
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/report_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

    # 或 GaussDB配置
    # driver-class-name: org.opengauss.Driver
    # url: jdbc:opengauss://localhost:5432/report_db
    # username: gauss_user
    # password: gauss_password

    druid:
      max-active: 50
```

### 4.2 FTP配置

```yaml
ftp:
  built-in:
    enabled: true           # 是否启用内置FTP
    port: 2021             # FTP端口
    username: rpa_user     # FTP用户名
    password: rpa_password # FTP密码
    root-directory: /data/ftp-root  # FTP根目录(需有读写权限)
    idle-timeout: 300      # 空闲超时(秒)
    max-connections: 10   # 最大连接数
```

### 4.3 Quartz调度配置

```yaml
spring:
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: always
    properties:
      org:
        quartz:
          scheduler:
            instanceName: ReportScheduler
            instanceId: AUTO
          jobStore:
            isClustered: true        # 集群模式
            clusterCheckinInterval: 20000  # 心跳间隔(ms)
            misfireThreshold: 60000   # Misfire阈值(ms)
```

---

## 5. 服务管理

### 5.1 启动脚本

```bash
./scripts/start.sh all        # 启动全部服务
./scripts/start.sh backend   # 只启动后端
./scripts/start.sh frontend  # 只启动前端
./scripts/start.sh status    # 查看状态
./scripts/start.sh stop      # 停止服务
./scripts/start.sh restart   # 重启服务
```

### 5.2 手动启动

```bash
# 后端 (端口 8082)
cd backend
java -Xms4g -Xmx4g -jar target/report-backend-1.0.0.jar --spring.profiles.active=dev

# 前端 (端口 8086)
cd frontend
npm run serve
# 或使用构建后的静态文件通过Nginx服务
```

### 5.3 Nginx配置

```nginx
server {
    listen 8086;
    server_name _;

    location / {
        root /path/to/frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 6. 访问地址

| 服务 | 地址 | 默认账号 |
|------|------|----------|
| 前端页面 | http://localhost:8086 | - |
| 后端API | http://localhost:8082 | - |
| Druid监控 | http://localhost:8082/druid/ | admin/admin |
| 内置FTP | ftp://localhost:2021 | rpa_user/rpa_password |

---

## 7. 目录结构要求

部署前需创建以下目录并授权:

```bash
# 创建目录
mkdir -p /data/ftp-root/upload
mkdir -p /data/ftp-root/for-upload
mkdir -p /data/ftp-root/done
mkdir -p /data/logs
mkdir -p /data/backup

# 授权
chmod -R 755 /data
chown -R appuser:appuser /data
```

---

## 8. 验证检查清单

部署完成后，请验证以下项目:

- [ ] 数据库连接正常
- [ ] 后端启动无异常
- [ ] 前端页面可访问
- [ ] 登录功能正常 (admin/admin123)
- [ ] FTP连接正常
- [ ] Quartz调度正常
- [ ] 告警功能正常

---

## 9. 故障排查

### 9.1 数据库连接失败

```bash
# 检查MySQL服务
systemctl status mysql

# 检查端口
netstat -tlnp | grep 3306

# 测试连接
mysql -u root -p -h localhost -P 3306
```

### 9.2 后端启动失败

```bash
# 查看日志
tail -f logs/backend.log

# 检查端口占用
netstat -tlnp | grep 8082

# 检查JDK版本
java -version
```

### 9.3 前端无法访问

```bash
# 检查Nginx状态
systemctl status nginx

# 检查日志
tail -f /var/log/nginx/error.log

# 检查端口
netstat -tlnp | grep 8086
```

---

## 10. 回滚操作

如需回滚到之前版本:

```bash
# 1. 停止服务
./scripts/start.sh stop

# 2. 恢复数据库 (如有备份)
mysql -u root -p report_db < /data/backup/report_db_YYYYMMDD.sql

# 3. 恢复应用
mv backend backend_v2
mv backend_v1 backend

# 4. 重启服务
./scripts/start.sh all
```

---

## 11. 联系支持

如有问题，请联系:

- 项目负责人: -
- 技术支持: -
- 文档版本: V2.0 (2026-05-07)
