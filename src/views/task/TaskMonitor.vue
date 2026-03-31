<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">任务监控</span>
    </div>

    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="任务类型">
        <el-select v-model="searchForm.taskType" placeholder="请选择任务类型" clearable>
          <el-option label="FTP扫描" value="FTP_SCAN" />
          <el-option label="文件处理" value="FILE_PROCESS" />
          <el-option label="数据导入" value="DATA_IMPORT" />
        </el-select>
      </el-form-item>
      <el-form-item label="任务名称">
        <el-input v-model="searchForm.taskName" placeholder="请输入任务名称" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
          <el-option label="待执行" value="PENDING" />
          <el-option label="执行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="taskName" label="任务名称" />
      <el-table-column prop="taskType" label="任务类型" width="120">
        <template slot-scope="{ row }">
          <span v-if="row.taskType === 'FTP_SCAN'">FTP扫描</span>
          <span v-else-if="row.taskType === 'FILE_PROCESS'">文件处理</span>
          <span v-else-if="row.taskType === 'DATA_IMPORT'">数据导入</span>
          <span v-else>{{ row.taskType }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="fileName" label="文件名" />
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="{ row }">
          <status-tag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="totalRows" label="总行数" width="100" align="center" />
      <el-table-column prop="successRows" label="成功行数" width="100" align="center" />
      <el-table-column prop="failedRows" label="失败行数" width="100" align="center" />
      <el-table-column prop="startTime" label="开始时间" width="160">
        <template slot-scope="{ row }">
          {{ row.startTime | formatTime }}
        </template>
      </el-table-column>
      <el-table-column prop="duration" label="执行时长" width="120">
        <template slot-scope="{ row }">
          {{ row.duration ? row.duration + 'ms' : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="handleViewLog(row)">查看日志</el-button>
          <el-button type="text" size="small" v-if="row.status === 'FAILED'" @click="handleRetry(row)">重试</el-button>
          <el-button type="text" size="small" v-if="row.status === 'PENDING'" @click="handleCancel(row)">取消</el-button>
          <el-button type="text" size="small" style="color: #F56C6C" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @pagination="handlePagination"
    />

    <el-dialog title="执行日志" :visible.sync="logDialogVisible" width="800px">
      <div class="log-container">
        <div v-for="(log, index) in logList" :key="index" :class="'log-' + log.logLevel.toLowerCase()">
          <span class="log-time">{{ log.createTime | formatTime }}</span>
          <span class="log-level">[{{ log.logLevel }}]</span>
          <span class="log-message">{{ log.logMessage }}</span>
        </div>
      </div>
      <span slot="footer">
        <el-button @click="logDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getTaskPage, retryTask, cancelTask, deleteTask } from '@/api/task'
import { getLogList } from '@/api/log'
import StatusTag from '@/components/StatusTag'
import Pagination from '@/components/Pagination'

export default {
  name: 'TaskMonitor',
  components: { StatusTag, Pagination },
  data() {
    return {
      searchForm: {
        taskType: '',
        taskName: '',
        status: ''
      },
      tableData: [],
      loading: false,
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      },
      logDialogVisible: false,
      logList: []
    }
  },
  filters: {
    formatTime(time) {
      if (!time) return '-'
      const date = new Date(time)
      const pad = n => n < 10 ? '0' + n : n
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      this.loading = true
      try {
        const params = {
          ...this.searchForm,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        }
        const res = await getTaskPage(params)
        this.tableData = res.data.records
        this.pagination.total = parseInt(res.data.total) || 0
      } catch (error) {
        console.error('加载数据失败', error)
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.pagination.pageNum = 1
      this.loadData()
    },
    handleReset() {
      this.searchForm = { taskType: '', taskName: '', status: '' }
      this.handleSearch()
    },
    handlePagination({ page, rows }) {
      this.pagination.pageNum = page
      this.pagination.pageSize = rows
      this.loadData()
    },
    async handleViewLog(row) {
      try {
        const res = await getLogList(row.id)
        this.logList = res.data
        this.logDialogVisible = true
      } catch (error) {
        this.$message.error('加载日志失败')
      }
    },
    async handleRetry(row) {
      try {
        await retryTask(row.id)
        this.$message.success('任务已重新提交')
        this.loadData()
      } catch (error) {
        this.$message.error('重试失败')
      }
    },
    async handleCancel(row) {
      try {
        await cancelTask(row.id)
        this.$message.success('任务已取消')
        this.loadData()
      } catch (error) {
        this.$message.error('取消失败')
      }
    },
    handleDelete(row) {
      this.$confirm('确认删除该任务记录?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteTask(row.id)
          this.$message.success('删除成功')
          this.loadData()
        } catch (error) {
          this.$message.error('删除失败')
        }
      })
    }
  }
}
</script>

<style scoped>
.log-container {
  max-height: 400px;
  overflow-y: auto;
  font-family: 'Courier New', monospace;
  font-size: 12px;
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 10px;
  border-radius: 4px;
}

.log-info { color: #d4d4d4; }
.log-warn { color: #e6a23c; }
.log-error { color: #f56c6c; }

.log-time { color: #808080; margin-right: 10px; }
.log-level { margin-right: 10px; }
.log-message { word-break: break-all; }
</style>
