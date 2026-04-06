# 报表数据处理平台

> 轻量化数据处理中间件 | RPA数据上传 → 本平台 → BI报表数据

[!\[Spring Boot\](https://img.shields.io/badge/Spring%20Boot-2.1.2-green.svg null)](https://spring.io/projects/spring-boot)
[!\[Vue.js\](https://img.shields.io/badge/Vue.js-2.6-blue.svg null)](https://vuejs.org/)
[!\[License\](https://img.shields.io/badge/License-MIT-yellow.svg null)](LICENSE)

***

## 📋 目录

- [项目简介](#项目简介)
- [核心功能](#核心功能)
- [系统架构](#系统架构)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [安装部署](#安装部署)
- [使用指南](#使用指南)
- [配置说明](#配置说明)
- [API接口](#api接口)
- [内置FTP服务](#内置ftp服务)
- [常见问题](#常见问题)
- [开发指南](#开发指南)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

***

## 项目简介

### 是什么？

报表数据处理平台是一个**轻量化的数据处理中间件**，作为RPA机器人数据上传和BI报表之间的中间处理环节。

### 解决什么问题？

```
┌─────────┐    ┌─────────┐    ┌───────────────┐    ┌─────────┐
│ RPA机器人 │ → │ FTP服务器 │ → │ 报表数据处理平台 │ → │ BI报表系统 │
└─────────┘    └─────────┘    └───────────────┘    └─────────┘
                                    ↑
                              本项目填补这个环节
```

**痛点场景**：

- RPA机器人抓取数据后上传到FTP
- BI系统需要标准化的数据库数据
- FTP中的Excel文件无法直接被BI系统使用

**本平台价值**：

- ✅ 自动监听FTP指定文件夹
  - ✅ 读取并解析Excel文件
- ✅ 数据清洗和格式转换
- ✅ 生成BI报表可用的数据表
- ✅ 历史数据版本化管理

***

## 核心功能

| 功能          | 说明                 |
| ----------- | ------------------ |
| FTP配置管理     | 管理多个外部FTP服务器连接     |
| 报表配置管理      | 配置报表模板、列映射、数据清洗规则  |
| 任务调度        | Quartz定时扫描FTP目录    |
| 手动触发        | 支持手动触发任务执行         |
| 数据清洗        | 支持值替换、空值处理等清洗规则    |
| 列映射JSON导入   | 批量导入列映射配置          |
| 任务监控        | 实时查看任务执行状态和进度      |
| 操作日志        | 记录所有操作便于追溯         |
| **内置FTP服务** | 内嵌FTP服务器，支持RPA直接上传 |
| 去重机制        | 避免重复处理相同文件         |

***

## 系统架构

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        报表数据处理平台                           │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐           ┌─────────────────┐            │
│  │    前端 (Vue)    │           │   后端 (Spring Boot)  │        │
│  │  ┌───────────┐  │    HTTP   │  ┌─────────────┐  │            │
│  │  │FTP配置页面│  │ ←───────→ │  │FtpConfigController│          │
│  │  │报表配置页面│  │           │  │ReportConfigController│       │
│  │  │任务监控页面│  │           │  │TaskController    │          │
│  │  │日志列表页面│  │           │  │BuiltInFtpConfigController│   │
│  │  └───────────┘  │           │  └─────────────┘  │            │
│  └─────────────────┘           └────────┬──────────┘            │
│                                          │                       │
│  ┌───────────────────────────────────────▼───────────────────┐    │
│  │                      服务层 (Service)                      │    │
│  │  ┌────────────┐  ┌────────────┐  ┌────────────────┐    │    │
│  │  │FtpConfigService│ │ReportConfigService│ │TaskService  │    │    │
│  │  │BuiltInFtpConfigService│ │DataProcessService│         │    │    │
│  │  └────────────┘  └────────────┘  └────────────────┘    │    │
│  └───────────────────────────────────────┬─────────────────┘    │
│                                          │                       │
│  ┌───────────────────────────────────────▼───────────────────┐    │
│  │                      数据层 (MyBatis-Plus)                  │    │
│  │  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────────────┐    │    │
│  │  │FtpConfig│ │ReportConfig│ │TaskExecution│ │BuiltInFtpConfig│ │    │
│  │  └────────┘ └────────┘ └────────┘ └────────────────┘    │    │
│  └───────────────────────────────────────┬─────────────────┘    │
│                                          │                       │
│  ┌───────────────────────────────────────▼───────────────────┐    │
│  │                    任务调度 (Quartz)                         │    │
│  │  ┌────────────────┐    ┌─────────────────┐                │    │
│  │  │   FtpScanJob   │    │  EmbeddedFtpServer │              │    │
│  │  │  扫描外部FTP   │    │  内置FTP服务器    │                │    │
│  │  └────────────────┘    └─────────────────┘                │    │
│  └───────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
           ↓                                    ↓
    ┌──────────────┐                    ┌──────────────┐
    │ MySQL数据库  │                    │ FTP服务器    │
    │ - ftp_config │                    │ (外部FTP)    │
    │ - report_config│                  └──────────────┘
    │ - task_execution│
    │ - built_in_ftp_config│
    └──────────────┘
```

### 数据流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        数据处理流程                                  │
└─────────────────────────────────────────────────────────────────────┘

1. FTP扫描
┌──────────┐    ┌──────────────┐    ┌──────────────┐
│ FTP目录  │ → │  FtpScanJob   │ → │ 匹配文件名   │
│ /data/   │    │ 定时扫描      │    │ *.xlsx       │
└──────────┘    └──────────────┘    └──────┬───────┘
                                          ↓

2. 文件处理
┌──────────┐    ┌──────────────┐    ┌──────────────┐
│ 下载文件 │ → │ DataProcessJob │ → │ 解析Excel   │
│          │    │              │    │              │
└──────────┘    └──────┬───────┘    └──────┬───────┘
                       ↓

3. 数据清洗
┌──────────┐    ┌──────────────┐    ┌──────────────┐
│ 列映射   │ → │ 数据类型转换  │ → │ 值替换清洗   │
│ A→order_id│   │ STRING/INTEGER│   │ '-' → '0'   │
└──────────┘    └──────────────┘    └──────┬───────┘
                                          ↓

4. 数据入库
┌──────────┐    ┌──────────────┐    ┌──────────────┐
│ 目标表   │ ← │ TableCreator  │ ← │ INSERT数据   │
│ t_sales  │    │ 自动建表     │    │              │
└──────────┘    └──────────────┘    └──────────────┘
                       ↓
                  ┌──────────────┐
                  │ 历史版本字段  │
                  │ pt_dt (分区) │
                  └──────────────┘
```

### 内置FTP架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    内置FTP服务架构                                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────┐         ┌──────────────────────────────────┐
│  RPA机器人   │  FTP    │      EmbeddedFtpServer            │
│             │ ──────→ │  ┌────────────────────────────┐  │
│  客户端      │         │  │    Apache FtpServer 1.2.0   │  │
└─────────────┘         │  │    被动模式 / 主动模式        │  │
                        │  └────────────┬───────────────┘  │
                        │               │                   │
                        │  ┌────────────▼───────────────┐  │
                        │  │     BuiltInFtpConfig       │  │
                        │  │  - 用户名: rpa_user        │  │
                        │  │  - 密码: rpa_password      │  │
                        │  │  - 端口: 2021              │  │
                        │  │  - 根目录: /data/ftp-root  │  │
                        │  └────────────┬───────────────┘  │
                        └───────────────┼───────────────────┘
                                        ↓
                               ┌────────────────┐
                               │  本地文件系统   │
                               │  /data/ftp-root│
                               │  └── upload/   │
                               │      ├── *.xlsx│
                               │      └── *.xls │
                               └────────────────┘
```

***

## 技术栈

### 后端

| 技术               | 版本     | 说明      |
| ---------------- | ------ | ------- |
| Spring Boot      | 2.1.2  | 应用框架    |
| MyBatis-Plus     | 3.4.3  | ORM框架   |
| Apache FtpServer | 1.2.0  | 内置FTP服务 |
| Quartz           | 内置     | 任务调度    |
| Druid            | 1.1.20 | 数据库连接池  |
| Apache POI       | 4.1.2  | Excel解析 |
| Hutool           | 5.8.25 | 工具库     |
| commons-net      | 3.9.0  | FTP客户端  |
| JDK              | 1.8    | Java运行时 |

### 前端

| 技术         | 版本   | 说明      |
| ---------- | ---- | ------- |
| Vue.js     | 2.6  | 前端框架    |
| Vue Router | 3.x  | 路由管理    |
| Vuex       | 3.x  | 状态管理    |
| Element UI | 2.x  | UI组件库   |
| Axios      | 0.21 | HTTP客户端 |

### 数据库

| 技术    | 版本   | 说明    |
| ----- | ---- | ----- |
| MySQL | 5.7+ | 关系数据库 |

***

## 快速开始

### 环境要求

- JDK 1.8+
- Node.js 14+ (前端开发)
- MySQL 5.7+
- Maven 3.6+

### 1. 克隆项目

```bash
git clone https://github.com/ASSIN1024/report-front.git
cd report-front
```

### 2. 配置数据库

创建MySQL数据库：

```sql
CREATE DATABASE report_db DEFAULT CHARACTER SET utf8mb4;
```

修改 `report-backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/report_db?useUnicode=true&characterEncoding=utf-8
    username: your_username
    password: your_password
```

### 3. 初始化数据库

```bash
mysql -u root -p report_db < report-backend/src/main/resources/schema.sql
```

### 4. 启动后端

```bash
cd report-backend
./mvnw spring-boot:run
# 或使用脚本
cd ../scripts && ./start.sh backend
```

后端启动后运行在 <http://localhost:8082>

### 5. 启动前端

```bash
cd src
npm install
npm run serve
```

前端启动后访问 <http://localhost:8080>

### 6. 验证安装

访问 <http://localhost:8080，使用默认账号登录，查看系统是否正常运行。>

***

## 安装部署

### 后端部署

#### 1. 修改生产配置

编辑 `application.yml` 或创建 `application-prod.yml`：

```yaml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:mysql://生产数据库地址:3306/report_db
    username: 生产用户名
    password: 生产密码
```

#### 2. 构建JAR包

```bash
cd report-backend
mvn clean package -DskipTests
```

生成的JAR包：`target/report-backend-1.0.0.jar`

#### 3. 运行服务

```bash
java -jar target/report-backend-1.0.0.jar --spring.profiles.active=prod
```

#### 4. 创建FTP目录

```bash
mkdir -p /data/ftp-root/upload
chmod 755 /data/ftp-root
```

### 前端部署

#### 1. 构建生产版本

```bash
cd src
npm run build
```

生成的文件在 `src/dist` 目录

#### 2. 配置nginx

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        root /path/to/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://localhost:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

***

## 使用指南

### 1. 添加FTP配置

进入 **FTP配置** 页面，点击 **新增配置**：

| 字段    | 说明      | 示例            |
| ----- | ------- | ------------- |
| 配置名称  | 标识名称    | 测试FTP         |
| FTP地址 | 服务器IP   | 192.168.1.100 |
| 端口    | FTP端口   | 21            |
| 用户名   | FTP用户名  | ftpuser       |
| 密码    | FTP密码   | ftppass       |
| 扫描路径  | 监控目录    | /data/reports |
| 文件匹配  | 文件名模式   | \*.xlsx       |
| 扫描间隔  | 扫描频率(秒) | 300           |

### 2. 添加报表配置

进入 **报表配置** 页面，点击 **新增报表**：

| 字段      | 说明            |
| ------- | ------------- |
| 报表编码    | 唯一标识代码        |
| 报表名称    | 显示名称          |
| 关联FTP   | 关联的FTP配置      |
| 文件匹配    | 匹配模式          |
| Sheet索引 | Excel Sheet序号 |
| 表头行     | 表头所在行         |
| 数据起始行   | 数据开始行         |
| 列映射     | 列与字段映射        |
| 输出表名    | 目标数据库表        |
| 输出模式    | 追加/覆盖         |

### 3. 配置列映射

点击 **列映射配置**，设置Excel列与数据库字段的对应关系：

```json
[
  {"excelColumn": "A", "fieldName": "order_id", "fieldType": "STRING"},
  {"excelColumn": "B", "fieldName": "product_name", "fieldType": "STRING"},
  {"excelColumn": "C", "fieldName": "quantity", "fieldType": "INTEGER"},
  {"excelColumn": "D", "fieldName": "amount", "fieldType": "DECIMAL"},
  {"excelColumn": "E", "fieldName": "order_date", "fieldType": "DATE"}
]
```

支持JSON导入，点击 **导入JSON** 按钮粘贴配置。

### 4. 配置数据清洗规则

在列映射中可以配置清洗规则：

| 规则类型 | 示例          | 说明     |
| ---- | ----------- | ------ |
| 值替换  | `-` → `0`   | 将特定值替换 |
| 空值处理 | NULL → `""` | 处理空值   |
| 去空格  | trim        | 去除首尾空格 |

### 5. 手动触发任务

在 **报表配置列表**，点击对应报表的 **立即扫描** 按钮：

1. 系统先检查FTP指定目录
2. 匹配文件名模式的文件会被下载
3. 解析Excel并进行数据清洗
4. 写入目标数据库表
5. 完成后可在任务监控查看结果

### 6. 查看任务日志

进入 **任务监控** 页面：

- 查看所有任务的执行状态
- 点击任务查看详细日志
- 支持按状态、时间筛选

***

## 配置说明

### 外部FTP配置

通过页面配置，存储在 `ftp_config` 表：

| 配置项          | 说明       | 默认值     |
| ------------ | -------- | ------- |
| enabled      | 是否启用     | true    |
| host         | FTP服务器地址 | -       |
| port         | 端口       | 21      |
| username     | 用户名      | -       |
| password     | 密码（加密存储） | -       |
| scanPath     | 扫描路径     | /       |
| filePattern  | 文件匹配模式   | \*.xlsx |
| scanInterval | 扫描间隔(秒)  | 300     |

### 内置FTP配置

通过页面配置或 `built_in_ftp_config` 表：

| 配置项              | 说明      | 默认值            |
| ---------------- | ------- | -------------- |
| enabled          | 是否启用    | false          |
| port             | 监听端口    | 2021           |
| username         | 用户名     | rpa\_user      |
| password         | 密码      | rpa\_password  |
| rootDirectory    | 根目录     | /data/ftp-root |
| maxConnections   | 最大连接数   | 10             |
| idleTimeout      | 空闲超时(秒) | 300            |
| passiveMode      | 被动模式    | true           |
| passivePortStart | 被动端口起始  | 50000          |
| passivePortEnd   | 被动端口结束  | 50100          |

### 报表配置

存储在 `report_config` 表：

| 配置项           | 说明               |
| ------------- | ---------------- |
| reportCode    | 报表唯一编码           |
| reportName    | 报表名称             |
| ftpConfigId   | 关联的FTP配置ID       |
| filePattern   | 文件名匹配模式          |
| sheetIndex    | Excel Sheet索引    |
| headerRow     | 表头行号             |
| dataStartRow  | 数据起始行            |
| columnMapping | 列映射JSON          |
| outputTable   | 输出表名             |
| outputMode    | APPEND/OVERWRITE |

***

## API接口

### FTP配置

| 接口                          | 方法     | 说明        |
| --------------------------- | ------ | --------- |
| `/api/ftp-config/page`      | GET    | 分页查询FTP配置 |
| `/api/ftp-config/{id}`      | GET    | 获取配置详情    |
| `/api/ftp-config`           | POST   | 新增FTP配置   |
| `/api/ftp-config`           | PUT    | 更新FTP配置   |
| `/api/ftp-config/{id}`      | DELETE | 删除FTP配置   |
| `/api/ftp-config/{id}/test` | POST   | 测试连接      |

### 报表配置

| 接口                                | 方法     | 说明       |
| --------------------------------- | ------ | -------- |
| `/api/report-config/page`         | GET    | 分页查询报表配置 |
| `/api/report-config/{id}`         | GET    | 获取报表详情   |
| `/api/report-config`              | POST   | 新增报表配置   |
| `/api/report-config`              | PUT    | 更新报表配置   |
| `/api/report-config/{id}`         | DELETE | 删除报表配置   |
| `/api/report-config/{id}/trigger` | POST   | 手动触发任务   |

### 任务管理

| 接口                    | 方法  | 说明     |
| --------------------- | --- | ------ |
| `/api/task/page`      | GET | 分页查询任务 |
| `/api/task/{id}`      | GET | 获取任务详情 |
| `/api/task/{id}/logs` | GET | 获取任务日志 |

### 内置FTP

| 接口                         | 方法   | 说明        |
| -------------------------- | ---- | --------- |
| `/api/built-in-ftp/config` | GET  | 获取内置FTP配置 |
| `/api/built-in-ftp/config` | PUT  | 更新内置FTP配置 |
| `/api/built-in-ftp/start`  | POST | 启动内置FTP服务 |
| `/api/built-in-ftp/stop`   | POST | 停止内置FTP服务 |
| `/api/built-in-ftp/status` | GET  | 获取服务状态    |

***

## 内置FTP服务

### 什么是内置FTP？

内置FTP是本平台自带的FTP服务器功能，无需依赖外部FTP即可接收RPA文件上传。

### 适用场景

- ✅ RPA机器人直接上传文件到本系统
- ✅ 作为外部FTP的备份方案
- ✅ 内网环境，简化部署

### 使用步骤

#### 1. 启用内置FTP

进入系统管理 → 内置FTP配置：

```yaml
enabled: true
port: 2021
username: rpa_user
password: rpa_password
rootDirectory: /data/ftp-root
```

#### 2. 创建必要目录

```bash
mkdir -p /data/ftp-root/upload
```

#### 3. 启动服务

点击 **启动FTP服务** 按钮，或调用API：

```bash
curl -X POST http://localhost:8082/api/built-in-ftp/start
```

#### 4. RPA连接配置

RPA端FTP连接配置：

| 配置项   | 值             |
| ----- | ------------- |
| 服务器地址 | 本系统地址         |
| 端口    | 2021          |
| 用户名   | rpa\_user     |
| 密码    | rpa\_password |
| 上传目录  | /upload       |

### 与外部FTP的区别

| 特性 | 外部FTP      | 内置FTP   |
| -- | ---------- | ------- |
| 部署 | 需要单独FTP服务器 | 随系统启动   |
| 管理 | 外部管理       | 平台统一管理  |
| 适用 | 已有FTP基础设施  | 新建或备份方案 |
| 端口 | 21         | 2021    |

***

## 常见问题

### Q1: 任务执行失败，日志显示"FTP连接失败"

**原因**：FTP服务器不可达或配置错误

**解决**：

1. 检查FTP服务器IP和端口
2. 确认FTP服务已启动
3. 测试用户名密码是否正确
4. 检查防火墙是否开放FTP端口

### Q2: 文件已上传但未被处理

**原因**：文件名不匹配或扫描未执行

**解决**：

1. 确认文件名符合 filePattern（如 `*.xlsx`）
2. 检查任务监控是否有扫描记录
3. 确认FTP配置已启用
4. 手动触发一次扫描测试

### Q3: Excel解析报错"Invalid header"

**原因**：表头行号配置错误

**解决**：

1. 检查报表配置的 `headerRow` 是否正确
2. 确认Excel文件格式完整
3. 检查是否有多余的空行

### Q4: 数据清洗规则不生效

**原因**：规则配置格式错误

**解决**：

1. JSON格式必须正确
2. 确认字段名匹配
3. 查看任务日志中的清洗详情

### Q5: 内置FTP无法启动

**原因**：端口被占用或目录权限不足

**解决**：

1. 检查端口2021是否被占用：`netstat -an | grep 2021`
2. 确认 `/data/ftp-root` 目录存在且有权限
3. 查看后台日志具体错误信息

### Q6: 如何查看详细日志？

**方法**：

1. **页面日志**：任务监控 → 点击任务 → 查看日志
2. **文件日志**：
   ```bash
   tail -f logs/report-backend.log
   ```
3. **数据库日志**：
   ```sql
   SELECT * FROM task_execution_log WHERE task_execution_id = #{taskId};
   ```

***

## 开发指南

### 项目结构

```
report-front/
├── report-backend/           # 后端项目
│   └── src/main/java/com/report/
│       ├── controller/       # 控制器层
│       ├── service/          # 服务层
│       │   └── impl/          # 服务实现
│       ├── job/               # 定时任务
│       ├── entity/            # 实体类
│       ├── mapper/            # 数据访问层
│       ├── util/              # 工具类
│       ├── config/            # 配置类
│       └── common/            # 公共类
├── src/                      # 前端项目
│   ├── views/                # 页面组件
│   ├── api/                  # API接口
│   ├── router/               # 路由配置
│   ├── store/                # 状态管理
│   └── components/           # 公共组件
├── scripts/                  # 脚本
│   └── start.sh             # 启动脚本
├── harness/                 # 项目管理
└── docs/                    # 项目文档
```

### 添加新功能

1. **后端新增Controller**
   ```java
   @RestController
   @RequestMapping("/api/your-feature")
   public class YourFeatureController {
       // 实现
   }
   ```
2. **新增Service**
   ```java
   public interface YourService {}
   @Service
   public class YourServiceImpl implements YourService {}
   ```
3. **前端新增页面**
   ```vue
   <template>
     <div>Your Feature Page</div>
   </template>
   ```

### 运行测试

```bash
cd report-backend
mvn test
```

### 代码规范

- 控制器层只做参数校验和响应封装
- 业务逻辑在Service层
- 数据库操作在Mapper层
- 工具类放在util包

***

## 贡献指南

### 提交规范

```
<type>(<scope>): <description>

- feat: 新功能
- fix: 修复bug
- docs: 文档变更
- refactor: 重构
- test: 测试相关
- chore: 构建/工具
```

示例：

```
feat(ftp): 添加内置FTP服务

添加Apache FtpServer实现内置FTP功能
关联任务: H-FTP-INTEGRATION
```

### 开发流程

1. Fork 本仓库
2. 创建特性分支 `git checkout -b feature/your-feature`
3. 提交变更 `git commit -m 'feat: add some feature'`
4. 推送分支 `git push origin feature/your-feature`
5. 创建 Pull Request

### 测试要求

- 新功能必须有单元测试
- 修改后需运行回归测试
- API接口需测试验证

***

## 许可证

本项目基于 [MIT 许可证](LICENSE) 开源。

***

## 联系方式

- 项目主页：<https://github.com/ASSIN1024/report-front>
- 问题反馈：<https://github.com/ASSIN1024/report-front/issues>

***

## 变更记录

| 日期         | 版本   | 变更内容        |
| ---------- | ---- | ----------- |
| 2026-04-05 | V1.0 | 初始版本发布      |
| 2026-04-05 | V1.1 | 新增内置FTP服务模块 |
| 2026-04-06 | V1.2 | 修复内置FTP MyBatis映射问题，新增配置驱动自动启动 |

