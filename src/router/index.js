import Vue from 'vue'
import VueRouter from 'vue-router'
import { getToken } from '@/utils/auth'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/ftp'
  },
  {
    path: '/data-center',
    name: 'DataCenter',
    component: () => import('@/views/data-center/Index.vue')
  },
  {
    path: '/ftp',
    name: 'FtpConfig',
    component: () => import('@/views/ftp/FtpConfig.vue')
  },
  {
    path: '/report',
    name: 'ReportList',
    component: () => import('@/views/report/ReportList.vue')
  },
  {
    path: '/report/config/:id?',
    name: 'ReportConfig',
    component: () => import('@/views/report/components/ReportConfig.vue'),
    props: true
  },
  {
    path: '/task',
    name: 'TaskMonitor',
    component: () => import('@/views/task/TaskMonitor.vue')
  },
  {
    path: '/trigger-monitor',
    name: 'TriggerMonitor',
    component: () => import('@/views/trigger/TriggerMonitor.vue')
  },
  {
    path: '/log',
    name: 'LogList',
    component: () => import('@/views/log/LogList.vue')
  },
  {
    path: '/operation-log',
    name: 'OperationLog',
    component: () => import('@/views/log/OperationLog.vue')
  },
  {
    path: '/system-log',
    name: 'SystemLog',
    component: () => import('@/views/log/SystemLog.vue')
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = getToken()

  // 如果路由需要认证
  if (to.meta.requiresAuth !== false && !token) {
    // 未登录，重定向到登录页
    next('/login')
  } else if (to.path === '/login' && token) {
    // 已登录，访问登录页时重定向到首页
    next('/')
  } else {
    next()
  }
})

export default router
