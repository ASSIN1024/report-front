<template>
  <div id="app">
    <el-container v-if="isLoggedIn">
      <el-aside :width="isCollapsed ? '64px' : '200px'" class="sidebar">
        <el-menu
          :default-active="$route.path"
          router
          :collapse="isCollapsed"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          class="sidebar-menu"
        >
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
            <span>处理记录</span>
          </el-menu-item>
          <el-menu-item index="/alert">
            <i class="el-icon-warning"></i>
            <span>告警管理</span>
          </el-menu-item>
          <el-menu-item index="/log">
            <i class="el-icon-tickets"></i>
            <span>执行日志</span>
          </el-menu-item>
        </el-menu>
        <div class="sidebar-toggle" @click="toggleSidebar">
          <i :class="isCollapsed ? 'el-icon-d-arrow-right' : 'el-icon-d-arrow-left'"></i>
        </div>
      </el-aside>
      <el-main class="main-panel">
        <router-view />
      </el-main>
    </el-container>
    <router-view v-else />
  </div>
</template>

<script>
import { getToken } from '@/utils/auth'

export default {
  name: 'App',
  data() {
    return {
      loggedIn: !!getToken(),
      isCollapsed: false
    }
  },
  computed: {
    isLoggedIn() {
      return this.loggedIn
    }
  },
  watch: {
    $route: {
      immediate: true,
      handler() {
        this.loggedIn = !!getToken()
      }
    }
  },
  created() {
    window.addEventListener('storage', this.checkLogin)
    this.$root.$on('login-success', this.checkLogin)
  },
  beforeDestroy() {
    window.removeEventListener('storage', this.checkLogin)
    this.$root.$off('login-success', this.checkLogin)
  },
  methods: {
    checkLogin() {
      this.loggedIn = !!getToken()
    },
    toggleSidebar() {
      this.isCollapsed = !this.isCollapsed
    }
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

#app {
  height: 100vh;

  .el-container {
    height: 100%;
  }

  .sidebar {
    background-color: $aside-bg-color;
    position: relative;
    transition: width $transition-duration ease;
    overflow: hidden;

    .sidebar-menu {
      border-right: none;

      &:not(.el-menu--collapse) {
        width: 200px;
      }
    }

    .sidebar-toggle {
      position: absolute;
      bottom: 20px;
      left: 50%;
      transform: translateX(-50%);
      color: $aside-text-color;
      cursor: pointer;
      padding: 8px;
      border-radius: $border-radius-base;
      transition: all 0.3s ease;

      &:hover {
        background-color: rgba(255, 255, 255, 0.1);
        color: #fff;
      }
    }
  }

  .main-panel {
    padding: $spacing-lg;
    background-color: $bg-color;
    overflow-y: auto;
  }
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    width: 0 !important;
    overflow: hidden;
  }

  .main-panel {
    margin-left: 0;
  }
}
</style>