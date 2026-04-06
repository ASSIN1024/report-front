# 智能分区日期提取 & 列跳过功能设计文档

> **版本**: V1.0
> **日期**: 2026-04-06
> **状态**: 已批准，待实施
> **采用方案**: 方案A - 最小改动方案

---

## 1. 需求概述

### 1.1 背景

当前系统在处理FTP上传的Excel文件时存在两个问题：

1. **pt_dt字段硬编码**：分区日期固定使用系统当前时间，无法根据业务数据（文件名中的时间）生成
2. **列冲突问题**：当Excel第一列是`id`时，与系统保留的主键`id`冲突导致建表失败

### 1.2 目标

- ✅ 从文件名智能提取日期作为pt_dt（支持多种格式）
- ✅ 支持跳过Excel前N列以避免与保留字段冲突
- ✅ 向后兼容，旧配置无需修改

---

## 2. 功能需求

### 2.1 需求1：智能分区日期提取 (pt_dt)

**输入**：
- 文件名字符串（如 `test_flow20260406.xlsx`）
- 可选的日期提取规则配置

**输出**：
- `LocalDate` 对象（如 `2026-04-06`）
- 如果未识别到日期，回退到当前系统日期

**支持的日期格式**（按优先级排序）：

| 格式 | 示例 | 提取结果 |
|------|------|----------|
| `yyyy-MM-dd` | `sales_2026-04-06.xlsx` | `2026-04-06` |
| `yyyyMMdd` | `test_flow20260406.xlsx` | `2026-04-06` |
| `yyyyMMdd_HHmm` | `data20260406_1430.xlsx` | `2026-04-06` |

**边界情况处理**：
- 文件名无日期 → 使用当前日期 + 记录INFO日志
- 多个日期匹配 → 取第一个匹配的
- 配置了特定格式 → 只使用指定格式匹配

### 2.2 需求2：列跳过功能

**输入**：
- 跳过列数 N（整数，默认0）
- Excel列映射配置

**行为**：
- 在解析Excel时，自动跳过前N列
- 列映射的excelColumn从第N+1列开始计算

**示例**：
```
原始Excel:
| A(id) | B(name) | C(amount) | D(date) |

配置 skipColumns=1 后:
实际读取: B → name, C → amount, D → date
跳过: A(id)
```

---

## 3. 技术设计

### 3.1 数据库变更

#### 3.1.1 report_config 表新增字段

```sql
ALTER TABLE report_config 
ADD COLUMN skip_columns INT DEFAULT 0 COMMENT '跳过前N列' AFTER data_start_row,
ADD COLUMN date_extract_pattern VARCHAR(50) NULL COMMENT '日期提取规则' AFTER skip_columns;
```

**字段说明**：

| 字段名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `skip_columns` | INT | 0 | 跳过Excel的前N列（0表示不跳过） |
| `date_extract_pattern` | VARCHAR(50) | NULL | 日期提取规则，NULL或空表示自动识别 |

**date_extract_pattern 可选值**：

| 值 | 含义 |
|----|------|
| NULL / 空 | 自动识别所有支持的格式 |
| `yyyy-MM-dd` | 仅匹配分隔符格式 |
| `yyyyMMdd` | 仅匹配紧凑格式 |
| `yyyyMMdd_HHmm` | 匹配带时间的紧凑格式 |

---

### 3.2 后端实现

#### 3.2.1 实体层变更

**ReportConfig.java 新增字段**：

```java
/**
 * 跳过前N列
 */
private Integer skipColumns;

/**
 * 日期提取规则
 * 可选值：AUTO/null, yyyyMMdd, yyyy-MM-dd, yyyyMMdd_HHmm
 */
private String dateExtractPattern;
```

#### 3.2.2 新增工具类：FileNameDateExtractor

**位置**: `com.report.util.FileNameDateExtractor`

**职责**：从文件名中智能提取日期

