<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">系统日志</span>
      <el-button type="primary" icon="el-icon-download" @click="handleExport">导出日志</el-button>
    </div>

    <el-form :inline="true" :model="queryForm" class="search-form">
      <el-form-item label="日志类型">
        <el-select v-model="queryForm.logType" placeholder="请选择日志类型" @change="handleLogTypeChange">
          <el-option label="全部日志" value="all" />
          <el-option label="错误日志" value="error" />
          <el-option label="操作日志" value="operation" />
          <el-option label="访问日志" value="access" />
        </el-select>
      </el-form-item>
      <el-form-item label="日志级别">
        <el-select v-model="queryForm.level" placeholder="请选择级别" clearable>
          <el-option label="DEBUG" value="DEBUG" />
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="queryForm.keyword" placeholder="请输入关键词" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleRefresh">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="20" class="log-info">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="info-item">
            <span class="info-label">日志文件:</span>
            <span class="info-value">{{ currentFileInfo.fileName || '-' }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="info-item">
            <span class="info-label">文件大小:</span>
            <span class="info-value">{{ formatFileSize(currentFileInfo.fileSize) }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="info-item">
            <span class="info-label">匹配行数:</span>
            <span class="info-value">{{ logResult.total || 0 }}</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="info-item">
            <span class="info-label">最后更新:</span>
            <span class="info-value">{{ formatTime(currentFileInfo.lastModified) }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="log-container">
      <div class="log-header">
        <span>日志内容</span>
        <el-button-group>
          <el-button size="mini" @click="scrollToTop">顶部</el-button>
          <el-button size="mini" @click="scrollToBottom">底部</el-button>
        </el-button-group>
      </div>
      <div class="log-content" ref="logContent">
        <div v-if="logResult.lines && logResult.lines.length > 0">
          <div 
            v-for="line in logResult.lines" 
            :key="line.lineNumber" 
            class="log-line"
            :class="getLogLevelClass(line.level)"
          >
            <span class="line-number">{{ line.lineNumber }}</span>
            <span class="line-timestamp">{{ line.timestamp }}</span>
            <span class="line-level" :class="'level-' + (line.level || 'INFO').toLowerCase()">{{ line.level || 'INFO' }}</span>
            <span class="line-logger">{{ line.logger }}</span>
            <span class="line-message">{{ line.message }}</span>
          </div>
        </div>
        <div v-else class="log-empty">
          <i class="el-icon-document"></i>
          <p>暂无日志数据</p>
        </div>
      </div>
    </div>

    <pagination
      :current-page="queryForm.pageNum"
      :page-size="queryForm.pageSize"
      :total="logResult.total || 0"
      @pagination="handlePagination"
    />
  </div>
</template>

<script>
import { getLogFileList, getLogFileInfo, queryLogs, exportLog } from '@/api/logFile'
import Pagination from '@/components/Pagination'

export default {
  name: 'SystemLog',
  components: { Pagination },
  data() {
    return {
      queryForm: {
        logType: 'all',
        level: '',
        keyword: '',
        startDate: '',
        endDate: '',
        pageNum: 1,
        pageSize: 100
      },
      logResult: {},
      currentFileInfo: {},
      logFiles: []
    }
  },
  created() {
    this.loadLogFiles()
    this.loadLogInfo()
    this.loadLogs()
  },
  methods: {
    async loadLogFiles() {
      try {
        const res = await getLogFileList()
        this.logFiles = res.data || []
      } catch (error) {
        console.error('加载日志文件列表失败', error)
      }
    },
    async loadLogInfo() {
      try {
        const res = await getLogFileInfo(this.queryForm.logType)
        this.currentFileInfo = res.data || {}
      } catch (error) {
        console.error('加载日志文件信息失败', error)
      }
    },
    async loadLogs() {
      try {
        const res = await queryLogs(this.queryForm)
        this.logResult = res.data || {}
      } catch (error) {
        console.error('加载日志失败', error)
      }
    },
    handleLogTypeChange() {
      this.queryForm.pageNum = 1
      this.loadLogInfo()
      this.loadLogs()
    },
    handleQuery() {
      this.queryForm.pageNum = 1
      this.loadLogs()
    },
    handleRefresh() {
      this.loadLogInfo()
      this.loadLogs()
      this.$message.success('日志已刷新')
    },
    handlePagination({ page, rows }) {
      this.queryForm.pageNum = page
      this.queryForm.pageSize = rows
      this.loadLogs()
    },
    async handleExport() {
      try {
        const response = await exportLog(this.queryForm.logType, this.queryForm.level, this.queryForm.keyword)
        const blob = new Blob([response], { type: 'application/octet-stream' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = `${this.queryForm.logType}-${Date.now()}.log`
        link.click()
        window.URL.revokeObjectURL(url)
        this.$message.success('日志导出成功')
      } catch (error) {
        this.$message.error('导出失败')
      }
    },
    scrollToTop() {
      this.$refs.logContent.scrollTop = 0
    },
    scrollToBottom() {
      const container = this.$refs.logContent
      container.scrollTop = container.scrollHeight
    },
    getLogLevelClass(level) {
      if (!level) return ''
      return 'log-' + level.toLowerCase()
    },
    formatFileSize(bytes) {
      if (!bytes) return '0 B'
      const units = ['B', 'KB', 'MB', 'GB']
      let i = 0
      while (bytes >= 1024 && i < units.length - 1) {
        bytes /= 1024
        i++
      }
      return bytes.toFixed(2) + ' ' + units[i]
    },
    formatTime(timestamp) {
      if (!timestamp) return '-'
      return new Date(timestamp).toLocaleString()
    }
  }
}
</script>

<style scoped>
.log-info {
  margin-bottom: 20px;
}
.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.info-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.info-value {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}
.log-container {
  border: 1px solid #DCDFE6;
  border-radius: 4px;
  margin-bottom: 20px;
}
.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: #F5F7FA;
  border-bottom: 1px solid #DCDFE6;
  font-weight: bold;
}
.log-content {
  height: 500px;
  overflow-y: auto;
  background: #1E1E1E;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  line-height: 1.6;
}
.log-line {
  display: flex;
  padding: 2px 10px;
  color: #D4D4D4;
  border-bottom: 1px solid #2D2D2D;
}
.log-line:hover {
  background: #2D2D2D;
}
.line-number {
  width: 60px;
  color: #858585;
  text-align: right;
  padding-right: 15px;
  flex-shrink: 0;
}
.line-timestamp {
  width: 180px;
  color: #569CD6;
  flex-shrink: 0;
}
.line-level {
  width: 60px;
  text-align: center;
  margin-right: 10px;
  flex-shrink: 0;
}
.level-debug { color: #608B4E; }
.level-info { color: #4EC9B0; }
.level-warn { color: #DCDCAA; }
.level-error { color: #F14C4C; font-weight: bold; }
.line-logger {
  width: 200px;
  color: #9CDCFE;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-right: 10px;
  flex-shrink: 0;
}
.line-message {
  flex: 1;
  color: #CE9178;
  word-break: break-all;
}
.log-debug { background: rgba(96, 139, 78, 0.1); }
.log-info { background: rgba(78, 201, 176, 0.05); }
.log-warn { background: rgba(220, 220, 170, 0.1); }
.log-error { background: rgba(241, 76, 76, 0.15); }
.log-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}
.log-empty i {
  font-size: 48px;
  margin-bottom: 10px;
}
</style>
