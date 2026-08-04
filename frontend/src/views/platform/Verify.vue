<template>
  <div class="page">
    <div class="page-head"><h1>公共核验</h1></div>
    <div class="verify-box">
      <el-input v-model="hash" placeholder="输入存证哈希值进行核验..." size="large" clearable @keyup.enter="doVerify"/>
      <el-button type="primary" size="large" style="margin-left:12px" @click="doVerify" :loading="loading">核验</el-button>
    </div>
    <div class="result-card" v-if="result">
      <div class="vok" v-if="result.valid"><el-icon :size="20" color="#059669"><CircleCheck/></el-icon><span>核验通过 — 该存证记录真实有效</span></div>
      <div class="vno" v-else><el-icon :size="20" color="#DC2626"><Close/></el-icon><span>{{ result.msg || '核验失败 — 未找到对应存证记录' }}</span></div>
      <el-descriptions v-if="result.valid" :column="1" border size="small" style="margin-top:16px">
        <el-descriptions-item label="哈希"><div class="hash-row"><span class="hash-text">{{ result.hash }}</span><el-button class="btn-copy" size="small" @click="copyResultHash"><el-icon :size="13"><DocumentCopy /></el-icon></el-button></div></el-descriptions-item>
        <el-descriptions-item label="存证时间">{{ result.time }}</el-descriptions-item>
        <el-descriptions-item label="区块号">#{{ result.block }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, Close, DocumentCopy } from '@element-plus/icons-vue'
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
    result.value = found ? { valid:true, ...found } : { valid:false, msg: '未找到对应存证记录' }
    loading.value = false
  }, 600)
}
const copyResultHash = async () => { if (result.value?.hash) { try { await navigator.clipboard.writeText(result.value.hash); ElMessage.success('已复制到剪贴板') } catch(e) {} } }
</script>
<style scoped>
.page { width:100%; }
.page-head { margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.verify-box { display:flex; align-items:center; margin-bottom:20px; }
.result-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:24px; }
.vok, .vno { display:flex; align-items:center; gap:10px; font-size:15px; font-weight:600; }
.vok { color:#059669; } .vno { color:#DC2626; }
.hash-row { display:flex; align-items:center; gap:8px; } .hash-text { font-family:monospace; font-size:12px; word-break:break-all; } .btn-copy { background:transparent!important; border:1px solid #E2E8F0!important; color:#64748B!important; border-radius:6px!important; padding:2px 6px!important; min-height:auto!important; } .btn-copy:hover { color:#2563EB!important; border-color:#2563EB!important; }
</style>
