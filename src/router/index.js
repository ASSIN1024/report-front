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
    path: '/alert',
    name: 'AlertList',
    component: () => import('@/views/alert/AlertList.vue')
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
  },
  {
    path: '/packing/config',
    name: 'PackingConfig',
    component: () => import('@/views/packing/PackingConfig.vue')
  },
  {
    path: '/packing/monitor',
    name: 'PackingMonitor',
    component: () => import('@/views/packing/PackingMonitor.vue')
  },
  {
    path: '/packing/alerts',
    name: 'PackingAlertList',
    component: () => import('@/views/packing/AlertList.vue')
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes
})

router.beforeEach((to, from, next) => {
  const token = getToken()

  if (to.meta.requiresAuth !== false && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
