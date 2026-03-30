<template>
  <el-tag :type="tagType" size="small">{{ text }}</el-tag>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    status: {
      type: String,
      required: true
    }
  },
  computed: {
    tagType() {
      const typeMap = {
        'PENDING': 'info',
        'RUNNING': 'primary',
        'SUCCESS': 'success',
        'FAILED': 'danger',
        'CANCELLED': 'warning',
        '0': 'danger',
        '1': 'success'
      }
      return typeMap[this.status] || 'info'
    },
    text() {
      const textMap = {
        'PENDING': '待执行',
        'RUNNING': '执行中',
        'SUCCESS': '成功',
        'FAILED': '失败',
        'CANCELLED': '已取消',
        '0': '禁用',
        '1': '启用'
      }
      return textMap[this.status] || this.status
    }
  }
}
</script>
