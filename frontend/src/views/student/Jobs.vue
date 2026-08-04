<template>
  <div class="page">
    <div class="page-head">
      <h1>实习择业</h1>
      <div class="filter-row">
        <el-input v-model="keyword" placeholder="搜索岗位、公司、地点..." style="width:260px" clearable>
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="filterLocation" placeholder="地点" clearable style="width:110px">
          <el-option v-for="loc in locations" :key="loc" :label="loc" :value="loc" />
        </el-select>
        <el-select v-model="filterType" placeholder="类型" clearable style="width:120px">
          <el-option label="全职实习" value="全职实习" />
          <el-option label="兼职实习" value="兼职实习" />
        </el-select>
        <span class="filter-count">共 {{ filteredJobs.length }} 个岗位</span>
      </div>
    </div>

    <el-alert
      v-if="placementLocked"
      class="lock-alert"
      type="success"
      show-icon
      :closable="false"
      title="你已有录用或在岗实习，暂不能继续投递其他岗位。"
    />

    <el-row :gutter="16" v-loading="loading">
      <el-col :xs="24" :sm="12" :md="8" v-for="job in filteredJobs" :key="job.id">
        <div class="job-card">
          <h3>{{ job.title }}</h3>
          <p class="company">{{ job.company }}</p>
          <div class="tags">
            <span class="jt">{{ job.location || '不限' }}</span>
            <span class="jt jt-salary">{{ job.salary || '面议' }}</span>
            <span class="jt jt-type">{{ job.type || '实习' }}</span>
          </div>
          <p class="desc">{{ job.description }}</p>
          <div class="job-foot">
            <span class="jf-count">{{ job.count || 0 }} 人已投递</span>
            <el-button
              v-if="canApply(job)"
              type="primary"
              size="small"
              @click="applyJob(job)"
            >投递简历</el-button>
            <el-button v-else size="small" disabled class="btn-applied">
              <el-icon :size="14"><CircleCheck /></el-icon>
              {{ applyButtonText(job) }}
            </el-button>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-if="filteredJobs.length===0 && !loading" description="暂无可投递的岗位" :image-size="80" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, Search } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/userStore'
import { useDataStore } from '../../stores/dataStore'
import api from '../../api/request'

const userStore = useUserStore()
const dataStore = useDataStore()
const keyword = ref('')
const filterLocation = ref('')
const filterType = ref('')
const loading = ref(false)
const allJobs = ref([])
const approvedCompanies = ref([])
const myApplications = ref([])
const myInternships = ref([])

const myId = computed(() => userStore.userInfo?.userId || null)
const myName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '')
const locations = computed(() => [...new Set(allJobs.value.map(j => j.location).filter(Boolean))])
const placementLocked = computed(() =>
  myApplications.value.some(a => a.status === 'accepted') ||
  myInternships.value.some(i => i.status === 'ACTIVE')
)

const fetchData = async () => {
  if (!myId.value) {
    allJobs.value = []
    approvedCompanies.value = []
    myApplications.value = []
    myInternships.value = []
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error('请先使用学生账号登录')
    return
  }
  loading.value = true
  try {
    const [jobsRes, entRes, appRes, internshipRes] = await Promise.all([
      api.get('/data/jobs'),
      api.get('/data/enterprise-list'),
      api.get('/data/applications', { params: { studentId: myId.value } }),
      api.get('/data/internships', { params: { studentId: myId.value } })
    ])
    allJobs.value = jobsRes.data || []
    approvedCompanies.value = (entRes.data || []).filter(e => e.status === 'APPROVED').map(e => e.name)
    myApplications.value = appRes.data || []
    myInternships.value = internshipRes.data || []
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(getApiError(e, '岗位数据加载失败'))
    allJobs.value = []
    approvedCompanies.value = []
    myApplications.value = []
    myInternships.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const filteredJobs = computed(() =>
  allJobs.value
    .filter(j => j.status === 'OPEN')
    .filter(j => approvedCompanies.value.length === 0 || approvedCompanies.value.includes(j.company))
    .filter(j => !keyword.value || (j.title || '').includes(keyword.value) || (j.company || '').includes(keyword.value) || (j.location || '').includes(keyword.value))
    .filter(j => !filterLocation.value || j.location === filterLocation.value)
    .filter(j => !filterType.value || j.type === filterType.value)
)

const hasApplied = (jobId) => myApplications.value.some(a => a.jobId === jobId && a.status !== 'rejected')
const canApply = (job) => !placementLocked.value && !hasApplied(job.id)
const applyButtonText = (job) => {
  if (hasApplied(job.id)) return '已投递'
  if (placementLocked.value) return '已录用'
  return '不可投递'
}

const applyJob = async (job) => {
  if (!myId.value) {
    ElMessage.warning('请先登录学生账号')
    return
  }
  if (placementLocked.value) {
    ElMessage.warning('你已有录用或在岗实习，不能再次投递其他岗位')
    return
  }
  if (hasApplied(job.id)) {
    ElMessage.info('你已经投递过该岗位')
    return
  }

  try {
    await ElMessageBox.confirm(`确认向 ${job.company} 的「${job.title}」投递简历？`, '确认投递', { type: 'info' })
  } catch {
    return
  }

  try {
    const saved = await dataStore.applyJob(
      myId.value,
      myName.value,
      userStore.userInfo?.schoolName || userStore.userInfo?.organizationName || '',
      userStore.userInfo?.major || '',
      job.id,
      job.title
    )
    if (saved) myApplications.value.push(saved)
    job.count = (job.count || 0) + 1
    ElMessage.success(saved?.txHash ? '投递成功，已写入链上归档' : '投递成功')
  } catch (e) {
    ElMessage.error(getApiError(e, '投递失败，请稍后重试'))
  }
}

const getApiError = (error, fallback) => {
  const data = error?.response?.data
  return data?.message || data?.error || (typeof data === 'string' ? data : '') || fallback
}
</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; gap:16px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; margin:0; }
.filter-row { display:flex; gap:10px; align-items:center; flex-wrap:wrap; }
.filter-count { font-size:12px; color:#94A3B8; }
.lock-alert { margin-bottom:16px; border-radius:10px; }
.job-card { background:#FFF; border:1px solid rgba(226,232,240,0.82); border-radius:14px; padding:20px; margin-bottom:16px; box-shadow:0 2px 14px rgba(0,0,0,0.05); transition:all 0.2s; }
.job-card:hover { box-shadow:0 8px 24px rgba(15,23,42,0.08); transform:translateY(-1px); }
.job-card h3 { font-size:16px; font-weight:700; color:#1E293B; margin:0; }
.company { color:#2563EB; font-size:13px; margin:6px 0 10px; font-weight:600; }
.tags { display:flex; gap:6px; margin-bottom:10px; flex-wrap:wrap; }
.jt { font-size:11px; padding:3px 8px; border-radius:6px; background:#F1F5F9; color:#64748B; }
.jt-salary { background:#FFFBEB; color:#D97706; }
.jt-type { background:#EFF6FF; color:#2563EB; }
.desc { color:#64748B; font-size:12px; line-height:1.6; margin-bottom:14px; min-height:40px; }
.job-foot { display:flex; justify-content:space-between; align-items:center; gap:12px; }
.jf-count { font-size:11px; color:#94A3B8; }
.btn-applied { background:#F1F5F9!important; border:1px solid #E2E8F0!important; color:#94A3B8!important; cursor:not-allowed!important; }
@media (max-width: 760px) {
  .page-head { align-items:flex-start; flex-direction:column; }
  .filter-row :deep(.el-input), .filter-row :deep(.el-select) { width:100% !important; }
}
</style>
