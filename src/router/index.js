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
    path: '/log',
    name: 'LogList',
    component: () => import('@/views/log/LogList.vue')
  }
]

const router = new VueRouter({
  mode: 'hash',
  routes
})

export default router
