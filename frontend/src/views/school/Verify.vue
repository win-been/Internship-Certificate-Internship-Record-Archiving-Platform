<template>
  <div class="page">
    <div class="page-head"><h1>存证核验</h1></div>
    <div class="verify-box"><el-input v-model="hash" placeholder="输入哈希值核验..." size="large" clearable/><el-button type="primary" size="large" style="margin-left:12px" @click="doVerify" :loading="loading">核验</el-button></div>
    <div class="result-card" v-if="result">
      <div class="vok" v-if="result.valid"><el-icon :size="20" color="#059669"><CircleCheck/></el-icon><span>核验通过</span></div>
      <div class="vno" v-else><el-icon :size="20" color="#DC2626"><Close/></el-icon><span>未找到记录</span></div>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Close } from '@element-plus/icons-vue'
import { useDataStore } from '../../stores/dataStore'
import { isValidHash } from '../../utils/hash'
const dataStore = useDataStore()
const hash = ref(''), loading = ref(false), result = ref(null)
const doVerify = () => {
  const val = hash.value.trim()
  if (!val) return
  if (!isValidHash(val)) {
    result.value = { valid: false, msg: '哈希格式错误，请输入0x开头的66位十六进制字符串' }
    ElMessage.warning('哈希格式错误，请检查')
    return
  }
  loading.value = true
  setTimeout(() => {
    const found = dataStore.archives.find(a => a.hash === val)
    result.value = found ? { valid:true, ...found } : { valid:false }
    loading.value = false
  }, 500)
}
</script>
<style scoped>
.page { width:100%; }
.page-head { margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.verify-box { display:flex; align-items:center; margin-bottom:20px; }
.result-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:24px; }
.vok, .vno { display:flex; align-items:center; gap:10px; font-size:15px; font-weight:600; }
.vok { color:#059669; } .vno { color:#DC2626; }
</style>
