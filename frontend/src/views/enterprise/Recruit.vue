<template>
  <div class="page">
    <div class="page-head">
      <h1>录用管理</h1>
      <el-button text @click="fetchData" :loading="loading">刷新数据</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterJob" placeholder="按岗位筛选" clearable style="width:180px">
        <el-option v-for="j in myJobs" :key="j.id" :label="j.title" :value="j.id" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="按状态筛选" clearable style="width:140px">
        <el-option label="待处理" value="pending" />
        <el-option label="已拒绝" value="rejected" />
      </el-select>
    </div>

    <div class="table-card">
      <div class="seg-tabs">
        <span class="seg-item" :class="{active:tab==='resume'}" @click="tab='resume'">简历筛选({{ filteredApps.length }})</span>
        <span class="seg-item" :class="{active:tab==='hired'}" @click="tab='hired'">已录用({{ hiredStudents.length }})</span>
      </div>

      <template v-if="tab==='resume'">
        <el-table :data="filteredApps" style="width:100%" empty-text="暂无需要筛选的简历" v-loading="loading">
          <el-table-column label="姓名" width="130">
            <template #default="{row}">{{ appName(row) }}</template>
          </el-table-column>
          <el-table-column label="学校" min-width="150">
            <template #default="{row}">{{ appSchool(row) }}</template>
          </el-table-column>
          <el-table-column label="专业" width="130">
            <template #default="{row}">{{ appMajor(row) }}</template>
          </el-table-column>
          <el-table-column label="投递岗位" min-width="170">
            <template #default="{row}">{{ appJobTitle(row) }}</template>
          </el-table-column>
          <el-table-column prop="applyDate" label="投递时间" width="110" />
          <el-table-column label="状态" width="90" align="center">
            <template #default="{row}">
              <span class="status-tag" :class="row.status === 'pending' ? 'tag-orange' : 'tag-gray'">
                {{ row.status === 'pending' ? '待处理' : '已拒绝' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="230" align="center">
            <template #default="{row}">
              <div class="op-row">
                <el-button v-if="row.status === 'pending'" class="btn-sm btn-green" size="small" @click="openOffer(row)">录用</el-button>
                <el-button class="btn-sm btn-blue" size="small" @click="viewApp(row)">查看</el-button>
                <el-button v-if="row.status === 'pending'" class="btn-sm btn-gray" size="small" @click="doReject(row)">拒绝</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <template v-if="tab==='hired'">
        <el-table :data="hiredStudents" style="width:100%" empty-text="暂无已录用学生" v-loading="loading">
          <el-table-column prop="name" label="学生姓名" width="140" />
          <el-table-column prop="school" label="学校" min-width="150" />
          <el-table-column prop="major" label="专业" width="130" />
          <el-table-column prop="position" label="录用岗位" min-width="170" />
          <el-table-column prop="startDate" label="入职日期" width="120" align="center" />
          <el-table-column label="协议" width="100" align="center">
            <template #default="{row}">
              <span class="status-tag" :class="row.signed ? 'tag-green' : 'tag-orange'">{{ row.signed ? '已签署' : '待签署' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="center">
            <template #default="{row}">
              <div class="op-row">
                <el-button v-if="!row.signed" class="btn-sm btn-blue" size="small" @click="doSign(row)">签协议</el-button>
                <el-button class="btn-sm btn-blue" size="small" @click="viewHired(row)">详情</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </div>

    <el-dialog v-model="detailVisible" title="简历详情" width="480px" append-to-body>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="学校">{{ detail.school }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ detail.major }}</el-descriptions-item>
        <el-descriptions-item label="投递岗位">{{ detail.jobTitle }}</el-descriptions-item>
        <el-descriptions-item label="投递时间">{{ detail.applyDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible=false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="hiredDetailVisible" title="录用详情" width="520px" append-to-body>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="学生姓名">{{ hiredDetail.name }}</el-descriptions-item>
        <el-descriptions-item label="学校">{{ hiredDetail.school }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ hiredDetail.major }}</el-descriptions-item>
        <el-descriptions-item label="录用岗位">{{ hiredDetail.position }}</el-descriptions-item>
        <el-descriptions-item label="投递日期">{{ hiredDetail.applyDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入职日期">{{ hiredDetail.startDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="协议状态">{{ hiredDetail.signed ? '已签署' : '待签署' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="hiredDetailVisible=false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="offerVisible" title="发送录用函" width="500px" append-to-body>
      <el-form :model="offerForm" label-width="86px">
        <el-form-item label="学生"><el-input :model-value="offerForm.name" disabled /></el-form-item>
        <el-form-item label="岗位"><el-input v-model="offerForm.position" /></el-form-item>
        <el-form-item label="入职日期"><el-date-picker v-model="offerForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="薪资"><el-input v-model="offerForm.salary" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="offerVisible=false">取消</el-button>
        <el-button type="primary" @click="sendOffer" :loading="savingOffer">发送录用函并上链</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'
import { useDataStore } from '../../stores/dataStore'

const userStore = useUserStore()
const dataStore = useDataStore()
const myCompanyId = computed(() => userStore.userInfo?.userId || null)
const myCompanyName = computed(() => userStore.userInfo?.organizationName || '企业')

const tab = ref('resume')
const filterJob = ref('')
const filterStatus = ref('')
const loading = ref(false)
const savingOffer = ref(false)
const applications = ref([])
const hiredStudents = ref([])
const myJobs = ref([])
const detailVisible = ref(false)
const hiredDetailVisible = ref(false)
const offerVisible = ref(false)
const offerAppId = ref(0)
const detail = reactive({ name:'', school:'', major:'', jobTitle:'', applyDate:'', status:'' })
const hiredDetail = reactive({ name:'', school:'', major:'', position:'', applyDate:'', startDate:'', signed:false })
const offerForm = reactive({ name:'', position:'', startDate:'', salary:'3,000-5,000' })
const fetchData = async () => {
  if (!myCompanyId.value) {
    applications.value = []
    myJobs.value = []
    hiredStudents.value = []
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error('请先使用企业账号登录')
    return
  }
  loading.value = true
  try {
    const [appsRes, jobsRes, internshipsRes] = await Promise.all([
      api.get('/data/applications', { params: { companyId: myCompanyId.value } }),
      api.get('/data/jobs', { params: { companyId: myCompanyId.value } }),
      api.get('/data/internships', { params: { enterpriseId: myCompanyId.value } })
    ])
    const appList = appsRes.data || []
    const jobList = jobsRes.data || []
    applications.value = appList
    myJobs.value = jobList
    hiredStudents.value = (internshipsRes.data || []).map(i => {
      const app = findAcceptedApp(i, appList)
      const job = findJobForApp(app, jobList)
      return {
      internshipId: i.id,
      name: firstGood(app?.name, i.studentName, '未命名学生'),
      school: firstGood(app?.school, i.schoolName, '未填写学校'),
      major: firstGood(app?.major, i.major, '未填写专业'),
      position: firstGood(job?.title, app?.jobTitle, i.position, '未关联岗位'),
      jobId: app?.jobId || job?.id || null,
      applyDate: app?.applyDate || '',
      startDate: i.startDate,
      studentId: i.studentId,
      companyId: i.companyId || i.enterpriseId,
      signed: Boolean(i.agreementSigned),
      agreementSignedAt: i.agreementSignedAt,
      txHash: i.blockchainTxHash
      }
    })
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(getApiError(e, '录用数据加载失败'))
    applications.value = []
    myJobs.value = []
    hiredStudents.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const filteredApps = computed(() => {
  let list = applications.value
  list = list.filter(a => a.status !== 'accepted')
  if (filterJob.value) list = list.filter(a => Number(a.jobId) === Number(filterJob.value))
  if (filterStatus.value) list = list.filter(a => a.status === filterStatus.value)
  else list = list.filter(a => a.status === 'pending')
  return list
})

const isBadText = (value) => {
  const text = String(value ?? '').trim()
  return !text || text.includes('?')
}
const firstGood = (...values) => {
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (!isBadText(text)) return text
  }
  return ''
}
const findJobForApp = (app, jobList = myJobs.value) => {
  if (!app?.jobId) return null
  return (jobList || []).find(j => Number(j.id) === Number(app.jobId)) || null
}
const findAcceptedApp = (internship, appList = applications.value) => {
  return (appList || []).find(a =>
    a.status === 'accepted' &&
    Number(a.studentId) === Number(internship.studentId) &&
    Number(a.companyId) === Number(internship.companyId || internship.enterpriseId)
  ) || null
}
const appName = (row) => firstGood(row?.name, '未命名学生')
const appSchool = (row) => firstGood(row?.school, '未填写学校')
const appMajor = (row) => firstGood(row?.major, '未填写专业')
const appJobTitle = (row) => firstGood(findJobForApp(row)?.title, row?.jobTitle, '未关联岗位')
const statusText = (status) => ({ pending:'待处理', rejected:'已拒绝', accepted:'已录用' }[status] || status || '-')

const viewApp = (row) => {
  Object.assign(detail, {
    ...row,
    name: appName(row),
    school: appSchool(row),
    major: appMajor(row),
    jobTitle: appJobTitle(row),
    status: statusText(row.status)
  })
  detailVisible.value = true
}
const viewHired = (row) => {
  Object.assign(hiredDetail, row)
  hiredDetailVisible.value = true
}

const doReject = async (row) => {
  try {
    await ElMessageBox.confirm('确定拒绝该申请？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await api.put('/data/applications/' + row.id, { status: 'rejected' })
    row.status = 'rejected'
    const local = (dataStore.applications || []).find(a => a.id === row.id)
    if (local) local.status = 'rejected'
    ElMessage.info('已拒绝')
  } catch (e) {
    ElMessage.error(getApiError(e, '拒绝失败'))
  }
}

const openOffer = (row) => {
  offerAppId.value = row.id
  offerForm.name = appName(row)
  offerForm.position = appJobTitle(row)
  offerForm.startDate = new Date().toISOString().slice(0, 10)
  offerVisible.value = true
}

const sendOffer = async () => {
  const app = applications.value.find(a => a.id === offerAppId.value)
  if (!app) return
  savingOffer.value = true
  try {
    const res = await api.put('/data/applications/' + offerAppId.value, {
      status: 'accepted',
      position: offerForm.position,
      startDate: offerForm.startDate || new Date().toISOString().slice(0, 10)
    })
    const local = (dataStore.applications || []).find(a => a.id === offerAppId.value)
    if (local) local.status = 'accepted'
    applications.value = applications.value.filter(a => a.id !== offerAppId.value && a.studentId !== app.studentId)
    hiredStudents.value.unshift({
      internshipId: res.data?.id,
      name: appName(app),
      school: appSchool(app),
      major: appMajor(app),
      position: appJobTitle(app),
      jobId: app.jobId,
      applyDate: app.applyDate,
      startDate: offerForm.startDate,
      studentId: app.studentId,
      companyId: myCompanyId.value,
      signed: false
    })
    offerVisible.value = false
    ElMessage.success(res.data?.txHash ? '录用函已发送，并已写入链上归档' : '录用函已发送')
    fetchData()
  } catch (e) {
    ElMessage.error(getApiError(e, '录用失败'))
  } finally {
    savingOffer.value = false
  }
}

const doSign = async (row) => {
  if (!row) return
  try {
    const res = await api.put('/data/hired/sign', {
      internshipId: row.internshipId,
      studentId: row.studentId,
      companyId: row.companyId
    })
    row.signed = true
    row.agreementSignedAt = res.data?.agreementSignedAt
    row.txHash = res.data?.txHash || row.txHash
    ElMessage.success('协议已签署，并已写入链上归档')
  } catch (e) {
    ElMessage.error(getApiError(e, '协议签署失败'))
  }
}

const getApiError = (error, fallback) => {
  const data = error?.response?.data
  return data?.message || data?.error || (typeof data === 'string' ? data : '') || fallback
}
</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; margin:0; }
.filter-bar { display:flex; gap:12px; flex-wrap:wrap; }
.table-card { margin-top:12px; background:#FFF; border:1px solid rgba(226,232,240,0.82); border-radius:14px; padding:0 20px 20px; box-shadow:0 2px 14px rgba(0,0,0,0.05); }
.seg-tabs { display:flex; gap:0; padding:12px 0 0; border-bottom:1px solid #F1F5F9; margin-bottom:16px; }
.seg-item { padding:8px 20px; cursor:pointer; font-size:13px; color:#64748B; position:relative; }
.seg-item:hover { color:#334155; }
.seg-item.active { color:#2563EB; font-weight:700; }
.seg-item.active::after { content:''; position:absolute; bottom:-1px; left:20px; right:20px; height:2px; background:#2563EB; }
.op-row { display:flex; gap:6px; align-items:center; justify-content:center; flex-wrap:wrap; }
.btn-sm { padding:4px 12px !important; font-size:12px !important; border-radius:6px !important; white-space:nowrap; min-height:28px !important; }
.btn-green { background:#ECFDF5 !important; border:1px solid #A7F3D0 !important; color:#059669 !important; }
.btn-green:hover { background:#D1FAE5 !important; }
.btn-blue { background:#EFF6FF !important; border:1px solid #BFDBFE !important; color:#2563EB !important; }
.btn-blue:hover { background:#DBEAFE !important; }
.btn-gray { background:#F8FAFC !important; border:1px solid #E2E8F0 !important; color:#64748B !important; }
.btn-gray:hover { background:#FEF2F2 !important; color:#DC2626 !important; border-color:#FECACA !important; }
.status-tag { display:inline-block; padding:3px 10px; border-radius:999px; font-size:12px; font-weight:600; }
.tag-green { background:#ECFDF5; color:#059669; }
.tag-orange { background:#FFFBEB; color:#D97706; }
.tag-gray { background:#F1F5F9; color:#64748B; }
@media (max-width:760px) {
  .page-head { align-items:flex-start; flex-direction:column; }
}
</style>
