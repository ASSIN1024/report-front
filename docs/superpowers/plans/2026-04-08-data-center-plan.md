# 数据中心 - 表管理功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在前端实现数据中心模块，对数据库表进行分层标记和管理，支持查看表结构和数据预览。

**Architecture:** 后端新增 table_layer_mapping 表和相关 Service/Controller，前端新增 data-center 页面，复用现有 DataService 的数据查询能力。

**Tech Stack:** Spring Boot 2.1.2 + MyBatis-Plus 3.x + Vue 2.6 + Element UI

---

## 一、文件结构

### 后端新建文件

| 文件 | 职责 |
|------|------|
| `entity/TableLayerMapping.java` | 表分层映射实体 |
| `mapper/TableLayerMappingMapper.java` | MyBatis Mapper |
| `service/DataCenterService.java` | 接口 |
| `service/impl/DataCenterServiceImpl.java` | 实现 |
| `controller/DataCenterController.java` | REST API |

### 后端修改文件

| 文件 | 修改内容 |
|------|----------|
| `resources/schema.sql` | 新增 table_layer_mapping 表 |
| `trigger/TriggerConfig.java` | 无（TriggerConfig 已存在） |

### 前端新建文件

| 文件 | 职责 |
|------|------|
| `views/data-center/Index.vue` | 数据中心入口页面 |
| `api/dataCenter.js` | API 请求封装 |

### 前端修改文件

| 文件 | 修改内容 |
|------|----------|
| `router/index.js` | 新增 /data-center 路由 |
| `api/request.js` | 无需修改，复用现有 |

---

## 二、任务分解

### Task 1: 数据库 Schema 变更

**Files:**
- Modify: `report-backend/src/main/resources/schema.sql`

- [ ] **Step 1: 添加 table_layer_mapping 表定义**

在 schema.sql 末尾添加：

```sql
-- 表分层映射表
CREATE TABLE IF NOT EXISTS table_layer_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    table_name VARCHAR(100) NOT NULL COMMENT '数据库表名',
    table_layer VARCHAR(20) COMMENT '流向分层: ODS/DWD/DWS/ADS',
    source_type VARCHAR(50) COMMENT '来源类型: FTP来源/TRIGGER/PIPELINE/MANUAL',
    source_id BIGINT COMMENT '关联来源ID',
    source_name VARCHAR(200) COMMENT '关联来源名称',
    business_domain VARCHAR(200) COMMENT '业务域描述',
    description VARCHAR(500) COMMENT '表描述',
    tags JSON COMMENT '自定义标签',
    marked TINYINT NOT NULL DEFAULT 0 COMMENT '是否已标记: 0-未标记, 1-已标记',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_table_name (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表分层映射表';
```

Run: `git diff report-backend/src/main/resources/schema.sql`
Expected: 看到新增的表定义

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/resources/schema.sql
git commit -m "feat(data-center): 添加 table_layer_mapping 表
关联任务: H-新功能"
```

---

### Task 2: 后端 Entity 和 Mapper

**Files:**
- Create: `report-backend/src/main/java/com/report/entity/TableLayerMapping.java`
- Create: `report-backend/src/main/java/com/report/mapper/TableLayerMappingMapper.java`

- [ ] **Step 1: 创建 TableLayerMapping 实体**

```java
package com.report.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.util.Date;

@Data
@TableName("table_layer_mapping")
public class TableLayerMapping {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String tableName;

    private String tableLayer;

    private String sourceType;

    private Long sourceId;

    private String sourceName;

    private String businessDomain;

    private String description;

    private String tags;

