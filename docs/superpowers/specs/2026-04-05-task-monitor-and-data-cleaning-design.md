# 任务监控增强与数据清洗配置设计

**日期**: 2026-04-05
**版本**: V1.0
**状态**: 已确认

---

## 1. 概述

本设计文档描述两个功能的实现方案：
1. **手动执行任务（立即扫描）** - 在报表配置列表支持手动触发FTP扫描
2. **数据清洗规则配置** - 在列映射中为每个字段配置数据清洗规则

---

## 2. 功能1：手动执行任务（立即扫描）

### 2.1 需求描述

用户在配置好报表后，希望立即测试一次任务执行流程，而不是等待FTP定时扫描。实现方案：
- 用户先将测试文件放置到FTP目录
- 在报表配置列表点击对应配置的"立即扫描"按钮
- 系统立即触发FtpScanJob执行扫描
- 扫描结果（包括无文件匹配的情况）融入任务监控系统展示

### 2.2 界面设计

**报表配置列表页**

| 报表编码 | 报表名称 | FTP配置 | 文件模式 | 状态 | 操作 |
|---------|---------|--------|---------|------|------|
| RPT_001 | 销售报表 | 测试FTP | *.xlsx | ● 启用 | [编辑] [立即扫描] [删除] |

**操作列说明**：
- **编辑** - 蓝色链接，进入编辑页面
- **立即扫描** - 绿色链接，触发该配置的FTP扫描
- **删除** - 红色链接，删除配置

**确认弹窗**

点击"立即扫描"后弹出确认框，显示扫描信息：
- FTP服务器地址和端口
- 扫描路径
- 文件匹配模式

提示用户确保测试文件已放置到FTP目录。

### 2.3 扫描结果状态

扫描完成后在任务监控中展示结果，包含三种状态：

| 状态 | 说明 | 展示 |
|------|------|------|
| 成功 | 找到并处理文件 | ✓ 绿色，显示处理行数 |
| 无文件 | 目录中无匹配文件 | ⚠️ 橙色警告，提示放置文件 |
| 失败 | FTP连接或处理出错 | ✗ 红色，显示错误详情 |

### 2.4 后端实现

**新增API端点**：
```
POST /api/report-config/{id}/scan
```

触发指定报表配置的FTP扫描，返回任务ID用于跟踪。

**FtpScanJob改造**：
- 现有FtpScanJob逻辑不变
- 新增手动触发入口，接收reportConfigId参数
- 扫描结果写入task_execution表
- 任务名称格式：`FTP扫描-{报表名称}`

---

## 3. 功能2：数据清洗规则配置

### 3.1 需求描述

在报表配置时，可以对数据进行初步清洗，保证数据库写入不会失败。典型场景：
- decimal类型字段出现"-"等脏数据，需要替换为0
- 空字符串需要处理为null或默认值

### 3.2 数据结构设计

**ColumnMapping扩展**

```json
{
  "excelColumn": "A",
  "fieldName": "amount",
  "fieldType": "DECIMAL",
  "dateFormat": "yyyy-MM-dd",
  "scale": 2,
  "cleanRules": [
    {"pattern": "-", "replace": "0"},
    {"pattern": "N/A", "replace": ""}
  ],
  "validators": {
    "required": true,
    "positiveOnly": true
  }
}
```

**字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| cleanRules | Array | 替换规则列表 |
| cleanRules[].pattern | String | 原始值（支持精确匹配） |
| cleanRules[].replace | String | 替换值 |
| validators | Object | 验证规则（可选） |
| validators.required | Boolean | 是否必填 |
| validators.positiveOnly | Boolean | 是否必须为正数 |

### 3.3 界面设计

**列映射配置表格**

| Excel列 | 字段名 | 字段类型 | 清洗规则 | 操作 |
|---------|--------|---------|---------|------|
| A | amount | DECIMAL ▼ | 配置规则 (2条) | 删除 |
| B | create_date | DATE ▼ | 无 | 删除 |

**清洗规则配置弹窗**

- 标题显示当前字段信息（字段名、类型、来源列）
- 替换规则表格：原始值 → 替换值，支持添加多条规则
- 验证规则（可选）：必填校验、正数校验、值范围
- 底部按钮：取消、保存规则

