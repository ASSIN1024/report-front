<template>
  <div class="alert-list">
    <el-card>
      <div slot="header">
        <span>告警列表</span>
      </div>
      <el-form :inline="true" :model="filters">
        <el-form-item label="告警类型">
          <el-select v-model="filters.alertType" placeholder="请选择" clearable>
            <el-option label="解析错误" value="PARSE_ERROR" />
            <el-option label="映射错误" value="MAPPING_ERROR" />
            <el-option label="打包错误" value="PACKING_ERROR" />
            <el-option label="消费超时" value="CONSUMPTION_TIMEOUT" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" placeholder="请选择" clearable>
            <el-option label="待处理" value="PENDING" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已忽略" value="IGNORED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAlerts">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="alerts" v-loading="loading">
        <el-table-column prop="alertType" label="告警类型" width="150">
          <template slot-scope="scope">
            {{ getTypeName(scope.row.alertType) }}
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" width="200" />
        <el-table-column prop="reason" label="告警原因" />
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === 'PENDING' ? 'danger' : 'success'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="150">
          <template slot-scope="scope">
            <el-button v-if="scope.row.status === 'PENDING'" size="mini" @click="resolveAlert(scope.row)">标记已解决</el-button>
            <el-button v-if="scope.row.status === 'PENDING'" size="mini" type="text" @click="ignoreAlert(scope.row)">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'AlertList',
  data() {
    return {
      alerts: [],
      loading: false,
      filters: {
        alertType: '',
        status: ''
      }
    };
  },
  mounted() {
    this.loadAlerts();
  },
  methods: {
    loadAlerts() {
      this.loading = true;
      const params = {};
      if (this.filters.status) params.status = this.filters.status;
      request.get('/packing/alerts', { params }).then(res => {
        this.alerts = res;
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },
    resolveAlert(alert) {
      request.put(`/packing/alerts/${alert.id}/resolve`).then(() => {
        this.$message.success('已标记为已解决');
        this.loadAlerts();
      });
    },
    ignoreAlert(alert) {
      request.put(`/packing/alerts/${alert.id}/ignore`).then(() => {
        this.$message.success('已忽略');
        this.loadAlerts();
      });
    },
    getTypeName(type) {
      const map = {
        'PARSE_ERROR': '解析错误',
        'MAPPING_ERROR': '映射错误',
        'PACKING_ERROR': '打包错误',
        'CONSUMPTION_TIMEOUT': '消费超时'
      };
      return map[type] || type;
    }
  }
};
</script>

<style scoped>
.alert-list {
  padding: 20px;
}
</style>