    @TableLogic
    private Integer marked;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
```

- [ ] **Step 2: 创建 TableLayerMappingMapper**

```java
package com.report.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.report.entity.TableLayerMapping;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TableLayerMappingMapper extends BaseMapper<TableLayerMapping> {
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/entity/TableLayerMapping.java
git add report-backend/src/main/java/com/report/mapper/TableLayerMappingMapper.java
git commit -m "feat(data-center): 添加 TableLayerMapping 实体和 Mapper
关联任务: H-新功能"
```

---

### Task 3: 后端 Service 层

**Files:**
- Create: `report-backend/src/main/java/com/report/service/DataCenterService.java`
- Create: `report-backend/src/main/java/com/report/service/impl/DataCenterServiceImpl.java`

- [ ] **Step 1: 创建 DataCenterService 接口**

```java
package com.report.service;

import com.report.entity.TableLayerMapping;
import java.util.List;
import java.util.Map;

public interface DataCenterService {
    List<TableLayerMapping> listTables(String tableLayer, String sourceType, String businessDomain);

    TableLayerMapping getTableByName(String tableName);

    boolean updateTableMapping(TableLayerMapping mapping);

    List<TableLayerMapping> listUntaggedTables();

    List<String> scanNewTables();

    Map<String, Object> getTableData(String tableName, Integer pageNum, Integer pageSize, String condition);

    List<Map<String, Object>> getTableColumns(String tableName);
}
```

- [ ] **Step 2: 创建 DataCenterServiceImpl 实现**

```java
package com.report.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.report.entity.TableLayerMapping;
import com.report.mapper.TableLayerMappingMapper;
import com.report.service.DataCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
public class DataCenterServiceImpl implements DataCenterService {

    @Autowired
    private TableLayerMappingMapper tableLayerMappingMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final Pattern SAFE_SQL_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\s=<>('%)\\.,]+$");

    @Override
    public List<TableLayerMapping> listTables(String tableLayer, String sourceType, String businessDomain) {
        LambdaQueryWrapper<TableLayerMapping> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(tableLayer)) {
            wrapper.eq(TableLayerMapping::getTableLayer, tableLayer);
        }
        if (StrUtil.isNotBlank(sourceType)) {
            wrapper.eq(TableLayerMapping::getSourceType, sourceType);
        }
        if (StrUtil.isNotBlank(businessDomain)) {
            wrapper.like(TableLayerMapping::getBusinessDomain, businessDomain);
        }
        wrapper.orderByDesc(TableLayerMapping::getUpdateTime);
        return tableLayerMappingMapper.selectList(wrapper);
    }

    @Override
    public TableLayerMapping getTableByName(String tableName) {
        return tableLayerMappingMapper.selectOne(
            new LambdaQueryWrapper<TableLayerMapping>().eq(TableLayerMapping::getTableName, tableName)
        );
    }

    @Override
    @Transactional
    public boolean updateTableMapping(TableLayerMapping mapping) {
        if (mapping.getId() != null) {
            return tableLayerMappingMapper.updateById(mapping) > 0;
        } else {
            mapping.setMarked(1);
            return tableLayerMappingMapper.insert(mapping) > 0;
        }
    }

    @Override
    public List<TableLayerMapping> listUntaggedTables() {
        return tableLayerMappingMapper.selectList(
            new LambdaQueryWrapper<TableLayerMapping>().eq(TableLayerMapping::getMarked, 0)
        );
    }

    @Override
    @Transactional
    public List<String> scanNewTables() {
        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE' " +
                     "AND TABLE_NAME NOT LIKE '\_%' AND TABLE_NAME NOT IN (" +
                     "SELECT table_name FROM table_layer_mapping" +
                     ")";
        List<String> newTables = jdbcTemplate.queryForList(sql, String.class);

        for (String tableName : newTables) {
            TableLayerMapping mapping = new TableLayerMapping();
            mapping.setTableName(tableName);
            mapping.setMarked(0);
            tableLayerMappingMapper.insert(mapping);
        }
        return newTables;
    }

    @Override
    public Map<String, Object> getTableData(String tableName, Integer pageNum, Integer pageSize, String condition) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("无效的表名");
        }

        pageNum = pageNum != null ? pageNum : 1;
        pageSize = pageSize != null ? pageSize : 20;
        int offset = (pageNum - 1) * pageSize;

        String countSql = "SELECT COUNT(*) FROM " + tableName;
        if (StrUtil.isNotBlank(condition)) {
            if (!isValidCondition(condition)) {
                throw new IllegalArgumentException("无效的筛选条件");
            }
            countSql += " WHERE " + condition;
        }
        Long total = jdbcTemplate.queryForObject(countSql, Long.class);

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        if (StrUtil.isNotBlank(condition)) {
            sql.append(" WHERE ").append(condition);
        }
        sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(sql.toString());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("size", pageSize);
        result.put("current", pageNum);
        result.put("pages", (total + pageSize - 1) / pageSize);
        return result;
    }