```java
@Slf4j
public class FileNameDateExtractor {

    private static final List<DateFormatPattern> SUPPORTED_PATTERNS = Arrays.asList(
        new DateFormatPattern(".*?(\\d{4})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]).*", "yyyy-MM-dd"),
        new DateFormatPattern(".*?(\\d{4})(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])(?:_(?:0[1-9]|1[0-9]|2[0-3])(?:[0-5][0-9]))?.*", "yyyyMMdd")
    );

    /**
     * 从文件名提取日期
     *
     * @param fileName 完整文件名（含扩展名）
     * @return 提取到的日期，如果未找到返回null
     */
    public static LocalDate extractDate(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            return null;
        }

        String baseName = getBaseName(fileName);

        for (DateFormatPattern pattern : SUPPORTED_PATTERNS) {
            LocalDate date = tryExtract(baseName, pattern);
            if (date != null) {
                log.debug("从文件名提取日期: {} -> {}", fileName, date);
                return date;
            }
        }

        return null;
    }

    /**
     * 使用指定模式提取日期
     *
     * @param fileName   文件名
     * @param patternStr 日期模式字符串（如 "yyyyMMdd"）
     * @return 提取到的日期
     */
    public static LocalDate extractDateWithPattern(String fileName, String patternStr) {
        if (StrUtil.isBlank(patternStr)) {
            return extractDate(fileName);
        }

        // 根据指定的pattern查找对应的正则并尝试提取
        // ...
    }
}
```

**内部数据结构**：

```java
@Data
@AllArgsConstructor
class DateFormatPattern {
    private String regex;      // 正则表达式
    private String format;     // 日期格式字符串
}
```

#### 3.2.3 修改 DataProcessJob.java

**修改位置**: `processFile()` 方法（约第136-141行）

**修改前**：
```java
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
for (Map<String, Object> row : dataList) {
    List<Object> values = new ArrayList<>();
    values.add(sdf.format(new Date()));  // 硬编码当前时间
```

**修改后**：
```java
// 从文件名智能提取pt_dt
String fileName = file.getName();
LocalDate partitionDate = FileNameDateExtractor.extractDate(
    fileName, 
    reportConfig.getDateExtractPattern()
);

if (partitionDate == null) {
    partitionDate = LocalDate.now();
    log.info("文件名未包含可识别日期，使用当前日期: {}", fileName);
} else {
    log.info("成功从文件名提取分区日期: {} -> {}", fileName, partitionDate);
}

for (Map<String, Object> row : dataList) {
    List<Object> values = new ArrayList<>();
    values.add(Date.valueOf(partitionDate));  // 使用提取/默认的日期
```

#### 3.2.4 修改 ExcelUtil.java

**修改位置**: `buildColumnIndexMap()` 方法（约第235行）

**新增参数**：`int skipColumns`

**核心逻辑**：
```java
private static Map<Integer, FieldMapping> buildColumnIndexMap(
    Sheet sheet, 
    int headerRow, 
    List<FieldMapping> columnMappings,
    int skipColumns  // 新增参数
) {
    Map<Integer, FieldMapping> map = new HashMap<>();
    Row headerRowObj = sheet.getRow(headerRow);
    
    if (headerRowObj != null) {
        for (Cell cell : headerRowObj) {
            // 跳过前N列
            if (cell.getColumnIndex() < skipColumns) {
                continue;
            }
            
            String columnName = getCellValueAsString(cell);
            if (StrUtil.isNotBlank(columnName)) {
                for (FieldMapping mapping : columnMappings) {
                    if (columnName.trim().equalsIgnoreCase(
                        mapping.getExcelColumn() != null ? mapping.getExcelColumn().trim() : ""
                    )) {
                        map.put(cell.getColumnIndex(), mapping);
                        break;
                    }
                }
            }
        }
    } else {
        // 无表头时的fallback逻辑也需要考虑skipColumns
        for (int i = 0; i < columnMappings.size(); i++) {
            FieldMapping mapping = columnMappings.get(i);
            String col = mapping.getExcelColumn();
            if (StrUtil.isNotBlank(col)) {
                int index = col.toUpperCase().charAt(0) - 'A';
                index += skipColumns;  // 偏移量
                map.put(index, mapping);
            }
        }
    }
    
    return map;
}
```

**调用处修改**：`readExcel()` 方法需要传递 `skipColumns` 参数

#### 3.2.5 修改 TableCreatorServiceImpl.java

**已修复**：保留字段冲突问题（上一轮已解决）

**无需额外改动**。

---

### 3.3 前端实现

#### 3.3.1 ReportConfig.vue 变更

**新增表单项位置**："文件扫描配置"卡片内，`dataStartRow` 之后

**新增UI元素**：

