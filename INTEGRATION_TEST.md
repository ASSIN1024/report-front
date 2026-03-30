# API接口联调测试文档

> **测试环境**: http://localhost:8082
> **文档版本**: V1.1
> **创建日期**: 2026-03-30
> **最后更新**: 2026-03-31

---

## 1. FTP配置接口测试

### 1.1 分页查询
```
GET /api/ftp/config/page?pageNum=1&pageSize=10

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [...],
        "total": 1,
        "size": 10,
        "current": 1
    }
}
```

### 1.2 新增配置
```
POST /api/ftp/config
Content-Type: application/json

{
    "configName": "测试FTP",
    "host": "192.168.1.100",
    "port": 21,
    "username": "ftpuser",
    "password": "ftppass",
    "scanPath": "/data/reports",
    "filePattern": "*.xlsx",
    "scanInterval": 300,
    "status": 1,
    "remark": "测试用"
}

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

### 1.3 测试连接
```
POST /api/ftp/config/test/{id}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": true
}
```

### 1.4 删除配置
```
DELETE /api/ftp/config/{id}

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

---

## 2. 报表配置接口测试

### 2.1 分页查询
```
GET /api/report/config/page?pageNum=1&pageSize=10

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [...],
        "total": 1
    }
}
```

### 2.2 新增配置
```
POST /api/report/config
Content-Type: application/json

{
    "reportCode": "SALES_REPORT",
    "reportName": "销售报表",
    "ftpConfigId": 1,
    "filePattern": "sales_*.xlsx",
    "sheetIndex": 0,
    "headerRow": 0,
    "dataStartRow": 1,
    "columnMappings": [
        {
            "excelColumn": "A",
            "fieldName": "order_id",
            "fieldType": "STRING"
        },
        {
            "excelColumn": "B",
            "fieldName": "product_name",
            "fieldType": "STRING"
        }
    ],
    "outputTable": "t_sales_data",
    "outputMode": "APPEND",
    "status": 1
}

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

### 2.3 文件上传
```
POST /api/report/config/upload
Content-Type: multipart/form-data

file: <Excel文件>
reportConfigId: 1

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": 1
}
```

---

## 3. 任务接口测试

### 3.1 分页查询
```
GET /api/task/page?pageNum=1&pageSize=10

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 1,
                "taskType": "FTP_SCAN",
                "taskName": "FTP扫描-销售报表",
                "status": "SUCCESS",
                "totalRows": 100,
                "successRows": 98,
                "failedRows": 2
            }
        ]
    }
}
```

### 3.2 任务重试
```
POST /api/task/retry/{id}

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

### 3.3 任务取消
```
POST /api/task/cancel/{id}

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

---

## 4. 日志接口测试

### 4.1 查询日志
```
GET /api/log/page?taskExecutionId=1&pageNum=1&pageSize=20

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 1,
                "logLevel": "INFO",
                "logMessage": "开始处理文件: sales.xlsx",
                "createTime": "2026-03-30 10:00:00"
            }
        ]
    }
}
```

---

## 5. 数据接口测试

### 5.1 获取输出表列表
```
GET /api/data/tables

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": ["t_sales_data", "t_order_data"]
}
```

### 5.2 获取表字段
```
GET /api/data/columns?tableName=t_sales_data

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": ["pt_dt", "order_id", "product_name", "quantity"]
}
```

### 5.3 分页查询数据
```
POST /api/data/query
Content-Type: application/json

{
    "tableName": "t_sales_data",
    "pageNum": 1,
    "pageSize": 20
}

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [...],
        "total": 100,
        "size": 20,
        "current": 1
    }
}
```

---

## 6. 系统配置接口测试

### 6.1 分页查询
```
GET /api/system/config/page?pageNum=1&pageSize=20

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {...}
}
```

### 6.2 获取配置值
```
GET /api/system/config?configKey=scan_interval

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": "300"
}
```

### 6.3 设置配置值
```
POST /api/system/config?configKey=scan_interval&configValue=600

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

---

## 7. 联调测试检查清单

| 编号 | 检查项 | 状态 |
|------|--------|------|
| 1 | FTP配置新增/编辑/删除正常 | ✅ |
| 2 | FTP连接测试接口正常 | ✅ |
| 3 | 报表配置CRUD正常 | ⏳ |
| 4 | 列映射配置解析正常 | ⏳ |
| 5 | 文件上传触发处理正常 | ⏳ |
| 6 | 任务列表查询正常 | ⏳ |
| 7 | 任务重试/取消功能正常 | ⏳ |
| 8 | 日志记录正常 | ✅ |
| 9 | 数据查询分页正常 | ⏳ |
| 10 | 系统配置读写正常 | ⏳ |

---

## 8. FTP功能测试结果

### 8.1 测试环境

