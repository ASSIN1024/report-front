# 任务监控增强与数据清洗配置实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现三个功能：1) 报表配置列表立即扫描按钮 2) 列映射清洗规则配置 3) JSON导入列映射

**Architecture:**
- 后端：扩展FieldMapping DTO增加清洗规则字段，改造DataProcessJob应用清洗规则，新增立即扫描API
- 前端：ReportConfig.vue页面增加操作列立即扫描按钮和清洗规则配置弹窗，ReportList.vue增加JSON导入功能

**Tech Stack:** Spring Boot 2.1.2, Vue 2.6, Element UI, MyBatis-Plus

---

## 文件结构

### 后端变更
- Modify: `report-backend/src/main/java/com/report/entity/dto/FieldMapping.java` - 增加cleanRules和validators字段
- Create: `report-backend/src/main/java/com/report/entity/dto/CleanRule.java` - 清洗规则DTO
- Create: `report-backend/src/main/java/com/report/entity/dto/Validators.java` - 验证规则DTO
- Modify: `report-backend/src/main/java/com/report/job/DataProcessJob.java` - 应用清洗规则逻辑
- Modify: `report-backend/src/main/java/com/report/controller/ReportConfigController.java` - 新增立即扫描和JSON导入API
- Create: `report-backend/src/main/java/com/report/util/ColumnMappingValidator.java` - JSON导入校验工具

### 前端变更
- Modify: `src/views/report/ReportList.vue` - 增加操作列立即扫描按钮
- Modify: `src/views/report/components/ReportConfig.vue` - 增加清洗规则配置和JSON导入
- Create: `src/views/report/components/CleanRulesDialog.vue` - 清洗规则配置弹窗组件
- Create: `src/views/report/components/JsonImportDialog.vue` - JSON导入向导组件
- Create: `src/api/reportConfig.js` - 新增API方法（如果不存在）

---

## Task 1: 后端 - 扩展FieldMapping DTO

**Files:**
- Modify: `report-backend/src/main/java/com/report/entity/dto/FieldMapping.java`
- Create: `report-backend/src/main/java/com/report/entity/dto/CleanRule.java`
- Create: `report-backend/src/main/java/com/report/entity/dto/Validators.java`

- [ ] **Step 1: 创建CleanRule.java**

```java
package com.report.entity.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class CleanRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private String pattern;
    private String replace;
}
```

- [ ] **Step 2: 创建Validators.java**

```java
package com.report.entity.dto;

import lombok.Data;
import java.io.Serializable;

@Data
public class Validators implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean required;
    private Boolean positiveOnly;
    private String minValue;
    private String maxValue;
}
```

- [ ] **Step 3: 扩展FieldMapping.java**

在FieldMapping类中添加字段：
```java
private List<CleanRule> cleanRules;
private Validators validators;
```

- [ ] **Step 4: 提交**

```bash
git add report-backend/src/main/java/com/report/entity/dto/
git commit -m "feat: 扩展FieldMapping DTO支持清洗规则"
```

---

## Task 2: 后端 - DataProcessJob应用清洗规则

**Files:**
- Modify: `report-backend/src/main/java/com/report/job/DataProcessJob.java:159-192`

- [ ] **Step 1: 修改convertValue方法**

找到现有的`convertValue`方法，修改为：

```java
private Object convertValue(Object value, String fieldType, String dateFormat, List<CleanRule> cleanRules) {
    if (value == null) {
        return null;
    }

    if (StrUtil.isBlank(String.valueOf(value))) {
        return null;
    }

    String strValue = String.valueOf(value).trim();

    // 应用清洗规则
    if (cleanRules != null && !cleanRules.isEmpty()) {
        for (CleanRule rule : cleanRules) {
            if (rule.getPattern() != null && strValue.equals(rule.getPattern())) {
                strValue = rule.getReplace() != null ? rule.getReplace() : "";
                value = strValue;
            }
        }
    }

    // 如果清洗后为空，返回null
    if (StrUtil.isBlank(strValue)) {
        return null;
    }

    try {
        switch (fieldType) {
            case "STRING":
                return strValue;
            case "INTEGER":
                if (strValue.contains(".")) {
                    return new BigDecimal(strValue).intValue();
                }
                return Long.parseLong(strValue);
            case "DECIMAL":
                return new BigDecimal(strValue);
            case "DATE":
                return parseDate(strValue, dateFormat);
            case "DATETIME":
                return parseDatetime(strValue, dateFormat);
            default:
                return strValue;
        }
    } catch (Exception e) {
        log.warn("数据类型转换失败: value={}, type={}", value, fieldType, e);
        return strValue;
    }
}
```

