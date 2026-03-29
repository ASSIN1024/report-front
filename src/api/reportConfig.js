import request from '@/utils/request'

export function getReportConfigPage(params) {
  return request({
    url: '/report/config/page',
    method: 'get',
    params
  })
}

export function getReportConfigListEnabled() {
  return request({
    url: '/report/config/list/enabled',
    method: 'get'
  })
}

export function getReportConfigById(id) {
  return request({
    url: `/report/config/${id}`,
    method: 'get'
  })
}

export function saveReportConfig(data) {
  return request({
    url: '/report/config',
    method: 'post',
    data
  })
}

export function updateReportConfig(data) {
  return request({
    url: '/report/config',
    method: 'put',
    data
  })
}

export function deleteReportConfig(id) {
  return request({
    url: `/report/config/${id}`,
    method: 'delete'
  })
}

export function uploadReportFile(data) {
  return request({
    url: '/report/config/upload',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