    @Override
    public List<Map<String, Object>> getTableColumns(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("无效的表名");
        }
        String sql = "SELECT COLUMN_NAME as columnName, DATA_TYPE as dataType, COLUMN_COMMENT as columnComment " +
                     "FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? " +
                     "ORDER BY ORDINAL_POSITION";
        return jdbcTemplate.queryForList(sql, tableName);
    }

    private boolean isValidTableName(String tableName) {
        return tableName != null && tableName.matches("^[a-zA-Z][a-zA-Z0-9_]*$");
    }

    private boolean isValidCondition(String condition) {
        if (condition == null || condition.length() > 500) {
            return false;
        }
        return SAFE_SQL_PATTERN.matcher(condition).matches();
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add report-backend/src/main/java/com/report/service/DataCenterService.java
git add report-backend/src/main/java/com/report/service/impl/DataCenterServiceImpl.java
git commit -m "feat(data-center): 添加 DataCenterService 服务层
关联任务: H-新功能"
```

---

### Task 4: 后端 Controller 层

**Files:**
- Create: `report-backend/src/main/java/com/report/controller/DataCenterController.java`

- [ ] **Step 1: 创建 DataCenterController**

```java
package com.report.controller;

import com.report.common.result.Result;
import com.report.entity.TableLayerMapping;
import com.report.service.DataCenterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/data-center")
public class DataCenterController {

    @Autowired
    private DataCenterService dataCenterService;

    @GetMapping("/tables")
    public Result<List<TableLayerMapping>> listTables(
            @RequestParam(required = false) String tableLayer,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String businessDomain) {
        return Result.success(dataCenterService.listTables(tableLayer, sourceType, businessDomain));
    }

    @GetMapping("/tables/{tableName}")
    public Result<TableLayerMapping> getTable(@PathVariable String tableName) {
        return Result.success(dataCenterService.getTableByName(tableName));
    }

    @PutMapping("/tables")
    public Result<Void> updateTable(@RequestBody TableLayerMapping mapping) {
        dataCenterService.updateTableMapping(mapping);
        return Result.success();
    }

    @GetMapping("/untagged")
    public Result<List<TableLayerMapping>> listUntagged() {
        return Result.success(dataCenterService.listUntaggedTables());
    }

    @PostMapping("/scan")
    public Result<List<String>> scanTables() {
        return Result.success(dataCenterService.scanNewTables());
    }

    @GetMapping("/tables/{tableName}/columns")
    public Result<List<Map<String, Object>>> getColumns(@PathVariable String tableName) {
        return Result.success(dataCenterService.getTableColumns(tableName));
    }

    @GetMapping("/tables/{tableName}/data")
    public Result<Map<String, Object>> getData(
            @PathVariable String tableName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String condition) {
        return Result.success(dataCenterService.getTableData(tableName, pageNum, pageSize, condition));
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add report-backend/src/main/java/com/report/controller/DataCenterController.java
git commit -m "feat(data-center): 添加 DataCenterController 接口层
关联任务: H-新功能"
```

---

### Task 5: 前端 API 层

**Files:**
- Create: `src/api/dataCenter.js`

- [ ] **Step 1: 创建 API 文件**

```javascript
import request from '@/utils/request'

export function getTableList(params) {
  return request({
    url: '/data-center/tables',
    method: 'get',
    params
  })
}

export function getTableDetail(tableName) {
  return request({
    url: `/data-center/tables/${tableName}`,
    method: 'get'
  })
}

export function updateTable(data) {
  return request({
    url: '/data-center/tables',
    method: 'put',
    data
  })
}

export function getUntaggedTables() {
  return request({
    url: '/data-center/untagged',
    method: 'get'
  })
}

export function scanTables() {
  return request({
    url: '/data-center/scan',
    method: 'post'
  })
}

export function getTableColumns(tableName) {
  return request({
    url: `/data-center/tables/${tableName}/columns`,
    method: 'get'
  })
}

export function getTableData(tableName, params) {
  return request({
    url: `/data-center/tables/${tableName}/data`,
    method: 'get',
    params
  })
}
```

- [ ] **Step 2: 提交**

```bash
git add src/api/dataCenter.js
git commit -m "feat(data-center): 添加数据中心 API 接口
关联任务: H-新功能"
```

---

### Task 6: 前端页面 - 数据中心入口

**Files:**
- Create: `src/views/data-center/Index.vue`

- [ ] **Step 1: 创建 Index.vue**

```vue
<template>
  <div class="data-center-container">
    <div class="left-panel">
      <div class="panel-header">
        <span class="panel-title">数据中心</span>
      </div>

      <div class="filter-section">
        <div class="filter-item">
          <label class="filter-label">按流向分层</label>
          <el-select v-model="filters.tableLayer" placeholder="请选择" clearable @change="handleFilterChange">
            <el-option label="ODS - 原始数据层" value="ODS" />
            <el-option label="DWD - 明细数据层" value="DWD" />
            <el-option label="DWS - 汇总数据层" value="DWS" />
            <el-option label="ADS - 应用数据层" value="ADS" />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">按来源类型</label>
          <el-select v-model="filters.sourceType" placeholder="请选择" clearable @change="handleFilterChange">
            <el-option label="FTP来源" value="FTP来源" />
            <el-option label="Trigger触发" value="TRIGGER" />
            <el-option label="Pipeline生成" value="PIPELINE" />
            <el-option label="手动创建" value="MANUAL" />
          </el-select>
        </div>

        <div class="filter-item">
          <label class="filter-label">按业务域</label>
          <el-input v-model="filters.businessDomain" placeholder="输入业务域模糊搜索" clearable @input="handleFilterChange" />
        </div>
      </div>

      <div class="table-tree">
        <div class="tree-node" :class="{ active: activeTab === 'untagged' }" @click="switchToUntagged">
          <span>📋 待标记表</span>
          <span class="count" v-if="untaggedCount > 0">{{ untaggedCount }}</span>
        </div>
        <div class="tree-node" :class="{ active: activeTable === item.tableName }"
             v-for="item in tableList" :key="item.id" @click="selectTable(item)">
          <span>📋 {{ item.tableName }}</span>
          <span class="count">{{ item.tableLayer || '未分层' }}</span>
        </div>
      </div>
    </div>

    <div class="main-panel">
      <div class="table-list-container">
        <div class="table-list-header">
          <span>表列表（{{ tableList.length }}）</span>
          <el-button type="primary" size="small" @click="handleScan">扫描新表</el-button>
        </div>
        <div class="table-list">
          <div class="table-item" :class="{ active: activeTable === item.tableName }"
               v-for="item in tableList" :key="item.id" @click="selectTable(item)">
            <div class="table-icon">{{ getLayerAbbr(item.tableLayer) }}</div>
            <div class="table-info">
              <div class="table-name">{{ item.tableName }}</div>
              <div class="table-meta">
                {{ item.businessDomain || '未设置业务域' }} · {{ item.sourceType || '未知来源' }}
              </div>
            </div>
            <div class="table-tags">
              <span class="tag" :class="item.tableLayer?.toLowerCase()">{{ item.tableLayer || '未分层' }}</span>
              <span class="tag" v-if="item.businessDomain">{{ item.businessDomain }}</span>
            </div>
          </div>
          <div v-if="tableList.length === 0" class="empty-state">
            <p>暂无数据</p>
          </div>
        </div>
      </div>

      <div class="table-detail-container" v-if="activeTable">
        <div class="detail-tabs">
          <div class="detail-tab" :class="{ active: detailTab === 'basic' }" @click="detailTab = 'basic'">基本信息</div>
          <div class="detail-tab" :class="{ active: detailTab === 'columns' }" @click="loadColumns">字段信息</div>
          <div class="detail-tab" :class="{ active: detailTab === 'data' }" @click="detailTab = 'data'">数据预览</div>
        </div>
        <div class="detail-content">
          <!-- 基本信息 -->
          <div v-if="detailTab === 'basic'" class="info-grid">
            <div class="info-item">
              <span class="info-label">表名称</span>
              <span class="info-value">{{ currentTable.tableName }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">流向分层</span>
              <span class="info-value">{{ currentTable.tableLayer || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">业务域</span>
              <span class="info-value">{{ currentTable.businessDomain || '未设置' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">来源类型</span>
              <span class="info-value">{{ currentTable.sourceType || '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">来源名称</span>
              <span class="info-value">{{ currentTable.sourceName || '未知' }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">表描述</span>
              <span class="info-value">{{ currentTable.description || '暂无描述' }}</span>
            </div>
            <div class="info-item full-width">
              <el-button type="primary" size="small" @click="handleEdit">编辑标记信息</el-button>
            </div>
          </div>

          <!-- 字段信息 -->
          <div v-if="detailTab === 'columns'">
            <el-table :data="columns" border stripe size="small">
              <el-table-column prop="columnName" label="字段名称" />
              <el-table-column prop="dataType" label="字段类型" width="150" />
              <el-table-column prop="columnComment" label="注释" />
            </el-table>
          </div>

          <!-- 数据预览 -->
          <div v-if="detailTab === 'data'">
            <div class="data-filter">
              <el-input v-model="dataCondition" placeholder="输入筛选条件，如：name LIKE '%张三%'" size="small" style="width: 300px;" />
              <el-button type="primary" size="small" @click="loadTableData">筛选</el-button>
              <el-button size="small" @click="dataCondition = ''; loadTableData()">重置</el-button>
            </div>
            <el-table :data="tableData" border stripe size="small" v-loading="dataLoading">
              <el-table-column v-for="col in dataColumns" :key="col" :prop="col" :label="col" />
            </el-table>
            <div class="pagination">
              <el-pagination
                @current-change="handlePageChange"
                :current-page="pagination.pageNum"
                :page-size="pagination.pageSize"
                layout="total, prev, pager, next"
                :total="pagination.total" />
            </div>
          </div>
        </div>
      </div>
      <div v-else class="empty-state" style="flex: 1;">
        <p>请选择左侧表查看详情</p>
      </div>
    </div>

    <el-dialog title="编辑标记信息" :visible.sync="editDialogVisible" width="500px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="流向分层">
          <el-select v-model="editForm.tableLayer" placeholder="请选择">
            <el-option label="ODS - 原始数据层" value="ODS" />
            <el-option label="DWD - 明细数据层" value="DWD" />
            <el-option label="DWS - 汇总数据层" value="DWS" />
            <el-option label="ADS - 应用数据层" value="ADS" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务域">
          <el-input v-model="editForm.businessDomain" placeholder="请输入业务域" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-select v-model="editForm.sourceType" placeholder="请选择">
            <el-option label="FTP来源" value="FTP来源" />
            <el-option label="Trigger触发" value="TRIGGER" />
            <el-option label="Pipeline生成" value="PIPELINE" />
            <el-option label="手动创建" value="MANUAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="表描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入表描述" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitEdit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getTableList, getTableDetail, updateTable, getUntaggedTables, scanTables, getTableColumns, getTableData } from '@/api/dataCenter'

export default {
  name: 'DataCenter',
  data() {
    return {
      filters: {
        tableLayer: '',
        sourceType: '',
        businessDomain: ''
      },
      activeTab: 'all',
      activeTable: '',
      tableList: [],
      untaggedCount: 0,
      currentTable: {},
      detailTab: 'basic',
      columns: [],
      tableData: [],
      dataColumns: [],
      dataCondition: '',
      dataLoading: false,
      pagination: {
        pageNum: 1,
        pageSize: 20,
        total: 0
      },
      editDialogVisible: false,
      editForm: {}
    }
  },
  mounted() {
    this.loadTables()
    this.loadUntaggedCount()
  },
  methods: {
    handleFilterChange() {
      this.loadTables()
    },
    loadTables() {
      getTableList(this.filters).then(res => {
        this.tableList = res.data || []
      })
    },
    loadUntaggedCount() {
      getUntaggedTables().then(res => {
        this.untaggedCount = (res.data || []).length
      })
    },
    switchToUntagged() {
      this.activeTab = 'untagged'
      getUntaggedTables().then(res => {
        this.tableList = res.data || []
      })
    },
    selectTable(item) {
      this.activeTable = item.tableName
      this.currentTable = item
      this.detailTab = 'basic'
    },
    loadColumns() {
      if (!this.activeTable) return
      this.detailTab = 'columns'
      getTableColumns(this.activeTable).then(res => {
        this.columns = res.data || []
      })
    },
    handlePageChange(page) {
      this.pagination.pageNum = page
      this.loadTableData()
    },
    loadTableData() {
      if (!this.activeTable) return
      this.detailTab = 'data'
      this.dataLoading = true
      getTableData(this.activeTable, {
        pageNum: this.pagination.pageNum,
        pageSize: this.pagination.pageSize,
        condition: this.dataCondition
      }).then(res => {
        this.dataLoading = false
        const data = res.data || {}
        this.tableData = data.records || []
        this.pagination.total = data.total || 0
        if (this.tableData.length > 0) {
          this.dataColumns = Object.keys(this.tableData[0])
        }
      }).catch(() => {
        this.dataLoading = false
      })
    },
    handleEdit() {
      this.editForm = { ...this.currentTable }
      this.editDialogVisible = true
    },
    handleSubmitEdit() {
      updateTable(this.editForm).then(() => {
        this.$message.success('更新成功')
        this.editDialogVisible = false
        this.loadTables()
        this.loadUntaggedCount()
      })
    },
    handleScan() {
      this.$confirm('是否扫描新表？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        scanTables().then(res => {
          const count = (res.data || []).length
          this.$message.success(`扫描完成，发现 ${count} 张新表`)
          this.loadTables()
          this.loadUntaggedCount()
        })
      })
    },
    getLayerAbbr(layer) {
      return layer || '??'
    }
  }
}
</script>

<style scoped>
.data-center-container {
  display: flex;
  height: calc(100vh - 84px);
}
.left-panel {
  width: 280px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
}
.panel-header {
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}
.panel-title {
  font-size: 16px;
  font-weight: 600;
}
.filter-section {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
}
.filter-item {
  margin-bottom: 12px;
}
.filter-item:last-child {
  margin-bottom: 0;
}
.filter-label {
  display: block;
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}
.table-tree {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.tree-node {
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  margin-bottom: 4px;
  font-size: 14px;
}
.tree-node:hover {
  background: #f5f7fa;
}
.tree-node.active {
  background: #ecf5ff;
  color: #409eff;
}
.tree-node .count {
  margin-left: auto;
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
  color: #666;
}
.main-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.table-list-container {
  height: 45%;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  background: #fff;
}
.table-list-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.table-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 16px;
}
.table-item {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  margin-top: 8px;
  cursor: pointer;
  border: 1px solid transparent;
}
.table-item:hover {
  border-color: #409eff;
  background: #fff;
}
.table-item.active {
  border-color: #409eff;
  background: #ecf5ff;
}
.table-icon {
  width: 36px;
  height: 36px;
  background: #409eff;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  font-weight: bold;
  margin-right: 10px;
}
.table-info {
  flex: 1;
}
.table-name {
  font-size: 13px;
  font-weight: 500;
}
.table-meta {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.table-tags {
  display: flex;
  gap: 4px;
}
.tag {
  padding: 2px 6px;
  background: #f0f2f5;
  border-radius: 4px;
  font-size: 11px;
  color: #666;
}
.tag.ods { background: #e6f7ff; color: #1890ff; }
.tag.dwd { background: #f6ffed; color: #52c41a; }
.tag.dws { background: #fff7e6; color: #faad14; }
.tag.ads { background: #fff1f0; color: #ff4d4f; }
.table-detail-container {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.detail-tabs {
  display: flex;
  border-bottom: 1px solid #e8e8e8;
  padding: 0 16px;
}
.detail-tab {
  padding: 12px 16px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}
.detail-tab:hover {
  color: #333;
}
.detail-tab.active {
  color: #409eff;
  border-bottom-color: #409eff;
}
.detail-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.info-item.full-width {
  grid-column: span 2;
}
.info-label {
  font-size: 12px;
  color: #999;
}
.info-value {
  font-size: 14px;
  color: #333;
}
.data-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #999;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add src/views/data-center/Index.vue
git commit -m "feat(data-center): 添加数据中心前端页面
关联任务: H-新功能"
```

---

### Task 7: 前端路由配置

**Files:**
- Modify: `src/router/index.js:1-57`

- [ ] **Step 1: 添加路由**

在 routes 数组中添加：

```javascript
{
  path: '/data-center',
  name: 'DataCenter',
  component: () => import('@/views/data-center/Index.vue')
}
```

- [ ] **Step 2: 提交**

```bash
git add src/router/index.js
git commit -m "feat(data-center): 添加数据中心路由配置
关联任务: H-新功能"
```

---

## 三、Spec 覆盖检查

| 设计文档章节 | 对应任务 |
|------------|---------|
| 数据库设计 | Task 1, 2 |
| API 接口 | Task 4 |
| 前端结构 | Task 5, 6, 7 |
| 组合筛选 | Task 6 |
| 数据预览 | Task 6 |
| 标记管理 | Task 6 |
| 扫描新表 | Task 3, 6 |
| 待标记表 | Task 6 |

**Gap 检查**: 无遗漏

---

## 四、类型一致性检查

| 项目 | 位置 | 一致性 |
|------|------|--------|
| TableLayerMapping 实体 | Task 2 | ✅ |
| Service 接口签名 | Task 3 | ✅ |
| Controller API 路径 | Task 4 | ✅ |
| 前端 API 调用 | Task 5 | ✅ |
| 页面数据绑定 | Task 6 | ✅ |

---

**Plan complete and saved to `docs/superpowers/plans/2026-04-08-data-center-plan.md`.**

Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
