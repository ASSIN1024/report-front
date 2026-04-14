import request from '@/utils/request'

export function getLogPage(params) {
  return request({
    url: '/log/page',
    method: 'get',
    params
  })
}

export function getLogList(taskExecutionId) {
  return request({
    url: `/log/list/${taskExecutionId}`,
    method: 'get'
  })
}
