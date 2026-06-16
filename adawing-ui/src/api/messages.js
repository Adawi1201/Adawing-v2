import request from './request.js'

export function listMessages(params) {
  return request.get('/messages', { params })
}

export function submitMessage(data) {
  return request.post('/messages', data)
}

export function listAdminMessages(params) {
  return request.get('/messages/admin', { params })
}

export function approveMessage(id, reply, avatarResourceId) {
  return request.post(`/messages/${id}/approve`, reply || '', {
    params: avatarResourceId != null ? { avatarResourceId } : {}
  })
}

export function rejectMessage(id, reason) {
  return request.post(`/messages/${id}/reject`, reason || '')
}

export function deleteMessage(id) {
  return request.delete(`/messages/${id}`)
}
