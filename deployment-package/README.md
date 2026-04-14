# 报表数据处理平台 - 移植部署包

## 包内容

```
deployment-package/
├── backend/                 # 后端源代码 (Spring Boot)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/report/    # Java 源代码
│       │   └── resources/
│       │       ├── mapper/         # MyBatis 映射文件
│       │       ├── migration/      # 数据库迁移脚本
│       │       └── *.yml           # 配置文件
├── frontend/                # 前端源代码 (Vue.js)
│   ├── package.json
│   ├── vue.config.js
│   ├── public/
│   └── src/                 # Vue 源代码
├── database/                # 数据库脚本
│   ├── schema.sql           # MySQL 表结构
│   ├── schema-gaussdb.sql   # GaussDB 表结构
│   ├── quartz_tables_mysql.sql
│   ├── quartz_tables_gaussdb.sql
│   ├── init-builtin-ftp.sql
│   ├── application-template.yml      # MySQL 环境配置模板
│   └── application-prod-template.yml # GaussDB 环境配置模板
├── scripts/                 # 构建和启动脚本
│   ├── start.sh             # 前后端启动脚本
│   └── build.sh             # 项目构建脚本
├── env.template             # 环境变量模板
└── setup.sh                 # 快速部署脚本
```

## 环境要求

### 必须安装

- **JDK 1.8+** (建议 JDK 8)
- **Maven 3.6+**
- **Node.js 14+** (前端构建)
- **MySQL 5.7+** 或 **GaussDB**

### 数据库初始化

```bash
# MySQL
mysql -u root -p < database/schema.sql
mysql -u root -p < database/quartz_tables_mysql.sql

# GaussDB
psql -U postgres -d report_db -f database/schema-gaussdb.sql
psql -U postgres -d report_db -f database/quartz_tables_gaussdb.sql
```

## 快速部署

### 方式一：使用部署脚本（推荐）

```bash
# 1. 解压移植包
tar -xzf deployment-package.tar.gz
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

### 方式二：手动部署

```bash
# 1. 解压移植包
tar -xzf deployment-package.tar.gz
cd deployment-package

# 2. 初始化数据库
mysql -u root -p < database/schema.sql

# 3. 构建后端
cd backend
mvn clean package -DskipTests
cd ..

# 4. 构建前端
cd frontend
npm install
npm run build
cd ..

# 5. 启动服务
./scripts/start.sh all
```

## 配置说明

### MySQL 开发环境

编辑 `backend/src/main/resources/application-dev.yml` 或使用环境变量：

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| 数据库地址 | DB_HOST | localhost | MySQL 主机地址 |
| 数据库端口 | DB_PORT | 3306 | MySQL 端口 |
| 数据库名称 | DB_NAME | report_db | 库名 |
| 用户名 | DB_USER | root | 数据库用户 |
| 密码 | DB_PASSWORD | root123 | 数据库密码 |
| FTP 启用 | FTP_ENABLED | true | 是否启用内置 FTP |
| FTP 端口 | FTP_PORT | 9021 | FTP 服务端口 |
| Druid 用户 | DRUID_USER | admin | 监控台用户名 |
| Druid 密码 | DRUID_PASSWORD | admin | 监控台密码 |

### GaussDB 生产环境

编辑 `backend/src/main/resources/application-prod.yml` 或使用环境变量：

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| 数据库地址 | GAUSSDB_HOST | localhost | GaussDB 主机 |
| 数据库端口 | GAUSSDB_PORT | 5432 | GaussDB 端口 |
| 数据库名称 | GAUSSDB_DB | report_db | 库名 |
| 用户名 | GAUSSDB_USER | report_user | 数据库用户 |
| 密码 | GAUSSDB_PASSWORD | password | 数据库密码 |

## 启动服务

### 使用启动脚本

```bash
./scripts/start.sh all        # 启动全部服务
./scripts/start.sh backend   # 只启动后端
./scripts/start.sh frontend  # 只启动前端
./scripts/start.sh status    # 查看状态
./scripts/start.sh stop      # 停止服务
```

### 手动启动

```bash
# 后端 (端口 8082)
cd backend
java -jar target/report-backend-1.0.0.jar --spring.profiles.active=dev

# 前端开发模式 (端口 8083)
cd frontend
npm run serve
```

## 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | http://localhost:8083 |
| 后端 API | http://localhost:8082 |
| Druid 监控 | http://localhost:8082/druid/ |

## 注意事项

1. **不要直接提交 jar 包和 dist 文件** - pipeline 需要后端代码支持
2. **首次部署需要初始化数据库** - 执行 schema.sql 和 quartz 表结构脚本
3. **敏感信息处理** - 生产环境请务必修改默认密码
4. **FTP 目录** - 内置 FTP 的 root-directory 需要有读写权限