| 组件 | 版本/配置 |
|------|----------|
| 后端服务 | Spring Boot 2.1.2, JDK 1.8, 端口8082 |
| 前端服务 | Vue 2.6.14, 端口8081 |
| FTP服务器 | Docker vsftpd, 主机网络模式 |
| 数据库 | MySQL 5.7+ |

### 8.2 FTP配置接口测试

#### 8.2.1 分页查询配置

```bash
GET /api/ftp/config/page?pageNum=1&pageSize=10

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": "2038633032830828546",
                "configName": "Docker FTP测试配置",
                "host": "127.0.0.1",
                "port": 21,
                "username": "ftpuser",
                "password": "ftppass",
                "scanPath": "/",
                "filePattern": "*.xlsx",
                "scanInterval": 300,
                "status": 1
            }
        ],
        "total": 3
    }
}
```

**测试结果**: ✅ 通过

#### 8.2.2 新增配置

```bash
POST /api/ftp/config
Content-Type: application/json

{
    "configName": "测试FTP配置",
    "host": "192.168.1.100",
    "port": 21,
    "username": "testuser",
    "password": "testpass",
    "scanPath": "/data",
    "filePattern": "*.xlsx",
    "scanInterval": 300,
    "status": 1,
    "remark": "测试用"
}

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

**测试结果**: ✅ 通过

#### 8.2.3 连接测试

```bash
POST /api/ftp/config/test/2038633032830828546

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": true
}
```

**测试结果**: ✅ 通过

#### 8.2.4 修改配置

```bash
PUT /api/ftp/config
Content-Type: application/json

{
    "id": "2038633032830828546",
    "configName": "Docker FTP测试配置-已修改",
    "host": "127.0.0.1",
    "port": 21,
    "username": "ftpuser",
    "password": "ftppass",
    "scanPath": "/data",
    "status": 1
}

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

**测试结果**: ✅ 通过

#### 8.2.5 删除配置

```bash
DELETE /api/ftp/config/2038645014615240706

Response:
{
    "code": 200,
    "message": "操作成功"
}
```

**测试结果**: ✅ 通过

### 8.3 问题修复记录

| 问题 | 原因 | 解决方案 | 状态 |
|------|------|----------|------|
| FTP连接测试返回false | Docker FTP被动模式配置问题 | 使用主机网络模式重建容器 | ✅ 已修复 |
| 前端显示"配置不存在" | JavaScript Long精度丢失 | Jackson Long转String配置 | ✅ 已修复 |
| StatusTag状态颜色错误 | 状态映射配置反向 | 修正映射关系 | ✅ 已修复 |

---

## 9. 操作日志接口测试

### 9.1 分页查询日志

```bash
GET /api/operation/log/page?pageNum=1&pageSize=10

Response:
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": "2038649075167121410",
                "module": "FTP配置",
                "operationType": "TEST",
                "operationDesc": "测试FTP连接",
                "targetId": "2038633032830828546",
                "targetName": "Docker FTP测试配置",
                "beforeData": "{...密码脱敏...}",
                "result": 1,
                "duration": 59,
                "createTime": 1774886790000
            }
        ],
        "total": 5
    }
}
```

**测试结果**: ✅ 通过

### 9.2 按条件筛选

```bash
GET /api/operation/log/page?module=FTP配置&operationType=TEST&result=1

Response:
{
    "code": 200,
    "data": {
        "records": [...],
        "total": 3
    }
}
```

**测试结果**: ✅ 通过

---

## 10. 日志文件接口测试

### 10.1 获取日志文件列表

```bash
GET /api/log/file/list

Response:
{
    "code": 200,
    "data": [
        {
            "logType": "error",
            "fileName": "report-platform-error.log",
            "fileSize": 7091,
            "lastModified": 1774886790514
        },
        {
            "logType": "all",
            "fileName": "report-platform.log",
            "fileSize": 16443,
            "lastModified": 1774886790514
        },
        {
            "logType": "operation",
            "fileName": "report-platform-operation.log",
            "fileSize": 545,
            "lastModified": 1774886790496
        }
    ]
}
```

**测试结果**: ✅ 通过

### 10.2 查询日志内容

```bash
GET /api/log/file/query?logType=all&level=ERROR&pageNum=1&pageSize=10

Response:
{
    "code": 200,
    "data": {
        "logType": "all",
        "total": 5,
        "lines": [
            {
                "lineNumber": 125,
                "timestamp": "2026-03-31 00:06:30.514",
                "level": "ERROR",
                "logger": "c.r.aspect.OperationLogAspect",
                "message": "操作日志记录..."
            }
        ]
    }
}
```

**测试结果**: ✅ 通过

---

## 11. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 |
|------|------|----------|--------|
| 2026-03-30 | V1.0 | 初始版本创建 | - |
| 2026-03-31 | V1.1 | 添加FTP功能测试结果 | AI Assistant |
| 2026-03-31 | V1.1 | 添加操作日志接口测试 | AI Assistant |
| 2026-03-31 | V1.1 | 添加日志文件接口测试 | AI Assistant |
