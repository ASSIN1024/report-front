# API接口文档

> **文档版本**: V1.1
> **创建日期**: 2026-03-30
> **最后更新**: 2026-03-30
> **基础路径**: http://localhost:8082/api

---

## 1. 接口概述

### 1.1 基础信息

| 项目 | 说明 |
|------|------|
| 基础路径 | /api |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | 无 (预留) |

### 1.2 统一响应格式

```json
{
    "code": 200,
    "message": "操作成功",
    "data": {},
    "timestamp": 1709136000000
}
```

### 1.3 响应码说明

| 响应码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 1.4 错误码定义

| 错误码 | 说明 |
|--------|------|
| 1001 | FTP连接错误 |
| 1002 | 文件解析错误 |
| 1003 | 数据校验错误 |
| 1004 | 数据库操作错误 |

---

## 2. FTP配置接口

### 2.1 分页查询

**请求**
```
GET /api/ftp/config/page
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码 (默认1) |
| pageSize | Integer | 否 | 每页条数 (默认10) |
| configName | String | 否 | 配置名称 (模糊匹配) |
| status | Integer | 否 | 状态 (1=启用, 0=禁用) |

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 1,
                "configName": "测试FTP",
                "host": "192.168.1.100",
                "port": 21,
                "username": "ftpuser",
                "password": "******",
                "scanPath": "/data/reports",
                "filePattern": "*.xlsx",
                "scanInterval": 300,
                "status": 1,
                "remark": "",
                "deleted": 0,
                "createTime": "2026-03-29 10:00:00",
                "updateTime": "2026-03-29 10:00:00"
            }
        ],
        "total": 1,
        "size": 10,
        "current": 1,
        "pages": 1
    }
}
```

### 2.2 获取启用列表

**请求**
```
GET /api/ftp/config/list/enabled
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": [
        {
            "id": 1,
            "configName": "测试FTP",
            "host": "192.168.1.100",
            "port": 21
        }
    ]
}
```

### 2.3 获取详情

**请求**
```
GET /api/ftp/config/{id}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": 1,
        "configName": "测试FTP",
        "host": "192.168.1.100",
        "port": 21,
        "username": "ftpuser",
        "password": "ftppass",
        "scanPath": "/data/reports",
        "filePattern": "*.xlsx",
        "scanInterval": 300,
        "status": 1,
        "remark": "测试用FTP配置",
        "deleted": 0,
        "createTime": "2026-03-29 10:00:00",
        "updateTime": "2026-03-29 10:00:00"
    }
}
```

### 2.4 新增配置

**请求**
```
POST /api/ftp/config
Content-Type: application/json
```

**请求体**
```json
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
    "remark": "测试用FTP配置"
}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

### 2.5 更新配置

**请求**
```
PUT /api/ftp/config
Content-Type: application/json
```

**请求体**
```json
{
    "id": 1,
    "configName": "测试FTP",
    "host": "192.168.1.100",
    "port": 21,
    "username": "ftpuser",
    "password": "newpass",
    "scanPath": "/data/reports",
    "filePattern": "*.xlsx",
    "scanInterval": 300,
    "status": 1,
    "remark": "更新备注"
}
```

### 2.6 删除配置

**请求**
```
DELETE /api/ftp/config/{id}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

### 2.7 测试连接

**请求**
```
POST /api/ftp/config/test/{id}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": true
}
```

---

## 3. 报表配置接口

### 3.1 分页查询

**请求**
```
GET /api/report/config/page
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码 (默认1) |
| pageSize | Integer | 否 | 每页条数 (默认10) |
| reportName | String | 否 | 报表名称 (模糊匹配) |
| status | Integer | 否 | 状态 (1=启用, 0=禁用) |

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 1,
                "reportCode": "SALES_REPORT",
                "reportName": "销售报表",
                "ftpConfigId": 1,
                "ftpConfigName": "测试FTP",
                "filePattern": "sales_*.xlsx",
                "sheetIndex": 0,
                "headerRow": 0,
                "dataStartRow": 1,
                "columnMappings": [
                    {
                        "excelColumn": "A",
                        "fieldName": "order_id",
                        "fieldType": "STRING"
                    }
                ],
                "outputTable": "t_sales_data",
                "outputMode": "APPEND",
                "status": 1,
                "remark": ""
            }
        ],
        "total": 1,
        "size": 10,
        "current": 1
    }
}
```

### 3.2 获取详情

**请求**
```
GET /api/report/config/{id}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": 1,
        "reportCode": "SALES_REPORT",
        "reportName": "销售报表",
        "ftpConfigId": 1,
        "ftpConfigName": "测试FTP",
        "filePattern": "sales_*.xlsx",
        "sheetIndex": 0,
        "headerRow": 0,
        "dataStartRow": 1,
        "columnMappings": [
            {
                "excelColumn": "A",
                "fieldName": "order_id",
                "fieldType": "STRING",
                "dateFormat": "",
                "scale": null
            }
        ],
        "outputTable": "t_sales_data",
        "outputMode": "APPEND",
        "status": 1,
        "remark": "销售数据报表配置"
    }
}
```

