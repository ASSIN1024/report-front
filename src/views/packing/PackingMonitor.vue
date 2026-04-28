<template>
  <div class="packing-monitor">
    <el-card>
      <div slot="header">
        <span>批次监控</span>
        <el-button style="float: right;" type="primary" @click="triggerPacking">手动触发打包</el-button>
      </div>
      <el-table :data="batches" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileCount" label="文件数" width="80" />
        <el-table-column prop="totalSize" label="大小 (MB)" width="120">
          <template slot-scope="scope">
            {{ (scope.row.totalSize / 1024 / 1024).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'PackingMonitor',
  data() {
    return {
      batches: [],
      loading: false
    };
  },
  mounted() {
    this.loadBatches();
  },
  methods: {
    loadBatches() {
      this.loading = true;
      request.get('/packing/batch').then(res => {
        this.batches = res;
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    triggerPacking() {
      request.post('/packing/trigger').then(() => {
        this.$message.success('打包已触发');
        setTimeout(() => this.loadBatches(), 1000);
      }).catch(() => {
        this.$message.error('触发失败');
      });
    },
    getStatusType(status) {
      const map = {
        'PENDING': 'info',
        'UPLOADING': 'warning',
        'CONSUMING': 'warning',
        'DONE': 'success',
        'FAILED': 'danger'
      };
      return map[status] || 'info';
    }
  }
};
</script>

<style scoped>
.packing-monitor {
  padding: 20px;
}
</style>
