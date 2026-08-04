<template>
  <div class="page">
    <div class="page-head"><h1>实习证明</h1><el-button type="primary" @click="showCreate=true" :icon="Plus" style="background:#06b6d4;border-color:#06b6d4">创建证明</el-button></div>

    <el-table :data="list" v-loading="loading" empty-text="暂无证明" class="fancy-table" row-class-name="anim-row">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="certificateNumber" label="编号" width="140"/>
      <el-table-column prop="internshipId" label="实习ID" width="80"/>
      <el-table-column prop="certificateContent" label="内容" show-overflow-tooltip/>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}">
          <el-tag size="small" :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="链上存证" width="190">
        <template #default="{row}"><ChainBadge :tx-hash="row.blockchainTxHash" /></template>
      </el-table-column>
      <el-table-column label="操作" width="240">
        <template #default="{row}">
          <el-button size="small" v-if="row.status==='DRAFT'||row.status==='PENDING_APPROVAL'" @click="approveSchool(row.id)" :icon="School">学校审批</el-button>
          <el-button size="small" v-if="row.status==='DRAFT'||row.status==='PENDING_APPROVAL'" @click="approveEnterprise(row.id)" :icon="OfficeBuilding">企业审批</el-button>
          <el-button size="small" type="success" v-if="row.status==='APPROVED'&&!row.blockchainTxHash" @click="uploadChain(row.id)" :loading="row._uploading" :icon="Link">上链</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showCreate" title="创建实习证明" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="实习ID"><el-input-number v-model="form.internshipId" style="width:100%"/></el-form-item>
        <el-form-item label="证明内容"><el-input v-model="form.certificateContent" type="textarea" :rows="6" placeholder="实习证明内容..."/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate=false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="saving">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/request'
import { useUserStore } from '../stores/userStore'
import { ElMessage } from 'element-plus'
import { Plus, School, OfficeBuilding, Link } from '@element-plus/icons-vue'
import ChainBadge from '../components/ChainBadge.vue'

const userStore = useUserStore()
const list = ref([])
const loading = ref(false)
const saving = ref(false)
const showCreate = ref(false)
const form = ref({ internshipId:1, certificateContent:'' })

const statusType = (s) => ({ DRAFT:'info', PENDING_APPROVAL:'warning', APPROVED:'success', ON_CHAIN:'', VERIFIED:'success' }[s]||'info')
const statusLabel = (s) => ({ DRAFT:'草稿', PENDING_APPROVAL:'待审批', APPROVED:'已审批', ON_CHAIN:'已上链', VERIFIED:'已验证' }[s]||s)

const load = async () => {
  loading.value = true
  try {
    const uid = userStore.userInfo.userId
    const res = await api.get(`/certificates/student/${uid}?size=50`)
    list.value = (res.data || []).map(r=>({...r,_uploading:false}))
  } catch(e){ console.error(e) }
  finally { loading.value = false }
}

const handleCreate = async () => {
  saving.value = true
  try {
    await api.post('/certificates', { ...form.value, studentId: userStore.userInfo.userId })
    ElMessage.success('证明创建成功')
    showCreate.value = false
    load()
  } catch(e){ ElMessage.error('创建失败') }
  finally { saving.value = false }
}

const approveSchool = async (id) => {
  try {
    await api.post(`/certificates/${id}/approve-school?approverId=${userStore.userInfo.userId}`)
    ElMessage.success('学校审批完成')
    load()
  } catch(e){ ElMessage.error('审批失败') }
}

const approveEnterprise = async (id) => {
  try {
    await api.post(`/certificates/${id}/approve-enterprise?approverId=${userStore.userInfo.userId}`)
    ElMessage.success('企业审批完成')
    load()
  } catch(e){ ElMessage.error('审批失败') }
}

const uploadChain = async (id) => {
  const row = list.value.find(r=>r.id===id)
  if(row) row._uploading = true
  try {
    await api.post(`/certificates/${id}/upload-blockchain`)
    ElMessage.success('已上链存证！')
    load()
  } catch(e){ ElMessage.error('上链失败: '+(e.response?.data?.message||e.message)) }
  finally { if(row) row._uploading = false }
}

onMounted(load)
</script>

<style scoped>
.page { width: 100%; animation: page-in .4s ease both; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-head h1 { color:#e2e8f0; font-size:22px; }

@keyframes page-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

<style>
.fancy-table .anim-row:hover td { background-color: rgba(6, 182, 212, .07) !important; }
.fancy-table .el-table__row td { transition: background-color .18s ease; }
</style>