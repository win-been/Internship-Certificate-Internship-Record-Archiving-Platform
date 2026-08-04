import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const STORAGE_KEY = 'platform_system_config'
const LEGACY_WEBASE_URL = 'http://192.168.58.128:5002/WeBASE-Front'

const defaults = {
  platformName: '实习存证平台',
  reviewMode: 'manual',
  autoChain: true,
  aiEnabled: true,
  webaseUrl: 'http://175.178.120.23:5002/WeBASE-Front',
  certContract: '0x45089777a6f445440344e925d4c154e9394a7734',
  recordContract: '0x626116d30a2ae564c8c56738e5fb51dcb65aa6ae',
}

export const useSystemConfig = defineStore('systemConfig', () => {
  let initial = { ...defaults }
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY))
    if (saved) Object.assign(initial, saved)
    if (initial.webaseUrl === LEGACY_WEBASE_URL) initial.webaseUrl = defaults.webaseUrl
  } catch (e) {}

  const platformName = ref(initial.platformName)
  const reviewMode = ref(initial.reviewMode)
  const autoChain = ref(initial.autoChain)
  const aiEnabled = ref(initial.aiEnabled)
  const webaseUrl = ref(initial.webaseUrl)
  const certContract = ref(initial.certContract)
  const recordContract = ref(initial.recordContract)

  const isAutoReview = computed(() => reviewMode.value === 'auto')

  function save(config) {
    if (config.platformName !== undefined) platformName.value = config.platformName
    if (config.reviewMode !== undefined) reviewMode.value = config.reviewMode
    if (config.autoChain !== undefined) autoChain.value = config.autoChain
    if (config.aiEnabled !== undefined) aiEnabled.value = config.aiEnabled
    if (config.webaseUrl !== undefined) webaseUrl.value = config.webaseUrl
    if (config.certContract !== undefined) certContract.value = config.certContract
    if (config.recordContract !== undefined) recordContract.value = config.recordContract
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        platformName: platformName.value,
        reviewMode: reviewMode.value,
        autoChain: autoChain.value,
        aiEnabled: aiEnabled.value,
        webaseUrl: webaseUrl.value,
        certContract: certContract.value,
        recordContract: recordContract.value,
      }))
    } catch (e) {}
    if (platformName.value) document.title = platformName.value
  }

  function reset() {
    save({ ...defaults })
  }

  return { platformName, reviewMode, autoChain, aiEnabled, webaseUrl, certContract, recordContract, isAutoReview, save, reset }
})
