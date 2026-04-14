<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">报表配置管理</span>
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增报表</el-button>
    </div>

    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="报表名称">
        <el-input v-model="searchForm.reportName" placeholder="请输入报表名称" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border stripe>
      <el-table-column prop="reportCode" label="报表编码" />
      <el-table-column prop="reportName" label="报表名称" />
      <el-table-column prop="ftpConfigName" label="关联FTP" />
      <el-table-column prop="filePattern" label="文件匹配" />
      <el-table-column prop="outputTable" label="输出表" />
      <el-table-column prop="outputMode" label="输出模式" width="100">
        <template slot-scope="{ row }">
          {{ row.outputMode === 'APPEND' ? '追加' : '覆盖' }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="{ row }">
          <status-tag :status="String(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="text" size="small" @click="handleView(row)">查看</el-button>
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
import { getReportConfigPage, deleteReportConfig, triggerScan } from '@/api/reportConfig'
import StatusTag from '@/components/StatusTag'
import Pagination from '@/components/Pagination'

export default {
  name: 'ReportList',
  components: { StatusTag, Pagination },
  data() {
    return {
      searchForm: {
        reportName: '',
        status: null
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
          ...this.searchForm,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        }
        const res = await getReportConfigPage(params)
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
      this.searchForm = { reportName: '', status: null }
      this.handleSearch()
    },
    handlePagination({ page, rows }) {
      this.pagination.pageNum = page
      this.pagination.pageSize = rows
      this.loadData()
    },
    handleAdd() {
      this.$router.push('/report/config')
    },
    handleEdit(row) {
      this.$router.push(`/report/config/${row.id}`)
    },
    handleView(row) {
      this.$router.push(`/report/config/${row.id}?readonly=true`)
    },
    handleDelete(row) {
      this.$confirm('确认删除该报表配置?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteReportConfig(row.id)
          this.$message.success('删除成功')
          this.loadData()
        } catch (error) {
          this.$message.error('删除失败')
        }
      })
    },
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
        await triggerScan(row.id)
        this.$message.success('扫描任务已创建，请到任务监控中查看结果')
        this.loadData()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error(error.message || '扫描启动失败')
        }
      }
    }
  }
}
</script>
