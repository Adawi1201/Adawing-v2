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

request.interceptors.response.use(
  (response) => {
    const data = response.data
    if (data && data.code !== undefined && data.code !== 200) {
      const err = new Error(data.message || 'Request failed')
      toast(data.message || 'Request failed', 'error')
      return Promise.reject(err)
    }
    return data
  },
  (error) => {
    const message = error.response?.data?.message || error.message || 'Network error'
    toast(message, 'error')
    return Promise.reject(new Error(message))
  }
)

export default request
