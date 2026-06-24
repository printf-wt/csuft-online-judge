import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

const TOKEN_KEY = 'csuft_oj_token'
const USER_KEY = 'csuft_oj_user'

localStorage.removeItem(TOKEN_KEY)
localStorage.removeItem(USER_KEY)

export const useAuthStore = defineStore('auth', () => {
  const token = ref(sessionStorage.getItem(TOKEN_KEY) || '')
  const user = ref(loadUser())

  const isLoggedIn = computed(() => Boolean(token.value))
  const role = computed(() => user.value?.role || '')
  const isTeacherOrAdmin = computed(() => ['TEACHER', 'ADMIN'].includes(role.value))
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setAuth(payload) {
    token.value = payload?.token || ''
    user.value = payload?.user || null

    if (token.value) {
      sessionStorage.setItem(TOKEN_KEY, token.value)
    } else {
      sessionStorage.removeItem(TOKEN_KEY)
    }

    if (user.value) {
      sessionStorage.setItem(USER_KEY, JSON.stringify(user.value))
    } else {
      sessionStorage.removeItem(USER_KEY)
    }
  }

  function logout() {
    setAuth(null)
  }

  return {
    token,
    user,
    isLoggedIn,
    role,
    isTeacherOrAdmin,
    isAdmin,
    setAuth,
    logout,
  }
})

function loadUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null')
  } catch {
    sessionStorage.removeItem(USER_KEY)
    return null
  }
}
