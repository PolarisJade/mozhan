const TOKEN_KEY = 'admin_token'
const EXPIRE_KEY = 'admin_token_expire'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token, expireTime) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(EXPIRE_KEY, expireTime)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(EXPIRE_KEY)
}

export function isTokenExpired() {
  const expireTime = localStorage.getItem(EXPIRE_KEY)
  if (!expireTime) return false
  return Date.now() > parseInt(expireTime)
}
