const RESOURCE_CONTENT_PATTERN = /\/api\/v2\/resource\/(\d+)\/content/g

export function resourceReference(id) {
  return `resource://${id}`
}

export function resourceReferenceImage(id, alt = '') {
  return `![${alt}](${resourceReference(id)})`
}

export function restoreResourceReferences(content) {
  if (!content) return content
  return content.replace(RESOURCE_CONTENT_PATTERN, (_, id) => resourceReference(id))
}
