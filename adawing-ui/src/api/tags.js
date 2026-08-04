import request from './request.js'

// ── 词表操作（zoom-tag: /api/v2/tags）──
export function createTag(data) {
  return request.post('/tags', data)
}

export function suggestTags(name) {
  return request.get('/tags/suggest', { params: { name } })
}

// 访客：全站标签列表（公开端点，不含文章计数）
export function listAllTags() {
  return request.get('/tags')
}

// ── 文章-标签关系操作（zoom-article: /api/v2/article-tags）──
export function listTagsWithCount() {
  return request.get('/article-tags')
}

export function listArticlesByTag(tag, params) {
  return request.get('/article-tags/by-tag', { params: { tag, ...params } })
}

export function mergeTags(sourceId, targetId) {
  return request.post(`/article-tags/${sourceId}/merge/${targetId}`)
}

export function deleteTag(id) {
  return request.delete(`/article-tags/${id}`)
}
