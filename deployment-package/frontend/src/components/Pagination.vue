<template>
  <div class="pagination-container">
    <el-pagination
      background
      :current-page="currentPage"
      :page-sizes="[10, 20, 50, 100]"
      :page-size="pageSize"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />
  </div>
</template>

<script>
export default {
  name: 'Pagination',
  props: {
    currentPage: {
      type: Number,
      default: 1
    },
    pageSize: {
      type: Number,
      default: 10
    },
    total: {
      type: Number,
      default: 0
    }
  },
  methods: {
    handleSizeChange(val) {
      this.$emit('pagination', { page: 1, rows: val })
    },
    handleCurrentChange(val) {
      this.$emit('pagination', { page: val, rows: this.pageSize })
    }
  }
}
</script>

<style scoped>
.pagination-container {
  padding: 20px 0;
  text-align: right;
}
.pagination-container >>> .el-pagination {
  display: inline-block;
}
.pagination-container >>> .el-pagination button {
  cursor: pointer !important;
}
.pagination-container >>> .el-pagination .el-pager li {
  cursor: pointer !important;
}
</style>
