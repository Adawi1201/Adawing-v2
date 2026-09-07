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

// 后端错误契约：HTTP 状态码为真实语义，body 为 Result JSON（code 与状态码一致）。
// 错误提示优先级：Result.msg > 状态映射 > 传输层兜底。
const STATUS_MESSAGES = {
  400: '请求参数错误',
  403: '无权访问',
  404: '资源不存在',
  405: '请求方法不支持',
  413: '文件大小超出限制'
}

async function resolveMessage(error) {
  const status = error.response.status
  const data = error.response.data
  // AuthImage 等 blob 请求的错误响应也是 Blob，需先解码
  if (data instanceof Blob) {
    try {
      const json = JSON.parse(await data.text())
      if (json?.msg) return json.msg
    } catch { /* 非 JSON 错误体，走状态映射 */ }
  } else if (data?.msg) {
    return data.msg
  }
  return STATUS_MESSAGES[status] || (status >= 500 ? '服务异常，请稍后重试' : '请求失败')
}

function toError(message, status) {
  const err = new Error(message)
  err.status = status
  return err
}

request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 契约外兜底：2xx 但业务码非 200（正常不会发生）
    if (data && !(data instanceof Blob) && data.code !== undefined && data.code !== 200) {
      const message = data.msg || '请求失败'
      toast(message, 'error')
      return Promise.reject(toError(message, data.code))
    }
    return data
  },
  async (error) => {
    const status = error.response?.status
    const isLoginRequest = error.config?.url?.includes('/auth/login')
    if (status === 401 && !isLoginRequest) {
      handleUnauthorized()
      return Promise.reject(toError('登录已过期，请重新登录', 401))
    }

    let message
    if (!error.response) {
      message = error.code === 'ECONNABORTED' ? '请求超时，请重试' : '网络异常，请稍后重试'
    } else {
      message = await resolveMessage(error)
    }

    // silent 请求由调用方内联展示错误，拦截器不再弹 toast
    if (!error.config?.silent) {
      toast(message, 'error')
    }
    return Promise.reject(toError(message, status))
  }
)

export default request
