<template>
  <div class="page">
    <div class="page-head">
      <h1>纠纷处理</h1>
      <div class="fr">
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width:110px">
          <el-option label="待处理" value="PENDING"/><el-option label="已解决" value="RESOLVED"/>
        </el-select>
      </div>
    </div>
    <div class="table-card">
      <el-table :data="filtered" style="width:100%" empty-text="暂无纠纷" v-loading="loading">
        <el-table-column prop="student" label="学生" width="80"/>
        <el-table-column label="申诉原因" min-width="250">
          <template #default="{row}">{{ displayText(row.reason, '申诉原因待补充') }}</template>
        </el-table-column>
        <el-table-column prop="date" label="时间" width="110"/>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{row}"><span class="st" :class="row.status==='PENDING'?'st-pend':'st-done'">{{row.status==='PENDING'?'待处理':'已解决'}}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{row}">
            <el-button v-if="row.status==='PENDING'" class="btn-do" size="small" @click="openDeal(row)">处理</el-button>
            <el-button v-else link type="primary" size="small" @click="viewResolved(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dealVisible" title="纠纷处理" width="460px">
      <el-form :model="dealForm" label-width="80px">
        <el-form-item label="学生"><el-input :model-value="dealForm.student" disabled/></el-form-item>
        <el-form-item label="原因"><el-input :model-value="dealForm.reason" type="textarea" :rows="3" disabled/></el-form-item>
        <el-form-item label="处理意见"><el-input v-model="dealForm.opinion" type="textarea" :rows="4" placeholder="请填写企业处理意见，提交后将自动上链存证"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="dealVisible=false">取消</el-button><el-button type="primary" @click="submitDeal" :loading="submitting">提交处理并上链</el-button></template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="纠纷详情" width="520px">
      <div v-if="currentDispute" class="detail-grid">
        <div class="detail-item"><span>学生</span><strong>{{ currentDispute.student || '-' }}</strong></div>
        <div class="detail-item"><span>状态</span><strong>{{ statusText(currentDispute.status) }}</strong></div>
        <div class="detail-item"><span>申诉时间</span><strong>{{ currentDispute.date || '-' }}</strong></div>
        <div class="detail-item"><span>处理时间</span><strong>{{ currentDispute.resolvedDate || '-' }}</strong></div>
        <div class="detail-item full"><span>申诉原因</span><p>{{ displayText(currentDispute.reason, '申诉原因待补充') }}</p></div>
        <div class="detail-item full"><span>处理意见</span><p>{{ displayText(currentDispute.opinion, '暂无处理意见') }}</p></div>
        <div class="detail-item full"><span>链上存证</span><p>{{ archiveText(currentDispute) }}</p></div>
      </div>
      <template #footer>
        <el-button @click="detailVisible=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'

const userStore = useUserStore()
const myCompanyId = computed(() => userStore.userInfo?.userId || null)
const filterStatus = ref(''), dealVisible = ref(false), detailVisible = ref(false), loading = ref(false), submitting = ref(false)
const disputes = ref([])
const dealForm = reactive({ id:0, student:'', reason:'', opinion:'' })
const currentDispute = ref(null)
const isBadText = (value) => !value || /^[?？\s]+$/.test(String(value).trim())
const displayText = (value, fallback) => isBadText(value) ? fallback : value
const statusText = (status) => status === 'PENDING' ? '待处理' : '已解决'
const archiveText = (row) => {
  if (row?.txHash || row?.blockchainTxHash) return '已上链，交易哈希：' + (row.txHash || row.blockchainTxHash)
  if (row?.chainStatus === 'ON_CHAIN') return '已完成链上存证'
  if (row?.archiveId || row?.sourceId) return '已生成存证记录'
  return '处理后将自动写入链上归档'
}

onMounted(async () => {
  if (!myCompanyId.value) return
  loading.value = true
  try {
    const res = await api.get('/data/disputes', { params: { companyId: myCompanyId.value } })
    disputes.value = res.data || []
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error('纠纷数据加载失败')
    disputes.value = []
  } finally { loading.value = false }
})

const filtered = computed(() => {
  let l = disputes.value.filter(d => d.companyId === myCompanyId.value)
  if (filterStatus.value) l = l.filter(d => d.status === filterStatus.value)
  return l
})

const openDeal = (row) => {
  dealForm.id = row.id; dealForm.student = row.student
  dealForm.reason = displayText(row.reason, '申诉原因待补充'); dealForm.opinion = ''
  dealVisible.value = true
}

const submitDeal = async () => {
  if (!myCompanyId.value) {
    ElMessage.warning('请先使用企业账号登录')
    return
  }
  submitting.value = true
  try {
    const payload = {
      status: 'RESOLVED',
      opinion: dealForm.opinion,
      resolvedDate: new Date().toISOString().slice(0, 10)
    }
    const res = await api.put('/data/disputes/' + dealForm.id, payload)
    const d = disputes.value.find(d => d.id === dealForm.id)
    if (d) Object.assign(d, res.data || payload)
    ElMessage.success('处理完成，已写入链上归档')
    dealVisible.value = false
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '纠纷处理失败')
  } finally { submitting.value = false; dealVisible.value = false }
}

const viewResolved = (row) => {
  currentDispute.value = row
  detailVisible.value = true
}
</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.fr { display:flex; gap:10px; }
.table-card { background:#FFF; border-radius:14px; padding:20px; box-shadow:0 2px 14px rgba(0,0,0,0.05); }
.st { display:inline-block; padding:2px 10px; border-radius:10px; font-size:12px; font-weight:500; }
.st-pend { background:#FFF7ED; color:#EA580C; }
.st-done { background:#ECFDF5; color:#059669; }
.btn-do { background:#2563EB!important; border-color:#2563EB!important; color:#FFF!important; border-radius:6px!important; padding:4px 14px!important; font-size:12px!important; }
.detail-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px; }
.detail-item { padding:12px 14px; border:1px solid rgba(226,232,240,0.9); border-radius:12px; background:#F8FAFC; }
.detail-item.full { grid-column:1/-1; }
.detail-item span { display:block; margin-bottom:6px; font-size:12px; color:#94A3B8; }
.detail-item strong { color:#334155; font-size:14px; }
.detail-item p { margin:0; color:#334155; line-height:1.7; white-space:pre-wrap; }
</style>
