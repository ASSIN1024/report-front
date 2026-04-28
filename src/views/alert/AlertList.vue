<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">告警管理</span>
    </div>

    <el-form :inline="true" :model="queryForm" class="search-form">
      <el-form-item label="级别">
        <el-select v-model="queryForm.level" placeholder="全部" clearable>
          <el-option label="ERROR" value="ERROR" />
          <el-option label="WARNING" value="WARNING" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="queryForm.type" placeholder="全部" clearable>
          <el-option label="映射失败" value="MAPPING_FAILED" />
          <el-option label="字段缺失" value="FIELD_MISSING" />
          <el-option label="解析错误" value="PARSE_ERROR" />
          <el-option label="FTP错误" value="FTP_ERROR" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryForm.status" placeholder="全部" clearable>
          <el-option label="未解决" value="OPEN" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border stripe>
      <el-table-column prop="fileName" label="文件名" min-width="180" />
      <el-table-column prop="alertLevel" label="级别" width="100">
        <template slot-scope="{ row }">
          <el-tag :type="row.alertLevel === 'ERROR' ? 'danger' : 'warning'" size="small">
            {{ row.alertLevel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="alertType" label="类型" width="140" />
      <el-table-column prop="alertMessage" label="告警信息" min-width="250" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="100">
        <template slot-scope="{ row }">
          <el-tag :type="row.status === 'OPEN' ? 'danger' : 'success'" size="small">
            {{ row.status === 'OPEN' ? '未解决' : '已解决' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template slot-scope="{ row }">
          <el-button
            v-if="row.status === 'OPEN'"
            type="text"
            size="small"
            @click="handleResolve(row)"
          >解决</el-button>
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
import { listAlerts, resolveAlert } from '@/api/alert'
import Pagination from '@/components/Pagination'

export default {
  name: 'AlertList',
  components: { Pagination },
  data() {
    return {
      queryForm: {
        level: '',
        type: '',
        status: ''
      },
      tableData: [],
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const params = {
          ...this.queryForm,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        }
        const res = await listAlerts(params)
        this.tableData = res.data.records
        this.pagination.total = res.data.total
      } catch (error) {
        console.error('加载告警数据失败', error)
      }
    },
    handleSearch() {
      this.pagination.pageNum = 1
      this.loadData()
    },
    handleReset() {
      this.queryForm = { level: '', type: '', status: '' }
      this.handleSearch()
    },
    handlePagination({ page, rows }) {
      this.pagination.pageNum = page
      this.pagination.pageSize = rows
      this.loadData()
    },
    handleResolve(row) {
      this.$confirm('确认标记该告警为已解决?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await resolveAlert(row.id)
          this.$message.success('已标记为已解决')
          this.loadData()
        } catch (error) {
          this.$message.error('操作失败')
        }
      })
    }
  }
}
</script>