### 3.3 新增配置

**请求**
```
POST /api/report/config
Content-Type: application/json
```

**请求体**
```json
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
            "fieldType": "STRING",
            "dateFormat": "",
            "scale": null
        }
    ],
    "outputTable": "t_sales_data",
    "outputMode": "APPEND",
    "status": 1,
    "remark": "销售数据报表配置"
}
```

### 3.4 更新配置

**请求**
```
PUT /api/report/config
Content-Type: application/json
```

### 3.5 删除配置

**请求**
```
DELETE /api/report/config/{id}
```

### 3.6 立即扫描

手动触发指定报表配置的FTP扫描。

**请求**
```
POST /api/report/config/{id}/scan
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": 123456
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| data | Long | 任务ID |

**错误响应**
```json
{
    "code": 400,
    "message": "报表配置不存在或未启用"
}
```

### 3.7 列映射JSON校验

校验列映射JSON格式和完整性。

**请求**
```
POST /api/report/config/{id}/column-mapping/validate
Content-Type: application/json
```

**请求体**
```json
{
    "json": "[{\"excelColumn\":\"A\",\"fieldName\":\"amount\",\"fieldType\":\"DECIMAL\"}]"
}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "valid": true,
        "errors": [],
        "count": 1
    }
}
```

**校验失败响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "valid": false,
        "errors": [
            {"line": 1, "message": "无效的字段类型: DECIMALL", "suggestion": "有效类型: STRING, INTEGER, DECIMAL, DATE, DATETIME"}
        ],
        "count": 0
    }
}
```

### 3.8 列映射JSON导入

导入列映射JSON配置到报表。

**请求**
```
POST /api/report/config/{id}/column-mapping/import
Content-Type: application/json
```

**请求体**
```json
{
    "json": "[{\"excelColumn\":\"A\",\"fieldName\":\"amount\",\"fieldType\":\"DECIMAL\",\"cleanRules\":[{\"pattern\":\"-\",\"replace\":\"0\"}]}]"
}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "success": true,
        "imported": 1
    }
}
```

---

## 4. 任务接口

### 4.1 分页查询

**请求**
```
GET /api/task/page
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码 (默认1) |
| pageSize | Integer | 否 | 每页条数 (默认10) |
| taskType | String | 否 | 任务类型 |
| taskName | String | 否 | 任务名称 |
| status | String | 否 | 状态 |

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 1,
                "taskType": "FTP_SCAN",
                "taskName": "FTP扫描任务",
                "reportConfigId": 1,
                "fileName": "sales_20260329.xlsx",
                "filePath": "/data/reports/sales_20260329.xlsx",
                "status": "SUCCESS",
                "totalRows": 1000,
                "successRows": 998,
                "failedRows": 2,
                "errorMessage": "",
                "startTime": "2026-03-29 10:00:00",
                "endTime": "2026-03-29 10:01:30",
                "duration": 90000
            }
        ],
        "total": 1,
        "size": 10,
        "current": 1
    }
}
```

### 4.2 任务重试

