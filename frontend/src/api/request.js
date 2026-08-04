import axios from 'axios'
import { useUserStore } from '../stores/userStore'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

api.interceptors.request.use(config => {
  const userStore = useUserStore()
  const token = userStore.token
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
}, error => Promise.reject(error))

api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      if (router.currentRoute.value.path !== '/login') {
        router.replace('/login')
      }
    }
    return Promise.reject(error)
  }
)

export default api
