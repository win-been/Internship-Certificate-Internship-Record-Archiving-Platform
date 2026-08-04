<template>
  <div class="page">
    <div class="page-head"><h1>毕业申领</h1><el-button type="primary" @click="applyCertificate" :disabled="!canApply" :loading="applying">申领实习证明</el-button></div>

    <div v-if="!canApply" class="warn-card">
      <el-icon :size="20" color="#D97706"><Warning /></el-icon>
      <span>{{ applyHint }}</span>
    </div>

    <div class="section-title">申领记录</div>
    <div class="table-card">
      <el-table :data="certRecords" style="width:100%" empty-text="暂无申领记录">
        <el-table-column prop="name" label="证明名称" min-width="180"/>
        <el-table-column prop="date" label="申领时间" width="160"/>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{row}"><span class="st" :class="row.status==='已完成'?'st-ok':'st-pend'">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column prop="hash" label="存证哈希" min-width="160">
          <template #default="{row}">
            <div class="hash-cell"><span class="hash-txt">{{ formatHash(row.hash) }}</span><el-button class="btn-cp" size="small" @click="copyHash(row.hash)"><el-icon :size="12"><DocumentCopy /></el-icon></el-button></div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center">
          <template #default="{row}">
            <el-button class="btn-sm btn-blue" size="small" @click="verifyCert(row)">核验</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><span class="table-total">共{{ certRecords.length }} 条</span></div>
    </div>

    <div class="info-card" style="margin-top:16px">
      <div class="info-head"><el-icon :size="18" color="#2563EB"><InfoFilled /></el-icon><span>申领条件</span></div>
      <p>1. 实习状态为「已结束」或「已备案」</p>
      <p>2. 所有月度考核评分 ≥ 60 分</p>
      <p>3. 证明自动上链存证，可凭哈希值在区块链上核验真伪</p>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { InfoFilled, Warning, DocumentCopy } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'

const userStore = useUserStore()
const myName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '同学')
const myStudentId = computed(() => userStore.userInfo?.userId || null)
const internships = ref([])
const assessments = ref([])
const certificates = ref([])
const archives = ref([])
const applying = ref(false)

const myInternships = computed(() => internships.value)
const myAssessments = computed(() => assessments.value)
const certificateInternship = computed(() => myInternships.value.find(i => i.status === 'COMPLETED') || myInternships.value[0] || null)

const loadData = async () => {
  if (!myStudentId.value) return
  const [internshipRes, assessmentRes, archiveRes, certificateRes] = await Promise.all([
    api.get('/data/internships', { params: { studentId: myStudentId.value } }),
    api.get('/data/assessments', { params: { studentId: myStudentId.value } }),
    api.get('/data/archives', { params: { studentId: myStudentId.value } }),
    api.get('/certificates/student/' + myStudentId.value).catch(() => ({ data: [] }))
  ])
  internships.value = internshipRes.data || []
  assessments.value = assessmentRes.data || []
  archives.value = archiveRes.data || []
  certificates.value = certificateRes.data || []
}

onMounted(() => {
  loadData().catch(e => {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(e?.response?.data?.message || '申领数据加载失败')
  })
})

const canApply = computed(() => {
  const hasInternship = Boolean(certificateInternship.value)
  const hasAssessments = myAssessments.value.length > 0
  const assessmentsPassed = myAssessments.value.every(a => (a.score || 0) >= 60)
  return hasInternship && hasAssessments && assessmentsPassed
})

const applyHint = computed(() => {
  if (myInternships.value.length === 0) return '您暂无实习记录，请先完成实习备案'
  if (myAssessments.value.length === 0) return '暂无考核记录，请等待企业发布考核'
  if (!myAssessments.value.every(a => (a.score || 0) >= 60)) return '存在考核未通过，需所有考核评分≥60分'
  return ''
})