**请求**
```
POST /api/task/retry/{id}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

### 4.3 任务取消

**请求**
```
POST /api/task/cancel/{id}
```

### 4.4 删除任务

**请求**
```
DELETE /api/task/{id}
```

---

## 5. 日志接口

### 5.1 分页查询

**请求**
```
GET /api/log/page
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| taskExecutionId | Long | 是 | 任务执行ID |
| pageNum | Integer | 否 | 页码 (默认1) |
| pageSize | Integer | 否 | 每页条数 (默认20) |

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 1,
                "taskExecutionId": 1,
                "logLevel": "INFO",
                "logMessage": "开始处理文件: sales_20260329.xlsx",
                "createTime": "2026-03-29 10:00:00"
            }
        ],
        "total": 10,
        "size": 20,
        "current": 1
    }
}
```

### 5.2 获取日志列表

**请求**
```
GET /api/log/list/{taskExecutionId}
```

---

## 6. 数据实体

### 6.1 FtpConfig

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| configName | String | 配置名称 |
| host | String | FTP主机 |
| port | Integer | 端口 |
| username | String | 用户名 |
| password | String | 密码 |
| scanPath | String | 扫描路径 |
| filePattern | String | 文件匹配模式 |
| scanInterval | Integer | 扫描间隔(秒) |
| status | Integer | 状态 |
| remark | String | 备注 |
| deleted | Integer | 删除标记 |
| createTime | Date | 创建时间 |
| updateTime | Date | 更新时间 |

### 6.2 ReportConfig

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| reportCode | String | 报表编码 |
| reportName | String | 报表名称 |
| ftpConfigId | Long | FTP配置ID |
| filePattern | String | 文件匹配模式 |
| sheetIndex | Integer | Sheet索引 |
| headerRow | Integer | 表头行号 |
| dataStartRow | Integer | 数据起始行 |
| columnMapping | String | 列映射配置(JSON) |
| outputTable | String | 输出表名 |
| outputMode | String | 输出模式 |
| status | Integer | 状态 |

### 6.3 TaskExecution

| 字段 | 类型 | 说明 |
|------|------|------|
| id | Long | 主键ID |
| taskType | String | 任务类型 |
| taskName | String | 任务名称 |
| reportConfigId | Long | 报表配置ID |
| fileName | String | 文件名 |
| filePath | String | 文件路径 |
| status | String | 状态 |
| totalRows | Integer | 总行数 |
| successRows | Integer | 成功行数 |
| failedRows | Integer | 失败行数 |
| errorMessage | String | 错误信息 |
| startTime | Date | 开始时间 |
| endTime | Date | 结束时间 |
| duration | Long | 执行时长(毫秒) |

---

## 7. 分页功能说明

### 7.1 分页组件实现

**组件位置**: `src/components/Pagination.vue`

**功能特性**:
- 支持页码切换
- 支持每页显示数量调整 (10/20/50/100)
- 支持跳转到指定页
- 显示总记录数

**使用方式**:
```vue
<pagination
  :current-page="pagination.pageNum"
  :page-size="pagination.pageSize"
  :total="pagination.total"
  @pagination="handlePagination"
/>
```

**事件处理**:
```javascript
handlePagination({ page, rows }) {
  this.pagination.pageNum = page
  this.pagination.pageSize = rows
  this.loadData()
}
```

### 7.2 后端分页返回格式

```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [...],
        "total": "21",
        "size": 10,
        "current": 1,
        "pages": 3
    }
}
```

**注意事项**:
- `total`字段返回的是字符串类型，前端需要使用`parseInt()`转换
- 分页参数: `pageNum`(页码), `pageSize`(每页条数)

---

## 8. 变更记录

| 日期 | 版本 | 变更内容 | 责任人 | 关联任务ID |
|------|------|----------|--------|------------|
| 2026-03-30 | V1.0 | [新增] 初始版本创建 | - | - |
| 2026-03-30 | V1.1 | [修改] 更新基础路径端口(8080→8082) | - | TASK-007 |
| 2026-04-01 | V1.2 | [新增] 分页功能实现说明 | AI Assistant | PAGE-001 |
| 2026-04-07 | V1.3 | [新增] Pipeline/Trigger API 文档 | AI Assistant | H-PIPELINE-IMPL |
| 2026-04-08 | V1.4 | [新增] 数据中心 API 文档 | AI Assistant | H-DATA-CENTER |

---

## 9. Pipeline 接口

### 9.1 获取所有 Pipeline

**请求**
```
GET /api/pipeline
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "pipelines": ["sales_data_pipeline"]
    }
}
```

### 9.2 执行 Pipeline

手动触发 Pipeline 执行。

**请求**
```
POST /api/pipeline/{code}/execute
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | String | 是 | Pipeline 编码 |
| partitionDate | String | 否 | 分区日期 (格式: yyyy-MM-dd，默认当天) |

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "taskId": 123456,
        "pipelineCode": "sales_data_pipeline",
        "partitionDate": "2026-04-07",
        "status": "EXECUTED"
    }
}
```

---

## 10. Trigger 接口

### 10.1 获取所有触发器

**请求**
```
GET /api/trigger
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": [
        {
            "id": 1,
            "triggerCode": "osd_sales_trigger",
            "triggerName": "OSD销售表触发器",
            "sourceTable": "osd_sales",
            "partitionColumn": "pt_dt",
            "pollIntervalSeconds": 60,
            "maxRetries": 60,
            "pipelineCode": "sales_data_pipeline",
            "status": "ENABLED"
        }
    ]
}
```

### 10.2 获取触发器详情

**请求**
```
GET /api/trigger/{code}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "triggerCode": "osd_sales_trigger",
        "triggerName": "OSD销售表触发器",
        "sourceTable": "osd_sales",
        "partitionColumn": "pt_dt",
        "pollIntervalSeconds": 60,
        "maxRetries": 60,
        "pipelineCode": "sales_data_pipeline",
        "status": "ENABLED"
    }
}
```

### 10.3 测试触发器

测试触发器是否能检测到数据。

**请求**
```
POST /api/trigger/{code}/test
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "triggerCode": "osd_sales_trigger",
        "partitionDate": "2026-04-07",
        "dataCount": 5,
        "hasData": true
    }
}
```

---

## 11. 数据中心接口

### 11.1 获取表列表

获取已标记的数据表列表，支持多维度筛选。

**请求**
```
GET /api/data-center/tables
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| tableLayer | String | 否 | 流向分层 (ODS/DWD/DWS/ADS) |
| sourceType | String | 否 | 来源类型 (FTP来源/TRIGGER/PIPELINE/MANUAL) |
| businessDomain | String | 否 | 业务域 (模糊匹配) |

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": [
        {
            "id": 1,
            "tableName": "ods_sales",
            "tableLayer": "ODS",
            "sourceType": "FTP来源",
            "sourceId": 1,
            "sourceName": "测试FTP",
            "businessDomain": "销售",
            "description": "销售原始数据",
            "tags": "[\"月度\",\"贷前\"]",
            "marked": 1,
            "createTime": "2026-04-08 10:00:00",
            "updateTime": "2026-04-08 10:00:00"
        }
    ]
}
```

### 11.2 获取表详情

**请求**
```
GET /api/data-center/tables/{tableName}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "id": 1,
        "tableName": "ods_sales",
        "tableLayer": "ODS",
        "sourceType": "FTP来源",
        "sourceId": 1,
        "sourceName": "测试FTP",
        "businessDomain": "销售",
        "description": "销售原始数据",
        "tags": "[\"月度\",\"贷前\"]",
        "marked": 1
    }
}
```

### 11.3 获取表字段信息

**请求**
```
GET /api/data-center/tables/{tableName}/columns
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": [
        {
            "columnName": "id",
            "dataType": "bigint",
            "columnComment": "主键ID"
        },
        {
            "columnName": "order_no",
            "dataType": "varchar",
            "columnComment": "订单编号"
        },
        {
            "columnName": "amount",
            "dataType": "decimal",
            "columnComment": "金额"
        }
    ]
}
```

### 11.4 获取表数据（分页预览）

**请求**
```
GET /api/data-center/tables/{tableName}/data
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| pageNum | Integer | 否 | 页码 (默认1) |
| pageSize | Integer | 否 | 每页条数 (默认20) |
| condition | String | 否 | 筛选条件 (如: name LIKE '%张三%') |

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "records": [
            {
                "id": 1,
                "order_no": "LN202401001",
                "customer_name": "张三",
                "amount": 100000.00,
                "pt_dt": "2024-01-15"
            }
        ],
        "total": 100,
        "size": 20,
        "current": 1,
        "pages": 5
    }
}
```

### 11.5 更新表标记信息

**请求**
```
PUT /api/data-center/tables
Content-Type: application/json
```

**请求体**
```json
{
    "id": 1,
    "tableName": "ods_sales",
    "tableLayer": "ODS",
    "sourceType": "FTP来源",
    "businessDomain": "销售",
    "description": "销售原始数据",
    "tags": "[\"月度\",\"贷前\"]"
}
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

