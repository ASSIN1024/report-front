<template>
  <div id="app">
    <el-container>
      <el-aside width="200px">
        <el-menu
          :default-active="$route.path"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/data-center">
            <i class="el-icon-collection"></i>
            <span>数据中心</span>
          </el-menu-item>
          <el-menu-item index="/ftp">
            <i class="el-icon-connection"></i>
            <span>FTP配置</span>
          </el-menu-item>
          <el-menu-item index="/report">
            <i class="el-icon-document"></i>
            <span>报表配置</span>
          </el-menu-item>
          <el-menu-item index="/task">
            <i class="el-icon-monitor"></i>
            <span>任务监控</span>
          </el-menu-item>
          <el-menu-item index="/trigger-monitor">
            <i class="el-icon-video-play"></i>
            <span>Trigger监控</span>
          </el-menu-item>
          <el-menu-item index="/log">
            <i class="el-icon-tickets"></i>
            <span>执行日志</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style lang="scss">
#app {
  height: 100vh;
  
  .el-container {
    height: 100%;
  }
  
  .el-aside {
    background-color: #304156;
    
    .el-menu {
      border-right: none;
    }
  }
  
  .el-main {
    padding: 20px;
    background-color: #f0f2f5;
  }
}
</style>
