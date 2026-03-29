<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">数据管理</span>
    </div>

    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="目标表">
        <el-select v-model="searchForm.tableName" placeholder="请选择目标表" clearable @change="handleTableChange">
          <el-option v-for="table in tableList" :key="table" :label="table" :value="table" />
        </el-select>
      </el-form-item>
      <el-form-item label="条件">
        <el-input v-model="searchForm.condition" placeholder="输入WHERE条件" clearable style="width: 300px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border stripe v-loading="loading" max-height="400">
      <el-table-column v-for="column in columns" :key="column" :prop="column" :label="column" />
    </el-table>

    <pagination
      v-if="searchForm.tableName"
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @pagination="handlePagination"
    />
  </div>
</template>

<script>
import { getOutputTables, getTableColumns, queryData } from '@/api/data'
import Pagination from '@/components/Pagination'

export default {
  name: 'DataManagement',
  components: { Pagination },
  data() {
    return {
      searchForm: {
        tableName: '',
        condition: ''
      },
      tableList: [],
      columns: [],
      tableData: [],
      loading: false,
      pagination: {
        pageNum: 1,
        pageSize: 20,
        total: 0
      }
    }
  },
  created() {
    this.loadTables()
  },
  methods: {
    async loadTables() {
      try {
        const res = await getOutputTables()
        this.tableList = res.data || []
      } catch (error) {
        console.error('加载表列表失败', error)
      }
    },
    async handleTableChange(tableName) {
      if (!tableName) {
        this.columns = []
        this.tableData = []
        return
      }
      try {
        const res = await getTableColumns(tableName)
        this.columns = res.data || []
        this.loadData()
      } catch (error) {
        this.$message.error('加载表结构失败')
      }
    },
    async loadData() {
      if (!this.searchForm.tableName) return
      this.loading = true
      try {
        const res = await queryData({
          ...this.searchForm,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        })
        this.tableData = res.data.records || []
        this.pagination.total = res.data.total || 0
      } catch (error) {
        this.$message.error('查询数据失败')
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.pagination.pageNum = 1
      this.loadData()
    },
    handleReset() {
      this.searchForm = { tableName: this.searchForm.tableName, condition: '' }
      this.handleSearch()
    },
    handlePagination({ page, rows }) {
      this.pagination.pageNum = page
      this.pagination.pageSize = rows
      this.loadData()
    }
  }
}
</script>
