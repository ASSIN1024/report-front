import request from '@/utils/request'

export function getTaskPage(params) {
  return request({
    url: '/task/page',
    method: 'get',
    params
  })
}

export function getTaskById(id) {
  return request({
    url: `/task/${id}`,
    method: 'get'
  })
}

export function retryTask(id) {
  return request({
    url: `/task/retry/${id}`,
    method: 'post'
  })
}

export function cancelTask(id) {
  return request({
    url: `/task/cancel/${id}`,
    method: 'post'
  })
}

export function deleteTask(id) {
  return request({
    url: `/task/${id}`,
    method: 'delete'
  })
}
