import request from './request.js'

export function listPublished(params) {
  return request.get('/articles/published', { params })
}

export function getPublished(id) {
  return request.get(`/articles/${id}`)
}

export function listArchive() {
  return request.get('/articles/archive')
}

export function listAdmin(params) {
  return request.get('/articles/admin', { params })
}

export function getAdmin(id) {
  return request.get(`/articles/admin/${id}`)
}

export function getStats() {
  return request.get('/articles/stats')
}

export function saveArticle(data) {
  return request.post('/articles', data)
}

export function publish(id) {
  return request.post(`/articles/${id}/publish`)
}

export function hide(id) {
  return request.post(`/articles/${id}/hide`)
}

export function submitForReview(id) {
  return request.post(`/articles/${id}/review`)
}

export function deleteArticle(id) {
  return request.delete(`/articles/${id}`)
}
