import axios from 'axios'
import { ElMessage } from 'element-plus'

import { useAuthStore } from '@/stores/auth'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

const refreshClient = axios.create({
  baseURL: '/api',
  timeout: 15000,
  withCredentials: true,
})

let refreshPromise = null

function refreshAccessToken() {
  const authStore = useAuthStore()
  if (!refreshPromise) {
    refreshPromise = refreshClient.post('/auth/refresh')
      .then((response) => {
        authStore.setAuth(response.data.data)
        return response.data.data.token
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

export async function restoreSession() {
  const authStore = useAuthStore()
  if (authStore.token) {
    return true
  }
  try {
    await refreshAccessToken()
    return true
  } catch {
    authStore.logout()
    return false
  }
}

http.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  config.headers['X-Request-ID'] ||= createRequestId()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const authStore = useAuthStore()
    const status = error.response?.status
    const originalRequest = error.config
    const isAuthRequest = originalRequest?.url?.includes('/auth/')
    error.requestId = error.response?.headers?.['x-request-id']
      || originalRequest?.headers?.['X-Request-ID']

    if (status === 401 && originalRequest && !originalRequest._retry && !isAuthRequest) {
      originalRequest._retry = true
      try {
        const token = await refreshAccessToken()
        originalRequest.headers.Authorization = `Bearer ${token}`
        return http.request(originalRequest)
      } catch (refreshError) {
        authStore.logout()
        const redirect = encodeURIComponent(window.location.pathname + window.location.search)
        window.location.href = `/login?redirect=${redirect}`
        return Promise.reject(refreshError)
      }
    }

    if (status === 401 && !isAuthRequest) {
      authStore.logout()
      window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error('没有权限执行此操作')
    } else if (status >= 500) {
      ElMessage.error('服务器错误，请稍后再试')
    } else if (!error.response) {
      ElMessage.error('网络连接失败，请检查网络')
    }
    return Promise.reject(error)
  },
)

export default http

function createRequestId() {
  return globalThis.crypto?.randomUUID?.()
    || `web-${Date.now()}-${Math.random().toString(16).slice(2)}`
}