```vue
<!-- 跳过前N列 -->
<el-form-item label="跳过前N列" prop="skipColumns">
  <el-input-number 
    v-model="form.skipColumns" 
    :min="0" 
    :max="10" 
    :disabled="readonly" 
  />
  <span style="margin-left: 10px; color: #909399; font-size: 12px;">
    如需跳过Excel的前几列（如序号列、ID列），请设置此值
  </span>
</el-form-item>

<!-- 日期提取规则 -->
<el-form-item label="日期提取规则" prop="dateExtractPattern">
  <el-select 
    v-model="form.dateExtractPattern" 
    :disabled="readonly" 
    clearable 
    placeholder="自动识别（推荐）"
    style="width: 300px;"
  >
    <el-option label="自动识别（推荐）" value="" />
    <el-option label="yyyyMMdd 格式" value="yyyyMMdd" />
    <el-option label="yyyy-MM-dd 格式" value="yyyy-MM-dd" />
    <el-option label="yyyyMMdd_HHmm 格式" value="yyyyMMdd_HHmm" />
  </el-select>
  <span style="margin-left: 10px; color: #909399; font-size: 12px;">
    从文件名自动提取分区日期的格式规则
  </span>
</el-form-item>
```

#### 3.3.2 data() 初始化

```javascript
data() {
  return {
    form: {
      // ... 现有字段
      skipColumns: 0,              // 新增
      dateExtractPattern: '',      // 新增
      columnMappings: []
    },
    // ... 其他数据
  }
}
```

---

## 4. 测试策略（TDD）

### 4.1 单元测试用例

#### 4.1.1 FileNameDateExtractorTest.java

**测试类位置**: `src/test/java/com/report/util/FileNameDateExtractorTest.java`

**测试用例清单**：

| # | 测试方法 | 输入 | 预期输出 | 场景类型 |
|---|---------|------|---------|---------|
| 1 | `testExtractDate_yyyyMMdd_format()` | `"test_flow20260406.xlsx"` | `2026-04-06` | 正常场景 |
| 2 | `testExtractDate_yyyy_MM_dd_format()` | `"sales_2026-04-06.xlsx"` | `2026-04-06` | 正常场景 |
| 3 | `testExtractDate_with_time_component()` | `"data20260406_1430.xlsx"` | `2026-04-06` | 带时间 |
| 4 | `testExtractDate_no_date_in_filename()` | `"random_data.xlsx"` | `null` | 无日期 |
| 5 | `testExtractDate_empty_filename()` | `""` 或 `null` | `null` | 异常输入 |
| 6 | `testExtractDate_multiple_dates()` | `"report_2025-01-01_20260406.xlsx"` | 第一个匹配 | 多个日期 |
| 7 | `testExtractDateWithPattern_specific_pattern()` | `"file20260406.xlsx"`, `"yyyyMMdd"` | `2026-04-06` | 指定模式 |
| 8 | `testExtractDateWithPattern_wrong_pattern()` | `"file2026-04-06.xlsx"`, `"yyyyMMdd"` | `null` | 模式不匹配 |

**测试代码示例**：

```java
@Test
void testExtractDate_yyyyMMdd_format() {
    LocalDate result = FileNameDateExtractor.extractDate("test_flow20260406.xlsx");
    assertNotNull(result);
    assertEquals(2026, result.getYear());
    assertEquals(Month.APRIL, result.getMonth());
    assertEquals(6, result.getDayOfMonth());
}

@Test
void testExtractDate_no_date_in_filename() {
    LocalDate result = FileNameDateExtractor.extractDate("random_data.xlsx");
    assertNull(result);
}
```

#### 4.1.2 TableCreatorServiceImplTest.java（补充）

**新增测试用例**：

| # | 测试方法 | 输入 | 预期输出 | 场景类型 |
|---|---------|------|---------|---------|
| 1 | `testCreateTable_skip_id_column()` | fieldName=`id` | 不重复创建id列 | 保留字段 |
| 2 | `testCreateTable_skip_pt_dt_column()` | fieldName=`pt_dt` | 不重复创建 | 保留字段 |
| 3 | `testCreateTable_normal_columns()` | 正常字段 | 正常创建 | 正常场景 |

#### 4.1.3 ExcelUtilTest.java（补充）

**新增测试用例**：

