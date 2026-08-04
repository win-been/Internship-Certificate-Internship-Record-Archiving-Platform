<template>
  <div class="page">
    <div class="page-head"><h1>存证档案管理</h1><div class="fr"><el-select v-model="filterType" placeholder="类型" clearable size="default" style="width:140px"><el-option label="录用单" value="录用单"/><el-option label="考核表" value="考核表"/><el-option label="实习协议" value="实习协议"/><el-option label="企业入驻" value="企业入驻"/><el-option label="申诉处理" value="申诉处理"/><el-option label="实习证书" value="实习证书"/></el-select></div></div>
    <div class="table-card">
      <el-table v-loading="loading" :data="filtered" style="width:100%" empty-text="暂无档案">
        <el-table-column prop="type" label="类型" width="90"><template #default="{row}"><span class="tt" :class="row.type==='录用单'?'tt-hire':'tt-exam'">{{row.type}}</span></template></el-table-column>
        <el-table-column label="名称" min-width="200">
          <template #default="{row}">
            <div class="archive-name">{{ archiveName(row) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="内容哈希" min-width="220">
          <template #default="{row}">
            <div class="hash-cell">
              <el-tooltip :content="row.hash || '暂无内容哈希'" placement="top">
                <span class="hash-text">{{ formatHash(row.hash) }}</span>
              </el-tooltip>
              <el-button class="btn-copy" size="small" @click="copyHash(row.hash)">
                <el-icon :size="13"><DocumentCopy /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="交易哈希" min-width="220">
          <template #default="{row}">
            <div v-if="row.txHash" class="hash-cell">
              <el-tooltip :content="row.txHash" placement="top">
                <span class="hash-text tx">{{ formatHash(row.txHash) }}</span>
              </el-tooltip>
              <el-button class="btn-copy" size="small" @click="copyHash(row.txHash)">
                <el-icon :size="13"><DocumentCopy /></el-icon>
              </el-button>
            </div>
            <el-tooltip v-else :content="row.chainError || '未返回交易哈希'" placement="top">
              <span class="chain-error">暂无交易</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="存证时间" width="170"/>
        <el-table-column label="同步状态" width="110" align="center">
          <template #default="{row}"><span class="chain-state" :class="'chain-' + chainState(row)">{{ chainLabel(row) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{row}">
            <div class="op-row">
              <el-button class="btn-export" size="small" @click="exportArchive(row)">导出凭据</el-button>
              <el-button class="btn-verify" size="small" @click="openVerify(row)">核验</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><span class="table-total">共 {{filtered.length}} 条</span></div>
    </div>

    <el-dialog v-model="verifyVisible" width="640px" class="verify-dialog" :show-close="false">
      <div class="verify-card" :class="{ pending: chainState(v) !== 'on-chain' }">
        <div class="verify-head">
          <div class="verify-mark">
            <el-icon :size="26"><CircleCheck /></el-icon>
          </div>
          <div>
            <div class="verify-kicker">On-chain verification</div>
            <h3>{{ chainState(v) === 'on-chain' ? '链上核验通过' : '等待链上确认' }}</h3>
            <p>{{ verifyText(v) }}</p>
          </div>
          <span class="verify-status">{{ chainLabel(v) }}</span>
        </div>

        <div class="verify-summary">
          <div>
            <span>单据名称</span>
            <strong>{{ archiveName(v) }}</strong>
          </div>
          <div>
            <span>单据类型</span>
            <strong>{{ archiveType(v.type) }}</strong>
          </div>
          <div>
            <span>存证时间</span>
            <strong>{{ v.time || '-' }}</strong>
          </div>
          <div>
            <span>区块高度</span>
            <strong>{{ v.block ? '#' + v.block : '待确认' }}</strong>
          </div>
        </div>

        <div class="hash-panel">
          <div class="hash-row-full">
            <div>
              <span>内容哈希</span>
              <code>{{ v.hash || '暂无内容哈希' }}</code>
            </div>
            <el-button class="copy-pill" size="small" @click="copyHash(v.hash)">复制</el-button>
          </div>
          <div class="hash-row-full">
            <div>
              <span>交易哈希</span>
              <code>{{ v.txHash || '暂无真实交易哈希' }}</code>
            </div>
            <el-button class="copy-pill" size="small" @click="copyHash(v.txHash)">复制</el-button>
          </div>
        </div>

        <div v-if="v.chainError" class="verify-error">{{ v.chainError }}</div>
      </div>
      <template #footer><el-button @click="verifyVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck, DocumentCopy } from '@element-plus/icons-vue'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'
const loading = ref(false)
const dataStore = useDataStore()

// Try API on mount, fallback to dataStore (already reactive via computed)
onMounted(async () => {
  if (!myCompanyId.value) return
  loading.value = true
  try {
    const res = await api.get('/data/archives', { params: { companyId: myCompanyId.value } })
    if (res.data && res.data.length > 0) {
      res.data.forEach(item => {
        if (!dataStore.archives.find(a => a.hash === item.hash)) {
          dataStore.archives.push(item)
        }
      })
    }
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error('档案数据加载失败')
  } finally { loading.value = false }
})
const userStore = useUserStore()
const myCompanyId = computed(() => userStore.userInfo?.userId || null)
const filterType=ref(''), verifyVisible=ref(false)
const v=reactive({name:'',hash:'',txHash:'',time:'',block:0,chainStatus:'',chainState:'',chainError:''})
const filtered = computed(() => {
  let list = myCompanyId.value ? (dataStore.archives || []).filter(a => a.companyId === myCompanyId.value) : []
  return filterType.value ? list.filter(a=>a.type===filterType.value) : list
})
const openVerify=(row)=>{Object.assign(v, row); verifyVisible.value=true}
const hasBadText = (value) => !value || String(value).includes('?')
const archiveType = (type) => {
  const map = {
    '岗位发布': '岗位发布',
    '简历投递': '简历投递',
    '录用处理': '录用处理',
    '实习登记': '实习备案',
    '考核提交': '考核提交',
    '纠纷发起': '纠纷发起',
    '纠纷处理': '纠纷处理',
    '协议签署': '协议签署',
    '日报提交': '日报提交'
  }
  return map[type] || type || '存证记录'
}
const archiveName = (row) => {
  const raw = row?.name || ''
  if (!hasBadText(raw)) return raw
  const type = archiveType(row?.type)
  const subject = row?.student || row?.studentName || '学生'
  const date = row?.time ? String(row.time).slice(0, 10) : ''
  return [subject, type, date].filter(Boolean).join(' · ')
}
const formatHash = (h) => h ? h.slice(0,10) + '...' + h.slice(-8) : '暂无'
const chainState = (row) => row?.chainStatus === 'ON_CHAIN' || row?.chainState === 'on-chain' || row?.txHash ? 'on-chain' : 'local'
const chainLabel = (row) => chainState(row) === 'on-chain' ? '已上链' : '待上链'
const verifyText = (row) => chainState(row) === 'on-chain' ? '核验通过，已返回真实链上交易哈希' : '尚未拿到 WeBASE 交易哈希，请查看失败原因'
const exportArchive = (row) => {
  const content = [
    '实习存证档案导出',
    '名称: ' + archiveName(row),
    '类型: ' + archiveType(row.type),
    '内容哈希: ' + (row.hash || ''),
    '交易哈希: ' + (row.txHash || '暂无真实交易哈希'),
    '上链状态: ' + chainLabel(row),
    '失败原因: ' + (row.chainError || '无'),
    '存证时间: ' + row.time,
    '区块号: #' + (row.block || 'N/A'),
    '',
    '本文档由区块链存证系统自动生成，可通过链上哈希进行真伪核验。'
  ].join('\n')
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = archiveName(row) + '.txt'
  a.click(); URL.revokeObjectURL(url)
  ElMessage.success('导出成功: ' + archiveName(row) + '.txt')
}
const copyHash = async (hash) => {
  if (!hash) { ElMessage.warning('暂无可复制的哈希'); return }
  try { await navigator.clipboard.writeText(hash); ElMessage.success({ message: '复制成功，可前往核验页面校验链上数据', duration: 2000 }) } catch (e) { ElMessage.warning('复制失败，请手动选择复制') }
}
</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.fr { display:flex; gap:10px; }
.tt { display:inline-block; padding:2px 10px; border-radius:10px; font-size:12px; font-weight:500; }
.tt-hire { background:#ECFDF5; color:#059669; }
.tt-exam { background:#EFF6FF; color:#2563EB; }
.chain-state { display:inline-block; padding:2px 10px; border-radius:10px; font-size:12px; font-weight:600; }
.chain-on-chain { background:#ECFDF5; color:#059669; }
.chain-local { background:#FFFBEB; color:#D97706; }
.chain-error { color:#D97706; font-size:12px; cursor:help; }
.op-row { display:flex; gap:6px; }
.archive-name { color:#334155; font-weight:600; line-height:1.5; }
.btn-export { background:#EFF6FF!important; border:1px solid #BFDBFE!important; color:#2563EB!important; border-radius:6px!important; padding:4px 12px!important; font-size:12px!important; }
.btn-verify { background:#F8FAFC!important; border:1px solid #E2E8F0!important; color:#64748B!important; border-radius:6px!important; padding:4px 12px!important; font-size:12px!important; }
.hash-cell { display:flex; align-items:center; gap:6px; } .hash-text { font-family:monospace; font-size:11px; color:#334155; word-break:break-all; cursor:default; } .hash-text.tx { color:#047857; } .btn-copy { background:transparent!important; border:none!important; color:#94A3B8!important; padding:2px!important; min-height:auto!important; } .btn-copy:hover { color:#2563EB!important; }
:deep(.verify-dialog .el-dialog__body) { padding:0; }
:deep(.verify-dialog .el-dialog__footer) { padding:14px 22px 18px; border-top:1px solid rgba(226,232,240,.75); }
.verify-card { overflow:hidden; border-radius:14px 14px 0 0; background:linear-gradient(180deg,#F8FFFB 0%,#FFF 42%); }
.verify-card.pending { background:linear-gradient(180deg,#FFFBEB 0%,#FFF 42%); }
.verify-head { position:relative; display:flex; align-items:flex-start; gap:16px; padding:24px 28px 22px; border-bottom:1px solid rgba(226,232,240,.82); }
.verify-head::after { content:''; position:absolute; left:0; right:0; bottom:-1px; height:2px; background:linear-gradient(90deg,#059669,rgba(37,99,235,.7),transparent); }
.verify-card.pending .verify-head::after { background:linear-gradient(90deg,#D97706,rgba(37,99,235,.45),transparent); }
.verify-mark { flex:0 0 auto; width:48px; height:48px; display:flex; align-items:center; justify-content:center; border-radius:16px; background:#ECFDF5; color:#059669; box-shadow:inset 0 0 0 1px rgba(5,150,105,.12); }
.verify-card.pending .verify-mark { background:#FFFBEB; color:#D97706; box-shadow:inset 0 0 0 1px rgba(217,119,6,.16); }
.verify-kicker { color:#94A3B8; font-size:11px; font-weight:800; letter-spacing:.08em; text-transform:uppercase; }
.verify-head h3 { margin:4px 0 4px; color:#0F172A; font-size:22px; line-height:1.2; }
.verify-head p { margin:0; color:#64748B; font-size:13px; }
.verify-status { margin-left:auto; padding:5px 11px; border-radius:999px; background:#ECFDF5; color:#047857; font-size:12px; font-weight:800; white-space:nowrap; }
.verify-card.pending .verify-status { background:#FFFBEB; color:#B45309; }
.verify-summary { display:grid; grid-template-columns:1fr 1fr; gap:12px; padding:20px 28px 8px; }
.verify-summary div { padding:13px 14px; border:1px solid rgba(226,232,240,.78); border-radius:12px; background:rgba(248,250,252,.7); }
.verify-summary span,
.hash-row-full span { display:block; margin-bottom:6px; color:#94A3B8; font-size:12px; font-weight:700; }
.verify-summary strong { color:#334155; font-size:13px; line-height:1.5; }
.hash-panel { display:flex; flex-direction:column; gap:10px; padding:12px 28px 22px; }
.hash-row-full { display:flex; align-items:flex-start; gap:12px; padding:14px; border:1px solid rgba(226,232,240,.82); border-radius:12px; background:#F8FAFC; }
.hash-row-full div { min-width:0; flex:1; }
.hash-row-full code { display:block; color:#1E293B; font-family:Consolas, Monaco, monospace; font-size:12px; line-height:1.6; word-break:break-all; white-space:normal; }
.copy-pill { flex:0 0 auto; border:1px solid #D9E4F2!important; color:#2563EB!important; background:#FFF!important; border-radius:999px!important; }
.verify-error { margin:0 28px 24px; padding:12px 14px; border-radius:12px; background:#FFFBEB; color:#B45309; font-size:12px; line-height:1.6; word-break:break-all; }
</style>
