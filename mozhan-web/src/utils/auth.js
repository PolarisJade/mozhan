import { InkMessage } from '@/utils/message'

const TOKEN_KEY = 'mozhan_token'
const USER_KEY = 'mozhan_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export function getStoredUser() {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export function setStoredUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeStoredUser() {
  localStorage.removeItem(USER_KEY)
}

export function clearAuth() {
  removeToken()
  removeStoredUser()
}

function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1]
    if (!base64Url) return null
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''),
    )
    return JSON.parse(json)
  } catch {
    return null
  }
}

export function isTokenExpired() {
  const token = getToken()
  if (!token) return true
  const payload = parseJwt(token)
  if (!payload) return true
  const now = Date.now()
  const exp = payload.exp * 1000
  return now >= exp
}

let router = null

export function setRouterInstance(routerInstance) {
  router = routerInstance
}

export function redirectToLogin(message = '请先登录') {
  if (!router) return
  clearAuth()
  const current = router.currentRoute.value
  if (current.name !== 'Login') {
    InkMessage.warning(message)
    router.push({
      name: 'Login',
      query: { redirect: current.fullPath },
    })
  }
}
