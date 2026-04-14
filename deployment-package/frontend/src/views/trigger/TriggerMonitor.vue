<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">Trigger监控</span>
      <el-button type="primary" icon="el-icon-refresh" @click="handleRefresh">刷新</el-button>
    </div>

    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
          <el-option label="全部" value="" />
          <el-option label="等待中" value="WAITING" />
          <el-option label="已触发" value="TRIGGERED" />
          <el-option label="已跳过" value="SKIPPED" />
        </el-select>
      </el-form-item>
      <el-form-item label="Trigger名称">
        <el-input v-model="searchForm.triggerName" placeholder="请输入Trigger名称" clearable />
      </el-form-item>
    </el-form>

    <el-table :data="filteredTriggerStates" border stripe v-loading="loading">
      <el-table-column prop="triggerName" label="Trigger名称" min-width="150" />
      <el-table-column prop="triggerCode" label="Trigger编码" width="150" />
      <el-table-column prop="status" label="状态" width="120">
        <template slot-scope="{ row }">
          <el-tag :type="getStatusType(row.status)" size="small">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="重试进度" width="120">
        <template slot-scope="{ row }">
          {{ row.retryCount }}/{{ row.maxRetries }}
        </template>
      </el-table-column>
      <el-table-column prop="pollIntervalSeconds" label="轮询间隔(秒)" width="120">
        <template slot-scope="{ row }">
          {{ row.pollIntervalSeconds || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="lastCheckTime" label="最后检查时间" width="160">
        <template slot-scope="{ row }">
          {{ row.lastCheckTime | formatTime }}
        </template>
      </el-table-column>
      <el-table-column prop="lastTriggerTime" label="最后触发时间" width="160">
        <template slot-scope="{ row }">
          {{ row.lastTriggerTime | formatTime }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="handleViewHistory(row)">查看历史</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="status-legend">
      <span class="legend-title">状态说明：</span>
      <el-tag type="warning" size="small">WAITING - 等待源数据就绪</el-tag>
      <el-tag type="success" size="small">TRIGGERED - 已触发Pipeline执行</el-tag>
      <el-tag type="danger" size="small">SKIPPED - 等待超时已跳过</el-tag>
    </div>

    <el-dialog title="Trigger执行历史" :visible.sync="historyDialogVisible" width="800px">
      <el-table :data="historyList" border stripe max-height="400">
        <el-table-column prop="executionTime" label="执行时间" width="160">
          <template slot-scope="{ row }">
            {{ row.executionTime | formatTime }}
          </template>
        </el-table-column>
        <el-table-column prop="triggerStatus" label="状态" width="100">
          <template slot-scope="{ row }">
            <el-tag :type="getStatusType(row.triggerStatus)" size="small">
              {{ getStatusText(row.triggerStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="dataCount" label="数据行数" width="100" align="center" />
        <el-table-column prop="partitionDate" label="分区日期" width="120" />
        <el-table-column prop="pipelineTaskId" label="Pipeline任务ID" width="120">
          <template slot-scope="{ row }">
            {{ row.pipelineTaskId || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="错误信息" min-width="150">
          <template slot-scope="{ row }">
            {{ row.errorMessage || '-' }}
          </template>
        </el-table-column>
      </el-table>
      <span slot="footer">
        <el-button @click="historyDialogVisible = false">关闭</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getTriggerStateList, getTriggerHistory } from '@/api/trigger'

export default {
  name: 'TriggerMonitor',
  data() {
    return {
      triggerStates: [],
      loading: false,
      searchForm: {
        status: '',
        triggerName: ''
      },
      historyDialogVisible: false,
      historyList: [],
      currentTriggerCode: '',
      refreshTimer: null
    }
  },
  computed: {
    filteredTriggerStates() {
      let result = this.triggerStates
      if (this.searchForm.status) {
        result = result.filter(item => item.status === this.searchForm.status)
      }
      if (this.searchForm.triggerName) {
        result = result.filter(item =>
          item.triggerName && item.triggerName.includes(this.searchForm.triggerName)
        )
      }
      return result
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
    this.loadTriggerStates()
    this.startAutoRefresh()
  },
  beforeDestroy() {
    this.stopAutoRefresh()
  },
  methods: {
    async loadTriggerStates() {
      this.loading = true
      try {
        const res = await getTriggerStateList()
        this.triggerStates = res.data || []
      } catch (error) {
        console.error('加载Trigger状态失败', error)
        this.$message.error('加载Trigger状态失败')
      } finally {
        this.loading = false
      }
    },
    handleRefresh() {
      this.loadTriggerStates()
    },
    startAutoRefresh() {
      this.refreshTimer = setInterval(() => {
        this.loadTriggerStates()
      }, 30000)
    },
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer)
        this.refreshTimer = null
      }
    },
    async handleViewHistory(row) {
      this.currentTriggerCode = row.triggerCode
      this.historyDialogVisible = true
      try {
        const res = await getTriggerHistory(row.triggerCode)
        this.historyList = res.data || []
      } catch (error) {
        console.error('加载历史记录失败', error)
        this.$message.error('加载历史记录失败')
      }
    },
    getStatusType(status) {
      const typeMap = {
        'WAITING': 'warning',
        'CHECKING': 'primary',
        'TRIGGERED': 'success',
        'SKIPPED': 'danger'
      }
      return typeMap[status] || 'info'
    },
    getStatusText(status) {
      const textMap = {
        'WAITING': '等待中',
        'CHECKING': '检查中',
        'TRIGGERED': '已触发',
        'SKIPPED': '已跳过'
      }
      return textMap[status] || status
    }
  }
}
</script>

<style scoped>
.page-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: bold;
}

.search-form {
  margin-bottom: 20px;
}

.status-legend {
  margin-top: 20px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.legend-title {
  font-weight: bold;
  color: #606266;
}
</style>