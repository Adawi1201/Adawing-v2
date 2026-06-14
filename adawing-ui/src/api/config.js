import request from './request.js'

export function getSiteConfig() {
  return request.get('/config/site')
}

export function saveSiteConfig(config) {
  return request.put('/config/site', config)
}
