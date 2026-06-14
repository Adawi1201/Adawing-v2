import request from './request.js'

export function listMcpKeys() {
  return request.get('/mcp-keys')
}

export function generateMcpKey(data) {
  return request.post('/mcp-keys', data)
}

export function revokeMcpKey(id) {
  return request.delete(`/mcp-keys/${id}`)
}