| # | 测试方法 | 输入 | 预期输出 | 场景类型 |
|---|---------|------|---------|---------|
| 1 | `testReadExcel_with_skipColumns()` | skipColumns=1 | 跳过第1列 | 列跳过 |
| 2 | `testReadExcel_skipColumns_zero()` | skipColumns=0 | 不跳过 | 默认值 |

---

## 5. 实施计划

### 5.1 任务分解

按照TDD原则，实施顺序如下：

#### Phase 1: 基础设施（TDD先行）

1. ✅ **编写单元测试** - `FileNameDateExtractorTest`
2. ❌ **实现工具类** - `FileNameDateExtractor`
3. ✅ **运行测试验证**

#### Phase 2: 数据库 & 实体层

4. ❌ **执行数据库迁移SQL**
5. ❌ **更新 ReportConfig.java 实体**
6. ❌ **更新 DTO 和 Mapper**

#### Phase 3: 业务逻辑集成

7. ❌ **修改 DataProcessJob.processFile()**
8. ❌ **修改 ExcelUtil.buildColumnIndexMap()**
9. ❌ **更新调用链传递新参数**

#### Phase 4: 前端界面

10. ❌ **修改 ReportConfig.vue 表单**
11. ❌ **添加表单校验规则**

#### Phase 5: 集成测试

12. ❌ **端到端测试 - 上传文件验证完整流程**
13. ❌ **回归测试 - 确保现有功能不受影响**

---

## 6. 向后兼容性

### 6.1 数据库兼容

- 新字段有默认值：`skip_columns=0`, `date_extract_pattern=NULL`
- 旧记录自动兼容，无需数据迁移脚本

### 6.2 API兼容

- 新增字段为可选字段
- 前端不传时使用默认值
- 不影响现有的报表配置保存/查询接口

### 6.3 行为兼容

- `skipColumns=0` 时行为与修改前完全一致
- `dateExtractPattern=NULL` 时自动识别，向后兼容

---

## 7. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 日期提取误判 | pt_dt错误 | TDD覆盖边界案例；日志记录提取结果 |
| 列跳过配置错误 | 数据错位 | 前端提示文案；限制最大跳过数(10) |
| 性能影响（正则） | 处理速度 | 预编译正则；缓存Pattern对象 |
| 向后兼容性破坏 | 旧配置异常 | 新字段有默认值；单元测试验证 |

---

## 8. 验收标准

### 功能验收

- [ ] 能从文件名正确提取 yyyyMMdd 格式日期
- [ ] 能从文件名正确提取 yyyy-MM-dd 格式日期
- [ ] 能从文件名正确提取 yyyyMMdd_HHmm 格式日期
- [ ] 文件名无日期时使用当前日期并记录日志
- [ ] 配置 skipColumns=1 时能正确跳过第1列
- [ ] 配置 skipColumns=0 时行为与之前一致
- [ ] 指定 dateExtractPattern 时只使用该格式
- [ ] 前端配置界面正常显示和保存新字段

### 质量验收

- [ ] 所有新增单元测试通过
- [ ] 现有测试不受影响
- [ ] 代码符合项目规范
- [ ] 无编译警告
- [ ] 日志信息清晰完整

---

## 9. 附录

### A. 文件变更清单

**后端**：
- `report-backend/src/main/java/com/report/entity/ReportConfig.java` - 新增2个字段
- `report-backend/src/main/java/com/report/util/FileNameDateExtractor.java` - **新建**
- `report-backend/src/main/java/com/report/job/DataProcessJob.java` - 修改processFile()
- `report-backend/src/main/java/com/report/util/ExcelUtil.java` - 修改buildColumnIndexMap()
- `report-backend/src/main/resources/schema.sql` - 新增DDL
- `report-backend/src/test/java/com/report/util/FileNameDateExtractorTest.java` - **新建**

**前端**：
- `src/views/report/components/ReportConfig.vue` - 新增2个表单项

### B. 参考资料

- [AGENTS.md](../../harness/AGENTS.md) - 项目规范
- [TableCreatorServiceImpl.java](../service/impl/TableCreatorServiceImpl.java) - 已修复的保留字段冲突
- [DataProcessJob.java](../job/DataProcessJob.java) - 文件处理主逻辑
- [ExcelUtil.java](../util/ExcelUtil.java) - Excel解析工具

---

**文档结束**

*下次更新*: 实施完成后补充实施细节和测试结果
