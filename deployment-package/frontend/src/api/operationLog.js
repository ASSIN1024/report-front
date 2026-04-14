import request from '@/utils/request'

export function getOperationLogPage(params) {
  return request({
    url: '/operation/log/page',
    method: 'get',
    params
  })
}

export function getOperationLogById(id) {
  return request({
    url: `/operation/log/${id}`,
    method: 'get'
  })
}
