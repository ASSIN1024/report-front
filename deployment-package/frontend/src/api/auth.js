import request from '@/utils/request'

export function login(username, password) {
  return request({
    url: '/auth/login',
    method: 'post',
    params: { username, password }
  })
}

export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post'
  })
}

export function getCurrentUser() {
  return request({
    url: '/auth/current-user',
    method: 'get'
  })
}

export function getCsrfToken() {
  return request({
    url: '/auth/csrf-token',
    method: 'get'
  })
}
