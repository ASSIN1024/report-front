import request from '@/utils/request'

export function getLogFileList() {
  return request({
    url: '/log/file/list',
    method: 'get'
  })
}

export function getLogFileInfo(logType) {
  return request({
    url: '/log/file/info',
    method: 'get',
    params: { logType }
  })
}

export function queryLogs(params) {
  return request({
    url: '/log/file/query',
    method: 'get',
    params
  })
}

export function getRecentLogs(logType, lines) {
  return request({
    url: '/log/file/recent',
    method: 'get',
    params: { logType, lines }
  })
}

export function exportLog(logType, level, keyword) {
  return request({
    url: '/log/file/export',
    method: 'get',
    params: { logType, level, keyword },
    responseType: 'blob'
  })
}
