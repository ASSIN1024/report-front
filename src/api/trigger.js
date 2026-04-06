import request from '@/utils/request'

export function getTriggerList() {
  return request({
    url: '/trigger',
    method: 'get'
  })
}

export function getTrigger(code) {
  return request({
    url: `/trigger/${code}`,
    method: 'get'
  })
}

export function getTriggerStateList() {
  return request({
    url: '/trigger/state',
    method: 'get'
  })
}

export function getTriggerState(code) {
  return request({
    url: `/trigger/state/${code}`,
    method: 'get'
  })
}

export function getTriggerHistory(code, params) {
  return request({
    url: `/trigger/history/${code}`,
    method: 'get',
    params
  })
}

export function testTrigger(code) {
  return request({
    url: `/trigger/${code}/test`,
    method: 'post'
  })
}