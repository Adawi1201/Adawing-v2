import request from './request.js'

export function listResources({ pool, allowFallback, page, size } = {}) {
  return request.get('/admin/resources', {
    params: { pool, allowFallback, page, size }
  })
}

export function uploadResource(file, pool = 'MISC') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('pool', pool)
  return request.post('/admin/resources/upload', formData)
}

export function allocateResource(id, targetPool) {
  return request.post(`/admin/resources/${id}/allocate`, null, {
    params: { targetPool }
  })
}

export function deleteResource(id) {
  return request.delete(`/admin/resources/${id}`)
}
