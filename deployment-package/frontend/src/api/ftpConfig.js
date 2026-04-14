import request from '@/utils/request'

export function getFtpConfigPage(params) {
  return request({
    url: '/ftp/config/page',
    method: 'get',
    params
  })
}

export function getFtpConfigListEnabled() {
  return request({
    url: '/ftp/config/list/enabled',
    method: 'get'
  })
}

export function getFtpConfigById(id) {
  return request({
    url: `/ftp/config/${id}`,
    method: 'get'
  })
}

export function saveFtpConfig(data) {
  return request({
    url: '/ftp/config',
    method: 'post',
    data
  })
}

export function updateFtpConfig(data) {
  return request({
    url: '/ftp/config',
    method: 'put',
    data
  })
}

export function deleteFtpConfig(id) {
  return request({
    url: `/ftp/config/${id}`,
    method: 'delete'
  })
}

export function testFtpConnection(id) {
  return request({
    url: `/ftp/config/test/${id}`,
    method: 'post'
  })
}
