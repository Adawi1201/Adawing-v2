import axios from 'axios'
import { toast } from '@/utils/toast.js'

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
  if (config.data instanceof FormData) {
    delete config.headers['Content-Type']
  }
  return config
})

// 并发请求同时 401 时只处理一次跳转
let handling401 = false

async function handleUnauthorized() {
  if (handling401) return
  // 动态导入，避免 request.js ↔ stores/auth.js 循环依赖
  const { default: router } = await import('@/router/index.js')
  const current = router.currentRoute.value
  // 仅管理端区域的 401 才注销并跳登录页；访客端（如匿名访问受限资源）不做任何跳转
  if (!current.path.startsWith('/yusal/admin') || current.name === 'AdminLogin') {
    return
  }
  handling401 = true
  toast('登录已过期，请重新登录', 'warn')
  try {
    const { useAuthStore } = await import('@/stores/auth.js')
    useAuthStore().logout()
    await router.push({ name: 'AdminLogin', query: { redirect: current.fullPath } })
  } finally {
    handling401 = false
  }
}

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && data.code !== undefined && data.code !== 200) {
      const err = new Error(data.msg || 'Request failed')
      toast(data.msg || 'Request failed', 'error')
      return Promise.reject(err)
    }
    return data
  },
  (error) => {
    const status = error.response?.status
    const isLoginRequest = error.config?.url?.includes('/auth/login')
    if (status === 401 && !isLoginRequest) {
      handleUnauthorized()
      return Promise.reject(new Error('登录已过期，请重新登录'))
    }
    const message = error.response?.data?.msg || error.message || 'Network error'
    toast(message, 'error')
    return Promise.reject(new Error(message))
  }
)

export default request
