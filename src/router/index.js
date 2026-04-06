import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
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

export default router
