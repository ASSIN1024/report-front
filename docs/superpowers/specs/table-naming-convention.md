# 数据仓库表命名规范

> **版本**: V1.0
> **创建日期**: 2026-04-08
> **状态**: 已确认

---

## 一、规范总览

| 分层 | 前缀 | 说明 |
|------|------|------|
| 原始数据层 | `ods_` | 来自源系统的原始数据 |
| 明细数据层 | `dwd_` | 标准化的明细数据 |
| 汇总数据层 | `dws_` | 轻度汇总数据 |
| 应用数据层 | `ads_` | 最终应用数据 |
| 维度表层 | `dim_` | 维度表 |
| 中间数据层 | `mid_` | 各层级之间的过渡/中间表 |
| 临时数据层 | `tmp_` | 临时计算，用完即删 |

---

## 二、系统表命名

系统表统一使用 `sys_` 前缀：

```
sys_ftp_config
sys_trigger_config
sys_task_execution
sys_operation_log
```

**注意**：系统表不纳入数据资产管理范围。

---

## 三、命名格式

```
{分层前缀}_{业务域}_{主题}_{版本?}

示例：
ods_sales_202401          # 销售原始数据 2024年1月
dwd_customer_base         # 客户明细基础表
dws_sales_daily           # 销售每日汇总
ads_finance_monthly       # 财务月度报表
dim_product               # 产品维度表
mid_sales_clean           # 销售数据清洗中间表
tmp_monthly_calc          # 月度计算临时表
```

---

## 四、命名规则

1. **全小写**，单词间用下划线分隔
2. **不超过 50 字符**
3. **避免缩写**
   - ✅ `amount`, `count`, `customer`
   - ❌ `amt`, `cnt`, `cust`
4. **版本号用 `_v1`, `_v2` 后缀**
5. **日期分区用 `_YYYYMMDD` 后缀**

---

## 五、中间表说明

### mid_（中间数据层）

用于各层级之间的过渡/中间表：

| 场景 | 示例 |
|------|------|
| ODS → DWD 清洗过渡 | `mid_sales_clean` |
| DWD → DWS 聚合中间 | `mid_order_aggregate` |
| DWD → DIM 维度构建 | `mid_customer_dim_build` |
| 任意层级再加工 | `mid_monthly_recal` |

### tmp_（临时数据层）

用于临时计算，用完即删的表：

| 场景 | 示例 |
|------|------|
| 月度汇总临时计算 | `tmp_monthly_summary_calc` |
| 复杂查询中间结果 | `tmp_complex_join_result` |

---

## 六、不规范表命名示例

| 原表名 | 建议新名称 | 问题 |
|--------|-----------|------|
| `osd_sales` | `ods_sales` | 拼写错误 |
| `osd_nanping` | `ods_nanping` | 拼写错误 |
| `layer_1_sales` | `dws_sales_summary_v1` | 不规范前缀 |
| `layer_2_summary` | `dws_summary_layer2` | 不规范前缀 |
| `t_sales_data` | `ods_sales_data` | 缺少分层前缀 |
| `t_customer_info` | `dim_customer_info` | 缺少分层前缀 |
| `t_e2e_test_report_v2` | `ads_test_report_e2e_v2` | 缺少分层前缀 |

---

## 七、扫描规则

数据中心模块扫描业务表时，只扫描以下前缀的表：

```sql
AND (
    TABLE_NAME LIKE 'ods_%' OR
    TABLE_NAME LIKE 'dwd_%' OR
    TABLE_NAME LIKE 'dws_%' OR
    TABLE_NAME LIKE 'ads_%' OR
    TABLE_NAME LIKE 'dim_%' OR
    TABLE_NAME LIKE 'mid_%' OR
    TABLE_NAME LIKE 'tmp_%'
)
```

**系统表**（不纳入管理）：
- `sys_*` - 系统配置表
- `%_config` - 配置表
- `%_log` - 日志表
- `%_mapping` - 映射表
- `trigger_*` - 触发器相关
- `task_*` - 任务相关
- `pipeline_*` - 流水线相关
- `built_in_*` - 内置配置
- `processed_file` - 文件处理记录
