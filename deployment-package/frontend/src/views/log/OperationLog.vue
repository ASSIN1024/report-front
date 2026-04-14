<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">操作日志</span>
    </div>

    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="操作模块">
        <el-input v-model="searchForm.module" placeholder="请输入模块名称" clearable />
      </el-form-item>
      <el-form-item label="操作类型">
        <el-select v-model="searchForm.operationType" placeholder="请选择操作类型" clearable>
          <el-option label="创建" value="CREATE" />
          <el-option label="修改" value="UPDATE" />
          <el-option label="删除" value="DELETE" />
          <el-option label="测试" value="TEST" />
        </el-select>
      </el-form-item>
      <el-form-item label="操作结果">
        <el-select v-model="searchForm.result" placeholder="请选择结果" clearable>
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border stripe>
      <el-table-column prop="module" label="操作模块" width="120" />
      <el-table-column prop="operationType" label="操作类型" width="100">
        <template slot-scope="{ row }">
          <el-tag :type="getTypeTagType(row.operationType)" size="small">
            {{ getTypeText(row.operationType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="operationDesc" label="操作描述" />
      <el-table-column prop="targetName" label="目标对象" width="150" />
      <el-table-column prop="result" label="结果" width="80">
        <template slot-scope="{ row }">
          <el-tag :type="row.result === 1 ? 'success' : 'danger'" size="small">
            {{ row.result === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="耗时(ms)" width="100" />
      <el-table-column prop="operatorIp" label="操作IP" width="130" />
      <el-table-column prop="createTime" label="操作时间" width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="handleDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @pagination="handlePagination"
    />

    <el-dialog title="操作日志详情" :visible.sync="detailVisible" width="700px">
      <el-descriptions :column="2" border v-if="currentLog">
        <el-descriptions-item label="操作模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ getTypeText(currentLog.operationType) }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentLog.operationDesc }}</el-descriptions-item>
        <el-descriptions-item label="目标ID">{{ currentLog.targetId }}</el-descriptions-item>
        <el-descriptions-item label="目标名称">{{ currentLog.targetName }}</el-descriptions-item>
        <el-descriptions-item label="操作结果">
          <el-tag :type="currentLog.result === 1 ? 'success' : 'danger'" size="small">
            {{ currentLog.result === 1 ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentLog.duration }}ms</el-descriptions-item>
        <el-descriptions-item label="操作IP">{{ currentLog.operatorIp }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.createTime }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="currentLog.errorMsg">
          <span style="color: #F56C6C">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
      </el-descriptions>
      
      <div v-if="currentLog && currentLog.beforeData" style="margin-top: 20px">
        <div class="detail-section-title">操作前数据:</div>
        <pre class="json-display">{{ formatJson(currentLog.beforeData) }}</pre>
      </div>
      
      <div v-if="currentLog && currentLog.afterData" style="margin-top: 10px">
        <div class="detail-section-title">操作后数据:</div>
        <pre class="json-display">{{ formatJson(currentLog.afterData) }}</pre>
      </div>
      
      <span slot="footer">
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getOperationLogPage, getOperationLogById } from '@/api/operationLog'
import Pagination from '@/components/Pagination'

export default {
  name: 'OperationLog',
  components: { Pagination },
  data() {
    return {
      searchForm: {
        module: '',
        operationType: '',
        result: null
      },
      tableData: [],
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      },
      detailVisible: false,
      currentLog: null
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const params = {
          ...this.searchForm,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        }
        const res = await getOperationLogPage(params)
        this.tableData = res.data.records
        this.pagination.total = res.data.total
      } catch (error) {
        console.error('加载数据失败', error)
      }
    },
    handleSearch() {
      this.pagination.pageNum = 1
      this.loadData()
    },
    handleReset() {
      this.searchForm = { module: '', operationType: '', result: null }
      this.handleSearch()
    },
    handlePagination({ page, rows }) {
      this.pagination.pageNum = page
      this.pagination.pageSize = rows
      this.loadData()
    },
    async handleDetail(row) {
      try {
        const res = await getOperationLogById(row.id)
        this.currentLog = res.data
        this.detailVisible = true
      } catch (error) {
        this.$message.error('获取详情失败')
      }
    },
    getTypeTagType(type) {
      const map = {
        'CREATE': 'success',
        'UPDATE': 'primary',
        'DELETE': 'danger',
        'TEST': 'warning'
      }
      return map[type] || 'info'
    },
    getTypeText(type) {
      const map = {
        'CREATE': '创建',
        'UPDATE': '修改',
        'DELETE': '删除',
        'TEST': '测试'
      }
      return map[type] || type
    },
    formatJson(jsonStr) {
      try {
        return JSON.stringify(JSON.parse(jsonStr), null, 2)
      } catch (e) {
        return jsonStr
      }
    }
  }
}
</script>

<style scoped>
.detail-section-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #303133;
}
.json-display {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
