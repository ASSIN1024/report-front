# 报表数据处理平台 - 部署包

> **版本**: V2.0.0
> **发布日期**: 2026-05-07

## 快速开始

### 1. 环境要求

- JDK 1.8+
- Maven 3.6+
- Node.js 14+
- MySQL 8.0 或 GaussDB 5.x

### 2. 快速部署

```bash
# 解压部署包
tar -xzf report-platform-deployment-v2.0.tar.gz
cd deployment-package

# 配置环境变量
cp env.template .env
vim .env

# 初始化数据库 (MySQL)
mysql -u root -p < database/schema.sql

# 构建项目
cd backend && mvn clean package -DskipTests && cd ..
cd frontend && npm install && npm run build && cd ..

# 启动服务
./scripts/start.sh all
```

### 3. 访问服务

| 服务 | 地址 | 账号 |
|------|------|------|
| 前端 | http://localhost:8086 | - |
| 后端API | http://localhost:8082 | - |
| Druid监控 | http://localhost:8082/druid/ | admin/admin |
| FTP服务 | ftp://localhost:2021 | rpa_user/rpa_password |

**默认管理员账号**: admin / admin123

## 目录结构

```
deployment-package/
├── backend/                 # 后端 (Spring Boot 2.1.2)
├── frontend/               # 前端 (Vue 2.6)
├── database/              # 数据库脚本
│   ├── schema.sql         # MySQL Schema
│   ├── schema-gaussdb.sql # GaussDB Schema
│   └── migration/         # 增量迁移脚本
├── scripts/               # 部署脚本
├── docs/                  # 部署文档
├── env.template           # 环境变量模板
├── setup.sh              # 快速部署脚本
└── VERSION               # 版本信息
```

## 详细文档

- [部署指南](docs/deployment-guide.md) - 完整部署说明
- [数据库设计](docs/database-schema.md) - 表结构说明
- [API接口](docs/api-reference.md) - 接口文档

## 版本信息

详见 [VERSION](VERSION) 文件

## 注意事项

1. 生产环境请务必修改默认密码
2. FTP目录需要有读写权限 (chmod 755)
3. 数据库首次需要执行schema初始化
4. 支持MySQL和GaussDB两种数据库
