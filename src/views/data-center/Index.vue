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
              <span class="tag" :class="getLayerClass(item.tableLayer)">{{ item.tableLayer || '未分层' }}</span>
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

          <div v-if="detailTab === 'columns'">
            <el-table :data="columns" border stripe size="small">
              <el-table-column prop="columnName" label="字段名称" />
              <el-table-column prop="dataType" label="字段类型" width="150" />
              <el-table-column prop="columnComment" label="注释" />
            </el-table>
          </div>

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
    },
    getLayerClass(layer) {
      return layer ? layer.toLowerCase() : ''
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