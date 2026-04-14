import request from '@/utils/request'

export function getTableList(params) {
  return request({
    url: '/data-center/tables',
    method: 'get',
    params
  })
}

export function getTableDetail(tableName) {
  return request({
    url: `/data-center/tables/${tableName}`,
    method: 'get'
  })
}

export function updateTable(data) {
  return request({
    url: '/data-center/tables',
    method: 'put',
    data
  })
}

export function getUntaggedTables() {
  return request({
    url: '/data-center/untagged',
    method: 'get'
  })
}

export function scanTables() {
  return request({
    url: '/data-center/scan',
    method: 'post'
  })
}

export function getTableColumns(tableName) {
  return request({
    url: `/data-center/tables/${tableName}/columns`,
    method: 'get'
  })
}

export function getTableData(tableName, params) {
  return request({
    url: `/data-center/tables/${tableName}/data`,
    method: 'get',
    params
  })
}