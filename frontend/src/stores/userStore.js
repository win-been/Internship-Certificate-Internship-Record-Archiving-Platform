import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// Extract a user-facing message from an axios/backend error
function extractError(error, fallback) {
  return error?.response?.data?.message || error?.message || fallback
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'))
  const isLoggingOut = ref(false)
  const isLoggedIn = computed(() => !!token.value)
  let logoutTimer = null

  const setSession = (data) => {
    isLoggingOut.value = false
    if (logoutTimer) {
      clearTimeout(logoutTimer)
      logoutTimer = null
    }
    token.value = data.token
    userInfo.value = {
      userId: data.userId,
      username: data.username,
      realName: data.realName,
      role: data.role,
      schoolId: data.schoolId,
      organizationName: data.organizationName,
      major: data.major,
      identityStatus: data.identityStatus
    }
    localStorage.setItem('token', token.value)
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  const updateUserInfo = (data) => {
    const has = (key) => Object.prototype.hasOwnProperty.call(data, key)
    const pick = (key, fallback) => has(key) ? data[key] : fallback
    userInfo.value = {
      ...userInfo.value,
      userId: has('userId') ? data.userId : (has('id') ? data.id : userInfo.value.userId),
      username: pick('username', userInfo.value.username),
      realName: pick('realName', userInfo.value.realName),
      role: pick('role', userInfo.value.role),
      schoolId: pick('schoolId', userInfo.value.schoolId),
      organizationName: pick('organizationName', userInfo.value.organizationName),
      major: pick('major', userInfo.value.major),
      email: pick('email', userInfo.value.email),
      phone: pick('phone', userInfo.value.phone),
      walletAddress: pick('walletAddress', userInfo.value.walletAddress),
      status: pick('status', userInfo.value.status),
      identityStatus: pick('identityStatus', userInfo.value.identityStatus)
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  const login = async (username, password) => {
    const api = (await import('../api/request')).default
    try {
      const response = await api.post('/auth/login', { username, password })
      const data = response.data
      setSession(data)
      return data
    } catch (error) {
      throw new Error(extractError(error, '登录失败，请检查用户名和密码'))
    }
  }

  const register = async (userData) => {
    const api = (await import('../api/request')).default
    try {
      const response = await api.post('/auth/register', userData)
      return response.data
    } catch (error) {
      throw new Error(extractError(error, '注册失败'))
    }
  }

  const logout = () => {
    isLoggingOut.value = true
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    if (logoutTimer) clearTimeout(logoutTimer)
    logoutTimer = setTimeout(() => {
      isLoggingOut.value = false
      logoutTimer = null
    }, 1500)
  }

  const getUserInfo = async (userId) => {
    try {
      const api = (await import('../api/request')).default
      const response = await api.get('/auth/user/' + userId)
      return response.data
    } catch (error) {
      throw error
    }
  }

  return { token, userInfo, isLoggedIn, isLoggingOut, login, register, logout, getUserInfo, updateUserInfo }
})
