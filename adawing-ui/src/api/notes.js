import request from './request.js'

export function listNotes(type, page = 1, size = 10) {
  return request.get('/notes', { params: { type, page, size } })
}

export function getNote(id) {
  return request.get(`/notes/${id}`)
}

export function saveNote(data) {
  return request.post('/notes', data)
}
