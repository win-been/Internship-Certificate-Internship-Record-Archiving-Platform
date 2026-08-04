<template>
  <div class="page">
    <div class="page-head">
      <h1>数据看板</h1>
      <el-button text @click="fetchDashboardData" :loading="loading">刷新</el-button>
    </div>

    <el-row :gutter="20" class="stats">
      <el-col :xs="12" :md="6" v-for="s in stats" :key="s.label">
        <div class="stat-card" :class="{ 'is-clickable': s.to }" @click="openStat(s)">
          <div class="sc-icon" :style="{background:s.bg}">
            <el-icon :size="18" :color="s.color"><component :is="s.icon" /></el-icon>
          </div>
          <div class="stat-num">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="dashboard-row">
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-head">实习就业率</div>
          <div class="chart-placeholder">
            <div class="rate-panel">
              <div class="rate-number">{{ employmentRate }}%</div>
              <div class="rate-copy">在岗学生 {{ activeInternships.length }} / 投递与在岗合计 {{ totalTrackedStudents }}</div>
            </div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :md="12">
        <div class="card">
          <div class="card-head">岗位类型分布</div>
          <div class="chart-placeholder">
            <div class="bar-list">
              <div class="bar-item" v-for="b in industryBars" :key="b.name">
                <span class="bar-label">{{ b.name }}</span>
                <div class="bar-track"><div class="bar-fill" :style="{width:b.pct+'%',background:b.color}"></div></div>
                <span class="bar-val">{{ b.count }}</span>
              </div>
              <el-empty v-if="industryBars.length === 0" description="暂无岗位数据" :image-size="70" />
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <div class="section-title">最新岗位发布</div>
    <div class="table-card">
      <el-table :data="jobs" size="small" empty-text="暂无岗位" v-loading="loading">
        <el-table-column prop="title" label="岗位" min-width="160" />
        <el-table-column prop="company" label="企业" min-width="140" />
        <el-table-column prop="location" label="地点" width="90" />
        <el-table-column prop="salary" label="薪资" width="150" />
        <el-table-column prop="count" label="申请" width="70" align="center" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}">
            <span class="st" :class="row.status === 'OPEN' ? 'st-open' : 'st-closed'">
              {{ row.status === 'OPEN' ? '开放中' : '已关闭' }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { User, OfficeBuilding, Document, Stamp } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'

const dataStore = useDataStore()
const userStore = useUserStore()
const router = useRouter()
const loading = ref(false)
const jobs = ref(dataStore.jobs || [])
const applications = ref(dataStore.applications || [])
const internships = ref(dataStore.internships || [])
const archives = ref(dataStore.archives || [])
const schoolStudents = ref(dataStore.schoolStudents || [])
const mySchoolId = computed(() => userStore.userInfo?.schoolId || userStore.userInfo?.userId || null)

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const [jobsRes, appsRes, internshipsRes, archivesRes, studentsRes] = await Promise.all([
      api.get('/data/jobs'),
      api.get('/data/applications', { params: { schoolId: mySchoolId.value } }),
      api.get('/data/internships', { params: { schoolId: mySchoolId.value } }),
      api.get('/data/archives'),
      api.get('/data/school-students', { params: { schoolId: mySchoolId.value } }).catch(() => null)
    ])
    jobs.value = jobsRes.data || []
    applications.value = appsRes.data || []
    internships.value = internshipsRes.data || []
    archives.value = archivesRes.data || []
    schoolStudents.value = studentsRes?.data || schoolStudents.value
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    if (e.response) ElMessage.error('看板数据加载失败')
    jobs.value = dataStore.jobs || []
    applications.value = dataStore.applications || []
    internships.value = dataStore.internships || []
    archives.value = dataStore.archives || []
    schoolStudents.value = dataStore.schoolStudents || []
  } finally {
    loading.value = false
  }
}

onMounted(fetchDashboardData)

