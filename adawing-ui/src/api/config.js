import request from './request.js'

export function getSiteConfig() {
  return request.get('/config/site')
}

export function saveSiteConfig(config) {
  return request.put('/config/site', config, { silent: true })
}

export function getWritingGuide() {
  return request.get('/config/writing-guide')
}

export function saveWritingGuide(content) {
  return request.put('/config/writing-guide', { content }, { silent: true })
}
