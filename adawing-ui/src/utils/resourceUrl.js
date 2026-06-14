/**
 * 将资源 ID 转换为资源中心的访问 URL。
 * 对应后端的 resource://<id> → /api/v2/resource/<id>/content 转换。
 * 所有静态资源统一通过资源中心引用，不再支持裸 URL。
 */
export function resourceContentUrl(id) {
  if (id == null || id === '') return ''
  return `/api/v2/resource/${id}/content`
}
