import request from '@/utils/request'

export function listAlerts(params) {
  return request({
    url: '/alert',
    method: 'get',
    params
  })
}

export function getAlert(id) {
  return request({
    url: `/alert/${id}`,
    method: 'get'
  })
}

export function resolveAlert(id) {
  return request({
    url: `/alert/${id}/resolve`,
    method: 'put'
  })
}
