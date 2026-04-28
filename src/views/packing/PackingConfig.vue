<template>
  <div class="packing-config">
    <el-card>
      <div slot="header">
        <span>打包配置</span>
      </div>
      <el-form :model="config" label-width="140px">
        <el-form-item label="最大包大小 (bytes)">
          <el-input-number v-model="config.max_package_size" :min="1024000" :step="1024000" />
          <span class="tip">默认 200MB (209715200)</span>
        </el-form-item>
        <el-form-item label="上传目录">
          <el-input v-model="config.upload_dir" placeholder="/data/ftp-root/for-upload" />
        </el-form-item>
        <el-form-item label="完成目录">
          <el-input v-model="config.done_dir" placeholder="/data/ftp-root/done" />
        </el-form-item>
        <el-form-item label="固定文件名">
          <el-input v-model="config.fixed_filename" placeholder="outputs.zip" />
        </el-form-item>
        <el-form-item label="轮询间隔 (秒)">
          <el-input-number v-model="config.polling_interval" :min="5" :step="5" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="saveConfig">保存配置</el-button>
          <el-button @click="loadConfig">刷新</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import request from '@/utils/request';

export default {
  name: 'PackingConfig',
  data() {
    return {
      config: {
        max_package_size: 209715200,
        upload_dir: '',
        done_dir: '',
        fixed_filename: 'outputs.zip',
        polling_interval: 30
      }
    };
  },
  mounted() {
    this.loadConfig();
  },
  methods: {
    loadConfig() {
      request.get('/packing/config').then(res => {
        this.config = { ...this.config, ...res };
      });
    },
    saveConfig() {
      const promises = Object.entries(this.config).map(([key, value]) => {
        return request.put('/packing/config', null, { params: { key, value } });
      });
      Promise.all(promises).then(() => {
        this.$message.success('配置保存成功');
      }).catch(() => {
        this.$message.error('配置保存失败');
      });
    }
  }
};
</script>

<style scoped>
.packing-config {
  padding: 20px;
}
.tip {
  margin-left: 10px;
  color: #909399;
}
</style>