### 3.4 后端实现

**DataProcessJob改造**

```java
// 在convertValue方法中应用清洗规则
private Object convertValue(Object value, FieldMapping mapping) {
    // 1. 应用替换规则
    if (mapping.getCleanRules() != null) {
        String strValue = String.valueOf(value);
        for (CleanRule rule : mapping.getCleanRules()) {
            if (strValue.equals(rule.getPattern())) {
                value = rule.getReplace();
                strValue = String.valueOf(value);
            }
        }
    }

    // 2. 类型转换
    return doConvert(value, mapping.getFieldType());
}
```

---

## 4. 功能3：列映射JSON导入

### 4.1 需求描述

支持通过JSON文本批量导入列映射配置，适用于：
- 100+列的大型报表配置
- 配置迁移和备份恢复

### 4.2 导入流程

**三步骤向导**：

1. **步骤1 - 输入JSON**
   - 大文本区域支持粘贴大型JSON
   - 显示JSON格式提示

2. **步骤2 - 格式校验**
   - JSON语法验证
   - 结构完整性验证（必填字段检查）
   - 数据类型验证
   - 错误定位：显示行号和具体错误原因

3. **步骤3 - 预览确认**
   - 显示解析后的配置预览表格
   - 汇总信息（列数、清洗规则数等）
   - 确认导入按钮
   - 成功/失败反馈

### 4.3 错误提示示例

```
✗ 校验失败 - 第15行

{
  "excelColumn": "P",
  "fieldName": "amount",    ← 第15行
  "fieldType": "DECIMALL",  ← 错误: 无效的类型 "DECIMALL"
  "cleanRules": [{"pattern": "-"}]
}

修复建议: 将 "DECIMALL" 改为 "DECIMAL"，有效类型包括: STRING, INTEGER, DECIMAL, DATE, DATETIME
```

### 4.4 后端实现

**新增API端点**：
```
POST /api/report-config/{id}/column-mapping/validate
  Request: { "json": "..." }
  Response: { "valid": true/false, "errors": [...] }

POST /api/report-config/{id}/column-mapping/import
  Request: { "json": "..." }
  Response: { "success": true/false, "imported": 25 }
```

---

## 5. 数据库变更

### 5.1 列映射表扩展

现有 `column_mapping` JSON字段结构扩展，支持新增 `cleanRules` 和 `validators` 字段。

**无需数据库 schema 变更**，通过 JSON 结构扩展实现。

---

## 6. API设计

### 6.1 立即扫描

```
POST /api/report-config/{id}/scan
Response: {
  "success": true,
  "taskId": 123456,
  "message": "扫描任务已创建"
}
```

### 6.2 清洗规则配置

```
PUT /api/report-config/{id}/column-mapping
Request: {
  "mappings": [
    {
      "excelColumn": "A",
      "fieldName": "amount",
      "fieldType": "DECIMAL",
      "cleanRules": [{"pattern": "-", "replace": "0"}]
    }
  ]
}
Response: {
  "success": true,
  "updated": 5
}
```

### 6.3 JSON导入

```
POST /api/report-config/{id}/column-mapping/validate
Request: { "json": "[...]" }
Response: {
  "valid": false,
  "errors": [
    {"line": 15, "message": "无效的类型 DECIMALL", "suggestion": "改为 DECIMAL"}
  ]
}

POST /api/report-config/{id}/column-mapping/import
Request: { "json": "[...]" }
Response: {
  "success": true,
  "imported": 25,
  "warnings": []
}
```

---

## 7. 任务监控融合

所有扫描任务（包括立即扫描和定时扫描）统一通过task_execution表管理：

| 字段 | 说明 |
|------|------|
| taskType | FTP_SCAN |
| taskName | FTP扫描-{报表名称} |
| status | PENDING / RUNNING / SUCCESS / FAILED / NO_FILE |
| errorMessage | 失败原因（无文件时显示提示信息） |

---

## 8. 设计确认

| 功能 | 状态 |
|------|------|
| 立即扫描位置（操作列） | ✓ 已确认 |
| 清洗规则配置弹窗 | ✓ 已确认 |
| JSON导入向导 | ✓ 已确认 |
| 扫描结果状态展示 | ✓ 已确认 |
