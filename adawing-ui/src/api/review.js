import request from './request.js'

export function listTasks(params) {
  return request.get('/review/tasks', { params })
}

export function getStats() {
  return request.get('/review/stats')
}

export function submitReview(data) {
  return request.post('/review/submit', data)
}

export function approve(id, data) {
  return request.post(`/review/${id}/approve`, data || {})
}

export function reject(id, data) {
  return request.post(`/review/${id}/reject`, data || {})
}
