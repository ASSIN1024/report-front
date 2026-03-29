# API接口联调测试文档

> **测试环境**: http://localhost:8080
> **文档版本**: V1.0
> **创建日期**: 2026-03-30

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
| 1 | FTP配置新增/编辑/删除正常 | ⏳ |
| 2 | FTP连接测试接口正常 | ⏳ |
| 3 | 报表配置CRUD正常 | ⏳ |
| 4 | 列映射配置解析正常 | ⏳ |
| 5 | 文件上传触发处理正常 | ⏳ |
| 6 | 任务列表查询正常 | ⏳ |
| 7 | 任务重试/取消功能正常 | ⏳ |
| 8 | 日志记录正常 | ⏳ |
| 9 | 数据查询分页正常 | ⏳ |
| 10 | 系统配置读写正常 | ⏳ |
