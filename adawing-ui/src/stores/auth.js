import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi, changePassword as changePasswordApi } from '@/api/auth.js'

const TOKEN_KEY = 'adawing_token'
const USERNAME_KEY = 'adawing_username'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const username = ref(localStorage.getItem(USERNAME_KEY) || '')
  const isLoggedIn = computed(() => !!token.value)

  async function login(usernameInput, password) {
    const res = await loginApi({ username: usernameInput, password })
    const value = res.data || res
    token.value = value
    username.value = usernameInput
    localStorage.setItem(TOKEN_KEY, value)
    localStorage.setItem(USERNAME_KEY, usernameInput)
    return value
  }

  async function changePassword(oldPassword, newPassword) {
    const user = username.value
    if (!user) throw new Error('User not found. Please sign in again.')
    await changePasswordApi({ username: user, oldPassword, newPassword })
  }

  function logout() {
    token.value = ''
    username.value = ''
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USERNAME_KEY)
  }

  return { token, username, isLoggedIn, login, logout, changePassword }
})
