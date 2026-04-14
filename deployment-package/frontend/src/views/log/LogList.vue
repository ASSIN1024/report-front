<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">执行日志</span>
    </div>

    <el-table :data="tableData" border stripe v-loading="loading">
      <el-table-column prop="taskExecutionId" label="任务ID" width="180" />
      <el-table-column prop="logLevel" label="日志级别" width="100">
        <template slot-scope="{ row }">
          <el-tag v-if="row.logLevel === 'ERROR'" type="danger" size="small">{{ row.logLevel }}</el-tag>
          <el-tag v-else-if="row.logLevel === 'WARN'" type="warning" size="small">{{ row.logLevel }}</el-tag>
          <el-tag v-else type="info" size="small">{{ row.logLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="logMessage" label="日志内容" />
      <el-table-column prop="createTime" label="时间" width="180">
        <template slot-scope="{ row }">
          {{ row.createTime | formatTime }}
        </template>
      </el-table-column>
    </el-table>

    <pagination
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @pagination="handlePagination"
    />
  </div>
</template>

<script>
import Pagination from '@/components/Pagination'

export default {
  name: 'LogList',
  components: { Pagination },
  data() {
    return {
      tableData: [],
      loading: false,
      pagination: {
        pageNum: 1,
        pageSize: 20,
        total: 0
      }
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
        const res = await this.$store.dispatch('log/getPage', {
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        })
        this.tableData = res.data.records
        this.pagination.total = res.data.total
      } catch (error) {
        console.error('加载数据失败', error)
      } finally {
        this.loading = false
      }
    },
    handlePagination({ page, rows }) {
      this.pagination.pageNum = page
      this.pagination.pageSize = rows
      this.loadData()
    }
  }
}
</script>