const activeInternships = computed(() => internships.value.filter(i => i.status === 'ACTIVE'))
const pendingApplications = computed(() => applications.value.filter(a => a.status === 'pending'))
const totalTrackedStudents = computed(() => activeInternships.value.length + pendingApplications.value.length)
const employmentRate = computed(() => totalTrackedStudents.value > 0 ? Math.round((activeInternships.value.length / totalTrackedStudents.value) * 100) : 0)

const stats = computed(() => [
  { label:'在岗学生', value: activeInternships.value.length, icon: User, color:'#2563EB', bg:'#EFF6FF' },
  { label:'合作企业', value: [...new Set((jobs.value || []).map(j => j.company || ''))].filter(Boolean).length, icon: OfficeBuilding, color:'#7C3AED', bg:'#F5F3FF' },
  { label:'发布岗位', value: jobs.value.length, icon: Document, color:'#059669', bg:'#ECFDF5' },
  { label:'链上存证', value: archives.value.filter(a => a.chainStatus === 'ON_CHAIN' || a.txHash).length, icon: Stamp, color:'#D97706', bg:'#FFFBEB', to:'/school/verify' },
])

const openStat = (stat) => {
  if (stat?.to) router.push(stat.to)
}

const colors = ['#2563EB', '#7C3AED', '#059669', '#D97706', '#06B6D4']
const industryBars = computed(() => {
  const groups = new Map()
  jobs.value.forEach(job => {
    const key = job.type || '其他岗位'
    groups.set(key, (groups.get(key) || 0) + 1)
  })
  const max = Math.max(...groups.values(), 1)
  return [...groups.entries()].map(([name, count], index) => ({
    name,
    count,
    pct: Math.max(12, Math.round((count / max) * 100)),
    color: colors[index % colors.length]
  }))
})
</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; margin:0; }
.stats { margin-bottom:8px; }
.stat-card { background:#FFF; border:1px solid rgba(226,232,240,0.82); border-radius:14px; padding:20px; box-shadow:0 2px 14px rgba(0,0,0,0.05); transition:all 0.2s; margin-bottom:16px; cursor:default; }
.stat-card.is-clickable { cursor:pointer; }
.stat-card:hover { transform:translateY(-1px); box-shadow:0 8px 24px rgba(15,23,42,0.08); }
.sc-icon { width:36px; height:36px; border-radius:50%; display:flex; align-items:center; justify-content:center; margin-bottom:12px; }
.stat-num { font-size:28px; font-weight:700; color:#1E293B; }
.stat-label { font-size:13px; color:#94A3B8; margin-top:2px; }
.dashboard-row { margin-top:4px; }
.card { background:#FFF; border:1px solid rgba(226,232,240,0.82); border-radius:14px; padding:20px; box-shadow:0 2px 14px rgba(0,0,0,0.05); margin-bottom:20px; }
.card-head { font-size:14px; font-weight:700; color:#334155; margin-bottom:16px; }
.chart-placeholder { min-height:180px; display:flex; align-items:center; justify-content:center; }
.rate-panel { text-align:center; }
.rate-number { font-size:42px; font-weight:800; color:#059669; }
.rate-copy { font-size:12px; color:#94A3B8; margin-top:6px; }
.bar-list { width:100%; display:flex; flex-direction:column; gap:14px; }
.bar-item { display:flex; align-items:center; gap:10px; }
.bar-label { width:78px; font-size:12px; color:#64748B; text-align:right; flex-shrink:0; }
.bar-track { flex:1; height:10px; background:#F1F5F9; border-radius:5px; overflow:hidden; }
.bar-fill { height:100%; border-radius:5px; transition:width 0.4s; }
.bar-val { width:28px; font-size:12px; color:#334155; font-weight:600; }
.section-title { font-size:14px; font-weight:700; color:#334155; margin:4px 0 12px; }
.table-card { background:#FFF; border:1px solid rgba(226,232,240,0.82); border-radius:14px; padding:18px; box-shadow:0 2px 14px rgba(0,0,0,0.05); }
.st { display:inline-block; padding:3px 9px; border-radius:999px; font-size:11px; font-weight:600; }
.st-open { background:#ECFDF5; color:#059669; }
.st-closed { background:#F1F5F9; color:#64748B; }
</style>