- [ ] **Step 2: 修改insertData方法中调用convertValue的地方**

在`insertData`方法第144行附近，将：
```java
value = convertValue(value, mapping.getFieldType(), mapping.getDateFormat());
```
改为：
```java
value = convertValue(value, mapping.getFieldType(), mapping.getDateFormat(), mapping.getCleanRules());
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/job/DataProcessJob.java
git commit -m "feat: DataProcessJob应用清洗规则转换"
```

---

## Task 3: 后端 - 新增立即扫描API

**Files:**
- Modify: `report-backend/src/main/java/com/report/controller/ReportConfigController.java`
- Create: `report-backend/src/main/java/com/report/service/FtpScanService.java` (如果需要)
- Modify: `report-backend/src/main/java/com/report/job/FtpScanJob.java`

- [ ] **Step 1: 在ReportConfigController添加立即扫描API**

在Controller中添加：

```java
@PostMapping("/{id}/scan")
public Result<Long> triggerScan(@PathVariable Long id) {
    ReportConfig config = reportConfigService.getById(id);
    if (config == null) {
        return Result.fail("报表配置不存在");
    }
    if (config.getStatus() != 1) {
        return Result.fail("报表配置未启用");
    }

    // 创建扫描任务
    TaskExecution task = taskService.createTask(
        "FTP_SCAN",
        "FTP扫描-" + config.getReportName(),
        id,
        null,
        null
    );

    // 触发异步扫描
    // 注意：需要确保FtpScanJob支持接收单个reportConfigId参数
    // 如果不支持，需要改造FtpScanJob

    return Result.success(task.getId());
}
```

- [ ] **Step 2: 改造FtpScanJob支持单个配置扫描**

修改`FtpScanJob.java`，添加一个方法支持扫描指定配置：

```java
public void scanConfig(Long reportConfigId, Long taskId) {
    ReportConfig config = reportConfigService.getById(reportConfigId);
    if (config == null) {
        taskService.finishTask(taskId, "FAILED", "报表配置不存在");
        return;
    }

    FtpConfig ftpConfig = ftpConfigService.getById(config.getFtpConfigId());
    if (ftpConfig == null) {
        taskService.finishTask(taskId, "FAILED", "FTP配置不存在");
        return;
    }

    FTPClient ftpClient = null;
    try {
        ftpClient = FtpUtil.connect(ftpConfig);
        if (ftpClient == null || !ftpClient.isConnected()) {
            taskService.finishTask(taskId, "FAILED", "FTP连接失败");
            return;
        }

        List<String> files = FtpUtil.listFiles(ftpClient, config.getScanPath(), config.getFilePattern());
        if (files == null || files.isEmpty()) {
            taskService.finishTask(taskId, "NO_FILE", "未匹配到文件");
            logService.saveLog(taskId, "WARN", "FTP目录中没有匹配的文件: " + config.getFilePattern());
            return;
        }

        // 处理文件...
        taskService.finishTask(taskId, "SUCCESS", null);

    } catch (Exception e) {
        log.error("FTP扫描异常", e);
        taskService.finishTask(taskId, "FAILED", e.getMessage());
    } finally {
        FtpUtil.disconnect(ftpClient);
    }
}
```

注意：现有代码中`ReportConfig`没有`scanPath`字段，需要确认字段名称。可能在现有代码中路径是存在别的地方。让我先检查ReportConfig结构。

