import axios from 'axios'
import { InkMessage } from '@/utils/message'
import { getToken, clearAuth } from '@/utils/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    // 后端 JwtInterceptor 直接解析 Authorization 头的 token，无需 Bearer 前缀
    config.headers.Authorization = token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res.code === 'number') {
      if (res.code === 200) {
        return res.data
      }
      if (res.code === 401) {
        handleUnauthorized()
      }
      const msg = res.message || '请求失败'
      InkMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
      return Promise.reject(error)
    }
    const msg =
      error.response?.data?.message || error.message || '网络异常，请稍后重试'
    InkMessage.error(msg)
    return Promise.reject(error)
  },
)

function handleUnauthorized() {
  clearAuth()
  const current = router.currentRoute.value
  const publicPages = ['Home', 'Login', 'Register', 'ArticleDetail', 'UserProfile']
  if (!publicPages.includes(current.name)) {
    router.push({
      name: 'Home',
    })
  }
}

export default request
