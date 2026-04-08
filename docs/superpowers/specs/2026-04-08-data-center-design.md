# 数据中心 - 表管理功能设计

**文档版本**: V1.0
**创建日期**: 2026-04-08
**状态**: 待用户确认

---

## 一、需求概述

### 1.1 背景

当前项目通过报表配置、Pipeline、Trigger 产出大量的数据表。当前每次产生的表都需要使用数据库管理软件去查看，希望能在前端页面对数据库进行分层标记和管理，并方便地查看表里的数据。

### 1.2 需求选择

| 维度 | 选择 |
|------|------|
| 分层方式 | 混合（流向分层 + 来源分类） |
| 查看功能 | 只读浏览（分页 + 条件筛选） |
| 权限控制 | 无限制，所有登录用户可查看 |
| 管理能力 | 只读管理 + 标记管理 |
| 界面布局 | 左侧筛选面板 + 右侧表列表和详情 |
| 自动发现 | 半自动（扫描发现 + 手动标记） |
| 菜单位置 | 独立一级菜单"数据中心" |

---

## 二、数据库设计

### 2.1 新建表：table_layer_mapping

```sql
CREATE TABLE table_layer_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    table_name VARCHAR(100) NOT NULL COMMENT '数据库表名',
    table_layer VARCHAR(20) COMMENT '流向分层: ODS/DWD/DWS/ADS',
    source_type VARCHAR(50) COMMENT '来源类型: FTP来源/TRIGGER/PIPELINE/MANUAL',
    source_id BIGINT COMMENT '关联来源ID',
    source_name VARCHAR(200) COMMENT '关联来源名称',
    business_domain VARCHAR(200) COMMENT '业务域描述（如：个人贷款、各项贷款、财务报表）',
    description VARCHAR(500) COMMENT '表描述',
    tags JSON COMMENT '自定义标签',
    marked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已标记: 0-未标记, 1-已标记',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表分层映射表';
```

### 2.2 枚举值说明

**流向分层（table_layer）**

| 值 | 说明 |
|----|------|
| ODS | 原始数据层（Operational Data Store） |
| DWD | 明细数据层（Wide Data / Data Warehouse Detail） |
| DWS | 汇总数据层（Data Warehouse Summary） |
| ADS | 应用数据层（Application Data Store） |

**来源类型（source_type）**

| 值 | 说明 |
|----|------|
| FTP来源 | FTP配置导入的表 |
| TRIGGER | Trigger 触发产生的表 |
| PIPELINE | Pipeline 执行产生的表 |
| MANUAL | 手动创建的表 |

---

## 三、前端设计

### 3.1 目录结构

```
前端 (Vue)
└── views/data-center/
    ├── DataCenterLayout.vue     # 整体布局
    ├── Index.vue               # 入口页面
    ├── FilterPanel.vue          # 左侧筛选面板
    │   ├── 按流向分层 ▼        # ODS / DWD / DWS / ADS / 全部
    │   ├── 按来源类型 ▼        # FTP来源 / TRIGGER / PIPELINE / MANUAL / 全部
    │   └── 按业务域 ▼          # 输入框，模糊匹配
    ├── TableTree.vue            # 表树（已标记表列表）
    ├── TableList.vue            # 表列表（右侧）
    ├── TableDetail.vue          # 表详情面板
    │   ├── 基本信息（表名、分层、业务域、描述、标签）
    │   ├── 关联信息（来源类型、来源名称）
    │   ├── 字段信息（字段名、类型、注释）
    │   └── 数据预览（分页 + 筛选）
    ├── UntaggedTables.vue       # 待标记表
    └── EditTableModal.vue       # 编辑标记弹窗
```

### 3.2 交互说明

**组合筛选**
- 三个筛选维度（流向分层、来源类型、业务域）可以自由组合
- 筛选条件之间是 AND 关系
- 业务域支持模糊匹配输入

**左侧面板**
- 顶部：三个维度筛选条件
- 下方：已标记表的树形列表

**右侧区域**
- 上方：表列表（卡片形式展示）
- 下方：选中表的详情面板（Tab 切换：基本信息 / 字段信息 / 数据预览）

---

## 四、后端设计

### 4.1 目录结构

```
后端 (Spring Boot)
└── com.report.controller/
    └── DataCenterController     # 数据中心 API
└── com.report.service/
    ├── DataCenterService       # 数据中心服务
    └── TableScanService        # 表扫描服务
└── com.report.mapper/
    ├── TableLayerMappingMapper # MyBatis Mapper
    ├── TriggerConfigMapper     # 关联查询 Trigger
    └── PipelineConfigMapper    # 关联查询 Pipeline
└── com.report.entity/
    └── TableLayerMapping      # 表分层映射实体
```

### 4.2 主要 API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/data-center/tables | GET | 获取表列表（支持多维度筛选） |
| /api/data-center/tables/{tableName} | GET | 获取表详情 |
| /api/data-center/tables/{tableName}/columns | GET | 获取表字段信息 |
| /api/data-center/tables/{tableName}/data | GET | 获取表数据（分页+筛选） |
| /api/data-center/tables/{tableName} | PUT | 更新表标记信息 |
| /api/data-center/untagged | GET | 获取未标记表 |
| /api/data-center/scan | POST | 扫描新表 |
| /api/data-center/filter-options | GET | 获取筛选选项枚举值 |

---

## 五、数据预览功能

### 5.1 分页浏览

- 每页固定条数（默认 20 条）
- 支持上一页/下一页/跳转指定页

### 5.2 条件筛选

- 用户输入筛选条件（SQL WHERE 子句）
- 后端进行安全校验，防止 SQL 注入
- 示例：`customer_name LIKE '%张三%'`

---

## 六、自动发现机制

### 6.1 扫描策略

- 系统定期扫描数据库，发现新表
- 新表自动插入 `table_layer_mapping`，`marked=0`
- 通过 `source_type` 和 `source_id` 关联来源信息

### 6.2 待标记表入口

- 所有 `marked=0` 的表汇总在"待标记"入口
- 用户可以批量或单个进行标记

---

## 七、关联信息展示

### 7.1 来源信息

通过 `source_type` 和 `source_id` 关联查询：

| source_type | 关联表 | 查询内容 |
|-------------|--------|----------|
| FTP来源 | ftp_config | 配置名称、服务器地址 |
| TRIGGER | trigger_config | 触发器名称、监听目标表 |
| PIPELINE | pipeline_config | 流水线名称、描述 |
| MANUAL | - | 无关联 |

### 7.2 关联 Pipeline/Trigger

在表详情面板的"关联信息"区域，展示：
- 来源类型和来源名称
- 关联的 Trigger 信息（如有）
- 关联的 Pipeline 信息（如有）

---

## 八、待标记表管理

### 8.1 统一入口

- 所有 `marked=0` 的表汇总在一个列表
- 显示表名、扫描时间、可能的来源类型

### 8.2 标记操作

- 用户选择表 → 编辑弹窗 → 填写分层、业务域、描述、标签
- 标记完成后 `marked=1`，表从待标记列表移除

---

## 九、页面原型

详见：[data-center-prototype.html](./data-center-prototype.html)

原型地址：http://localhost:8899/docs/superpowers/specs/data-center-prototype.html

---

## 十、技术实现要点

1. **SQL 注入防护**：数据筛选条件需进行严格校验
2. **分页查询**：使用 MyBatis-Plus 的分页插件
3. **表结构缓存**：定期缓存表结构信息，减少数据库查询
4. **扫描任务**：可配置为定时任务或手动触发
