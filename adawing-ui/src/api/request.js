import axios from 'axios'

const request = axios.create({
  baseURL: '/api/v2',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('adawing_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // 上传文件时删除默认 JSON Content-Type，让 axios 自动设置 multipart/form-data
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && data.code !== undefined && data.code !== 200) {
      const err = new Error(data.message || 'Request failed')
      showToast(data.message || 'Request failed', 'error')
      return Promise.reject(err)
    }
    return data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || 'Network error'
    showToast(message, 'error')
    return Promise.reject(new Error(message))
  }
)

// ── Toast notification ──
function showToast(text, type = 'info') {
  if (typeof document === 'undefined') return
  const existing = document.querySelector('.api-toast')
  if (existing) existing.remove()

  const el = document.createElement('div')
  el.className = `api-toast api-toast-${type}`
  el.textContent = text
  document.body.appendChild(el)

  requestAnimationFrame(() => el.classList.add('api-toast-in'))
  setTimeout(() => {
    el.classList.remove('api-toast-in')
    setTimeout(() => el.remove(), 300)
  }, 3500)
}

// Inject toast styles once
if (typeof document !== 'undefined' && !document.querySelector('#api-toast-style')) {
  const style = document.createElement('style')
  style.id = 'api-toast-style'
  style.textContent = `
.api-toast {
  position: fixed; bottom: 24px; right: 24px; z-index: 9999;
  max-width: 380px; padding: 10px 18px;
  font-size: 12px; font-family: inherit;
  border: 1px solid var(--line, rgba(0,0,0,.22));
  background: var(--bg, #FEFEFE); color: var(--ink, #2D2D2D);
  letter-spacing: 0.04em; border-radius: 4px;
  box-shadow: 0 4px 18px rgba(0,0,0,.12);
  opacity: 0; transform: translateY(12px);
  transition: opacity 0.25s, transform 0.25s;
  pointer-events: none;
}
.api-toast-in { opacity: 1; transform: translateY(0); }
.api-toast-error { border-color: #c45c5c; color: #c45c5c; background: rgba(196,92,92,.06); }
`
  document.head.appendChild(style)
}

export default request