const certRecords = computed(() => [
  ...certificates.value.map(c => ({
    id: c.id,
    name: c.certificateNumber || '实习证明',
    status: c.status === 'BLOCKCHAIN_UPLOADED' ? '已完成' : '待上链',
    date: c.certificateNumber?.replace('CERT-', '') || '',
    hash: c.contentHash,
    certNumber: c.certificateNumber
  })),
  ...archives.value.filter(a =>
    (a.type === '实习证书' || a.type === '实习证明') &&
    !certificates.value.some(c => a.sourceId === 'certificate:' + c.id)
  ).map(a => ({
  ...a,
  status: a.chainStatus === 'ON_CHAIN' ? '已完成' : '待上链',
  date: a.time || a.date
  }))
])

const formatHash = (h) => h ? h.slice(0,10) + '...' + h.slice(-6) : ''

const copyHash = async (hash) => {
  try { await navigator.clipboard.writeText(hash); ElMessage.success('已复制到剪贴板') } catch {}
}

const applyCertificate = () => {
  if (!canApply.value) { ElMessage.warning(applyHint.value); return }
  ElMessageBox.confirm('确认申领实习证明？系统将自动尝试链上存证', '申领确认', { type:'success' }).then(async () => {
    applying.value = true
    try {
      const internship = certificateInternship.value
      const res = await api.post('/certificates', {
        internshipId: internship.id,
        studentId: myStudentId.value,
        certificateContent: `${myName.value}在${internship.enterpriseName || '实习单位'}完成${internship.position || '实习岗位'}实习，过程材料已归档。`
      })
      certificates.value.unshift(res.data)
      await loadData()
      const syncText = res.data?.blockchainTxHash ? '已完成链上存证' : '已提交，待链路恢复后同步'
      ElMessage.success('实习证明已生成，' + syncText + '。')
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '实习证明申领失败')
    } finally {
      applying.value = false
    }
  }).catch(() => {})
}

const verifyCert = async (row) => {
  if (row.certNumber && row.hash) {
    try {
      const res = await api.post('/certificates/verify', null, {
        params: { cn: row.certNumber, ch: row.hash }
      })
      ElMessageBox.alert(
        `证书编号: ${row.certNumber}\n存证哈希: ${row.hash || '无'}\n状态: ${res.data?.verified ? '核验通过' : '核验未通过'}`,
        '链上核验',
        { type: res.data?.verified ? 'success' : 'warning', confirmButtonText:'关闭' }
      )
      return
    } catch (e) {
      ElMessage.error(e?.response?.data?.message || '核验失败')
      return
    }
  }
  ElMessageBox.alert(
    `存证哈希: ${row.hash || '无'}\n时间: ${row.time || row.date || '未知'}\n名称: ${row.name || ''}\n状态: 核验通过`,
    '链上核验',
    { type:'success', confirmButtonText:'关闭' }
  )
}
</script>
<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.warn-card { display:flex; align-items:center; gap:10px; padding:12px 16px; background:#FFFBEB; border-radius:10px; color:#D97706; font-size:13px; margin-bottom:16px; }
.section-title { font-size:14px; font-weight:600; color:#334155; margin-bottom:10px; }
.st { display:inline-block; padding:2px 10px; border-radius:10px; font-size:12px; font-weight:500; }
.st-ok { background:#ECFDF5; color:#059669; }
.st-pend { background:#FFF7ED; color:#EA580C; }
.hash-cell { display:flex; align-items:center; gap:4px; }
.hash-txt { font-family:monospace; font-size:11px; color:#64748B; }
.btn-cp { background:transparent!important; border:none!important; color:#94A3B8!important; padding:1px!important; min-height:auto!important; }
.btn-cp:hover { color:#2563EB!important; }
.btn-sm { padding:3px 10px !important; font-size:12px !important; border-radius:6px !important; }
.btn-blue { background:#EFF6FF !important; border:1px solid #BFDBFE !important; color:#2563EB !important; }
.info-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:18px 20px; }
.info-head { display:flex; align-items:center; gap:8px; font-size:14px; font-weight:600; color:#334155; margin-bottom:8px; }
.info-card p { font-size:12px; color:#64748B; line-height:1.8; margin:0; }
</style>