### 11.6 获取未标记表

**请求**
```
GET /api/data-center/untagged
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": [
        {
            "id": 2,
            "tableName": "ods_customer",
            "marked": 0,
            "createTime": "2026-04-08 12:00:00"
        }
    ]
}
```

### 11.7 扫描新表

扫描数据库中发现的新表（符合命名规范的 ods/dwd/dws/ads/dim/mid/tmp 前缀）。

**请求**
```
POST /api/data-center/scan
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": ["ods_new_table", "dwd_summary"]
}
```

---

## 12. 内置FTP接口

### 12.1 获取配置

**请求**
```
GET /api/built-in-ftp/config
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "enabled": true,
        "port": 2021,
        "username": "rpa_user",
        "rootDirectory": "/data/ftp-root",
        "maxConnections": 10,
        "idleTimeout": 300,
        "passiveMode": true
    }
}
```

### 12.2 更新配置

**请求**
```
PUT /api/built-in-ftp/config
Content-Type: application/json
```

**请求体**
```json
{
    "enabled": true,
    "port": 2021,
    "username": "rpa_user",
    "password": "new_password",
    "rootDirectory": "/data/ftp-root",
    "maxConnections": 10,
    "idleTimeout": 300,
    "passiveMode": true
}
```

### 12.3 启动FTP服务

**请求**
```
POST /api/built-in-ftp/start
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": null
}
```

### 12.4 停止FTP服务

**请求**
```
POST /api/built-in-ftp/stop
```

### 12.5 获取服务状态

**请求**
```
GET /api/built-in-ftp/status
```

**响应**
```json
{
    "code": 200,
    "message": "操作成功",
    "data": {
        "running": true,
        "port": 2021,
        "startTime": "2026-04-08 10:00:00"
    }
}
```
