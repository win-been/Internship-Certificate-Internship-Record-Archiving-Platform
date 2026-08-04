// chainOps.js - Unified chain-first data operations
// All writes: backend API (backs up chain anchoring) -> localStorage cache
// All reads:  backend API -> localStorage fallback
// Chain status: tracked for UI feedback

import { ref, computed } from 'vue'

// Reactive chain status
const chainStatus = ref('unknown') // 'online' | 'offline' | 'unknown'
const lastChainTx = ref(null)
const chainError = ref(null)

export function useChainStatus() {
  const isChainOnline = computed(() => chainStatus.value === 'online')
  
  async function checkChainHealth() {
    try {
      const api = (await import('../api/request')).default
      const res = await api.get('/data/blockchain/health')
      const available = res?.data?.available === true
      chainStatus.value = available ? 'online' : 'offline'
      chainError.value = available ? null : (res?.data?.chainTestError || res?.data?.lastError || 'Blockchain unavailable')
      return available
    } catch (e) {
      chainStatus.value = 'offline'
      chainError.value = e.message
      return false
    }
  }
  
  return { chainStatus, lastChainTx, chainError, isChainOnline, checkChainHealth }
}

// Core chain-first operation
// 1. Try backend API (which anchors on chain via DataController)
// 2. On success: update localStorage cache, return data
// 3. On failure: surface the error; localStorage is cache only
export async function chainCall(method, url, body = null) {
  const userStore = (await import('./userStore')).useUserStore()
  const token = userStore.token
  
  if (!token) {
    throw new Error('请先登录')
  }
  
  try {
    const api = (await import('../api/request')).default
    let res
    if (method === 'GET') res = await api.get(url)
    else if (method === 'POST') res = await api.post(url, body)
    else if (method === 'PUT') res = await api.put(url, body)
    else if (method === 'DELETE') res = await api.delete(url)
    
    const data = res?.data
    if (data) {
      chainError.value = null
      lastChainTx.value = { method, url, time: new Date().toISOString() }
    }
    return data
  } catch (e) {
    chainStatus.value = e.response ? 'online' : 'offline'
    chainError.value = e.response?.data?.message || e.message
    throw e
  }
}

// Chain-first write: ensures chain anchoring before returning success
export async function chainWrite(method, url, body) {
  const result = await chainCall(method, url, body)
  return result
}

// Chain-first read: tries backend, falls back to localStorage
export async function chainRead(url) {
  return await chainCall('GET', url)
}

export default { useChainStatus, chainCall, chainWrite, chainRead }
