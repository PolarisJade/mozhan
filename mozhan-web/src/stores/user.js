import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/user'
import {
  getToken,
  setToken,
  removeToken,
  getStoredUser,
  setStoredUser,
  clearAuth,
  isTokenExpired,
} from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const storedToken = getToken()
  const token = ref(isTokenExpired() ? null : storedToken)
  const user = ref(getStoredUser())

  const isLoggedIn = computed(() => !!token.value)

  function setAuth(loginData) {
    token.value = loginData.token
    user.value = loginData.user
    setToken(loginData.token)
    setStoredUser(loginData.user)
  }

  async function login(form) {
    const data = await loginApi(form)
    setAuth({
      token: data.token,
      user: {
        id: data.userId,
        nickname: data.nickname,
        avatar: data.avatar,
        username: data.username,
      },
    })
    return data
  }

  function logout() {
    token.value = null
    user.value = null
    clearAuth()
  }

  function updateLocalUser(partial) {
    if (!user.value) return
    user.value = { ...user.value, ...partial }
    setStoredUser(user.value)
  }

  async function refreshUserInfo() {
    const userId = user.value?.id
    if (!userId) return null
    const data = await getUserInfo(userId)
    user.value = data
    setStoredUser(data)
    return data
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    logout,
    setAuth,
    updateLocalUser,
    refreshUserInfo,
  }
})