实际上我之前读取的ReportConfig没有scanPath字段，它只有filePattern。scanPath可能是在FtpConfig中配置的。让我在实现时注意这一点。

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/controller/ReportConfigController.java
git add report-backend/src/main/java/com/report/job/FtpScanJob.java
git commit -m "feat: 新增报表配置立即扫描API"
```

---

## Task 4: 后端 - JSON导入校验API

**Files:**
- Create: `report-backend/src/main/java/com/report/util/ColumnMappingValidator.java`
- Modify: `report-backend/src/main/java/com/report/controller/ReportConfigController.java`

- [ ] **Step 1: 创建ColumnMappingValidator.java**

```java
package com.report.util;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.report.entity.dto.CleanRule;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ColumnMappingValidator {

    private static final List<String> VALID_FIELD_TYPES = List.of(
        "STRING", "INTEGER", "DECIMAL", "DATE", "DATETIME"
    );

    public static class ValidationError {
        private int line;
        private String message;
        private String suggestion;

        public ValidationError(int line, String message, String suggestion) {
            this.line = line;
            this.message = message;
            this.suggestion = suggestion;
        }

        public int getLine() { return line; }
        public String getMessage() { return message; }
        public String getSuggestion() { return suggestion; }
    }

    public static List<ValidationError> validate(String json) {
        List<ValidationError> errors = new ArrayList<>();

        if (StrUtil.isBlank(json)) {
            errors.add(new ValidationError(0, "JSON不能为空", null));
            return errors;
        }

        try {
            JSONArray array = JSONUtil.parseArray(json);

            for (int i = 0; i < array.size(); i++) {
                int lineNum = i + 1;
                JSONObject obj = array.getJSONObject(i);

                // 检查必填字段
                if (obj.getStr("excelColumn") == null) {
                    errors.add(new ValidationError(lineNum, "缺少excelColumn字段", "添加 \"excelColumn\": \"A\""));
                }

                if (obj.getStr("fieldName") == null) {
                    errors.add(new ValidationError(lineNum, "缺少fieldName字段", "添加 \"fieldName\": \"column_name\""));
                }

                String fieldType = obj.getStr("fieldType");
                if (fieldType == null) {
                    errors.add(new ValidationError(lineNum, "缺少fieldType字段", "添加 \"fieldType\": \"STRING\""));
                } else if (!VALID_FIELD_TYPES.contains(fieldType)) {
                    errors.add(new ValidationError(lineNum, "无效的字段类型: " + fieldType,
                        "有效类型: " + String.join(", ", VALID_FIELD_TYPES)));
                }

                // 检查cleanRules格式
                if (obj.containsKey("cleanRules") && obj.get("cleanRules") != null) {
                    JSONArray rules = obj.getJSONArray("cleanRules");
                    for (int j = 0; j < rules.size(); j++) {
                        JSONObject rule = rules.getJSONObject(j);
                        if (rule.getStr("pattern") == null) {
                            errors.add(new ValidationError(lineNum, "第" + (j+1) + "条规则缺少pattern字段", null));
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.error("JSON解析失败", e);
            errors.add(new ValidationError(0, "JSON格式错误: " + e.getMessage(), "检查JSON语法"));
        }

        return errors;
    }

    public static int countMappings(String json) {
        try {
            JSONArray array = JSONUtil.parseArray(json);
            return array.size();
        } catch (Exception e) {
            return 0;
        }
    }
}
```

注意：需要在类顶部添加import `cn.hutool.core.util.StrUtil;`

- [ ] **Step 2: 在ReportConfigController添加JSON导入校验API**

```java
@PostMapping("/{id}/column-mapping/validate")
public Result<Map<String, Object>> validateColumnMapping(
    @PathVariable Long id,
    @RequestBody Map<String, String> params) {

    String json = params.get("json");
    List<ColumnMappingValidator.ValidationError> errors =
        ColumnMappingValidator.validate(json);

    Map<String, Object> result = new HashMap<>();
    result.put("valid", errors.isEmpty());
    result.put("errors", errors);
    result.put("count", ColumnMappingValidator.countMappings(json));

    return Result.success(result);
}
```

- [ ] **Step 3: 添加导入API**

```java
@PostMapping("/{id}/column-mapping/import")
public Result<Map<String, Object>> importColumnMapping(
    @PathVariable Long id,
    @RequestBody Map<String, String> params) {

    ReportConfig config = reportConfigService.getById(id);
    if (config == null) {
        return Result.fail("报表配置不存在");
    }

    String json = params.get("json");
    List<ColumnMappingValidator.ValidationError> errors =
        ColumnMappingValidator.validate(json);

    if (!errors.isEmpty()) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("errors", errors);
        return Result.fail("校验失败", result);
    }

    // 更新配置
    config.setColumnMapping(json);
    reportConfigService.updateById(config);

    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("imported", ColumnMappingValidator.countMappings(json));

    return Result.success(result);
}
```

- [ ] **Step 4: 提交**

```bash
git add report-backend/src/main/java/com/report/util/ColumnMappingValidator.java
git add report-backend/src/main/java/com/report/controller/ReportConfigController.java
git commit -m "feat: 新增列映射JSON导入校验和导入API"
```

---

## Task 5: 前端 - ReportList增加立即扫描按钮

**Files:**
- Modify: `src/views/report/ReportList.vue`

- [ ] **Step 1: 查看现有ReportList.vue结构**

读取现有文件了解表格结构，然后添加操作列：

```vue
<el-table-column label="操作" width="180" fixed="right">
  <template slot-scope="{ row }">
    <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
    <el-button
      v-if="row.status === 1"
      type="text"
      size="small"
      style="color: #67C23A"
      @click="handleScan(row)">立即扫描</el-button>
    <el-button
      v-else
      type="text"
      size="small"
      disabled>立即扫描</el-button>
    <el-button type="text" size="small" style="color: #F56C6C" @click="handleDelete(row)">删除</el-button>
  </template>
</el-table-column>
```

- [ ] **Step 2: 添加handleScan方法**

```javascript
async handleScan(row) {
  try {
    await this.$confirm(
      `确定要立即扫描 "${row.reportName}" 的FTP目录吗？\n请确保测试文件已放置到FTP服务器的指定目录。`,
      '确认立即扫描',
      {
        confirmButtonText: '确认扫描',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const res = await triggerScan(row.id)
    this.$message.success('扫描任务已创建')
    // 刷新任务列表或跳转到任务监控
    this.loadData()
  } catch (error) {
    if (error !== 'cancel') {
      this.$message.error(error.message || '扫描启动失败')
    }
  }
}
```

- [ ] **Step 3: 添加API调用**

在api/reportConfig.js添加：
```javascript
export function triggerScan(id) {
  return request({
    url: `/api/report/config/${id}/scan`,
    method: 'post'
  })
}
```

- [ ] **Step 4: 提交**

```bash
git add src/views/report/ReportList.vue src/api/reportConfig.js
git commit -m "feat: 报表配置列表增加立即扫描功能"
```

---

## Task 6: 前端 - ReportConfig增加清洗规则配置

**Files:**
- Create: `src/views/report/components/CleanRulesDialog.vue`
- Modify: `src/views/report/components/ReportConfig.vue`

- [ ] **Step 1: 创建CleanRulesDialog.vue**

```vue
<template>
  <el-dialog :visible.sync="visible" title="清洗规则配置" width="600px" @close="handleClose">
    <div class="field-info">
      <span><strong>字段:</strong> {{ currentField.fieldName }}</span>
      <span><strong>类型:</strong> {{ currentField.fieldType }}</span>
      <span><strong>来源:</strong> 列 {{ currentField.excelColumn }}</span>
    </div>

    <h4>替换规则</h4>
    <p class="subtitle">当检测到以下值时，替换为指定值</p>

    <el-table :data="rules" border size="small">
      <el-table-column label="原始值">
        <template slot-scope="{ row, $index }">
          <el-input v-model="row.pattern" size="small" placeholder="输入值" />
        </template>
      </el-table-column>
      <el-table-column label="替换为">
        <template slot-scope="{ row, $index }">
          <el-input v-model="row.replace" size="small" placeholder="替换为" />
        </template>
      </el-table-column>
      <el-table-column width="60">
        <template slot-scope="{ $index }">
          <el-button type="text" style="color: #F56C6C" @click="removeRule($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-button size="small" plain @click="addRule" style="margin-top: 10px;">+ 添加规则</el-button>

    <div slot="footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'CleanRulesDialog',
  props: {
    visible: Boolean,
    currentField: Object
  },
  data() {
    return {
      rules: []
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.rules = this.currentField.cleanRules
          ? [...this.currentField.cleanRules]
          : []
      }
    }
  },
  methods: {
    addRule() {
      this.rules.push({ pattern: '', replace: '' })
    },
    removeRule(index) {
      this.rules.splice(index, 1)
    },
    handleSave() {
      this.$emit('update:visible', false)
      this.$emit('save', this.rules)
    },
    handleClose() {
      this.$emit('update:visible', false)
    }
  }
}
</script>
```

- [ ] **Step 2: 修改ReportConfig.vue**

在script中添加导入：
```javascript
import CleanRulesDialog from './CleanRulesDialog.vue'
```

添加组件引用和data：
```javascript
components: { CleanRulesDialog },
data() {
  return {
    // ... existing data
    cleanRulesVisible: false,
    currentColumnIndex: null,
    currentColumn: {}
  }
}
```

在template的列映射表格中添加配置规则按钮列：
```vue
<el-table-column label="清洗规则" width="140">
  <template slot-scope="{ row, $index }">
    <el-button
      type="text"
      @click="openCleanRules(row, $index)">
      {{ row.cleanRules && row.cleanRules.length ? `规则 (${row.cleanRules.length})` : '配置规则' }}
    </el-button>
  </template>
</el-table-column>
```

添加methods：
```javascript
openCleanRules(row, index) {
  this.currentColumnIndex = index
  this.currentColumn = { ...row }
  this.cleanRulesVisible = true
},
handleCleanRulesSave(rules) {
  this.form.columnMappings[this.currentColumnIndex].cleanRules = rules
}
```

添加弹窗组件：
```vue
<CleanRulesDialog
  :visible.sync="cleanRulesVisible"
  :current-field="currentColumn"
  @save="handleCleanRulesSave"
/>
```

- [ ] **Step 3: 提交**

```bash
git add src/views/report/components/CleanRulesDialog.vue
git add src/views/report/components/ReportConfig.vue
git commit -m "feat: 报表配置增加清洗规则配置功能"
```

---

## Task 7: 前端 - JSON导入向导

**Files:**
- Create: `src/views/report/components/JsonImportDialog.vue`
- Modify: `src/views/report/components/ReportConfig.vue`

- [ ] **Step 1: 创建JsonImportDialog.vue**

这是一个3步骤的向导组件：

```vue
<template>
  <el-dialog :visible.sync="visible" title="JSON导入列映射" width="700px" @close="handleClose">
    <!-- 步骤1: 输入JSON -->
    <div v-if="step === 1">
      <el-input
        type="textarea"
        v-model="jsonText"
        :rows="12"
        placeholder="粘贴JSON配置..."
        style="font-family: monospace;" />
      <div class="tip">支持批量导入列映射配置，清洗规则为可选项</div>
    </div>

    <!-- 步骤2: 校验结果 -->
    <div v-if="step === 2">
      <div v-if="validating" style="text-align: center; padding: 40px;">
        <i class="el-icon-loading" style="font-size: 30px;"></i>
        <p>校验中...</p>
      </div>
      <div v-else-if="validationPassed">
        <el-alert type="success" :closable="false">
          <p>校验通过！共解析 <strong>{{ previewCount }}</strong> 个列映射配置</p>
        </el-alert>
        <pre class="preview">{{ truncatedPreview }}</pre>
      </div>
      <div v-else>
        <el-alert type="error" :closable="false">
          <p>校验失败，请修复以下错误：</p>
        </el-alert>
        <div v-for="err in errors" :key="err.line + err.message" class="error-item">
          <strong>第{{ err.line }}行:</strong> {{ err.message }}
          <span v-if="err.suggestion" class="suggestion">修复建议: {{ err.suggestion }}</span>
        </div>
      </div>
    </div>

    <!-- 步骤3: 确认导入 -->
    <div v-if="step === 3">
      <el-alert type="warning" :closable="false">
        确认导入以下配置到报表: <strong>{{ reportName }}</strong>
      </el-alert>
      <div class="summary">
        <div class="summary-item"><strong>{{ previewCount }}</strong><span>列映射</span></div>
      </div>
      <div class="warning">此操作将替换现有的列映射配置</div>
    </div>

    <span slot="footer">
      <el-button v-if="step > 1" @click="step--">上一步</el-button>
      <el-button @click="handleClose">取消</el-button>
      <el-button v-if="step < 3" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="step === 3" type="success" @click="doImport">确认导入</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { validateColumnMapping, importColumnMapping } from '@/api/reportConfig'

export default {
  name: 'JsonImportDialog',
  props: {
    visible: Boolean,
    reportConfigId: [String, Number],
    reportName: String
  },
  data() {
    return {
      step: 1,
      jsonText: '',
      errors: [],
      validationPassed: false,
      previewCount: 0,
      validating: false
    }
  },
  computed: {
    truncatedPreview() {
      const parsed = JSON.parse(this.jsonText || '[]')
      return JSON.stringify(parsed.slice(0, 3), null, 2) +
        (parsed.length > 3 ? '\n...' : '')
    }
  },
  methods: {
    async nextStep() {
      if (this.step === 1) {
        await this.validate()
      } else if (this.step === 2 && this.validationPassed) {
        this.step = 3
      }
    },
    async validate() {
      this.validating = true
      try {
        const res = await validateColumnMapping(this.reportConfigId, { json: this.jsonText })
        this.errors = res.data.errors || []
        this.validationPassed = res.data.valid
        this.previewCount = res.data.count || 0
        if (this.validationPassed) {
          this.step = 2
        }
      } catch (e) {
        this.$message.error('校验请求失败')
      } finally {
        this.validating = false
      }
    },
    async doImport() {
      try {
        const res = await importColumnMapping(this.reportConfigId, { json: this.jsonText })
        if (res.success) {
          this.$message.success(`成功导入 ${res.data.imported} 个列映射`)
          this.$emit('success')
          this.handleClose()
        }
      } catch (e) {
        this.$message.error('导入失败')
      }
    },
    handleClose() {
      this.step = 1
      this.jsonText = ''
      this.errors = []
      this.validationPassed = false
      this.$emit('update:visible', false)
    }
  }
}
</script>
```

- [ ] **Step 2: 修改ReportConfig.vue添加JSON导入入口**

在列映射卡片头部添加按钮：
```vue
<div slot="header">
  <span>列映射配置</span>
  <div style="float: right;">
    <el-button type="text" @click="handleAddColumn" v-if="!readonly">+ 添加映射</el-button>
    <el-button type="text" @click="jsonImportVisible = true" v-if="!readonly">JSON导入</el-button>
  </div>
</div>
```

添加数据和组件引用：
```javascript
import JsonImportDialog from './JsonImportDialog.vue'
components: { CleanRulesDialog, JsonImportDialog },
data() {
  return {
    // ... existing data
    jsonImportVisible: false
  }
}
```

添加弹窗组件：
```vue
<JsonImportDialog
  :visible.sync="jsonImportVisible"
  :report-config-id="form.id"
  :report-name="form.reportName"
  @success="loadData"
/>
```

- [ ] **Step 3: 添加API方法**

在api/reportConfig.js添加：
```javascript
export function validateColumnMapping(id, data) {
  return request({
    url: `/api/report/config/${id}/column-mapping/validate`,
    method: 'post',
    data
  })
}

export function importColumnMapping(id, data) {
  return request({
    url: `/api/report/config/${id}/column-mapping/import`,
    method: 'post',
    data
  })
}
```

- [ ] **Step 4: 提交**

```bash
git add src/views/report/components/JsonImportDialog.vue
git add src/views/report/components/ReportConfig.vue
git add src/api/reportConfig.js
git commit -m "feat: 报表配置增加JSON导入功能"
```

---

## Task 8: 集成测试

- [ ] **Step 1: 启动后端服务测试API**

```bash
cd report-backend
mvn spring-boot:run
```

测试端点：
- POST /api/report/config/{id}/scan - 立即扫描
- POST /api/report/config/{id}/column-mapping/validate - JSON校验
- POST /api/report/config/{id}/column-mapping/import - JSON导入

- [ ] **Step 2: 启动前端测试UI**

```bash
npm run serve
```

测试场景：
1. 在报表配置列表点击"立即扫描"
2. 在报表配置编辑页面配置清洗规则
3. 使用JSON导入功能

---

## 设计确认清单

| 功能 | 状态 |
|------|------|
| FieldMapping扩展cleanRules字段 | ✓ |
| DataProcessJob应用清洗规则 | ✓ |
| 立即扫描API | ✓ |
| 清洗规则配置弹窗 | ✓ |
| JSON导入向导 | ✓ |
| ReportList操作列立即扫描 | ✓ |
