import request from './request.js'

export function listTags() {
  return request.get('/tags')
}

export function createTag(data) {
  return request.post('/tags', data)
}

export function suggestTags(name) {
  return request.get('/tags/suggest', { params: { name } })
}

export function mergeTags(sourceId, targetId) {
  return request.post(`/tags/${sourceId}/merge/${targetId}`)
}
