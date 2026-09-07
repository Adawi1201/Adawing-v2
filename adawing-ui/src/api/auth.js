import request from './request.js'

export function login(data) {
  return request.post('/auth/login', data, { silent: true })
}

export function changePassword(data) {
  return request.post('/auth/change-password', data, { silent: true })
}
