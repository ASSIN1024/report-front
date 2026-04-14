import request from './request'

export function getOutputTables() {
  return request({
    url: '/data/tables',
    method: 'get'
  })
}

export function getTableColumns(tableName) {
  return request({
    url: '/data/columns',
    method: 'get',
    params: { tableName }
  })
}

export function queryData(data) {
  return request({
    url: '/data/query',
    method: 'post',
    data
  })
}

export function queryList(data) {
  return request({
    url: '/data/list',
    method: 'post',
    data
  })
}
